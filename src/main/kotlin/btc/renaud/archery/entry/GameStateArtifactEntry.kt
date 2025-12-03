package btc.renaud.archery.entry

import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.engine.paper.entry.entries.ArtifactEntry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.core.entries.Entry as TWEntry
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.engine.paper.entry.AssetManager
import kotlinx.coroutines.runBlocking
import org.bukkit.inventory.ItemStack
import org.bukkit.configuration.file.YamlConfiguration
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.get
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

/**
 * Stores temporary state for an active archery game. The data is serialized as
 * JSON and cleared when the game ends.
 */
@Tags("game_state")
@Entry("game_state_artifact", "Temporary state for an active archery game", Colors.MEDIUM_PURPLE, "mdi:chart-line")
class GameStateArtifactEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Whether a game is currently running")
    val running: Boolean = false,
    @Help("Current scores for players")
    val players: Map<String, Int> = emptyMap(),
    @Help("Number of active targets")
    val activeTargets: Int = 0,
    @Help("Time remaining in seconds")
    val timeRemaining: Long = 0,
    override val artifactId: String = "renaud:archery_game_state",
) : ArtifactEntry, TWEntry

// -- Persistence helpers ----------------------------------------------------

private val gson: Gson
    get() = get(Gson::class.java, named("dataSerializer"))
private val assetManager: AssetManager
    get() = get(AssetManager::class.java)

fun GameStateArtifactEntry.loadData(): JsonObject {
    if (artifactId.isBlank()) return JsonObject()
    val json = runBlocking {
        try {
            if (assetManager.containsAsset(this@loadData)) {
                assetManager.fetchStringAsset(this@loadData)
            } else null
        } catch (_: Throwable) {
            null
        }
    }
    return if (json == null) JsonObject() else gson.fromJson(json, JsonObject::class.java) ?: JsonObject()
}

fun GameStateArtifactEntry.saveData(obj: JsonObject) {
    if (artifactId.isBlank()) return
    val json = gson.toJson(obj)
    runBlocking { assetManager.storeStringAsset(this@saveData, json) }
}

fun GameStateArtifactEntry.setRunning(running: Boolean) {
    val root = loadData()
    root.addProperty("running", running)
    saveData(root)
}

fun GameStateArtifactEntry.storeInventory(uuid: UUID, contents: Array<ItemStack?>) {
    val root = loadData()
    val invObj = root.getAsJsonObject("inventories") ?: JsonObject().also { root.add("inventories", it) }
    invObj.addProperty(uuid.toString(), contents.toBase64())
    saveData(root)
}

fun GameStateArtifactEntry.restoreInventory(uuid: UUID): Array<ItemStack?>? {
    val root = loadData()
    val invObj = root.getAsJsonObject("inventories") ?: return null
    val data = invObj.remove(uuid.toString())?.asString ?: return null
    saveData(root)
    return fromBase64(data)
}

private fun Array<ItemStack?>.toBase64(): String {
    val config = YamlConfiguration()
    config.set("size", this.size)
    for (i in indices) {
        // YamlConfiguration handles ItemStack (ConfigurationSerializable)
        config.set("items.$i", this[i])
    }
    val yaml = config.saveToString()
    return Base64.getEncoder().encodeToString(yaml.toByteArray(StandardCharsets.UTF_8))
}

private fun fromBase64(data: String): Array<ItemStack?> {
    val yamlString = String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8)
    val config = YamlConfiguration()
    config.loadFromString(yamlString)
    val size = config.getInt("size")
    val array = arrayOfNulls<ItemStack>(size)
    for (i in 0 until size) {
        array[i] = config.getItemStack("items.$i")
    }
    return array
}

