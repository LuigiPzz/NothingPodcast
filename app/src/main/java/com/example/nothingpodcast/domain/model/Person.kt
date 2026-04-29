package com.example.nothingpodcast.domain.model

/**
 * Represents a person involved in a podcast (Host, Guest, Producer, etc.)
 * Part of Podcasting 2.0 standard.
 */
data class Person(
    val name: String,
    val role: String? = null,
    val imageUrl: String? = null,
    val link: String? = null
)
