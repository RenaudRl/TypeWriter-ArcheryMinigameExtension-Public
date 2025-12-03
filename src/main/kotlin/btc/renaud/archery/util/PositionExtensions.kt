package btc.renaud.archery.util

import com.typewritermc.core.utils.point.Position
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import java.util.UUID

/** Converts a TypeWriter [Position] into a Bukkit [Location]. */
fun Position.toLocation(): Location {
    val worldId = world.identifier
    val world: World? = when {
        worldId.isBlank() -> {
            Bukkit.getLogger().warning("No world specified for position; defaulting to first loaded world")
            Bukkit.getWorlds().firstOrNull()
        }
        else -> runCatching { UUID.fromString(worldId) }.getOrNull()?.let { Bukkit.getWorld(it) }
            ?: Bukkit.getWorld(worldId)
    }

    requireNotNull(world) { "World $worldId not found" }

    return Location(world, x, y, z, yaw, pitch)
}

