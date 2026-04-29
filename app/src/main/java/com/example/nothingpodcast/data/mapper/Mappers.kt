package com.example.nothingpodcast.data.mapper

import com.example.nothingpodcast.data.local.database.entity.EpisodeEntity
import com.example.nothingpodcast.data.local.database.entity.PodcastEntity
import com.example.nothingpodcast.data.remote.api.ItunesPodcastDto
import com.example.nothingpodcast.domain.model.Chapter
import com.example.nothingpodcast.domain.model.Episode
import com.example.nothingpodcast.domain.model.Podcast
import com.example.nothingpodcast.domain.model.Person
import com.example.nothingpodcast.domain.model.Soundbite
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ── iTunes DTO → Domain ──────────────────────────────────────────────────

fun ItunesPodcastDto.toDomain(): Podcast = Podcast(
    id          = collectionId.toString(),
    title       = collectionName,
    author      = artistName,
    description = description ?: "",
    imageUrl    = artworkUrl600.ifBlank { artworkUrl100 },
    feedUrl     = feedUrl ?: "",
    episodeCount = trackCount
)

// ── Domain ↔ Entity ──────────────────────────────────────────────────────

private val gson = Gson()

fun Podcast.toEntity(): PodcastEntity = PodcastEntity(
    id          = id,
    title       = title,
    author      = author,
    description = description,
    imageUrl    = imageUrl,
    feedUrl     = feedUrl,
    episodeCount = episodeCount,
    isSubscribed = isSubscribed,
    lastUpdated = lastUpdated,
    orderIndex  = orderIndex,
    fundingUrl = fundingUrl,
    fundingText = fundingText,
    personsJson = if (persons.isNotEmpty()) gson.toJson(persons) else null,
    locationName = locationName,
    locationGeo = locationGeo,
    socialInteractUrl = socialInteractUrl,
    medium = medium,
    podcastGuid = podcastGuid,
    licenseUrl = licenseUrl,
    licenseName = licenseName
)

fun PodcastEntity.toDomain(unplayedCount: Int = 0): Podcast = Podcast(
    id           = id,
    title        = title,
    author       = author,
    description  = description,
    imageUrl     = imageUrl,
    feedUrl      = feedUrl,
    episodeCount = episodeCount,
    isSubscribed = isSubscribed,
    lastUpdated  = lastUpdated,
    unplayedCount = unplayedCount,
    orderIndex   = orderIndex,
    fundingUrl   = fundingUrl,
    fundingText  = fundingText,
    persons      = if (!personsJson.isNullOrBlank()) {
        val type = object : TypeToken<List<Person>>() {}.type
        gson.fromJson(personsJson, type)
    } else emptyList(),
    locationName = locationName,
    locationGeo  = locationGeo,
    socialInteractUrl = socialInteractUrl,
    medium = medium,
    podcastGuid = podcastGuid,
    licenseUrl = licenseUrl,
    licenseName = licenseName
)

fun EpisodeEntity.toDomain(): Episode {
    val chaptersType = object : TypeToken<List<Chapter>>() {}.type
    val chaptersList: List<Chapter> = if (!chaptersJson.isNullOrBlank()) {
        gson.fromJson(chaptersJson, chaptersType)
    } else emptyList()

    return Episode(
        id = id,
        podcastId = podcastId,
        podcastTitle = podcastTitle,
        podcastImageUrl = podcastImageUrl,
        title = title,
        description = description,
        audioUrl = audioUrl,
        imageUrl = imageUrl,
        duration = duration,
        publishDate = publishDate,
        isPlayed = isPlayed,
        playbackPosition = playbackPosition,
        isDownloaded = isDownloaded,
        downloadPath = downloadPath,
        fileSize = fileSize,
        chapters = chaptersList,
        transcriptUrl = transcriptUrl,
        transcriptType = transcriptType,
        soundbites = if (!soundbitesJson.isNullOrBlank()) {
            val type = object : TypeToken<List<Soundbite>>() {}.type
            gson.fromJson(soundbitesJson, type)
        } else emptyList(),
        podcastGuid = podcastGuid,
        season = season,
        episodeNumber = episodeNumber,
        episodeType = episodeType
    )
}

fun Episode.toEntity(): EpisodeEntity {
    return EpisodeEntity(
        id = id,
        podcastId = podcastId,
        podcastTitle = podcastTitle,
        podcastImageUrl = podcastImageUrl,
        title = title,
        description = description,
        audioUrl = audioUrl,
        imageUrl = imageUrl,
        duration = duration,
        publishDate = publishDate,
        isPlayed = isPlayed,
        playbackPosition = playbackPosition,
        isDownloaded = isDownloaded,
        downloadPath = downloadPath,
        fileSize = fileSize,
        chaptersJson = if (chapters.isNotEmpty()) gson.toJson(chapters) else null,
        transcriptUrl = transcriptUrl,
        transcriptType = transcriptType,
        soundbitesJson = if (soundbites.isNotEmpty()) gson.toJson(soundbites) else null,
        podcastGuid = podcastGuid,
        season = season,
        episodeNumber = episodeNumber,
        episodeType = episodeType
    )
}
