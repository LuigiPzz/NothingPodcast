package com.example.nothingpodcast.data.remote.rss

import kotlinx.coroutines.*
import org.json.JSONObject
import android.util.Xml
import com.example.nothingpodcast.data.local.database.entity.EpisodeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import com.google.gson.Gson
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.text.HtmlCompat

@Singleton
class RssFeedParser @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val ns: String? = null

    // RSS date formats (podcasts use varied formats)
    private val dateFormats = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
    )

    /**
     * Fetch and parse RSS feed, returning a list of EpisodeEntity.
     * Network call is blocking — call from a coroutine (IO dispatcher).
     */
    @Throws(IOException::class, XmlPullParserException::class)
    suspend fun parseEpisodes(
        feedUrl: String,
        podcastId: String,
        podcastTitle: String,
        podcastImageUrl: String
    ): List<EpisodeEntity> = coroutineScope {
        val request = Request.Builder().url(feedUrl).build()
        val rawEpisodesWithUrl = withContext(Dispatchers.IO) {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $feedUrl")
                val stream = response.body?.byteStream() ?: throw IOException("Empty feed body")

                val parser = Xml.newPullParser().apply {
                    setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    setInput(stream, null)
                }
                parseFeed(parser, podcastId, podcastTitle, podcastImageUrl)
            }
        }
        
        // Fetch Podcasting 2.0 chapters concurrently
        val fetchJobs = rawEpisodesWithUrl.map { (ep, url) ->
            async(Dispatchers.IO) {
                if (url.isNullOrBlank()) return@async ep
                
                try {
                    val req = Request.Builder().url(url).build()
                    okHttpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val json = resp.body?.string()
                            if (json != null) {
                                val jsonObject = JSONObject(json)
                                if (jsonObject.has("chapters")) {
                                    val chaptersArray = jsonObject.getJSONArray("chapters")
                                    val extracted = mutableListOf<com.example.nothingpodcast.domain.model.Chapter>()
                                    for (i in 0 until chaptersArray.length()) {
                                        val chapterObj = chaptersArray.getJSONObject(i)
                                        val title = chapterObj.optString("title")
                                        val startTime = chapterObj.optLong("startTime")
                                        if (title.isNotBlank()) {
                                            extracted.add(com.example.nothingpodcast.domain.model.Chapter(title, startTime))
                                        }
                                    }
                                    if (extracted.isNotEmpty()) {
                                        return@async ep.copy(chaptersJson = com.google.gson.Gson().toJson(extracted))
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to original
                }
                ep
            }
        }
        
        fetchJobs.awaitAll()
    }

    /**
     * Fetch and parse podcast metadata (title, author, image) from RSS.
     */
    suspend fun fetchPodcastMetadata(feedUrl: String): com.example.nothingpodcast.domain.model.Podcast = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(feedUrl).build()
        
        var title = ""
        var author = ""
        var description = ""
        var imageUrl = ""
        var fundingUrl: String? = null
        var fundingText: String? = null
        val persons = mutableListOf<com.example.nothingpodcast.domain.model.Person>()
        var locationName: String? = null
        var locationGeo: String? = null
        var socialInteractUrl: String? = null
        var medium: String? = null
        var podcastGuid: String? = null
        var licenseUrl: String? = null
        var licenseName: String? = null
        
        withContext(Dispatchers.IO) {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $feedUrl")
                val stream = response.body?.byteStream() ?: throw IOException("Empty feed body")

                val parser = Xml.newPullParser().apply {
                    setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    setInput(stream, null)
                }

                parser.nextTag()
                parser.require(XmlPullParser.START_TAG, ns, "rss")
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType != XmlPullParser.START_TAG) continue
                    if (parser.name == "channel") {
                        val depth = parser.depth
                        while (!(parser.next() == XmlPullParser.END_TAG && parser.name == "channel" && parser.depth == depth)) {
                            if (parser.eventType != XmlPullParser.START_TAG) continue
                            when (parser.name) {
                                "title" -> if (parser.depth == depth + 1) title = readText(parser) else skipTag(parser)
                                "itunes:author" -> if (parser.depth == depth + 1) author = readText(parser) else skipTag(parser)
                                "description" -> if (parser.depth == depth + 1) description = readText(parser).cleanHtml() else skipTag(parser)
                                "itunes:image" -> {
                                    if (parser.depth == depth + 1) {
                                        imageUrl = parser.getAttributeValue(null, "href") ?: ""
                                    }
                                    skipTag(parser)
                                }
                                "podcast:funding" -> {
                                    if (parser.depth == depth + 1) {
                                        fundingUrl = parser.getAttributeValue(null, "url")
                                        fundingText = readText(parser)
                                    } else skipTag(parser)
                                }
                                "podcast:person" -> {
                                    if (parser.depth == depth + 1) {
                                        val role = parser.getAttributeValue(null, "role")
                                        val link = parser.getAttributeValue(null, "href")
                                        val img = parser.getAttributeValue(null, "img")
                                        val name = readText(parser)
                                        if (name.isNotBlank()) {
                                            persons.add(com.example.nothingpodcast.domain.model.Person(name, role, img, link))
                                        }
                                    } else skipTag(parser)
                                }
                                "podcast:location" -> {
                                    if (parser.depth == depth + 1) {
                                        locationGeo = parser.getAttributeValue(null, "geo")
                                        locationName = readText(parser)
                                    } else skipTag(parser)
                                }
                                "podcast:socialInteract" -> {
                                    if (parser.depth == depth + 1) {
                                        socialInteractUrl = parser.getAttributeValue(null, "uri") ?: parser.getAttributeValue(null, "url")
                                    }
                                    skipTag(parser)
                                }
                                "podcast:medium" -> {
                                    if (parser.depth == depth + 1) {
                                        medium = readText(parser)
                                    } else skipTag(parser)
                                }
                                "podcast:guid" -> {
                                    if (parser.depth == depth + 1) {
                                        podcastGuid = readText(parser)
                                    } else skipTag(parser)
                                }
                                "podcast:license" -> {
                                    if (parser.depth == depth + 1) {
                                        licenseUrl = parser.getAttributeValue(null, "url")
                                        licenseName = readText(parser)
                                    } else skipTag(parser)
                                }
                                else -> skipTag(parser)
                            }
                        }
                    } else {
                        skipTag(parser)
                    }
                }
            }
        }

        com.example.nothingpodcast.domain.model.Podcast(
            id = feedUrl.hashCode().toString(),
            title = title.cleanHtml().takeIf { it.isNotBlank() } ?: "Unknown Podcast",
            author = author.cleanHtml().takeIf { it.isNotBlank() } ?: "Unknown Author",
            description = description.cleanHtml(),
            imageUrl = imageUrl,
            feedUrl = feedUrl,
            isSubscribed = true,
            fundingUrl = fundingUrl,
            fundingText = fundingText,
            persons = persons,
            locationName = locationName,
            locationGeo = locationGeo,
            socialInteractUrl = socialInteractUrl,
            medium = medium,
            podcastGuid = podcastGuid,
            licenseUrl = licenseUrl,
            licenseName = licenseName
        )
    }

    private fun parseFeed(
        parser: XmlPullParser,
        podcastId: String,
        podcastTitle: String,
        podcastImageUrl: String
    ): List<Pair<EpisodeEntity, String?>> {
        val episodes = mutableListOf<Pair<EpisodeEntity, String?>>()
        parser.nextTag()
        parser.require(XmlPullParser.START_TAG, ns, "rss")
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name == "channel") {
                episodes.addAll(
                    parseChannel(parser, podcastId, podcastTitle, podcastImageUrl)
                )
            }
        }
        return episodes
    }

    private fun parseChannel(
        parser: XmlPullParser,
        podcastId: String,
        podcastTitle: String,
        podcastImageUrl: String
    ): List<Pair<EpisodeEntity, String?>> {
        val episodes = mutableListOf<Pair<EpisodeEntity, String?>>()
        while (!(parser.next() == XmlPullParser.END_TAG && parser.name == "channel")) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name == "item") {
                parseItem(parser, podcastId, podcastTitle, podcastImageUrl)
                    ?.let { episodes.add(it) }
            } else {
                skipTag(parser)
            }
        }
        return episodes
    }

    private fun parseItem(
        parser: XmlPullParser,
        podcastId: String,
        podcastTitle: String,
        podcastImageUrl: String
    ): Pair<EpisodeEntity, String?>? {
        val itemDepth = parser.depth
        var guid: String? = null
        var title: String? = null
        var description: String? = null
        var audioUrl: String? = null
        var duration: Long = 0L
        var publishDate: Long = 0L
        var imageUrl: String = podcastImageUrl
        var fileSize: Long = 0L
        var remoteChaptersUrl: String? = null
        var transcriptUrl: String? = null
        var transcriptType: String? = null
        val soundbites = mutableListOf<com.example.nothingpodcast.domain.model.Soundbite>()
        var podcastGuid: String? = null
        var season: Int? = null
        var episodeNumber: Int? = null
        var episodeType: String? = null
        val htmlChapters = mutableListOf<com.example.nothingpodcast.domain.model.Chapter>()

        while (true) {
            val eventType = parser.next()
            if (eventType == XmlPullParser.END_TAG && parser.name == "item" && parser.depth == itemDepth) break
            if (eventType != XmlPullParser.START_TAG) continue

            val isDirectChild = parser.depth == itemDepth + 1

            when (parser.name) {
                "guid"              -> if (isDirectChild) guid = readText(parser)
                "title"             -> if (isDirectChild) title = readText(parser)
                "description"       -> if (isDirectChild && description.isNullOrBlank()) description = readText(parser)
                "content:encoded"   -> if (isDirectChild) description = readText(parser)
                "itunes:summary"    -> if (isDirectChild && description.isNullOrBlank()) description = readText(parser)
                "enclosure"         -> if (isDirectChild) {
                    audioUrl = parser.getAttributeValue(null, "url")
                    fileSize = parser.getAttributeValue(null, "length")?.toLongOrNull() ?: 0L
                    skipTag(parser)
                }
                "itunes:duration"   -> if (isDirectChild) duration = parseDuration(readText(parser))
                "pubDate"           -> if (isDirectChild) publishDate = parseDate(readText(parser))
                "itunes:image"      -> if (isDirectChild) {
                    val href = parser.getAttributeValue(null, "href")
                    if (!href.isNullOrBlank()) imageUrl = href
                    skipTag(parser)
                }
                "itunes:season" -> if (isDirectChild) season = readText(parser).toIntOrNull()
                "itunes:episode" -> if (isDirectChild) episodeNumber = readText(parser).toIntOrNull()
                "itunes:episodeType" -> if (isDirectChild) episodeType = readText(parser)
                "podcast:chapters"  -> if (isDirectChild) {
                    val type = parser.getAttributeValue(null, "type")
                    if (type == "application/json+chapters") {
                        remoteChaptersUrl = parser.getAttributeValue(null, "url")
                    }
                    skipTag(parser)
                }
                "podcast:transcript" -> if (isDirectChild) {
                    transcriptUrl = parser.getAttributeValue(null, "url")
                    transcriptType = parser.getAttributeValue(null, "type")
                    skipTag(parser)
                }
                "podcast:guid" -> if (isDirectChild) {
                    podcastGuid = readText(parser)
                }
                "podcast:soundbite" -> if (isDirectChild) {
                    val startStr = parser.getAttributeValue(null, "startTime")
                    val durStr = parser.getAttributeValue(null, "duration")
                    val sTitle = readText(parser)
                    val sStart = startStr?.toDoubleOrNull()?.toLong() ?: 0L
                    val sDur = durStr?.toDoubleOrNull()?.toLong() ?: 0L
                    if (sStart >= 0) {
                        soundbites.add(com.example.nothingpodcast.domain.model.Soundbite(sTitle, sStart, sDur))
                    }
                }
                "button"            -> if (isDirectChild) {
                    val startTimeStr = parser.getAttributeValue(null, "data-start-time")
                    val buttonTitle = parser.getAttributeValue(null, "data-title")
                    if (startTimeStr != null && buttonTitle != null) {
                        val timeSeconds = startTimeStr.toLongOrNull() ?: 0L
                        if (buttonTitle.isNotBlank()) {
                            htmlChapters.add(com.example.nothingpodcast.domain.model.Chapter(buttonTitle, timeSeconds))
                        }
                    }
                    skipTag(parser)
                }
                else -> skipTag(parser)
            }
        }

        val resolvedGuid = guid ?: audioUrl ?: return null
        if (audioUrl.isNullOrBlank()) return null

        val rawDescription = description ?: ""
        val cleanedDescription = rawDescription.cleanHtml()
        
        var finalChapters = htmlChapters.distinctBy { it.startTime }.sortedBy { it.startTime }
        if (finalChapters.isEmpty()) {
            finalChapters = extractChapters(rawDescription)
        }

        val episodeEntity = EpisodeEntity(
            id = "$podcastId-$resolvedGuid",
            podcastId = podcastId,
            podcastTitle = podcastTitle,
            podcastImageUrl = podcastImageUrl,
            title = title?.cleanHtml() ?: "Untitled Episode",
            description = cleanedDescription,
            audioUrl = audioUrl,
            imageUrl = imageUrl,
            duration = duration,
            publishDate = publishDate,
            fileSize = fileSize,
            chaptersJson = if (finalChapters.isNotEmpty()) Gson().toJson(finalChapters) else null,
            transcriptUrl = transcriptUrl,
            transcriptType = transcriptType,
            soundbitesJson = if (soundbites.isNotEmpty()) Gson().toJson(soundbites) else null,
            podcastGuid = podcastGuid,
            season = season,
            episodeNumber = episodeNumber,
            episodeType = episodeType
        )
        return Pair(episodeEntity, remoteChaptersUrl)
    }

    private fun extractChapters(description: String): List<com.example.nothingpodcast.domain.model.Chapter> {
        val chapters = mutableListOf<com.example.nothingpodcast.domain.model.Chapter>()
        
        // 1. Try to find embedded HTML chapters (like Digitalia's custom player)
        // Matches: data-start-time="155" data-title="2. Castamatic 13"
        val htmlChapterRegex = Regex("data-start-time=\"(\\d+)\"\\s*data-title=\"([^\"]+)\"")
        val htmlMatches = htmlChapterRegex.findAll(description).toList()
        if (htmlMatches.isNotEmpty()) {
            htmlMatches.forEach { match ->
                val timeSeconds = match.groupValues[1].toLongOrNull() ?: 0L
                val title = match.groupValues[2].trim()
                if (title.isNotBlank()) {
                    chapters.add(com.example.nothingpodcast.domain.model.Chapter(title, timeSeconds))
                }
            }
            return chapters.distinctBy { it.startTime }.sortedBy { it.startTime }
        }

        // 2. Fallback to standard text-based timestamp parsing
        // Improved Regex: matches timestamps like 00:00, 0:00:00, [00:00], (00:00)
        // It looks for a sequence of digits and colons at the start of a line or after a break.
        val regex = Regex("(?m)^\\s*[\\(\\[]?(\\d{1,2}:\\d{2}(?::\\d{2})?)[\\)\\]]?\\s*[-–—]?\\s*(.*)")
        
        // We also try searching without the line start anchor for more flexibility
        val flexibleRegex = Regex("[\\(\\[]?(\\d{1,2}:\\d{2}(?::\\d{2})?)[\\)\\]]?\\s*[-–—]\\s*(.*)")

        val lines = description.replace("<br>", "\n").replace("<p>", "\n").replace("</p>", "\n").cleanHtml().lines()
        
        lines.forEach { line ->
            val match = regex.find(line) ?: flexibleRegex.find(line)
            if (match != null) {
                val timeStr = match.groupValues[1]
                val title = match.groupValues[2].trim()
                val timeSeconds = parseDuration(timeStr)
                if (title.isNotBlank() && title.length < 100) { // Avoid false positives with long text
                    chapters.add(com.example.nothingpodcast.domain.model.Chapter(title, timeSeconds))
                }
            }
        }
        
        // Deduplicate and sort by time
        return chapters.distinctBy { it.startTime }.sortedBy { it.startTime }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text ?: ""
            parser.nextTag()
        }
        return result
    }

    private fun skipTag(parser: XmlPullParser) {
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG   -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    /** Parses HH:MM:SS or MM:SS or plain seconds */
    private fun parseDuration(raw: String): Long {
        val parts = raw.trim().split(":").map { it.toLongOrNull() ?: 0L }
        return when (parts.size) {
            3    -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            2    -> parts[0] * 60 + parts[1]
            1    -> parts[0]
            else -> 0L
        }
    }

    private fun parseDate(raw: String): Long {
        for (fmt in dateFormats) {
            try { return fmt.parse(raw.trim())?.time ?: 0L } catch (_: Exception) {}
        }
        return 0L
    }

    private fun String.cleanHtml(): String {
        if (this.isBlank()) return ""
        return try {
            HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
        } catch (e: Exception) {
            // Fallback to manual cleaning if HtmlCompat fails or for very old APIs (though minSdk is 26)
            this.replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").trim()
        }
    }
}
