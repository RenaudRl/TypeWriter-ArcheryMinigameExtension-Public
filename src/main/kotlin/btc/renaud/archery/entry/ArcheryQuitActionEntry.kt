package btc.renaud.archery.entry

import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import com.typewritermc.engine.paper.entry.entries.get
import btc.renaud.archery.interaction.ArcheryInteraction
import btc.renaud.archery.lobby.ArcheryLobbyManager

/** Action entry that removes a player from an archery arena. */
@Entry("archery_quit", "Leave an archery arena", Colors.RED, "mdi:exit-run")
class ArcheryQuitActionEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Arena to leave")
    val game: Ref<ArcheryGameDefinitionEntry> = emptyRef(),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val arena = game.get() ?: return
        val interaction = ArcheryInteraction.interactionFor(player)
        if (interaction != null) {
            interaction.handleQuit(player)
        } else {
            ArcheryLobbyManager.lobby(arena).leave(player)
        }
    }
}
