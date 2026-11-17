package com.velocity.speedometer.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.velocity.speedometer.BuildConfig
import com.velocity.speedometer.R
import com.velocity.speedometer.domain.model.TrackPoint
import com.velocity.speedometer.ui.theme.LocalVelocityColors
import com.velocity.speedometer.ui.theme.VelocityCyan
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker as OsmMarker
import org.osmdroid.views.overlay.Polyline as OsmPolyline

@Composable
fun TripRouteMap(
    points: List<TrackPoint>,
    modifier: Modifier = Modifier
) {
    val velocityColors = LocalVelocityColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, velocityColors.divider)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.detail_route_map),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
            )

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.detail_no_points),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    if (BuildConfig.MAPS_API_KEY.isNotBlank()) {
                        GoogleMapsRouteContent(
                            points = points,
                            isDark = velocityColors.isDark
                        )
                    } else {
                        OsmDroidRouteContent(points = points)
                    }
                }

                if (BuildConfig.MAPS_API_KEY.isBlank()) {
                    Text(
                        text = stringResource(R.string.detail_map_fallback_notice),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleMapsRouteContent(
    points: List<TrackPoint>,
    isDark: Boolean
) {
    val latLngs = remember(points) {
        points.map { LatLng(it.lat, it.lng) }
    }
    val startPoint = latLngs.first()
    val endPoint = latLngs.last()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startPoint, 14f)
    }

    LaunchedEffect(latLngs) {
        if (latLngs.size >= 2) {
            val boundsBuilder = LatLngBounds.builder()
            latLngs.forEach { boundsBuilder.include(it) }
            runCatching {
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 64)
                )
            }
        } else {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(startPoint, 15f))
        }
    }

    val startLabel = stringResource(R.string.detail_start_marker)
    val endLabel = stringResource(R.string.detail_end_marker)

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isTrafficEnabled = false),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            mapToolbarEnabled = false
        ),
        mapColorScheme = if (isDark) ComposeMapColorScheme.DARK else ComposeMapColorScheme.LIGHT
    ) {
        Polyline(
            points = latLngs,
            color = VelocityCyan,
            width = 10f,
            geodesic = true
        )
        Marker(
            state = remember(startPoint) { MarkerState(position = startPoint) },
            title = startLabel
        )
        if (latLngs.size > 1) {
            Marker(
                state = remember(endPoint) { MarkerState(position = endPoint) },
                title = endLabel
            )
        }
    }
}

@Composable
private fun OsmDroidRouteContent(
    points: List<TrackPoint>
) {
    val context = LocalContext.current
    val geoPoints = remember(points) {
        points.map { GeoPoint(it.lat, it.lng) }
    }
    val startTitle = stringResource(R.string.detail_start_marker)
    val endTitle = stringResource(R.string.detail_end_marker)

    val mapView = remember(context) {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView },
        update = { view ->
            view.overlays.clear()

            val polyline = OsmPolyline(view).apply {
                setPoints(geoPoints)
                outlinePaint.color = AndroidColor.parseColor("#00E5FF")
                outlinePaint.strokeWidth = 9f
            }
            view.overlays.add(polyline)

            if (geoPoints.isNotEmpty()) {
                val startMarker = OsmMarker(view).apply {
                    position = geoPoints.first()
                    title = startTitle
                    setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                }
                view.overlays.add(startMarker)

                if (geoPoints.size > 1) {
                    val endMarker = OsmMarker(view).apply {
                        position = geoPoints.last()
                        title = endTitle
                        setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                    }
                    view.overlays.add(endMarker)
                }

                view.post {
                    if (geoPoints.size >= 2) {
                        val box = BoundingBox.fromGeoPointsSafe(geoPoints)
                        view.zoomToBoundingBox(box, false, 64)
                    } else {
                        view.controller.setZoom(15.0)
                        view.controller.setCenter(geoPoints.first())
                    }
                }
            }
            view.invalidate()
        }
    )
}
