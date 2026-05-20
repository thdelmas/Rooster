package com.rooster.rooster.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ComputeSmartWakeUseCaseTest {

    private val useCase = ComputeSmartWakeUseCase()

    private val now = 1_700_000_000_000L
    private val hour = 60 * 60 * 1000L

    @Test
    fun `sunrise anchor wins when set`() {
        val sunrise = now + 9 * hour
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = 480,
                mandatoryWakeMillis = null,
                sunriseAnchorMillis = sunrise,
            )
        )
        assertEquals(sunrise, result)
    }

    @Test
    fun `mandatory caps a late sunrise anchor`() {
        val sunrise = now + 10 * hour
        val mandatory = now + 8 * hour
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = 480,
                mandatoryWakeMillis = mandatory,
                sunriseAnchorMillis = sunrise,
            )
        )
        assertEquals(mandatory, result)
    }

    @Test
    fun `sunrise anchor used when earlier than mandatory`() {
        val sunrise = now + 6 * hour
        val mandatory = now + 9 * hour
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = 480,
                mandatoryWakeMillis = mandatory,
                sunriseAnchorMillis = sunrise,
            )
        )
        assertEquals(sunrise, result)
    }

    @Test
    fun `no sunrise — falls back to mandatory`() {
        val mandatory = now + 8 * hour
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = 480,
                mandatoryWakeMillis = mandatory,
                sunriseAnchorMillis = null,
            )
        )
        assertEquals(mandatory, result)
    }

    @Test
    fun `no sunrise no mandatory — falls back to now plus target sleep`() {
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = 480,
                mandatoryWakeMillis = null,
                sunriseAnchorMillis = null,
            )
        )
        assertEquals(now + 8 * hour, result)
    }

    @Test
    fun `target sleep does not push wake forward when sunrise is set`() {
        // sunrise is 4h from now; target sleep is 8h. Wake should be sunrise,
        // not sunrise + 4h. Bedtime is the user's problem.
        val sunrise = now + 4 * hour
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = 480,
                mandatoryWakeMillis = null,
                sunriseAnchorMillis = sunrise,
            )
        )
        assertEquals(sunrise, result)
    }

    @Test
    fun `target sleep does not push wake forward when mandatory is set`() {
        val mandatory = now + 4 * hour
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = 480,
                mandatoryWakeMillis = mandatory,
                sunriseAnchorMillis = null,
            )
        )
        assertEquals(mandatory, result)
    }

    @Test
    fun `negative target sleep treated as zero in fallback`() {
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = -10,
                mandatoryWakeMillis = null,
                sunriseAnchorMillis = null,
            )
        )
        assertEquals(now, result)
    }
}
