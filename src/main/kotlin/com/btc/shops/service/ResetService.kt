package com.btc.shops.service

import com.btc.shops.manifest.ResetPolicy
import com.btc.shops.manifest.ShopDefinitionEntry
import com.typewritermc.core.extension.annotations.Singleton
import java.time.*
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Singleton
class ResetService {
    private val nextReset = ConcurrentHashMap<String, Long>()

    fun shouldReset(definition: ShopDefinitionEntry): Boolean {
        val now = System.currentTimeMillis()
        val next = nextReset.getOrPut(definition.id) { calculateNext(now, definition) }
        return if (now >= next) {
            nextReset[definition.id] = calculateNext(now, definition)
            true
        } else {
            false
        }
    }

    fun remaining(definition: ShopDefinitionEntry): Long {
        val now = System.currentTimeMillis()
        val next = nextReset.getOrPut(definition.id) { calculateNext(now, definition) }
        return (next - now).coerceAtLeast(0)
    }

    private fun calculateNext(now: Long, definition: ShopDefinitionEntry): Long {
        val zone = ZoneId.systemDefault()
        val zoned = Instant.ofEpochMilli(now).atZone(zone)
        val hour = definition.resetHour.coerceIn(0, 23)
        val minute = definition.resetMinute.coerceIn(0, 59)

        return when (definition.resetPolicy) {
            ResetPolicy.INTERVAL -> now + TimeUnit.SECONDS.toMillis(definition.resetIntervalSeconds)
            ResetPolicy.DAILY -> {
                val base = zoned.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                val next = if (base.toInstant().toEpochMilli() > now) base else base.plusDays(1)
                next.toInstant().toEpochMilli()
            }
            ResetPolicy.WEEKLY -> {
                val base = zoned.with(TemporalAdjusters.nextOrSame(definition.resetDayOfWeek))
                    .withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                val next = if (base.toInstant().toEpochMilli() > now) base else base.plusWeeks(1)
                next.toInstant().toEpochMilli()
            }
            ResetPolicy.MONTHLY -> {
                val day = definition.resetDayOfMonth
                val base = zoned.withDayOfMonth(day.coerceIn(1, zoned.toLocalDate().lengthOfMonth()))
                    .withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                val next = if (base.toInstant().toEpochMilli() > now) base else base.plusMonths(1)
                    .withDayOfMonth(day.coerceIn(1, base.plusMonths(1).toLocalDate().lengthOfMonth()))
                next.toInstant().toEpochMilli()
            }
            else -> Long.MAX_VALUE
        }
    }
}

