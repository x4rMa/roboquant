/*
 * Copyright 2020-2022 Neural Layer
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

package org.roboquant.brokers.sim.execution

import org.roboquant.brokers.sim.PricingEngine
import org.roboquant.feeds.Event
import org.roboquant.orders.*
import java.util.*
import kotlin.reflect.KClass

/**
 * Engine that simulates how orders are executed on financial markets. For any order to be executed, it needs a
 * corresponding [OrderExecutor] to be registered.
 *
 * @property pricingEngine pricing engine to use to determine the price
 * @constructor Create new Execution engine
 */
class ExecutionEngine(private val pricingEngine: PricingEngine) {

    /**
     * @suppress
     */
    companion object {

        /**
         * All the registered [OrderExecutorFactory]
         */
        val factories = mutableMapOf<KClass<*>, OrderExecutorFactory<Order>>()

        /**
         * Return the order handler for the provided [order]. This will throw an exception if no [OrderExecutorFactory]
         * is registered for the order::class.
         */
        fun getExecutor(order: Order): OrderExecutor<Order> {
            val factory = factories.getValue(order::class)
            return factory.getHandler(order)
        }

        /**
         * Unregister the order handler for order type [T]
         */
        inline fun <reified T : Order> unregister() {
            factories.remove(T::class)
        }

        /**
         * Register a new order handler [factory] for order type [T]. If there was already an order handler registered
         * for the same class it will be replaced.
         */
        inline fun <reified T : Order> register(factory: OrderExecutorFactory<T>) {
            @Suppress("UNCHECKED_CAST")
            factories[T::class] = factory as OrderExecutorFactory<Order>
        }

        init {
            // register all the default included order handlers

            // Single Order types
            register<MarketOrder> { MarketOrderExecutor(it) }
            register<LimitOrder> { LimitOrderExecutor(it) }
            register<StopLimitOrder> { StopLimitOrderExecutor(it) }
            register<StopOrder> { StopOrderExecutor(it) }
            register<TrailLimitOrder> { TrailLimitOrderExecutor(it) }
            register<TrailOrder> { TrailOrderExecutor(it) }

            // Advanced order types
            register<BracketOrder> { BracketOrderExecutor(it) }
            register<OCOOrder> { OCOOrderExecutor(it) }
            register<OTOOrder> { OTOOrderExecutor(it) }

            // Modify order types
            register<UpdateOrder> { UpdateOrderExecutor(it) }
            register<CancelOrder> { CancelOrderExecutor(it) }
        }

    }

    // Return the create-handlers
    private val createOrders = LinkedList<CreateOrderExecutor<*>>()

    // Return the modify-handlers
    private val modifyOrders = LinkedList<ModifyOrderExecutor<*>>()

    /**
     * Get the open order handlers
     */
    private fun <T : OrderExecutor<*>> List<T>.open() = filter { it.status.open }

    /**
     * Remove all handlers of closed orders, both create orders and modify orders
     */
    internal fun removeClosedOrders() {
        createOrders.removeIf { it.status.closed }
        modifyOrders.removeIf { it.status.closed }
    }

    /**
     * Return the order states of all handlers
     */
    internal val orderStates
        get() = createOrders.map { Pair(it.order, it.status) } + modifyOrders.map { Pair(it.order, it.status) }


    /**
     * Add a new [order] to the execution engine. Orders can only be processed if there is a corresponding handler
     * registered for the order class.
     */
    fun add(order: Order): Boolean {

        return when (val executor = getExecutor(order)) {
            is ModifyOrderExecutor -> modifyOrders.add(executor)
            is CreateOrderExecutor -> createOrders.add(executor)
        }

    }


    /**
     * Add all [orders] to the execution engine.
     * @see [add]
     */
    fun addAll(orders: List<Order>) {
        for (order in orders) add(order)
    }


    /**
     * Execute all the handlers of orders that are not yet closed based on the [event].
     *
     * Underlying Logic:
     *
     * 1. First process any open modify orders (like cancel or update)
     * 2. Then process any regular order but only if there is a price action in the event for the underlying asses
     */
    fun execute(event: Event): List<Execution> {
        val time = event.time

        // We always first execute modify orders. These are run even if there is
        // no price for the asset known
        for (handler in modifyOrders.open()) {
            val createHandler = createOrders.firstOrNull { it.order.id == handler.order.id }
            handler.execute(createHandler, time)
        }

        // Now run the create order commands
        val executions = mutableListOf<Execution>()
        val prices = event.prices
        for (handler in createOrders.open()) {
            val action = prices[handler.asset] ?: continue
            val pricing = pricingEngine.getPricing(action, time)
            val newExecutions = handler.execute(pricing, time)
            executions.addAll(newExecutions)
        }
        return executions
    }

    /**
     * Clear any state in the execution engine. All the pending open orders will be removed.
     */
    fun clear() {
        createOrders.clear()
        pricingEngine.clear()
    }

}


