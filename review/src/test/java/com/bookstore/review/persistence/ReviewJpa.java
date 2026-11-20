package com.bookstore.review.persistence;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceUnitTransactionType;

/**
 * Builds the review module's persistence unit for tests, in code and without {@code persistence.xml}
 * (Jakarta Persistence 3.2). Hibernate checks the mapping against the tables Flyway made, so a
 * mismatch between {@link ReviewEntity} and the migration breaks the tests (ADR-0023).
 */
public final class ReviewJpa {

    private ReviewJpa() {
    }

    /**
     * Opens the persistence unit on a database.
     *
     * @param dataSource a database with the review module's tables
     * @return the entity manager factory
     */
    public static EntityManagerFactory entityManagerFactory(DataSource dataSource) {
        return new PersistenceConfiguration("review")
                .transactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL)
                .managedClass(ReviewEntity.class)
                .property("jakarta.persistence.nonJtaDataSource", dataSource)
                .property("hibernate.hbm2ddl.auto", "validate")
                .createEntityManagerFactory();
    }
}
