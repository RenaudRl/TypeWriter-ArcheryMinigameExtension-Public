package com.btc.shops.service

import com.btc.shops.manifest.PriceStrategy
import com.typewritermc.core.extension.annotations.Singleton

/**
 * Calculates dynamic buy and sell prices based on configured strategies.
 */
@Singleton
class PriceService {
    fun calculateBuyPrice(stock: Int, strategy: PriceStrategy): Double {
        val clamped = stock.coerceIn(0, strategy.stockMax)
        return strategy.calculateBuyPrice(clamped)
    }

    fun calculateSellPrice(stock: Int, strategy: PriceStrategy): Double {
        val clamped = stock.coerceIn(0, strategy.stockMax)
        return strategy.calculateSellPrice(clamped)
    }
}

