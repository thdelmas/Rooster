package com.rooster.rooster.util

import com.rooster.rooster.data.local.entity.AstronomyDataEntity
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

/**
 * On-device solar position calculator using NOAA solar equations.
 * Computes sunrise, sunset, solar noon, and twilight times for a given location and date.
 *
 * Based on NOAA Solar Calculator: https://gml.noaa.gov/grad/solcalc/
 * Accuracy: ~1 minute for sunrise/sunset at most latitudes.
 */
object SolarCalculator {

    // Sun zenith angles for different twilight definitions
    private const val ZENITH_OFFICIAL = 90.833       // sunrise/sunset (includes refraction)
    private const val ZENITH_CIVIL = 96.0            // civil twilight
    private const val ZENITH_NAUTICAL = 102.0        // nautical twilight
    private const val ZENITH_ASTRONOMICAL = 108.0    // astronomical twilight

    /**
     * Calculate all solar events for a given location and date.
     * Returns an AstronomyDataEntity ready for Room insertion.
     */
    fun calculate(latitude: Float, longitude: Float, date: Calendar = Calendar.getInstance()): AstronomyDataEntity {
        val year = date.get(Calendar.YEAR)
        val month = date.get(Calendar.MONTH) + 1
        val day = date.get(Calendar.DAY_OF_MONTH)
        val timezone = date.timeZone

        val jd = julianDay(year, month, day)
        val jc = julianCentury(jd)
        val dateMs = date.timeInMillis

        val solarNoonLST = solarNoonLocal(jc, longitude.toDouble(), timezone, dateMs)
        val solarNoonMs = timeToMillis(date, solarNoonLST)

        val sunrise = sunriseSet(jc, latitude.toDouble(), longitude.toDouble(), timezone, dateMs, ZENITH_OFFICIAL, true)
        val sunset = sunriseSet(jc, latitude.toDouble(), longitude.toDouble(), timezone, dateMs, ZENITH_OFFICIAL, false)

        val civilDawn = sunriseSet(jc, latitude.toDouble(), longitude.toDouble(), timezone, dateMs, ZENITH_CIVIL, true)
        val civilDusk = sunriseSet(jc, latitude.toDouble(), longitude.toDouble(), timezone, dateMs, ZENITH_CIVIL, false)

        val nauticalDawn = sunriseSet(jc, latitude.toDouble(), longitude.toDouble(), timezone, dateMs, ZENITH_NAUTICAL, true)
        val nauticalDusk = sunriseSet(jc, latitude.toDouble(), longitude.toDouble(), timezone, dateMs, ZENITH_NAUTICAL, false)

        val astroDawn = sunriseSet(jc, latitude.toDouble(), longitude.toDouble(), timezone, dateMs, ZENITH_ASTRONOMICAL, true)
        val astroDusk = sunriseSet(jc, latitude.toDouble(), longitude.toDouble(), timezone, dateMs, ZENITH_ASTRONOMICAL, false)

        val sunriseMs = timeToMillis(date, sunrise)
        val sunsetMs = timeToMillis(date, sunset)
        val dayLength = sunsetMs - sunriseMs

        return AstronomyDataEntity(
            id = 1,
            latitude = latitude,
            longitude = longitude,
            sunrise = sunriseMs,
            sunset = sunsetMs,
            solarNoon = solarNoonMs,
            civilDawn = timeToMillis(date, civilDawn),
            civilDusk = timeToMillis(date, civilDusk),
            nauticalDawn = timeToMillis(date, nauticalDawn),
            nauticalDusk = timeToMillis(date, nauticalDusk),
            astroDawn = timeToMillis(date, astroDawn),
            astroDusk = timeToMillis(date, astroDusk),
            lastUpdated = System.currentTimeMillis(),
            dayLength = dayLength
        )
    }

