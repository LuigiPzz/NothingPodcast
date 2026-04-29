package com.example.nothingpodcast.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesApiService {
    @GET("search")
    suspend fun searchPodcasts(
        @Query("term") term: String,
        @Query("media") media: String = "podcast",
        @Query("entity") entity: String = "podcast",
        @Query("limit") limit: Int = 30,
        @Query("country") country: String = "IT"
    ): ItunesSearchResponse
}

data class ItunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ItunesPodcastDto> = emptyList()
)

data class ItunesPodcastDto(
    val collectionId: Long = 0L,
    val collectionName: String = "",
    val artistName: String = "",
    val feedUrl: String? = null,
    val artworkUrl600: String = "",
    val artworkUrl100: String = "",
    val trackCount: Int = 0,
    val primaryGenreName: String = "",
    val description: String? = null
)
