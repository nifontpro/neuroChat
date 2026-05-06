# Log

Хронологический журнал операций над вики. Append-only.
Формат записей — см. [SCHEMA.md](SCHEMA.md#logmd--формат).

Быстрый просмотр последних записей:
```
grep "^## \[" wiki/log.md | tail -10
```

---

## [2026-05-06] fix | Stale модель из DataStore + правильный маппинг non-stream error
- presentation: ChatViewModel.sanitizeCurrentModel() — после загрузки availableModels проверяет, что текущая модель в списке; иначе переключает на baseSettings.model или на первую из списка
- data: OpenAiClient.handleNonStreamingResponse — JSON `{"error":...}` при HTTP 200 теперь мапится в BAD_REQUEST (а не UNKNOWN), сохраняя providerMessage
- root cause: после переезда на rus-gpt у пользователя в DataStore лежала модель `claude-haiku-4-5` от прежнего провайдера; при первой попытке стрима провайдер ответил `model_not_found`
- notes: BUILD SUCCESSFUL desktop

## [2026-05-06] impl | Переезд на rus-gpt + динамический список моделей
- config: litellm.properties → baseUrl=https://rus-gpt.com/v1, новый apiKey, default model=z-ai/glm-5.1
- data: OpenAiClient.listModels() (GET /v1/models, парсит OpenAI-совместимый ModelsListResponse)
- data: OpenAiModels.kt — новые DTO ModelsListResponse / ModelEntry
- domain: IChatRepository.listModels(): Result<List<String>, DataError>
- presentation: ChatState.availableModels + isLoadingModels; loadAvailableModels() в onStart VM
- presentation: ModelSelector(currentModel, models, isLoading, onSelect) — fallback на AVAILABLE_MODELS если запрос упал
- notes: на rus-gpt 38 моделей; AVAILABLE_MODELS оставлен как safety-fallback при оффлайне; BUILD SUCCESSFUL desktop, тесты domain зелёные

## [2026-05-06] fix | Обработка Cloudflare 52x (HTTP 521 «Web server is down»)
- domain: новый DataError.Remote.UPSTREAM_DOWN
- data: NetworkErrorMapper — коды 520..527 мапятся в UPSTREAM_DOWN (отдельно от общего SERVER_ERROR 5xx)
- presentation: DataErrorMessage + строки error_upstream_down (en/ru) — «Сервер LLM временно недоступен (Cloudflare 52x)»
- notes: BUILD SUCCESSFUL desktop, тесты domain зелёные

## [2026-05-06] session-end | Остановились на Л10
- последнее задание: Л10 (3 стратегии управления контекстом) — реализовано полностью
- сделано в сессии:
  - impl Л10: ContextStrategy enum (SLIDING_WINDOW / STICKY_FACTS / BRANCHING), переключатель в SettingsPanel + slash-команды
  - Sliding Window: формализован поверх существующего maxContextMessages + summary из /compact
  - Sticky Facts: модель Fact, UpdateFactsUseCase (отдельный LLM-вызов с JSON-парсингом), refreshFactsInBackground после ответа модели, персист в DataStore (facts_json)
  - Branching: Room v2 с branchId + таблица branches, createBranchFrom копирует все сообщения родителя, UI секция с переключением/удалением
  - fix Compose Desktop exit 137 (OOM): jvmArgs + уменьшение Gradle daemon heap до 2560M; ключевой шаг — ./gradlew --stop
- следующие задания: Л3, Л5 — ещё не обрабатывались; для Л10 опционально остаётся pages/concepts/context-strategies.md
- notes: BUILD SUCCESSFUL на desktop/Android/iOS; десктоп подтверждённо запускается после --stop

## [2026-05-06] fix | Compose Desktop exit 137 (OOM SIGKILL)
- root cause: дефолт JVM без -Xmx + Gradle daemon -Xmx4096M съедают RAM, macOS убивает приложение
- fix: composeApp/build.gradle.kts — jvmArgs в compose.desktop.application + tasks.withType<JavaExec> для desktopRun/hotRunDesktop*
- fix: gradle.properties — org.gradle.jvmargs с 4096M → 2560M
- key step: ./gradlew --stop, иначе старый daemon живёт с прежним heap (без этого фикс не применился сразу)
- created: pages/analyses/compose-desktop-oom-fix.md
- notes: подтверждено пользователем — заработало после --stop

## [2026-05-06] impl | Л10 — Реализованы 3 стратегии управления контекстом
- domain: ContextStrategy enum, Fact, Branch модели
- domain: UpdateFactsUseCase (отдельный LLM-вызов, JSON parse)
- domain: BuildChatContextUseCase ветвится по strategy
- domain: HandleCommandUseCase + команды /strategy, /branch, /branches, /switch
- data: Room v2 — ChatMessageEntity.branchId, новая таблица branches; миграция destructive
- data: UserSettingsStorage — strategy, facts (JSON), currentBranchId
- presentation: StrategySelector, FactsSection, BranchesSection в SettingsPanel
- presentation: ChatViewModel — refreshFactsInBackground, createBranch/switch/delete
- created: pages/sources/less-l10.md
- notes: BUILD SUCCESSFUL desktop/Android/iOS, тесты domain зелёные

## [2026-05-03] ingest | Л1 — Минимальный LLM API клиент
- source: raw/less/l1.txt
- created: pages/sources/less-l1.md
- created concepts: pages/concepts/llm-api-client.md, pages/concepts/sse-streaming.md
- notes: задание полностью реализовано в NeuroChat (OpenAiClient + ChatScreen)

## [2026-05-04] session-end | Остановились на Л9
- последнее задание: Л9 (сжатие контекста) — реализовано + персистентность summary в DataStore
- также сделано в сессии: Л1✅ Л2✅(+impl max_tokens) Л4✅ Л6✅ Л7✅ Л8✅ Л9✅(+impl /compact)
- фикс: max_tokens + thinking budget_tokens конфликт (clamp в OpenAiClient)
- рефактор: KDoc /** */ вместо // перед классами/функциями во всём проекте (25 файлов)
- следующие задания: Л3, Л5 и всё что выше Л9 — ещё не обрабатывались
- notes: summary хранится в DataStore (conversation_summary key); при /clear summary тоже сбрасывается

## [2026-05-04] impl | Л9 — Реализовано сжатие контекста (/compact)
- added: CommandResult.CompactRequested, /compact в HandleCommandUseCase
- added: ChatState.conversationSummary, ChatViewModel.compactHistory()
- updated: BuildChatContextUseCase — параметр conversationSummary
- added: строки cmd_compact_start/done/failed, /compact в /? справке
- created: pages/sources/less-l9.md, pages/concepts/context-compression.md
- notes: BUILD SUCCESSFUL; summary хранится in-memory, не персистируется

## [2026-05-03] ingest | Л8 — Работа с токенами
- source: raw/less/l8.txt
- created: pages/sources/less-l8.md, pages/concepts/token-statistics.md
- notes: полностью реализовано; TokenUsage из SSE, StatisticsSection в UI

## [2026-05-03] ingest | Л7 — Сохранение контекста
- source: raw/less/l7.txt
- created: pages/sources/less-l7.md, pages/concepts/chat-history.md
- notes: полностью реализовано через Room KMP; loadHistory в onStart VM

## [2026-05-03] ingest | Л6 — Первый агент
- source: raw/less/l6.txt
- created: pages/sources/less-l6.md, pages/concepts/agent-architecture.md
- notes: задание полностью реализовано; ChatViewModel = агент, ChatRepository = инкапсулированный транспорт

## [2026-05-03] ingest | Л4 — Эксперимент с температурой
- source: raw/less/l4.txt
- created: pages/sources/less-l4.md, pages/concepts/temperature.md
- notes: задание учебное — новой фичи не требует, temperature полностью реализован

## [2026-05-03] impl | Л2 — Реализован max_tokens
- added: ApiSettings.maxTokens, ChatRequest.max_tokens, UserSettingsStorage.saveMaxTokens
- added: ChatState.maxTokens, ChatAction.OnMaxTokensChange, ChatViewModel.updateMaxTokens
- added: MaxTokensSlider.kt, SettingsPanel обновлён, строки label_max_tokens/label_max_tokens_unlimited
- updated: wiki/pages/sources/less-l2.md, concepts/llm-api-client.md, concepts/response-format-control.md
- notes: компиляция чистая (BUILD SUCCESSFUL)

## [2026-05-03] ingest | Л2 — Контроль формата ответа
- source: raw/less/l2.txt
- created: pages/sources/less-l2.md
- created concepts: pages/concepts/response-format-control.md
- updated: pages/concepts/llm-api-client.md (добавлены пробелы: max_tokens, stop, response_format)
- notes: частично реализовано — systemPrompt есть, max_tokens и stop отсутствуют в ChatRequest/ApiSettings

## [2026-05-03 00:00] init
- created: wiki/SCHEMA.md, wiki/index.md, wiki/log.md
- created dirs: wiki/raw/, wiki/raw/assets/, wiki/pages/{sources,entities,concepts,analyses}/
- notes: пустая структура готова, источников ещё нет
