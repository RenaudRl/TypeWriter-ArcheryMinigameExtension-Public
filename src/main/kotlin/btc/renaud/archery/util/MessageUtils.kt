package btc.renaud.archery.util

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player

/**
 * Applies placeholder replacement and color codes to the given message.
 */
fun formatMessage(message: String, placeholders: Map<String, String> = emptyMap()): String {
    val replaced = replacePlaceholders(message, placeholders)
    val component = LEGACY_AMPERSAND.deserialize(replaced)
    return LEGACY_SECTION.serialize(component)
}

/**
 * Sends a formatted message to the player.
 * Supports color codes using '&', placeholders of the form %key%,
 * and multiline messages separated by '\n'.
 */
fun Player.sendFormatted(message: String, placeholders: Map<String, String> = emptyMap()) {
    val colored = formatMessage(message, placeholders)
    colored.split("\n").forEach { line ->
        sendMessage(LEGACY_SECTION.deserialize(line))
    }
}

private fun replacePlaceholders(message: String, placeholders: Map<String, String>): String {
    var formatted = message
    placeholders.forEach { (key, value) ->
        formatted = formatted.replace("%$key%", value)
    }
    return formatted
}

private val LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand()
private val LEGACY_SECTION = LegacyComponentSerializer.legacySection()

