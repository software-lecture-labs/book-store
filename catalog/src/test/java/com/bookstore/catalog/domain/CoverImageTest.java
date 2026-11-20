package com.bookstore.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookstore.shared.kernel.error.ValidationException;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class CoverImageTest {

    private static final byte[] PNG = CatalogTestData.png();
    private static final byte[] JPEG = CatalogTestData.jpeg();

    @Test
    void theTypeComesFromTheFirstBytes() {
        assertThat(CoverImage.of(PNG).type()).isEqualTo(CoverImage.Type.PNG);
        assertThat(CoverImage.of(JPEG).type()).isEqualTo(CoverImage.Type.JPEG);
        assertThat(CoverImage.Type.PNG.mediaType()).isEqualTo("image/png");
        assertThat(CoverImage.Type.ofKey("covers/1.JPG")).contains(CoverImage.Type.JPEG);
        assertThat(CoverImage.Type.ofKey("covers/1.gif")).isEmpty();
    }

    @Test
    void otherFilesEmptyFilesAndLargeFilesAreRefused() {
        assertThatThrownBy(() -> CoverImage.of("GIF89a".getBytes())).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> CoverImage.of(new byte[0])).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> CoverImage.of(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new CoverImage(CoverImage.Type.JPEG, PNG)).isInstanceOf(ValidationException.class);

        byte[] large = Arrays.copyOf(PNG, CoverImage.MAX_BYTES + 1);
        assertThatThrownBy(() -> CoverImage.of(large))
                .isInstanceOfSatisfying(ValidationException.class,
                        e -> assertThat(e.violations().getFirst().message()).contains("2 MB"));
        assertThat(CoverImage.of(Arrays.copyOf(PNG, CoverImage.MAX_BYTES)).content()).hasSize(CoverImage.MAX_BYTES);
    }

    @Test
    void theBytesAreCopied() {
        byte[] bytes = PNG.clone();
        CoverImage image = CoverImage.of(bytes);
        bytes[0] = 0;
        image.content()[1] = 0;

        assertThat(image.content()).isEqualTo(PNG);
        assertThat(image).isEqualTo(CoverImage.of(PNG));
    }
}
