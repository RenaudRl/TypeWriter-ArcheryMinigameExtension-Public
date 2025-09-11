package btc.renaud.archery.util

import org.bukkit.ChatColor
import org.bukkit.entity.Player

/**
 * Sends a formatted message to the player.
 * Supports color codes using '&', placeholders of the form %key%,
 * and multiline messages separated by '\n'.
 */
fun Player.sendFormatted(message: String, placeholders: Map<String, String> = emptyMap()) {
    var formatted = message
    placeholders.forEach { (key, value) ->
        formatted = formatted.replace("%$key%", value)
    }
    val colored = ChatColor.translateAlternateColorCodes('&', formatted)
    colored.split("\n").forEach { line -> this.sendMessage(line) }
}
