---
type: concept
name: "История чата (персистентность)"
slug: chat-history
tags: [persistence, sqlite, room, history]
sources: [sources/less-l7]
related_concepts: [concepts/agent-architecture]
updated_at: 2026-05-03
---

## Определение

Сохранение истории диалога между сессиями: сообщения записываются в БД после каждого обмена и восстанавливаются при запуске приложения.

## Реализация в NeuroChat

**Стек:** Room KMP (androidx.room) + SQLite, работает на Android, Desktop и iOS.

```
ChatViewModel
  ├── loadHistory()     ← onStart: загружает все сообщения из БД
  └── saveMessage()     ← после стрима: сохраняет user + assistant

RoomChatHistoryDataSource  (implements IChatHistoryDataSource)
  └── ChatMessageDao    ← getAll / insert / deleteAll

ChatMessageEntity       ← @Entity("messages"): id, role, content, ...
NeuroChatDatabase       ← @Database, platform-specific DatabaseFactory
```

**Что сохраняется:** завершённые `user` и `assistant` сообщения.  
**Что не сохраняется:** системные сообщения (результаты команд `/system`, `/t` и т.д.).

## Контекстное окно

`BuildChatContextUseCase` обрезает историю до `maxContextMessages` последних сообщений перед отправкой в API — независимо от того, сколько сообщений хранится в БД. Системный промпт добавляется отдельно всегда.

## Связи

- [[concepts/agent-architecture]] — агент, использующий историю
