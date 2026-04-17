package com.loopers.interfaces.api.user.ranking

import com.loopers.application.user.ranking.RankingPeriod
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

/**
 * `period` 쿼리 파라미터의 대소문자 무관 바인딩용 Converter.
 * 잘못된 값은 `IllegalArgumentException` → Spring MVC가 `MethodArgumentTypeMismatchException`으로 감싸
 * `ApiControllerAdvice`의 기존 400 핸들러가 응답한다.
 */
@Component
class RankingPeriodConverter : Converter<String, RankingPeriod> {
    override fun convert(source: String): RankingPeriod = RankingPeriod.valueOf(source.uppercase())
}
