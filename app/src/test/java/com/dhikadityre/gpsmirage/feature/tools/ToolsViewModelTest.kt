package com.dhikadityre.gpsmirage.feature.tools

import com.dhikadityre.gpsmirage.core.model.Location
import com.dhikadityre.gpsmirage.domain.location.MapResetCoordinator
import com.dhikadityre.gpsmirage.domain.location.MockLocationManager
import com.dhikadityre.gpsmirage.domain.location.MockStartResult
import com.dhikadityre.gpsmirage.domain.location.ResetMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ToolsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class RecordingMockLocationManager : MockLocationManager {
        private val _isRunning = MutableStateFlow(true)
        override val isRunning: StateFlow<Boolean> = _isRunning
        private val _activeLocation = MutableStateFlow<Location?>(null)
        override val activeLocation: StateFlow<Location?> = _activeLocation
        var stopCount = 0

        override fun start(location: Location): MockStartResult {
            _isRunning.value = true
            return MockStartResult.Success
        }

        override fun stop() {
            stopCount++
            _isRunning.value = false
        }

        override fun isAppSetAsMockProvider(): Boolean = true
    }

    private lateinit var mockLocationManager: RecordingMockLocationManager
    private lateinit var mapResetCoordinator: MapResetCoordinator
    private lateinit var viewModel: ToolsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockLocationManager = RecordingMockLocationManager()
        mapResetCoordinator = MapResetCoordinator()
        viewModel = ToolsViewModel(
            mockLocationManager = mockLocationManager,
            mapResetCoordinator = mapResetCoordinator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState observes mock running status`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isMockRunning)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `onSoftReset stops mock, emits ResetMode SOFT and sets feedback message`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onSoftReset()
        advanceUntilIdle()

        assertEquals(1, mockLocationManager.stopCount)
        assertFalse(viewModel.uiState.value.isMockRunning)
        assertNotNull(viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.message!!.contains("Soft Reset Berhasil"))

        val resetReq = mapResetCoordinator.consumeReset()
        assertNotNull(resetReq)
        assertEquals(ResetMode.SOFT, resetReq!!.mode)
    }

    @Test
    fun `onHardReset stops mock, emits ResetMode HARD and sets feedback message`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onHardReset()
        advanceUntilIdle()

        assertEquals(1, mockLocationManager.stopCount)
        assertFalse(viewModel.uiState.value.isMockRunning)
        assertNotNull(viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.message!!.contains("Hard Reset Berhasil"))

        val resetReq = mapResetCoordinator.consumeReset()
        assertNotNull(resetReq)
        assertEquals(ResetMode.HARD, resetReq!!.mode)
    }

    @Test
    fun `clearMessage clears the message in uiState`() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onSoftReset()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.message)

        viewModel.clearMessage()
        assertNull(viewModel.uiState.value.message)
    }
}
