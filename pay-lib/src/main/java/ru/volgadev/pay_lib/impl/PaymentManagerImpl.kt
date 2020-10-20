package ru.volgadev.pay_lib.impl

import android.content.Context
import ru.volgadev.pay_lib.MerchantData
import ru.volgadev.pay_lib.PaymentManager
import ru.volgadev.pay_lib.PaymentRequest

internal class PaymentManagerImpl(val context: Context) : PaymentManager {

    override fun requestPayment(
        merchantData: MerchantData,
        paymentRequest: PaymentRequest,
        isTest: Boolean
    ) {
        PaymentActivity.openPaymentActivity(context, merchantData, paymentRequest, isTest)
    }

    override fun isPayed(): Boolean {
        return false
    }
}