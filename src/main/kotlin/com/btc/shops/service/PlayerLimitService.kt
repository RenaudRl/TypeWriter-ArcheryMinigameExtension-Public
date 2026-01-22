package com.btc.shops.service

import com.btc.shops.manifest.ShopDefinitionEntry
import com.typewritermc.core.extension.annotations.Singleton
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Singleton
class PlayerLimitService(
    private val resetService: ResetService
) {
    private val usage = ConcurrentHashMap<String, MutableMap<UUID, Int>>()

    private fun key(definition: ShopDefinitionEntry, index: Int) = "${definition.id}:$index"

    fun reset(definition: ShopDefinitionEntry) {
        val prefix = "${definition.id}:"
        usage.keys.removeIf { it.startsWith(prefix) }
    }

    fun remaining(player: Player, definition: ShopDefinitionEntry, index: Int, limit: Int): Int {
        if (limit <= 0) return Int.MAX_VALUE
        if (resetService.shouldReset(definition)) {
            reset(definition)
        }
        val map = usage.getOrPut(key(definition, index)) { mutableMapOf() }
        val used = map[player.uniqueId] ?: 0
        return limit - used
    }

    fun record(player: Player, definition: ShopDefinitionEntry, index: Int, amount: Int) {
        if (amount <= 0) return
        if (resetService.shouldReset(definition)) {
            reset(definition)
        }
        val map = usage.getOrPut(key(definition, index)) { mutableMapOf() }
        map[player.uniqueId] = (map[player.uniqueId] ?: 0) + amount
    }
}

