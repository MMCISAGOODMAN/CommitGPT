package com.commitgpt.ai

import com.commitgpt.settings.CommitGptSettings
import com.commitgpt.template.TemplateEngine
import com.commitgpt.template.WorkMode

object PromptBuilder {
    private const val DEFAULT_JSON_PROMPT = """
You are a Git commit message assistant. Analyze the provided git diff and return a JSON object with the following fields:
- type: one of feat, fix, docs, style, refactor, test, chore
- scope: the affected module or area (use empty string if unclear)
- subject: a concise summary in imperative mood (max 72 chars)
- body: detailed description of changes (optional, use empty string if not needed)
- footer: issue references like "Closes #123" (optional, use empty string if not needed)

Return ONLY valid JSON, no markdown fences, no extra text.
Example:
{"type":"feat","scope":"auth","subject":"add login validation","body":"Implemented email and password validation","footer":""}
"""

    private const val DEFAULT_FREE_PROMPT = """
You are a Git commit message assistant. Analyze the provided git diff and generate a complete, well-formatted commit message.
Follow Conventional Commits style when appropriate. Return ONLY the commit message text, no markdown fences, no extra explanation.
"""

    private val FIELD_DESCRIPTIONS = mapOf(
        "type" to "- type: one of feat, fix, docs, style, refactor, test, chore",
        "scope" to "- scope: affected module or area (empty string if unclear)",
        "subject" to "- subject: plain description of the change (imperative mood, max 72 chars)",
        "body" to "- body: detailed description (empty string if not needed)",
        "footer" to "- footer: issue references like \"Closes #123\" (empty string if not needed)",
    )

    fun buildSystemPrompt(
        settings: CommitGptSettings,
        workMode: WorkMode,
        templateContent: String? = null,
    ): String {
        val basePrompt = when (workMode) {
            WorkMode.NO_TEMPLATE -> DEFAULT_FREE_PROMPT.trim()
            else -> buildTemplateAwareJsonPrompt(templateContent)
        }

        val custom = settings.customPrompt.trim()
        if (custom.isEmpty()) {
            return basePrompt
        }

        return if (workMode == WorkMode.NO_TEMPLATE) {
            custom
        } else {
            """
$custom

IMPORTANT: You MUST still return ONLY valid JSON (no markdown). Required fields:
${describeFields(TemplateEngine.extractAiVariables(templateContent ?: "{subject}"))}

Template to fill after parsing:
$templateContent
            """.trimIndent()
        }
    }

    private fun buildTemplateAwareJsonPrompt(templateContent: String?): String {
        if (templateContent.isNullOrBlank()) {
            return DEFAULT_JSON_PROMPT.trim()
        }

        val aiVars = TemplateEngine.extractAiVariables(templateContent)
        val requiredFields = if (aiVars.isEmpty()) setOf("subject") else aiVars
        val fixedPrefixRules = buildFixedPrefixRules(templateContent, requiredFields)

        return """
You are a Git commit message assistant. Analyze the provided git diff.

The final commit message is built from this template (fixed parts are inserted automatically):
---
$templateContent
---

Return a JSON object with ONLY these fields:
${describeFields(requiredFields)}
$fixedPrefixRules

Rules:
- Return ONLY valid JSON, no markdown fences, no extra text
- Use empty string "" for fields not listed above
- Do NOT use Conventional Commits format inside JSON values unless the template explicitly contains {type} or {scope}
- subject must be a plain description ONLY (e.g. "add new methods to get first name")
- NEVER put "feat(...):", "fix(...):", ticket keys, or scope paths inside subject

Example:
${buildJsonExample(requiredFields, templateContent)}
        """.trimIndent()
    }

    private fun buildFixedPrefixRules(template: String, requiredFields: Set<String>): String {
        if (!TemplateEngine.hasFixedPrefix(template) || "subject" !in requiredFields) {
            return ""
        }
        val fixedTexts = Regex("""\{fixed:([^}]*)}""", RegexOption.IGNORE_CASE)
            .findAll(template)
            .map { it.groupValues[1] }
            .joinToString(", ")
        return """
- Fixed text already in template: $fixedTexts — do NOT repeat these in any JSON field
- subject must NOT include type, scope, package path, or ticket number
        """.trimIndent()
    }

    private fun describeFields(fields: Set<String>): String {
        val ordered = listOf("type", "scope", "subject", "body", "footer").filter { it in fields }
        return ordered.joinToString("\n") { FIELD_DESCRIPTIONS[it] ?: "- $it" }
    }

    private fun buildJsonExample(fields: Set<String>, template: String): String {
        val subjectExample = if (TemplateEngine.hasFixedPrefix(template)) {
            "add new methods to get first name"
        } else {
            "resolve login timeout issue"
        }
        val examples = mapOf(
            "type" to "fix",
            "scope" to "auth",
            "subject" to subjectExample,
            "body" to "Increased session timeout and added retry logic",
            "footer" to "Closes #123",
        )
        val ordered = listOf("type", "scope", "subject", "body", "footer").filter { it in fields }
        val pairs = ordered.joinToString(",") { "\"$it\":\"${examples[it] ?: ""}\"" }
        return "{$pairs}"
    }

    fun buildUserPrompt(diff: String): String {
        return """
Analyze the following staged git diff and generate the commit message content:

```diff
$diff
```
""".trim()
    }
}

object ResponseParser {
    private val gson = com.google.gson.Gson()

    fun parseJsonResponse(content: String): com.commitgpt.ai.model.CommitJsonResponse {
        val cleaned = extractJson(content)
        return try {
            gson.fromJson(cleaned, com.commitgpt.ai.model.CommitJsonResponse::class.java)
                ?: com.commitgpt.ai.model.CommitJsonResponse(subject = cleaned.trim())
        } catch (_: Exception) {
            com.commitgpt.ai.model.CommitJsonResponse(subject = content.trim())
        }
    }

    fun parseFreeTextResponse(content: String): String {
        return content.trim()
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
            .lines()
            .dropWhile { it.startsWith("```") }
            .joinToString("\n")
            .trim()
    }

    private fun extractJson(content: String): String {
        val trimmed = content.trim()
        if (trimmed.startsWith("{")) {
            return trimmed
        }
        val jsonBlock = Regex("```(?:json)?\\s*([\\s\\S]*?)```").find(trimmed)
        if (jsonBlock != null) {
            return jsonBlock.groupValues[1].trim()
        }
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1)
        }
        return trimmed
    }
}
