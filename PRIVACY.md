# Privacy Policy

**Last updated:** June 11, 2026

CommitGPT is an IntelliJ IDEA plugin that helps you generate Git commit messages using AI services you configure.

## Data Collection by the Plugin Author

CommitGPT **does not** collect, store, or transmit any personal data to the plugin author or any third party controlled by the plugin author.

All configuration (API keys, templates, preferences) is stored **locally** on your machine using IntelliJ's `PropertiesComponent`.

## Data Sent to AI Providers

When you click **Analyze Changes and Generate**, the plugin:

1. Reads the **staged Git diff** from your local repository
2. Sends the diff (and your configured prompt) to the **AI provider you selected** (e.g. OpenAI, Qwen, DeepSeek, Ollama)
3. Receives a generated commit message from that provider

The following may be included in requests to your chosen AI provider:

- Staged file diffs (code changes)
- Git branch name
- Git author name
- Your custom prompt text (if configured)

Your **API key** is sent to the configured provider's API endpoint for authentication. It is stored locally and never sent to the plugin author.

## Your Responsibilities

- Review your organization's policies before sending code diffs to external AI services
- Use Ollama or a self-hosted endpoint if you need fully local processing
- Keep your API keys secure and rotate them if compromised
- Review generated commit messages before committing

## Third-Party Services

This plugin integrates with third-party AI APIs. Their data handling is governed by their respective privacy policies:

- [OpenAI Privacy Policy](https://openai.com/policies/privacy-policy)
- [Alibaba Cloud / DashScope](https://www.alibabacloud.com/help/en/legal/latest/alibaba-cloud-international-website-terms-of-use)
- [DeepSeek Privacy Policy](https://www.deepseek.com/)
- [Ollama](https://ollama.com/) (local, no external transmission when using localhost)

## Contact

For privacy-related questions, open an issue at [github.com/MMCISAGOODMAN/CommitGPT/issues](https://github.com/MMCISAGOODMAN/CommitGPT/issues).
