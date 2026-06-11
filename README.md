# CommitGPT

[![JetBrains Marketplace](https://img.shields.io/badge/JetBrains-Marketplace-blue)](https://plugins.jetbrains.com/plugin/PLACEHOLDER)
[![GitHub](https://img.shields.io/badge/GitHub-MMCISAGOODMAN%2FCommitGPT-blue?logo=github)](https://github.com/MMCISAGOODMAN/CommitGPT)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

IntelliJ IDEA plugin that generates Git commit messages from staged diffs using AI.

[English](#english) | [中文](#中文)

---

## English

### Features

- **Three work modes** — preset templates, custom templates, or free-form AI generation
- **Flexible templates** — Conventional Commits presets, custom templates with `{fixed:...}` literals and AI variables
- **Multiple AI providers** — OpenAI, Qwen (DashScope), DeepSeek, Ollama, or any compatible API
- **Commit dialog integration** — AI assistant panel in the Commit tool window
- **Keyboard shortcut** — `Ctrl+Alt+G` (macOS: `⌃⌥G`)

### Requirements

- IntelliJ IDEA 2023.3+ (build 233+)
- Git plugin (bundled)
- API key for your AI provider, or local Ollama

### Installation

Search **CommitGPT Assistant** in **Settings → Plugins**, or install from [JetBrains Marketplace](https://plugins.jetbrains.com).

Manual install: build with `gradle buildPlugin`, then **Settings → Plugins → Install Plugin from Disk**.

### Quick Start

1. **Settings → Tools → CommitGPT Assistant → AI Configuration** — configure provider and API key
2. **Settings → Tools → CommitGPT Assistant → Template Management** — create templates (optional)
3. Stage changes, open the **Commit** tool window
4. Select work mode and template in the **AI Commit Assistant** panel
5. Click **Analyze Changes and Generate**, review, and confirm

### Template Variables

| Variable | Source |
|----------|--------|
| `{type}` `{scope}` `{subject}` `{body}` `{footer}` | AI |
| `{fixed:text}` | Fixed literal in template |
| `{emoji}` `{author}` `{branch}` `{date}` | Plugin |

Example: `{fixed:ARTCORE-3232}: {subject}` → `ARTCORE-3232: add login validation`

### Build

```bash
gradle buildPlugin
```

### Privacy

Staged diffs are sent only to the AI provider you configure. See [PRIVACY.md](PRIVACY.md).

### License

[MIT](LICENSE)

---

## 中文

### 功能

- **三种工作模式**：预设模板 / 自定义模板 / 无模板
- **灵活模板**：内置 Conventional Commits 预设，支持 `{fixed:固定文本}` 与 AI 变量
- **多 AI 提供商**：OpenAI、通义千问、DeepSeek、Ollama、自定义
- **Commit 窗口集成**：AI 助手面板
- **快捷键**：`Ctrl+Alt+G`

### 使用

1. **Settings → Tools → CommitGPT Assistant** 配置 AI 与模板
2. Stage 变更，打开 **Commit** 窗口
3. 选择模式和模板，点击 **分析变更并生成**
4. 预览确认后填入 commit 消息

### 模板示例

`{fixed:ARTCORE-3232}: {subject}` → `ARTCORE-3232: add login validation`

### 隐私

暂存区 diff 仅发送到你配置的 AI 提供商。详见 [PRIVACY.md](PRIVACY.md).
