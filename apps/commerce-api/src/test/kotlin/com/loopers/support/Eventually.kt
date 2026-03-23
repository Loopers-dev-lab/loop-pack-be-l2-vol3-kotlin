package com.loopers.support

fun eventually(
    timeoutMillis: Long = 3_000,
    intervalMillis: Long = 50,
    assertion: () -> Unit,
) {
    val deadline = System.currentTimeMillis() + timeoutMillis
    var lastError: AssertionError? = null

    while (System.currentTimeMillis() < deadline) {
        try {
            assertion()
            return
        } catch (error: AssertionError) {
            lastError = error
            Thread.sleep(intervalMillis)
        }
    }

    throw lastError ?: AssertionError("Condition was not met within ${timeoutMillis}ms")
}
