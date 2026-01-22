package com.btc.shops.manifest

import com.typewritermc.core.extension.annotations.Help

/** Policies describing when shop stocks are reset. */
enum class ResetPolicy {
    @Help("No automatic stock reset.")
    NONE,
    @Help("Reset using a cron expression.")
    CRON,
    @Help("Reset after a fixed time interval.")
    INTERVAL,
    @Help("Reset every day.")
    DAILY,
    @Help("Reset every week.")
    WEEKLY,
    @Help("Reset every month.")
    MONTHLY
}

