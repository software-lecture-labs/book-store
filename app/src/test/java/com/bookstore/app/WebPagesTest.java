package com.bookstore.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.catalog.domain.InsufficientStockException;
import com.bookstore.catalog.domain.Language;
import com.bookstore.sales.api.CartChangedException;
import com.bookstore.sales.api.PaymentDeclinedException;
import com.bookstore.sales.application.CheckoutService;
import com.bookstore.sales.domain.OrderRequestLog;
import com.bookstore.sales.domain.OrderStatus;
import com.bookstore.sales.domain.PaymentMethod;
import com.bookstore.sales.domain.StockShortageException;
import com.bookstore.shared.kernel.error.AccessDeniedException;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.web.Caller;
import com.bookstore.shared.web.ProblemMapper;
import com.bookstore.shared.web.WebException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * The HTML pages of ADR-0011 against their dictionaries and the API (NFR-4): both languages have
 * the same texts, no page asks for a text that is missing, every error code the server sends has a
 * message, and the pages build their content without {@code innerHTML}.
 */
class WebPagesTest {

    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Pattern HTML_KEY = Pattern.compile("data-i18n(?:-placeholder|-title|-label)?=\"([^\"]+)\"");
    private static final Pattern JS_LITERAL = Pattern.compile("'([a-z]+\\.[A-Za-z_.-]+)'");
    private static final Pattern LINK = Pattern.compile("(?:href|src)=\"([^\"#?:]+)[^\"]*\"");

    private final Map<String, String> turkish = dictionary("tr");
    private final Map<String, String> english = dictionary("en");

    @Test
    void bothLanguagesHaveTheSameTextsAndNoneIsEmpty() {
        assertThat(english.keySet()).isEqualTo(turkish.keySet());
        assertThat(turkish.values()).noneMatch(String::isBlank);
        assertThat(english.values()).noneMatch(String::isBlank);
    }

    @Test
    void everyTextAPageAsksForExists() {
        Set<String> wanted = new TreeSet<>();
        for (Path page : files(".html")) {
            wanted.addAll(matches(HTML_KEY, read(page)));
        }
        Set<String> namespaces = new TreeSet<>();
        turkish.keySet().forEach(key -> namespaces.add(key.substring(0, key.indexOf('.'))));
        for (Path script : files(".js")) {
            matches(JS_LITERAL, read(script)).stream()
                    .filter(literal -> namespaces.contains(literal.substring(0, literal.indexOf('.'))))
                    .filter(literal -> !literal.matches(".*\\.(html|js|json|css)"))
                    .forEach(wanted::add);
        }
        assertThat(wanted).isNotEmpty();
        assertThat(turkish.keySet()).containsAll(wanted);
    }

    @Test
    void everyStatusPaymentMethodLanguageAndRoleHasAName() {
        Arrays.stream(OrderStatus.values()).forEach(status -> assertThat(turkish).containsKey("status." + status));
        Arrays.stream(OrderStatus.values()).forEach(status -> assertThat(turkish).containsKey("admin.moveTo." + status));
        Arrays.stream(PaymentMethod.values()).forEach(method -> assertThat(turkish).containsKey("payment." + method));
        Arrays.stream(Language.values()).forEach(language -> assertThat(turkish).containsKey("language." + language.code()));
        assertThat(turkish).containsKeys("role." + Caller.CUSTOMER, "role." + Caller.ADMIN);
    }

    @Test
    void everyErrorCodeOfTheServerHasAMessage() {
        List<String> codes = new ArrayList<>(List.of(
                AccessDeniedException.CODE, NotFoundException.CODE, ValidationException.CODE,
                CartChangedException.CODE, PaymentDeclinedException.CODE,
                InsufficientStockException.CODE, StockShortageException.CODE, ProblemMapper.INTERNAL_ERROR,
                WebException.unauthenticated().code(), WebException.signInFailed().code(),
                WebException.clientHeaderMissing().code(), WebException.forbidden().code(),
                WebException.invalid(List.of()).code(), WebException.noRoute("/").code(),
                WebException.methodNotAllowed("GET", "/").code(), WebException.unsupportedMediaType().code(),
                // Codes written where they are thrown.
                "email.taken", "book.inactive", "review.duplicate", "review.not-eligible", "cart.empty",
                "order.empty-cart", "order.illegal-transition", "request.bad-parameter", "request.malformed-json",
                // Phase 8, the administrator's endpoints (ADR-0021).
                "book.isbn-taken", "author.in-use", "category.in-use", "category.has-children", "category.name-taken",
                "customer.admin-protected", "request.unreadable",
                // Phase 9, lost updates (ADR-0022).
                WebException.preconditionFailed().code(), ConflictException.CONCURRENT_MODIFICATION,
                CheckoutService.KEY_REUSED, OrderRequestLog.DUPLICATE_KEY));
        codes.forEach(code -> assertThat(turkish).as(code).containsKey("problem." + code));
    }

    @Test
    void everyPageHasTheSharedPartsAndItsLinksLeadSomewhere() {
        for (Path page : files(".html")) {
            String html = read(page);
            assertThat(html).as(page.toString()).contains("id=\"site-header\"", "id=\"messages\"",
                    "<script type=\"module\" src=\"js/pages/", "href=\"css/site.css\"");
            for (String target : matches(LINK, html)) {
                assertThat(WEBAPP.resolve(target)).as(page + " → " + target).exists();
            }
        }
    }

    @Test
    void scriptsNeverWriteHtmlFromText() {
        for (Path script : files(".js")) {
            assertThat(read(script)).as(script.toString())
                    .doesNotContain("innerHTML", "outerHTML", "insertAdjacentHTML", "document.write", "eval(");
        }
    }

    private static Map<String, String> dictionary(String language) {
        try {
            return new ObjectMapper().readValue(WEBAPP.resolve("i18n/" + language + ".json").toFile(),
                    new TypeReference<>() { });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> files(String suffix) {
        try (Stream<Path> paths = Files.walk(WEBAPP)) {
            return paths.filter(path -> path.toString().endsWith(suffix)).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> matches(Pattern pattern, String text) {
        List<String> found = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }
}
