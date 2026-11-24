package com.bookstore.customer.domain;

/**
 * What a user is allowed to do.
 */
public enum Role {

    /** Can browse, buy and review. */
    CUSTOMER,

    /** Can additionally manage the catalog, orders, users and reviews. */
    ADMIN
}
