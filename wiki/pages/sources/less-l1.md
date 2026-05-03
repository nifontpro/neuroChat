---
type: source
title: "Л1 — Минимальный LLM API клиент"
slug: less-l1
source_path: raw/less/l1.txt
ingested_at: 2026-05-03
tags: [lesson, llm-api, networking]
references: [concepts/llm-api-client, concepts/sse-streaming]
---

## Краткое содержание

Задание первого занятия: написать минимальный код, отправляющий запрос в LLM через API, получающий ответ и выводящий его в консоль или простой интерфейс (CLI / Web).

## Требования задания

- Отправить запрос в LLM через API
- Получить ответ
- Вывести в консоль или простой интерфейс (CLI / Web)

## Статус в NeuroChat

✅ **Реализовано** (с превышением минимума)

- `core/data/.../network/OpenAiClient.kt` — Ktor-клиент, POST `/chat/completions`, SSE-стриминг → `Flow<StreamToken>`
- `core/data/.../network/ChatRepository.kt` — оборачивает клиент, маппит ошибки в `DataError`
- `feature/chat/presentation/.../ChatViewModel.kt` — подписывается на flow, накапливает токены
- `feature/chat/presentation/.../ChatScreen.kt` — Compose Multiplatform UI (Android + Desktop + iOS)

Интерфейс — Compose UI вместо CLI; требование «простой интерфейс» покрыто с запасом.

## Связи

- [[concepts/llm-api-client]] — реализация концепции API-клиента
- [[concepts/sse-streaming]] — используется SSE вместо единого ответа
