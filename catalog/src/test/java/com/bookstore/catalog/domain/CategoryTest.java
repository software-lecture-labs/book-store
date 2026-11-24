package com.bookstore.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ValidationException;

import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    void subcategoryIsLinkedInBothDirections() {
        Category literature = Category.root("Literature");
        Category novel = literature.addSubcategory("Novel");

        assertThat(literature.isRoot()).isTrue();
        assertThat(literature.children()).containsExactly(novel);
        assertThat(novel.parent()).containsSame(literature);
        assertThat(novel.depth()).isEqualTo(2);
        assertThat(novel.path()).containsExactly("Literature", "Novel");
    }

    @Test
    void treeIsLimitedToTwoLevels() {
        Category novel = Category.root("Literature").addSubcategory("Novel");

        assertThatThrownBy(() -> novel.addSubcategory("Historical Novel"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("2 levels");
    }

    @Test
    void siblingNamesMustBeUnique() {
        Category literature = Category.root("Literature");
        literature.addSubcategory("Novel");

        assertThatThrownBy(() -> literature.addSubcategory("  novel "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void selfAndDescendantsVisitsTheRootFirst() {
        Category literature = Category.root("Literature");
        Category novel = literature.addSubcategory("Novel");
        Category poetry = literature.addSubcategory("Poetry");

        assertThat(literature.selfAndDescendants()).containsExactly(literature, novel, poetry);
        assertThat(novel.selfAndDescendants()).containsExactly(novel);
    }

    @Test
    void restoringAChildAttachesItToTheParent() {
        Category literature = Category.reconstituteRoot(CategoryId.of(1), 0, "Literature");
        Category novel = Category.reconstituteChild(CategoryId.of(2), 0, "Novel", literature);

        assertThat(literature.children()).containsExactly(novel);
        assertThat(literature.children()).isUnmodifiable();
        assertThat(novel).hasToString("Category[Literature > Novel]");
    }
}
