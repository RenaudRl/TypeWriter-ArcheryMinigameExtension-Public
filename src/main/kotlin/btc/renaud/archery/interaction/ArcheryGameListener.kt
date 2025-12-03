package btc.renaud.archery.interaction

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin

private val plugin: Plugin = Bukkit.getPluginManager().getPlugin("TypeWriter")
    ?: error("TypeWriter plugin not found")

/** Global listener handling archery game events. */
object ArcheryGameListener : Listener {
    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        ArcheryInteraction.interactionFor(event.player)?.handleQuit(event.player)
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        ArcheryInteraction.interactionFor(event.player)?.enforceZone(event)
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val shooter = event.entity.shooter as? Player ?: return
        ArcheryInteraction.interactionFor(shooter)?.handleProjectile(event)
    }

    @EventHandler
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val shooter = event.entity.shooter as? Player ?: return
        ArcheryInteraction.interactionFor(shooter)?.recordShot(shooter)
    }
}

