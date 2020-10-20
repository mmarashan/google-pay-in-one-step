package ru.volgadev.googlepayapiclient

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import ru.volgadev.pay_lib.MerchantData
import ru.volgadev.pay_lib.PaymentRequest
import ru.volgadev.pay_lib.PaymentActivity
import ru.volgadev.pay_lib.centsToString

class MainActivity : AppCompatActivity() {

    private val itemToBuy =
        ShopItem(name = "Good Book", description = "Good book about Android", priceCents = 200L)

    private val isTest = true

    /* Get this data from your bank */
    private val merchantData = MerchantData(
        merchantName = "example",
        gateway = "example",
        gatewayMerchantId = "example"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        itemName.text = itemToBuy.name
        itemDescription.text = itemToBuy.description
        val priceStr = itemToBuy.priceCents.centsToString()+" $"
        itemPrice.text = priceStr

        val paymentRequest = PaymentRequest(
            itemToBuy.name, itemToBuy.description, itemToBuy.priceCents,
            "USD", "US"
        )

        buyNowBtn.setOnClickListener {
            PaymentActivity.openPaymentActivity(
                applicationContext,
                merchantData,
                paymentRequest,
                isTest
            )
        }
    }
}