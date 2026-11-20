package com.bookstore.architecture;

import static com.bookstore.architecture.Layers.API;
import static com.bookstore.architecture.Layers.APP;
import static com.bookstore.architecture.Layers.APPLICATION;
import static com.bookstore.architecture.Layers.DOMAIN;
import static com.bookstore.architecture.Layers.PERSISTENCE;
import static com.bookstore.architecture.Layers.SHARED_JDBC;
import static com.bookstore.architecture.Layers.transactionControl;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import java.sql.SQLException;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Rules for data access (ADR-0008). */
@AnalyzeClasses(packages = "com.bookstore", importOptions = ImportOption.DoNotIncludeTests.class)
class PersistenceRulesTest {

    /** ADR-0008: JDBC types stay inside persistence, the JDBC toolkit and the composition root. */
    @ArchTest
    static final ArchRule only_persistence_touches_jdbc = noClasses()
            .that().resideOutsideOfPackages(PERSISTENCE, SHARED_JDBC, APP)
            .should().dependOnClassesThat().resideInAnyPackage("java.sql..", "javax.sql..")
            .because("ADR-0008: the rest of the code sees repositories, not connections and result sets");

    /** ADR-0008: repository ports do not leak SQLException. */
    @ArchTest
    static final ArchRule repository_contracts_hide_sql_exceptions = noMethods()
            .that().areDeclaredInClassesThat().resideInAnyPackage(DOMAIN, APPLICATION, API)
            .should().declareThrowableOfType(SQLException.class)
            .because("ADR-0008: a port must not tell its callers which storage is behind it");

    /** ADR-0008: the caller owns the transaction; repositories never commit or roll back. */
    @ArchTest
    static final ArchRule repositories_leave_commit_to_the_caller = noClasses()
            .that().doNotHaveFullyQualifiedName("com.bookstore.shared.jdbc.JdbcTransactionRunner")
            .should().callMethodWhere(transactionControl())
            .because("ADR-0008: one use case is one transaction, started and ended by the TransactionRunner");
}
