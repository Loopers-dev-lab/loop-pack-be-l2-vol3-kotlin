package com.loopers

import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Email
import com.loopers.domain.user.GenderType
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.Name
import com.loopers.domain.user.Password
import com.loopers.domain.user.User
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ArchitectureTest {

    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("com.loopers")

    @Test
    @DisplayName("interfaces, application 레이어에서 User.reconstitute 호출 금지")
    fun `reconstitute는 infrastructure에서만 호출 가능하다`() {
        noClasses()
            .that().resideInAnyPackage("..interfaces..", "..application..")
            .should().callMethod(
                User.Companion::class.java,
                "reconstitute",
                Long::class.java,
                LoginId::class.java,
                Password::class.java,
                Name::class.java,
                BirthDate::class.java,
                Email::class.java,
                GenderType::class.java,
            )
            .because("reconstitute는 DB 복원 전용이며, infrastructure 레이어에서만 호출해야 한다")
            .check(classes)
    }

    @Test
    @DisplayName("domain 레이어는 다른 레이어에 의존하지 않는다")
    fun `domain은 infrastructure, interfaces, application에 의존하지 않는다`() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..", "..interfaces..", "..application..")
            .because("domain은 핵심 비즈니스 규칙이며, 외부 레이어에 의존하면 안 된다")
            .check(classes)
    }
}
