package com.dhikadityre.gpsmirage.domain.location

import com.dhikadityre.gpsmirage.core.model.Coordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RewindCoordinatorTest {

    private lateinit var coordinator: RewindCoordinator

    private val centerAnchor = Coordinate(-6.213726, 106.819897) // A0: Mayapada
    private val mockA = Coordinate(-6.2088, 106.8456)            // A: Manggarai
    private val lockB = Coordinate(-6.2150, 106.8250)            // B: Lock 1
    private val posC = Coordinate(-6.2200, 106.8300)             // C: Map panned
    private val lockD = Coordinate(-6.2110, 106.8200)            // D: Lock 2

    @Before
    fun setUp() {
        coordinator = RewindCoordinator()
        coordinator.setCenterAnchor(centerAnchor)
    }

    @Test
    fun `RW-001 - user without mock and without lock rewinds to Center Anchor and disables button`() {
        // 1. Initial at anchor, then user moves map to C
        coordinator.onMapMoved(posC)

        assertTrue("Rewind button should be enabled when map moved away from anchor", coordinator.isRewindAvailable())
        val target = coordinator.executeRewindStep()
        assertNotNull(target)
        assertEquals(centerAnchor, target?.coordinate)
        assertFalse(target?.requiresUnlock ?: true)

        // Map reaches anchor
        coordinator.onMapMoved(centerAnchor)
        assertFalse("Rewind button should be disabled when final target reached", coordinator.isRewindAvailable())
        assertNull(coordinator.executeRewindStep())
    }

    @Test
    fun `RW-002 and RW-003 - user without mock rewinds to Lock B then Center Anchor`() {
        // User locks at B
        coordinator.onLockPosition(lockB)
        // Geser ke C
        coordinator.onMapMoved(posC)

        // Tap rewind 1 (RW-002)
        assertTrue(coordinator.isRewindAvailable())
        val target1 = coordinator.executeRewindStep()
        assertNotNull(target1)
        assertEquals(lockB, target1?.coordinate)
        assertFalse(target1?.requiresUnlock ?: true)

        // Map reaches B
        coordinator.onMapMoved(lockB)

        // Tap rewind 2 (RW-003)
        assertTrue("Rewind should be available for second stage", coordinator.isRewindAvailable())
        val target2 = coordinator.executeRewindStep()
        assertNotNull(target2)
        assertEquals(centerAnchor, target2?.coordinate)
        assertFalse(target2?.requiresUnlock ?: true)

        // Map reaches anchor
        coordinator.onMapMoved(centerAnchor)
        assertFalse("Rewind should be disabled after final anchor target reached", coordinator.isRewindAvailable())
        assertNull(coordinator.executeRewindStep())
    }

    @Test
    fun `RW-004 and RW-005 - user with mock A and lock B rewinds to B then auto-unlocks and rewinds to A`() {
        // User mocked at A, locked at B, map at C
        coordinator.onMockStarted(mockA)
        coordinator.onLockPosition(lockB)
        coordinator.onMapMoved(posC)

        // Tap rewind 1 (RW-004)
        val target1 = coordinator.executeRewindStep()
        assertNotNull(target1)
        assertEquals(lockB, target1?.coordinate)
        assertFalse(target1?.requiresUnlock ?: true)

        // Map is at B, user is still locked
        coordinator.onMapMoved(lockB)

        // Tap rewind 2 (RW-005)
        val target2 = coordinator.executeRewindStep()
        assertNotNull(target2)
        assertEquals(mockA, target2?.coordinate)
        assertTrue("Second rewind to mock position must require unlock", target2?.requiresUnlock ?: false)

        // Map reaches A and unlocks
        coordinator.onUnlockPosition()
        coordinator.onMapMoved(mockA)
        assertFalse("Rewind should be disabled once mock location final target reached", coordinator.isRewindAvailable())
    }

    @Test
    fun `RW-006 - manual unlock after rewind 1 still targets mock location without requiring auto-unlock`() {
        coordinator.onMockStarted(mockA)
        coordinator.onLockPosition(lockB)
        coordinator.onMapMoved(posC)

        // Rewind 1
        val target1 = coordinator.executeRewindStep()
        assertEquals(lockB, target1?.coordinate)
        coordinator.onMapMoved(lockB)

        // Manual unlock
        coordinator.onUnlockPosition()

        // Rewind 2
        val target2 = coordinator.executeRewindStep()
        assertEquals(mockA, target2?.coordinate)
        assertFalse("Since already unlocked manually, requiresUnlock should be false", target2?.requiresUnlock ?: true)
    }

    @Test
    fun `RW-007 - map already at mock A as final target has rewind disabled`() {
        coordinator.onMockStarted(mockA)
        coordinator.onMapMoved(mockA)

        assertFalse(coordinator.isRewindAvailable())
        assertNull(coordinator.executeRewindStep())
    }

    @Test
    fun `RW-009 - mock and lock at identical coordinate A preserves two distinct stages`() {
        coordinator.onMockStarted(mockA)
        coordinator.onLockPosition(mockA)
        coordinator.onMapMoved(posC)

        // Step 1: Lock A
        val target1 = coordinator.executeRewindStep()
        assertNotNull(target1)
        assertEquals(mockA, target1?.coordinate)
        assertFalse(target1?.requiresUnlock ?: true)

        coordinator.onMapMoved(mockA)

        // Step 2: Mock A with requiresUnlock
        val target2 = coordinator.executeRewindStep()
        assertNotNull(target2)
        assertEquals(mockA, target2?.coordinate)
        assertTrue(target2?.requiresUnlock ?: false)

        coordinator.onUnlockPosition()
        coordinator.onMapMoved(mockA)
        assertFalse(coordinator.isRewindAvailable())
    }

    @Test
    fun `RW-010 - relocking at D after rewinding to B updates stage 1 to D`() {
        coordinator.onMockStarted(mockA)
        coordinator.onLockPosition(lockB)
        coordinator.onMapMoved(posC)

        // Rewind to B
        coordinator.executeRewindStep()
        coordinator.onMapMoved(lockB)

        // Re-lock at D
        coordinator.onLockPosition(lockD)
        coordinator.onMapMoved(posC)

        // Tap rewind 1 -> should go to D
        val target1 = coordinator.executeRewindStep()
        assertEquals(lockD, target1?.coordinate)

        coordinator.onMapMoved(lockD)

        // Tap rewind 2 -> should go to A
        val target2 = coordinator.executeRewindStep()
        assertEquals(mockA, target2?.coordinate)
    }

    @Test
    fun `RW-014 - mock A and lock B, rewind to B, unlock, pan to C, rewind goes to A`() {
        coordinator.onMockStarted(mockA)
        coordinator.onLockPosition(lockB)
        coordinator.onMapMoved(posC)

        coordinator.executeRewindStep()
        coordinator.onMapMoved(lockB)

        coordinator.onUnlockPosition()
        coordinator.onMapMoved(posC)

        val target = coordinator.executeRewindStep()
        assertEquals(mockA, target?.coordinate)
    }

    @Test
    fun `RW-015 - no mock and lock B, rewind to B, unlock, pan to C, rewind goes to Center Anchor`() {
        coordinator.onLockPosition(lockB)
        coordinator.onMapMoved(posC)

        coordinator.executeRewindStep()
        coordinator.onMapMoved(lockB)

        coordinator.onUnlockPosition()
        coordinator.onMapMoved(posC)

        val target = coordinator.executeRewindStep()
        assertEquals(centerAnchor, target?.coordinate)
    }

    @Test
    fun `RW-018 and OD-06 - panning map away from final target re-enables rewind to final target`() {
        // Map at center anchor
        coordinator.onMapMoved(centerAnchor)
        assertFalse(coordinator.isRewindAvailable())

        // User pans to C
        coordinator.onMapMoved(posC)
        assertTrue(coordinator.isRewindAvailable())

        val target = coordinator.executeRewindStep()
        assertEquals(centerAnchor, target?.coordinate)
    }

    @Test
    fun `RW-016 and RW-032 - starting mock at new coordinate resets final target`() {
        coordinator.onMockStarted(mockA)
        coordinator.onLockPosition(lockB)

        // User starts new mock at lockD
        coordinator.onMockStarted(lockD)
        coordinator.onMapMoved(posC)

        // Target 1 is still lock B if locked
        val target1 = coordinator.executeRewindStep()
        assertEquals(lockB, target1?.coordinate)

        // Target 2 is new mock lockD
        val target2 = coordinator.executeRewindStep()
        assertEquals(lockD, target2?.coordinate)
    }

    @Test
    fun `RW-026 - rapid tap advances sequentially and does not skip or corrupt state`() {
        coordinator.onMockStarted(mockA)
        coordinator.onLockPosition(lockB)
        coordinator.onMapMoved(posC)

        // Rapid tap 1
        val t1 = coordinator.executeRewindStep()
        assertEquals(lockB, t1?.coordinate)

        // Rapid tap 2 without map moved yet
        val t2 = coordinator.executeRewindStep()
        assertEquals(mockA, t2?.coordinate)
        assertTrue(t2?.requiresUnlock ?: false)

        // Rapid tap 3
        val t3 = coordinator.executeRewindStep()
        assertNull("Rapid tap beyond stages must return null and not corrupt", t3)
        assertFalse(coordinator.isRewindAvailable())
    }

    @Test
    fun `RW-033 - unconfigured coordinator handles calls safely without crashing`() {
        val emptyCoordinator = RewindCoordinator()
        assertFalse(emptyCoordinator.isRewindAvailable())
        assertNull(emptyCoordinator.executeRewindStep())
    }

    @Test
    fun `RW-008 and RW-020 - lock at X outside radius is honored as stage 1 before center anchor`() {
        val posX = Coordinate(-6.3000, 106.9000) // X: Outside radius
        coordinator.onLockPosition(posX)
        coordinator.onMapMoved(posC)

        // Stage 1: Lock X
        val t1 = coordinator.executeRewindStep()
        assertEquals(posX, t1?.coordinate)

        coordinator.onMapMoved(posX)

        // Stage 2: Center Anchor
        val t2 = coordinator.executeRewindStep()
        assertEquals(centerAnchor, t2?.coordinate)
        coordinator.onMapMoved(centerAnchor)

        assertFalse(coordinator.isRewindAvailable())
    }

    @Test
    fun `RW-024 - very close coordinates preserve distinct sequential stages`() {
        val closeCoord = Coordinate(mockA.latitude + 0.000005, mockA.longitude + 0.000005)
        coordinator.onMockStarted(mockA)
        coordinator.onLockPosition(closeCoord)
        coordinator.onMapMoved(posC)

        val t1 = coordinator.executeRewindStep()
        assertEquals(closeCoord, t1?.coordinate)

        val t2 = coordinator.executeRewindStep()
        assertEquals(mockA, t2?.coordinate)
    }

    @Test
    fun `RW-036 - rewind to B then map moved again to C still rewinds to A`() {
        coordinator.onMockStarted(mockA)
        coordinator.onLockPosition(lockB)
        coordinator.onMapMoved(posC)

        // Rewind to B
        val t1 = coordinator.executeRewindStep()
        assertEquals(lockB, t1?.coordinate)
        coordinator.onMapMoved(lockB)

        // Map moved again to C
        coordinator.onMapMoved(posC)

        // Next rewind must still target A
        val t2 = coordinator.executeRewindStep()
        assertEquals(mockA, t2?.coordinate)
    }
}
