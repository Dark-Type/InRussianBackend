package com.inRussian.repositories.v2

import com.inRussian.tables.TaskEntity
import com.inRussian.tables.Themes
import com.inRussian.tables.v2.UserSectionQueueItemTable
import com.inRussian.tables.v2.UserSectionQueueStateTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID
import kotlin.and
import kotlin.text.get
import kotlin.text.set

class SectionCompletionRepository() {

    private val themeSectionIdCol = Themes.id
    private val themeOrderCol = Themes.orderNum
    private val taskThemeIdCol = TaskEntity.id

    suspend fun markCompletedFlag(userId: UUID, sectionId: UUID, completed: Boolean) = newSuspendedTransaction(
        Dispatchers.IO
    ) {
        UserSectionQueueStateTable.update(
            where = { (UserSectionQueueStateTable.userId eq userId) and (UserSectionQueueStateTable.sectionId eq sectionId) }
        ) {
            it[UserSectionQueueStateTable.completed] = completed
        }
    }

    suspend fun hasRemainingThemesWithTasksAfter(sectionId: UUID, lastOrderNum: Int?): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            val themeRows = Themes
                .selectAll().where { themeSectionIdCol eq sectionId }
                .let { query ->
                    if (lastOrderNum != null) query.adjustWhere { themeOrderCol greater lastOrderNum } else query
                }
                .orderBy(themeOrderCol to SortOrder.ASC)
                .toList()

            themeRows.forEach { themeRow ->
                val themeId = themeRow[Themes.columns.first { it.name == "id" } as Column<UUID>]
                val hasTasks = TaskEntity
                    .selectAll().where { taskThemeIdCol eq themeId }
                    .limit(1)
                    .any()
                if (hasTasks) return@newSuspendedTransaction true
            }
            false
        }

    suspend fun isQueueEmpty(userId: UUID, sectionId: UUID): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        !UserSectionQueueItemTable
            .selectAll()
            .where { (UserSectionQueueItemTable.userId eq userId) and (UserSectionQueueItemTable.sectionId eq sectionId) }
            .limit(1)
            .any()
    }
}