package com.decade.practice.model.domain

class SyncContext(
    var eventVersion: Int,
) {
    constructor(syncContext: SyncContext) : this(syncContext.eventVersion)

    fun incVersion(): Int {
        return ++eventVersion
    }
}
