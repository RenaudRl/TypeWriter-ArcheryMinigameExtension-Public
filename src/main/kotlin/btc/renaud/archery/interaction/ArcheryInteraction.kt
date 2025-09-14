package btc.renaud.archery.interaction

import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.core.interaction.Interaction
import com.typewritermc.core.interaction.InteractionContext
import com.typewritermc.core.utils.ok
import com.typewritermc.engine.paper.entry.entries.Event
import com.typewritermc.engine.paper.entry.entries.EventTrigger
import com.typewritermc.engine.paper.entry.triggerFor
import com.typewritermc.engine.paper.interaction.TriggerContinuation
import com.typewritermc.engine.paper.interaction.TriggerHandler
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import btc.renaud.archery.entry.GameMode
import btc.renaud.archery.entry.GameStateArtifactEntry
import btc.renaud.archery.entry.ArcheryGameDefinitionEntry
import btc.renaud.archery.entry.ScoreboardMode
import btc.renaud.archery.entry.Zone
import btc.renaud.archery.entry.ShootingZoneOrder
import btc.renaud.archery.entry.restoreInventory
import btc.renaud.archery.entry.setRunning
import btc.renaud.archery.entry.GameStatArtifactEntry
import btc.renaud.archery.entry.recordScore
import btc.renaud.archery.util.toLocation
import btc.renaud.archery.util.contains
import btc.renaud.archery.util.formatMessage
import com.typewritermc.engine.paper.entry.entries.get

private val plugin: Plugin = Bukkit.getPluginManager().getPlugin("TypeWriter")
    ?: error("TypeWriter plugin not found")

/**
 * Manages an archery game interaction. The interaction keeps track of players,
 * scores and remaining time. When the timer expires a stop trigger is fired
 * to terminate the game.
 */
