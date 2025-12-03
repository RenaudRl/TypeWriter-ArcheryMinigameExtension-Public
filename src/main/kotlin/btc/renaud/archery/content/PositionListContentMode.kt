package btc.renaud.archery.content

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.typewritermc.core.entries.Entry
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.pageId
import com.typewritermc.core.interaction.context
import com.typewritermc.core.utils.failure
import com.typewritermc.core.utils.ok
import com.typewritermc.engine.paper.content.ContentContext
import com.typewritermc.engine.paper.content.entryId
import com.typewritermc.engine.paper.content.fieldPath
import com.typewritermc.engine.paper.content.modes.ImmediateFieldValueContentMode
import com.typewritermc.engine.paper.entry.entries.InteractionEndTrigger
import com.typewritermc.engine.paper.entry.fieldValue
import com.typewritermc.engine.paper.logger
import com.typewritermc.engine.paper.plugin
import com.typewritermc.engine.paper.entry.StagingManager
import com.typewritermc.engine.paper.entry.triggerFor
import com.typewritermc.engine.paper.ui.ClientSynchronizer
import com.typewritermc.core.utils.point.Position
import com.typewritermc.core.utils.point.World
import com.typewritermc.engine.paper.utils.round
import kotlinx.coroutines.delay
import org.bukkit.entity.Player
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent
import java.lang.reflect.Type
import kotlin.time.Duration.Companion.milliseconds

/**
 * Content mode that captures the player's current position and appends it to a
 * list field instead of overwriting it. This prevents list fields such as
 * `targetPositions` from being corrupted by a single captured object.
 */
class PositionListContentMode(context: ContentContext, player: Player) :
    ImmediateFieldValueContentMode<Position>(context, player) {

    override val type: Type = Position::class.java

    override suspend fun setup(): Result<Unit> {
        val entryId = context.entryId
            ?: return failure("No entryId found for PositionListContentMode. This is a bug, please report it.")
        val fieldPath = context.fieldPath
            ?: return failure("No fieldPath found for PositionListContentMode. This is a bug, please report it.")

        plugin.launch {
            delay(200.milliseconds)
            try {
                appendPosition(entryId, fieldPath, value())
            } catch (e: Exception) {
                logger.severe("Failed to append captured position to $fieldPath for entry $entryId")
                e.printStackTrace()
            } finally {
                InteractionEndTrigger.triggerFor(player, context())
            }
        }

        return ok(Unit)
    }

    override fun value(): Position {
        val location = player.location
        return Position(
            World(location.world.name),
            location.x.round(2),
            location.y.round(2),
            location.z.round(2),
            location.yaw.round(2),
            location.pitch.round(2),
        )
    }

    private fun appendPosition(entryId: String, fieldPath: String, position: Position) {
        val pageId = Ref(entryId, Entry::class).pageId
        if (pageId == null) {
            logger.warning("No pageId found for entry $entryId when capturing position; falling back to direct set.")
            Ref(entryId, Entry::class).fieldValue(fieldPath, listOf(position))
            return
        }

        val stagingManager = KoinJavaComponent.get<StagingManager>(StagingManager::class.java)
        val gson = KoinJavaComponent.get<Gson>(Gson::class.java, named("dataSerializer"))
        val clientSynchronizer = KoinJavaComponent.get<ClientSynchronizer>(ClientSynchronizer::class.java)

        val page = stagingManager.pages[pageId]
        val entry = page
            ?.getAsJsonArray("entries")
            ?.firstOrNull { it.asJsonObject["id"]?.asString == entryId }
            ?.asJsonObject

        val existing = entry?.get(fieldPath)
        val positionJson = gson.toJsonTree(position, type)
        val newValue: JsonElement = when {
            existing == null || existing.isJsonNull -> JsonArray().apply { add(positionJson) }
            existing.isJsonArray -> existing.deepCopy().asJsonArray.apply { add(positionJson) }
            else -> JsonArray().apply {
                add(existing.deepCopy())
                add(positionJson)
            }
        }

        val result = stagingManager.updateEntryField(pageId, entryId, fieldPath, newValue)
        if (result.isFailure) {
            logger.warning("Failed to update $fieldPath for entry $entryId: ${result.exceptionOrNull()?.message}")
            return
        }

        clientSynchronizer.sendEntryFieldUpdate(pageId, entryId, fieldPath, newValue)
    }
}
