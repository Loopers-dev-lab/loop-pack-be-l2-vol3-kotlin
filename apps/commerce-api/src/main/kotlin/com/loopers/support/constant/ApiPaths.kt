package com.loopers.support.constant

object ApiPaths {

    object Users {
        const val BASE = "/api/v1/users"
        const val REGISTER = BASE
        const val ME = "$BASE/me"
        const val ME_PASSWORD = "$BASE/me/password"
        const val ME_COUPONS = "$BASE/me/coupons"
    }

    object Brands {
        const val BASE = "/api/v1/brands"
        const val BY_ID = "$BASE/{brandId}"
    }

    object AdminBrands {
        const val BASE = "/api/admin/v1/brands"
        const val BY_ID = "$BASE/{brandId}"
    }

    object Products {
        const val BASE = "/api/v1/products"
        const val BY_ID = "$BASE/{productId}"
    }

    object AdminProducts {
        const val BASE = "/api/admin/v1/products"
        const val BY_ID = "$BASE/{productId}"
    }

    object Likes {
        const val BASE = "/api/v1/likes"
        const val BY_PRODUCT_ID = "$BASE/{productId}"
        const val ME = "$BASE/me"
    }

    object Orders {
        const val BASE = "/api/v1/orders"
        const val ME = "$BASE/me"
        const val ME_BY_ID = "$ME/{orderId}"
    }

    object AdminOrders {
        const val BASE = "/api/admin/v1/orders"
        const val BY_ID = "$BASE/{orderId}"
    }

    object Coupons {
        const val BASE = "/api/v1/coupons"
        const val ISSUE = "$BASE/{couponId}/issue"
    }

    object AdminCoupons {
        const val BASE = "/api/admin/v1/coupons"
        const val BY_ID = "$BASE/{couponId}"
        const val ISSUES = "$BASE/{couponId}/issues"
    }

    object Examples {
        const val BASE = "/api/v1/examples"
        const val BY_ID = "$BASE/{id}"
    }
}
