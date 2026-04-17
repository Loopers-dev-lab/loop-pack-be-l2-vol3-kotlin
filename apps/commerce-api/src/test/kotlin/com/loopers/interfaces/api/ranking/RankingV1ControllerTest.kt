package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingFacade
import com.loopers.application.ranking.RankingItemInfo
import com.loopers.application.ranking.RankingPageInfo
import com.loopers.application.ranking.RankingProductInfo
import com.loopers.domain.ranking.RankingPeriod
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("RankingV1Controller")
class RankingV1ControllerTest {

    private val rankingFacade: RankingFacade = mockk()
    private val controller = RankingV1Controller(rankingFacade)

    @DisplayName("period=DAILY 를 전달하면 일간 경로로 facade 에 위임한다")
    @Test
    fun dailyPeriodDelegatesToFacade() {
        val expectedDate = LocalDate.of(2026, 4, 10)
        val pageInfo = RankingPageInfo(
            content = listOf(
                RankingItemInfo(
                    rank = 1,
                    score = 5.0,
                    product = RankingProductInfo(id = 10L, name = "상품", price = 1000L, brandName = "브랜드"),
                ),
            ),
            totalElements = 1,
            page = 1,
            size = 20,
        )
        every { rankingFacade.getRankings(RankingPeriod.DAILY, expectedDate, 1, 20) } returns pageInfo

        val response = controller.getRankings(period = RankingPeriod.DAILY, date = "20260410", size = 20, page = 1)

        assertThat(response.data).isNotNull
        assertThat(response.data!!.content).hasSize(1)
        assertThat(response.data!!.content[0].rank).isEqualTo(1)
    }

    @DisplayName("period=WEEKLY 를 전달하면 주간 경로로 facade 에 위임한다")
    @Test
    fun weeklyPeriodDelegatesToFacade() {
        val expectedDate = LocalDate.of(2026, 4, 15)
        val pageInfo = RankingPageInfo.empty(1, 20)
        every { rankingFacade.getRankings(RankingPeriod.WEEKLY, expectedDate, 1, 20) } returns pageInfo

        controller.getRankings(period = RankingPeriod.WEEKLY, date = "20260415", size = 20, page = 1)

        verify(exactly = 1) { rankingFacade.getRankings(RankingPeriod.WEEKLY, expectedDate, 1, 20) }
    }

    @DisplayName("period=MONTHLY 를 전달하면 월간 경로로 facade 에 위임한다")
    @Test
    fun monthlyPeriodDelegatesToFacade() {
        val expectedDate = LocalDate.of(2026, 4, 15)
        val pageInfo = RankingPageInfo.empty(1, 20)
        every { rankingFacade.getRankings(RankingPeriod.MONTHLY, expectedDate, 1, 20) } returns pageInfo

        controller.getRankings(period = RankingPeriod.MONTHLY, date = "20260415", size = 20, page = 1)

        verify(exactly = 1) { rankingFacade.getRankings(RankingPeriod.MONTHLY, expectedDate, 1, 20) }
    }

    @DisplayName("기본값은 page=1, size=20 이다")
    @Test
    fun defaultsToPage1Size20() {
        val pageInfo = RankingPageInfo.empty(1, 20)
        every { rankingFacade.getRankings(any(), any(), 1, 20) } returns pageInfo

        val response = controller.getRankings(period = RankingPeriod.DAILY, date = "20260410", size = 20, page = 1)

        assertThat(response.data).isNotNull
        assertThat(response.data!!.page).isEqualTo(1)
        assertThat(response.data!!.size).isEqualTo(20)
    }
}
