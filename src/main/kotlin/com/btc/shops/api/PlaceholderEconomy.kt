package com.btc.shops.api

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import java.util.UUID

/**
 * Economy implementation backed by PlaceholderAPI.
 * It retrieves balances from a configured placeholder and
 * executes console commands to deposit or withdraw funds.
 */
class PlaceholderEconomy(
    private val balancePlaceholder: String,
    private val addCommand: String,
    private val removeCommand: String
) : Economy {
    override fun getBalance(playerId: UUID): Double {
        val player = Bukkit.getPlayer(playerId) ?: return 0.0
        val value = PlaceholderAPI.setPlaceholders(player, balancePlaceholder)
        return value.toDoubleOrNull() ?: 0.0
    }

    override fun withdraw(playerId: UUID, amount: Double): Boolean {
        val player = Bukkit.getPlayer(playerId) ?: return false
        val current = getBalance(playerId)
        if (current < amount) return false
        val command = removeCommand
            .replace("{player}", player.name)
            .replace("{amount}", amount.toString())
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
        return true
    }

    override fun deposit(playerId: UUID, amount: Double) {
        val player = Bukkit.getPlayer(playerId) ?: return
        val command = addCommand
            .replace("{player}", player.name)
            .replace("{amount}", amount.toString())
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
    }
}

