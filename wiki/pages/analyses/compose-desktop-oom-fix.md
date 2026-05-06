---
type: analysis
title: "Compose Desktop падает с exit 137 (SIGKILL/OOM): фикс jvmArgs"
slug: compose-desktop-oom-fix
created_at: 2026-05-06
question: "Почему :composeApp:desktopRun падает с exit 137 и как это починить"
sources: []
references: []
tags: [compose-desktop, gradle, jvm, troubleshooting, build]
---

## Симптом

Запуск десктопа из Android Studio (Run/Debug на `fun main()` в `composeApp`) падает с:

```
Execution failed for task ':composeApp:desktopRun'.
> Process 'command '/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java''
  finished with non-zero exit value 137
  (this value may indicate that the process was terminated with the SIGKILL signal,
  which is often caused by the system running out of memory)
```

## Диагноз

Exit 137 = `SIGKILL` от ядра macOS из-за нехватки RAM. Compose Desktop приложение
по умолчанию стартует **без явного `-Xmx`**, JVM берёт дефолт (~25% системной памяти).
На машине, где параллельно работают Android Studio + Gradle daemon (4 ГБ) + KSP daemon +
Kotlin compiler daemon — оперативки физически не остаётся, и macOS первым отстреливает
наименее «защищённый» процесс — приложение.

Из косвенных проверок:
- `./gradlew :composeApp:run` (Compose-плагиновый таск) на чистой системе запускался без
  падений (`main branch created`, исключений нет) — значит дело не в коде.
- `:composeApp:desktopRun` — это **отдельный** generic JVM-таск из Kotlin JVM target,
  не `:composeApp:run`. Он не наследует jvmArgs из `compose.desktop.application` и main
  class подсовывает только IDE через свою Run configuration.
- В логах было видно `daemonOpts=-Xmx4096M` — Gradle daemon забирал 4 ГБ.

## Решение

**1. `composeApp/build.gradle.kts`** — явный heap для приложения и для всех «run»-тасков
из плагина hot-reload, которые могут запустить JVM:

```kotlin
compose.desktop {
    application {
        mainClass = "ru.nb.neurochat.MainKt"
        jvmArgs += listOf("-Xmx1024M", "-Xms256M")
        // ...
    }
}

// :composeApp:desktopRun (от Kotlin JVM target) и hotRunDesktop (от compose hot reload)
// — отдельные JavaExec-таски, не наследуют jvmArgs из compose.desktop.application.
tasks.withType<JavaExec>().configureEach {
    val n = name
    if (n == "desktopRun" || n.startsWith("hotRunDesktop") || n.startsWith("hotDevDesktop")) {
        jvmArgs("-Xmx1024M", "-Xms256M")
    }
}
```

**2. `gradle.properties`** — уменьшил heap Gradle daemon:

```properties
org.gradle.jvmargs=-Xmx2560M -Dfile.encoding=UTF-8
```

**3. ⚠ Обязательно `./gradlew --stop`** после правки `gradle.properties`.

> Без этого старый daemon продолжает жить со старым `-Xmx4096M`, и фикс **не применится**.
> Это и было ключевой ошибкой первого подхода: после правки файлов SIGKILL повторился,
> и только перезапуск daemon'а через `--stop` решил проблему. Подтверждено пользователем.

## Что не делать

- Не пытайся запускать `./gradlew :composeApp:desktopRun` напрямую из CLI:
  ```
  > No main class specified and classpath is not an executable jar.
  ```
  Это generic JVM-таск, main class он получает только от IDE. Для CLI используй
  `./gradlew :composeApp:run`.
- Не повышай heap Gradle daemon обратно «на всякий случай» — у него и так достаточно
  для KSP/Kotlin daemon при `2560M`.

## Чек-лист при повторении

1. Проверить `composeApp/build.gradle.kts` — есть ли `jvmArgs` в `compose.desktop.application`
   и в `tasks.withType<JavaExec>` для `desktopRun`/`hotRunDesktop*`.
2. Проверить `gradle.properties` — `org.gradle.jvmargs` не выше `2560M`.
3. `./gradlew --stop` → перезапуск IDE или повторный Run.
4. Если 1024M мало (для очень больших диалогов) — поднять до 1536–2048M, но сначала
   уменьшить gradle daemon ещё.

## Связи

Фикс не специфичен для Л10 — симптом возможен в любой сессии при дефиците RAM.
Коммитнут вместе с реализацией [[sources/less-l10]] (стратегии управления контекстом),
но не зависит от неё содержательно.
