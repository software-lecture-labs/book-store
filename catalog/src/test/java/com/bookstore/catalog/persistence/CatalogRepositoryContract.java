package com.bookstore.catalog.persistence;

import static com.bookstore.catalog.domain.BookSpecifications.active;
import static com.bookstore.catalog.domain.BookSpecifications.byAuthor;
import static com.bookstore.catalog.domain.BookSpecifications.inCategories;
import static com.bookstore.catalog.domain.BookSpecifications.inLanguage;
import static com.bookstore.catalog.domain.BookSpecifications.inStock;
import static com.bookstore.catalog.domain.BookSpecifications.matchingText;
import static com.bookstore.catalog.domain.BookSpecifications.pricedFrom;
import static com.bookstore.catalog.domain.BookSpecifications.pricedUpTo;
import static com.bookstore.catalog.domain.BookSpecifications.stockBelow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.catalog.domain.Author;
import com.bookstore.catalog.domain.AuthorId;
import com.bookstore.catalog.domain.Book;
import com.bookstore.catalog.domain.BookSort;
import com.bookstore.catalog.domain.BookSortField;
import com.bookstore.catalog.domain.Category;
import com.bookstore.catalog.domain.Language;
import com.bookstore.catalog.domain.RatingSummary;
import com.bookstore.catalog.domain.Stock;
import com.bookstore.shared.jdbc.JdbcExecutor;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.paging.SortOrder;
import com.bookstore.shared.kernel.specification.Specification;

import java.time.Year;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The contract of the catalog's repositories (ADR-0008): every implementation – in memory, H2 and
 * PostgreSQL – must pass these tests. Subclasses only say which one they test.
 */
abstract class CatalogRepositoryContract {

    protected final CatalogDb db = newDb();

    /** @return fresh, empty repositories of the implementation under test */
    protected abstract CatalogDb newDb();

    @Nested
    class Categories {

        @Test
        void treeIsRebuiltWithParentsAndChildren() {
            Category literature = db.rootCategory("Literature");
            db.categories.save(literature.addSubcategory("Novel"));
            db.categories.save(literature.addSubcategory("Poetry"));
            db.rootCategory("Art");

            List<Category> roots = db.categories.findRoots();

            assertThat(roots).extracting(Category::name).containsExactly("Art", "Literature");
            assertThat(roots.get(1).children()).extracting(Category::name).containsExactly("Novel", "Poetry");
            Category novel = db.categories.findById(roots.get(1).children().getFirst().id()).orElseThrow();
            assertThat(novel.path()).containsExactly("Literature", "Novel");
        }

        @Test
        void siblingNamesAreUniqueInTheDatabaseToo() {
            Category literature = db.rootCategory("Literature");
            db.categories.save(literature.addSubcategory("Novel"));
            Category reloaded = db.categories.findById(literature.id()).orElseThrow();
            Category poetry = db.categories.save(reloaded.addSubcategory("Poetry"));
            poetry.rename("Novel");

            assertThatThrownBy(() -> db.categories.save(poetry))
                    .isInstanceOfSatisfying(ConflictException.class,
                            e -> assertThat(e.code()).isEqualTo("category.name-taken"));
        }

        @Test
        void renameAndDelete() {
            Category art = db.rootCategory("Art");
            art.rename("Arts");
            db.categories.save(art);

            assertThat(db.categories.findById(art.id())).hasValueSatisfying(c -> {
                assertThat(c.name()).isEqualTo("Arts");
                assertThat(c.version()).isEqualTo(1);
            });

            db.categories.delete(art);
            assertThat(db.categories.findById(art.id())).isEmpty();
        }
    }

    @Nested
    class Authors {

        @Test
        void savedSortedAndRenamed() {
            Author pamuk = db.author("Orhan Pamuk");
            db.author("Ahmet Ümit");

            assertThat(db.authors.findAll()).extracting(Author::name).containsExactly("Ahmet Ümit", "Orhan Pamuk");

            pamuk.rename("Ferit Orhan Pamuk");
            db.authors.save(pamuk);
            assertThat(db.authors.findById(pamuk.id())).map(Author::name).contains("Ferit Orhan Pamuk");
            assertThat(db.authors.findAllById(List.of(pamuk.id(), AuthorId.of(999)))).hasSize(1);
            assertThat(db.authors.findAllById(List.of())).isEmpty();
        }
    }

    @Nested
    class Books {

