package no.uio.ifi.in2000.team54.util

import no.uio.ifi.in2000.team54.domain.SolarArray
import kotlin.math.abs
import kotlin.math.max

object CalculationUtil {

    // Constants for clarity and maintainability
    private const val DEFAULT_PANEL_TEMPERATURE_CELSIUS = 25.0
    private const val DEFAULT_PANEL_EFFICIENCY = 0.20
    private const val TEMPERATURE_EFFICIENCY_LOSS_PER_CELSIUS = 0.5 // Percentage loss per degree Celsius
    private const val MINIMUM_OPTIMAL_INCLINE_ANGLE = 15.0 // Degrees
    private const val MAXIMUM_OPTIMAL_INCLINE_ANGLE = 45.0 // Degrees
    private const val INCLINE_ANGLE_EFFICIENCY_DECREASE_PER_DEGREE = 0.05
    private const val DIRECTION_EFFICIENCY_DECREASE_PER_DEGREE = 0.005
    private const val MAXIMUM_SNOW_COVERAGE_METERS = 4.0
    private const val MAXIMUM_CLOUD_COVER_SCALE = 8

    // Data source for average monthly sun hours
    private val averageMonthlySunHours = mapOf(
        "01" to 74.0,
        "02" to 90.0,
        "03" to 150.0,
        "04" to 200.0,
        "05" to 210.0,
        "06" to 230.0,
        "07" to 210.0,
        "08" to 180.0,
        "09" to 150.0,
        "10" to 90.0,
        "11" to 60.0,
        "12" to 55.0
    )

    /**
     * Calculates the estimated electricity production for a solar array over a year.
     *
     * @param monthlyCloud Map of monthly cloud cover (0-8 scale).
     * @param monthlySnow Map of monthly snow coverage (in meters).
     * @param monthlyRadiance Map of monthly solar radiance (W/m^2).
     * @param monthlyTemperatures Map of monthly average temperatures (Celsius).
     * @param solarArray The solar array configuration.
     * @return A map of monthly electricity production (kWh).
     */
    fun calculateMonthlyElectricityProduction(
        monthlyCloud: Map<String, Double>,
        monthlySnow: Map<String, Double>,
        monthlyRadiance: Map<String, Double>,
        monthlyTemperatures: Map<String, Double>,
        solarArray: SolarArray
    ): Map<String, Double> {
        val adjustedMonthlyIrradiance = calculateAdjustedSolarIrradiance(monthlyCloud, monthlySnow, monthlyRadiance)
        val roofSections = solarArray.roofSections

        return adjustedMonthlyIrradiance.mapValues { (month, irradiance) ->
            val sunHours = averageMonthlySunHours[month] ?: 0.0
            val temperature = monthlyTemperatures[month] ?: DEFAULT_PANEL_TEMPERATURE_CELSIUS
            val efficiency = calculatePanelEfficiency(temperature)

            roofSections.sumOf { roofSection ->
                irradiance * sunHours * efficiency * roofSection.area * calculateDirectionImpact(roofSection.direction) * calculateInclineAngleImpact(roofSection.incline)
            } / 1000.0 // Convert to kWh
        }
    }

    /**
     * Calculates the adjusted solar irradiance based on cloud and snow coverage.
     *
     * @param monthlyCloud Map of monthly cloud cover.
     * @param monthlySnow Map of monthly snow coverage.
     * @param monthlyRadiance Map of monthly solar radiance.
     * @return A map of adjusted monthly solar irradiance.
     */
    private fun calculateAdjustedSolarIrradiance(
        monthlyCloud: Map<String, Double>,
        monthlySnow: Map<String, Double>,
        monthlyRadiance: Map<String, Double>
    ): Map<String, Double> {
        return monthlyRadiance.mapValues { (month, radiance) ->
            var adjustedIrradiance = radiance
            val snowFactor = monthlySnow[month]?.let { calculateSnowLossFactor(it) } ?: 1.0
            val cloudFactor = monthlyCloud[month]?.let { calculateCloudLossFactor(it) } ?: 1.0
            adjustedIrradiance *= snowFactor * cloudFactor
            adjustedIrradiance
        }
    }

    /**
     * Calculates the efficiency of the solar panel based on temperature.
     *
     * @param temperature The temperature of the solar panel (Celsius).
     * @return The efficiency of the solar panel.
     */
    private fun calculatePanelEfficiency(temperature: Double): Double {
        var efficiency = DEFAULT_PANEL_EFFICIENCY
        if (temperature > DEFAULT_PANEL_TEMPERATURE_CELSIUS) {
            val temperatureDifference = temperature - DEFAULT_PANEL_TEMPERATURE_CELSIUS
            val efficiencyLossPercentage = temperatureDifference * TEMPERATURE_EFFICIENCY_LOSS_PER_CELSIUS
            efficiency -= efficiency * (efficiencyLossPercentage / 100.0)
        }
        return efficiency
    }

    /**
     * Calculates the loss factor due to snow coverage.
     *
     * @param snowCoverage The snow coverage (in meters).
     * @return The snow loss factor.
     */
    private fun calculateSnowLossFactor(snowCoverage: Double): Double {
        return when {
            snowCoverage <= 0.0 -> 1.0
            snowCoverage >= MAXIMUM_SNOW_COVERAGE_METERS -> 0.90
            else -> 1.0 - (snowCoverage * 0.02)
        }
    }

    /**
     * Calculates the loss factor due to cloud cover.
     *
     * @param cloudCover The cloud cover (0-8 scale).
     * @return The cloud loss factor.
     */
    private fun calculateCloudLossFactor(cloudCover: Double): Double {
        return when {
            cloudCover <= 0.0 -> 1.0
            cloudCover >= MAXIMUM_CLOUD_COVER_SCALE -> 0.50
            else -> 1.0 - (cloudCover * 0.05)
        }
    }

    /**
     * Calculates the impact on efficiency due to the incline angle of the roof.
     *
     * @param inclineAngle The incline angle of the roof (degrees).
     * @return The impact factor on efficiency.
     */
    private fun calculateInclineAngleImpact(inclineAngle: Double): Double {
        if (inclineAngle in MINIMUM_OPTIMAL_INCLINE_ANGLE..MAXIMUM_OPTIMAL_INCLINE_ANGLE) {
            return 1.0
        }
        val angleDifference = if (inclineAngle < MINIMUM_OPTIMAL_INCLINE_ANGLE) {
            MINIMUM_OPTIMAL_INCLINE_ANGLE - inclineAngle
        } else {
            inclineAngle - MAXIMUM_OPTIMAL_INCLINE_ANGLE
        }
        return max(0.5, 1 - (angleDifference * INCLINE_ANGLE_EFFICIENCY_DECREASE_PER_DEGREE))
    }

    /**
     * Calculates the impact on efficiency due to the direction (azimuth) of the roof.
     *
     * @param azimuth The direction (azimuth) of the roof (degrees).
     * @return The impact factor on efficiency.
     */
    private fun calculateDirectionImpact(azimuth: Double): Double {
        val directionDifference = abs(180 - azimuth)
        return max(0.5, 1 - (directionDifference * DIRECTION_EFFICIENCY_DECREASE_PER_DEGREE))
    }
}