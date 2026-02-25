package com.loopers.support.constant

object AuthHeaders {

    object User {
        const val LOGIN_ID = "X-Loopers-LoginId"
        const val LOGIN_PW = "X-Loopers-LoginPw"
    }

    object Admin {
        const val LDAP = "X-Loopers-Ldap"
        const val LDAP_VALUE = "loopers.admin"
    }
}
