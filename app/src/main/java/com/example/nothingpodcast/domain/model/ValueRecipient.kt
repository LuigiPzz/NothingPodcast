package com.example.nothingpodcast.domain.model

/**
 * Represents a recipient in the Value for Value (V4V) system.
 * Part of Podcasting 2.0 standard.
 */
data class ValueRecipient(
    val name: String? = null,
    val type: String = "node",
    val address: String, // Public key for Lightning
    val split: Int = 100, // Percentage
    val customKey: String? = null,
    val customValue: String? = null
)
