package com.bookstore.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Rules that keep the business modules apart (ADR-0002, ADR-0003). Production classes only:
 * tests may build a module's objects directly to set up a scenario.
 */
@AnalyzeClasses(packages = "com.bookstore", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryTest {

    private static final String[] BUSINESS_MODULES = {"catalog", "customer", "sales", "review"};

    /** ADR-0002: the shared kernel belongs to no business module and sees none of them. */
    @ArchTest
    static final ArchRule shared_kernel_depends_on_no_business_module = noClasses()
            .that().resideInAPackage("com.bookstore.shared.kernel..")
            .should().dependOnClassesThat().resideInAnyPackage(businessModulePackages())
            .because("ADR-0002: shared-kernel holds only types that belong to no business module");

    /** ADR-0002: the shared JDBC toolkit is technical and sees no business module either. */
    @ArchTest
    static final ArchRule shared_jdbc_depends_on_no_business_module = noClasses()
            .that().resideInAPackage("com.bookstore.shared.jdbc..")
            .should().dependOnClassesThat().resideInAnyPackage(businessModulePackages())
            .because("ADR-0018: shared-jdbc is a technical module below the business modules");

    /** ADR-0019: the shared web toolkit is technical and sees no business module either. */
    @ArchTest
    static final ArchRule shared_web_depends_on_no_business_module = noClasses()
            .that().resideInAPackage("com.bookstore.shared.web..")
            .should().dependOnClassesThat().resideInAnyPackage(businessModulePackages())
            .because("ADR-0019: shared-web is a technical module below the business modules' rest packages");

    /** ADR-0002: the business modules depend on each other without cycles. */
    @ArchTest
    static final ArchRule business_modules_are_free_of_cycles = slices()
            .matching("com.bookstore.(catalog|customer|sales|review)..")
            .should().beFreeOfCycles()
            .because("ADR-0002: catalog and customer depend on no business module, sales on both, review on all three");

    /** ADR-0003: a business module uses another one only through its api package. */
    @ArchTest
    static final ArchRule catalog_is_used_only_through_its_api = onlyThroughApi("catalog");

    /** ADR-0003: a business module uses another one only through its api package. */
    @ArchTest
    static final ArchRule customer_is_used_only_through_its_api = onlyThroughApi("customer");

    /** ADR-0003: a business module uses another one only through its api package. */
    @ArchTest
    static final ArchRule sales_is_used_only_through_its_api = onlyThroughApi("sales");

    /** ADR-0003: a business module uses another one only through its api package. */
    @ArchTest
    static final ArchRule review_is_used_only_through_its_api = onlyThroughApi("review");

    private static ArchRule onlyThroughApi(String module) {
        String[] others = java.util.Arrays.stream(BUSINESS_MODULES)
                .filter(other -> !other.equals(module))
                .map(other -> "com.bookstore." + other + "..")
                .toArray(String[]::new);
        return noClasses()
                .that().resideInAnyPackage(others)
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.bookstore." + module + ".application..",
                        "com.bookstore." + module + ".domain..",
                        "com.bookstore." + module + ".persistence..")
                .because("ADR-0003: other modules may use " + module + " only through com.bookstore." + module + ".api");
    }

    private static String[] businessModulePackages() {
        return java.util.Arrays.stream(BUSINESS_MODULES).map(m -> "com.bookstore." + m + "..").toArray(String[]::new);
    }
}
