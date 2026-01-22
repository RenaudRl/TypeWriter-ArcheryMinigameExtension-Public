package com.btc.shops.util

import com.btc.shops.manifest.ShopDefinitionEntry
import com.btc.shops.service.GuiService
import com.btc.shops.service.ItemFactory
import org.bukkit.entity.Player
import org.koin.java.KoinJavaComponent

/** Convenience helper to open a [ShopDefinitionEntry] for a [Player]. */
fun Player.openShop(definition: ShopDefinitionEntry, page: Int = 0) {
    val gui = KoinJavaComponent.get<GuiService>(GuiService::class.java)
    gui.open(this, definition, page)
}

/** Capture the item currently held in the main hand under the given [id]. */
fun Player.captureShopItem(id: String) {
    val factory = KoinJavaComponent.get<ItemFactory>(ItemFactory::class.java)
    factory.capture(id, this)
}

