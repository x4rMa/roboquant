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

package org.roboquant.strategies

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class RatingTest {

    @Test
    fun isPositive() {
        val rating = Rating.BUY
        assertTrue(rating.isPositive)
    }

    @Test
    fun isNegative() {
        val rating = Rating.SELL
        assertTrue(rating.isNegative)
    }

    @Test
    fun conflicts() {
        val rating = Rating.SELL
        assertTrue(rating.conflicts(Rating.BUY))
    }

    @Test
    fun direction() {
        assertEquals(1, Rating.BUY.direction)
        assertEquals(1, Rating.OUTPERFORM.direction)
        assertEquals(0, Rating.HOLD.direction)
        assertEquals(-1, Rating.SELL.direction)
    }

    @Test
    fun getValue() {
        val rating = Rating.SELL
        assertEquals(-2, rating.value)
    }
}
