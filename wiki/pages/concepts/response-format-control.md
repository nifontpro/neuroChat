---
type: concept
name: "Контроль формата ответа"
slug: response-format-control
tags: [prompt-engineering, max-tokens, stop-sequence, response-format]
sources: [sources/less-l2]
related_concepts: [concepts/llm-api-client]
updated_at: 2026-05-03
---

## Определение

Набор API-параметров, позволяющих управлять структурой и объёмом ответа LLM: явное описание формата, ограничение длины (`max_tokens`), условие завершения (`stop`), JSON-режим (`response_format`).

## Реализация в NeuroChat

| Параметр | Статус |
|---|---|
| Формат через `systemPrompt` | ✅ реализован |
| `max_tokens` | ✅ реализован (`ApiSettings.maxTokens` → `ChatRequest` → `MaxTokensSlider`) |
| `stop` sequences | — покрывается системным промптом |
| `response_format` (JSON mode) | — не реализован |

## Связи

- [[concepts/llm-api-client]] — клиент, который нужно расширить
- [[sources/less-l2]] — задание, выявившее пробел
