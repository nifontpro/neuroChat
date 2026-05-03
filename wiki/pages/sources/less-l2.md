---
type: source
title: "Л2 — Контроль формата ответа"
slug: less-l2
source_path: raw/less/l2.txt
ingested_at: 2026-05-03
tags: [lesson, response-format, max-tokens, stop-sequence]
references: [concepts/response-format-control, concepts/llm-api-client]
---

## Краткое содержание

Задание второго занятия: отправить один и тот же запрос с разным уровнем контроля ответа — без ограничений и с явным описанием формата, ограничением длины и stop-последовательностью.

## Требования задания

- Добавить явное описание формата ответа
- Добавить ограничение на длину ответа (`max_tokens`)
- Добавить условие завершения (`stop` sequence или явная инструкция)
- Сравнить ответы: без ограничений vs. с ограничениями

## Статус в NeuroChat

✅ **Реализовано**

| Требование | Статус | Где |
|---|---|---|
| Описание формата через system prompt | ✅ | `ApiSettings.systemPrompt`, `SettingsPanel` → `SystemPromptField` |
| `max_tokens` — ограничение длины | ✅ | `ApiSettings.maxTokens`, `ChatRequest.maxTokens`, `MaxTokensSlider` в SettingsPanel |
| `stop` sequences | — | Покрывается «явной инструкцией» через systemPrompt |
| `response_format` (JSON mode) | — | Выходит за рамки задания |

`max_tokens` передаётся в API-запрос; 0/null = без ограничений. Настройка сохраняется в DataStore.

## Связи

- [[concepts/response-format-control]] — концепция управления форматом ответа
- [[concepts/llm-api-client]] — базовый клиент, который нужно расширить
