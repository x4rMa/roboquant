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

import kotlin.test.Test
import org.roboquant.Roboquant
import org.roboquant.TestData
import org.roboquant.brokers.Position
import org.roboquant.brokers.sim.execution.InternalAccount
import org.roboquant.common.Currency
import org.roboquant.common.Size
import org.roboquant.feeds.Event
import org.roboquant.feeds.random.RandomWalkFeed
import org.roboquant.feeds.TradePrice
import org.roboquant.loggers.LastEntryLogger
import org.roboquant.loggers.latestRun
import org.roboquant.strategies.EMAStrategy
import java.time.Instant
import kotlin.test.assertTrue

internal class AlphaBetaMetricTest {

    @Test
    fun test() {
        val feed = TestData.feed
        val marketAsset = feed.assets.first()
        val strategy = EMAStrategy.PERIODS_5_15
        val alphaBetaMetric = AlphaBetaMetric(marketAsset, 50)
        val logger = LastEntryLogger()
        val roboquant = Roboquant(strategy, alphaBetaMetric, logger = logger)
        roboquant.run(feed, name = "test")

        val alpha = logger.getMetric("account.alpha").latestRun().last().value
        assertTrue(!alpha.isNaN())

        val beta = logger.getMetric("account.beta").latestRun().last().value
        assertTrue(!beta.isNaN())
    }

    @Test
    fun test2() {
        val feed = RandomWalkFeed.lastYears(1, nAssets = 1)
        val asset = feed.assets.first()
        val internalAccount = InternalAccount(Currency.USD)
        val metric = AlphaBetaMetric(asset, 50)

        repeat(60) {
            val price = it + 10.0
            val event = Event(listOf(TradePrice(asset, price)), Instant.now())

            // Our portfolio is exactly the same as market reference asset, so ALPHA should be 0 and BETA 1
            internalAccount.setPosition(Position(asset, Size(10), 10.0, price))
            val account = internalAccount.toAccount()

            val r = metric.calculate(account, event)
            if (r.isNotEmpty()) {
                val alpha = r["account.alpha"]!!
                val beta = r["account.beta"]!!
                assertTrue(alpha in -0.02..0.02)
                assertTrue(beta in 0.98..1.02)
            }

        }

    }

}
