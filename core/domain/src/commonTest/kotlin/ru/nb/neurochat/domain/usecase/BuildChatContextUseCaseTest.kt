package ru.nb.neurochat.domain.usecase

import ru.nb.neurochat.domain.model.ChatMessage
import ru.nb.neurochat.domain.model.ChatRole
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildChatContextUseCaseTest {

    private val useCase = BuildChatContextUseCase()

    private fun user(content: String) = ChatMessage(ChatRole.User, content)

    @Test
    fun emptyHistory_noSystem_returnsEmpty() {
        assertEquals(emptyList(), useCase(emptyList(), null, 0))
    }

    @Test
    fun systemPrompt_prependedAsFirstMessage() {
        val msgs = listOf(user("hi"))
        val ctx = useCase(msgs, "you are helpful", 0)
        assertEquals(2, ctx.size)
        assertEquals(ChatRole.System, ctx[0].role)
        assertEquals("you are helpful", ctx[0].content)
        assertEquals(msgs[0], ctx[1])
    }

    @Test
    fun maxContextZero_noTrim() {
        val msgs = (1..10).map { user("m$it") }
        val ctx = useCase(msgs, null, 0)
        assertEquals(10, ctx.size)
    }

    @Test
    fun maxContextLessThanHistory_trimsToLastN() {
        val msgs = (1..10).map { user("m$it") }
        val ctx = useCase(msgs, null, 3)
        assertEquals(3, ctx.size)
        assertEquals("m8", ctx[0].content)
        assertEquals("m10", ctx[2].content)
    }

    @Test
    fun maxContextLargerThanHistory_returnsAll() {
        val msgs = (1..3).map { user("m$it") }
        val ctx = useCase(msgs, null, 50)
        assertEquals(3, ctx.size)
    }

    @Test
    fun systemPromptWithTrim_promptStaysFirst() {
        val msgs = (1..5).map { user("m$it") }
        val ctx = useCase(msgs, "sys", 2)
        assertEquals(3, ctx.size)
        assertEquals(ChatRole.System, ctx[0].role)
        assertEquals("m4", ctx[1].content)
        assertEquals("m5", ctx[2].content)
    }
}
