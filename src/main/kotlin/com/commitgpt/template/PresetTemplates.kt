package com.commitgpt.template

data class CommitTemplate(
    val id: String,
    val name: String,
    val content: String,
    val isPreset: Boolean = false,
)

object PresetTemplates {
    val all: List<CommitTemplate> = listOf(
        CommitTemplate(
            id = "conventional",
            name = "标准 Conventional",
            content = "{type}({scope}): {subject}",
            isPreset = true,
        ),
        CommitTemplate(
            id = "conventional-body",
            name = "带详细说明",
            content = "{type}({scope}): {subject}\n\n{body}",
            isPreset = true,
        ),
        CommitTemplate(
            id = "conventional-footer",
            name = "带关联 Issue",
            content = "{type}({scope}): {subject}\n\n{body}\n\n{footer}",
            isPreset = true,
        ),
        CommitTemplate(
            id = "emoji",
            name = "Emoji 风格",
            content = ":{emoji}: {type}({scope}): {subject}",
            isPreset = true,
        ),
        CommitTemplate(
            id = "simple",
            name = "简洁版",
            content = "{subject}",
            isPreset = true,
        ),
        CommitTemplate(
            id = "fixed-prefix",
            name = "固定前缀",
            content = "{fixed:bugfix}: {subject}",
            isPreset = true,
        ),
        CommitTemplate(
            id = "fixed-type-subject",
            name = "固定类型 + 描述",
            content = "{fixed:feat}: {subject}",
            isPreset = true,
        ),
        CommitTemplate(
            id = "ticket-subject",
            name = "Ticket + 描述",
            content = "{fixed:TICKET-123}: {subject}",
            isPreset = true,
        ),
    )

    fun findById(id: String): CommitTemplate? = all.find { it.id == id }

    val default: CommitTemplate = all.first()
}
