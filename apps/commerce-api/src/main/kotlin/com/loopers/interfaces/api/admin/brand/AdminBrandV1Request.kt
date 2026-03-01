package com.loopers.interfaces.api.admin.brand

import com.loopers.application.admin.brand.AdminBrandCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

class AdminBrandV1Request {
    data class Register(
        @field:NotBlank
        val name: String,
    ) {
        fun toCommand(admin: String): AdminBrandCommand.Register =
            AdminBrandCommand.Register(name = name, admin = admin)
    }

    data class Update(
        @field:NotBlank
        val name: String,
        @field:NotBlank
        @field:Pattern(regexp = "ACTIVE|INACTIVE", message = "ACTIVE 또는 INACTIVE만 허용됩니다.")
        val status: String,
    ) {
        fun toCommand(brandId: Long, admin: String): AdminBrandCommand.Update =
            AdminBrandCommand.Update(brandId = brandId, name = name, status = status, admin = admin)
    }
}