class ArcheryInteraction(
    val arena: String,
    initialPlayers: List<Player>,
    override val context: InteractionContext,
    override val priority: Int,
    val eventTriggers: List<EventTrigger>,
    var timeRemaining: Duration,
    private val endTeleport: Location,
    private val initialInventories: Map<UUID, Array<ItemStack?>>, 
    private val stateArtifact: GameStateArtifactEntry?,
    private val scoreboardMode: ScoreboardMode,
    private val manageInventory: Boolean,
    private val mode: GameMode,
    private val definition: ArcheryGameDefinitionEntry,
) : Interaction {
    private val logger = LoggerFactory.getLogger("ArcheryInteraction")
    private val players = initialPlayers.toMutableList()
    private val scores = initialPlayers.associate { it.uniqueId to 0 }.toMutableMap()
    private val shots = initialPlayers.associate { it.uniqueId to 0 }.toMutableMap()
    private val zoneList: List<Zone> = when (definition.shootingZoneOrder) {
        ShootingZoneOrder.RANDOM -> definition.shootingZones.shuffled()
        ShootingZoneOrder.SEQUENTIAL -> definition.shootingZones
    }
    private val spawnLocations: List<Location> = zoneList.map { it.spawn.toLocation() }
    private val playerZones = mutableMapOf<UUID, Int>()
    private val targets = mutableSetOf<Block>()
    private val statArtifact: GameStatArtifactEntry? = definition.gameStatArtifact.get()
    private var startTime: Long = 0
    private var scoreboard: Scoreboard? = null
    private var objective: Objective? = null
    private val soloObjectives = mutableMapOf<UUID, Objective>()
    private val playerLines = mutableMapOf<UUID, String>()
    private val respawnTasks = mutableSetOf<BukkitTask>()

    private fun formatLine(player: Player, score: Int): String {
        val template = when (mode) {
            GameMode.MAX_TARGET -> definition.messages.scoreboardLineMaxTarget
            GameMode.TIME_ATTACK -> definition.messages.scoreboardLineTimeAttack
        }
        return formatMessage(
            template,
            mapOf(
                "player" to player.name,
                "score" to score.toString(),
                "max" to definition.maxTargets.toString(),
                "shots" to (shots[player.uniqueId] ?: 0).toString()
            )
        )
    }

    private fun runSync(block: () -> Unit) {
        if (Bukkit.isPrimaryThread()) block() else Bukkit.getScheduler().runTask(plugin, block)
    }

    private fun addScore(player: Player, points: Int) {
        val newScore = (scores[player.uniqueId] ?: 0) + points
        scores[player.uniqueId] = newScore
        if (scoreboardMode != ScoreboardMode.NONE) {
            runSync {
                when (scoreboardMode) {
                    ScoreboardMode.MULTI -> {
                        val obj = objective ?: return@runSync
                        playerLines.remove(player.uniqueId)?.let { obj.scoreboard?.resetScores(it) }
                        val line = formatLine(player, newScore)
                        playerLines[player.uniqueId] = line
                        obj.getScore(line).score = newScore
                    }
                    ScoreboardMode.SOLO -> {
                        val obj = soloObjectives[player.uniqueId] ?: return@runSync
                        playerLines.remove(player.uniqueId)?.let { obj.scoreboard?.resetScores(it) }
                        val line = formatLine(player, newScore)
                        playerLines[player.uniqueId] = line
                        obj.getScore(line).score = newScore
                    }
                    else -> {}
                }
            }
        }
        if (mode == GameMode.MAX_TARGET && definition.maxTargets > 0 && newScore >= definition.maxTargets) {
            ArcheryStopTrigger.triggerFor(player, context)
        }
    }

    fun recordShot(player: Player) {
        shots[player.uniqueId] = (shots[player.uniqueId] ?: 0) + 1
        if (scoreboardMode != ScoreboardMode.NONE) {
            addScore(player, 0)
        }
    }

    override suspend fun initialize(): Result<Unit> {
        ArcheryGameListener // ensure listener loaded
        logger.info("Starting archery game in arena {}", arena)
        startTime = System.currentTimeMillis()
        players.forEach { p -> interactionPlayers[p.uniqueId] = this }
        spawnTargets()
        assignZones()
        if (scoreboardMode != ScoreboardMode.NONE) {
            runSync {
                val manager = Bukkit.getScoreboardManager()
                when (scoreboardMode) {
                    ScoreboardMode.MULTI -> {
                        val board = manager.newScoreboard
                        val obj = board.registerNewObjective(
                            "archery",
                            "dummy",
                            Component.text("Time: ${timeRemaining.seconds}s")
                        )
                        obj.displaySlot = DisplaySlot.SIDEBAR
                        players.forEach { p ->
                            p.scoreboard = board
                            val line = formatLine(p, 0)
                            playerLines[p.uniqueId] = line
                            obj.getScore(line).score = 0
                        }
                        scoreboard = board
                        objective = obj
                    }
                    ScoreboardMode.SOLO -> {
                        players.forEach { p ->
                            val board = manager.newScoreboard
                            val obj = board.registerNewObjective(
                                "archery",
                                "dummy",
                                Component.text("Time: ${timeRemaining.seconds}s")
                            )
                            obj.displaySlot = DisplaySlot.SIDEBAR
                            val line = formatLine(p, 0)
                            playerLines[p.uniqueId] = line
                            obj.getScore(line).score = 0
                            p.scoreboard = board
                            soloObjectives[p.uniqueId] = obj
                        }
                    }
                    else -> {}
                }
            }
        }
        return ok(Unit)
    }

    override suspend fun tick(deltaTime: Duration) {
        timeRemaining = timeRemaining.minus(deltaTime)
        if (scoreboardMode != ScoreboardMode.NONE) {
            runSync {
                when (scoreboardMode) {
                    ScoreboardMode.MULTI -> {
                        objective?.displayName(Component.text("Time: ${timeRemaining.seconds}s"))
                        players.forEach { p ->
                            val score = scores[p.uniqueId] ?: 0
                            val line = playerLines[p.uniqueId] ?: return@forEach
                            objective?.getScore(line)?.score = score
                        }
                    }
                    ScoreboardMode.SOLO -> {
                        players.forEach { p ->
                            val obj = soloObjectives[p.uniqueId] ?: return@forEach
                            obj.displayName(Component.text("Time: ${timeRemaining.seconds}s"))
                            val score = scores[p.uniqueId] ?: 0
                            val line = playerLines[p.uniqueId] ?: return@forEach
                            obj.getScore(line).score = score
                        }
                    }
                    else -> {}
                }
            }
        }
        if (timeRemaining.isZero || timeRemaining.isNegative) {
            players.firstOrNull()?.let { player ->
                ArcheryStopTrigger.triggerFor(player, context)
            }
        }
    }

    override suspend fun teardown() {
        logger.info("Archery game in arena {} ended", arena)
        val restored = if (manageInventory) {
            players.associateWith { p ->
                stateArtifact?.restoreInventory(p.uniqueId) ?: initialInventories[p.uniqueId]
            }
        } else emptyMap()
        runSync {
            objective?.unregister()
            soloObjectives.values.forEach { it.unregister() }
            soloObjectives.clear()
            respawnTasks.forEach { it.cancel() }
            respawnTasks.clear()
            players.forEach { p ->
                if (manageInventory) {
                    p.inventory.clear()
                    restored[p]?.let {
                        p.inventory.contents = it
                        p.updateInventory()
                    }
                }
                p.teleport(endTeleport)
                if (scoreboardMode != ScoreboardMode.NONE) {
                    p.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
                }
                interactionPlayers.remove(p.uniqueId)
            }
            targets.forEach { it.type = Material.AIR }
            targets.clear()
            playerLines.clear()
        }
        players.forEach { p -> definition.endTrigger.triggerFor(p, context) }
        statArtifact?.let { artifact ->
            scores.forEach { (uuid, score) ->
                artifact.recordScore(mode, uuid, score)
            }
        }
        stateArtifact?.setRunning(false)
    }

    private fun spawnTargets() {
        runSync {
            definition.targetPositions.forEach { pos ->
                val block = pos.toLocation().block
                block.type = definition.targetBlock
                targets += block
            }
        }
    }

    private fun assignZones() {
        if (zoneList.isEmpty()) return
        runSync {
            players.forEachIndexed { index, player ->
                val zoneIndex = index % spawnLocations.size
                playerZones[player.uniqueId] = zoneIndex
                player.teleport(spawnLocations[zoneIndex])
            }
        }
    }

    fun enforceZone(event: PlayerMoveEvent) {
        val zoneIndex = playerZones[event.player.uniqueId] ?: return
        val zone = zoneList.getOrNull(zoneIndex) ?: return
        val to = event.to ?: return
        if (zone.contains(to)) return

        val c1 = zone.corner1.toLocation()
        val c2 = zone.corner2.toLocation()
        val minX = min(c1.x, c2.x)
        val maxX = max(c1.x, c2.x)
        val minY = min(c1.y, c2.y)
        val maxY = max(c1.y, c2.y)
        val minZ = min(c1.z, c2.z)
        val maxZ = max(c1.z, c2.z)

        val clamped = to.clone().apply {
            x = x.coerceIn(minX, maxX)
            y = y.coerceIn(minY, maxY)
            z = z.coerceIn(minZ, maxZ)
        }
        event.player.teleport(clamped)
    }

    fun handleProjectile(event: ProjectileHitEvent) {
        val player = event.entity.shooter as? Player ?: return
        val block = event.hitBlock ?: return
        if (!targets.contains(block)) return
        if (mode == GameMode.MAX_TARGET) {
            addScore(player, 1)
        } else {
            val face = event.hitBlockFace ?: return
            val arrowVec = event.entity.location.toVector()
            val center = block.location.toVector().add(Vector(0.5, 0.5, 0.5))
            val diff = arrowVec.subtract(center)
            val distanceSquared = when {
                face.modX != 0 -> diff.y * diff.y + diff.z * diff.z
                face.modY != 0 -> diff.x * diff.x + diff.z * diff.z
                else -> diff.x * diff.x + diff.y * diff.y
            }
            val points = when {
                distanceSquared < 0.015625 -> 3 // within 0.125 blocks of center
                distanceSquared < 0.0625 -> 2   // within 0.25 blocks of center
                else -> 1
            }
            addScore(player, points)
        }
        runSync {
            block.type = definition.hitBlock
            var task: BukkitTask? = null
            task = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                block.type = definition.targetBlock
                task?.let { respawnTasks.remove(it) }
            }, definition.targetRespawnCooldown * 20)
            respawnTasks += task
        }
        teleportNextZone(player)
    }

    private fun teleportNextZone(player: Player) {
        if (spawnLocations.size <= 1) return
        val current = playerZones[player.uniqueId] ?: return
        val next = (current + 1) % spawnLocations.size
        playerZones[player.uniqueId] = next
        runSync { player.teleport(spawnLocations[next]) }
    }

    fun handleQuit(player: Player) {
        players.remove(player)
        playerZones.remove(player.uniqueId)
        val restored = if (manageInventory) {
            stateArtifact?.restoreInventory(player.uniqueId) ?: initialInventories[player.uniqueId]
        } else null
        runSync {
            if (scoreboardMode == ScoreboardMode.MULTI) {
                playerLines.remove(player.uniqueId)?.let { line ->
                    objective?.scoreboard?.resetScores(line)
                }
            } else if (scoreboardMode == ScoreboardMode.SOLO) {
                playerLines.remove(player.uniqueId)
                soloObjectives.remove(player.uniqueId)?.unregister()
            } else {
                playerLines.remove(player.uniqueId)
            }
            if (manageInventory) {
                player.inventory.clear()
                restored?.let {
                    player.inventory.contents = it
                    player.updateInventory()
                }
            }
            player.teleport(endTeleport)
            if (scoreboardMode != ScoreboardMode.NONE) player.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        }
        interactionPlayers.remove(player.uniqueId)
        if (players.isEmpty()) {
            definition.endTrigger.triggerFor(player, context)
            // Ensure the interaction stops when no participants remain
            ArcheryStopTrigger.triggerFor(player, context)
        }
    }

    companion object {
        private val interactionPlayers = mutableMapOf<UUID, ArcheryInteraction>()
        fun interactionFor(player: Player): ArcheryInteraction? = interactionPlayers[player.uniqueId]
    }
}

