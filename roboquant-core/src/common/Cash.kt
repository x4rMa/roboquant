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

package org.roboquant.common

import org.roboquant.brokers.Account
import java.lang.Exception


/**
 * Cash can contain amounts of multiple currencies at the same time. So for example a single instance of Cash can
 * contain both USD and EURO amounts.
 *
 * You can add other currencies to a Cash instance. If the currency is already contained in the Cash instance, it will
 * be added to the existing amount, otherwise the currency and amount will be added.
 *
 * It is used throughout roboquant in order to support trading in multiple assets with different currency denominations.
 *
 * For storing monetary amounts internally it uses [Double], since it is accurate enough for trading while providing large
 * performance benefits over BigDecimal.
 *
 * Cash itself will never convert the currencies it contains. However, an account can do this if required, provided the
 * appropriate conversion rates are available. See also [Account.convertToCurrency] on how to convert a Cash instance
 * to a single amount value.
 *
 */
class Cash(vararg amounts: Pair<Currency, Double>) {

    private val data = mutableMapOf<Currency, Double>()

    init {
        amounts.forEach { deposit(it.first, it.second) }
    }


    /**
     * Return the currencies that are hold in this Cash object. Currencies with
     * zero balance will not be included.
     */
    val currencies: List<Currency>
        get() = data.filter { it.value != 0.0 }.keys.toList()


    /**
     * Get the amount for a certain [currency]. If the currency is not
     * found, 0.0 will be returned.
     *
     */
    fun getAmount(currency: Currency): Double {
        return data.getOrDefault(currency, 0.0)
    }

    /**
     * Is this cash instance empty, meaning it has zero entries with a non-zero balance.
     */
    fun isEmpty(): Boolean {
        return !isNotEmpty()
    }

    /**
     * Is this cash instance not empty, meaning it has at least one entry that has a non-zero balance.
     */
    fun isNotEmpty(): Boolean {
        return data.any { it.value != 0.0 }
    }

    /**
     * Add operator + to allow for cash + cash
     */
    operator fun plus(other: Cash): Cash {
        val result = this.copy()
        result.deposit(other)
        return result
    }

    /**
     * Add operator - to allow for cash - cash
     */
    operator fun minus(other: Cash): Cash {
        val result = this.copy()
        result.withdraw(other)
        return result
    }


    /**
     * Set a monetary [amount] denominated in the specified [currency]. If the currency already exist, its amount
     * will be overwritten, otherwise a new entry will be created.
     */
    fun set(currency: Currency, amount: Double) {
        data[currency] = amount
    }

    /**
     * Deposit a monetary [amount] denominated in teh specified [currency]. If the currency already exist, it
     * will be added to the existing amount, otherwise a new entry will be created.
     */
    fun deposit(currency: Currency, amount: Double) {
        data[currency] = data.getOrDefault(currency, 0.0) + amount
    }


    /**
     * Deposit the cash hold in an [other] Cash instance into this one.
     */
    fun deposit(other: Cash) {
        other.data.forEach { deposit(it.key, it.value) }
    }

    /**
     * Withdraw  a monetary [amount] denominated in the specified [currency]. If the currency already exist, it
     * will be deducted from the existing amount, otherwise a new entry will be created.
     *
     * @param currency
     * @param amount
     */
    fun withdraw(currency: Currency, amount: Double) {
        deposit(currency, -amount)
    }



    fun toAmount() : Double {
        return when(currencies.size) {
            0 -> 0.0
            1 -> data.filter { it.value != 0.0 }.values.first()
            else -> throw Exception("Multicurrency account")
        }
    }


    /**
     * Withdraw the cash hold in an [other] Cash instance into this one.
     */
    fun withdraw(other: Cash) {
        other.data.forEach { deposit(it.key, -it.value) }
    }


    /**
     * Does the wallet contain multiple currencies with a non-zero balance.
     */
    fun isMultiCurrency(): Boolean {
        return currencies.size > 1
    }


    /**
     * Create a copy of this cash instance
     */
    fun copy(): Cash {
        val result = Cash()
        result.data.putAll(data)
        return result
    }

    /**
     * Clear this Cash instance, removing all entries.
     */
    fun clear() {
        data.clear()
    }

    /**
     * Provide a map representation of the cash hold where the key is the [Currency] and the value is the amount.
     * By default, empty values will not be included but this can be changed by setting [includeEmpty] to true.
     */
    fun toMap(includeEmpty: Boolean = false): Map<Currency, Double> =
        if (includeEmpty) data.toMap() else data.filter { it.value != 0.0 }


    /**
     * Create a string representation with respecting currency preferred settings when formatting the amounts.
     */
    override fun toString(): String {
        val sb = StringBuffer()
        for ((c, v) in data) {
            if (v != 0.0)
                sb.append("${c.displayName} => ${c.format(v)} \n")
        }
        return sb.toString()
    }

    /**
     * Provide a short summary including all currencies, also the one that have a zero balance.
     */
    fun summary(header: String = "Cash"): Summary {
        val s = Summary(header)
        data.forEach {
            s.add(it.key.displayName, it.key.format(it.value))
        }
        return s
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Cash) {
            data.toMap() == other.toMap()
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return data.toMap().hashCode()
    }

}



// Some extensions to make it easier to create cash objects with one currency
val Number.EUR
    get() = Cash(Currency.EUR to this.toDouble())

val Number.USD
    get() = Cash(Currency.USD to this.toDouble())