/*
 * Copyright 2021 Neural Layer
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

import org.roboquant.brokers.Account
import org.roboquant.feeds.Event

/**
 * Metric that calculates both the realized and unrealized Profit and Loss. The unrealized PNL is calculated based
 * on the assets in the portfolio and the last known market price. The realized PNL is based on actual trades made
 * and the profit they generated.
 *
 * All amounts are converted to the base currency of the account.
 *
 * @constructor Create new PNL metric
 */
class PNL : SimpleMetric() {

    /**
     * Calculate any metrics given the event of information. This will be called at the
     * end of each step in a run. The result is returned using the base currency of
     * the account.
     *
     * @param account
     * @return
     */
    override fun calc(account: Account, event: Event): MetricResults {
        val now = event.now
        val result = mutableMapOf<String, Double>()

        val pnl = account.trades.realizedPnL()
        val realizedPNL = account.convertToCurrency(pnl, now = now)
        result["pnl.realized"] = realizedPNL

        val totalValue = account.portfolio.unrealizedPNL()
        val unrealizedPNL = account.convertToCurrency(totalValue, now = now)
        result["pnl.unrealized"] = unrealizedPNL

        return result
    }



}