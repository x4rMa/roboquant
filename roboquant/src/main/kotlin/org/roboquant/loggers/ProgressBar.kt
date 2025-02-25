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

package org.roboquant.loggers

import org.roboquant.common.Timeframe
import java.time.Instant
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Display the progress of a run as a bar. Where often a progress bar works based on discrete steps, this
 * progress bar implementation works on the elapsed time. So how much time out the total time has passed, using the
 * time in the event as a reference.
 *
 * This implementation avoids unnecessary updates, so overhead is limited.
 */
internal class ProgressBar {

    var timeframe: Timeframe = Timeframe.INFINITE
    private var currentPercent = -1
    private val progressChar = getProgressChar()
    private var pre: String = ""
    private var post: String = ""
    private var nextUpdate = Instant.MIN
    private var lastOutput = ""

    /**
     * Start a progress bar for the [run] and [timeframe]
     */
    fun start(run: String, timeframe: Timeframe) {
        val runName = if (run.length > 30) run.substring(0, 27) + "..." else run
        currentPercent = -1
        post = "| $runName"
        pre = "${timeframe.toPrettyString()} | "
        nextUpdate = Instant.MIN
        lastOutput = ""
        this.timeframe = timeframe
    }

    /**
     * Update the progress bar, giving the provided [time]
     */
    fun update(time: Instant) {

        // Only if percentage changes we are going to refresh
        val totalDuration = timeframe.duration
        val currentDuration = Timeframe(timeframe.start, time).duration
        var percent = (currentDuration.seconds * 100.0 / totalDuration.seconds).roundToInt()
        percent = min(percent, 100)
        if (percent == currentPercent) return

        // Avoid updating the progress meter too often
        val now = Instant.now()
        if (now < nextUpdate) return
        nextUpdate = now.plusMillis(500)

        currentPercent = percent

        draw(percent)
    }

    private fun draw(percent: Int) {
        val sb = StringBuilder(100)
        sb.append('\r').append(pre)
        sb.append(String.format(Locale.ENGLISH, "%3d", percent)).append("% |")
        val filled = percent * TOTAL_BAR_LENGTH / 100
        repeat(TOTAL_BAR_LENGTH) {
            if (it <= filled) sb.append(progressChar) else sb.append(' ')
        }

        sb.append(post)
        if (percent == 100) sb.append("\n")
        val str = sb.toString()

        // Only update if there are some changes to the progress bar
        if (str != lastOutput) {
            print(str)
            lastOutput = str
            System.out.flush()
        }
    }

    /**
     * Signal that the current task is done, so the progress bar can show it has finished.
     */
    fun done() {
        if ((currentPercent < 100) && (currentPercent >= 0)) {
            draw(100)
            System.out.flush()
        }
    }

    private companion object {

        private const val TOTAL_BAR_LENGTH = 36

        private fun getProgressChar(): Char {
            return if (System.getProperty("os.name").startsWith("Win")) '=' else '█'
        }
    }
}
