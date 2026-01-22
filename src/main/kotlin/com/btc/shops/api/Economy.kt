package com.btc.shops.api

import java.util.UUID

/**
 * Abstraction over any currency implementation.
 * Provides minimal operations required by the shop system.
 */
interface Economy {
    /** Returns the balance of the given player. */
    fun getBalance(playerId: UUID): Double

    /** Withdraws [amount] from the player if possible. */
    fun withdraw(playerId: UUID, amount: Double): Boolean

    /** Deposits [amount] to the player. */
    fun deposit(playerId: UUID, amount: Double)
}

