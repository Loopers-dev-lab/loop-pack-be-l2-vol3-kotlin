package com.loopers

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

@Suppress("ktlint:standard:property-naming")
@AnalyzeClasses(packages = ["com.loopers"], importOptions = [ImportOption.DoNotIncludeTests::class])
class ArchitectureTest {

    @ArchTest
    val `domain은 infrastructure에 의존하지 않는다`: ArchRule = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("..infrastructure..")

    @ArchTest
    val `domain은 interfaces에 의존하지 않는다`: ArchRule = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("..interfaces..")

    @ArchTest
    val `domain은 application에 의존하지 않는다`: ArchRule = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("..application..")

    @ArchTest
    val `application은 infrastructure에 의존하지 않는다`: ArchRule = noClasses()
        .that().resideInAPackage("..application..")
        .should().dependOnClassesThat()
        .resideInAPackage("..infrastructure..")

    @ArchTest
    val `application은 interfaces에 의존하지 않는다`: ArchRule = noClasses()
        .that().resideInAPackage("..application..")
        .should().dependOnClassesThat()
        .resideInAPackage("..interfaces..")

    @ArchTest
    val `infrastructure는 interfaces에 의존하지 않는다`: ArchRule = noClasses()
        .that().resideInAPackage("..infrastructure..")
        .should().dependOnClassesThat()
        .resideInAPackage("..interfaces..")

    @ArchTest
    val `support는 application에 의존하지 않는다`: ArchRule = noClasses()
        .that().resideInAPackage("..support..")
        .should().dependOnClassesThat()
        .resideInAPackage("..application..")
}
