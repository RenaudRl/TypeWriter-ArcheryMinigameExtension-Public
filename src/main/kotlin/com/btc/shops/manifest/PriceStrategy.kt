package com.btc.shops.manifest

import com.typewritermc.core.extension.annotations.Help
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * Strategy containing all price and stock related settings.
 */
@Serializable
data class PriceStrategy(
    @Help("Base price when stock is full.")
    val price: Double = 0.0,
    @Help("Base sell price when stock is full.")
    val sellPrice: Double = 0.0,
    @Help("Minimum sell price limit.")
    val sellPriceMin: Double = 0.0,
    @Help("Maximum sell price limit.")
    val sellPriceMax: Double = Double.MAX_VALUE,
    @Help("Maximum stock available.")
    val stockMax: Int = 0,
    @Help("Minimum buy price limit.")
    val min: Double = 0.0,
    @Help("Maximum buy price limit.")
    val max: Double = Double.MAX_VALUE,
    @Help("Fraction of the maximum stock required to trigger a price change. Applies to buy and sell prices.")
    val step: Double = 0.1,
    @Help("Round result to the nearest multiple of this value. Set to 0 to disable rounding.")
    val rounding: Double = 0.01
) {
    fun calculateBuyPrice(stock: Int): Double =
        calculateDynamic(stock, price, min, max)

    fun calculateSellPrice(stock: Int): Double =
        calculateDynamic(stock, sellPrice, sellPriceMin, sellPriceMax)

    private fun calculateDynamic(stock: Int, base: Double, min: Double, max: Double): Double {
        if (stockMax <= 0) return round(base.coerceIn(min, max))
        val soldRatio = (stockMax - stock).toDouble() / stockMax.toDouble()
        val steppedRatio = if (step > 0) (soldRatio / step).toInt() * step else soldRatio
        val raw = base + (max - base) * steppedRatio
        val clamped = raw.coerceIn(min, max)
        return round(clamped)
    }

    private fun round(value: Double): Double {
        if (rounding <= 0) return value
        val factor = (value / rounding).roundToInt()
        return factor * rounding
    }
}

