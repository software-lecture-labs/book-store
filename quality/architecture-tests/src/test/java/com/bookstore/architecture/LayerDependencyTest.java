package com.bookstore.architecture;

import static com.bookstore.architecture.Layers.API;
import static com.bookstore.architecture.Layers.APP;
import static com.bookstore.architecture.Layers.APPLICATION;
import static com.bookstore.architecture.Layers.DOMAIN;
import static com.bookstore.architecture.Layers.PERSISTENCE;
import static com.bookstore.architecture.Layers.REST;
import static com.bookstore.architecture.Layers.SHARED_JDBC;
import static com.bookstore.architecture.Layers.SHARED_KERNEL;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The package matrix inside each business module (ADR-0003): which of {@code api},
 * {@code application}, {@code domain}, {@code persistence} and {@code rest} may see which.
 */
@AnalyzeClasses(packages = "com.bookstore", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyTest {

    /** ADR-0003: domain sees only the JDK, the shared kernel and domain packages. */
    @ArchTest
    static final ArchRule domain_does_not_see_other_layers = classes()
            .that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", SHARED_KERNEL, DOMAIN)
            .because("ADR-0003: the domain is the centre; api, application, persistence and rest depend on it, "
                    + "never the other way round");

    /** ADR-0003: api is a contract that other modules and the web layer can use on its own. */
    @ArchTest
    static final ArchRule api_is_a_pure_contract = noClasses()
            .that().resideInAPackage(API)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, APPLICATION, PERSISTENCE, REST, SHARED_JDBC)
            .because("ADR-0003: api carries only commands, DTOs and facade interfaces");

    /** ADR-0003: a façade is part of a module's contract, so it lives in the api package. */
    @ArchTest
    static final ArchRule facades_live_in_api = classes()
            .that().haveSimpleNameEndingWith("Facade")
            .should().resideInAPackage(API)
            .andShould().beInterfaces()
            .because("ADR-0003: other modules and the web layer call a module only through its api façades");

    /** ADR-0003, ADR-0006: application knows ports and contracts, not their implementations. */
    @ArchTest
    static final ArchRule application_depends_on_contracts_not_implementations = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE, REST, SHARED_JDBC, "java.sql..",
                    "javax.sql..")
            .orShould().dependOnClassesThat().haveSimpleNameStartingWith("Jdbc")
            .orShould().dependOnClassesThat().haveSimpleNameStartingWith("InMemory")
            .because("ADR-0006: use cases get their collaborators as interfaces through the constructor");

    /** ADR-0003: persistence implements domain ports and sees nothing above the domain. */
    @ArchTest
    static final ArchRule persistence_only_sees_domain = noClasses()
            .that().resideInAPackage(PERSISTENCE)
            .should().dependOnClassesThat().resideInAnyPackage(API, APPLICATION, REST)
            .because("ADR-0003: persistence is an adapter for the domain's repository ports");

    /** ADR-0003: rest translates HTTP into calls on the api contract. Empty until Phase 5. */
    @ArchTest
    static final ArchRule rest_sees_only_the_contract = noClasses()
            .that().resideInAPackage(REST)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, APPLICATION, PERSISTENCE, SHARED_JDBC)
            .allowEmptyShould(true)
            .because("ADR-0003, ADR-0019: rest uses only api and shared-web, so the web layer cannot reach around the use cases");

    /** ADR-0009, ADR-0019: HTTP, JSON and Bean Validation libraries stay at the HTTP boundary. */
    @ArchTest
    static final ArchRule web_libraries_stay_in_rest = noClasses()
            .that().resideOutsideOfPackages(REST, "com.bookstore.shared.web..", APP)
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.ws.rs..", "jakarta.json..",
                    "jakarta.validation..", "com.sun.net.httpserver..", "com.fasterxml.jackson..", "org.hibernate.validator..")
            .because("ADR-0009, ADR-0010: only the rest packages, shared-web and the JAX-RS application know they speak HTTP and JSON");

    /** ADR-0003: no business package depends on rest; only the composition root may wire it. */
    @ArchTest
    static final ArchRule nothing_depends_on_rest = noClasses()
            .that().resideOutsideOfPackages(REST, APP)
            .should().dependOnClassesThat().resideInAPackage(REST)
            .because("ADR-0003: rest is the outermost layer");
}
