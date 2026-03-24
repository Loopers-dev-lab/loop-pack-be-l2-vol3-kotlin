package com.loopers.application.user

import com.loopers.domain.user.event.ActionType
import com.loopers.domain.user.event.UserActionEvent
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("UserActionLogHandler")
class UserActionLogHandlerTest {

    private val handler = UserActionLogHandler()

    @DisplayName("유저 행동 이벤트를 처리할 때,")
    @Nested
    inner class OnUserAction {

        @DisplayName("상품 조회 이벤트를 예외 없이 로그로 출력한다.")
        @Test
        fun logsProductViewedEvent() {
            // arrange
            val event = UserActionEvent(
                userId = 1L,
                actionType = ActionType.PRODUCT_VIEWED,
                targetId = 100L,
                timestamp = Instant.now(),
            )

            // act & assert — 예외 없이 정상 완료
            handler.onUserAction(event)
        }

        @DisplayName("주문 요청 이벤트를 예외 없이 로그로 출력한다.")
        @Test
        fun logsOrderPlacedEvent() {
            // arrange
            val event = UserActionEvent(
                userId = 1L,
                actionType = ActionType.ORDER_PLACED,
                targetId = 200L,
                timestamp = Instant.now(),
            )

            // act & assert
            handler.onUserAction(event)
        }
    }
}
