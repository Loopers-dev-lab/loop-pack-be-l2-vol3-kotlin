package com.loopers.application.brand

import com.loopers.domain.brand.BrandChanger
import com.loopers.domain.brand.BrandReader
import com.loopers.domain.brand.BrandRegister
import com.loopers.domain.brand.BrandRemover
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrandFacade(
    private val brandRegister: BrandRegister,
    private val brandReader: BrandReader,
    private val brandChanger: BrandChanger,
    private val brandRemover: BrandRemover,
) {

    @Transactional
    fun register(command: RegisterCommand): BrandInfo.Detail {
        val brand = brandRegister.register(command.name)
        return BrandInfo.Detail.from(brand)
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): BrandInfo.Detail {
        val brand = brandReader.getById(id)
        return BrandInfo.Detail.from(brand)
    }

    @Transactional(readOnly = true)
    fun getAllActive(): List<BrandInfo.Main> {
        return brandReader.getAllActive().map { BrandInfo.Main.from(it) }
    }

    @Transactional
    fun changeName(id: Long, command: ChangeNameCommand): BrandInfo.Detail {
        val brand = brandChanger.changeName(id, command.name)
        return BrandInfo.Detail.from(brand)
    }

    @Transactional
    fun remove(id: Long) {
        brandRemover.remove(id)
    }

    data class RegisterCommand(val name: String)
    data class ChangeNameCommand(val name: String)
}
