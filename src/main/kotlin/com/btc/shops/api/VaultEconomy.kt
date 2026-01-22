package com.btc.shops.api

import net.milkbowl.vault.economy.Economy as Vault
import org.bukkit.Bukkit
import java.util.UUID

/**
 * Adapter for Vault based economies.
 */
class VaultEconomy(private val vault: Vault) : Economy {
    override fun getBalance(playerId: UUID): Double =
        vault.getBalance(Bukkit.getOfflinePlayer(playerId))

    override fun withdraw(playerId: UUID, amount: Double): Boolean {
        val result = vault.withdrawPlayer(Bukkit.getOfflinePlayer(playerId), amount)
        return result.transactionSuccess()
    }

    override fun deposit(playerId: UUID, amount: Double) {
        vault.depositPlayer(Bukkit.getOfflinePlayer(playerId), amount)
    }
}

