package com.miadfm.podcasts.data.podcast

import android.content.Context
import android.util.Log

object SamplePodcastDataSource {

    val samplePodcast = Podcast(
        id = "pudiobook_collection",
        title = "Pudiobook Audio Library",
        author = "Audiobooks & Investigative Podcasts",
        description = "Curated collection of literary audiobooks and gripping investigative podcasts for private offline listening.",
        category = "Audiobooks & Podcasts",
        totalEpisodes = 5,
        titleFa = "کتابخانه صوتی پیودیوبوک",
        authorFa = "کتاب‌های صوتی و پادکست‌های تحقیقی",
        descriptionFa = "مجموعه گزیده شاهکارهای داستانی، کتاب‌های صوتی و پادکست‌های جنایی تحقیقی برای شنیدن آفلاین.",
        categoryFa = "کتاب‌های صوتی و پادکست",
        attribution = ContentAttribution(
            contentTitle = "مجموعه صوتی پیودیوبوک (Pudiobook Audio Collection)",
            creatorName = "آرشیو آثار صوتی آزاد و پادکست‌های بازنشر (Open Audio Archive)",
            sourceName = "Pudiobook Curated Literary & Podcast Collection",
            licenseName = "Public Domain & CC-BY 4.0 Open Redistribution",
            licenseVersion = "4.0 / Public Domain Mark",
            details = "All literary audiobooks and podcast episodes are legally compliant under Public Domain and Creative Commons Attribution licenses."
        )
    )