        @Test
        void everyAttributeSurvivesTheRoundTrip() {
            Author gaiman = db.author("Neil Gaiman");
            Author pratchett = db.author("Terry Pratchett");
            Book saved = db.books.save(db.bookBuilder("Good Omens", "249.90", 7)
                    .authors(List.of(gaiman, pratchett))
                    .publisher("Workman")
                    .publicationYear(Year.of(1990))
                    .language(Language.ENGLISH)
                    .pageCount(412)
                    .description("Kıyamet geliyor.")
                    .coverImage("covers/good-omens.jpg")
                    .ratings(new RatingSummary(2, 9))
                    .build());

            Book loaded = db.books.findById(saved.id()).orElseThrow();

            assertThat(loaded.isbn()).isEqualTo(saved.isbn());
            assertThat(loaded.title()).isEqualTo("Good Omens");
            assertThat(loaded.authors()).extracting(Author::name)
                    .containsExactly("Author of Good Omens", "Neil Gaiman", "Terry Pratchett");
            assertThat(loaded.category().id()).isEqualTo(saved.category().id());
            assertThat(loaded.publisher()).isEqualTo("Workman");
            assertThat(loaded.publicationYear()).contains(Year.of(1990));
            assertThat(loaded.language()).isEqualTo(Language.ENGLISH);
            assertThat(loaded.pageCount()).isEqualTo(412);
            assertThat(loaded.description()).isEqualTo("Kıyamet geliyor.");
            assertThat(loaded.price()).isEqualTo(Money.of("249.90"));
            assertThat(loaded.stock()).isEqualTo(Stock.of(7));
            assertThat(loaded.coverImage()).contains("covers/good-omens.jpg");
            assertThat(loaded.isActive()).isTrue();
            assertThat(loaded.ratings()).isEqualTo(new RatingSummary(2, 9));
            assertThat(loaded.version()).isZero();
            assertThat(db.books.findByIsbn(saved.isbn())).map(Book::id).contains(saved.id());
        }

        @Test
        void optionalAttributesMayBeMissing() {
            Book saved = db.book("Untitled", "10", 0);

            Book loaded = db.books.findById(saved.id()).orElseThrow();

            assertThat(loaded.publicationYear()).isEmpty();
            assertThat(loaded.coverImage()).isEmpty();
            assertThat(loaded.publisher()).isEmpty();
        }

        @Test
        void updateChangesAttributesAuthorsAndVersion() {
            Book book = db.book("Dune", "100", 5);
            Author herbert = db.author("Frank Herbert");

            book.changePrice(Money.of("120"));
            book.reserve(2);
            book.changeAuthors(List.of(herbert));
            book.deactivate();
            db.books.save(book);

            Book loaded = db.books.findById(book.id()).orElseThrow();
            assertThat(loaded.price()).isEqualTo(Money.of("120.00"));
            assertThat(loaded.stock().quantity()).isEqualTo(3);
            assertThat(loaded.authors()).containsExactly(herbert);
            assertThat(loaded.isActive()).isFalse();
            assertThat(loaded.version()).isEqualTo(1);
        }

        @Test
        void isbnIsUnique() {
            Book first = db.book("First", "10", 1);
            Book copy = db.bookBuilder("Copy", "10", 1).isbn(first.isbn()).build();

            assertThatThrownBy(() -> db.books.save(copy))
                    .isInstanceOfSatisfying(ConflictException.class,
                            e -> assertThat(e.code()).isEqualTo("book.isbn-taken"));
        }

        @Test
        void staleCopyCannotOverwriteANewerOne() {
            Book book = db.book("Dune", "100", 5);
            Book first = db.books.findById(book.id()).orElseThrow();
            Book second = db.books.findById(book.id()).orElseThrow();

            first.reserve(5);
            db.books.save(first);
            second.reserve(5);

            assertThatThrownBy(() -> db.books.save(second))
                    .isInstanceOfSatisfying(ConflictException.class,
                            e -> assertThat(e.code()).isEqualTo(JdbcExecutor.CONCURRENT_MODIFICATION));
            assertThat(db.books.findById(book.id()).orElseThrow().stock().quantity()).isZero();
        }

        @Test
        void knowsWhetherAuthorsAndCategoriesHaveBooks() {
            Book book = db.book("Dune", "100", 5);
            Author nobody = db.author("Nobody");

            assertThat(db.books.existsByAuthorId(book.authors().iterator().next().id())).isTrue();
            assertThat(db.books.existsByAuthorId(nobody.id())).isFalse();
            assertThat(db.books.existsByCategoryId(book.category().id())).isTrue();
            assertThat(db.books.existsByCategoryId(db.rootCategory("Empty").id())).isFalse();
        }
    }

    /**
     * The same searches as the domain's {@code BookSpecificationsTest}, run as SQL: the database
     * must agree with {@link Specification#isSatisfiedBy} and the sort comparator.
     */
    @Nested
    class Search {

        private final Category literature = db.rootCategory("Literature");
        private final Category novels = db.categories.save(literature.addSubcategory("Novel"));
        private final Author ali = db.author("Sabahattin Ali");

        private final Book madonna = db.books.save(db.bookBuilder("Kürk Mantolu Madonna", "120", 5)
                .author(ali).category(novels).publicationYear(Year.of(1943)).build());
        private final Book dune = db.books.save(db.bookBuilder("Dune", "350", 0)
                .language(Language.ENGLISH).publicationYear(Year.of(1965)).build());
        private final Book calikusu = db.books.save(db.bookBuilder("Çalıkuşu", "90", 2).category(novels).build());
        private final Book hidden = db.books.save(db.bookBuilder("Hidden", "50", 9).active(false).build());

