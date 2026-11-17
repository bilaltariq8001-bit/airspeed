package com.velocity.speedometer.domain.util

import com.velocity.speedometer.domain.model.TrackPoint
import com.velocity.speedometer.domain.model.Trip
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object TripExporter {
    private val isoFormatter: DateTimeFormatter =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)

    fun toGpx(trip: Trip, points: List<TrackPoint>): String {
        val sb = StringBuilder()
        val startIso = isoFormatter.format(Instant.ofEpochMilli(trip.startTime))
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine(
            """<gpx version="1.1" creator="Velocity Speedometer" xmlns="http://www.topografix.com/GPX/1/1">"""
        )
        sb.appendLine("  <metadata>")
        sb.appendLine("    <name>Velocity Trip #${trip.id}</name>")
        sb.appendLine("    <time>$startIso</time>")
        sb.appendLine("  </metadata>")
        sb.appendLine("  <trk>")
        sb.appendLine("    <name>Velocity Trip #${trip.id}</name>")
        sb.appendLine("    <trkseg>")
        for (pt in points) {
            val ptTime = isoFormatter.format(Instant.ofEpochMilli(pt.timestamp))
            sb.appendLine(
                String.format(
                    Locale.US,
                    """      <trkpt lat="%.7f" lon="%.7f">""",
                    pt.lat,
                    pt.lng
                )
            )
            sb.appendLine(String.format(Locale.US, "        <ele>%.2f</ele>", pt.altitude))
            sb.appendLine("        <time>$ptTime</time>")
            sb.appendLine("        <extensions>")
            sb.appendLine(String.format(Locale.US, "          <speed_mps>%.3f</speed_mps>", pt.speedMps))
            sb.appendLine(String.format(Locale.US, "          <accuracy_m>%.2f</accuracy_m>", pt.accuracy))
            sb.appendLine(String.format(Locale.US, "          <bearing_deg>%.2f</bearing_deg>", pt.bearing))
            sb.appendLine("        </extensions>")
            sb.appendLine("      </trkpt>")
        }
        sb.appendLine("    </trkseg>")
        sb.appendLine("  </trk>")
        sb.appendLine("</gpx>")
        return sb.toString()
    }

    fun toCsv(trip: Trip, points: List<TrackPoint>): String {
        val sb = StringBuilder()
        sb.appendLine("trip_id,timestamp_iso,timestamp_ms,latitude,longitude,speed_mps,speed_kmh,altitude_m,accuracy_m,bearing_deg")
        for (pt in points) {
            val ptTime = isoFormatter.format(Instant.ofEpochMilli(pt.timestamp))
            val speedKmh = SpeedConverter.fromMps(pt.speedMps, com.velocity.speedometer.domain.model.SpeedUnit.KMH)
            sb.appendLine(
                String.format(
                    Locale.US,
                    "%d,%s,%d,%.7f,%.7f,%.3f,%.2f,%.2f,%.2f,%.2f",
                    trip.id,
                    ptTime,
                    pt.timestamp,
                    pt.lat,
                    pt.lng,
                    pt.speedMps,
                    speedKmh,
                    pt.altitude,
                    pt.accuracy,
                    pt.bearing
                )
            )
        }
        return sb.toString()
    }
}