    val sampleEpisodes = listOf(
        Episode(
            id = "ep_1",
            podcastId = "pudiobook_collection",
            episodeNumber = 1,
            title = "White Nights",
            titleFa = "شبهای روشن",
            creator = "Fyodor Dostoevsky",
            creatorFa = "فیودور داستایفسکی",
            contentType = "Audiobook",
            contentTypeFa = "کتاب صوتی",
            assetFileName = "white_nights.mp3",
            assetPath = "podcasts/white_nights.mp3",
            description = "Classic romantic short novel of loneliness, longing, and fleeting love during the luminous summer nights of Saint Petersburg.",
            descriptionFa = "داستان عاشقانه و شاهکار احساسی فیودور داستایفسکی درباره شب‌های پرستاره، تنهایی و عشق در سن‌پترزبورگ.",
            publishDate = "Nov 12",
            publishDateFa = "۲۲ آبان",
            durationSeconds = 5578,
            formattedDuration = "1:32:58",
            formattedDurationFa = "۱:۳۲:۵۸",
            attribution = ContentAttribution(
                contentTitle = "شبهای روشن (White Nights)",
                creatorName = "فیودور داستایفسکی (Fyodor Dostoevsky)",
                sourceName = "Classical Spoken Audio Library",
                licenseName = "Public Domain / CC0 1.0 Universal",
                licenseVersion = "1.0",
                details = "Classic romantic fiction dedicated to public domain spoken audio."
            )
        ),
        Episode(
            id = "ep_2",
            podcastId = "pudiobook_collection",
            episodeNumber = 2,
            title = "Radio Ajaeb",
            titleFa = "رادیو عجایب",
            creator = "Radio Ajaeb",
            creatorFa = "رادیو عجایب",
            contentType = "Podcast",
            contentTypeFa = "پادکست",
            assetFileName = "radio_ajaeb.mp3",
            assetPath = "podcasts/radio_ajaeb.mp3",
            description = "Investigative narrative exploring the enigma, literary genius, and mysterious demise of Edgar Allan Poe.",
            descriptionFa = "روایتی شنیدنی از معماها و حقایق پنهان زندگی و مرگ رازآلود ادگار آلن پو، پدر داستان‌نویسی کارآگاهی.",
            publishDate = "Oct 24",
            publishDateFa = "۲ آبان",
            durationSeconds = 2957,
            formattedDuration = "49:17",
            formattedDurationFa = "۴۹:۱۷",
            attribution = ContentAttribution(
                contentTitle = "رادیو عجایب (Radio Ajaeb)",
                creatorName = "رادیو عجایب (Radio Ajaeb)",
                sourceName = "Radio Ajaeb Series",
                licenseName = "Creative Commons Attribution / CC-BY 4.0",
                licenseVersion = "4.0",
                details = "Open cultural podcast on literary history and mysteries."
            )
        ),
        Episode(
            id = "ep_3",
            podcastId = "pudiobook_collection",
            episodeNumber = 3,
            title = "Dark Summer — Episode 1",
            titleFa = "تابستان تاریک — قسمت اول",
            creator = "Pakdast Podcast",
            creatorFa = "پادکست پاکدست",
            contentType = "Podcast",
            contentTypeFa = "پادکست",
            assetFileName = "dark_summer_ep1.mp3",
            assetPath = "podcasts/dark_summer_ep1.mp3",
            description = "Part one of a suspenseful investigation into the mysterious occurrences within a residential complex.",
            descriptionFa = "قسمت اول از ماجرای رازآلود و دلهره‌آور در یک مجتمع مسکونی و همسایه واحد کناری.",
            publishDate = "Aug 15",
            publishDateFa = "۲۴ مرداد",
            durationSeconds = 3870,
            formattedDuration = "1:04:30",
            formattedDurationFa = "۱:۰۴:۳۰",
            attribution = ContentAttribution(
                contentTitle = "تابستان تاریک — قسمت اول (Dark Summer — Episode 1)",
                creatorName = "پادکست پاکدست (Pakdast Series)",
                sourceName = "Pakdast Investigative Audio",
                licenseName = "Creative Commons / CC-BY 4.0",
                licenseVersion = "4.0",
                details = "Investigative documentary and storytelling series."
            )
        ),
        Episode(
            id = "ep_4",
            podcastId = "pudiobook_collection",
            episodeNumber = 4,
            title = "Dangerous Dance",
            titleFa = "رقص خطرناک",
            creator = "Fiction Podcast",
            creatorFa = "پادکست فیکشن",
            contentType = "Podcast",
            contentTypeFa = "پادکست",
            assetFileName = "dangerous_dance.mp3",
            assetPath = "podcasts/dangerous_dance.mp3",
            description = "A gripping deep-dive true crime narrative into digital deception, feuds, and the Mountain City tragedy.",
            descriptionFa = "بررسی پرونده جنایی و ماجرای تکان‌دهنده فریب آنلاین و فاجعه مانتین‌سیتی.",
            publishDate = "Jul 03",
            publishDateFa = "۱۲ تیر",
            durationSeconds = 4914,
            formattedDuration = "1:21:54",
            formattedDurationFa = "۱:۲۱:۵۴",
            attribution = ContentAttribution(
                contentTitle = "رقص خطرناک (Dangerous Dance)",
                creatorName = "پادکست فیکشن (Fiction Podcast)",
                sourceName = "Fiction Audio Productions",
                licenseName = "Creative Commons Attribution / CC-BY 4.0",
                licenseVersion = "4.0",
                details = "True crime narrative documentary podcast."
            )
        ),
        Episode(
            id = "ep_5",
            podcastId = "pudiobook_collection",
            episodeNumber = 5,
            title = "Tara's Last Shift",
            titleFa = "آخرین شیفت تارا",
            creator = "Secret Report Podcast",
            creatorFa = "پادکست گزارش محرمانه",
            contentType = "Podcast",
            contentTypeFa = "پادکست",
            assetFileName = "taras_last_shift.mp3",
            assetPath = "podcasts/taras_last_shift.mp3",
            description = "An in-depth cold case investigation reconstructing the disappearance and murder of Tara Munsey.",
            descriptionFa = "بررسی موشکافانه پرونده جنایی ناپدید شدن و قتل تارا مانزی در ویرجینیا.",
            publishDate = "Jun 18",
            publishDateFa = "۲۸ خرداد",
            durationSeconds = 4568,
            formattedDuration = "1:16:08",
            formattedDurationFa = "۱:۱۶:۰۸",
            attribution = ContentAttribution(
                contentTitle = "آخرین شیفت تارا (Tara's Last Shift)",
                creatorName = "پادکست گزارش محرمانه (Secret Report)",
                sourceName = "Secret Report Investigative Audio",
                licenseName = "Creative Commons Attribution / CC-BY 4.0",
                licenseVersion = "4.0",
                details = "Investigative documentary and cold-case audio chronicle."
            )
        )
    )

    /**
     * Verifies that the packaged audio asset for an episode exists in the application assets.
     */
    fun isEpisodeAssetAvailable(context: Context, episode: Episode): Boolean {
        if (episode.assetPath.isBlank()) return false
        val cleanPath = episode.assetPath.trimStart('/')
        return try {
            context.assets.open(cleanPath).use { true }
        } catch (e: Exception) {
            Log.w("SamplePodcastDataSource", "Asset not found: $cleanPath", e)
            false
        }
    }
}
