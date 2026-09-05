package com.miadfm.podcasts.data.podcast

data class ContentAttribution(
    val contentTitle: String,
    val creatorName: String,
    val sourceName: String,
    val licenseName: String,
    val licenseVersion: String? = null,
    val licenseUrl: String? = null,
    val details: String? = null
)

data class Podcast(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val category: String,
    val totalEpisodes: Int,
    val titleFa: String? = null,
    val authorFa: String? = null,
    val descriptionFa: String? = null,
    val categoryFa: String? = null,
    val attribution: ContentAttribution? = null
)

data class Episode(
    val id: String,
    val podcastId: String,
    val episodeNumber: Int,
    val title: String,
    val titleFa: String,
    val creator: String? = null,
    val creatorFa: String? = null,
    val contentType: String = "Podcast",
    val contentTypeFa: String = "پادکست",
    val assetFileName: String = "",
    val assetPath: String = "",
    val description: String,
    val descriptionFa: String,
    val publishDate: String,
    val publishDateFa: String,
    val durationSeconds: Int,
    val formattedDuration: String,
    val formattedDurationFa: String,
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long? = null,
    val attribution: ContentAttribution? = null
)
