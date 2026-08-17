package com.travel.app

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

// Derives the module list from whatever "com.travel.<module>" packages are actually
// on this module's classpath, so adding a new business module needs zero edits here.
@AnalyzeClasses(packages = ["com.travel"], importOptions = [ImportOption.DoNotIncludeTests::class])
class ModuleBoundaryArchTest {
    @ArchTest
    fun internal_packages_are_only_accessed_from_within_their_own_module(classes: JavaClasses) {
        val modules =
            classes
                .asSequence()
                .map { it.packageName }
                .filter { it.startsWith("com.travel.") }
                .mapNotNull { pkg -> pkg.removePrefix("com.travel.").substringBefore('.').takeIf(String::isNotBlank) }
                .toSet()

        for (module in modules) {
            noClasses()
                .that().resideOutsideOfPackage("com.travel.$module..")
                .should().dependOnClassesThat().resideInAPackage("com.travel.$module.internal..")
                .because("com.travel.$module.internal is private to the $module module")
                .check(classes)
        }
    }
}
