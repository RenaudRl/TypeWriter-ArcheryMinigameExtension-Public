package btc.renaud.archery.util

import btc.renaud.archery.entry.Zone
import org.bukkit.Location
import kotlin.math.max
import kotlin.math.min

/** Returns true if the location lies within this zone. */
fun Zone.contains(location: Location): Boolean {
    val c1 = corner1.toLocation()
    val c2 = corner2.toLocation()
    if (location.world?.uid != c1.world.uid) return false

    val minX = min(c1.x, c2.x)
    val maxX = max(c1.x, c2.x)
    val minY = min(c1.y, c2.y)
    val maxY = max(c1.y, c2.y)
    val minZ = min(c1.z, c2.z)
    val maxZ = max(c1.z, c2.z)

    return location.x in minX..maxX &&
        location.y in minY..maxY &&
        location.z in minZ..maxZ
}
