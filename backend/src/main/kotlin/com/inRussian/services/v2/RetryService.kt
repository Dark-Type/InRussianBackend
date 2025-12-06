package com.inRussian.services.v2

import com.inRussian.repositories.RetrySwitchRepository

class RetryService(private val repository: RetrySwitchRepository) {
    suspend fun getRetrySwitchStatus(): Result<Boolean> {
        return try {
            val status = repository.getStatus()
            Result.success(status)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setRetrySwitchStatus(enabled: Boolean): Result<Boolean> {
        return try {
            repository.toggle(enabled)
            Result.success(enabled)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}