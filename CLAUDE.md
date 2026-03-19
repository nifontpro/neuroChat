# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Проект

**NeuroChat** — Kotlin Multiplatform чат-бот с OpenAI-совместимым API. Поддерживает Android, iOS, Desktop (JVM). UI построен на Compose Multiplatform с Material3 Adaptive. Пакет: `ru.nb.neurochat`.

## Команды

```bash
./gradlew :androidApp:assembleDebug           # Android APK
./gradlew :androidApp:assembleRelease
./gradlew :composeApp:run                     # Desktop запуск
./gradlew :composeApp:compileDesktopMain      # Desktop компиляция
./gradlew :composeApp:compileKotlinIosSimulatorArm64  # iOS компиляция
./gradlew :core:domain:build                  # Собрать отдельный модуль
```

## Модульная архитектура

```
build-logic/convention/       → Convention plugins (ru.nb.neurochat.convention.*)
                                KmpLibrary, CmpLibrary, CmpFeature, CmpApplication,
                                AndroidApplication, AndroidApplicationCompose, BuildKonfig

androidApp/                   → Android точка входа (MainActivity, NeuroChatApp, Manifest)
composeApp/                   → KMP shared UI + точки входа Desktop/iOS + DI init
core/
  domain/                     → Чистые модели, интерфейсы, util (Result/Error/DataError)
  data/                       → Ktor клиент, ChatRepository, ConnectivityObserver, BuildKonfig
  presentation/               → DeviceConfiguration, ObserveAsEvents, ClearFocusOnTap
  designsystem/               → NeuroChatTheme (Material3 light/dark)
feature/
  chat/presentation/          → ChatViewModel, ChatScreen (адаптивный), SettingsPanel, DI
```

**Зависимости:**
```
androidApp → composeApp → feature:chat:presentation → core:presentation
                                                     → core:designsystem
                                                     → core:domain
                                                     → core:data → core:domain
```

## Ключевые паттерны

**Convention Plugins (AGP 9.0):** KMP модули используют `com.android.kotlin.multiplatform.library`, androidApp — `com.android.application`. Desktop target: `jvm("desktop")` → `desktopMain`.

**Adaptive Layout:** `DeviceConfiguration` определяет тип устройства из `WindowSizeClass`. На desktop/tablet landscape — `Row(SettingsPanel | Chat)`, на mobile — `ModalBottomSheet` для настроек.

**ConnectivityObserver (expect/actual):**
- Android: `ConnectivityManager.NetworkCallback`
- iOS: `nw_path_monitor`
- Desktop: polling DNS (8.8.8.8, 1.1.1.1)

**BuildKonfig:** Читает `litellm.properties` при сборке → генерирует API_KEY, BASE_URL, MODEL, TIMEOUT_SECONDS. Public accessor: `ru.nb.neurochat.data.defaultApiSettings()`.

**DI (Koin):** `initKoin()` в composeApp принимает `platformModules` lambda для platform-specific DI (ConnectivityObserver). Вызывается в NeuroChatApp (Android), main.kt (Desktop), MainViewController (iOS).

**Ktor engine:** `androidMain` → okhttp, `iosMain` → darwin, `desktopMain` → cio.

**Streaming:** `OpenAiClient.chatStream()` → `Flow<StreamToken>` через SSE. `ChatViewModel` накапливает токены.

## Технологический стек

- **Kotlin** 2.3.10, **Compose Multiplatform** 1.10.1, **Material3 Adaptive** 1.2.0
- **Ktor** 3.4.0, **kotlinx.serialization** 1.10.0, **Koin** 4.1.1
- **Kermit** 2.0.8 — логирование
- **Android:** compileSdk/targetSdk 36, minSdk 26, Java 17
- **AGP** 9.0.1, **KSP** 2.3.4
- Зависимости: `gradle/libs.versions.toml`
