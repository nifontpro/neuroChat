# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Проект

**NeuroChat** — Kotlin Multiplatform чат-бот с OpenAI-совместимым API. Поддерживает Android, iOS, Desktop (JVM). UI построен на Compose Multiplatform. Пакет: `ru.nb.neurochat`.

## Команды

```bash
./gradlew :composeApp:assembleDebug        # Android APK
./gradlew :composeApp:assembleRelease
./gradlew :composeApp:run                  # Desktop запуск
./gradlew :composeApp:test                 # Unit тесты
./gradlew :composeApp:lint                 # Lint
./gradlew :core:domain:build               # Собрать отдельный модуль
```

## Модульная архитектура

```
build-logic/                  → convention plugin: neurochat.kmp-library
                                (настраивает KMP targets для Android/iOS/JVM)
core/
  domain/                     → чистые модели + интерфейсы, без зависимостей
  network/                    → Ktor клиент + реализация IChatRepository
feature/
  chat/                       → ChatViewModel, ChatState, ChatScreen (UI)
composeApp/                   → точки входа + Koin DI инициализация
```

**Зависимости между модулями:**
```
composeApp → feature:chat → core:domain
composeApp → core:network → core:domain
```

## Ключевые паттерны

**Expected/Actual для Ktor engine:** каждая платформа подключает свой engine через sourceset:
- `androidMain` → `ktor-client-okhttp`
- `iosMain` → `ktor-client-darwin`
- `jvmMain` → `ktor-client-cio`
- `commonMain` вызывает `HttpClient { }` — engine подхватывается автоматически

**DI (Koin):** инициализируется один раз в платформенной точке входа:
- Android: `MainActivity.onCreate()` с `androidContext()`
- Desktop: перед `application { }`
- iOS: перед `ComposeUIViewController`

**Streaming:** `OpenAiClient.chatStream()` возвращает `Flow<StreamToken>` — SSE парсинг через Ktor channel. `ChatViewModel` накапливает токены в последнее сообщение ассистента.

**Настройки API:** `ApiSettings` задаётся в `composeApp/di/AppModule.kt` → `defaultSettings`. Это временное решение, будет вынесено в экран Settings.

## Технологический стек

- **Kotlin** 2.3.0, **Compose Multiplatform** 1.10.0, **Material3**
- **Ktor** 3.1.3 — HTTP клиент
- **kotlinx.serialization** 1.9.0 — JSON
- **Koin** 4.0.0 — DI
- **Android:** compileSdk/targetSdk 36, minSdk 26, Java 11
- **AGP** 8.11.2, **Gradle** 8.14.3
- Зависимости: `gradle/libs.versions.toml`

## Gradle properties

В `gradle.properties` включены:
- `org.gradle.configuration-cache=true`
- `org.gradle.caching=true`
- `org.gradle.jvmargs=-Xmx4096M`
