package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class PaymentTest {
    @DisplayName("결제를 생성할 때, ")
    @Nested
    inner class Create {
        @DisplayName("유효한 정보가 주어지면, PENDING 상태로 생성된다.")
        @Test
        fun createsWithPendingStatus_whenValidDataIsProvided() {
            // arrange & act
            val payment = Payment(
                orderId = 1L,
                userId = 1L,
                cardType = CardType.SAMSUNG,
                cardNo = "xxxx-xxxx-xxxx-1234",
                amount = 50000L,
            )

            // assert
            assertAll(
                { assertThat(payment.orderId).isEqualTo(1L) },
                { assertThat(payment.userId).isEqualTo(1L) },
                { assertThat(payment.cardType).isEqualTo(CardType.SAMSUNG) },
                { assertThat(payment.cardNo).isEqualTo("xxxx-xxxx-xxxx-1234") },
                { assertThat(payment.amount).isEqualTo(50000L) },
                { assertThat(payment.status).isEqualTo(PaymentStatus.PENDING) },
                { assertThat(payment.transactionKey).isNull() },
                { assertThat(payment.reason).isNull() },
            )
        }
    }

    @DisplayName("트랜잭션 키를 할당할 때, ")
    @Nested
    inner class AssignTransactionKey {
        @DisplayName("최초 할당이면, 트랜잭션 키가 설정된다.")
        @Test
        fun assignsKey_whenCalledFirstTime() {
            // arrange
            val payment = Payment(orderId = 1L, userId = 1L, cardType = CardType.KB, cardNo = "xxxx-xxxx-xxxx-5678", amount = 10000L)

            // act
            payment.assignTransactionKey("20250816:TR:abc123")

            // assert
            assertThat(payment.transactionKey).isEqualTo("20250816:TR:abc123")
        }

        @DisplayName("이미 할당된 상태에서 호출하면, 예외가 발생한다.")
        @Test
        fun throwsException_whenKeyAlreadyAssigned() {
            // arrange
            val payment = Payment(orderId = 1L, userId = 1L, cardType = CardType.KB, cardNo = "xxxx-xxxx-xxxx-5678", amount = 10000L)
            payment.assignTransactionKey("20250816:TR:abc123")

            // act & assert
            assertThrows<CoreException> {
                payment.assignTransactionKey("20250816:TR:def456")
            }
        }
    }

    @DisplayName("결제를 완료할 때, ")
    @Nested
    inner class Complete {
        @DisplayName("PENDING 상태에서 SUCCESS로 완료하면, 상태와 사유가 변경된다.")
        @Test
        fun completesToSuccess_whenPending() {
            // arrange
            val payment = Payment(orderId = 1L, userId = 1L, cardType = CardType.SAMSUNG, cardNo = "xxxx-xxxx-xxxx-1234", amount = 50000L)

            // act
            payment.complete(PaymentStatus.SUCCESS, "정상 승인되었습니다.")

            // assert
            assertAll(
                { assertThat(payment.status).isEqualTo(PaymentStatus.SUCCESS) },
                { assertThat(payment.reason).isEqualTo("정상 승인되었습니다.") },
            )
        }

        @DisplayName("PENDING 상태에서 FAILED로 완료하면, 상태와 사유가 변경된다.")
        @Test
        fun completesToFailed_whenPending() {
            // arrange
            val payment = Payment(orderId = 1L, userId = 1L, cardType = CardType.HYUNDAI, cardNo = "xxxx-xxxx-xxxx-9012", amount = 30000L)

            // act
            payment.complete(PaymentStatus.FAILED, "한도초과입니다.")

            // assert
            assertAll(
                { assertThat(payment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(payment.reason).isEqualTo("한도초과입니다.") },
            )
        }

        @DisplayName("이미 완료된 결제에 다시 complete()를 호출하면, 예외가 발생한다.")
        @Test
        fun throwsException_whenAlreadyCompleted() {
            // arrange
            val payment = Payment(orderId = 1L, userId = 1L, cardType = CardType.SAMSUNG, cardNo = "xxxx-xxxx-xxxx-1234", amount = 50000L)
            payment.complete(PaymentStatus.SUCCESS, "정상 승인되었습니다.")

            // act & assert
            assertThrows<CoreException> {
                payment.complete(PaymentStatus.FAILED, "한도초과입니다.")
            }
        }
    }

    @DisplayName("카드 번호를 마스킹할 때, ")
    @Nested
    inner class MaskCardNo {
        @DisplayName("전체 카드 번호가 주어지면, 뒤 4자리만 남기고 마스킹한다.")
        @Test
        fun masksCardNumber_whenFullNumberIsProvided() {
            // arrange & act
            val masked = Payment.maskCardNo("1234-5678-9012-3456")

            // assert
            assertThat(masked).isEqualTo("xxxx-xxxx-xxxx-3456")
        }

        @DisplayName("하이픈 없는 카드 번호도 마스킹한다.")
        @Test
        fun masksCardNumber_whenNoHyphens() {
            // arrange & act
            val masked = Payment.maskCardNo("1234567890123456")

            // assert
            assertThat(masked).isEqualTo("xxxx-xxxx-xxxx-3456")
        }
    }
}
