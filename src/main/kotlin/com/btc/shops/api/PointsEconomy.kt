package com.btc.shops.api

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple in-memory economy used mainly for tests or development.
 */
class PointsEconomy : Economy {
    private val balances = ConcurrentHashMap<UUID, Double>()

    override fun getBalance(playerId: UUID): Double = balances[playerId] ?: 0.0

    override fun withdraw(playerId: UUID, amount: Double): Boolean {
        val current = getBalance(playerId)
        if (current < amount) return false
        balances[playerId] = current - amount
        return true
    }

    override fun deposit(playerId: UUID, amount: Double) {
        balances[playerId] = getBalance(playerId) + amount
    }
}