/** Trigger used to start an archery interaction. */
data class ArcheryStartTrigger(
    val arena: String,
    val priority: Int,
    val participants: List<Player>,
    val endTeleport: Location,
    val initialInventories: Map<UUID, Array<ItemStack?>> = emptyMap(),
    val stateArtifact: GameStateArtifactEntry? = null,
    val eventTriggers: List<EventTrigger> = emptyList(),
    val scoreboardMode: ScoreboardMode = ScoreboardMode.NONE,
    val manageInventory: Boolean = true,
    val mode: GameMode = GameMode.MAX_TARGET,
    val timeLimit: Duration = Duration.ofMinutes(5),
    val definition: ArcheryGameDefinitionEntry
) : EventTrigger {
    override val id: String = "renaud.archery.start"
}

/** Trigger used to stop an archery interaction. */
data object ArcheryStopTrigger : EventTrigger {
    override val id: String = "renaud.archery.stop"
}

/** Handles start and stop triggers for the archery interaction. */
@Singleton
class ArcheryTriggerHandler : TriggerHandler {
    override suspend fun trigger(event: Event, currentInteraction: Interaction?): TriggerContinuation {
        if (ArcheryStopTrigger in event && currentInteraction is ArcheryInteraction) {
            return TriggerContinuation.Multi(
                TriggerContinuation.EndInteraction,
                TriggerContinuation.Append(
                    Event(event.player, currentInteraction.context, currentInteraction.eventTriggers)
                )
            )
        }
        return tryStart(event)
    }

    private fun tryStart(event: Event): TriggerContinuation {
        val trigger = event.triggers.filterIsInstance<ArcheryStartTrigger>().maxByOrNull { it.priority }
            ?: return TriggerContinuation.Nothing

        return TriggerContinuation.StartInteraction(
            ArcheryInteraction(
                trigger.arena,
                trigger.participants,
                event.context,
                trigger.priority,
                trigger.eventTriggers,
                trigger.timeLimit,
                trigger.endTeleport,
                trigger.initialInventories,
                trigger.stateArtifact,
                trigger.scoreboardMode,
                trigger.manageInventory,
                trigger.mode,
                trigger.definition
            )
        )
    }
}

