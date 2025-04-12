package com.decade.practice.model

data class ChatSnapshot(
    val conversation: Conversation,
    val eventList: MutableList<ChatEvent> = mutableListOf(),
) {
    val identifier: ChatIdentifier
        get() = conversation.chat.identifier

    fun addFirst(event: ChatEvent) {
        if (!event.committed) {
            eventList.add(0, event)
            return
        }
        var posToAdd = 0
        eventList.forEach { existing ->
            if (!existing.committed) posToAdd++

            if (existing.id == event.id) {
                existing.committed = true
                existing.receiveTime = event.receiveTime
                existing.eventVersion = event.eventVersion
                return
            }
        }
        eventList.add(posToAdd, event)
        return
    }

    fun addAll(list: List<ChatEvent>) {
        if (eventList.isEmpty()) {
            eventList.addAll(list)
            return
        }
        list.forEach { chatEvent ->
            add(chatEvent)
        }
    }

    fun add(chatEvent: ChatEvent) {
        if (eventList.isEmpty()) {
            eventList.add(chatEvent)
            return
        }
        assert(chatEvent.committed)
        val time = chatEvent.receiveTime
        val timeLast = eventList.lastOrNull()?.receiveTime ?: Long.MAX_VALUE
        if (time < timeLast)
            eventList.add(chatEvent)
    }

    fun rollBack(time: Int) {
        while (eventList.isNotEmpty()) {
            val timeFirst = eventList.firstOrNull()?.receiveTime
            if (timeFirst != null && timeFirst <= time)
                break
            eventList.removeFirst()
        }
    }

    val messages = emptyList<Message>()

    override fun hashCode(): Int {
        return conversation.chat.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ChatSnapshot)
            return false
        return other.identifier == identifier
    }

    companion object {
        fun singleEvent(event: ChatEvent): ChatSnapshot {
            val conversation = Conversation(
                event.chat, event.partner, event.owner,
            )
            return ChatSnapshot(conversation, mutableListOf(event))
        }
    }
}

