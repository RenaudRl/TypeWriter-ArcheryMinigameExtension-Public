package com.btc.shops.manifest

import com.typewritermc.core.extension.annotations.Help

/**
 * Supported economy providers for shop pricing.
 */
enum class CurrencyType {
    @Help("Use Vault's economy implementation.")
    VAULT,

    @Help("Use PlaceholderAPI placeholders and commands.")
    PLACEHOLDER,

    @Help("Use a simple in-memory points economy.")
    POINTS
}


