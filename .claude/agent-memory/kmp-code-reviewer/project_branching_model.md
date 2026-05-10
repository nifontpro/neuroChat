---
name: Ссылочная модель ветвления (v3)
description: Паттерны и инварианты ссылочной модели ветвления в RoomChatHistoryDataSource — критично для будущих ревью
type: project
---

В core/data реализована ссылочная (не копирующая) модель ветвления истории чата.

## Ключевые инварианты

- `Branch.forkFromMessageId = null` только у root-ветки (parentBranchId = null).
- `Branch.forkFromMessageId = 0L` у дочерней ветки, если родитель не имел сообщений при fork.
- `Branch.forkFromMessageId > 0` у дочерней ветки с реальным cutoff.
- НИКОГДА не сохранять `forkFromMessageId = null` для ветки с `parentBranchId != null` — это ломает алгоритм чтения (null интерпретируется как «брать все сообщения предка», включая будущие).

## Критически найденный баг (исправлен в ревью, май 2026)

`createBranchFrom` использовал `maxOwnId(parent.id)` который возвращает `Long?`. При null (нет сообщений) напрямую сохранялся null в `forkFromMessageId`. Это приводило к тому, что дочерняя ветка получала все будущие сообщения родителя. Исправлено: `?: 0L`.

## Алгоритм чтения buildAncestorChainTopDown

Строит список (branchId, cutoff) от корня к leaf. cutoff для предка N = forkFromMessageId ветки N+1. Для leaf cutoff = null (читать всё своё). Защита от циклов через `visited: Set<Long>`.

## Транзакционность (добавлена в ревью, май 2026)

clearAll и deleteBranch обёрнуты в `database.useWriterConnection { it.immediateTransaction { } }`. Для этого RoomChatHistoryDataSource принимает `NeuroChatDatabase` напрямую помимо DAO.

**Why:** Room 2.7 KMP не имеет `withTransaction` как extension на RoomDatabase. Правильный паттерн: `useWriterConnection { transactor -> transactor.immediateTransaction { ... } }`.

**How to apply:** При любых многошаговых операциях с несколькими DAO-вызовами в core:data использовать этот паттерн.
