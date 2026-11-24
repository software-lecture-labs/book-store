package com.bookstore.architecture;

import static com.bookstore.architecture.Layers.API;
import static com.bookstore.architecture.Layers.DOMAIN;
import static com.bookstore.architecture.Layers.SHARED_KERNEL;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.bookstore.shared.kernel.event.DomainEvent;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/** Rules for the application server platform (ADR-0010). */
@AnalyzeClasses(packages = "com.bookstore", importOptions = ImportOption.DoNotIncludeTests.class)
class PlatformRulesTest {

    private static final String OBSERVES = "jakarta.enterprise.event.Observes";

    /** ADR-0010: the api contract does not know the platform it runs on. */
    @ArchTest
    static final ArchRule api_knows_no_framework = noClasses()
            .that().resideInAPackage(API)
            .should().dependOnClassesThat().resideInAnyPackage("jakarta..", "javax..", "org.eclipse.microprofile..")
            .because("ADR-0010: the api package is a plain-Java contract");

    /** ADR-0004, ADR-0010: domain and shared kernel carry no CDI or JTA annotations. */
    @ArchTest
    static final ArchRule domain_carries_no_platform_annotations = noClasses()
            .that().resideInAnyPackage(DOMAIN, SHARED_KERNEL)
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.enterprise..", "jakarta.inject..",
                    "jakarta.transaction..")
            .because("ADR-0010: only application, persistence and app classes are beans");

    /**
     * ADR-0010: only the JTA implementation of the TransactionRunner port draws transactions, so the
     * same use case code runs with JDBC transactions in tests (2026-11-17 note).
     */
    @ArchTest
    static final ArchRule only_the_transaction_runner_demarcates_transactions = noClasses()
            .that().doNotHaveFullyQualifiedName("com.bookstore.app.JtaTransactionRunner")
            .should().dependOnClassesThat().resideInAPackage("jakarta.transaction..")
            .because("ADR-0010: the use case is the transaction boundary, drawn through the TransactionRunner port");

    /** ADR-0007, ADR-0010: observers of domain events run only after the transaction committed. */
    @ArchTest
    static final ArchRule domain_event_observers_wait_for_commit = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("com.bookstore..")
            .should(observeDomainEventsOnlyAfterSuccess())
            .because("ADR-0010: a rolled-back change must never reach a customer");

    private static ArchCondition<JavaMethod> observeDomainEventsOnlyAfterSuccess() {
        return new ArchCondition<>("observe domain events only during AFTER_SUCCESS") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (JavaParameter parameter : method.getParameters()) {
                    boolean domainEvent = parameter.getRawType().isAssignableTo(DomainEvent.class);
                    for (JavaAnnotation<JavaParameter> annotation : parameter.getAnnotations()) {
                        if (!domainEvent || !annotation.getRawType().getName().equals(OBSERVES)) {
                            continue;
                        }
                        Object during = annotation.get("during").orElse(null);
                        boolean afterSuccess = during instanceof JavaEnumConstant constant
                                && constant.name().equals("AFTER_SUCCESS");
                        if (!afterSuccess) {
                            events.add(SimpleConditionEvent.violated(method,
                                    method.getFullName() + " observes a domain event before the commit"));
                        }
                    }
                }
            }
        };
    }
}
