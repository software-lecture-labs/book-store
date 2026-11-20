package com.bookstore.architecture;

import static com.bookstore.architecture.Layers.API;
import static com.bookstore.architecture.Layers.APPLICATION;
import static com.bookstore.architecture.Layers.DOMAIN;
import static com.bookstore.architecture.Layers.PERSISTENCE;
import static com.bookstore.architecture.Layers.SHARED_KERNEL;
import static com.bookstore.architecture.Layers.SHARED_KERNEL_DOMAIN;
import static com.bookstore.architecture.Layers.reconstitution;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.codeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.bookstore.shared.kernel.error.DomainException;

import java.util.Optional;

import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/** Rules for the domain model (ADR-0004). */
@AnalyzeClasses(packages = "com.bookstore", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainModelRulesTest {

    /** ADR-0004: the domain and the shared kernel are plain Java. */
    @ArchTest
    static final ArchRule domain_is_framework_free = noClasses()
            .that().resideInAnyPackage(DOMAIN, SHARED_KERNEL)
            .should().dependOnClassesThat().resideInAnyPackage("jakarta..", "javax..", "java.sql..", "org..",
                    "com.fasterxml..")
            .because("ADR-0004: business rules must run and be tested without a container, a database or a framework");

    /** ADR-0004: state changes through intention-revealing methods, never setters. */
    @ArchTest
    static final ArchRule domain_has_no_setters = noMethods()
            .that().areDeclaredInClassesThat().resideInAnyPackage(DOMAIN, SHARED_KERNEL)
            .should().haveNameMatching("set[A-Z].*")
            .because("ADR-0004: a setter lets callers skip the rule that should guard the change");

    /** ADR-0004: Optional is a return type only, so no field (or record component) holds one. */
    @ArchTest
    static final ArchRule optional_is_not_a_field_type = noFields()
            .that().areDeclaredInClassesThat().resideInAPackage("com.bookstore..")
            .should().haveRawType(Optional.class)
            .because("ADR-0004: Optional is a return type only; keep a nullable value and offer an Optional accessor");

    /** ADR-0004: Optional is a return type only, so no method or constructor takes one. */
    @ArchTest
    static final ArchRule optional_is_not_a_parameter_type = codeUnits()
            .that().areDeclaredInClassesThat().resideInAPackage("com.bookstore..")
            .should(notTakeAnOptional())
            .because("ADR-0004: Optional is a return type only; overload the method or accept null");

    /** ADR-0004: every exception a module throws on purpose shares one base. */
    @ArchTest
    static final ArchRule domain_exceptions_share_one_base = classes()
            .that().resideInAnyPackage(DOMAIN, APPLICATION, API)
            .and().areAssignableTo(Throwable.class)
            .should().beAssignableTo(DomainException.class)
            .because("ADR-0004: the HTTP layer maps one exception hierarchy to status codes");

    /** ADR-0004: only persistence rebuilds stored aggregates. */
    @ArchTest
    static final ArchRule only_persistence_reconstitutes = noClasses()
            .that().resideOutsideOfPackages(PERSISTENCE, DOMAIN, SHARED_KERNEL_DOMAIN)
            .should().callMethodWhere(reconstitution())
            .because("ADR-0004: reconstitution skips the creation rules, so only stored data may use it");

    private static ArchCondition<JavaCodeUnit> notTakeAnOptional() {
        return new ArchCondition<>("not take an Optional parameter") {
            @Override
            public void check(JavaCodeUnit unit, ConditionEvents events) {
                if (unit.getModifiers().contains(JavaModifier.SYNTHETIC)) {
                    return;
                }
                boolean takesOptional = unit.getRawParameterTypes().stream()
                        .anyMatch(type -> type.isEquivalentTo(Optional.class));
                if (takesOptional) {
                    events.add(SimpleConditionEvent.violated(unit,
                            unit.getFullName() + " takes an Optional in " + unit.getSourceCodeLocation()));
                }
            }
        };
    }
}
