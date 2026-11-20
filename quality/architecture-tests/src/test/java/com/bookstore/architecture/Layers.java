package com.bookstore.architecture;

import com.bookstore.shared.kernel.domain.Entity;
import com.bookstore.shared.kernel.event.DomainEventPublisher;

import java.sql.Connection;
import java.util.Set;
import java.util.regex.Pattern;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethodCall;

/**
 * Package patterns and call predicates shared by the rule classes. A business module's layer is
 * {@code com.bookstore.<module>.<layer>}; {@code *} matches one package name, {@code ..} any
 * number of them.
 */
final class Layers {

    static final String API = "com.bookstore.*.api..";
    static final String APPLICATION = "com.bookstore.*.application..";
    static final String DOMAIN = "com.bookstore.*.domain..";
    static final String PERSISTENCE = "com.bookstore.*.persistence..";
    static final String REST = "com.bookstore.*.rest..";

    static final String SHARED_KERNEL = "com.bookstore.shared.kernel..";
    static final String SHARED_KERNEL_DOMAIN = "com.bookstore.shared.kernel.domain..";
    static final String SHARED_JDBC = "com.bookstore.shared.jdbc..";
    static final String APP = "com.bookstore.app..";

    /** A domain package of a business module, or the shared kernel's entity base. */
    private static final Pattern DOMAIN_PACKAGE =
            Pattern.compile("com\\.bookstore\\.(\\w+\\.domain|shared\\.kernel\\.domain)(\\..*)?");

    private static final Set<String> RECONSTITUTING_METHODS = Set.of("markPersisted", "persisted");

    private static final Set<String> TRANSACTION_CONTROL =
            Set.of("commit", "rollback", "setAutoCommit", "setSavepoint", "releaseSavepoint");

    private Layers() {
    }

    /**
     * Calls that rebuild a stored aggregate: {@code reconstitute…} factories,
     * {@code Entity.markPersisted} and the builders' {@code persisted(id, version)}.
     *
     * @return the predicate
     */
    static DescribedPredicate<JavaMethodCall> reconstitution() {
        return DescribedPredicate.describe("a method that rebuilds a stored aggregate", call -> {
            String name = call.getName();
            boolean reconstituting = name.startsWith("reconstitute")
                    || RECONSTITUTING_METHODS.contains(name);
            return reconstituting && DOMAIN_PACKAGE.matcher(call.getTargetOwner().getPackageName()).matches();
        });
    }

    /**
     * Calls to {@code pullEvents()} on an entity.
     *
     * @return the predicate
     */
    static DescribedPredicate<JavaMethodCall> pullingEvents() {
        return DescribedPredicate.describe("Entity.pullEvents()", call -> call.getName().equals("pullEvents")
                && call.getTargetOwner().isAssignableTo(Entity.class));
    }

    /**
     * Calls to {@code publish} or {@code publishAll} on a domain event publisher.
     *
     * @return the predicate
     */
    static DescribedPredicate<JavaMethodCall> publishingEvents() {
        return DescribedPredicate.describe("DomainEventPublisher.publish/publishAll", call ->
                (call.getName().equals("publish") || call.getName().equals("publishAll"))
                        && call.getTargetOwner().isAssignableTo(DomainEventPublisher.class));
    }

    /**
     * Calls that end or shape a JDBC transaction.
     *
     * @return the predicate
     */
    static DescribedPredicate<JavaMethodCall> transactionControl() {
        return DescribedPredicate.describe("Connection.commit/rollback/setAutoCommit", call ->
                TRANSACTION_CONTROL.contains(call.getName())
                        && call.getTargetOwner().isAssignableTo(Connection.class));
    }
}
