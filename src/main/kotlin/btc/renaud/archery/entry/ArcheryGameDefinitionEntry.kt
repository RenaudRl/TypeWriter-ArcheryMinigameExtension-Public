package btc.renaud.archery.entry

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Entry as TWEntry
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.ContentEditor
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.core.extension.annotations.Colored
import com.typewritermc.core.extension.annotations.MultiLine
import com.typewritermc.core.extension.annotations.Placeholder
import com.typewritermc.core.utils.point.Position
import com.typewritermc.engine.paper.content.modes.custom.PositionContentMode
import com.typewritermc.engine.paper.entry.ManifestEntry
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.WritableFactEntry
import com.typewritermc.engine.paper.utils.item.Item
import com.typewritermc.engine.paper.utils.item.SerializedItem
import org.bukkit.Material
import java.lang.reflect.Type

/**
 * Defines a playable archery arena and its settings. This manifest is used to
 * spawn arenas and configure game behaviour.
 */
@Tags("archery_game")
@Entry("archery_game_definition", "create archery arena", Colors.PURPLE, "mdi:target")
class ArcheryGameDefinitionEntry(
    override val id: String = "",
    override val name: String = "",
    @Placeholder
    @Colored
    @MultiLine
    @Help("Description displayed to players")
    val description: String = "",
    @Help("List of target positions (add multiple items, capture each in-game)")
    val targetPositions: List<TargetPosition> = emptyList(),
    @Help("Zones players must stand in to shoot")
    val shootingZones: List<Zone> = emptyList(),
    @Help("Material used for target blocks")
    val targetBlock: Material = Material.TARGET,
    @Help("Block placed when a target is hit")
    val hitBlock: Material = Material.EMERALD_BLOCK,
    @Help("Maximum number of active targets")
    val maxTargets: Int = 0,
    @Help("Cooldown in seconds before a target respawns")
    val targetRespawnCooldown: Long = 0,
    @Help("Cooldown in seconds before a shooting zone becomes available again")
    val shootingZoneCooldown: Long = 0,
    @Help("Order in which shooting zones are selected")
    val shootingZoneOrder: ShootingZoneOrder = ShootingZoneOrder.RANDOM,
    @Help("Teleport location for the lobby")
    @ContentEditor(PositionContentMode::class)
    val lobbyTeleport: Position = Position.ORIGIN,
    @Help("Radius players may move within the lobby")
    val lobbyRadius: Double = 5.0,
    @Help("Teleport location at game start")
    @ContentEditor(PositionContentMode::class)
    val startTeleport: Position = Position.ORIGIN,
    @Help("Teleport location at game end")
    @ContentEditor(PositionContentMode::class)
    val endTeleport: Position = Position.ORIGIN,
    @Help("Trigger executed when the game starts")
    val startTrigger: Ref<TriggerableEntry> = emptyRef(),
    @Help("Trigger executed when the game ends")
    val endTrigger: Ref<TriggerableEntry> = emptyRef(),
    @Help("Artifact storing persistent arena configuration")
    val arenaArtifact: Ref<ArenaArtifactEntry> = emptyRef(),
    @Help("Artifact storing temporary game state")
    val gameStateArtifact: Ref<GameStateArtifactEntry> = emptyRef(),
    @Help("Artifact storing persistent game statistics")
    val gameStatArtifact: Ref<GameStatArtifactEntry> = emptyRef(),
    @Help("Game mode to play")
    val mode: GameMode = GameMode.MAX_TARGET,
    @Help("Scoreboard display mode")
    val scoreboardMode: ScoreboardMode = ScoreboardMode.MULTI,
    @Help("Allow players to keep their own inventory without receiving a kit")
    val bringOwnItems: Boolean = false,
    @Help("Players have only one life")
    val hardcore: Boolean = false,
    @Help("Minimum number of players to start")
    val minPlayers: Int = 1,
    @Help("Maximum number of players allowed")
    val maxPlayers: Int = 16,
    @Help("Countdown in seconds before the game auto-starts")
    val autoStartCountdown: Int = 10,
    @Help("Cooldown in seconds before the lobby can start a new game")
    val lobbyCooldown: Int = 0,
    @Help("Time limit in seconds for TIME_ATTACK mode")
    val timeLimitSeconds: Int = 300,
    @Help("Item given to players as a bow")
    val bowItem: Item = SerializedItem(material = Material.BOW),
    @Help("Item given to players as arrows")
    val arrowItem: Item = SerializedItem(material = Material.ARROW),
    @Help("Optional fact kept in sync with each player's current score during a game")
    val scoreFact: Ref<WritableFactEntry> = emptyRef(),
    @Help("Customizable messages for lobby and game")
    val messages: Messages = Messages(),
) : ManifestEntry, TWEntry

