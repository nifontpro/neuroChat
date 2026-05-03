# Log

Хронологический журнал операций над вики. Append-only.
Формат записей — см. [SCHEMA.md](SCHEMA.md#logmd--формат).

Быстрый просмотр последних записей:
```
grep "^## \[" wiki/log.md | tail -10
```

---

## [2026-05-03] ingest | Л1 — Минимальный LLM API клиент
- source: raw/less/l1.txt
- created: pages/sources/less-l1.md
- created concepts: pages/concepts/llm-api-client.md, pages/concepts/sse-streaming.md
- notes: задание полностью реализовано в NeuroChat (OpenAiClient + ChatScreen)

## [2026-05-04] impl | Л9 — Реализовано сжатие контекста (/compact)
- added: CommandResult.CompactRequested, /compact в HandleCommandUseCase
- added: ChatState.conversationSummary, ChatViewModel.compactHistory()
- updated: BuildChatContextUseCase — параметр conversationSummary
- added: строки cmd_compact_start/done/failed, /compact в /? справке
- created: pages/sources/less-l9.md, pages/concepts/context-compression.md
- notes: BUILD SUCCESSFUL; summary хранится in-memory, не персистируется

## [2026-05-03] ingest | Л8 — Работа с токенами
- source: raw/less/l8.txt
- created: pages/sources/less-l8.md, pages/concepts/token-statistics.md
- notes: полностью реализовано; TokenUsage из SSE, StatisticsSection в UI

## [2026-05-03] ingest | Л7 — Сохранение контекста
- source: raw/less/l7.txt
- created: pages/sources/less-l7.md, pages/concepts/chat-history.md
- notes: полностью реализовано через Room KMP; loadHistory в onStart VM

## [2026-05-03] ingest | Л6 — Первый агент
- source: raw/less/l6.txt
- created: pages/sources/less-l6.md, pages/concepts/agent-architecture.md
- notes: задание полностью реализовано; ChatViewModel = агент, ChatRepository = инкапсулированный транспорт

## [2026-05-03] ingest | Л4 — Эксперимент с температурой
- source: raw/less/l4.txt
- created: pages/sources/less-l4.md, pages/concepts/temperature.md
- notes: задание учебное — новой фичи не требует, temperature полностью реализован

## [2026-05-03] impl | Л2 — Реализован max_tokens
- added: ApiSettings.maxTokens, ChatRequest.max_tokens, UserSettingsStorage.saveMaxTokens
- added: ChatState.maxTokens, ChatAction.OnMaxTokensChange, ChatViewModel.updateMaxTokens
- added: MaxTokensSlider.kt, SettingsPanel обновлён, строки label_max_tokens/label_max_tokens_unlimited
- updated: wiki/pages/sources/less-l2.md, concepts/llm-api-client.md, concepts/response-format-control.md
- notes: компиляция чистая (BUILD SUCCESSFUL)

## [2026-05-03] ingest | Л2 — Контроль формата ответа
- source: raw/less/l2.txt
- created: pages/sources/less-l2.md
- created concepts: pages/concepts/response-format-control.md
- updated: pages/concepts/llm-api-client.md (добавлены пробелы: max_tokens, stop, response_format)
- notes: частично реализовано — systemPrompt есть, max_tokens и stop отсутствуют в ChatRequest/ApiSettings

## [2026-05-03 00:00] init
- created: wiki/SCHEMA.md, wiki/index.md, wiki/log.md
- created dirs: wiki/raw/, wiki/raw/assets/, wiki/pages/{sources,entities,concepts,analyses}/
- notes: пустая структура готова, источников ещё нет
