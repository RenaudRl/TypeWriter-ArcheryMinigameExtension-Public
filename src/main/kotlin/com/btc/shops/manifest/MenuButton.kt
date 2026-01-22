package com.btc.shops.manifest

import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.utils.item.Item
import kotlinx.serialization.Serializable

/**
 * Generic button configuration used in menus.
 */
@Serializable
data class MenuButton(
    @Help("Inventory slot used for this button.")
    val slot: Int = -1,
    @Help("Item displayed for the button.")
    val item: Var<Item> = ConstVar(Item.Empty),
    @Help("Display name of the button. Supports color codes and placeholders.")
    val name: String = "",
    @Help("Lore lines for the button. Placeholders: {amount}, {buy}, {sell}. Supports color codes, placeholders and multiline.")
    val lore: List<String> = emptyList()
)

