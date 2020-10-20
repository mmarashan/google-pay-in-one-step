package ru.volgadev.pay_lib.impl

import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentDataRequest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ru.volgadev.pay_lib.MerchantData

internal class PayRequestsManager(
    private val merchantData: MerchantData
) {
    /**
     * Create a Google Pay API base request object with properties used in all requests.
     *
     * @return Google Pay API base request object.
     * @throws JSONException
     */
    private fun baseRequestJsonObject(): JSONObject {
        return JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
        }
    }

    private val allowedAuthMethods = listOf(
        "PAN_ONLY",
        "CRYPTOGRAM_3DS"
    )

    /**
     * Gateway Integration: Identify your gateway and your app's gateway merchant identifier.
     *
     * The Google Pay API response will return an encrypted payment method capable of being charged
     * by a supported gateway after payer authorization.
     *
     * @return Payment data tokenization for the CARD payment method.
     * @throws JSONException
     * @see [PaymentMethodTokenizationSpecification](https://developers.google.com/pay/api/android/reference/object.PaymentMethodTokenizationSpecification)
     */
    private val gatewayTokenizationSpecificationJsonObject = JSONObject().apply {
        put("type", "PAYMENT_GATEWAY")
        put(
            "parameters", JSONObject(
                mapOf(
                    "gateway" to merchantData.gateway,
                    "gatewayMerchantId" to merchantData.gatewayMerchantId
                )
            )
        )
    }


    /**
     * Describe your app's support for the CARD payment method.
     *
     * The provided properties are applicable to both an IsReadyToPayRequest and a
     * PaymentDataRequest.
     *
     * @return A CARD PaymentMethod object describing accepted cards.
     * @see [PaymentMethod](https://developers.google.com/pay/api/android/reference/object.PaymentMethod)
     */
    private fun baseCardPaymentMethodJsonObject(): JSONObject {

        val allowedAuthMethodsJsonArray = JSONArray(allowedAuthMethods)
        val allowedCardNetworksJsonArray = JSONArray(merchantData.allowedCardNetworks)

        return JSONObject().apply {

            val parameters = JSONObject().apply {
                put("allowedAuthMethods", allowedAuthMethodsJsonArray)
                put("allowedCardNetworks", allowedCardNetworksJsonArray)
                put("billingAddressRequired", true)
                put("billingAddressParameters", JSONObject().apply {
                    put("format", "FULL")
                })
            }

            put("type", "CARD")
            put("parameters", parameters)
        }
    }

    /**
     * Describe the expected returned payment data for the CARD payment method
     *
     * @return A CARD PaymentMethod describing accepted cards and optional fields.
     * @throws JSONException
     * @see [PaymentMethod](https://developers.google.com/pay/api/android/reference/object.PaymentMethod)
     */
    private fun allowedPaymentMethodsJsonObject(): JSONObject {
        val cardPaymentMethod = baseCardPaymentMethodJsonObject()
        cardPaymentMethod.put(
            "tokenizationSpecification",
            gatewayTokenizationSpecificationJsonObject
        )
        return cardPaymentMethod
    }

    /**
     * An object describing accepted forms of payment by your app, used to determine a viewer's
     * readiness to pay.
     *
     * @return API version and payment methods supported by the app.
     * @see [IsReadyToPayRequest](https://developers.google.com/pay/api/android/reference/object.IsReadyToPayRequest)
     */
    fun isReadyToPayRequest(): IsReadyToPayRequest {
        val request = baseRequestJsonObject()
        val json = request.apply {
            put(
                "allowedPaymentMethods",
                JSONArray().put(baseCardPaymentMethodJsonObject())
            )
        }
        return IsReadyToPayRequest.fromJson(json.toString())
    }

    /**
     * An object describing information requested in a Google Pay payment sheet
     *
     * @return Payment data expected by your app.
     * @see [PaymentDataRequest](https://developers.google.com/pay/api/android/reference/object.PaymentDataRequest)
     */
    fun paymentDataRequest(
        price: Long,
        countryCode: String,
        currencyCode: String
    ): PaymentDataRequest {
        val request = baseRequestJsonObject()
        val merchantInfo: JSONObject = JSONObject().put("merchantName", merchantData.merchantName)

        val transactionInfo = JSONObject().apply {
            put("totalPrice", price.centsToString())
            put("totalPriceStatus", "FINAL")
            put("countryCode", countryCode)
            put("currencyCode", currencyCode)
        }

        val json = request.apply {
            put(
                "allowedPaymentMethods",
                JSONArray().put(
                    allowedPaymentMethodsJsonObject()
                )
            )
            put("transactionInfo", transactionInfo)
            put("merchantInfo", merchantInfo)

        }
        return PaymentDataRequest.fromJson(json.toString())
    }
}