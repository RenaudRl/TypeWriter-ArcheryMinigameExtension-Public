package com.btc.shops.manifest

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.ManifestEntry
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.utils.item.Item
import java.time.DayOfWeek
import kotlinx.serialization.Contextual

/**
 * Declarative definition of a shop and its items.
 */
@Tags("shop")
@Entry("shop_definition", "Defines a shop", Colors.GREEN, "mdi:cart")
class ShopDefinitionEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Inventory title displayed to players. Supports color codes and placeholders.")
    val title: String = "",
    @Help("Number of inventory rows (1-6).")
    val rows: Int = 6,
    @Help("Item used to fill empty slots.")
    val fillerItem: Var<Item> = ConstVar(Item.Empty),
    @Help("Lore lines appended to each item to display prices. Placeholders: {buy}, {sell}. Supports color codes and placeholders.")
    val priceLore: List<String> = listOf("&eBuy: {buy}", "&cSell: {sell}"),
    @Help("Item used for navigating to the next page.")
    val nextPageButton: Var<Item> = ConstVar(Item.Empty),
    @Help("Display name for the next page button. Supports color codes and placeholders.")
    val nextPageButtonName: String = "&aNext",
    @Help("Lore for the next page button. Supports color codes, placeholders and multiline.")
    val nextPageButtonLore: List<String> = emptyList(),
    @Help("Item used for navigating to the previous page.")
    val previousPageButton: Var<Item> = ConstVar(Item.Empty),
    @Help("Display name for the previous page button. Supports color codes and placeholders.")
    val previousPageButtonName: String = "&cPrevious",
    @Help("Lore for the previous page button. Supports color codes, placeholders and multiline.")
    val previousPageButtonLore: List<String> = emptyList(),
    @Help("Economy provider used for pricing.")
    val currency: CurrencyType = CurrencyType.VAULT,
    @Help("Placeholder used to fetch player balance when currency is placeholder.")
    val balancePlaceholder: String = "",
    @Help("Command to add currency. {player} and {amount} will be replaced.")
    val addCommand: String = "",
    @Help("Command to remove currency. {player} and {amount} will be replaced.")
    val removeCommand: String = "",
    @Help("Tax percentage applied on purchases.")
    val taxes: Double = 0.0,
    @Help("Policy determining when stock resets.")
    val resetPolicy: ResetPolicy = ResetPolicy.NONE,
    @Help("Hour of day used for reset policies.")
    val resetHour: Int = 0,
    @Help("Minute of hour used for reset policies.")
    val resetMinute: Int = 0,
    @Help("Day of week used when reset policy is WEEKLY.")
    val resetDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    @Help("Day of month used when reset policy is MONTHLY.")
    val resetDayOfMonth: Int = 1,
    @Help("Interval in seconds when reset policy is INTERVAL.")
    val resetIntervalSeconds: Long = 0,
    @Help("Items available in the shop.")
    val items: List<ShopItemConfig> = emptyList(),
    @Help("Button used to buy a full stack at once.")
    val buyStackButton: MenuButton = MenuButton(),
    @Help("Button used to sell a full stack at once.")
    val sellStackButton: MenuButton = MenuButton(),
    @Help("Button that lets the player enter a custom amount to buy.")
    val buyAmountButton: MenuButton = MenuButton(),
    @Help("Button that lets the player enter a custom amount to sell.")
    val sellAmountButton: MenuButton = MenuButton(),
    @Help("Button returning to the main shop from the amount menu.")
    val backButton: MenuButton = MenuButton(),
    @Help("Button that displays additional information about the item.")
    val infoButton: MenuButton = MenuButton(),
    @Help("Messages shown when the information button is clicked. Supports color codes, placeholders and multiline.")
    val infoMessage: List<String> = emptyList(),
    @Help("Message shown when entering a custom buy amount. Supports color codes and placeholders.")
    val buyAmountPrompt: String = "&eEnter amount to buy:",
    @Help("Message shown when entering a custom sell amount. Supports color codes and placeholders.")
    val sellAmountPrompt: String = "&eEnter amount to sell:",
    @Help("Label used for the confirmation button in amount dialogs. Supports color codes and placeholders.")
    val amountConfirmButton: String = "&aConfirm",
    @Help("Label for the custom amount input. Supports color codes and placeholders.")
    val amountInputLabel: String = "&fAmount",
    @Help("Placeholder text inside the custom amount input. Supports color codes and placeholders.")
    val amountInputPlaceholder: String = "",
    @Help("Width of the custom amount input field.")
    val amountInputWidth: Int = 4,
    @Help("Maximum allowed length for the custom amount input.")
    val amountInputMaxLength: Int = 10,
    @Help("Message when the player cannot afford a purchase. Placeholders: {amount}, {price}.")
    val cannotAffordMessage: String = "&cYou cannot afford this purchase",
    @Help("Message when the player's inventory is full. Placeholders: {amount}, {price}.")
    val inventoryFullMessage: String = "&cNot enough inventory space",
    @Help("Message sent after a successful purchase. Placeholders: {amount}, {price}.")
    val purchaseMessage: String = "&aPurchased x{amount} for {price}",
    @Help("Message when the item cannot be sold. Placeholders: {amount}, {price}.")
    val cannotSellMessage: String = "&cThis item cannot be sold",
    @Help("Message when the player doesn't have enough items to sell. Placeholders: {amount}, {price}.")
    val notEnoughItemsMessage: String = "&cYou don't have enough items to sell",
    @Help("Message sent after a successful sale. Placeholders: {amount}, {price}.")
    val sellMessage: String = "&aSold x{amount} for {price}",
    @Help("Message when no economy provider is available.")
    val noEconomyMessage: String = "&cNo economy available",
    @Help("Criteria that must be met to access the shop.")
    val criteria: List<@Contextual Criteria> = emptyList(),
    @Help("Message when the required criteria are not met.")
    val criteriaFailMessage: String = "&cYou cannot access this shop",
    @Help("Item displayed when a shop item is locked.")
    val lockedItem: Var<Item> = ConstVar(Item.Empty),
    @Help("Message when a player reaches the purchase limit for an item.")
    val limitReachedMessage: String = "&cPurchase limit reached",
    @Help("Artifact used for persistent storage and synchronisation.")
    val artifact: Ref<ShopArtifact> = emptyRef()
) : ManifestEntry

