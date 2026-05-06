package ru.nb.neurochat.data.db

import ru.nb.neurochat.domain.model.Branch
import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.ChatRole
import ru.nb.neurochat.domain.model.ResponseStatistics

internal fun ChatMessageEntity.toChatMessage(): ChatMessage = ChatMessage(
    role = ChatRole.entries.first { it.value == role },
    content = content,
    statistics = if (durationMs != null && tokenCount != null && charCount != null) {
        ResponseStatistics(
            durationMs = durationMs,
            tokenCount = tokenCount,
            charCount = charCount,
        )
    } else null,
)

internal fun ChatMessage.toEntity(branchId: Long): ChatMessageEntity = ChatMessageEntity(
    branchId = branchId,
    role = role.value,
    content = content,
    durationMs = statistics?.durationMs,
    tokenCount = statistics?.tokenCount,
    charCount = statistics?.charCount,
)

internal fun BranchEntity.toBranch(): Branch = Branch(
    id = id,
    name = name,
    parentBranchId = parentBranchId,
    createdAt = createdAt,
)
