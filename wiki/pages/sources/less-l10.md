---
type: source
title: "Л10 — Управление контекстом: 3 стратегии"
slug: less-l10
source_path: raw/less/l10.txt
ingested_at: 2026-05-06
tags: [lesson, context, strategies, sliding-window, sticky-facts, branching]
references: [concepts/context-compression, concepts/chat-history, concepts/agent-architecture]
---

## Краткое содержание

Задание: реализовать в агенте 3 стратегии управления контекстом и переключатель между ними.

## Требования задания

1. **Sliding Window** — хранить последние N сообщений, остальное отбрасывать.
2. **Sticky Facts / Key-Value Memory** — отдельный блок «facts» (ключ-значение), обновляемый после каждого user-сообщения; в запрос идут facts + последние N сообщений.
3. **Branching** — checkpoint, 2 ветки от одной точки, независимое продолжение и переключение.
4. Переключатель стратегий, прогон одного сценария по всем трём, сравнение качества/стабильности/токенов/UX.

## Статус в NeuroChat

✅ **Реализовано** (стратегия выбирается в SettingsPanel или slash-командой `/strategy`).

### Архитектурные точки

| Слой | Артефакт |
|---|---|
| domain | `ContextStrategy` enum (`SLIDING_WINDOW`, `STICKY_FACTS`, `BRANCHING`) |
| domain | `Fact(key, value)` модель |
| domain | `Branch(id, name, parentBranchId, createdAt)` модель |
| domain | `BuildChatContextUseCase` — единый сборщик контекста, ветка по `strategy` |
| domain | `UpdateFactsUseCase` — отдельный LLM-вызов для извлечения/обновления фактов (JSON parse) |
| domain | `IChatHistoryDataSource` — методы для работы с ветками (`getMessages(branchId)`, `createBranchFrom(...)`, `getBranches()`, `deleteBranch(...)`) |
| data | Room v2: `ChatMessageEntity.branchId`, новая таблица `branches` (BranchEntity, BranchDao) |
| data | `RoomChatHistoryDataSource` копирует все сообщения родителя в новую ветку при `createBranchFrom` |
| data | `UserSettingsStorage` — `keyContextStrategy`, `keyFactsJson` (kotlinx.serialization), `keyCurrentBranchId` |
| presentation | `ChatState`: `contextStrategy`, `facts`, `isUpdatingFacts`, `branches`, `currentBranchId` |
| presentation | `ChatViewModel.refreshFactsInBackground()` — после ответа модели в STICKY_FACTS режиме |
| presentation | `StrategySelector` (RadioGroup), `FactsSection`, `BranchesSection` — компоненты в SettingsPanel |
| presentation | Команды `/strategy <window\|facts\|branching>`, `/branch <name>`, `/branches`, `/switch <id\|name>` |

### Стратегии: что отправляем в LLM

- **SLIDING_WINDOW**: `[system: prompt] [system: Summary…] [last N messages]`. `maxContextMessages=0` — без обрезки. `Summary` приходит из `/compact` (Л9), персистится в DataStore.
- **STICKY_FACTS**: `[system: prompt] [system: Known facts…\n- key: value\n…] [last N messages]`. После каждого ответа модели в фоне дёргается `UpdateFactsUseCase` — отдельный round-trip с инструкцией «верни обновлённый JSON массив `[{key, value}]`», ответ парсится `Json.decodeFromString`. При ошибке парсинга — старые facts остаются.
- **BRANCHING**: `[system: prompt] [все сообщения текущей ветки]`. Контекстное окно игнорируется; при `/branch <name>` создаётся копия всех сообщений текущей ветки в новой строке `branches`, обе ветки далее независимы. Переключение через UI или `/switch`.

### Файлы

**Создано:**
- `core/domain/.../model/{ContextStrategy,Fact,Branch}.kt`
- `core/domain/.../usecase/UpdateFactsUseCase.kt`
- `core/data/.../db/{BranchEntity,BranchDao}.kt`
- `feature/chat/presentation/.../components/settings/{StrategySelector,FactsSection,BranchesSection}.kt`

**Изменено:**
- `core/domain/.../usecase/BuildChatContextUseCase.kt` — параметры `strategy`, `facts`
- `core/domain/.../usecase/HandleCommandUseCase.kt` — команды `/strategy`, `/branch`, `/branches`, `/switch`
- `core/domain/.../datasource/IChatHistoryDataSource.kt` — branchId-based API
- `core/data/.../db/{ChatMessageEntity,ChatMessageDao,NeuroChatDatabase,ChatMessageMapper,RoomChatHistoryDataSource}.kt` — Room v2 + ветки
- `core/data/.../preferences/UserSettingsStorage.kt` — strategy/facts/currentBranchId
- `core/data/.../di/DataModule.kt` — BranchDao
- `feature/chat/presentation/.../{ChatState,ChatAction,ChatViewModel}.kt`
- `feature/chat/presentation/.../components/SettingsPanel.kt`
- `feature/chat/presentation/.../di/ChatModule.kt`
- `feature/chat/presentation/.../composeResources/values{,-ru}/string.xml`

### Грабли

- Room v2 миграция destructive (`fallbackToDestructiveMigration(dropAllTables = true)`) — стирает старую БД при первом запуске; для проекта приемлемо.
- `kotlin.time.Clock` (stable с 2.1.20) требует `@OptIn(ExperimentalTime::class)` на 2.3 — добавлен в `RoomChatHistoryDataSource`.
- `UpdateFactsUseCase` обнуляет `thinkingBudget` и `maxTokens` в копии `ApiSettings` — служебный вызов не должен «думать» и не должен ограничиваться лимитом ответа модели.

## Связи

- [[concepts/context-compression]] — Л9, summary живёт в стратегии SLIDING_WINDOW
- [[concepts/agent-architecture]] — стратегия = политика, инкапсулированная в `ChatViewModel + BuildChatContextUseCase`
- [[concepts/chat-history]] — теперь хранится с привязкой к `branchId`
