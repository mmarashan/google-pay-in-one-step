package ru.volgadev.pay_lib

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

interface PaymentManager {

    fun requestPayment(
        merchantData: MerchantData,
        paymentRequest: PaymentRequest,
        isTest: Boolean = true
    )

    fun isPayed(): Boolean
}

/**
 * @param merchantName name encoded as UTF-8. Merchant name is rendered in the payment sheet.
 * @param gateway payment gateway from https://developers.google.com/pay/api?hl=ru#participating-processors
 * @param gatewayMerchantId
 * @param allowedCardNetworks One or more card networks that you support,
 *        also supported by the Google Pay API.
 */
@Parcelize
data class MerchantData(
    val merchantName: String,
    val gateway: String,
    val gatewayMerchantId: String,
    val allowedCardNetworks: List<String> = listOf(
        "AMEX", "DISCOVER", "INTERAC", "JCB", "MASTERCARD", "VISA"
    )
) : Parcelable

/**
 * @param title purchase title
 * @param description purchase description
 * @param price price in like-cents dimensions
 * @param currencyCode ISO 4217 alphabetic currency code.
 * @param countryCode ISO 3166-1 alpha-2 country code where the transaction is processed.
 *          This is required for merchants based in European Economic Area (EEA) countries.
 */
@Parcelize
data class PaymentRequest(
    val title: String,
    val description: String,
    val price: Long,
    val currencyCode: String,
    val countryCode: String
) : Parcelable