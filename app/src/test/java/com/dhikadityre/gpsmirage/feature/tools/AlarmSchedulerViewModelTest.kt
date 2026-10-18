package com.dhikadityre.gpsmirage.feature.tools

import com.dhikadityre.gpsmirage.core.model.ReminderAlarm
import com.dhikadityre.gpsmirage.data.repository.ReminderAlarmRepositoryTest.FakeAlarmManager
import com.dhikadityre.gpsmirage.data.repository.ReminderAlarmRepositoryTest.FakeReminderAlarmDao
import com.dhikadityre.gpsmirage.data.repository.ReminderAlarmRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmSchedulerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeReminderAlarmDao
    private lateinit var fakeAlarmManager: FakeAlarmManager
    private lateinit var repository: ReminderAlarmRepositoryImpl
    private lateinit var viewModel: AlarmSchedulerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeReminderAlarmDao()
        fakeAlarmManager = FakeAlarmManager()
        repository = ReminderAlarmRepositoryImpl(fakeDao, fakeAlarmManager)
        viewModel = AlarmSchedulerViewModel(repository, fakeAlarmManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state observes empty alarms and master enabled by default`() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.alarms.isEmpty())
        assertTrue(state.isMasterEnabled)
        assertFalse(state.isDialogOpen)
    }

    @Test
    fun `saveAlarm adds new alarm and updates state message`() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.openCreateDialog()
        assertTrue(viewModel.uiState.value.isDialogOpen)

        viewModel.saveAlarm(
            label = "Absen Masuk",
            hour = 8,
            minute = 15,
            daysOfWeek = setOf(1, 2, 3, 4, 5)
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDialogOpen)
        assertEquals(1, state.alarms.size)
        assertEquals("Absen Masuk", state.alarms[0].label)
        assertEquals("Jadwal alarm berhasil disimpan", state.userMessage)
    }

    @Test
    fun `toggleMaster disables and enables all alarms`() = runTest(testDispatcher) {
        viewModel.openCreateDialog()
        viewModel.saveAlarm("A1", 8, 0, setOf(1))
        advanceUntilIdle()

        // Turn master toggle off
        viewModel.toggleMaster(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isMasterEnabled)
        assertFalse(viewModel.uiState.value.alarms[0].isEnabled)

        // Turn master toggle on
        viewModel.toggleMaster(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isMasterEnabled)
        assertTrue(viewModel.uiState.value.alarms[0].isEnabled)
    }

    @Test
    fun `delete confirmation flow deletes alarm successfully`() = runTest(testDispatcher) {
        viewModel.openCreateDialog()
        viewModel.saveAlarm("A1", 8, 0, setOf(1))
        advanceUntilIdle()

        val alarm = viewModel.uiState.value.alarms[0]
        viewModel.requestDelete(alarm)
        assertEquals(alarm, viewModel.uiState.value.alarmToDelete)

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.alarms.isEmpty())
        assertNull(viewModel.uiState.value.alarmToDelete)
        assertEquals("Alarm berhasil dihapus", viewModel.uiState.value.userMessage)
    }

    @Test
    fun `saveAlarm with full parameters preserves all new options`() = runTest(testDispatcher) {
        viewModel.openCreateDialog()
        viewModel.saveAlarm(
            title = "Kantor Pusat",
            label = "Jangan lupa tap lokasi",
            hour = 7,
            minute = 45,
            daysOfWeek = emptySet(),
            isDeleteAfterRinging = true,
            soundUri = "content://media/internal/audio/media/1",
            snoozeMinutes = 10,
            isVibrate = false
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.alarms.size)
        val alarm = state.alarms[0]
        assertEquals("Kantor Pusat", alarm.title)
        assertEquals("Jangan lupa tap lokasi", alarm.label)
        assertEquals(7, alarm.hour)
        assertEquals(45, alarm.minute)
        assertTrue(alarm.daysOfWeek.isEmpty())
        assertTrue(alarm.isDeleteAfterRinging)
        assertEquals("content://media/internal/audio/media/1", alarm.soundUri)
        assertEquals(10, alarm.snoozeMinutes)
        assertFalse(alarm.isVibrate)
    }

    @Test
    fun `testAlarm triggers userMessage confirmation`() = runTest(testDispatcher) {
        viewModel.testAlarm(
            title = "Test Judul",
            label = "Test Label",
            soundUri = null,
            isVibrate = true,
            snoozeMinutes = 5
        )
        assertEquals("Notifikasi pengujian berhasil dikirim!", viewModel.uiState.value.userMessage)
    }
}
