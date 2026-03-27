package com.loopers.application.event

interface UserActionLogWriter {
    fun write(command: UserActionLogCommand)
}
