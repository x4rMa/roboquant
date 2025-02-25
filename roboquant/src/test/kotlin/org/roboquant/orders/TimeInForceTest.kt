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

package org.roboquant.orders

import kotlin.test.Test
import java.time.Instant
import kotlin.test.assertEquals

internal class TimeInForceTest {

    @Test
    fun test() {
        assertEquals("GTC", GTC().toString())
        assertEquals("FOK", FOK().toString())
        assertEquals("IOC", IOC().toString())
        assertEquals("DAY", DAY().toString())
        val now = Instant.now()
        assertEquals("GTD($now)", GTD(now).toString())
    }

}
