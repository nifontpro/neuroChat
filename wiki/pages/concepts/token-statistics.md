---
type: concept
name: "Статистика токенов"
slug: token-statistics
tags: [tokens, statistics, usage, cost]
sources: [sources/less-l8]
related_concepts: [concepts/agent-architecture, concepts/llm-api-client]
updated_at: 2026-05-03
---

## Определение

Подсчёт и отображение токенов: сколько потрачено на запрос, сколько пришло в ответе, как растёт накопленный итог по ходу сессии.

## Источники данных

**От провайдера (точные):** `TokenUsage` из финального SSE-чанка (`stream_options.include_usage = true`):
- `promptTokens` — токены текущего запроса (история + системный промпт)
- `completionTokens` — токены ответа модели
- `totalTokens` — сумма

**Клиентские (приблизительные):** `ResponseStatistics`:
- `tokenCount` — количество эмитированных токенов в `consumeToken()`
- `durationMs` — время от отправки до завершения стрима
- `tokensPerSecond` — скорость генерации
- `charCount` — символов в ответе

**Накопленные:**
- `ChatState.sessionTotalTokens` — сумма `totalTokens` за все запросы текущей сессии (сбрасывается при очистке истории)

## UI

`StatisticsSection` в `SettingsPanel` — включается свитчем «Показывать статистику», отображает:
- Время генерации, токенов получено, скорость, символов
- Токенов в запросе / в ответе / итого за запрос (от API)
- Итого токенов за сессию

## Переполнение контекста

При отправке слишком длинного диалога провайдер возвращает HTTP 400 → `ApiException` → `DataError.BAD_REQUEST` → сообщение об ошибке в `ChatScreen`. Превентивно ограничить рост контекста можно через `maxContextMessages` (слайдер «Контекст»).

## Связи

- [[concepts/agent-architecture]] — агент, собирающий статистику
- [[concepts/llm-api-client]] — `stream_options.include_usage` в запросе
