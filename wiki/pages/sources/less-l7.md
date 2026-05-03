---
type: source
title: "Л7 — Сохранение контекста"
slug: less-l7
source_path: raw/less/l7.txt
ingested_at: 2026-05-03
tags: [lesson, history, persistence, sqlite, room]
references: [concepts/chat-history]
---

## Краткое содержание

Задание седьмого занятия: добавить агенту сохранение истории диалога (JSON или SQLite), загрузку при перезапуске и продолжение диалога без потери контекста.

## Требования задания

- Хранить историю диалога в JSON или SQLite
- При перезапуске загружать историю обратно
- Продолжать диалог как будто агент не выключался

## Статус в NeuroChat

✅ **Реализовано** (SQLite через Room KMP)

| Требование | Где реализовано |
|---|---|
| Хранилище истории | Room KMP: `NeuroChatDatabase`, `ChatMessageEntity`, `ChatMessageDao` |
| Сохранение сообщений | `RoomChatHistoryDataSource.saveMessage()` — вызывается после каждого завершённого обмена |
| Загрузка при старте | `ChatViewModel.loadHistory()` — вызывается в `onStart` StateFlow |
| Очистка | `ChatAction.OnClearHistory` → `historyDataSource.clearAll()` |

Хранятся только завершённые сообщения (user + assistant после окончания стрима). Системные сообщения (результаты команд) не сохраняются.

## Связи

- [[concepts/chat-history]] — концепция персистентности истории
