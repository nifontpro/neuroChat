---
type: concept
name: "LLM API клиент"
slug: llm-api-client
tags: [networking, ktor, openai-compatible]
sources: [sources/less-l1, sources/less-l2]
related_concepts: [concepts/sse-streaming, concepts/response-format-control]
updated_at: 2026-05-03
---

## Определение

HTTP-клиент для отправки запросов к OpenAI-совместимому API (`POST /chat/completions`) и получения ответов модели.

## Реализация в NeuroChat

- **`OpenAiClient`** (`core/data/.../network/OpenAiClient.kt`) — внутренний Ktor-клиент. Поддерживает SSE-стриминг и fallback на обычный JSON-ответ. Конфигурируется через `ApiSettings`.
- **`ChatRepository`** (`core/data/.../network/ChatRepository.kt`) — публичный фасад, маппит ошибки в доменный `DataError`.
- **`ApiSettings`** (`core/domain/.../model/ApiSettings.kt`) — параметры запроса: `baseUrl`, `apiKey`, `model`, `systemPrompt`, `temperature`, `timeoutSeconds`, `thinkingBudget`.

## Текущие возможности

✅ Стриминг SSE → `Flow<StreamToken>`  
✅ Системный промпт  
✅ Temperature  
✅ Thinking budget (Anthropic extended thinking)  
✅ Статистика токенов (из финального SSE-чанка)  
✅ `max_tokens` (слайдер в SettingsPanel, сохраняется в DataStore)  
— `stop` sequences (покрывается через systemPrompt)  
— `response_format` (JSON mode, не реализован)  

## Связи

- [[concepts/sse-streaming]] — механизм получения ответа
- [[concepts/response-format-control]] — управление форматом (частично реализовано)
