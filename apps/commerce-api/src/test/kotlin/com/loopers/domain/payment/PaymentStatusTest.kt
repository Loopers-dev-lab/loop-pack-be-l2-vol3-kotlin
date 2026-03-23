package com.loopers.domain.payment

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PaymentStatusTest {

    @DisplayName("상태 전이가 허용되는 경우,")
    @Nested
    inner class ValidTransitions {

        @DisplayName("REQUESTED에서 PENDING으로 전이할 수 있다.")
        @Test
        fun canTransitionFromRequestedToPending() {
            assertThat(PaymentStatus.REQUESTED.canTransitionTo(PaymentStatus.PENDING)).isTrue()
        }

        @DisplayName("REQUESTED에서 FAILED로 전이할 수 있다.")
        @Test
        fun canTransitionFromRequestedToFailed() {
            assertThat(PaymentStatus.REQUESTED.canTransitionTo(PaymentStatus.FAILED)).isTrue()
        }

        @DisplayName("PENDING에서 SUCCESS로 전이할 수 있다.")
        @Test
        fun canTransitionFromPendingToSuccess() {
            assertThat(PaymentStatus.PENDING.canTransitionTo(PaymentStatus.SUCCESS)).isTrue()
        }

        @DisplayName("PENDING에서 FAILED로 전이할 수 있다.")
        @Test
        fun canTransitionFromPendingToFailed() {
            assertThat(PaymentStatus.PENDING.canTransitionTo(PaymentStatus.FAILED)).isTrue()
        }
    }

    @DisplayName("상태 전이가 허용되지 않는 경우,")
    @Nested
    inner class InvalidTransitions {

        @DisplayName("REQUESTED에서 SUCCESS로 직접 전이할 수 없다.")
        @Test
        fun cannotTransitionFromRequestedToSuccess() {
            assertThat(PaymentStatus.REQUESTED.canTransitionTo(PaymentStatus.SUCCESS)).isFalse()
        }

        @DisplayName("SUCCESS는 종료 상태이므로 다른 상태로 전이할 수 없다.")
        @Test
        fun cannotTransitionFromSuccess() {
            assertThat(PaymentStatus.SUCCESS.canTransitionTo(PaymentStatus.PENDING)).isFalse()
            assertThat(PaymentStatus.SUCCESS.canTransitionTo(PaymentStatus.FAILED)).isFalse()
            assertThat(PaymentStatus.SUCCESS.canTransitionTo(PaymentStatus.REQUESTED)).isFalse()
        }

        @DisplayName("FAILED는 종료 상태이므로 다른 상태로 전이할 수 없다.")
        @Test
        fun cannotTransitionFromFailed() {
            assertThat(PaymentStatus.FAILED.canTransitionTo(PaymentStatus.PENDING)).isFalse()
            assertThat(PaymentStatus.FAILED.canTransitionTo(PaymentStatus.SUCCESS)).isFalse()
            assertThat(PaymentStatus.FAILED.canTransitionTo(PaymentStatus.REQUESTED)).isFalse()
        }
    }
}
