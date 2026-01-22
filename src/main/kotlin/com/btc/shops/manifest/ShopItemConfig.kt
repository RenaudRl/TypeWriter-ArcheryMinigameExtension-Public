package com.btc.shops.manifest

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.utils.item.Item

/**
 * Configuration for a single item in a shop.
 */
@Serializable
data class ShopItemConfig(
    @Help("Item displayed for this shop entry.")
    val item: Var<Item> = ConstVar(Item.Empty),
    @Help("Custom display name for the item.")
    val name: String = "",
    @Help("Additional lore lines for this item.")
    val lore: List<String> = emptyList(),
    @Help("Criteria required for this item to be displayed.")
    val criteria: List<@Contextual Criteria> = emptyList(),
    @Help("Maximum amount a player can purchase.")
    val limitPerPlayer: Int = 0,
    @Help("Price and stock strategy.")
    val strategy: PriceStrategy = PriceStrategy()
)

