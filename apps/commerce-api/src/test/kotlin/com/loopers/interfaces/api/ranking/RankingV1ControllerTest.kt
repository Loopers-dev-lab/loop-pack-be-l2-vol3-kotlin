package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingFacade
import com.loopers.application.ranking.RankingItemInfo
import com.loopers.application.ranking.RankingPageInfo
import com.loopers.application.ranking.RankingProductInfo
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("RankingV1Controller")
class RankingV1ControllerTest {

    private val rankingFacade: RankingFacade = mockk()
    private val controller = RankingV1Controller(rankingFacade)

    @DisplayName("getRankings는 date 파라미터를 파싱해서 facade에 전달한다")
    @Test
    fun parsesDateAndDelegatesToFacade() {
        // arrange
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
        every { rankingFacade.getRankings(expectedDate, 1, 20) } returns pageInfo

        // act
        val response = controller.getRankings(date = "20260410", size = 20, page = 1)

        // assert
        assertThat(response.data).isNotNull
        assertThat(response.data!!.content).hasSize(1)
        assertThat(response.data!!.content[0].rank).isEqualTo(1)
    }

    @DisplayName("기본값은 page=1, size=20이다")
    @Test
    fun defaultsToPage1Size20() {
        val pageInfo = RankingPageInfo.empty(1, 20)
        every { rankingFacade.getRankings(any(), 1, 20) } returns pageInfo

        val response = controller.getRankings(date = "20260410", size = 20, page = 1)

        assertThat(response.data).isNotNull
        assertThat(response.data!!.page).isEqualTo(1)
        assertThat(response.data!!.size).isEqualTo(20)
    }
}
