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
@file:Suppress("LongParameterList")

package org.roboquant.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter

/**
 * Asset is used to uniquely identify a financial instrument. So it can represent a stock, a future or a
 * cryptocurrency. See the [AssetType] for the supported types.
 *
 * For asset types that require additional information (like options or futures), the symbol name is expected to
 * contain this information.
 *
 * All of its properties are read-only, and assets are ideally only created once and reused thereafter. An asset
 * instance is immutable.
 *
 * @property symbol none empty symbol name, for derivatives like options this includes the details that identify the
 * contract
 * @property type type of asset class, default is [AssetType.STOCK]
 * @property currency currency, default is [Currency.USD]
 * @property exchange Exchange this asset is traded on, default is [Exchange.DEFAULT]
 * @property multiplier contract size multiplier, default is 1.0.
 * @property id asset identifier, default is an empty string
 * @constructor Create a new asset
 */
data class Asset(
    val symbol: String,
    val type: AssetType = AssetType.STOCK,
    val currency: Currency = Currency.USD,
    val exchange: Exchange = Exchange.DEFAULT,
    val multiplier: Double = 1.0,
    val id: String = ""
) : Comparable<Asset> {

    /**
     * Alternative constructor that allows to use strings to identify the currency and exchange.
     */
    constructor(
        symbol: String,
        type: AssetType = AssetType.STOCK,
        currencyCode: String,
        exchangeCode: String = "",
        multiplier: Double = 1.0,
        id: String = ""
    ) : this(symbol, type, Currency.getInstance(currencyCode), Exchange.getInstance(exchangeCode), multiplier, id)

    init {
        require(symbol.isNotBlank()) { "Symbol in an asset cannot be empty or blank" }
    }

    /**
     * Contains methods to create specific asset types, like options or futures using international standards to
     * generate the appropriate symbol name.
     */
    companion object {

        /**
         * Returns an option contract using the OCC (`Options Clearing Corporation`) option symbol standard.
         * The OCC option symbol string consists of four parts:
         *
         * 1. Uppercase [symbol] of the underlying stock or ETF, padded with trailing spaces to six characters
         * 2. The [expiration] date, in the format `yymmdd`
         * 3. The Option [type], single character either P(ut) or C(all)
         * 4. The strike price, as the [price] x 1000, front padded with zeros to make it eight digits
         */
        fun optionContract(
            symbol: String,
            expiration: LocalDate,
            type: Char,
            price: BigDecimal,
            multiplier: Double = 100.0,
            currencyCode: String = "USD",
            exchangeCode: String = "",
            id: String = ""
        ): Asset {
            require(symbol.isNotBlank()) { "Symbol cannot be blank" }
            require(type in setOf('P', 'C')) { "Type should be P or C" }
            val formatter = DateTimeFormatter.ofPattern("yyMMdd")
            val optionSymbol = "%-6s".format(symbol.uppercase()) +
                    expiration.format(formatter) +
                    type.uppercase() +
                    "%08d".format(price.multiply(BigDecimal(1000)).toInt())

            return Asset(optionSymbol, AssetType.OPTION, currencyCode, exchangeCode, multiplier, id)
        }

        /**
         * Returns a future contract based on the provided parameters. It will generate a [symbol] name using the
         * Future Contract Trading Code standard.
         */
        fun futureContract(
            symbol: String,
            month: Month,
            year: Int,
            currencyCode: String = "USD",
            exchangeCode: String = "",
            multiplier: Double = 1.0,
            id: String = ""
        ): Asset {
            val months = arrayOf('F', 'G', 'H', 'J', 'K', 'M', 'N', 'Q', 'U', 'V', 'X', 'Z')
            val monthEncoding = months[month.value - 1]
            val yearCode = year.toString()
            val futureSymbol = "$symbol$monthEncoding${yearCode.takeLast(2)}"
            return Asset(futureSymbol, AssetType.FUTURES, currencyCode, exchangeCode, multiplier, id)
        }

        /**
         * Return a Crypto asset based on the [base] currency, [quote] currency and [exchangeCode]
         */
        fun crypto(base: String, quote: String, exchangeCode: String) =
            Asset("$base/$quote", AssetType.CRYPTO, quote, exchangeCode)

        /**
         * Returns a forex currency pair asset based on the provided [symbol]. This method will try to determine the
         * base and quote currency based on the provided symbol name.
         */
        fun forexPair(symbol: String): Asset {
            val codes = symbol.split('_', '-', ' ', '/', ':')
            val (base, quote) = if (codes.size == 2) {
                val c1 = codes.first().uppercase()
                val c2 = codes.last().uppercase()
                Pair(c1, c2)
            } else if (codes.size == 1 && symbol.length == 6) {
                // We assume a 3-3 split
                val c1 = symbol.substring(0, 3).uppercase()
                val c2 = symbol.substring(3, 6).uppercase()
                Pair(c1, c2)
            } else {
                throw UnsupportedException("Cannot parse $symbol to currency pair")
            }
            return Asset("$base/$quote", AssetType.FOREX, quote, "FOREX")
        }

    }

    /**
     * Return the value of the asset given the provided [size] and [price]. The calculation takes the [multiplier]
     * of the asset into account.
     */
    fun value(size: Size, price: Double): Amount {
        // If size is zero, an unknown price (Double.NanN) is fine
        return if (size.iszero) Amount(currency, 0.0) else Amount(currency, size.toDouble() * multiplier * price)
    }

    /**
     * Faster hashcode that is well distributed than the default data class generated one
     */
    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    /**
     * Compares this asset with the [other] asset for order based on the [symbol] name. Returns zero if this asset
     * is equal to the specified [other] asset, a negative number if it's less than [other], or a positive number
     * if it's greater than [other].
     */
    override fun compareTo(other: Asset): Int {
        return symbol.compareTo(other.symbol)
    }

    /**
     * Return the contract size for a given [amount] and [price]. The provided amount and price should be denoted in
     * the currency of the asset. When rounding is required, this method will always round down.
     *
     * It supports fractional sizes by providing a number of [fractions] bigger than 0.
     */
    fun contractSize(amount: Double, price: Double, fractions: Int = 0): Size {
        require(fractions >= 0) { "factions has to be >= 0, found $fractions" }
        val singleContractValue = value(Size.ONE, price).value
        val size = BigDecimal(amount / singleContractValue).setScale(fractions, RoundingMode.DOWN)
        return Size(size)
    }

    /**
     * @see Object.equals
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Asset

        if (symbol != other.symbol) return false
        if (type != other.type) return false
        if (currency != other.currency) return false
        if (exchange != other.exchange) return false
        if (multiplier != other.multiplier) return false
        return id == other.id
    }

}

/**
 * Get an asset based on its [symbol] name. Will throw a NoSuchElementException if no asset is found. If there are
 * multiple assets with the same symbol, the first one will be returned.
 */
fun Collection<Asset>.getBySymbol(symbol: String): Asset = first { it.symbol == symbol }

/**
 * Find an asset based on its [symbols] name. Will return an empty list if no assets are matched.
 */
fun Collection<Asset>.findBySymbols(vararg symbols: String): List<Asset> = findBySymbols(symbols.asList())

/**
 * Find an asset based on its [symbols] name. Will return an empty list if no assets are matched.
 */
fun Collection<Asset>.findBySymbols(symbols: Collection<String>): List<Asset> = filter { it.symbol in symbols }

/**
 * Find all assets based on their [currencyCodes]. Returns an empty list if no matching assets can be found.
 */
fun Collection<Asset>.findByCurrencies(vararg currencyCodes: String): List<Asset> =
    findByCurrencies(currencyCodes.asList())

/**
 * Find all assets based on their [currencyCodes]. Returns an empty list if no matching assets can be found.
 */
fun Collection<Asset>.findByCurrencies(currencyCodes: Collection<String>): List<Asset> =
    filter { it.currency.currencyCode in currencyCodes }

/**
 * Get all unique symbols from the assets
 */
val Collection<Asset>.symbols: Array<String>
    get() = map { it.symbol }.distinct().toTypedArray()

/**
 * Find all assets based on their [exchangeCodes]. Returns an empty list if no matching assets can be found.
 */
fun Collection<Asset>.findByExchanges(exchangeCodes: Collection<String>): List<Asset> =
    filter { it.exchange.exchangeCode in exchangeCodes }

/**
 * Find all assets based on their [exchangeCodes]. Returns an empty list if no matching assets can be found.
 */
fun Collection<Asset>.findByExchanges(vararg exchangeCodes: String): List<Asset> =
    findByExchanges(exchangeCodes.asList())

/**
 * Select [n] random assets from a collection, without duplicates. [n] has to equal or smaller than the size of the
 * collection
 */
fun Collection<Asset>.random(n: Int): List<Asset> = shuffled().take(n)

/**
 * Provide a [Summary] for a collection of assets
 */
fun Collection<Asset>.summary(name: String = "assets"): Summary {
    return if (isEmpty()) {
        val s = Summary(name)
        s.add("EMPTY")
        s
    } else {
        val lines = mutableListOf<List<Any>>()
        lines.add(listOf("symbol", "type", "ccy", "exchange", "multiplier", "id"))
        forEach {
            with(it) {
                lines.add(listOf(symbol, type, currency.currencyCode, exchange.exchangeCode, multiplier, id))
            }
        }
        lines.summary(name)
    }

}

