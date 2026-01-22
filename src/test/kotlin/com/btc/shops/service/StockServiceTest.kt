package com.btc.shops.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StockServiceTest {
    private val service = StockService()

    @Test
    fun buyReducesStock() {
        val newStock = service.buy(current = 10, amount = 3)
        assertEquals(7, newStock)
    }

    @Test
    fun buyFailsWhenInsufficient() {
        assertFailsWith<IllegalStateException> {
            service.buy(current = 2, amount = 5)
        }
    }

    @Test
    fun sellRespectsMax() {
        val newStock = service.sell(current = 10, amount = 5, max = 12)
        assertEquals(12, newStock)
    }

    @Test
    fun resetUsesMax() {
        val stock = service.reset(max = 15)
        assertEquals(15, stock)
    }

    @Test
    fun trackedBuyAndSellRespectLimits() {
        val strategy = com.btc.shops.manifest.PriceStrategy(stockMax = 10)
        val afterBuy = service.buy("shop", 0, 5, strategy)
        assertEquals(5, afterBuy)
        val afterSell = service.sell("shop", 0, 10, strategy)
        assertEquals(10, afterSell)
    }

    @Test
    fun trackedBuyFailsWhenStockExceeded() {
        val strategy = com.btc.shops.manifest.PriceStrategy(stockMax = 5)
        assertFailsWith<IllegalStateException> {
            service.buy("shop2", 0, 6, strategy)
        }
    }
}

