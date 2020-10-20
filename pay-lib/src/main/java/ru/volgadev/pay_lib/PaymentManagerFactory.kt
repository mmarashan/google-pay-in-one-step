package ru.volgadev.pay_lib

import android.content.Context
import androidx.annotation.AnyThread
import ru.volgadev.pay_lib.impl.PaymentManagerImpl

object PaymentManagerFactory {

    @AnyThread
    @JvmStatic
    fun createPaymentManager(context: Context): PaymentManager {
        return PaymentManagerImpl(context)
    }
}