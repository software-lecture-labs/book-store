package com.bookstore.customer.api;

import com.bookstore.shared.kernel.error.NotFoundException;
import com.bookstore.shared.kernel.error.RuleViolationException;
import com.bookstore.shared.kernel.identity.CustomerId;
import com.bookstore.shared.kernel.paging.Page;
import com.bookstore.shared.kernel.paging.PageRequest;

/**
 * Managing user accounts (ADM-6). Callers check that only administrators reach it (ACC-6).
 */
public interface CustomerAdminFacade {

    /**
     * Lists every account, newest first.
     *
     * @param page which page
     * @return the accounts
     */
    Page<CustomerSummary> customers(PageRequest page);

    /**
     * Lets an account sign in again or stops it from signing in (ACC-7). An open session of a
     * stopped account ends with its next request.
     *
     * @param customerId the account
     * @param active     whether it may sign in
     * @return the changed account
     * @throws NotFoundException      if there is no such account
     * @throws RuleViolationException {@code customer.admin-protected} for an administrator's account
     */
    CustomerSummary setActive(CustomerId customerId, boolean active);
}
