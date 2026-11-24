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
import java.util.HashMap;
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
    void everyPageLetsTheKeyboardSkipToItsContent() {
        for (Path page : files(".html")) {
            assertThat(read(page)).as(page.toString())
                    .contains("<a class=\"skip-link\" href=\"#main\"", "<main id=\"main\" tabindex=\"-1\">");
        }
    }

    /** WCAG 1.3.1 and 4.1.2: every field has a name a screen reader can say. */
    @Test
    void everyFormFieldHasALabel() {
        Pattern field = Pattern.compile("<(input|select|textarea)\\b[^>]*>");
        Pattern id = Pattern.compile("\\bid=\"([^\"]+)\"");
        for (Path page : files(".html")) {
            String html = read(page);
            Matcher matcher = field.matcher(html);
            while (matcher.find()) {
                String tag = matcher.group();
                if (tag.contains("type=\"hidden\"") || tag.contains("aria-label=") || tag.contains("data-i18n-label=")) {
                    continue;
                }
                Matcher fieldId = id.matcher(tag);
                boolean labelledFor = fieldId.find() && html.contains("for=\"" + fieldId.group(1) + "\"");
                String before = html.substring(0, matcher.start());
                boolean insideLabel = before.lastIndexOf("<label") > before.lastIndexOf("</label>");
                assertThat(labelledFor || insideLabel).as(page + ": " + tag).isTrue();
            }
        }
    }

    /** WCAG 1.4.3 and 1.4.11: text at least 4.5:1 and the rating stars 3:1, in both themes. */
    @Test
    void theColoursOfBothThemesAreReadable() {
        String css = read(WEBAPP.resolve("css/site.css"));
        int dark = css.indexOf("@media (prefers-color-scheme: dark)");
        List<Map<String, String>> themes = List.of(colours(css.substring(css.indexOf(":root {"), dark)),
                colours(css.substring(dark, css.indexOf("}\n}", dark))));
        String[][] text = {{"ink", "paper"}, {"ink", "surface"}, {"ink", "accent-soft"}, {"muted", "paper"},
            {"muted", "surface"}, {"accent", "paper"}, {"accent", "surface"}, {"accent-ink", "accent"},
            {"danger", "danger-soft"}, {"danger", "surface"}, {"success", "success-soft"}, {"success", "surface"}};
        for (Map<String, String> theme : themes) {
            for (String[] pair : text) {
                assertThat(contrast(theme.get(pair[0]), theme.get(pair[1]))).as("%s on %s", pair[0], pair[1])
                        .isGreaterThanOrEqualTo(4.5);
            }
            assertThat(contrast(theme.get("star"), theme.get("paper"))).as("stars").isGreaterThanOrEqualTo(3.0);
            assertThat(contrast(theme.get("star"), theme.get("surface"))).as("stars").isGreaterThanOrEqualTo(3.0);
        }
    }

    private static Map<String, String> colours(String block) {
        Map<String, String> colours = new HashMap<>();
        Matcher matcher = Pattern.compile("--([a-z-]+):\\s*(#[0-9a-fA-F]{6})").matcher(block);
        while (matcher.find()) {
            colours.put(matcher.group(1), matcher.group(2));
        }
        return colours;
    }

    private static double contrast(String first, String second) {
        double a = luminance(first);
        double b = luminance(second);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    private static double luminance(String hex) {
        double[] rgb = new double[3];
        for (int i = 0; i < 3; i++) {
            double channel = Integer.parseInt(hex.substring(1 + 2 * i, 3 + 2 * i), 16) / 255.0;
            rgb[i] = channel <= 0.03928 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
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
