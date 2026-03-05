package com.loopers.interfaces.api.admin.brand

import com.loopers.application.admin.brand.AdminBrandResult

class AdminBrandV1Response {
    data class Register(
        val id: Long,
        val name: String,
        val status: String,
    ) {
        companion object {
            fun from(result: AdminBrandResult.Register): Register =
                Register(id = result.id, name = result.name, status = result.status)
        }
    }

    data class Update(
        val id: Long,
        val name: String,
        val status: String,
    ) {
        companion object {
            fun from(result: AdminBrandResult.Update): Update =
                Update(id = result.id, name = result.name, status = result.status)
        }
    }

    data class Detail(
        val id: Long,
        val name: String,
        val status: String,
    ) {
        companion object {
            fun from(result: AdminBrandResult.Detail): Detail =
                Detail(id = result.id, name = result.name, status = result.status)
        }
    }

    data class Summary(
        val id: Long,
        val name: String,
        val status: String,
    ) {
        companion object {
            fun from(result: AdminBrandResult.Summary): Summary =
                Summary(id = result.id, name = result.name, status = result.status)
        }
    }
}
