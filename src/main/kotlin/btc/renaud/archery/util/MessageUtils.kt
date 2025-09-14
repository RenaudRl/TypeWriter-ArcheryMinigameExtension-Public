package btc.renaud.archery.util

import org.bukkit.ChatColor
import org.bukkit.entity.Player

/**
 * Applies placeholder replacement and color codes to the given message.
 */
fun formatMessage(message: String, placeholders: Map<String, String> = emptyMap()): String {
    var formatted = message
    placeholders.forEach { (key, value) ->
        formatted = formatted.replace("%$key%", value)
    }
    return ChatColor.translateAlternateColorCodes('&', formatted)
}

/**
 * Sends a formatted message to the player.
 * Supports color codes using '&', placeholders of the form %key%,
 * and multiline messages separated by '\n'.
 */
fun Player.sendFormatted(message: String, placeholders: Map<String, String> = emptyMap()) {
    val colored = formatMessage(message, placeholders)
    colored.split("\n").forEach { line -> this.sendMessage(line) }
}
