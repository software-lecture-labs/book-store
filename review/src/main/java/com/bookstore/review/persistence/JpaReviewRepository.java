package com.bookstore.review.persistence;

import com.bookstore.review.domain.Rating;
import com.bookstore.review.domain.Review;
import com.bookstore.review.domain.ReviewId;
import com.bookstore.review.domain.ReviewRepository;
import com.bookstore.shared.kernel.error.ConflictException;
import com.bookstore.shared.kernel.identity.BookId;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

/**
 * The review repository on Jakarta Persistence, the second implementation next to
 * {@link JdbcReviewRepository} (ADR-0008, ADR-0023). It passes the same contract tests. JPA takes
 * over the SQL text, the row mapping, the generated key and the version check; the domain model
 * stays as it is, and {@link ReviewEntity} is the separate persistence model.
 *
 * <p>Each call runs in its own resource-local transaction, so the repository does not take part in
 * the transactions of the JDBC repositories. That is why the server keeps the JDBC repository; a
 * container-managed {@code EntityManager} on the same JTA data source would be the next step.
 */
public class JpaReviewRepository implements ReviewRepository {

    private static final String NEWEST_FIRST = " order by r.createdAt desc, r.id desc";

    private final EntityManagerFactory entityManagers;

    /**
     * Creates the repository.
     *
     * @param entityManagers the persistence unit with {@link ReviewEntity}
     */
    public JpaReviewRepository(EntityManagerFactory entityManagers) {
        this.entityManagers = Objects.requireNonNull(entityManagers, "entityManagers");
    }

    @Override
    public Optional<Review> findById(ReviewId id) {
        return inTransaction(em -> Optional.ofNullable(em.find(ReviewEntity.class, id.value())).map(ReviewEntity::toReview));
    }

    @Override
    public Optional<Review> findByBookIdAndCustomerId(BookId bookId, CustomerId customerId) {
        return inTransaction(em -> em.createQuery(
                        "select r from ReviewEntity r where r.bookId = :book and r.userId = :user", ReviewEntity.class)
                .setParameter("book", bookId.value())
                .setParameter("user", customerId.value())
                .getResultStream().findFirst().map(ReviewEntity::toReview));
    }

    @Override
    public Page<Review> findByBookId(BookId bookId, PageRequest page) {
        return inTransaction(em -> {
            long total = em.createQuery("select count(r) from ReviewEntity r where r.bookId = :book", Long.class)
                    .setParameter("book", bookId.value()).getSingleResult();
            TypedQuery<ReviewEntity> query = em.createQuery(
                    "select r from ReviewEntity r where r.bookId = :book" + NEWEST_FIRST, ReviewEntity.class)
                    .setParameter("book", bookId.value());
            return new Page<>(pageOf(query, page), page, total);
        });
    }

    @Override
    public List<Rating> findRatingsByBookId(BookId bookId) {
        return inTransaction(em -> em.createQuery("select r.rating from ReviewEntity r where r.bookId = :book",
                        Integer.class)
                .setParameter("book", bookId.value())
                .getResultStream().map(Rating::of).toList());
    }

    @Override
    public Page<Review> findAll(PageRequest page) {
        return inTransaction(em -> {
            long total = em.createQuery("select count(r) from ReviewEntity r", Long.class).getSingleResult();
            TypedQuery<ReviewEntity> query = em.createQuery("select r from ReviewEntity r" + NEWEST_FIRST,
                    ReviewEntity.class);
            return new Page<>(pageOf(query, page), page, total);
        });
    }

    /**
     * Stores a review. A new one is persisted; a stored one is merged with the version it was read
     * with, so JPA refuses it if someone else changed the row in between.
     *
     * @throws ConflictException {@code review.duplicate} for a second review of the same book by the
     *                           same customer; {@link ConflictException#CONCURRENT_MODIFICATION} for an
     *                           outdated copy
     */
    @Override
    public Review save(Review review) {
        try {
            ReviewEntity stored = inTransaction(em -> {
                if (review.isNew()) {
                    ReviewEntity entity = ReviewEntity.of(review);
                    em.persist(entity);
                    em.flush();
                    return entity;
                }
                ReviewEntity current = em.find(ReviewEntity.class, review.id().value());
                if (current == null || current.version() != review.version()) {
                    throw new OptimisticLockException("Review " + review.id() + " was changed or deleted");
                }
                current.copyChangeable(review);
                em.flush();
                return current;
            });
            review.markPersisted(ReviewId.of(stored.id()), stored.version());
            return review;
        } catch (OptimisticLockException e) {
            throw new ConflictException(ConflictException.CONCURRENT_MODIFICATION,
                    "Review " + review.id() + " was changed or deleted by someone else");
        } catch (PersistenceException e) {
            if (violates(e, "uq_reviews_user_book")) {
                throw new ConflictException("review.duplicate", "The customer has already reviewed this book");
            }
            throw e;
        }
    }

    @Override
    public void delete(Review review) {
        inTransaction(em -> em.createQuery("delete from ReviewEntity r where r.id = :id")
                .setParameter("id", review.id().value()).executeUpdate());
    }

    private static List<Review> pageOf(TypedQuery<ReviewEntity> query, PageRequest page) {
        return query.setFirstResult(Math.toIntExact(page.offset()))
                .setMaxResults(page.size())
                .getResultStream().map(ReviewEntity::toReview).toList();
    }

    /** Runs work in a new entity manager and resource-local transaction, rolled back on failure. */
    private <T> T inTransaction(Function<EntityManager, T> work) {
        try (EntityManager em = entityManagers.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            try {
                T result = work.apply(em);
                transaction.commit();
                return result;
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        }
    }

    /** Both PostgreSQL and H2 name the violated constraint somewhere in the cause chain. */
    private static boolean violates(Throwable failure, String constraint) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().toLowerCase(Locale.ROOT).contains(constraint)) {
                return true;
            }
        }
        return false;
    }
}
