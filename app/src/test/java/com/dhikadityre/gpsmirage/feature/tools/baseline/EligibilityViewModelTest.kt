package com.dhikadityre.gpsmirage.feature.tools.baseline

import com.dhikadityre.gpsmirage.domain.baseline.EligibilityReport
import com.dhikadityre.gpsmirage.domain.baseline.MockEligibilityChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EligibilityViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeChecker: FakeEligibilityChecker
    private lateinit var viewModel: EligibilityViewModel

    private class FakeEligibilityChecker : MockEligibilityChecker {
        var reportToReturn = EligibilityReport(
            isDeveloperOptionsEnabled = true,
            isMockAppSet = true,
            isGpsEnabled = true,
            hasLocationPermission = true,
            isDeviceRooted = false
        )

        override fun checkEligibility(): EligibilityReport = reportToReturn
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeChecker = FakeEligibilityChecker()
        viewModel = EligibilityViewModel(fakeChecker)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state runs check and marks fully eligible when all requirements pass`() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertNotNull(state.report)
        assertTrue(state.report!!.isFullyEligible)
        assertTrue(state.report!!.isDeveloperOptionsEnabled)
        assertTrue(state.report!!.isMockAppSet)
        assertTrue(state.report!!.isGpsEnabled)
        assertTrue(state.report!!.hasLocationPermission)
        assertFalse(state.report!!.isDeviceRooted)
    }

    @Test
    fun `when developer options are disabled, isFullyEligible is false`() = runTest {
        fakeChecker.reportToReturn = fakeChecker.reportToReturn.copy(isDeveloperOptionsEnabled = false)
        viewModel.refreshEligibility()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.report)
        assertFalse(state.report!!.isFullyEligible)
        assertFalse(state.report!!.isDeveloperOptionsEnabled)
    }

    @Test
    fun `when mock app is not set, isFullyEligible is false`() = runTest {
        fakeChecker.reportToReturn = fakeChecker.reportToReturn.copy(isMockAppSet = false)
        viewModel.refreshEligibility()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.report)
        assertFalse(state.report!!.isFullyEligible)
        assertFalse(state.report!!.isMockAppSet)
    }

    @Test
    fun `when gps is disabled, isFullyEligible is false`() = runTest {
        fakeChecker.reportToReturn = fakeChecker.reportToReturn.copy(isGpsEnabled = false)
        viewModel.refreshEligibility()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.report)
        assertFalse(state.report!!.isFullyEligible)
        assertFalse(state.report!!.isGpsEnabled)
    }

    @Test
    fun `when location permission is missing, isFullyEligible is false`() = runTest {
        fakeChecker.reportToReturn = fakeChecker.reportToReturn.copy(hasLocationPermission = false)
        viewModel.refreshEligibility()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.report)
        assertFalse(state.report!!.isFullyEligible)
        assertFalse(state.report!!.hasLocationPermission)
    }

    @Test
    fun `root detection is reported accurately without breaking basic eligibility`() = runTest {
        fakeChecker.reportToReturn = fakeChecker.reportToReturn.copy(isDeviceRooted = true)
        viewModel.refreshEligibility()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.report)
        assertTrue(state.report!!.isDeviceRooted)
        assertTrue(state.report!!.isFullyEligible)
    }
}
