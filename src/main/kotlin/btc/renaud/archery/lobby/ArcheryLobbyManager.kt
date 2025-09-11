package btc.renaud.archery.lobby

import btc.renaud.archery.entry.ArcheryGameDefinitionEntry
import btc.renaud.archery.interaction.ArcheryStartTrigger
import btc.renaud.archery.util.toLocation
import btc.renaud.archery.util.sendFormatted
import btc.renaud.archery.entry.setRunning
import btc.renaud.archery.entry.storeInventory
import com.typewritermc.engine.paper.entry.entries.get
import com.typewritermc.core.interaction.InteractionContext
import com.typewritermc.engine.paper.entry.triggerFor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.GameMode as BukkitGameMode
import java.util.UUID

private val plugin: Plugin = Bukkit.getPluginManager().getPlugin("TypeWriter")
    ?: error("TypeWriter plugin not found")

object ArcheryLobbyManager {
    private val lobbies = mutableMapOf<String, Lobby>()
    internal val playerLobbies = mutableMapOf<UUID, Lobby>()

    init {
        Bukkit.getPluginManager().registerEvents(LobbyListener, plugin)
    }

    fun lobby(definition: ArcheryGameDefinitionEntry): Lobby =
        lobbies.getOrPut(definition.id) { Lobby(definition) }

    private object LobbyListener : Listener {
        @EventHandler
        fun onMove(event: PlayerMoveEvent) {
            playerLobbies[event.player.uniqueId]?.enforceRadius(event)
        }
    }
}

class Lobby(private val definition: ArcheryGameDefinitionEntry) {
    private val players = mutableSetOf<Player>()
    private val originalGamemode = mutableMapOf<UUID, BukkitGameMode>()
    private val originalLocation = mutableMapOf<UUID, Location>()
    private val savedInventories = mutableMapOf<UUID, Array<ItemStack?>>()
    private val lobbyLocation: Location = definition.lobbyTeleport.toLocation()
    private val radius: Double = definition.lobbyRadius
    private var countdownTask: BukkitRunnable? = null
    private var cooldownEnd: Long = 0

    fun join(player: Player, context: InteractionContext) {
        runSync {
            if (players.size >= definition.maxPlayers) {
                player.sendFormatted(definition.messages.lobbyFull, mapOf("arena" to definition.name))
                return@runSync
            }
            players += player
            originalGamemode[player.uniqueId] = player.gameMode
            originalLocation[player.uniqueId] = player.location
            player.gameMode = BukkitGameMode.SPECTATOR
            player.teleport(lobbyLocation)
            ArcheryLobbyManager.playerLobbies[player.uniqueId] = this
            player.sendFormatted(
                definition.messages.joined,
                mapOf(
                    "arena" to definition.name,
                    "count" to players.size.toString(),
                    "min" to definition.minPlayers.toString()
                )
            )
            if (canStart() && cooldownComplete()) startCountdown(context)
        }
    }

    fun leave(player: Player) {
        runSync {
            players -= player
            ArcheryLobbyManager.playerLobbies.remove(player.uniqueId)
            player.teleport(originalLocation.remove(player.uniqueId) ?: lobbyLocation)
            player.gameMode = originalGamemode.remove(player.uniqueId) ?: BukkitGameMode.SURVIVAL
            savedInventories.remove(player.uniqueId)?.let { player.inventory.contents = it }
            player.sendFormatted(
                definition.messages.left,
                mapOf("arena" to definition.name)
            )
            if (!canStart()) cancelCountdown()
        }
    }

    private fun runSync(block: () -> Unit) {
        if (Bukkit.isPrimaryThread()) {
            block()
        } else {
            Bukkit.getScheduler().runTask(plugin, block)
        }
    }

    private fun canStart(): Boolean = players.size >= definition.minPlayers
    private fun cooldownComplete(): Boolean = System.currentTimeMillis() >= cooldownEnd

    private fun startCountdown(context: InteractionContext) {
        var remaining = definition.autoStartCountdown
        countdownTask = object : BukkitRunnable() {
            override fun run() {
                if (remaining <= 0) {
                    cancel()
                    startGame(context)
                    return
                }
                broadcast(
                    definition.messages.countdown,
                    mapOf("seconds" to remaining.toString())
                )
                remaining--
            }
        }.also { it.runTaskTimer(plugin, 20L, 20L) }
    }

    private fun startGame(context: InteractionContext) {
        broadcast(definition.messages.starting)
        val state = definition.gameStateArtifact.get()
        state?.setRunning(true)
        val startLoc = definition.startTeleport.toLocation()
        val inventories: Map<UUID, Array<ItemStack?>> = if (definition.bringOwnItems) {
            emptyMap()
        } else {
            players.associate { p ->
                val inv = p.inventory.contents.clone()
                savedInventories[p.uniqueId] = inv
                state?.storeInventory(p.uniqueId, inv)
                p.inventory.clear()
                p.inventory.addItem(
                    definition.bowItem.build(p, context),
                    definition.arrowItem.build(p, context)
                )
                p.uniqueId to inv
            }
        }
        players.forEach { p ->
            p.teleport(startLoc)
            p.gameMode = BukkitGameMode.SURVIVAL
            ArcheryLobbyManager.playerLobbies.remove(p.uniqueId)
        }
        definition.startTrigger.triggerFor(players.first(), context)
        val trigger = ArcheryStartTrigger(
            definition.id,
            0,
            players.toList(),
            definition.endTeleport.toLocation(),
            inventories,
            state,
            scoreboardMode = definition.scoreboardMode,
            mode = definition.mode,
            timeLimit = java.time.Duration.ofSeconds(definition.timeLimitSeconds.toLong()),
            definition = definition,
            manageInventory = !definition.bringOwnItems
        )
        trigger.triggerFor(players.first(), context)
        cooldownEnd = System.currentTimeMillis() + definition.lobbyCooldown * 1000
        players.clear()
        countdownTask = null
    }

    private fun cancelCountdown() {
        countdownTask?.cancel()
        countdownTask = null
    }

    private fun broadcast(message: String, placeholders: Map<String, String> = emptyMap()) {
        players.forEach { it.sendFormatted(message, placeholders) }
    }

    fun enforceRadius(event: PlayerMoveEvent) {
        if (!players.contains(event.player)) return
        val to = event.to ?: return
        if (to.distanceSquared(lobbyLocation) > radius * radius) {
            event.player.teleport(lobbyLocation)
        }
    }
}