/** Individual target position that must be captured in-game. */
@JsonAdapter(TargetPositionAdapter::class)
data class TargetPosition(
    @Help("Capture in-game to set where a target can spawn")
    @ContentEditor(PositionContentMode::class)
    val position: Position = Position.ORIGIN,
)

private class TargetPositionAdapter : JsonSerializer<TargetPosition>, JsonDeserializer<TargetPosition> {
    override fun serialize(src: TargetPosition, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonObject().apply { add("position", context.serialize(src.position, Position::class.java)) }
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): TargetPosition {
        if (json.isJsonObject) {
            val obj = json.asJsonObject
            if (obj.has("position")) {
                val pos = context.deserialize<Position>(obj.get("position"), Position::class.java)
                return TargetPosition(pos ?: Position.ORIGIN)
            }
            if (obj.has("world")) {
                val pos = context.deserialize<Position>(obj, Position::class.java)
                return TargetPosition(pos ?: Position.ORIGIN)
            }
        }
        val pos = context.deserialize<Position>(json, Position::class.java)
        return TargetPosition(pos ?: Position.ORIGIN)
    }
}

/** Represents a cuboid region defined by two corner positions. */
data class Zone(
    @Help("First corner of the cuboid zone")
    @ContentEditor(PositionContentMode::class)
    val corner1: Position = Position.ORIGIN,
    @Help("Second corner of the cuboid zone")
    @ContentEditor(PositionContentMode::class)
    val corner2: Position = Position.ORIGIN,
    @Help("Player spawn location inside the zone")
    @ContentEditor(PositionContentMode::class)
    val spawn: Position = Position.ORIGIN,
)

/** Order in which players cycle through shooting zones. */
enum class ShootingZoneOrder { RANDOM, SEQUENTIAL }

/** Available game modes. */
enum class GameMode {
    /** Players must hit a fixed number of targets as fast as possible. */
    MAX_TARGET,

    /** Players have limited time to hit as many targets as they can. */
    TIME_ATTACK
}

/** Scoreboard display options. */
enum class ScoreboardMode { NONE, SOLO, MULTI }

/** Holds configurable messages. Supports placeholders, color codes and multiline text. */
data class Messages(
    @Placeholder
    @Colored
    @MultiLine
    val lobbyFull: String = "&cLobby is full",
    @Placeholder
    @Colored
    @MultiLine
    @Help("Placeholders: %arena%,%count%,%min%")
    val joined: String = "&aJoined %arena% (%count%/%min%)",
    @Placeholder
    @Colored
    @MultiLine
    val left: String = "&cLeft %arena%",
    @Placeholder
    @Colored
    @MultiLine
    @Help("Placeholders: %seconds%")
    val countdown: String = "&eGame starting in %seconds%...",
    @Placeholder
    @Colored
    @MultiLine
    val starting: String = "&aGame starting!",
    @Placeholder
    @Colored
    @Help("Placeholders: %player%,%score%")
    val scoreboardLineMaxTarget: String = "%player%: %score%",
    @Placeholder
    @Colored
    @Help("Placeholders: %player%,%score%")
    val scoreboardLineTimeAttack: String = "%player%: %score%"
)


