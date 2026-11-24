package com.bookstore.shared.kernel.paging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ValidationException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class PageTest {

    private final List<Integer> numbers = IntStream.rangeClosed(1, 45).boxed().toList();

    @Test
    void cutsTheRequestedPageOutOfAList() {
        Page<Integer> page = Page.fromList(numbers, new PageRequest(1, 20));

        assertThat(page.content()).first().isEqualTo(21);
        assertThat(page.content()).hasSize(20);
        assertThat(page.totalElements()).isEqualTo(45);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void lastPageMayBeShortAndHasNoNext() {
        Page<Integer> page = Page.fromList(numbers, new PageRequest(2, 20));

        assertThat(page.content()).containsExactly(41, 42, 43, 44, 45);
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void pageBeyondTheEndIsEmptyButKeepsTheTotal() {
        Page<Integer> page = Page.fromList(numbers, new PageRequest(9, 20));

        assertThat(page.isEmpty()).isTrue();
        assertThat(page.totalElements()).isEqualTo(45);
    }

    @Test
    void emptyResultHasZeroPages() {
        Page<String> page = Page.empty(PageRequest.first());

        assertThat(page.totalPages()).isZero();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isFalse();
    }

    @Test
    void mapKeepsPagingInformation() {
        Page<String> page = Page.fromList(numbers, PageRequest.first()).map(n -> "#" + n);

        assertThat(page.content()).startsWith("#1", "#2");
        assertThat(page.totalElements()).isEqualTo(45);
    }

    @Test
    void contentIsACopy() {
        List<Integer> source = new java.util.ArrayList<>(List.of(1, 2));
        Page<Integer> page = new Page<>(source, PageRequest.first(), 2);
        source.add(3);

        assertThat(page.content()).containsExactly(1, 2);
        assertThatThrownBy(() -> page.content().add(4)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void requestValidatesNumberAndSize() {
        assertThat(new PageRequest(2, 10).offset()).isEqualTo(20);
        assertThat(PageRequest.first().next().number()).isEqualTo(1);

        assertThatThrownBy(() -> new PageRequest(-1, 10)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new PageRequest(0, 0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new PageRequest(0, 101)).isInstanceOf(ValidationException.class);
    }

    @Test
    void sortOrderReversesForDescending() {
        List<Integer> values = List.of(2, 3, 1);

        assertThat(values.stream().sorted(SortOrder.ASC.apply(Comparator.<Integer>naturalOrder())))
                .containsExactly(1, 2, 3);
        assertThat(values.stream().sorted(SortOrder.DESC.apply(Comparator.<Integer>naturalOrder())))
                .containsExactly(3, 2, 1);
    }
}
