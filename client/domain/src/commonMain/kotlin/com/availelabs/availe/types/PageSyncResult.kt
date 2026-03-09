package com.availelabs.availe.types

import com.availelabs.availe.model.error.AppError

sealed interface PageSyncResult {
    data class Success(
        val itemsFetched: Int,
        val hasMoreAvailable: Boolean
    ) : PageSyncResult

    data class Failure(
        val error: AppError
    ) : PageSyncResult
}