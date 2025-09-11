package btc.renaud.archery.entry

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.entries.ArtifactEntry
import com.typewritermc.core.entries.Entry as TWEntry

/**
 * Stores the persistent configuration for an archery arena. The `artifactId`
 * is constant and used by the AssetManager to read or write the JSON data.
 */
@Tags("arena")
@Entry("arena_artifact", "Persistent configuration for an archery arena", Colors.MEDIUM_PURPLE, "mdi:target")
class ArenaArtifactEntry(
    override val id: String = "",
    override val name: String = "",
    override val artifactId: String = "renaud:archery_arena",
) : ArtifactEntry, TWEntry

