package com.btc.shops.service

import com.btc.shops.manifest.ShopDefinitionEntry
import com.typewritermc.core.entries.Query
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.extensions.placeholderapi.PlaceholderHandler
import org.bukkit.entity.Player

@Singleton
class PlaceholderService(
    private val priceService: PriceService,
    private val stockService: StockService,
    private val resetService: ResetService
) : PlaceholderHandler {

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        val first = params.indexOf(':')
        val second = params.indexOf(':', first + 1)
        if (first == -1 || second == -1) return null

        val prefix = params.substring(0, first)
        if (prefix != "shop") return null

        val shopKey = params.substring(first + 1, second)
        val shop =
            Query.findByName<ShopDefinitionEntry>(shopKey)
                ?: Query.findById<ShopDefinitionEntry>(shopKey)
                ?: return null

        val rest = params.substring(second + 1)
        val colonIndex = rest.indexOf(':')
        return if (colonIndex != -1 && rest.substring(0, colonIndex).toIntOrNull() != null) {
            handleItemPlaceholder(shop, rest.substring(0, colonIndex), rest.substring(colonIndex + 1))
        } else {
            handleShopPlaceholder(shop, rest)
        }
    }

    private fun handleShopPlaceholder(shop: ShopDefinitionEntry, key: String): String? {
        val (identifier, format) = key.split('_', limit = 2).let { it[0] to it.getOrNull(1) }
        return when (identifier) {
            "name" -> shop.name
            "cooldown" -> formatDuration(resetService.remaining(shop), format)
            "entryid" -> shop.id
            else -> null
        }
    }

    private fun handleItemPlaceholder(
        shop: ShopDefinitionEntry,
        itemIndexRaw: String,
        key: String
    ): String? {
        val index = itemIndexRaw.toIntOrNull() ?: return null
        val config = shop.items.getOrNull(index) ?: return null
        val stock = stockService.getStock(shop.id, index, config.strategy.stockMax)

        return when (key) {
            "buy_price" -> priceService.calculateBuyPrice(stock, config.strategy).toString()
            "sell_price" -> priceService.calculateSellPrice(stock, config.strategy).toString()
            "stock" -> stock.toString()
            "stock_max" -> config.strategy.stockMax.toString()
            else -> null
        }
    }

}

internal fun formatDuration(millis: Long, pattern: String? = null): String {
    val totalSeconds = millis / 1000
    val days = totalSeconds / 86_400
    val hours = (totalSeconds % 86_400) / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60

    val defaultPattern = "{d}j:{h}h:{m}m:{s}s"
    val fmt = pattern ?: defaultPattern

    fun Long.pad() = toString().padStart(2, '0')

    return fmt
        .replace("{d}", days.toString())
        .replace("{h}", hours.pad())
        .replace("{m}", minutes.pad())
        .replace("{s}", seconds.pad())
}

