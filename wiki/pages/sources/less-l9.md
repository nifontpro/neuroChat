---
type: source
title: "Л9 — Управление контекстом: сжатие истории"
slug: less-l9
source_path: raw/less/l9.txt
ingested_at: 2026-05-04
tags: [lesson, context, compression, compact, summary]
references: [concepts/context-compression]
---

## Краткое содержание

Задание девятого занятия: реализовать механизм сжатия истории — хранить последние N сообщений «как есть», остальное заменять summary; сравнить качество и расход токенов до/после.

## Требования задания

- Хранить последние N сообщений как есть
- Остальное заменять summary (каждые 10 сообщений)
- Хранить summary отдельно, подставлять в запрос вместо полной истории
- Сравнить: без сжатия / со сжатием / расход токенов

## Статус в NeuroChat

✅ **Реализовано** (команда `/compact`)

| Требование | Реализация |
|---|---|
| Команда сжатия | `/compact` → вызывает LLM суммаризировать текущую историю |
| Хранение summary | `ChatState.conversationSummary: String?` |
| Подстановка в запрос | `BuildChatContextUseCase` — вставляет summary как system-сообщение после основного промпта |
| Очистка истории после compact | `messages = emptyList()`, `historyDataSource.clearAll()` |
| Сброс summary | При `OnClearHistory` (summary не сбрасывается) и при `OnResetSettings` |

**Поведение:**
1. `/compact` — показывает «Сжимаю историю…», стримит запрос к LLM с промптом суммаризации
2. При успехе — очищает `messages` и `DB`, сохраняет текст сводки в `conversationSummary`
3. Следующие запросы видят: `[system: main prompt] [system: Summary of previous conversation: ...] [user messages]`

**Ограничение:** сжатие только по команде (не автоматически каждые N сообщений).

## Связи

- [[concepts/context-compression]] — концепция сжатия контекста
- [[concepts/chat-history]] — история, которую сжимаем
- [[concepts/agent-architecture]] — агент, управляющий контекстом
