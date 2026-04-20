package com.loopers.application.user.ranking

/**
 * 랭킹 조회 기간 구분자.
 *
 * interfaces 레이어에서 HTTP 파라미터로 바인딩 후 UseCase에 전달하므로
 * domain이 아닌 application 레이어에 둔다 (interfaces → domain 직접 의존 금지 규칙).
 */
enum class RankingPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
}
