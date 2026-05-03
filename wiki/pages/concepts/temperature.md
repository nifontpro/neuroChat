---
type: concept
name: "Температура (temperature)"
slug: temperature
tags: [llm-parameter, creativity, determinism]
sources: [sources/less-l4]
related_concepts: [concepts/llm-api-client, concepts/response-format-control]
updated_at: 2026-05-03
---

## Определение

Параметр `temperature` управляет случайностью выборки токенов при генерации ответа. Чем выше значение — тем более разнообразные и творческие ответы, чем ниже — тем более предсказуемые и точные.

## Диапазон значений

| Значение | Характер | Применение |
|---|---|---|
| 0 | Полностью детерминированный | Факты, код, точные инструкции |
| 0.7 | Сбалансированный | Общий чат, объяснения |
| 1.2+ | Творческий, разнообразный | Идеи, сторителлинг, брейншторм |
| 2.0 | Максимально хаотичный | Редко полезен |

## Реализация в NeuroChat

- `ApiSettings.temperature: Double?` — null = дефолт провайдера
- `TemperatureSlider` (0.0–2.0, шаг 0.1) в `SettingsPanel`
- Сохраняется в DataStore; передаётся напрямую в `ChatRequest.temperature`
- Исключение: при включённом thinking (`ThinkingConfig`) температура принудительно выставляется в 1.0 (требование Anthropic)

## Связи

- [[concepts/llm-api-client]] — клиент, передающий temperature в запрос
- [[concepts/response-format-control]] — смежный параметр контроля ответа
