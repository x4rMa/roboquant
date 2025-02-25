/*
 * Copyright 2020-2023 Neural Layer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.roboquant.metrics


import org.hipparchus.stat.correlation.Covariance
import org.hipparchus.stat.descriptive.moment.Variance
import org.roboquant.brokers.Account
import org.roboquant.common.Asset
import org.roboquant.common.PriceSeries
import org.roboquant.common.returns
import org.roboquant.common.totalReturn
import org.roboquant.feeds.Event

/**
 * Calculates the Alpha and Beta of an account. This implementation not only looks at the portfolio positions, but
 * looks at the returns of the complete account, so including cash balances.
 *
 * - Alpha measures the performance of an investment as compared to the market
 * - Beta measures the volatility (or systematic risk) of the account compared to the market
 *
 * The provided risk-free return should be for the same duration as a period.
 *
 * @property referenceAsset The asset to use as reference for the market volatility and returns
 * @property period Over how many events to calculate the beta
 * @property priceType The type of price to use, default is "DEFAULT"
 * @property riskFreeReturn the risk-free return, 1% is 0.01. Default is 0.0
 * @constructor
 */
class AlphaBetaMetric(
    private val referenceAsset: Asset,
    private val period: Int,
    private val priceType: String = "DEFAULT",
    private val riskFreeReturn: Double = 0.0,
) : Metric {

    private val marketData = PriceSeries(period + 1)
    private val portfolioData = PriceSeries(period + 1)

    /**
     * Based on the provided [account] and [event], calculate any metrics and return them.
     */
    override fun calculate(account: Account, event: Event): Map<String, Double> {
        val action = event.prices[referenceAsset] ?: return emptyMap()

        val price = action.getPrice(priceType)
        marketData.add(price)

        val equity = account.equity
        val value = account.convert(equity, time = event.time).value
        portfolioData.add(value)

        if (marketData.isFull() && portfolioData.isFull()) {
            val x1 = marketData.toDoubleArray()
            val x2 = portfolioData.toDoubleArray()

            val marketReturns = x1.returns()
            val portfolioReturns = x1.returns()

            val covariance = Covariance().covariance(portfolioReturns, marketReturns)
            val variance = Variance().evaluate(marketReturns)
            val beta = covariance / variance

            val alpha =
                (x1.totalReturn() - riskFreeReturn) - beta * (x2.totalReturn() - riskFreeReturn)
            return mapOf(
                "account.alpha" to alpha,
                "account.beta" to beta
            )
        }
        return emptyMap()
    }

    override fun reset() {
        portfolioData.clear()
        marketData.clear()
    }
}
