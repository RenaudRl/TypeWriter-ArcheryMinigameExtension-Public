package btc.renaud.archery.placeholder

import btc.renaud.archery.entry.ArcheryGameDefinitionEntry
import btc.renaud.archery.entry.GameMode
import com.typewritermc.engine.paper.extensions.placeholderapi.PlaceholderHandler
import com.typewritermc.core.entries.Query
import org.bukkit.entity.Player
import com.typewritermc.engine.paper.entry.entries.get
import com.typewritermc.core.extension.annotations.Singleton
import btc.renaud.archery.entry.playerTopScore
import btc.renaud.archery.entry.topScore

/**
 * Provides PlaceholderAPI hooks for archery statistics.
 *
 * Supported placeholders:
 * - %typewriter_archery_top_<definitionId>_<mode>%: top score for a definition and mode
 * - %typewriter_archery_player_top_<definitionId>_<mode>%: player's top score for that definition and mode
 */
@Singleton
class ArcheryPlaceholders : PlaceholderHandler {
    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (!params.startsWith("archery_", ignoreCase = true)) return null
        val sub = params.substringAfter("archery_")
        return when {
            sub.startsWith("top_", ignoreCase = true) -> handleTop(sub.substringAfter("top_"))
            sub.startsWith("player_top_", ignoreCase = true) -> handlePlayer(player, sub.substringAfter("player_top_"))
            else -> null
        }
    }

    private fun handleTop(params: String): String? {
        val parts = params.split('_', limit = 2)
        if (parts.size != 2) return null
        val defId = parts[0]
        val mode = parts[1].toGameMode() ?: return null
        val def = Query.findById<ArcheryGameDefinitionEntry>(defId) ?: return null
        val artifact = def.gameStatArtifact.get() ?: return null
        return artifact.topScore(mode).toString()
    }

    private fun handlePlayer(player: Player?, params: String): String? {
        player ?: return null
        val parts = params.split('_', limit = 2)
        if (parts.size != 2) return null
        val defId = parts[0]
        val mode = parts[1].toGameMode() ?: return null
        val def = Query.findById<ArcheryGameDefinitionEntry>(defId) ?: return null
        val artifact = def.gameStatArtifact.get() ?: return null
        return artifact.playerTopScore(mode, player.uniqueId).toString()
    }

    private fun String.toGameMode(): GameMode? = try {
        GameMode.valueOf(this.uppercase())
    } catch (_: IllegalArgumentException) {
        null
    }
}


