package com.btc.shops.util

/**
 * Very small cron expression helper. It is intentionally tiny and only supports
 * returning the current time to keep the example lightweight.
 */
object CronUtil {
    fun next(pattern: String, now: Long = System.currentTimeMillis()): Long = now
}

