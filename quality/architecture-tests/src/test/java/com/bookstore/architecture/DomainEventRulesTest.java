package com.bookstore.architecture;

import static com.bookstore.architecture.Layers.APPLICATION;
import static com.bookstore.architecture.Layers.DOMAIN;
import static com.bookstore.architecture.Layers.SHARED_KERNEL_DOMAIN;
import static com.bookstore.architecture.Layers.publishingEvents;
import static com.bookstore.architecture.Layers.pullingEvents;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.bookstore.shared.kernel.event.DomainEvent;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Rules for domain events (ADR-0007). */
@AnalyzeClasses(packages = "com.bookstore", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainEventRulesTest {

    /** ADR-0007: an event is an immutable fact. */
    @ArchTest
    static final ArchRule domain_events_are_records = classes()
            .that().areAssignableTo(DomainEvent.class)
            .and().areNotInterfaces()
            .should().beRecords()
            .because("ADR-0007: an event describes something that happened and never changes");

    /** ADR-0007: events belong to the domain that raises them. */
    @ArchTest
    static final ArchRule domain_events_live_in_the_domain = classes()
            .that().areAssignableTo(DomainEvent.class)
            .should().resideInAnyPackage(DOMAIN, "com.bookstore.shared.kernel.event..")
            .because("ADR-0007: aggregates raise events, so the event types live next to them");

    /** ADR-0007: only use cases pull events, after saving the aggregate. */
    @ArchTest
    static final ArchRule only_application_pulls_events = noClasses()
            .that().resideOutsideOfPackages(APPLICATION, SHARED_KERNEL_DOMAIN)
            .should().callMethodWhere(pullingEvents())
            .because("ADR-0007: events are published only after the aggregate is stored");

    /** ADR-0007: only use cases publish events; aggregates and repositories never do. */
    @ArchTest
    static final ArchRule only_application_publishes_events = noClasses()
            .that().resideOutsideOfPackages(APPLICATION, "com.bookstore.shared.kernel.event..")
            .should().callMethodWhere(publishingEvents())
            .because("ADR-0007: the use case saves the aggregate first, then hands its events to the publisher");
}
