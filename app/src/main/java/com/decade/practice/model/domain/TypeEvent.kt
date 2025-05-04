package com.decade.practice.model.domain

class TypeEvent(
    var chat: ChatIdentifier,
    var from: String,
    var time: Long,
) {
    constructor(chat: Chat) : this(chat.identifier, chat.partner, 0)
}
