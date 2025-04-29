package no.uio.ifi.in2000.team54.data.frost

import android.util.Base64
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import no.uio.ifi.in2000.team54.domain.Coordinates
import no.uio.ifi.in2000.team54.enums.Elements
import no.uio.ifi.in2000.team54.model.frost.AvailableObservationResponse
import no.uio.ifi.in2000.team54.model.frost.ObservationData
import no.uio.ifi.in2000.team54.model.frost.ObservationResponse
import no.uio.ifi.in2000.team54.model.frost.SensorSystem
import no.uio.ifi.in2000.team54.model.frost.SourceResponse
import kotlin.to

class FrostDatasource {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    // authentication
    private val raw = "b8d04ecc-dc8a-40a9-942e-2acfb8aba15d" + ":" // client id for team54@uio.no
    private val encoded: String =
        Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    private val authHeader = "Basic $encoded"

    private val nameMap = mapOf(
        Elements.TEMP to "mean(air_temperature%20P1M)",
        Elements.CLOUD to "mean(cloud_area_fraction%20P1D)",
        Elements.SNOW to "mean(snow_coverage_type%20P1M)",
        Elements.IRRIDANCE to "mean(surface_downwelling_shortwave_flux_in_air%20PT1H)"
    )

    private var sensorMap: MutableMap<Elements, MutableList<String>> = mutableMapOf()

    private val referenceTime = "2022-12-31/2024-12-31"


    suspend fun fetchObservationDataFromFrost(
        coordinates: Coordinates,
        elementName: Elements,
    ): List<ObservationData> {
        val start = System.currentTimeMillis()
        val sensorUrl = ""

        var response: HttpResponse

        // find out which of the sensors are available for the time series.
        val getAvailableSensors = "https://frost.met.no/observations/availableTimeSeries/v0.jsonld?sources=$sensorUrl&referencetime=2022-12-31%2F2024-12-31&elements=${nameMap[elementName]}"

        response = client.get(getAvailableSensors) {
            header(HttpHeaders.Authorization, authHeader)
            header(HttpHeaders.Accept, "application/json")
        }

        // stores available sensor response in the AvailableObservationReponse data class
        val availableObservationResponse: AvailableObservationResponse = response.body()
        val availableObservations = availableObservationResponse.data

        // retrieves the first sensor from the response, which is the most suitable (in terms of time and closest in location) for our query
        val sensorId = availableObservations.firstOrNull()?.sourceId ?: return emptyList()
        Log.i("testingSensors", "element: $elementName, sensors: $sensorId")

        // use the new sensorId, which is also the closest with available data for the reference time to retrieve observation data.
        var url = "https://frost.met.no/observations/v0.jsonld?sources=$sensorId&referencetime=$referenceTime&elements=${nameMap[elementName]}"

        if (nameMap[elementName] == "mean(surface_downwelling_shortwave_flux_in_air%20PT1H)") {
            url = "$url&qualities=0" // no data of other qualities
        }

        url = "$url&fields=sourceId%2CelementId%2Cvalue%2Cunit%2CqualityCode%2CreferenceTime" // restric amount of data retrieved

        try {
            response = client.get(url) {
                header(HttpHeaders.Authorization, authHeader)
                header(HttpHeaders.Accept, "application/json")
            }
        } catch (e: Exception) {
            Log.e("fetchObservationData", "Error fetching data for timeline $referenceTime", e)
            return emptyList()  // Return an empty list or handle the error
        }
        // stores the response from the api in to the ObservationResponse data class
        val observationResponse: ObservationResponse = response.body()

        println("TIME : ${System.currentTimeMillis()-start}")
        return observationResponse.data
    }
}