package com.spartons.passengerapp.collection

import com.google.android.gms.maps.model.Marker
import java.util.*

object MarkerCollection {

    private val markers: MutableList<Marker> = LinkedList()

    fun insertMarker(marker: Marker) = apply {
        markers.add(marker)
    }

    fun getMarker(driverId: String): Marker? {
        for (marker in markers)
            if (marker.tag == driverId) return marker
        return null
    }

    fun clearMarkers() = apply {
        markers.clear()
    }

    fun removeMarker(driverId: String) {
        val marker = getMarker(driverId)
        marker?.remove()
        if (marker != null) markers.remove(marker)
    }

    fun allMarkers() = markers
}
