# Changelog

All notable changes to CommitGPT will be documented in this file.

## [1.0.1] - 2026-06-17

### Fixed

- Remove `until-build` cap so the plugin installs on IntelliJ IDEA 2025.x and 2026.x (previously limited to 2024.3 / build 243)

## [1.0.0] - 2026-06-11

### Changed

- Marketplace display name set to **CommitGPT Assistant** to avoid conflict with the deprecated [CommitGPT](https://plugins.jetbrains.com/plugin/21412-commitgpt) plugin

### Added

- AI-powered commit message generation from staged Git diffs
- Three work modes: preset template, custom template, and free-form
- Built-in Conventional Commits templates plus fixed-prefix templates (`{fixed:...}`)
- Custom template editor with variable hints and JSON preview
- AI providers: OpenAI, Qwen (DashScope), DeepSeek, Ollama, and custom endpoints
- Commit dialog AI assistant panel with template preview
- Keyboard shortcut `Ctrl+Alt+G` / `⌃⌥G`
- Settings: template management and AI configuration
- Test connection for AI provider configuration

[1.0.1]: https://github.com/MMCISAGOODMAN/CommitGPT/releases/tag/v1.0.1
[1.0.0]: https://github.com/MMCISAGOODMAN/CommitGPT/releases/tag/v1.0.0
