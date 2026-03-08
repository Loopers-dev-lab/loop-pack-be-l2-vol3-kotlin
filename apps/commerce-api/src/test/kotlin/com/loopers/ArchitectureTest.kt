package com.loopers

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandName
import com.loopers.domain.brand.BrandStatus
import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponName
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.UserCoupon
import com.loopers.domain.like.Like
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.product.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductImage
import com.loopers.domain.product.ProductName
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.product.Stock
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
import java.time.ZonedDateTime

class ArchitectureTest {

    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("com.loopers")

    @Test
    @DisplayName("interfaces, application 레이어에서 User.reconstitute 호출 금지")
    fun `User reconstitute는 infrastructure에서만 호출 가능하다`() {
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
    @DisplayName("interfaces, application 레이어에서 Brand.reconstitute 호출 금지")
    fun `Brand reconstitute는 infrastructure에서만 호출 가능하다`() {
        noClasses()
            .that().resideInAnyPackage("..interfaces..", "..application..")
            .should().callMethod(
                Brand.Companion::class.java,
                "reconstitute",
                Long::class.java,
                BrandName::class.java,
                String::class.java,
                String::class.java,
                BrandStatus::class.java,
                ZonedDateTime::class.java,
            )
            .because("reconstitute는 DB 복원 전용이며, infrastructure 레이어에서만 호출해야 한다")
            .check(classes)
    }

    @Test
    @DisplayName("interfaces, application 레이어에서 Product.reconstitute 호출 금지")
    fun `Product reconstitute는 infrastructure에서만 호출 가능하다`() {
        noClasses()
            .that().resideInAnyPackage("..interfaces..", "..application..")
            .should().callMethod(
                Product.Companion::class.java,
                "reconstitute",
                Long::class.java,
                Long::class.java,
                ProductName::class.java,
                String::class.java,
                Money::class.java,
                Stock::class.java,
                String::class.java,
                ProductStatus::class.java,
                Int::class.java,
                ZonedDateTime::class.java,
                List::class.java,
            )
            .because("reconstitute는 DB 복원 전용이며, infrastructure 레이어에서만 호출해야 한다")
            .check(classes)
    }

    @Test
    @DisplayName("interfaces, application 레이어에서 ProductImage.reconstitute 호출 금지")
    fun `ProductImage reconstitute는 infrastructure에서만 호출 가능하다`() {
        noClasses()
            .that().resideInAnyPackage("..interfaces..", "..application..")
            .should().callMethod(
                ProductImage.Companion::class.java,
                "reconstitute",
                Long::class.java,
                String::class.java,
                Int::class.java,
            )
            .because("reconstitute는 DB 복원 전용이며, infrastructure 레이어에서만 호출해야 한다")
            .check(classes)
    }

    @Test
    @DisplayName("interfaces, application 레이어에서 Like.reconstitute 호출 금지")
    fun `Like reconstitute는 infrastructure에서만 호출 가능하다`() {
        noClasses()
            .that().resideInAnyPackage("..interfaces..", "..application..")
            .should().callMethod(
                Like.Companion::class.java,
                "reconstitute",
                Long::class.java,
                Long::class.java,
                Long::class.java,
                ZonedDateTime::class.java,
            )
            .because("reconstitute는 DB 복원 전용이며, infrastructure 레이어에서만 호출해야 한다")
            .check(classes)
    }

    @Test
    @DisplayName("interfaces, application 레이어에서 Order.reconstitute 호출 금지")
    fun `Order reconstitute는 infrastructure에서만 호출 가능하다`() {
        noClasses()
            .that().resideInAnyPackage("..interfaces..", "..application..")
            .should().callMethod(
                Order.Companion::class.java,
                "reconstitute",
                Long::class.java,
                Long::class.java,
                Long::class.javaObjectType,
                OrderStatus::class.java,
                Money::class.java,
                Money::class.java,
                Money::class.java,
                ZonedDateTime::class.java,
                List::class.java,
            )
            .because("reconstitute는 DB 복원 전용이며, infrastructure 레이어에서만 호출해야 한다")
            .check(classes)
    }

    @Test
    @DisplayName("interfaces, application 레이어에서 OrderItem.reconstitute 호출 금지")
    fun `OrderItem reconstitute는 infrastructure에서만 호출 가능하다`() {
        noClasses()
            .that().resideInAnyPackage("..interfaces..", "..application..")
            .should().callMethod(
                OrderItem.Companion::class.java,
                "reconstitute",
                Long::class.java,
                Long::class.java,
                String::class.java,
                String::class.java,
                Money::class.java,
                Int::class.java,
            )
            .because("reconstitute는 DB 복원 전용이며, infrastructure 레이어에서만 호출해야 한다")
            .check(classes)
    }

    @Test
    @DisplayName("interfaces, application 레이어에서 Coupon.reconstitute 호출 금지")
    fun `Coupon reconstitute는 infrastructure에서만 호출 가능하다`() {
        noClasses()
            .that().resideInAnyPackage("..interfaces..", "..application..")
            .should().callMethod(
                Coupon.Companion::class.java,
                "reconstitute",
                Long::class.java,
                CouponName::class.java,
                DiscountType::class.java,
                Long::class.java,
                Money::class.java,
                Int::class.javaObjectType,
                Int::class.java,
                ZonedDateTime::class.java,
                ZonedDateTime::class.java,
            )
            .because("reconstitute는 DB 복원 전용이며, infrastructure 레이어에서만 호출해야 한다")
            .check(classes)
    }

    @Test
    @DisplayName("interfaces, application 레이어에서 UserCoupon.reconstitute 호출 금지")
    fun `UserCoupon reconstitute는 infrastructure에서만 호출 가능하다`() {
        noClasses()
            .that().resideInAnyPackage("..interfaces..", "..application..")
            .should().callMethod(
                UserCoupon.Companion::class.java,
                "reconstitute",
                Long::class.java,
                Long::class.java,
                Long::class.java,
                CouponStatus::class.java,
                DiscountType::class.java,
                Long::class.java,
                Money::class.java,
                ZonedDateTime::class.java,
                ZonedDateTime::class.java,
                ZonedDateTime::class.java,
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
