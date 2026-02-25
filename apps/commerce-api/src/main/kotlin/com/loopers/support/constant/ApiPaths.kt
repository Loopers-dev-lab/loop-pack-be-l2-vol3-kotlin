package com.loopers.support.constant

object ApiPaths {

    object Users {
        const val BASE = "/api/v1/users"
        const val REGISTER = BASE
        const val ME = "$BASE/me"
        const val ME_PASSWORD = "$BASE/me/password"
    }

    object Brands {
        const val BASE = "/api/v1/brands"
        const val BY_ID = "$BASE/{brandId}"
    }

    object AdminBrands {
        const val BASE = "/api/admin/v1/brands"
        const val BY_ID = "$BASE/{brandId}"
    }

    object Examples {
        const val BASE = "/api/v1/examples"
        const val BY_ID = "$BASE/{id}"
    }
}
