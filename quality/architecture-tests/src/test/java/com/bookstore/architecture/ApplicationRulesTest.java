package com.bookstore.architecture;

import static com.bookstore.architecture.Layers.APPLICATION;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Rules for use cases and their collaborators (ADR-0006). */
@AnalyzeClasses(packages = "com.bookstore", importOptions = ImportOption.DoNotIncludeTests.class)
class ApplicationRulesTest {

    /** ADR-0006: collaborators come through the constructor and never change afterwards. */
    @ArchTest
    static final ArchRule application_collaborators_are_private_final = fields()
            .that().areDeclaredInClassesThat().resideInAPackage(APPLICATION)
            .and().areNotStatic()
            .should().bePrivate()
            .andShould().beFinal()
            .because("ADR-0006: constructor injection into private final fields");

    /** ADR-0006: time comes from an injected Clock, so tests can fix it. */
    @ArchTest
    static final ArchRule no_hidden_clock = noClasses()
            .should().callMethod(Instant.class, "now")
            .orShould().callMethod(LocalDate.class, "now")
            .orShould().callMethod(LocalDateTime.class, "now")
            .orShould().callMethod(LocalTime.class, "now")
            .orShould().callMethod(OffsetDateTime.class, "now")
            .orShould().callMethod(ZonedDateTime.class, "now")
            .orShould().callMethod(System.class, "currentTimeMillis")
            .because("ADR-0006: read the time from the injected Clock, e.g. Instant.now(clock)");
}
