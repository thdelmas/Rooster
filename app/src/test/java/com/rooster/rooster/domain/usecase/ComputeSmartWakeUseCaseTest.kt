package com.rooster.rooster.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ComputeSmartWakeUseCaseTest {

    private val useCase = ComputeSmartWakeUseCase()

    private val now = 1_700_000_000_000L
    private val hour = 60 * 60 * 1000L
    private val day = 24 * hour

    @Test
    fun `no anchor, no mandatory — wakes after target sleep`() {
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = 480,
                mandatoryWakeMillis = null,
                solarAnchorMillis = null
            )
        )
        assertEquals(now + 8 * hour, result)
    }

    @Test
    fun `mandatory only at bedtime — wakes at next mandatory`() {
        val mandatory = now + 8 * hour
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = 480,
                mandatoryWakeMillis = mandatory,
                solarAnchorMillis = null
            )
        )
        assertEquals(mandatory, result)
    }

    @Test
    fun `mandatory only during daytime — wakes at next mandatory, not now plus sleep`() {
        // caller passes next-occurrence mandatory (tomorrow morning) — 21h from now
        val mandatoryTomorrow = now + 21 * hour
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = 480,
                mandatoryWakeMillis = mandatoryTomorrow,
                solarAnchorMillis = null
            )
        )
        assertEquals(mandatoryTomorrow, result)
    }

    @Test
    fun `solar anchor after earliest — wakes at anchor`() {
        val anchor = now + 9 * hour
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = 480,
                mandatoryWakeMillis = null,
                solarAnchorMillis = anchor
            )
        )
        assertEquals(anchor, result)
    }

    @Test
    fun `solar anchor before earliest — pushed forward by one day to honor min sleep`() {
        val anchor = now + 6 * hour
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = 480,
                mandatoryWakeMillis = null,
                solarAnchorMillis = anchor
            )
        )
        assertEquals(anchor + day, result)
    }

    @Test
    fun `mandatory caps a late solar anchor`() {
        // anchor pushes to day-after, mandatory is tomorrow — mandatory wins
        val anchor = now + 5 * hour
        val mandatory = now + 8 * hour
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = 480,
                mandatoryWakeMillis = mandatory,
                solarAnchorMillis = anchor
            )
        )
        assertEquals(mandatory, result)
    }

    @Test
    fun `solar and mandatory both fit — solar wins`() {
        val anchor = now + 9 * hour
        val mandatory = now + 10 * hour
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = 480,
                mandatoryWakeMillis = mandatory,
                solarAnchorMillis = anchor
            )
        )
        assertEquals(anchor, result)
    }

    @Test
    fun `zero target sleep, solar set — wakes at solar`() {
        val anchor = now + 30 * 60 * 1000L
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = 0,
                mandatoryWakeMillis = null,
                solarAnchorMillis = anchor
            )
        )
        assertEquals(anchor, result)
    }

    @Test
    fun `negative target sleep treated as zero`() {
        val result = useCase.execute(
            ComputeSmartWakeUseCase.Input(
                nowMillis = now,
                targetSleepMinutes = -10,
                mandatoryWakeMillis = null,
                solarAnchorMillis = null
            )
        )
        assertEquals(now, result)
    }
}
