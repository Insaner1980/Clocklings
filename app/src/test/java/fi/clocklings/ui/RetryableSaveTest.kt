package fi.clocklings.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RetryableSaveTest {
    @Test
    fun aRetainedRetryCannotStartAnotherWrite() {
        val save = RetryableSave()
        var attempts = 0
        lateinit var result: (Boolean) -> Unit
        save.run {
            attempts++
            result = it
        }
        result(false)
        val retry = checkNotNull(save.retry)

        retry()
        retry()

        assertEquals(2, attempts)
        assertNull(save.retry)
    }

    @Test
    fun newRequestClearsFailureAndIgnoresOlderResults() {
        val save = RetryableSave()
        lateinit var firstResult: (Boolean) -> Unit
        lateinit var secondResult: (Boolean) -> Unit
        save.run { firstResult = it }
        firstResult(false)
        assertNotNull(save.retry)
        save.run { secondResult = it }
        assertNull(save.retry)
        firstResult(false)
        assertNull(save.retry)
        secondResult(false)
        assertNotNull(save.retry)
        firstResult(true)
        assertNotNull(save.retry)
        save.retry?.invoke()
        assertNull(save.retry)
        secondResult(true)
        assertNull(save.retry)
    }
}
