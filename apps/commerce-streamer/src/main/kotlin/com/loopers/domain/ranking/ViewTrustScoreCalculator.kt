package com.loopers.domain.ranking

class ViewTrustScoreCalculator {

    fun calculate(signals: ViewSignals): Double {
        var score = 0.0

        score += if (signals.isLoggedIn) LOGIN_SCORE else GUEST_SCORE
        score += if (signals.hasUserAgent) UA_PRESENT_SCORE else UA_ABSENT_SCORE
        score += if (signals.hasReferer) REFERER_PRESENT_SCORE else REFERER_ABSENT_SCORE
        score += calculateRateScore(signals.requestsPerMinute)
        score += calculateDiversityScore(signals.distinctProductsIn10Min)

        return score.coerceIn(MIN_TRUST_SCORE, MAX_TRUST_SCORE)
    }

    private fun calculateRateScore(requestsPerMinute: Long): Double {
        return when {
            requestsPerMinute <= NORMAL_RATE_THRESHOLD -> RATE_NORMAL_SCORE
            requestsPerMinute <= SUSPICIOUS_RATE_THRESHOLD -> RATE_SUSPICIOUS_SCORE
            else -> RATE_BOT_SCORE
        }
    }

    private fun calculateDiversityScore(distinctProducts: Long): Double {
        return when {
            distinctProducts >= DIVERSE_THRESHOLD -> DIVERSITY_GOOD_SCORE
            distinctProducts >= 2 -> DIVERSITY_MODERATE_SCORE
            else -> DIVERSITY_POOR_SCORE
        }
    }

    companion object {
        private const val LOGIN_SCORE = 0.3
        private const val GUEST_SCORE = 0.05

        private const val UA_PRESENT_SCORE = 0.1
        private const val UA_ABSENT_SCORE = 0.0

        private const val REFERER_PRESENT_SCORE = 0.1
        private const val REFERER_ABSENT_SCORE = 0.0

        private const val NORMAL_RATE_THRESHOLD = 3L
        private const val SUSPICIOUS_RATE_THRESHOLD = 10L
        private const val RATE_NORMAL_SCORE = 0.3
        private const val RATE_SUSPICIOUS_SCORE = 0.1
        private const val RATE_BOT_SCORE = 0.0

        private const val DIVERSE_THRESHOLD = 3L
        private const val DIVERSITY_GOOD_SCORE = 0.2
        private const val DIVERSITY_MODERATE_SCORE = 0.1
        private const val DIVERSITY_POOR_SCORE = 0.0

        const val MIN_TRUST_SCORE = 0.0
        const val MAX_TRUST_SCORE = 1.0
    }
}
