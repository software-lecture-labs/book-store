package com.bookstore.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.catalog.api.AdminBookRow;
import com.bookstore.catalog.api.BookCommand;
import com.bookstore.catalog.api.BookDetails;
import com.bookstore.catalog.api.BookSearchQuery;
import com.bookstore.catalog.api.CatalogStats;
import com.bookstore.catalog.api.CategoryNode;
import com.bookstore.catalog.domain.Author;
import com.bookstore.catalog.domain.Book;
import com.bookstore.catalog.domain.CatalogTestData;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.error.DomainException;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.ValidationException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.paging.PageRequest;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CatalogAdminServiceTest {

    private final CatalogShop shop = new CatalogShop();
    private final Author ali = shop.storedAuthor("Sabahattin Ali");
    private final Author hikmet = shop.storedAuthor("Nâzım Hikmet");

    private BookCommand madonna(String isbn) {
        return new BookCommand(isbn, "Kürk Mantolu Madonna", List.of(ali.id().value()), shop.novels.id().value(),
                "YKY", 1943, "tr", 160, "A novel.", Money.of("95.00"), 12);
    }

    @Nested
    class Books {

        @Test
        void aNewBookIsOnSaleWithEverythingTheAdministratorEntered() {
            BookDetails created = shop.admin.createBook(madonna("978-975-363-802-9"));

            assertThat(created.isbn()).isEqualTo("9789753638029");
            assertThat(created.authors()).extracting(BookDetails.AuthorRef::name).containsExactly("Sabahattin Ali");
            assertThat(created.categoryPath()).containsExactly("Literature", "Novel");
            assertThat(created.publicationYear().getValue()).isEqualTo(1943);
            assertThat(created.stock()).isEqualTo(12);
            assertThat(created.active()).isTrue();
            assertThat(shop.catalog.book(created.id()).title()).isEqualTo("Kürk Mantolu Madonna");
        }

        @Test
        void anIsbnBelongsToOneBook() {
            shop.admin.createBook(madonna("9789753638029"));

            assertThatThrownBy(() -> shop.admin.createBook(madonna("978-975-363-802-9")))
                    .isInstanceOfSatisfying(ConflictException.class,
                            e -> assertThat(e.code()).isEqualTo("book.isbn-taken"));
        }

        @Test
        void wrongValuesAreRejectedBeforeAnythingIsStored() {
            assertThatThrownBy(() -> shop.admin.createBook(madonna("12345"))).isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> shop.admin.createBook(new BookCommand("9789753638029", "Title", List.of(),
                    shop.novels.id().value(), null, null, "tr", 0, null, Money.of("10"), 1)))
                    .isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> shop.admin.createBook(new BookCommand("9789753638029", "Title", List.of(999L),
                    shop.novels.id().value(), null, null, "tr", 0, null, Money.of("10"), 1)))
                    .isInstanceOf(NotFoundException.class);
            assertThatThrownBy(() -> shop.admin.createBook(new BookCommand("9789753638029", "Title",
                    List.of(ali.id().value()), shop.novels.id().value(), null, null, "zz", 0, null, Money.of("10"), 1)))
                    .isInstanceOf(ValidationException.class);
            assertThat(shop.admin.books(null, PageRequest.first()).totalElements()).isZero();
        }

        @Test
        void anUpdateChangesEverythingButTheIsbn() {
            BookDetails created = shop.admin.createBook(madonna("9789753638029"));

            BookDetails changed = shop.admin.updateBook(created.id(), new BookCommand("9780000000002", "Madonna in a Fur Coat",
                    List.of(hikmet.id().value(), ali.id().value()), shop.poetry.id().value(), null, null, "en", 0, null,
                    Money.of("120.00"), 3));

            assertThat(changed.isbn()).isEqualTo("9789753638029");
            assertThat(changed.title()).isEqualTo("Madonna in a Fur Coat");
            assertThat(changed.authors()).extracting(BookDetails.AuthorRef::name)
                    .containsExactly("Nâzım Hikmet", "Sabahattin Ali");
            assertThat(changed.categoryPath()).containsExactly("Literature", "Poetry");
            assertThat(changed.language()).isEqualTo("en");
            assertThat(changed.price()).isEqualTo(Money.of("120.00"));
            assertThat(changed.stock()).isEqualTo(3);
        }

        @Test
        void aChangeMadeOnAnOutdatedVersionIsRefused() {
            BookDetails created = shop.admin.createBook(madonna("9789753638029"));
            BookDetails cheaper = shop.admin.changePrice(created.id(), Money.of("90.00"), created.version());

            assertThat(cheaper.version()).isGreaterThan(created.version());
            assertThatThrownBy(() -> shop.admin.changeStock(created.id(), 1, created.version()))
                    .isInstanceOfSatisfying(ConflictException.class,
                            e -> assertThat(e.code()).isEqualTo(ConflictException.CONCURRENT_MODIFICATION));
            assertThat(shop.admin.book(created.id()).stock()).isEqualTo(12);
            assertThat(shop.admin.setActive(created.id(), false, cheaper.version()).active()).isFalse();
        }

        @Test
        void priceAndStockChangeOnTheirOwn() {
            BookDetails created = shop.admin.createBook(madonna("9789753638029"));

            assertThat(shop.admin.changePrice(created.id(), Money.of("99.90")).price()).isEqualTo(Money.of("99.90"));
            assertThat(shop.admin.changeStock(created.id(), 0).inStock()).isFalse();
            assertThatThrownBy(() -> shop.admin.changeStock(created.id(), -1)).isInstanceOf(DomainException.class);
            assertThatThrownBy(() -> shop.admin.changePrice(created.id(), Money.ZERO)).isInstanceOf(DomainException.class);
        }

        @Test
        void aBookOffSaleDisappearsForCustomersButNotForTheAdministrator() {
            BookDetails created = shop.admin.createBook(madonna("9789753638029"));

            assertThat(shop.admin.setActive(created.id(), false).active()).isFalse();

            assertThat(shop.catalog.search(BookSearchQuery.everything(), PageRequest.first()).content()).isEmpty();
            assertThatThrownBy(() -> shop.catalog.book(created.id())).isInstanceOf(NotFoundException.class);
            assertThat(shop.admin.book(created.id()).active()).isFalse();
            assertThat(shop.admin.books("madonna", PageRequest.first()).content())
                    .extracting(AdminBookRow::active).containsExactly(false);

            assertThat(shop.admin.setActive(created.id(), true).active()).isTrue();
            assertThat(shop.catalog.book(created.id()).title()).isEqualTo("Kürk Mantolu Madonna");
        }
    }

    @Nested
    class AuthorsAndCategories {

        @Test
        void authorsAreListedByTurkishAlphabetAndCanBeRenamed() {
            shop.storedAuthor("Çetin Altan");

            assertThat(shop.admin.authors()).extracting(a -> a.name())
                    .containsExactly("Çetin Altan", "Nâzım Hikmet", "Sabahattin Ali");
            assertThat(shop.admin.renameAuthor(ali.id().value(), "S. Ali").name()).isEqualTo("S. Ali");
            assertThat(shop.admin.addAuthor("Orhan Pamuk").id()).isPositive();
        }

        @Test
        void anAuthorWithBooksStays() {
            shop.admin.createBook(madonna("9789753638029"));

            assertThatThrownBy(() -> shop.admin.removeAuthor(ali.id().value()))
                    .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.code()).isEqualTo("author.in-use"));
            shop.admin.removeAuthor(hikmet.id().value());
            assertThat(shop.admin.authors()).extracting(a -> a.name()).containsExactly("Sabahattin Ali");
        }

        @Test
        void categoriesAreAddedRenamedAndRemovedWhenEmpty() {
            CategoryNode art = shop.admin.addCategory("Art", null);
            CategoryNode painting = shop.admin.addCategory("Painting", art.id());

            assertThat(shop.catalog.categoryTree()).extracting(CategoryNode::name)
                    .containsExactlyInAnyOrder("Literature", "Science", "Art");
            assertThat(shop.admin.renameCategory(painting.id(), "Paintings").name()).isEqualTo("Paintings");
            assertThatThrownBy(() -> shop.admin.removeCategory(art.id()))
                    .isInstanceOfSatisfying(ConflictException.class,
                            e -> assertThat(e.code()).isEqualTo("category.has-children"));

            shop.admin.removeCategory(painting.id());
            shop.admin.removeCategory(art.id());
            assertThat(shop.catalog.categoryTree()).extracting(CategoryNode::name)
                    .containsExactlyInAnyOrder("Literature", "Science");
        }

        @Test
        void aCategoryWithBooksStays() {
            shop.admin.createBook(madonna("9789753638029"));

            assertThatThrownBy(() -> shop.admin.removeCategory(shop.novels.id().value()))
                    .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.code()).isEqualTo("category.in-use"));
        }
    }

    @Test
    void aCoverIsStoredServedAndReplaced() {
        BookDetails created = shop.admin.createBook(madonna("9789753638029"));
        assertThat(shop.catalog.cover(created.id())).isEmpty();

        BookDetails withCover = shop.admin.attachCover(created.id(), CatalogTestData.png());
        assertThat(withCover.coverImage()).endsWith(".png");
        assertThat(shop.catalog.cover(created.id())).hasValueSatisfying(cover -> {
            assertThat(cover.mediaType()).isEqualTo("image/png");
            assertThat(cover.content()).isEqualTo(CatalogTestData.png());
        });

        BookDetails replaced = shop.admin.attachCover(created.id(), CatalogTestData.jpeg());
        assertThat(shop.files.keys()).containsExactly(replaced.coverImage());
        assertThat(shop.catalog.cover(created.id()).orElseThrow().mediaType()).isEqualTo("image/jpeg");
    }

    @Test
    void aWrongFileOrAnUnknownBookStoresNothing() {
        BookDetails created = shop.admin.createBook(madonna("9789753638029"));

        assertThatThrownBy(() -> shop.admin.attachCover(created.id(), "GIF89a".getBytes()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> shop.admin.attachCover(BookId.of(999),
                CatalogTestData.png())).isInstanceOf(NotFoundException.class);
        assertThat(shop.files.keys()).isEmpty();
    }

    @Test
    void theOverviewCountsBooksAndListsThoseRunningLowFewestFirst() {
        shop.book("Plenty", "10", 50);
        shop.book("Three left", "10", 3);
        shop.book("Sold out", "10", 0);
        Book hidden = shop.book("Off sale", "10", 1);
        hidden.deactivate();
        shop.books.save(hidden);

        CatalogStats stats = shop.admin.stats(5);

        assertThat(stats.totalBooks()).isEqualTo(4);
        assertThat(stats.activeBooks()).isEqualTo(3);
        assertThat(stats.lowStock()).extracting(AdminBookRow::title).containsExactly("Sold out", "Three left");
    }
}
