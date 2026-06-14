package com.example.foodike.payment.di

import com.example.foodike.payment.domain.port.PaymentGatewayPort
import com.example.foodike.payment.domain.port.PaymentRepository
import com.example.foodike.payment.domain.service.PaymentService
import com.example.foodike.payment.infrastructure.gateway.RazorpayConfig
import com.example.foodike.payment.infrastructure.gateway.RazorpayGateway
import com.example.foodike.payment.infrastructure.gateway.StubPaymentGateway
import com.example.foodike.payment.infrastructure.persistence.ExposedPaymentRepository
import org.koin.core.module.Module
import org.koin.dsl.module

val paymentModule: Module = module {
    single<PaymentRepository> { ExposedPaymentRepository() }
    single<PaymentGatewayPort> {
        val config = get<RazorpayConfig>()
        if (config.mode.equals("live", ignoreCase = true)) {
            RazorpayGateway(config)
        } else {
            StubPaymentGateway()
        }
    }
    // OrderQueryPort is bound in the app composition root (it bridges to order-service).
    single { PaymentService(paymentRepository = get(), orders = get(), gateway = get(), eventBus = get()) }
}
