package btc.renaud.archery.init

import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import org.slf4j.LoggerFactory

/**
 * Initializes the archery game extension. It currently only logs when the
 * extension is loaded and unloaded. The object is kept stateless as required by
 * Typewriter's extension model.
 */
@Singleton
object ArcheryInitializer : Initializable {
    private val logger = LoggerFactory.getLogger("ArcheryInitializer")

    override suspend fun initialize() {
        logger.info("Archery extension initializing")
    }

    override suspend fun shutdown() {
        logger.info("Archery extension shutting down")
    }
}


