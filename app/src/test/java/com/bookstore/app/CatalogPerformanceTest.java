package com.bookstore.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.catalog.api.BookSearchQuery;
import com.bookstore.catalog.api.BookSummary;
import com.bookstore.catalog.application.CatalogService;
import com.bookstore.catalog.persistence.JdbcAuthorRepository;
import com.bookstore.catalog.persistence.InMemoryFileStorage;
import com.bookstore.catalog.persistence.JdbcBookRepository;
import com.bookstore.catalog.persistence.JdbcCategoryRepository;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.jdbc.TestDatabase;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.paging.SortOrder;
import com.bookstore.shared.kernel.text.TextNormalizer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * NFR-9 (ADR-0022): with 10,000 books, the catalog list and the search answer within 300 ms. The
 * books are written straight into the tables; each query is measured after a warm-up, and the
 * median of several runs is compared, so one slow run of a busy machine does not decide.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogPerformanceTest {

    static final int BOOKS = 10_000;
    static final Duration LIMIT = Duration.ofMillis(300);

    private static final int AUTHORS = 500;
    private static final int WARM_UP = 3;
    private static final int RUNS = 9;
    private static final String[] WORDS = {"Kürk", "Mantolu", "Madonna", "Çalıkuşu", "Deniz", "Gece", "Yolculuk",
        "Şehir", "Ağaç", "Işık", "Sessiz", "Kırmızı", "Bahar", "Ölüm", "Göç", "Saat"};

    private final TestDatabase database = TestDatabase.withModules("catalog");
    private CatalogService catalog;
    private long novelsId;

    @BeforeAll
    void fillTheCatalog() {
        JdbcExecutor jdbc = database.jdbc();
        long literature = jdbc.insert("INSERT INTO categories (parent_id, name) VALUES (?, ?)", null, "Edebiyat");
        novelsId = jdbc.insert("INSERT INTO categories (parent_id, name) VALUES (?, ?)", literature, "Roman");
        long science = jdbc.insert("INSERT INTO categories (parent_id, name) VALUES (?, ?)", null, "Bilim");
        long[] categories = {novelsId, literature, science};

        List<Object[]> authors = new ArrayList<>();
        for (int a = 1; a <= AUTHORS; a++) {
            String name = WORDS[a % WORDS.length] + " Yazar " + a;
            authors.add(new Object[]{name, TextNormalizer.searchKey(name)});
        }
        jdbc.batch("INSERT INTO authors (name, name_key) VALUES (?, ?)", authors);
        long firstAuthor = jdbc.queryForLong("SELECT MIN(id) FROM authors");

        List<Object[]> books = new ArrayList<>();
        for (int b = 1; b <= BOOKS; b++) {
            String title = WORDS[b % WORDS.length] + " " + WORDS[(b / 7) % WORDS.length] + " " + b;
            books.add(new Object[]{isbn(b), title, TextNormalizer.searchKey(title), categories[b % categories.length],
                1900 + b % 125, b % 4 == 0 ? "en" : "tr", new BigDecimal(20 + b % 480).setScale(2), b % 13});
        }
        jdbc.batch("INSERT INTO books (isbn, title, title_key, category_id, publication_year, language, price, stock)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)", books);
        long firstBook = jdbc.queryForLong("SELECT MIN(id) FROM books");
        List<Object[]> links = new ArrayList<>();
        for (int b = 0; b < BOOKS; b++) {
            links.add(new Object[]{firstBook + b, firstAuthor + b % AUTHORS, 0});
        }
        jdbc.batch("INSERT INTO book_authors (book_id, author_id, sort_order) VALUES (?, ?, ?)", links);

        JdbcCategoryRepository categoryRepository = new JdbcCategoryRepository(jdbc);
        catalog = new CatalogService(new JdbcBookRepository(jdbc, categoryRepository), new JdbcAuthorRepository(jdbc),
                categoryRepository, new InMemoryFileStorage());
    }

    @Test
    void theFirstPageOfTheWholeCatalog() {
        Page<BookSummary> page = measure(() -> catalog.search(BookSearchQuery.everything(), PageRequest.first()));

        assertThat(page.totalElements()).isEqualTo(BOOKS);
    }

    @Test
    void aTextSearchOverTitlesAndAuthors() {
        Page<BookSummary> page = measure(() -> catalog.search(BookSearchQuery.everything().withText("kirmizi"),
                PageRequest.first()));

        assertThat(page.totalElements()).isPositive();
    }

    @Test
    void aCategoryWithItsSubcategoriesInStockByPriceDescending() {
        BookSearchQuery query = BookSearchQuery.everything()
                .inCategory(novelsId)
                .onlyInStock()
                .priced(Money.of("50"), Money.of("400"))
                .sortedBy("PRICE", SortOrder.DESC);

        Page<BookSummary> page = measure(() -> catalog.search(query, new PageRequest(3, 20)));

        assertThat(page.content()).hasSize(20);
    }

    /** Runs a query after a warm-up and requires the median time to stay under the limit. */
    private <T> T measure(Supplier<T> query) {
        T answer = null;
        for (int i = 0; i < WARM_UP; i++) {
            answer = query.get();
        }
        long[] nanos = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            answer = query.get();
            nanos[i] = System.nanoTime() - start;
        }
        Arrays.sort(nanos);
        Duration median = Duration.ofNanos(nanos[RUNS / 2]);
        assertThat(median).as("median of %d runs", RUNS).isLessThan(LIMIT);
        return answer;
    }

    private static String isbn(int n) {
        String body = "978" + String.format("%09d", n);
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (body.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return body + (10 - sum % 10) % 10;
    }
}
