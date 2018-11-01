package com.team1091.websiteGen

import java.time.LocalDate

// Blog posts
data class Post(
        val date: LocalDate,
        val title: String,
        val url: String,
        val content: String,
        val outputDir: String
)

// General Pages
data class Page(
        val title: String,
        val url: String,
        val content: String,
        val outputDir: String
)