    /**
     * Convert fractional hours (local time) to UTC millis on the given date.
     */
    private fun timeToMillis(date: Calendar, hours: Double): Long {
        val h = hours.toInt()
        val m = ((hours - h) * 60).toInt()
        val s = (((hours - h) * 60 - m) * 60).toInt()

        val cal = Calendar.getInstance(date.timeZone).apply {
            set(Calendar.YEAR, date.get(Calendar.YEAR))
            set(Calendar.MONTH, date.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, s)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    // ==================== NOAA Solar Equations ====================

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) { y--; m += 12 }
        val a = y / 100
        val b = 2 - a + a / 4
        return (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + day + b - 1524.5
    }

    private fun julianCentury(jd: Double): Double {
        return (jd - 2451545.0) / 36525.0
    }

    private fun geomMeanLongSun(t: Double): Double {
        var l = 280.46646 + t * (36000.76983 + 0.0003032 * t)
        while (l > 360) l -= 360
        while (l < 0) l += 360
        return l
    }

    private fun geomMeanAnomalySun(t: Double): Double {
        return 357.52911 + t * (35999.05029 - 0.0001537 * t)
    }

    private fun eccentricityEarthOrbit(t: Double): Double {
        return 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
    }

    private fun sunEqOfCenter(t: Double): Double {
        val m = Math.toRadians(geomMeanAnomalySun(t))
        return sin(m) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
                sin(2 * m) * (0.019993 - 0.000101 * t) +
                sin(3 * m) * 0.000289
    }

    private fun sunTrueLong(t: Double): Double {
        return geomMeanLongSun(t) + sunEqOfCenter(t)
    }

    private fun sunApparentLong(t: Double): Double {
        val omega = 125.04 - 1934.136 * t
        return sunTrueLong(t) - 0.00569 - 0.00478 * sin(Math.toRadians(omega))
    }

    private fun meanObliquityOfEcliptic(t: Double): Double {
        return 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
    }

    private fun obliquityCorrection(t: Double): Double {
        val omega = 125.04 - 1934.136 * t
        return meanObliquityOfEcliptic(t) + 0.00256 * cos(Math.toRadians(omega))
    }

    private fun sunDeclination(t: Double): Double {
        val e = Math.toRadians(obliquityCorrection(t))
        val lambda = Math.toRadians(sunApparentLong(t))
        return Math.toDegrees(asin(sin(e) * sin(lambda)))
    }

    private fun equationOfTime(t: Double): Double {
        val epsilon = Math.toRadians(obliquityCorrection(t))
        val l0 = Math.toRadians(geomMeanLongSun(t))
        val e = eccentricityEarthOrbit(t)
        val m = Math.toRadians(geomMeanAnomalySun(t))

        val y = tan(epsilon / 2).pow(2)

        val eqTime = y * sin(2 * l0) -
                2 * e * sin(m) +
                4 * e * y * sin(m) * cos(2 * l0) -
                0.5 * y * y * sin(4 * l0) -
                1.25 * e * e * sin(2 * m)

        return Math.toDegrees(eqTime) * 4 // Convert to minutes
    }

    /**
     * Calculate hour angle for a given zenith angle.
     * Returns NaN for polar day/night (sun never reaches that zenith).
     */
    private fun hourAngle(lat: Double, declin: Double, zenith: Double): Double {
        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(declin)
        val zenRad = Math.toRadians(zenith)

        val cosHA = (cos(zenRad) / (cos(latRad) * cos(decRad))) - tan(latRad) * tan(decRad)

        if (cosHA > 1.0 || cosHA < -1.0) return Double.NaN // No sunrise/sunset

        return Math.toDegrees(acos(cosHA))
    }

    /**
     * Calculate solar noon in local decimal hours.
     */
    private fun solarNoonLocal(t: Double, longitude: Double, tz: TimeZone, dateMillis: Long): Double {
        val eqTime = equationOfTime(t)
        val offset = tz.getOffset(dateMillis) / 3600000.0 // hours, includes DST when in effect
        return (720.0 - 4 * longitude - eqTime + offset * 60) / 60.0
    }

    /**
     * Calculate sunrise or sunset time in local decimal hours.
     * @param isSunrise true for sunrise, false for sunset
     */
    private fun sunriseSet(
        t: Double, lat: Double, lng: Double, tz: TimeZone, dateMillis: Long,
        zenith: Double, isSunrise: Boolean
    ): Double {
        val eqTime = equationOfTime(t)
        val declin = sunDeclination(t)
        val ha = hourAngle(lat, declin, zenith)
        val offset = tz.getOffset(dateMillis) / 3600000.0 // hours, includes DST when in effect

        if (ha.isNaN()) {
            // Polar day/night fallback: return solar noon ± 0 (event doesn't occur)
            return (720.0 - 4 * lng - eqTime + offset * 60) / 60.0
        }

        return if (isSunrise) {
            (720.0 - 4 * (lng + ha) - eqTime + offset * 60) / 60.0
        } else {
            (720.0 - 4 * (lng - ha) - eqTime + offset * 60) / 60.0
        }
    }
}
