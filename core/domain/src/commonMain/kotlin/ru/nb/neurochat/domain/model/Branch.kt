package ru.nb.neurochat.domain.model

/**
 * Ветка диалога. id = 1 у системно создаваемой главной ветки «main».
 * [parentBranchId] — null у main, иначе id ветки, от которой ответвились.
 * Точка ответвления — все сообщения родителя на момент создания (физически копируются в новую ветку).
 */
data class Branch(
    val id: Long,
    val name: String,
    val parentBranchId: Long?,
    val createdAt: Long,
)
