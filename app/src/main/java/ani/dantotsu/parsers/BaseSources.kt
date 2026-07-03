package ani.dantotsu.parsers

import ani.dantotsu.Lazier
import ani.dantotsu.media.Media
import ani.dantotsu.media.anime.Episode
// removed: // removed: // removed: Episode
import ani.dantotsu.tryWithSuspend
import ani.dantotsu.util.Logger
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.source.model.SManga

abstract class WatchSources : BaseSources() {

    override operator fun get(i: Int): AnimeParser {
        return (list.getOrNull(i) ?: list.firstOrNull())?.get?.value as? AnimeParser
            ?: EmptyAnimeParser()
    }

    fun isDownloadedSource(i: Int): Boolean {
        return get(i) is OfflineAnimeParser
    }

    suspend fun loadEpisodesFromMedia(i: Int, media: Media): MutableMap<String, Episode> {
        return tryWithSuspend(true) {
            val res = get(i).autoSearch(media) ?: return@tryWithSuspend mutableMapOf()
            loadEpisodes(i, res.link, res.extra, res.sAnime)
        } ?: mutableMapOf()
    }

    suspend fun loadEpisodes(
        i: Int,
        showLink: String,
        extra: Map<String, String>?,
        sAnime: SAnime?
    ): MutableMap<String, Episode> {
        val map = mutableMapOf<String, Episode>()
        val parser = get(i)
        tryWithSuspend(true) {
            if (sAnime != null) {
                parser.loadEpisodes(showLink, extra, sAnime).forEach {
                    map[it.number] = Episode(
                        it.number,
                        it.link,
                        it.title,
                        it.description,
                        it.thumbnail,
                        it.isFiller,
                        extra = it.extra,
                        sEpisode = it.sEpisode
                    )
                }
            } else if (parser is OfflineAnimeParser) {
                parser.loadEpisodes(showLink, extra, SAnime.create()).forEach {
                    map[it.number] = Episode(
                        it.number,
                        it.link,
                        it.title,
                        it.description,
                        it.thumbnail,
                        it.isFiller,
                        extra = it.extra,
                        sEpisode = it.sEpisode
                    )
                }
            }
        }
        return map
    }

}

abstract class AnimeReadSources : BaseSources() {

    override operator fun get(i: Int): BaseParser {
        return (list.getOrNull(i) ?: list.firstOrNull())?.get?.value as? BaseParser
            ?: EmptyBaseParser()
    }

    suspend fun loadChaptersFromMedia(i: Int, media: Media): MutableMap<String, Episode> {
        return tryWithSuspend(true) {
            val res = get(i).autoSearch(media) ?: return@tryWithSuspend mutableMapOf()
            loadChapters(i, res)
        } ?: mutableMapOf()
    }

    suspend fun loadChapters(i: Int, show: ShowResponse): MutableMap<String, Episode> {
        val map = mutableMapOf<String, Episode>()
        val parser = get(i)

        show.sManga?.let { sManga ->
            tryWithSuspend(true) {
                parser.loadChapters(show.link, show.extra, sManga).forEach {
                    map["${it.number}-${it.scanlator}"] = Episode(it)
                }
            }
        }
        //must be downloaded
        if (show.sManga == null) {
            Logger.log("sManga is null")
        }
        if (parser is OfflineBaseParser && show.sManga == null) {
            tryWithSuspend(true) {
                // Since we've checked, we can safely cast parser to OfflineBaseParser and call its methods
                parser.loadChapters(show.link, show.extra, SManga.create()).forEach {
                    map["${it.number}-${it.scanlator}"] = Episode(it)
                }
            }
        } else {
            Logger.log("Parser is not an instance of OfflineBaseParser")
        }

        Logger.log("map size ${map.size}")
        return map
    }
}

abstract class AnimeReadSources : BaseSources() {
    override operator fun get(i: Int): BaseParser? {
        return if (list.isNotEmpty()) {
            (list.getOrNull(i) ?: list[0]).get.value as BaseParser
        } else {
            return EmptyBaseParser()
        }
    }

    suspend fun loadChaptersFromMedia(i: Int, media: Media): MutableMap<String, Episode> {
        return tryWithSuspend(true) {
            val res = get(i)?.autoSearch(media) ?: return@tryWithSuspend mutableMapOf()
            loadChapters(i, res)
        } ?: mutableMapOf()
    }

    suspend fun loadChapters(i: Int, show: ShowResponse): MutableMap<String, Episode> {
        val map = mutableMapOf<String, Episode>()
        val parser = get(i) ?: return map
        
        tryWithSuspend(true) {
            val book = parser.loadBook(show.link, show.extra)
            book.chapters.forEach { bookChapter ->
                val sChap = eu.kanade.tachiyomi.source.model.SChapter.create().apply {
                    name = bookChapter.name
                    url = bookChapter.link
                    chapter_number = bookChapter.number
                }

                val mangaChapter = Episode(
                    number = bookChapter.name,
                    link = bookChapter.link,
                    title = bookChapter.name,
                    description = null,
                    sChapter = sChap,
                    scanlator = bookChapter.scanlator ?: get(i)?.name,
                    date = 0L,
                    progress = ""
                )
                map["${mangaChapter.number}-${mangaChapter.scanlator}"] = mangaChapter
            }
        }
        return map
    }
}

class EmptyBaseParser : BaseParser() {

    override val volumeRegex: Regex = Regex("")

    override suspend fun loadBook(link: String, extra: Map<String, String>?): Book {
        return Book("", "", null, emptyList())  // Return an empty Book object or some default value
    }

    override suspend fun search(query: String): List<ShowResponse> {
        return listOf() // Return an empty list or some default value
    }
}

abstract class BaseSources {
    abstract val list: List<Lazier<BaseParser>>

    val names: List<String> get() = list.map { it.name }

    fun flushText() {
        list.forEach {
            if (it.get.isInitialized())
                it.get.value?.showUserText = ""
        }
    }

    open operator fun get(i: Int): BaseParser? {
        return list[i].get.value
    }

    fun saveResponse(i: Int, mediaId: Int, response: ShowResponse) {
        get(i)?.saveShowResponse(mediaId, response, true)
    }
}

