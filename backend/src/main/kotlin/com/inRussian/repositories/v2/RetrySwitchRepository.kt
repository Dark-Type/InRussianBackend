package com.inRussian.repositories.v2

import com.inRussian.config.DatabaseFactory.dbQuery
import com.inRussian.tables.v2.RetrySwitchTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update


interface RetrySwitchRepository {
    suspend fun getStatus(): Boolean
    suspend fun toggle(newStatus: Boolean)
}

class RetrySwitchRepositoryImplementation : RetrySwitchRepository {

    override suspend fun getStatus(): Boolean = dbQuery {
        RetrySwitchTable.selectAll()
            .limit(1)
            .map { it[RetrySwitchTable.enabled] }
            .firstOrNull() ?: false
    }

    override suspend fun toggle(newStatus: Boolean) = dbQuery {
        val updated = RetrySwitchTable.update({ RetrySwitchTable.id eq 1 }) {
            it[enabled] = newStatus
        }
        if (updated == 0) {
            RetrySwitchTable.insert {
                it[id] = 1
                it[enabled] = newStatus
            }
        }
    }
}