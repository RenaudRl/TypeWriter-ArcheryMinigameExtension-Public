package btc.renaud.archery.entry

import com.typewritermc.core.entries.Entry as TWEntry
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.entries.ArtifactEntry
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.engine.paper.entry.AssetManager
import kotlinx.coroutines.runBlocking
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.get
import java.util.UUID

/**
 * Stores persistent statistics for archery games. Scores are saved in a JSON
 * structure and survive server restarts.
 */
@Tags("game_stat")
@Entry("game_stat_artifact", "Persistent statistics for archery games", Colors.PURPLE, "mdi:podium")
class GameStatArtifactEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Unique identifier for stored statistics")
    override val artifactId: String = "renaud:archery_game_stat",
) : ArtifactEntry, TWEntry

// -- Persistence helpers ----------------------------------------------------

private val gson: Gson
    get() = get(Gson::class.java, named("dataSerializer"))
private val assetManager: AssetManager
    get() = get(AssetManager::class.java)

fun GameStatArtifactEntry.loadData(): JsonObject {
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

fun GameStatArtifactEntry.saveData(obj: JsonObject) {
    if (artifactId.isBlank()) return
    val json = gson.toJson(obj)
    runBlocking { assetManager.storeStringAsset(this@saveData, json) }
}

fun GameStatArtifactEntry.recordScore(mode: GameMode, uuid: UUID, score: Int) {
    val root = loadData()
    val global = root.getAsJsonObject("global") ?: JsonObject().also { root.add("global", it) }
    val players = root.getAsJsonObject("players") ?: JsonObject().also { root.add("players", it) }

    val key = mode.name
    val currentGlobal = global.get(key)?.asInt ?: 0
    if (score > currentGlobal) global.addProperty(key, score)

    val playerObj = players.getAsJsonObject(uuid.toString()) ?: JsonObject().also { players.add(uuid.toString(), it) }
    val currentPlayer = playerObj.get(key)?.asInt ?: 0
    if (score > currentPlayer) playerObj.addProperty(key, score)

    saveData(root)
}

fun GameStatArtifactEntry.topScore(mode: GameMode): Int {
    val root = loadData()
    val global = root.getAsJsonObject("global") ?: return 0
    return global.get(mode.name)?.asInt ?: 0
}

fun GameStatArtifactEntry.playerTopScore(mode: GameMode, uuid: UUID): Int {
    val root = loadData()
    val players = root.getAsJsonObject("players") ?: return 0
    val playerObj = players.getAsJsonObject(uuid.toString()) ?: return 0
    return playerObj.get(mode.name)?.asInt ?: 0
}

