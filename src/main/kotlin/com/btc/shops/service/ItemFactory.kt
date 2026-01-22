package com.btc.shops.service

import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.utils.item.Item
import com.typewritermc.engine.paper.utils.item.toItem
import org.bukkit.entity.Player

/**
 * Stores and captures custom items for later use in shops.
 */
@Singleton
class ItemFactory {
    private val customItems: MutableMap<String, Item> = mutableMapOf()

    fun get(id: String): Item? = customItems[id]

    fun register(id: String, item: Item) {
        customItems[id] = item
    }

    fun capture(id: String, player: Player) {
        register(id, player.inventory.itemInMainHand.clone().toItem())
    }
}

