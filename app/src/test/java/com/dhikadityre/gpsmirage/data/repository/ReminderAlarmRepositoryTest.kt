package com.dhikadityre.gpsmirage.data.repository

import com.dhikadityre.gpsmirage.core.database.dao.ReminderAlarmDao
import com.dhikadityre.gpsmirage.core.database.entity.ReminderAlarmEntity
import com.dhikadityre.gpsmirage.core.model.ReminderAlarm
import com.dhikadityre.gpsmirage.data.alarm.ReminderAlarmManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ReminderAlarmRepositoryTest {

    private lateinit var fakeDao: FakeReminderAlarmDao
    private lateinit var fakeAlarmManager: FakeAlarmManager
    private lateinit var repository: ReminderAlarmRepositoryImpl

    class FakeReminderAlarmDao : ReminderAlarmDao {
        private val list = mutableListOf<ReminderAlarmEntity>()
        private val flow = MutableStateFlow<List<ReminderAlarmEntity>>(emptyList())
        private var nextId = 1L

        private fun emit() {
            flow.value = list.toList()
        }

        override fun getAll(): Flow<List<ReminderAlarmEntity>> = flow.asStateFlow()

        override suspend fun getAllList(): List<ReminderAlarmEntity> = list.toList()

        override suspend fun getEnabledAlarms(): List<ReminderAlarmEntity> {
            return list.filter { it.isEnabled }
        }

        override suspend fun getById(id: Long): ReminderAlarmEntity? {
            return list.find { it.id == id }
        }

        override suspend fun count(): Int = list.size

        override suspend fun insert(entity: ReminderAlarmEntity): Long {
            val id = if (entity.id == 0L) nextId++ else entity.id
            val saved = entity.copy(id = id)
            list.removeIf { it.id == id }
            list.add(saved)
            emit()
            return id
        }

        override suspend fun update(entity: ReminderAlarmEntity) {
            list.removeIf { it.id == entity.id }
            list.add(entity)
            emit()
        }

        override suspend fun delete(id: Long) {
            list.removeIf { it.id == id }
            emit()
        }

        override suspend fun setAllEnabled(isEnabled: Boolean, now: Long) {
            val updated = list.map { it.copy(isEnabled = isEnabled, updatedAt = now) }
            list.clear()
            list.addAll(updated)
            emit()
        }

        override suspend fun updateEnabled(id: Long, isEnabled: Boolean, now: Long) {
            val index = list.indexOfFirst { it.id == id }
            if (index != -1) {
                list[index] = list[index].copy(isEnabled = isEnabled, updatedAt = now)
                emit()
            }
        }
    }

    class FakeAlarmManager : ReminderAlarmManager() {
        val scheduledAlarms = mutableListOf<ReminderAlarm>()
        val cancelledIds = mutableListOf<Long>()

        override fun scheduleAlarm(alarm: ReminderAlarm) {
            scheduledAlarms.removeIf { it.id == alarm.id }
            scheduledAlarms.add(alarm)
        }

        override fun cancelAlarm(alarmId: Long) {
            scheduledAlarms.removeIf { it.id == alarmId }
            cancelledIds.add(alarmId)
        }

        override fun canScheduleExact(): Boolean = true
    }

    @Before
    fun setUp() {
        fakeDao = FakeReminderAlarmDao()
        fakeAlarmManager = FakeAlarmManager()
        repository = ReminderAlarmRepositoryImpl(fakeDao, fakeAlarmManager)
    }

    @Test
    fun `saveAlarm inserts new enabled alarm and schedules it`() = runTest {
        val alarm = ReminderAlarm(
            label = "Absen Pagi",
            hour = 8,
            minute = 0,
            daysOfWeek = setOf(1, 2, 3, 4, 5),
            isEnabled = true
        )

        val id = repository.saveAlarm(alarm)
        assertTrue(id > 0)

        val saved = repository.getAlarmById(id)
        assertNotNull(saved)
        assertEquals("Absen Pagi", saved?.label)
        assertEquals(8, saved?.hour)

        // Alarm manager should have scheduled it
        assertEquals(1, fakeAlarmManager.scheduledAlarms.size)
        assertEquals(id, fakeAlarmManager.scheduledAlarms[0].id)
    }

    @Test
    fun `saveAlarm with disabled state cancels any existing alarm`() = runTest {
        val alarm = ReminderAlarm(
            label = "Absen Pagi",
            hour = 8,
            minute = 0,
            isEnabled = true
        )
        val id = repository.saveAlarm(alarm)
        assertEquals(1, fakeAlarmManager.scheduledAlarms.size)

        // Update to disabled
        repository.saveAlarm(alarm.copy(id = id, isEnabled = false))
        assertEquals(0, fakeAlarmManager.scheduledAlarms.size)
        assertTrue(fakeAlarmManager.cancelledIds.contains(id))
    }

    @Test
    fun `toggleAlarm disables alarm and cancels schedule`() = runTest {
        val alarm = ReminderAlarm(label = "Test", hour = 9, minute = 30, isEnabled = true)
        val id = repository.saveAlarm(alarm)

        repository.toggleAlarm(id, false)
        val afterToggle = repository.getAlarmById(id)
        assertFalse(afterToggle!!.isEnabled)
        assertTrue(fakeAlarmManager.cancelledIds.contains(id))
    }

    @Test
    fun `deleteAlarm removes from database and cancels schedule`() = runTest {
        val alarm = ReminderAlarm(label = "Hapus", hour = 12, minute = 0, isEnabled = true)
        val id = repository.saveAlarm(alarm)

        repository.deleteAlarm(id)
        assertNull(repository.getAlarmById(id))
        assertTrue(fakeAlarmManager.cancelledIds.contains(id))
    }

    @Test
    fun `setAllAlarmsEnabled toggles all and manages alarm schedules`() = runTest {
        val id1 = repository.saveAlarm(ReminderAlarm(label = "1", hour = 8, minute = 0, isEnabled = true))
        val id2 = repository.saveAlarm(ReminderAlarm(label = "2", hour = 9, minute = 0, isEnabled = true))
        assertEquals(2, fakeAlarmManager.scheduledAlarms.size)

        // Disable all
        repository.setAllAlarmsEnabled(false)
        val allDisabled = repository.getAllAlarms().first()
        assertTrue(allDisabled.all { !it.isEnabled })
        assertTrue(fakeAlarmManager.cancelledIds.contains(id1))
        assertTrue(fakeAlarmManager.cancelledIds.contains(id2))

        // Enable all
        repository.setAllAlarmsEnabled(true)
        val allEnabled = repository.getAllAlarms().first()
        assertTrue(allEnabled.all { it.isEnabled })
        assertEquals(2, fakeAlarmManager.scheduledAlarms.size)
    }
}
