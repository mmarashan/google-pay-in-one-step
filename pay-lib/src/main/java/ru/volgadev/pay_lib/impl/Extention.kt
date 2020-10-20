package ru.volgadev.pay_lib.impl

import android.content.Context
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import java.math.BigDecimal
import java.math.RoundingMode

fun Context.createPaymentsClient(isTest: Boolean = true): PaymentsClient {
    val paymentEnvironment: Int =
        if (isTest) WalletConstants.ENVIRONMENT_TEST else WalletConstants.ENVIRONMENT_PRODUCTION

    val walletOptions = Wallet.WalletOptions.Builder()
        .setEnvironment(paymentEnvironment)
        .build()

    return Wallet.getPaymentsClient(this, walletOptions)
}

/**
 * Converts cents to a string format accepted by [PayRequestsManager.paymentDataRequest].
 *
 * @param cents value of the price.
 */
fun Long.centsToString() = BigDecimal(this)
    .divide(BigDecimal(100))
    .setScale(2, RoundingMode.HALF_EVEN)
    .toString()