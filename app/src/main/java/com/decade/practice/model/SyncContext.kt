package com.decade.practice.model

class SyncContext(
    var eventVersion: Int,
) {
    constructor(syncContext: SyncContext) : this(syncContext.eventVersion)

    fun incVersion(): Int {
        return ++eventVersion
    }
}
