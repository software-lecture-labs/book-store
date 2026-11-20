package com.bookstore.review.api;

import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;

import java.time.Instant;

/**
 * A review as shown on a book's page.
 *
 * @param id         the review's id
 * @param bookId     the reviewed book
 * @param customerId the author's id, so a page can mark the viewer's own review
 * @param authorName the author's public name, for example {@code "Ayşe Y."}
 * @param stars      the rating, from 1 to 5
 * @param comment    the comment, or an empty string
 * @param createdAt  when it was written
 * @param edited     whether it was changed after it was written
 */
public record ReviewView(long id, BookId bookId, CustomerId customerId, String authorName, int stars, String comment,
                         Instant createdAt, boolean edited) {
}
