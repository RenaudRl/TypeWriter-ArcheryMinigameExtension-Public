package com.btc.shops.service

import com.btc.shops.manifest.MenuButton
import com.btc.shops.manifest.ShopDefinitionEntry
import com.btc.shops.manifest.ShopItemConfig
import com.btc.shops.api.Economy
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.entry.entries.get
import com.typewritermc.engine.paper.entry.matches
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import io.papermc.paper.registry.data.dialog.action.DialogAction
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.utils.item.Item

/**
 * Builds and displays shop inventories for players with navigation and safety.
 */
@Singleton
class GuiService(
    private val plugin: JavaPlugin,
    private val economyService: EconomyService,
    private val priceService: PriceService,
    private val stockService: StockService,
    private val resetService: ResetService,
    private val playerLimitService: PlayerLimitService
) : Listener {

    private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()

    private fun String.toComponent(player: Player): Component =
        legacySerializer.deserialize(applyPlaceholders(player, this))

    private fun String.toComponentLines(player: Player): List<Component> =
        applyPlaceholders(player, this).split("\n").map { legacySerializer.deserialize(it) }

    private fun ComponentStringBuilder(
        player: Player,
        template: String,
        placeholders: Map<String, Any> = emptyMap()
    ): Component {
        var processed = template
        placeholders.forEach { (key, value) ->
            processed = processed.replace("{$key}", value.toString())
        }
        return processed.toComponent(player)
    }

    private sealed interface View
    private data class ShopView(
        val definition: ShopDefinitionEntry,
        val page: Int,
        val slots: Map<Int, ShopItemConfig>
    ) : View

    private data class AmountView(
        val definition: ShopDefinitionEntry,
        val cfg: ShopItemConfig,
        val page: Int
    ) : View

    private val openShops: MutableMap<UUID, View> = mutableMapOf()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable { refreshOpenShops() }, 20L, 20L)
    }

    private fun applyPlaceholders(player: Player, text: String): String {
        return if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PlaceholderAPI.setPlaceholders(player, text)
        } else text
    }

    fun open(player: Player, definition: ShopDefinitionEntry, page: Int = 0, delayTicks: Long = 0L) {
        val task = Runnable {
            if (!definition.criteria.matches(player)) {
                player.sendMessage(definition.criteriaFailMessage.toComponent(player))
                return@Runnable
            }
            if (resetService.shouldReset(definition)) {
                definition.items.forEachIndexed { index, cfg ->
                    stockService.reset(definition.id, index, cfg.strategy.stockMax)
                }
                playerLimitService.reset(definition)
            }
            val size = definition.rows * 9
            val prevSlot = size - 9
            val nextSlot = size - 1
            val infoSlotUsed = definition.infoButton.slot in 0 until size
            val pageSlots = size - 2 - if (infoSlotUsed) 1 else 0
            val startIndex = page * pageSlots
            val title = definition.title.toComponent(player)
            val inventory: Inventory = Bukkit.createInventory(null, size, title)
            val slotMap = mutableMapOf<Int, ShopItemConfig>()

            // Fill with filler item if configured
            definition.fillerItem.get(player)?.build(player)?.let { filler ->
                for (slot in 0 until size) inventory.setItem(slot, filler)
            }

            // Add items skipping navigation slots
            val pageItems = definition.items.drop(startIndex).take(pageSlots)
            var itemIndex = 0
            for (slot in 0 until size) {
                if (slot == prevSlot || slot == nextSlot || (infoSlotUsed && slot == definition.infoButton.slot)) continue
                val cfg = pageItems.getOrNull(itemIndex++) ?: break
                if (!cfg.criteria.matches(player)) {
                    definition.lockedItem.get(player)?.build(player)?.let { inventory.setItem(slot, it) }
                    continue
                }
                inventory.setItem(slot, buildItem(cfg, player, definition))
                slotMap[slot] = cfg
            }

            // Navigation buttons
            if (page > 0) {
                inventory.setItem(
                    prevSlot,
                    buildNavItem(
                        player,
                        definition.previousPageButton,
                        definition.previousPageButtonName,
                        definition.previousPageButtonLore
                    )
                )
            }
            if (startIndex + pageSlots < definition.items.size) {
                inventory.setItem(
                    nextSlot,
                    buildNavItem(
                        player,
                        definition.nextPageButton,
                        definition.nextPageButtonName,
                        definition.nextPageButtonLore
                    )
                )
            }

            if (infoSlotUsed) {
                inventory.setItem(
                    definition.infoButton.slot,
                    buildButton(null, player, definition, 1, definition.infoButton)
                )
            }

            player.openInventory(inventory)
            openShops[player.uniqueId] = ShopView(definition, page, slotMap)
        }
        if (delayTicks <= 0L) {
            if (Bukkit.isPrimaryThread()) {
                task.run()
            } else {
                Bukkit.getScheduler().runTask(plugin, task)
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks)
        }
    }

    private fun refreshOpenShops() {
        openShops.forEach { (uuid, view) ->
            val player = Bukkit.getPlayer(uuid) ?: return@forEach
            val inventory = player.openInventory.topInventory
            when (view) {
                is ShopView -> {
                    view.slots.forEach { (slot, cfg) ->
                        inventory.setItem(slot, buildItem(cfg, player, view.definition))
                    }
                    val def = view.definition
                    if (def.infoButton.slot in 0 until inventory.size) {
                        inventory.setItem(
                            def.infoButton.slot,
                            buildButton(null, player, def, 1, def.infoButton)
                        )
                    }
                }
                is AmountView -> {
                    val def = view.definition
                    val stackAmount = view.cfg.item.get(player)?.build(player)?.maxStackSize ?: 64
                    if (def.buyStackButton.slot in 0 until inventory.size) {
                        inventory.setItem(
                            def.buyStackButton.slot,
                            buildButton(view.cfg, player, def, stackAmount, def.buyStackButton)
                        )
                    }
                    if (def.sellStackButton.slot in 0 until inventory.size) {
                        inventory.setItem(
                            def.sellStackButton.slot,
                            buildButton(view.cfg, player, def, stackAmount, def.sellStackButton)
                        )
                    }
                    if (def.buyAmountButton.slot in 0 until inventory.size) {
                        inventory.setItem(
                            def.buyAmountButton.slot,
                            buildButton(view.cfg, player, def, 1, def.buyAmountButton)
                        )
                    }
                    if (def.sellAmountButton.slot in 0 until inventory.size) {
                        inventory.setItem(
                            def.sellAmountButton.slot,
                            buildButton(view.cfg, player, def, 1, def.sellAmountButton)
                        )
                    }
                    if (def.backButton.slot in 0 until inventory.size) {
                        inventory.setItem(
                            def.backButton.slot,
                            buildButton(view.cfg, player, def, 1, def.backButton)
                        )
                    }
                }
            }
        }
    }

    private fun List<String>.translateMultiline(player: Player): List<Component> =
        flatMap { it.toComponentLines(player) }

    private fun buildItem(cfg: ShopItemConfig, player: Player, definition: ShopDefinitionEntry): ItemStack {
        val stack = cfg.item.get(player)?.build(player) ?: ItemStack(Material.STONE)
        val meta = stack.itemMeta
        if (cfg.name.isNotEmpty()) meta.displayName(cfg.name.toComponent(player))
        val baseLore = meta.lore()?.toList() ?: emptyList()
        val customLore = cfg.lore.translateMultiline(player)
        val priceLore = definition.priceLore.flatMap { line ->
            var processed = line
            var show = true
            if (processed.contains("{buy}")) {
                val price = getBuyPrice(definition, cfg, 1)
                if (price > 0) processed = processed.replace("{buy}", formatPrice(price)) else show = false
            }
            if (processed.contains("{sell}")) {
                val price = getSellPrice(definition, cfg, 1)
                if (price > 0) processed = processed.replace("{sell}", formatPrice(price)) else show = false
            }
            if (!show) emptyList() else processed.toComponentLines(player)
        }
        val lore = baseLore + customLore + priceLore
        meta.lore(lore.takeIf { it.isNotEmpty() })
        stack.itemMeta = meta
        return stack
    }

    private fun buildNavItem(player: Player, base: Var<Item>, name: String, lore: List<String>): ItemStack {
        val stack = base.get(player)?.build(player) ?: ItemStack(Material.ARROW)
        val meta = stack.itemMeta
        if (name.isNotEmpty()) meta.displayName(name.toComponent(player))
        val translatedLore = lore.translateMultiline(player)
        meta.lore(translatedLore.takeIf { it.isNotEmpty() })
        stack.itemMeta = meta
        return stack
    }

    private fun buildButton(
        cfg: ShopItemConfig?,
        player: Player,
        definition: ShopDefinitionEntry,
        amount: Int,
        button: MenuButton
    ): ItemStack {
        val stack = button.item.get(player)?.build(player)
            ?: cfg?.item?.get(player)?.build(player)
            ?: ItemStack(Material.STONE)
        stack.amount = amount.coerceAtMost(stack.maxStackSize)
        val meta = stack.itemMeta
        if (button.name.isNotEmpty()) {
            val nameComponent = processText(button.name, cfg, definition, amount, player).toComponent(player)
            meta.displayName(nameComponent)
        }
        val lore = button.lore.flatMap {
            processText(it, cfg, definition, amount, player).toComponentLines(player)
        }
        if (lore.isNotEmpty()) {
            meta.lore(lore)
        } else {
            meta.lore(null)
        }
        stack.itemMeta = meta
        return stack
    }

    private fun processText(
        template: String,
        cfg: ShopItemConfig?,
        definition: ShopDefinitionEntry,
        amount: Int,
        player: Player
    ): String {
        var result = template.replace("{amount}", amount.toString())
        val buy = if (cfg != null) getBuyPrice(definition, cfg, amount) else 0.0
        val sell = if (cfg != null) getSellPrice(definition, cfg, amount) else 0.0
        result = result.replace("{buy}", formatPrice(buy))
        result = result.replace("{sell}", formatPrice(sell))
        return result
    }

    private fun getBuyPrice(definition: ShopDefinitionEntry, cfg: ShopItemConfig, amount: Int): Double {
        val index = definition.items.indexOf(cfg)
        val stock = stockService.getStock(definition.id, index, cfg.strategy.stockMax)
        val unit = priceService.calculateBuyPrice(stock, cfg.strategy)
        return unit * amount
    }

    private fun getSellPrice(definition: ShopDefinitionEntry, cfg: ShopItemConfig, amount: Int): Double {
        val index = definition.items.indexOf(cfg)
        val stock = stockService.getStock(definition.id, index, cfg.strategy.stockMax)
        val unit = priceService.calculateSellPrice(stock, cfg.strategy)
        return unit * amount
    }

    private fun formatPrice(value: Double): String = String.format("%.2f", value)

    private fun sendMessage(player: Player, template: String, placeholders: Map<String, Any> = emptyMap()) {
        player.sendMessage(ComponentStringBuilder(player, template, placeholders))
    }

    private fun handleBuy(
        player: Player,
        cfg: ShopItemConfig,
        definition: ShopDefinitionEntry,
        economy: Economy,
        amount: Int = 1
    ) {
        val index = definition.items.indexOf(cfg)
        if (cfg.limitPerPlayer > 0) {
            val remaining = playerLimitService.remaining(player, definition, index, cfg.limitPerPlayer)
            if (remaining < amount) {
                sendMessage(player, definition.limitReachedMessage, mapOf("limit" to cfg.limitPerPlayer))
                return
            }
        }
        val cost = getBuyPrice(definition, cfg, amount)
        if (!economy.withdraw(player.uniqueId, cost)) {
            sendMessage(player, definition.cannotAffordMessage, mapOf("amount" to amount, "price" to formatPrice(cost)))
            return
        }
        val item = cfg.item.get(player)?.build(player)?.apply { this.amount = amount } ?: ItemStack(Material.STONE, amount)
        val leftovers = player.inventory.addItem(item)
        if (leftovers.isNotEmpty()) {
            economy.deposit(player.uniqueId, cost)
            sendMessage(player, definition.inventoryFullMessage, mapOf("amount" to amount, "price" to formatPrice(cost)))
            return
        }
        stockService.buy(definition.id, index, amount, cfg.strategy)
        if (cfg.limitPerPlayer > 0) {
            playerLimitService.record(player, definition, index, amount)
        }
        sendMessage(player, definition.purchaseMessage, mapOf("amount" to amount, "price" to formatPrice(cost)))
        refreshOpenShops()
    }

    private fun handleSell(
        player: Player,
        cfg: ShopItemConfig,
        definition: ShopDefinitionEntry,
        economy: Economy,
        amount: Int = 1
    ) {
        val reward = getSellPrice(definition, cfg, amount)
        if (reward <= 0.0) {
            sendMessage(player, definition.cannotSellMessage, mapOf("amount" to amount, "price" to formatPrice(reward)))
            return
        }
        val template = cfg.item.get(player)?.build(player)?.apply { this.amount = amount } ?: ItemStack(Material.STONE, amount)
        if (!player.inventory.containsAtLeast(template, amount)) {
            sendMessage(player, definition.notEnoughItemsMessage, mapOf("amount" to amount, "price" to formatPrice(reward)))
            return
        }
        player.inventory.removeItem(template)
        economy.deposit(player.uniqueId, reward)
        val index = definition.items.indexOf(cfg)
        stockService.sell(definition.id, index, amount, cfg.strategy)
        sendMessage(player, definition.sellMessage, mapOf("amount" to amount, "price" to formatPrice(reward)))
        refreshOpenShops()
    }

    private fun handleSellAll(
        player: Player,
        cfg: ShopItemConfig,
        definition: ShopDefinitionEntry,
        economy: Economy
    ) {
        val template = cfg.item.get(player)?.build(player) ?: ItemStack(Material.STONE)
        val allAmount = player.inventory.contents.filterNotNull().filter { it.isSimilar(template) }.sumOf { it.amount }
        if (allAmount <= 0) {
            sendMessage(player, definition.notEnoughItemsMessage, mapOf("amount" to 0, "price" to formatPrice(0.0)))
            return
        }
        var remaining = allAmount
        while (remaining > 0) {
            val remove = template.clone().apply { amount = remaining.coerceAtMost(64) }
            player.inventory.removeItem(remove)
            remaining -= remove.amount
        }
        val reward = getSellPrice(definition, cfg, allAmount)
        economy.deposit(player.uniqueId, reward)
        val index = definition.items.indexOf(cfg)
        stockService.sell(definition.id, index, allAmount, cfg.strategy)
        sendMessage(player, definition.sellMessage, mapOf("amount" to allAmount, "price" to formatPrice(reward)))
        refreshOpenShops()
    }

    private fun openAmountInputDialog(
        player: Player,
        definition: ShopDefinitionEntry,
        cfg: ShopItemConfig,
        page: Int,
        buy: Boolean
    ) {
        val nameComponent = cfg.name.toComponent(player)
        val plainTitle = PlainTextComponentSerializer.plainText().serialize(nameComponent).ifBlank { cfg.name }
        val title = Component.text(plainTitle)
        val rawPrompt = if (buy) definition.buyAmountPrompt else definition.sellAmountPrompt
        val message = rawPrompt.toComponent(player)
        val confirm = definition.amountConfirmButton.toComponent(player)
        val body = DialogBody.plainMessage(message)
        val action = DialogAction.customClick({ response, audience ->
            val p = audience as? Player ?: return@customClick
            val amount = response.getText("amount")?.toIntOrNull()
            if (amount == null || amount <= 0) {
                p.sendMessage(Component.text("Invalid amount.", NamedTextColor.RED))
                return@customClick
            }
            val economy = economyService.resolve(definition)
            if (economy == null) {
                sendMessage(p, definition.noEconomyMessage)
                return@customClick
            }
            if (buy) {
                handleBuy(p, cfg, definition, economy, amount)
            } else {
                handleSell(p, cfg, definition, economy, amount)
            }
            Bukkit.getScheduler().runTask(plugin, Runnable { openAmountMenu(p, definition, cfg, page) })
        }, ClickCallback.Options.builder().uses(1).build())

        val dialog = Dialog.create { factory ->
            val base = DialogBase.builder(title)
                .body(listOf(body))
                .inputs(
                    listOf(
                        DialogInput.text(
                            "amount",
                            definition.amountInputWidth,
                            definition.amountInputLabel.toComponent(player),
                            true,
                            applyPlaceholders(player, definition.amountInputPlaceholder),
                            definition.amountInputMaxLength,
                            null
                        )
                    )
                )
                .canCloseWithEscape(true)
                .build()
            val button = ActionButton.builder(confirm).action(action).build()
            factory.empty().base(base).type(DialogType.multiAction(listOf(button), null, 1))
        }
        player.showDialog(dialog)
    }

    private fun openAmountMenu(player: Player, definition: ShopDefinitionEntry, cfg: ShopItemConfig, page: Int) {
        val inventory = Bukkit.createInventory(null, 27, cfg.name.toComponent(player))
        definition.fillerItem.get(player)?.build(player)?.let { filler ->
            for (slot in 0 until inventory.size) inventory.setItem(slot, filler)
        }
        val stackAmount = cfg.item.get(player)?.build(player)?.maxStackSize ?: 64

        if (definition.buyStackButton.slot in 0 until inventory.size) {
            inventory.setItem(
                definition.buyStackButton.slot,
                buildButton(cfg, player, definition, stackAmount, definition.buyStackButton)
            )
        }
        if (definition.sellStackButton.slot in 0 until inventory.size) {
            inventory.setItem(
                definition.sellStackButton.slot,
                buildButton(cfg, player, definition, stackAmount, definition.sellStackButton)
            )
        }
        if (definition.buyAmountButton.slot in 0 until inventory.size) {
            inventory.setItem(
                definition.buyAmountButton.slot,
                buildButton(cfg, player, definition, 1, definition.buyAmountButton)
            )
        }
        if (definition.sellAmountButton.slot in 0 until inventory.size) {
            inventory.setItem(
                definition.sellAmountButton.slot,
                buildButton(cfg, player, definition, 1, definition.sellAmountButton)
            )
        }
        if (definition.backButton.slot in 0 until inventory.size) {
            inventory.setItem(
                definition.backButton.slot,
                buildButton(cfg, player, definition, 1, definition.backButton)
            )
        }
        player.openInventory(inventory)
        openShops[player.uniqueId] = AmountView(definition, cfg, page)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val view = openShops[player.uniqueId] ?: return

        if (event.clickedInventory != event.view.topInventory) {
            event.isCancelled = true
            return
        }

        event.isCancelled = true
        when (view) {
            is ShopView -> {
                val definition = view.definition
                val page = view.page
                val size = definition.rows * 9
                val prevSlot = size - 9
                val nextSlot = size - 1
                val infoSlot = definition.infoButton.slot
                val pageSlots = size - 2 - if (infoSlot in 0 until size) 1 else 0
                when (event.rawSlot) {
                    nextSlot -> {
                        val startIndex = (page + 1) * pageSlots
                        if (startIndex < definition.items.size) open(player, definition, page + 1)
                    }
                    prevSlot -> {
                        if (page > 0) open(player, definition, page - 1)
                    }
                    infoSlot -> {
                        definition.infoMessage.forEach {
                            val msg = processText(it, null, definition, 1, player).toComponent(player)
                            player.sendMessage(msg)
                        }
                    }
                    else -> {
                        val cfg = view.slots[event.rawSlot] ?: return
                        val economy = economyService.resolve(definition)
                        if (economy == null) {
                            sendMessage(player, definition.noEconomyMessage)
                            return
                        }
                        when {
                            event.isShiftClick && event.isLeftClick -> openAmountMenu(player, definition, cfg, page)
                            event.isShiftClick && event.isRightClick -> handleSellAll(player, cfg, definition, economy)
                            event.isLeftClick -> handleBuy(player, cfg, definition, economy)
                            event.isRightClick -> handleSell(player, cfg, definition, economy)
                        }
                    }
                }
            }
            is AmountView -> {
                val definition = view.definition
                val economy = economyService.resolve(definition)
                if (economy == null) {
                    sendMessage(player, definition.noEconomyMessage)
                    return
                }
                when (event.rawSlot) {
                    definition.buyStackButton.slot -> {
                        val stackAmount = view.cfg.item.get(player)?.build(player)?.maxStackSize ?: 64
                        handleBuy(player, view.cfg, definition, economy, stackAmount)
                    }
                    definition.sellStackButton.slot -> {
                        val stackAmount = view.cfg.item.get(player)?.build(player)?.maxStackSize ?: 64
                        handleSell(player, view.cfg, definition, economy, stackAmount)
                    }
                    definition.buyAmountButton.slot -> {
                        player.closeInventory()
                        openAmountInputDialog(player, definition, view.cfg, view.page, true)
                    }
                    definition.sellAmountButton.slot -> {
                        player.closeInventory()
                        openAmountInputDialog(player, definition, view.cfg, view.page, false)
                    }
                    definition.backButton.slot -> open(player, definition, view.page)
                }
            }
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        if (openShops.containsKey(player.uniqueId)) event.isCancelled = true
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        openShops.remove(event.player.uniqueId)
    }
}

