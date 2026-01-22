package com.btc.shops.service

import com.btc.shops.api.Economy
import com.btc.shops.api.PlaceholderEconomy
import com.btc.shops.api.PointsEconomy
import com.btc.shops.api.VaultEconomy
import com.btc.shops.manifest.CurrencyType
import com.btc.shops.manifest.ShopDefinitionEntry
import com.typewritermc.core.extension.annotations.Singleton
import org.bukkit.Bukkit

/**
 * Resolves the appropriate [Economy] implementation for a shop definition.
 */
@Singleton
class EconomyService {
    fun resolve(definition: ShopDefinitionEntry): Economy? = when (definition.currency) {
        CurrencyType.VAULT -> Bukkit.getServicesManager()
            .getRegistration(net.milkbowl.vault.economy.Economy::class.java)
            ?.provider?.let { VaultEconomy(it) }
        CurrencyType.PLACEHOLDER -> if (definition.balancePlaceholder.isNotBlank() &&
            definition.addCommand.isNotBlank() && definition.removeCommand.isNotBlank()) {
            PlaceholderEconomy(definition.balancePlaceholder, definition.addCommand, definition.removeCommand)
        } else {
            null
        }
        CurrencyType.POINTS -> PointsEconomy()
    }
}

