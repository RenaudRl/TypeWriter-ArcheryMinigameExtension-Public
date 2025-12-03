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
import btc.renaud.archery.lobby.ArcheryLobbyManager

/** Action entry that lets a player join a specific archery arena. */
@Entry("archery_join", "Join an archery arena", Colors.RED, "mdi:target")
class ArcheryJoinActionEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Arena to join")
    val game: Ref<ArcheryGameDefinitionEntry> = emptyRef(),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val arena = game.get() ?: return
        ArcheryLobbyManager.lobby(arena).join(player, context)
    }
}

