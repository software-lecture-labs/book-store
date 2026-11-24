package com.bookstore.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.PageRequest;

import org.junit.jupiter.api.Test;

class FavoriteServiceTest {
    private final CustomerShop shop = new CustomerShop();
    private final CustomerId ayse = shop.customer("ayse@example.com");
    private final CustomerId mehmet = shop.customer("mehmet@example.com");

    @Test
    void customerAddsAndListsFavorites() {
        shop.favorites.add(ayse, BookId.of(3));
        shop.favorites.add(ayse, BookId.of(1));

        assertThat(shop.favorites.favorites(ayse, PageRequest.first()).content())
                .containsExactly(BookId.of(1), BookId.of(3));
        assertThat(shop.favorites.favorites(mehmet, PageRequest.first()).content()).isEmpty();
    }

    @Test
    void addingTheSameBookAgainIsAConflict() {
        shop.favorites.add(ayse, BookId.of(3));

        assertThatThrownBy(() -> shop.favorites.add(ayse, BookId.of(3)))
                .isInstanceOfSatisfying(ConflictException.class,
                        error -> assertThat(error.code()).isEqualTo(FavoriteService.FAVORITE_EXISTS));
    }

    @Test
    void customerRemovesAFavorite() {
        shop.favorites.add(ayse, BookId.of(3));

        shop.favorites.remove(ayse, BookId.of(3));

        assertThat(shop.favorites.favorites(ayse, PageRequest.first()).content()).isEmpty();
    }
}
