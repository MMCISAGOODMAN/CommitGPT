package com.commitgpt.ai.model

data class CommitJsonResponse(
    val type: String = "",
    val scope: String = "",
    val subject: String = "",
    val body: String = "",
    val footer: String = "",
)
