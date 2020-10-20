package ru.volgadev.pay_lib

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.MainThread
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import kotlinx.android.synthetic.main.activity_checkout.*
import org.json.JSONException
import org.json.JSONObject

class PaymentActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        val paymentRequest = intent.getParcelableExtra<PaymentRequest>(PAYMENT_REQUEST_EXTRA)
        val merchantParameters = intent.getParcelableExtra<MerchantData>(MERCHANT_DATA_EXTRA)
        val isTest = intent.getBooleanExtra(IS_TEST_EXTRA, true)
        if (paymentRequest == null || merchantParameters == null) {
            throw IllegalStateException("You should open pay activity via PaymentActivity.openPaymentActivity(...)")
        }

        val requestsManager = PayRequestsManager(merchantParameters)
        val paymentsClient = applicationContext.createPaymentsClient(isTest)

        drawPurchase(paymentRequest)
        possiblyShowGooglePayButton(paymentsClient, requestsManager)

        googlePayButton.setOnClickListener {
            requestPayment(
                paymentsClient,
                requestsManager,
                paymentRequest
            )
        }
    }

    @MainThread
    private fun drawPurchase(paymentRequest: PaymentRequest) {
        Log.v(TAG, "drawPurchase($paymentRequest)")
        detailTitle.text = paymentRequest.title
        detailDescription.text = paymentRequest.description
        val priceText = "${paymentRequest.price.centsToString()} ${paymentRequest.currencyCode}"
        detailPrice.text = priceText
    }

    private fun possiblyShowGooglePayButton(
        paymentsClient: PaymentsClient,
        payRequestsManager: PayRequestsManager
    ) {
        val request = payRequestsManager.isReadyToPayRequest()

        val task = paymentsClient.isReadyToPay(request)
        task.addOnCompleteListener { completedTask ->
            try {
                completedTask.getResult(ApiException::class.java)?.let { available ->
                    if (available) {
                        googlePayButton.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(
                            this,
                            "Unfortunately, Google Pay is not available on this device",
                            Toast.LENGTH_LONG
                        ).show();
                    }
                }
            } catch (exception: ApiException) {
                Log.w("isReadyToPay failed", exception)
            }
        }
    }

    private fun requestPayment(
        paymentsClient: PaymentsClient,
        payRequestsManager: PayRequestsManager,
        paymentRequest: PaymentRequest
    ) {
        /* prevent multiple clicks */
        googlePayButton.isClickable = false

        val paymentDataRequest = payRequestsManager.paymentDataRequest(
            paymentRequest.price,
            paymentRequest.countryCode,
            paymentRequest.currencyCode
        )
        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(paymentDataRequest), this, PAYMENT_REQUEST_CODE
        )
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            // Value passed in AutoResolveHelper
            PAYMENT_REQUEST_CODE -> {
                when (resultCode) {
                    RESULT_OK ->
                        data?.let { intent ->
                            PaymentData.getFromIntent(intent)?.let(::handlePaymentSuccess)
                        }

                    RESULT_CANCELED -> {
                        // The user cancelled the payment attempt
                    }

                    AutoResolveHelper.RESULT_ERROR -> {
                        AutoResolveHelper.getStatusFromIntent(data)?.let {
                            handleError(it.statusCode)
                        }
                    }
                }

                // Re-enables the Google Pay payment button.
                googlePayButton.isClickable = true
            }
        }
    }

    /**
     * PaymentData response object contains the payment information, as well as any additional
     * requested information, such as billing and shipping address.
     *
     * @param paymentData A response object returned by Google after a payer approves payment.
     * @see [Payment
     * Data](https://developers.google.com/pay/api/android/reference/object.PaymentData)
     */
    private fun handlePaymentSuccess(paymentData: PaymentData) {
        val paymentInformation = paymentData.toJson() ?: return

        try {
            // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
            val paymentMethodData =
                JSONObject(paymentInformation).getJSONObject("paymentMethodData")
            val billingName = paymentMethodData.getJSONObject("info")
                .getJSONObject("billingAddress").getString("name")
            Log.d("BillingName", billingName)

            Toast.makeText(
                this,
                getString(R.string.payments_show_name, billingName),
                Toast.LENGTH_LONG
            ).show()

            // Logging token string.
            Log.d(
                "GooglePaymentToken", paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("token")
            )

        } catch (e: JSONException) {
            Log.e("handlePaymentSuccess", "Error: " + e.toString())
        }

    }

    /**
     * At this stage, the user has already seen a popup informing them an error occurred. Normally,
     * only logging is required.
     *
     * @see [
     * Wallet Constants Library](https://developers.google.com/android/reference/com/google/android/gms/wallet/WalletConstants.constant-summary)
     */
    private fun handleError(statusCode: Int) {
        when (statusCode) {
            WalletConstants.ERROR_CODE_AUTHENTICATION_FAILURE -> {
                Log.e(TAG, "ERROR_CODE_AUTHENTICATION_FAILURE")
            }
            WalletConstants.ERROR_CODE_BUYER_ACCOUNT_ERROR -> {
                Log.e(TAG, "ERROR_CODE_BUYER_ACCOUNT_ERROR")
            }
            WalletConstants.ERROR_CODE_SPENDING_LIMIT_EXCEEDED -> {
                Log.e(TAG, "ERROR_CODE_SPENDING_LIMIT_EXCEEDED")
            }
            WalletConstants.ERROR_CODE_INVALID_TRANSACTION -> {
                Log.e(TAG, "ERROR_CODE_INVALID_TRANSACTION")
            }
            WalletConstants.ERROR_CODE_UNSUPPORTED_API_VERSION -> {
                Log.e(TAG, "ERROR_CODE_UNSUPPORTED_API_VERSION")
            }
            WalletConstants.ERROR_CODE_UNKNOWN -> {
                Log.e(TAG, "ERROR_CODE_UNKNOWN")
            }
            else -> {
                Log.e(TAG, "Error with unknown code $statusCode")
            }
        }
    }

    companion object {

        private val TAG = PaymentActivity.javaClass.name
        private const val PAYMENT_REQUEST_CODE = 991
        private const val MERCHANT_DATA_EXTRA = "MERCHANT_PARAMETERS_EXTRA"
        private const val PAYMENT_REQUEST_EXTRA = "PAYMENT_REQUEST_EXTRA"
        private const val IS_TEST_EXTRA = "IS_TEST_EXTRA"

        fun openPaymentActivity(
            context: Context,
            merchantData: MerchantData,
            paymentRequest: PaymentRequest,
            isTest: Boolean = true
        ) {
            val intent = Intent(context, PaymentActivity::class.java).apply {
                putExtra(PAYMENT_REQUEST_EXTRA, paymentRequest)
                putExtra(MERCHANT_DATA_EXTRA, merchantData)
                putExtra(IS_TEST_EXTRA, isTest)
            }
            context.startActivity(intent)
        }
    }
}