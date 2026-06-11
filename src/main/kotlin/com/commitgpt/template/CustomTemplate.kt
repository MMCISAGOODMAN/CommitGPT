package com.commitgpt.template

data class CustomTemplate(
    val id: String,
    val name: String,
    val content: String,
) {
    fun toCommitTemplate(): CommitTemplate = CommitTemplate(
        id = id,
        name = name,
        content = content,
        isPreset = false,
    )
}
