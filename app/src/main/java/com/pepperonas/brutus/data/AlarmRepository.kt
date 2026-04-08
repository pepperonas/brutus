package com.pepperonas.brutus.data

import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val dao: AlarmDao) {

    val allAlarms: Flow<List<AlarmEntity>> = dao.getAllAlarms()

    suspend fun getById(id: Long): AlarmEntity? = dao.getById(id)

    suspend fun getEnabledAlarms(): List<AlarmEntity> = dao.getEnabledAlarms()

    suspend fun insert(alarm: AlarmEntity): Long = dao.insert(alarm)

    suspend fun update(alarm: AlarmEntity) = dao.update(alarm)

    suspend fun delete(alarm: AlarmEntity) = dao.delete(alarm)

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)
}
