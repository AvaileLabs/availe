package com.availelabs.availe.sources.remote

import com.availelabs.availe.domain.model.EventFeedFilter
import com.availelabs.availe.domain.model.ListingCategoryFilter
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ListingRemoteDataSourceTest {

    @Test
    fun getListingsPage_returnsEmptyList_whenResponseBodyIsEmptyArray() {
        runBlocking {
            val httpClient = createHttpClient {
                respond(
                    content = "[]",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }

            val listingRemoteDataSource = ListingRemoteDataSource(
                httpClient = httpClient,
                json = createJson()
            )

            val listings = listingRemoteDataSource.getListingsPage(
                page = 0,
                size = 20,
                publisherId = null,
                filter = ListingCategoryFilter.All,
                eventFeedFilter = EventFeedFilter.None
            )

            assertEquals(0, listings.size)
        }
    }

    private fun createHttpClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): HttpClient {
        return HttpClient(MockEngine(handler)) {
            defaultRequest {
                url("http://localhost/api/")
            }
        }
    }

    private fun createJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
