package com.example.craftable

data class Comment(
    val userId: String? = null,
    val text: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val likedBy: Map<String, Boolean>? = null,
    val replies: Map<String, Comment>? = null
)

data class Post(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageBase64: String = "",
    val timestamp: Long = 0,
    val userId: String = "",
    val comments: Map<String, Comment>? = null,
    val likes: Int = 0,
    val likedBy: Map<String, Boolean>? = null
)