        @Test
        void activeBooksComeBackByTitle() {
            assertThat(search(active())).containsExactly("Çalıkuşu", "Dune", "Kürk Mantolu Madonna");
            assertThat(search(Specification.all())).contains("Hidden");
        }

        @Test
        void textIgnoresCaseAndTurkishLettersInTitlesAndAuthors() {
            assertThat(search(matchingText("CALIKUSU"))).containsExactly("Çalıkuşu");
            assertThat(search(matchingText("sabahattın"))).containsExactly("Kürk Mantolu Madonna");
            assertThat(search(matchingText("mantolu"))).containsExactly("Kürk Mantolu Madonna");
        }

        @Test
        void textMatchesIsbnFragmentsOfFourDigitsOrMore() {
            assertThat(search(matchingText(dune.isbn().formatted()))).containsExactly("Dune");
            assertThat(search(matchingText("978"))).isEmpty();
        }

        @Test
        void wildcardsInTheTextAreTakenLiterally() {
            assertThat(search(matchingText("%"))).isEmpty();
            assertThat(search(matchingText("_"))).isEmpty();
        }

        @Test
        void filters() {
            assertThat(search(pricedFrom(Money.of("100")).and(pricedUpTo(Money.of("350")))))
                    .containsExactly("Dune", "Kürk Mantolu Madonna");
            assertThat(search(inLanguage(Language.ENGLISH))).containsExactly("Dune");
            assertThat(search(active().and(inStock()))).containsExactly("Çalıkuşu", "Kürk Mantolu Madonna");
            assertThat(search(inCategories(Set.of(novels.id()))))
                    .containsExactly("Çalıkuşu", "Kürk Mantolu Madonna");
            assertThat(search(byAuthor(ali.id()))).containsExactly("Kürk Mantolu Madonna");
            assertThat(search(stockBelow(3))).containsExactly("Çalıkuşu", "Dune");
            assertThat(search(active().and(stockBelow(6)))).containsExactly("Çalıkuşu", "Dune", "Kürk Mantolu Madonna");
        }

        @Test
        void orAndNotAreTranslatedToo() {
            assertThat(search(inLanguage(Language.ENGLISH).or(byAuthor(ali.id()))))
                    .containsExactly("Dune", "Kürk Mantolu Madonna");
            assertThat(search(inStock().not())).containsExactly("Dune");
        }

        @Test
        void sorting() {
            madonna.addRating(5);
            db.books.save(madonna);
            dune.addRating(3);
            db.books.save(dune);

            assertThat(search(active(), sort(BookSortField.PRICE, SortOrder.DESC)))
                    .containsExactly("Dune", "Kürk Mantolu Madonna", "Çalıkuşu");
            assertThat(search(active(), sort(BookSortField.PUBLICATION_YEAR, SortOrder.ASC)))
                    .containsExactly("Çalıkuşu", "Kürk Mantolu Madonna", "Dune");
            assertThat(search(active(), sort(BookSortField.PUBLICATION_YEAR, SortOrder.DESC)))
                    .containsExactly("Dune", "Kürk Mantolu Madonna", "Çalıkuşu");
            assertThat(search(active(), sort(BookSortField.RATING, SortOrder.DESC)))
                    .containsExactly("Kürk Mantolu Madonna", "Dune", "Çalıkuşu");
        }

        @Test
        void sqlAndInMemoryRulesAgreeOnEveryCombination() {
            List<Book> all = List.of(madonna, dune, calikusu, hidden);
            List<Specification<Book>> rules = List.of(Specification.all(), active(), inStock().not(),
                    active().and(matchingText("dune").or(byAuthor(ali.id()))),
                    inCategories(Set.of(novels.id())).and(pricedUpTo(Money.of("100")).not()),
                    stockBelow(5).or(active().not()));
            for (Specification<Book> rule : rules) {
                for (BookSortField field : BookSortField.values()) {
                    for (SortOrder order : SortOrder.values()) {
                        BookSort sort = sort(field, order);
                        List<String> expected = all.stream().filter(rule::isSatisfiedBy)
                                .sorted(sort.comparator()).map(Book::title).toList();

                        assertThat(search(rule, sort)).as(rule + " " + field + " " + order)
                                .containsExactlyElementsOf(expected);
                    }
                }
            }
        }

        @Test
        void pagesCarryTheTotal() {
            Page<Book> second = db.books.search(active(), BookSort.DEFAULT, new PageRequest(1, 2));

            assertThat(second.content()).extracting(Book::title).containsExactly("Kürk Mantolu Madonna");
            assertThat(second.totalElements()).isEqualTo(3);
            assertThat(second.totalPages()).isEqualTo(2);
            assertThat(second.content().getFirst().authors()).contains(ali);
        }

        private BookSort sort(BookSortField field, SortOrder order) {
            return new BookSort(field, order);
        }

        private List<String> search(Specification<Book> rule) {
            return search(rule, BookSort.DEFAULT);
        }

        private List<String> search(Specification<Book> rule, BookSort sort) {
            return db.books.search(rule, sort, PageRequest.first()).content().stream().map(Book::title).toList();
        }
    }
}
