package com.btc.shops.service

import com.btc.shops.manifest.PriceStrategy
import kotlin.test.Test
import kotlin.test.assertEquals

class PriceServiceTest {
    private val service = PriceService()

    @Test
    fun buyPriceIncreasesWhenStockDrops() {
        val strategy = PriceStrategy(
            price = 100.0,
            min = 100.0,
            max = 200.0,
            step = 0.2,
            stockMax = 25
        )
        val priceFull = service.calculateBuyPrice(stock = 25, strategy = strategy)
        val priceAfterFive = service.calculateBuyPrice(stock = 20, strategy = strategy)
        assertEquals(100.0, priceFull)
        assertEquals(120.0, priceAfterFive)
    }

    @Test
    fun roundingIsApplied() {
        val strategy = PriceStrategy(
            price = 0.0,
            min = 0.0,
            max = 10.0,
            step = 0.0,
            rounding = 0.5,
            stockMax = 25
        )
        val price = service.calculateBuyPrice(stock = 19, strategy = strategy)
        assertEquals(2.5, price)
    }

    @Test
    fun sellPriceRespectsLimits() {
        val strategy = PriceStrategy(
            sellPrice = 50.0,
            sellPriceMin = 25.0,
            sellPriceMax = 75.0,
            step = 0.2,
            stockMax = 25
        )
        val priceFull = service.calculateSellPrice(stock = 25, strategy = strategy)
        val priceAfterFive = service.calculateSellPrice(stock = 20, strategy = strategy)
        assertEquals(50.0, priceFull)
        assertEquals(55.0, priceAfterFive)
    }

    @Test
    fun priceUpdatesAfterTransactions() {
        val stockService = StockService()
        val strategy = PriceStrategy(
            price = 100.0,
            min = 100.0,
            max = 200.0,
            step = 0.2,
            stockMax = 25
        )
        val initial = stockService.getStock("shop", 0, strategy.stockMax)
        val priceFull = service.calculateBuyPrice(initial, strategy)
        assertEquals(100.0, priceFull)

        stockService.buy("shop", 0, 5, strategy)
        val afterBuy = stockService.getStock("shop", 0, strategy.stockMax)
        val priceAfterBuy = service.calculateBuyPrice(afterBuy, strategy)
        assertEquals(120.0, priceAfterBuy)

        stockService.sell("shop", 0, 5, strategy)
        val afterSell = stockService.getStock("shop", 0, strategy.stockMax)
        val priceAfterSell = service.calculateBuyPrice(afterSell, strategy)
        assertEquals(100.0, priceAfterSell)
    }
}

