package com.btc.shops.service

import com.btc.shops.manifest.PriceStrategy
import com.typewritermc.core.extension.annotations.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles stock mutations for shop items.
 */
@Singleton
class StockService {
    private val stocks = ConcurrentHashMap<String, Int>()

    private fun key(shopId: String, itemId: Int) = "$shopId:$itemId"

    fun getStock(shopId: String, itemId: Int, max: Int): Int =
        stocks.getOrPut(key(shopId, itemId)) { max }

    /** Deduct [amount] from [current] stock. */
    fun buy(current: Int, amount: Int): Int {
        require(amount >= 0) { "Amount must be positive" }
        check(current >= amount) { "Insufficient stock" }
        return current - amount
    }

    fun buy(shopId: String, itemId: Int, amount: Int, strategy: PriceStrategy): Int {
        val current = getStock(shopId, itemId, strategy.stockMax)
        val updated = buy(current, amount)
        stocks[key(shopId, itemId)] = updated
        return updated
    }

    /** Adds [amount] to [current] stock but not beyond [max]. */
    fun sell(current: Int, amount: Int, max: Int): Int {
        require(amount >= 0) { "Amount must be positive" }
        val result = current + amount
        return if (result > max) max else result
    }

    fun sell(shopId: String, itemId: Int, amount: Int, strategy: PriceStrategy): Int {
        val current = getStock(shopId, itemId, strategy.stockMax)
        val updated = sell(current, amount, strategy.stockMax)
        stocks[key(shopId, itemId)] = updated
        return updated
    }

    /** Resets stock back to its maximum value. */
    fun reset(max: Int): Int = max

    fun reset(shopId: String, itemId: Int, max: Int) {
        stocks[key(shopId, itemId)] = max
    }
}

