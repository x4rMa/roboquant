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

package org.roboquant.ibkr

import org.roboquant.common.Asset
import org.roboquant.common.AssetType
import org.roboquant.common.Config
import org.roboquant.feeds.PriceAction
import org.roboquant.feeds.filter
import kotlin.test.Test
import kotlin.test.assertTrue

internal class IBKRHistoricFeedTestIT {

    @Test
    fun ibkrEUFeed() {
        Config.getProperty("test.ibkr") ?: return

        val feed = IBKRHistoricFeed()
        val symbols = listOf("ABN", "ASML", "KPN")
        val assets = symbols.map { Asset(it, AssetType.STOCK, "EUR", "AEB") }.toTypedArray()
        feed.retrieve(*assets)
        feed.waitTillRetrieved()
        val actions = feed.filter<PriceAction>()
        assertTrue(actions.isNotEmpty())

    }


    @Test
    fun ibkrUSFeed() {
        Config.getProperty("test.ibkr") ?: return

        val feed = IBKRHistoricFeed()
        val symbols = listOf("TSLA", "AAPL")
        val assets = symbols.map { Asset(it, AssetType.STOCK, "USD", "") }.toTypedArray()
        feed.retrieve(*assets)
        feed.waitTillRetrieved()
        val actions = feed.filter<PriceAction>()
        assertTrue(actions.isNotEmpty())
    }


}
