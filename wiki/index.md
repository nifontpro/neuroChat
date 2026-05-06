# Index

_Обновлён: 2026-05-06 · страниц: 18 · источников: 8_

Каталог страниц вики. Поддерживается агентом — см. [SCHEMA.md](SCHEMA.md).

## Overview
- _(пусто — будет добавлено после накопления материала)_

## Sources
- [[sources/less-l10]] — Л10: 3 стратегии контекста — Sliding Window / Sticky Facts / Branching + переключатель (2026-05-06)
- [[sources/less-l9]] — Л9: сжатие контекста — команда /compact, summary в BuildChatContextUseCase (2026-05-04)
- [[sources/less-l8]] — Л8: работа с токенами — подсчёт prompt/completion/session, StatisticsSection (2026-05-03)
- [[sources/less-l7]] — Л7: сохранение контекста — Room KMP SQLite, загрузка при старте (2026-05-03)
- [[sources/less-l6]] — Л6: первый агент — отдельная сущность с инкапсулированной логикой (2026-05-03)
- [[sources/less-l4]] — Л4: эксперимент с температурой (0 / 0.7 / 1.2) (2026-05-03)
- [[sources/less-l2]] — Л2: контроль формата ответа (max_tokens, stop, response_format) (2026-05-03)
- [[sources/less-l1]] — Л1: минимальный LLM API клиент (2026-05-03)

## Entities

### Person
- _(пусто)_

### Product
- _(пусто)_

### Org
- _(пусто)_

### Place
- _(пусто)_

### Event
- _(пусто)_

### Other
- _(пусто)_

## Concepts
- [[concepts/llm-api-client]] — OpenAI-совместимый Ktor-клиент; реализован полностью
- [[concepts/sse-streaming]] — SSE-стриминг токенов через Flow; реализован полностью
- [[concepts/response-format-control]] — max_tokens / stop / response_format; max_tokens реализован
- [[concepts/temperature]] — параметр случайности генерации; слайдер 0.0–2.0, реализован полностью
- [[concepts/agent-architecture]] — паттерн агента: ChatViewModel + ChatRepository + OpenAiClient
- [[concepts/chat-history]] — персистентность истории через Room KMP SQLite
- [[concepts/token-statistics]] — подсчёт токенов (prompt/completion/session), StatisticsSection UI
- [[concepts/context-compression]] — /compact: LLM суммаризирует историю, summary подставляется в контекст

## Analyses
- [[analyses/compose-desktop-oom-fix]] — exit 137 (SIGKILL) при `:composeApp:desktopRun`: jvmArgs + уменьшение Gradle daemon heap, обязательно `./gradlew --stop` (2026-05-06)
