package com.loopers.application.fcfscoupon

import com.loopers.domain.common.PageQuery
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminFcfsCouponFacade(
    private val fcfsCouponService: FcfsCouponService,
) {
    @Transactional
    fun createTemplate(command: FcfsCouponCommand.CreateTemplate): FcfsCouponTemplateInfo {
        return fcfsCouponService.createTemplate(command)
            .let { FcfsCouponTemplateInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getTemplate(id: Long): FcfsCouponTemplateInfo {
        return fcfsCouponService.getTemplate(id)
            .let { FcfsCouponTemplateInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getTemplates(page: Int, size: Int): List<FcfsCouponTemplateInfo> {
        return fcfsCouponService.getTemplates(PageQuery(page, size))
            .content.map { FcfsCouponTemplateInfo.from(it) }
    }

    @Transactional
    fun updateTemplate(id: Long, command: FcfsCouponCommand.UpdateTemplate): FcfsCouponTemplateInfo {
        return fcfsCouponService.updateTemplate(id, command)
            .let { FcfsCouponTemplateInfo.from(it) }
    }

    @Transactional
    fun deleteTemplate(id: Long) {
        fcfsCouponService.deleteTemplate(id)
    }
}
