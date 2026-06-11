package com.commitgpt.template

import com.commitgpt.ai.model.CommitJsonResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object TemplateEngine {
    val AI_VARIABLES = setOf("type", "scope", "subject", "body", "footer")
    val LOCAL_VARIABLES = setOf("emoji", "author", "branch", "date")
    val VARIABLES = AI_VARIABLES + LOCAL_VARIABLES

    private val FIXED_PATTERN = Regex("""\{fixed:([^}]*)}""", RegexOption.IGNORE_CASE)
    private val VARIABLE_PATTERN = Regex("""\{([^}]+)}""")
    private val CONVENTIONAL_PREFIX = Regex(
        """^(feat|fix|docs|style|refactor|test|chore)(\([^)]*\))?:\s*""",
        RegexOption.IGNORE_CASE,
    )

    private val TYPE_EMOJI_MAP = mapOf(
        "feat" to "✨",
        "fix" to "🐛",
        "docs" to "📝",
        "style" to "💄",
        "refactor" to "♻️",
        "test" to "✅",
        "chore" to "🔧",
    )

    fun fillTemplate(
        template: String,
        aiResponse: CommitJsonResponse,
        author: String = "",
        branch: String = "",
        date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    ): String {
        val normalized = normalizeAiResponse(template, aiResponse)
        val emoji = TYPE_EMOJI_MAP[normalized.type.lowercase()] ?: "📌"
        val values = mapOf(
            "type" to normalized.type,
            "scope" to normalized.scope,
            "subject" to normalized.subject,
            "body" to normalized.body,
            "footer" to normalized.footer,
            "emoji" to emoji,
            "author" to author,
            "branch" to branch,
            "date" to date,
        )
        return replaceVariables(template, values)
    }

    fun fillWithTestJson(
        template: String,
        testJson: CommitJsonResponse,
        author: String = "Test User",
        branch: String = "main",
        date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    ): String = fillTemplate(template, testJson, author, branch, date)

    fun extractAiVariables(template: String): Set<String> {
        val withoutFixed = stripFixedPlaceholders(template)
        return VARIABLE_PATTERN.findAll(withoutFixed)
            .map { it.groupValues[1].trim() }
            .filter { it in AI_VARIABLES }
            .toSet()
    }

    fun hasFixedPrefix(template: String): Boolean = FIXED_PATTERN.containsMatchIn(template)

    fun isSubjectOnlyTemplate(template: String): Boolean {
        return extractAiVariables(template) == setOf("subject")
    }

    /**
     * Cleans AI output when template already contains fixed ticket/prefix text.
     */
    fun normalizeAiResponse(template: String, response: CommitJsonResponse): CommitJsonResponse {
        var subject = response.subject.trim()
        val body = response.body.trim()
        val footer = response.footer.trim()

        if (hasFixedPrefix(template) && "subject" in extractAiVariables(template)) {
            subject = stripConventionalPrefix(subject)
            subject = stripEmbeddedTicketKeys(subject, template)
        }

        // AI sometimes returns full conventional line as subject; drop unused type/scope from output
        if (isSubjectOnlyTemplate(template) && !hasFixedPrefix(template)) {
            subject = stripConventionalPrefix(subject)
        }

        return response.copy(
            subject = subject,
            body = body,
            footer = footer,
            type = if (isSubjectOnlyTemplate(template)) "" else response.type,
            scope = if (isSubjectOnlyTemplate(template)) "" else response.scope,
        )
    }

    private fun stripConventionalPrefix(text: String): String {
        return CONVENTIONAL_PREFIX.replace(text, "").trim()
    }

    private fun stripEmbeddedTicketKeys(subject: String, template: String): String {
        val fixedValues = FIXED_PATTERN.findAll(template).map { it.groupValues[1].trim() }
        var result = subject
        for (fixed in fixedValues) {
            if (fixed.isBlank()) continue
            result = result.replace(Regex("""(?i)^${Regex.escape(fixed)}[:\s-]*"""), "")
            result = result.replace(Regex("""(?i)\b${Regex.escape(fixed)}\b[:\s-]*"""), "")
        }
        return result.trim()
    }

    private fun stripFixedPlaceholders(template: String): String {
        return FIXED_PATTERN.replace(template, "")
    }

    private fun replaceVariables(template: String, values: Map<String, String>): String {
        var result = template
        result = FIXED_PATTERN.replace(result) { match -> match.groupValues[1] }
        for ((key, value) in values) {
            result = result.replace("{$key}", value)
        }
        result = VARIABLE_PATTERN.replace(result, "")
        return cleanupMessage(result)
    }

    private fun cleanupMessage(message: String): String {
        return message
            .lines()
            .map { it.trimEnd() }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    fun extractUsedVariables(template: String): Set<String> {
        val vars = mutableSetOf<String>()
        if (hasFixedPrefix(template)) vars.add("fixed")
        extractAiVariables(template).forEach { vars.add(it) }
        VARIABLE_PATTERN.findAll(stripFixedPlaceholders(template))
            .map { it.groupValues[1].trim() }
            .filter { it in LOCAL_VARIABLES }
            .forEach { vars.add(it) }
        return vars
    }
}
