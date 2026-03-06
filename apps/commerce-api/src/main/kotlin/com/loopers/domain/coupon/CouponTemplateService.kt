package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class CouponTemplateService(
    private val couponTemplateRepository: CouponTemplateRepository,
) {
    @Transactional
    fun createTemplate(
        name: String,
        type: CouponType,
        value: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): CouponTemplateModel {
        return couponTemplateRepository.save(
            CouponTemplateModel(
                name = CouponName(name),
                type = type,
                value = CouponValue(value),
                minOrderAmount = minOrderAmount?.let { MinOrderAmount(it) },
                expiredAt = expiredAt,
            )
        )
    }

    @Transactional(readOnly = true)
    fun getTemplates(pageable: Pageable): Page<CouponTemplateModel> {
        return couponTemplateRepository.findAll(pageable)
    }

    @Transactional(readOnly = true)
    fun getTemplateById(id: Long): CouponTemplateModel {
        return couponTemplateRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "쿠폰 템플릿을 찾을 수 없습니다.")
    }

    @Transactional
    fun updateTemplate(
        id: Long,
        name: String,
        type: CouponType,
        value: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): CouponTemplateModel {
        val template = getTemplateById(id)
        template.update(
            name = CouponName(name),
            type = type,
            value = CouponValue(value),
            minOrderAmount = minOrderAmount?.let { MinOrderAmount(it) },
            expiredAt = expiredAt,
        )
        return template
    }

    @Transactional
    fun deleteTemplate(id: Long) {
        val template = getTemplateById(id)
        template.delete()
    }
}
