---
type: concept
name: "Сжатие контекста (context compression)"
slug: context-compression
tags: [context, compression, summary, compact]
sources: [sources/less-l9]
related_concepts: [concepts/chat-history, concepts/agent-architecture, concepts/token-statistics]
updated_at: 2026-05-04
---

## Определение

Механизм управления длиной контекста: вместо отправки полной истории диалога LLM суммаризирует её, и дальнейшие запросы используют компактную сводку вместо тысяч токенов истории.

## Реализация в NeuroChat

### Команда `/compact`

```
Пользователь вводит /compact
        ↓
ChatViewModel.compactHistory()
        ↓
LLM вызов: [system: COMPACT_SYSTEM_PROMPT] + [все сообщения]
        ↓
summary: String  ← накапливается из Flow<StreamToken>
        ↓
messages = [], historyDataSource.clearAll()
conversationSummary = summary
        ↓
Следующий запрос:
  [system: основной промпт]
  [system: "Summary of previous conversation:\n<summary>"]
  [user/assistant: свежие сообщения]
```

### Prompt суммаризации

```
Summarize the following conversation concisely. Preserve all key facts,
decisions, and context. Output only the summary, no preamble.
```

### `BuildChatContextUseCase`

Параметр `conversationSummary: String?` — если задан, вставляется как system-сообщение сразу после основного системного промпта.

## Ограничения

- Сжатие только по явной команде, не автоматическое
- При `/clear` (OnClearHistory) summary тоже очищается — нет истории, нет и сводки

## Связи

- [[concepts/chat-history]] — персистентная история до компактизации
- [[concepts/token-statistics]] — сравнение расхода токенов до/после
- [[concepts/agent-architecture]] — агент, управляющий контекстом
