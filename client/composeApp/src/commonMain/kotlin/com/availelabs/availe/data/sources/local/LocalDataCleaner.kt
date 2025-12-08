package com.availelabs.availe.data.sources.local

import com.availelabs.availe.data.database.IDatabaseFileController
import com.availelabs.availe.data.database.IDatabaseProvider
import com.availelabs.availe.data.preferences.IListingPreferences
import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.removeTokens

interface ILocalDataCleaner {
    suspend fun clearAllData()
}

class LocalDataCleaner @OptIn(ExperimentalOpenIdConnect::class) constructor(
    private val tokenStore: TokenStore,
    private val listingPreferences: IListingPreferences,
    private val databaseProvider: IDatabaseProvider,
    private val databaseFileController: IDatabaseFileController
) : ILocalDataCleaner {

    @OptIn(ExperimentalOpenIdConnect::class)
    override suspend fun clearAllData() {
        try {
            databaseProvider.closeAndClearDatabase()
        } catch (_: Exception) {
        }

        try {
            databaseFileController.deleteDatabaseFile()
        } catch (_: Exception) {
        }

        try {
            tokenStore.removeTokens()
        } catch (_: Exception) {
        }

        try {
            listingPreferences.clear()
        } catch (_: Exception) {
        }
    }
}