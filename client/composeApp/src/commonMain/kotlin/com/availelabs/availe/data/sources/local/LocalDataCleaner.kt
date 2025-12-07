package com.availelabs.availe.data.sources.local

import org.publicvalue.multiplatform.oidc.ExperimentalOpenIdConnect
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.removeTokens

interface ILocalDataCleaner {
    suspend fun clearAllData()
}

class LocalDataCleaner @OptIn(ExperimentalOpenIdConnect::class) constructor(
    private val tokenStore: TokenStore,
    private val listingPreferences: IListingPreferences,
    private val listingLocalDataSource: IListingLocalDataSource
) : ILocalDataCleaner {

    @OptIn(ExperimentalOpenIdConnect::class)
    override suspend fun clearAllData() {
        try {
            tokenStore.removeTokens()
        } catch (_: Exception) {
        }

        try {
            listingPreferences.clear()
        } catch (_: Exception) {
        }

        try {
            listingLocalDataSource.deleteAllListings()
        } catch (_: Exception) {
        }
    }
}