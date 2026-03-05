package com.loopers.support.arch

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture

@AnalyzeClasses(packages = ["com.loopers"], importOptions = [ImportOption.DoNotIncludeTests::class])
class ArchitectureTest {

    @ArchTest
    val layerDependencyRule: ArchRule = layeredArchitecture()
        .consideringAllDependencies()
        .layer("Interfaces").definedBy("com.loopers.interfaces..")
        .layer("Application").definedBy("com.loopers.application..")
        .layer("Domain").definedBy("com.loopers.domain..")
        .layer("Infrastructure").definedBy("com.loopers.infrastructure..")
        .whereLayer("Interfaces").mayNotBeAccessedByAnyLayer()
        .whereLayer("Application").mayOnlyBeAccessedByLayers("Interfaces")
        .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
        .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()

    @ArchTest
    val domainShouldNotDependOnFrameworks: ArchRule = classes()
        .that().resideInAPackage("com.loopers.domain..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage(
            "com.loopers.domain..",
            "com.loopers.support..",
            "java..",
            "kotlin..",
            "org.jetbrains..",
        )
        .because("domain 레이어는 순수 POJO로 유지한다")

    @ArchTest
    val userApplicationShouldNotDependOnAdminClasses: ArchRule = noClasses()
        .that().resideInAPackage("com.loopers.application.user..")
        .should().dependOnClassesThat()
        .haveSimpleNameStartingWith("Admin")
        .because("User application은 Admin 전용 클래스를 참조할 수 없다")

    @ArchTest
    val adminApplicationShouldNotDependOnUserApplication: ArchRule = noClasses()
        .that().resideInAPackage("com.loopers.application.admin..")
        .should().dependOnClassesThat()
        .resideInAPackage("com.loopers.application.user..")
        .because("Admin application은 User application을 참조할 수 없다")

    @ArchTest
    val userApplicationShouldNotDependOnAdminApplication: ArchRule = noClasses()
        .that().resideInAPackage("com.loopers.application.user..")
        .should().dependOnClassesThat()
        .resideInAPackage("com.loopers.application.admin..")
        .because("User application은 Admin application을 참조할 수 없다")
}
