package com.loopers.application.fcfscoupon

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import com.loopers.domain.fcfscoupon.FcfsCouponIssueRequestModel
import com.loopers.domain.fcfscoupon.FcfsCouponIssueRequestRepository
import com.loopers.domain.fcfscoupon.FcfsCouponTemplateModel
import com.loopers.domain.fcfscoupon.FcfsCouponTemplateRepository
import org.springframework.stereotype.Component

@Component
class FcfsCouponService(
    private val templateRepository: FcfsCouponTemplateRepository,
    private val issueRequestRepository: FcfsCouponIssueRequestRepository,
) {
    fun createTemplate(command: FcfsCouponCommand.CreateTemplate): FcfsCouponTemplateModel {
        return templateRepository.save(
            FcfsCouponTemplateModel(
                name = command.name,
                description = command.description,
                discountType = command.discountType,
                discountValue = command.discountValue,
                minOrderAmount = command.minOrderAmount,
                maxDiscountAmount = command.maxDiscountAmount,
                totalQuantity = command.totalQuantity,
                startedAt = command.startedAt,
                endedAt = command.endedAt,
            ),
        )
    }

    fun getTemplate(id: Long): FcfsCouponTemplateModel {
        return templateRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "선착순 쿠폰 템플릿을 찾을 수 없습니다. id=$id")
    }

    fun getTemplates(pageQuery: PageQuery): PageResult<FcfsCouponTemplateModel> {
        return templateRepository.findAll(pageQuery)
    }

    fun updateTemplate(id: Long, command: FcfsCouponCommand.UpdateTemplate): FcfsCouponTemplateModel {
        val template = getTemplate(id)
        val updated = template.update(
            name = command.name,
            description = command.description,
            discountType = command.discountType,
            discountValue = command.discountValue,
            minOrderAmount = command.minOrderAmount,
            maxDiscountAmount = command.maxDiscountAmount,
            totalQuantity = command.totalQuantity,
            startedAt = command.startedAt,
            endedAt = command.endedAt,
        )
        return templateRepository.save(updated)
    }

    fun deleteTemplate(id: Long) {
        val template = getTemplate(id)
        templateRepository.save(template.delete())
    }

    fun createIssueRequest(templateId: Long, memberId: Long): FcfsCouponIssueRequestModel {
        return issueRequestRepository.save(
            FcfsCouponIssueRequestModel(
                templateId = templateId,
                memberId = memberId,
            ),
        )
    }

    fun getIssueRequest(id: Long): FcfsCouponIssueRequestModel {
        return issueRequestRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "발급 요청을 찾을 수 없습니다. id=$id")
    }
}
