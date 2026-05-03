---
type: concept
name: "SSE-стриминг"
slug: sse-streaming
tags: [networking, streaming, sse]
sources: [sources/less-l1]
related_concepts: [concepts/llm-api-client]
updated_at: 2026-05-03
---

## Определение

Server-Sent Events (SSE) — механизм получения ответа LLM по частям (токен за токеном) вместо ожидания полного ответа. Сервер отдаёт поток строк `data: {...}`, завершающийся `data: [DONE]`.

## Реализация в NeuroChat

`OpenAiClient.consumeSseStream()` читает `ByteReadChannel` построчно, парсит JSON-чанки в `StreamToken`, эмитирует в `channelFlow`. `ChatViewModel` собирает поток и обновляет UI инкрементально.

Fallback: если `Content-Type` ответа не `event-stream` — парсится как обычный JSON (`ChatCompletionResponse`).

## Связи

- [[concepts/llm-api-client]] — клиент, реализующий стриминг
