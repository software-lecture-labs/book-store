package com.bookstore.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.catalog.api.BookDetails;
import com.bookstore.catalog.api.BookSearchQuery;
import com.bookstore.catalog.api.BookSummary;
import com.bookstore.catalog.api.CategoryNode;
import com.bookstore.catalog.domain.Author;
import com.bookstore.catalog.domain.Book;
import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.money.Money;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;
import com.bookstore.shared.kernel.paging.SortOrder;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class CatalogServiceTest {

    private final CatalogShop shop = new CatalogShop();
    private final Author ali = shop.author("Sabahattin Ali");
    private final Book madonna = shop.book("Kürk Mantolu Madonna", "120", 5, shop.novels, ali);
    private final Book kuyucakli = shop.book("Kuyucaklı Yusuf", "95", 0, shop.novels, ali);
    private final Book poems = shop.book("Bütün Şiirleri", "60", 3, shop.poetry, shop.author("Nazım Hikmet"));
    private final Book cosmos = shop.book("Cosmos", "200", 4, shop.science, shop.author("Carl Sagan"));

    @Test
    void listsEveryActiveBookByTitle() {
        Page<BookSummary> page = shop.catalog.search(BookSearchQuery.everything(), PageRequest.first());

        assertThat(titles(page)).containsExactly("Bütün Şiirleri", "Cosmos", "Kürk Mantolu Madonna", "Kuyucaklı Yusuf");
        assertThat(page.totalElements()).isEqualTo(4);
    }

    @Test
    void hidesDeactivatedBooks() {
        cosmos.deactivate();
        shop.books.save(cosmos);

        assertThat(titles(shop.catalog.search(BookSearchQuery.everything(), PageRequest.first())))
                .doesNotContain("Cosmos");
        assertThatThrownBy(() -> shop.catalog.book(cosmos.id())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void parentCategoryIncludesItsSubcategories() {
        BookSearchQuery literature = BookSearchQuery.everything().inCategory(shop.literature.id().value());
        BookSearchQuery poetry = BookSearchQuery.everything().inCategory(shop.poetry.id().value());

        assertThat(titles(shop.catalog.search(literature, PageRequest.first())))
                .containsExactlyInAnyOrder("Kürk Mantolu Madonna", "Kuyucaklı Yusuf", "Bütün Şiirleri");
        assertThat(titles(shop.catalog.search(poetry, PageRequest.first()))).containsExactly("Bütün Şiirleri");
    }

    @Test
    void unknownCategoryIsReported() {
        assertThatThrownBy(() -> shop.catalog.search(BookSearchQuery.everything().inCategory(999), PageRequest.first()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listsAnAuthorsBooks() {
        BookSearchQuery query = BookSearchQuery.everything().byAuthor(ali.id().value());

        assertThat(titles(shop.catalog.search(query, PageRequest.first())))
                .containsExactly("Kürk Mantolu Madonna", "Kuyucaklı Yusuf");
    }

    @Test
    void combinesTextFiltersAndSorting() {
        BookSearchQuery query = BookSearchQuery.everything()
                .withText("SABAHATTIN")
                .onlyInStock();
        BookSearchQuery cheapFirst = BookSearchQuery.everything()
                .priced(Money.of("50"), Money.of("150"))
                .sortedBy("PRICE", SortOrder.ASC);

        assertThat(titles(shop.catalog.search(query, PageRequest.first()))).containsExactly("Kürk Mantolu Madonna");
        assertThat(titles(shop.catalog.search(cheapFirst, PageRequest.first())))
                .containsExactly("Bütün Şiirleri", "Kuyucaklı Yusuf", "Kürk Mantolu Madonna");
    }

    @Test
    void pagesThroughResults() {
        Page<BookSummary> second = shop.catalog.search(BookSearchQuery.everything(), new PageRequest(1, 3));

        assertThat(titles(second)).containsExactly("Kuyucaklı Yusuf");
        assertThat(second.totalPages()).isEqualTo(2);
        assertThat(second.hasPrevious()).isTrue();
    }

    @Test
    void summaryShowsStockAndRating() {
        madonna.addRating(5);
        madonna.addRating(4);
        shop.books.save(madonna);

        List<BookSummary> found = shop.catalog.search(BookSearchQuery.everything().byAuthor(ali.id().value()),
                PageRequest.first()).content();

        BookSummary madonnaSummary = found.get(0);
        assertThat(madonnaSummary.title()).isEqualTo("Kürk Mantolu Madonna");
        assertThat(madonnaSummary.inStock()).isTrue();
        assertThat(madonnaSummary.averageRating()).isEqualTo(new BigDecimal("4.5"));
        assertThat(madonnaSummary.ratingCount()).isEqualTo(2);
        assertThat(found.get(1).inStock()).as("Kuyucaklı Yusuf has no stock").isFalse();
    }

    @Test
    void detailPageHasBreadcrumbsAuthorsAndDistribution() {
        BookDetails details = shop.catalog.book(madonna.id());

        assertThat(details.title()).isEqualTo("Kürk Mantolu Madonna");
        assertThat(details.categoryPath()).containsExactly("Literature", "Novel");
        assertThat(details.authors()).containsExactly(new BookDetails.AuthorRef(ali.id().value(), "Sabahattin Ali"));
        assertThat(details.stock()).isEqualTo(5);
    }

    @Test
    void unknownBookIsReported() {
        assertThatThrownBy(() -> shop.catalog.book(BookId.of(999))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void categoryTreeIsCopiedForTheMenu() {
        List<CategoryNode> tree = shop.catalog.categoryTree();

        assertThat(tree).extracting(CategoryNode::name).containsExactly("Literature", "Science");
        assertThat(tree.getFirst().children()).extracting(CategoryNode::name).containsExactly("Novel", "Poetry");
    }

    private static List<String> titles(Page<BookSummary> page) {
        return page.content().stream().map(BookSummary::title).toList();
    }
}
