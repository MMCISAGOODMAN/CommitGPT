package com.commitgpt.template

enum class WorkMode(val displayName: String) {
    PRESET_TEMPLATE("预设模板"),
    CUSTOM_TEMPLATE("自定义模板"),
    NO_TEMPLATE("无模板");

    companion object {
        fun fromName(name: String?): WorkMode =
            entries.find { it.name == name } ?: PRESET_TEMPLATE
    }
}
