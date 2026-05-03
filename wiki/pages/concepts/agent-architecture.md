---
type: concept
name: "Архитектура агента"
slug: agent-architecture
tags: [agent, architecture, viewmodel, clean-architecture]
sources: [sources/less-l6]
related_concepts: [concepts/llm-api-client, concepts/sse-streaming]
updated_at: 2026-05-03
---

## Определение

Агент — сущность, инкапсулирующая логику взаимодействия с LLM: принимает запрос пользователя, формирует контекст, вызывает API, получает ответ и возвращает его в интерфейс.

## Реализация в NeuroChat

```
Пользователь → ChatScreen → ChatAction
                               ↓
                         ChatViewModel  ← агент
                         (состояние + оркестрация)
                               ↓
                         ChatRepository
                         (инкапсуляция HTTP)
                               ↓
                         OpenAiClient
                         (SSE-стриминг)
                               ↓
                         LLM API
```

### ChatViewModel — ядро агента

- Хранит `ChatState` (история, настройки, статус загрузки)
- Принимает `ChatAction` от UI
- Формирует контекст через `BuildChatContextUseCase`
- Подписывается на `Flow<Result<StreamToken>>` из репозитория
- Обновляет UI-состояние токен за токеном

### ChatRepository — транспортный слой

- Единственная публичная точка доступа к сети из feature-модуля
- Маппит `ApiException` → `DataError` (доменный тип)
- Не знает про UI и ViewModel

### Отличие от «просто одного API-вызова»

Агент поддерживает: историю сообщений, оконный контекст (`maxContextMessages`), системный промпт, стриминг, команды (`/system`, `/t`, `/think`), сохранение настроек, статистику токенов.

## Связи

- [[concepts/llm-api-client]] — транспорт агента
- [[concepts/sse-streaming]] — механизм получения токенов
