package com.inRussian.repositories

import com.inRussian.config.dbQuery
import com.inRussian.models.content.Report
import com.inRussian.requests.content.CreateReportRequest
import com.inRussian.tables.Reports
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID


interface ReportsRepository {
    suspend fun create(reporterId: String, req: CreateReportRequest): Report
    suspend fun findById(id: String): Report?
    suspend fun delete(id: String): Boolean
    suspend fun listAll(): List<Report>
}

class ExposedReportsRepository : ReportsRepository {
    override suspend fun create(reporterId: String, req: CreateReportRequest): Report = dbQuery {
        val id = Reports.insertAndGetId {
            it[description] = req.description
            it[taskId] = UUID.fromString(req.taskId)
            it[Reports.reporterId] = UUID.fromString(reporterId)
        }
        Reports.selectAll().where { Reports.id eq id }.single().toReport()
    }

    override suspend fun findById(id: String): Report? = dbQuery {
        Reports.selectAll().where { Reports.id eq UUID.fromString(id) }.singleOrNull()?.toReport()
    }

    override suspend fun delete(id: String): Boolean = dbQuery {
        Reports.deleteWhere { Reports.id eq UUID.fromString(id) } > 0
    }

    override suspend fun listAll(): List<Report> = dbQuery {
        Reports.selectAll().orderBy(Reports.createdAt, SortOrder.DESC).map { it.toReport() }
    }

    private fun ResultRow.toReport() = Report(
        id = this[Reports.id].toString(),
        description = this[Reports.description],
        taskId = this[Reports.taskId].toString(),
        reporterId = this[Reports.reporterId].toString(),
        createdAt = this[Reports.createdAt].toString()
    )
}