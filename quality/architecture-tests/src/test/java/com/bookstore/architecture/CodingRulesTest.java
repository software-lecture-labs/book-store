package com.bookstore.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Coding conventions (ADR-0005). */
@AnalyzeClasses(packages = "com.bookstore", importOptions = ImportOption.DoNotIncludeTests.class)
class CodingRulesTest {

    /** ADR-0005: nothing writes to the console or dumps stack traces. */
    @ArchTest
    static final ArchRule no_standard_streams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
            .because("ADR-0005: output goes through System.Logger");

    /** ADR-0005: one logging API, System.Logger; java.util.logging is not used directly. */
    @ArchTest
    static final ArchRule no_java_util_logging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
            .because("ADR-0005: System.Logger lets the server choose the backend");

    /** ADR-0005: package names say what is inside; no dumping-ground packages. */
    @ArchTest
    static final ArchRule no_dumping_ground_packages = noClasses()
            .should().resideInAnyPackage("..util..", "..utils..", "..common..", "..helper..", "..helpers..",
                    "..misc..")
            .because("ADR-0005: a package named after no concept collects unrelated code");
}
