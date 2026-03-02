package com.loopers.domain.brand

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "tb_brand")
@SQLRestriction("deleted_at IS NULL")
class BrandModel(
    name: Name,
    logoImageUrl: LogoImageUrl,
    description: Description,
    address: Address,
    email: Email,
    phoneNumber: PhoneNumber,
    businessNumber: BusinessNumber,
) : BaseEntity() {

    @Column(unique = true, nullable = false)
    var name: Name = name
        protected set

    @Column
    var logoImageUrl: LogoImageUrl = logoImageUrl
        protected set

    @Column
    var description: Description = description
        protected set

    @Column
    var zipCode: String = address.zipCode
        protected set

    @Column
    var roadAddress: String = address.roadAddress
        protected set

    @Column
    var detailAddress: String = address.detailAddress
        protected set

    @Column
    var email: Email = email
        protected set

    @Column
    var phoneNumber: PhoneNumber = phoneNumber
        protected set

    @Column(unique = true)
    var businessNumber: BusinessNumber = businessNumber
        protected set

    fun update(
        name: Name,
        logoImageUrl: LogoImageUrl,
        description: Description,
        address: Address,
        email: Email,
        phoneNumber: PhoneNumber,
        businessNumber: BusinessNumber,
    ) {
        this.name = name
        this.logoImageUrl = logoImageUrl
        this.description = description
        this.zipCode = address.zipCode
        this.roadAddress = address.roadAddress
        this.detailAddress = address.detailAddress
        this.email = email
        this.phoneNumber = phoneNumber
        this.businessNumber = businessNumber
    }
}
