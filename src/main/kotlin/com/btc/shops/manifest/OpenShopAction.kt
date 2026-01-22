package com.btc.shops.manifest

import com.btc.shops.service.GuiService
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Action opening a shop for a player.
 */
@Tags("shop_action")
@Entry("open_shop", "Open a shop for a player", Colors.YELLOW, "mdi:cart")
class OpenShopAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Shop definition to open for the player.")
    val shop: Ref<ShopDefinitionEntry> = emptyRef()
) : ActionEntry, KoinComponent {
    private val guiService: GuiService by inject()

    override fun ActionTrigger.execute() {
        val definition = shop.get() ?: return
        guiService.open(player, definition, delayTicks = 3L)
    }
}

