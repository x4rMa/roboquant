package org.roboquant.brokers.sim

import org.roboquant.common.Asset
import org.roboquant.common.Size
import org.roboquant.common.bips
import org.roboquant.feeds.PriceBar
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PricingEngineTest {

    private val priceBar = PriceBar(Asset("TEST"), 100.0, 101.0, 99.0, 100.5, 1000)

    @Test
    fun noCostTest() {
        val pe = NoCostPricingEngine()
        val pricing = pe.getPricing(priceBar, Instant.now())
        val price = pricing.marketPrice(Size(100))
        assertEquals(priceBar.getPrice(), price)
    }

    @Test
    fun spreadPricing() {
        // Pricing engine with 200 BIPS (2%) spread
        val pe = SpreadPricingEngine(200.bips, "OPEN")
        val pricing = pe.getPricing(priceBar, Instant.now())
        val price = pricing.marketPrice(Size(100))
        assertEquals(priceBar.getPrice("OPEN") * 1.01, price)

        val price2 = pricing.marketPrice(Size(-100))
        assertEquals(priceBar.getPrice("OPEN") * 0.99, price2)
    }
}
