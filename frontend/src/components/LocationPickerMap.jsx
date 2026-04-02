import { useEffect } from 'react'
import { MapContainer, Marker, Popup, TileLayer, useMap, useMapEvents } from 'react-leaflet'
import { originalLocationIcon, selectedLocationIcon } from './leafletIcons'

const GLOBAL_CENTER = [20, 0]
const GLOBAL_ZOOM = 2
const LOCAL_ZOOM = 13

function LocationPickerMap({
  originalLatitude,
  originalLongitude,
  selectedLatitude,
  selectedLongitude,
  onPickLocation,
}) {
  const hasOriginal = Number.isFinite(originalLatitude) && Number.isFinite(originalLongitude)
  const hasSelected = Number.isFinite(selectedLatitude) && Number.isFinite(selectedLongitude)

  const center = hasSelected
    ? [selectedLatitude, selectedLongitude]
    : hasOriginal
      ? [originalLatitude, originalLongitude]
      : GLOBAL_CENTER
  const zoom = hasSelected || hasOriginal ? LOCAL_ZOOM : GLOBAL_ZOOM

  return (
    <div className="location-map-wrapper">
      <MapContainer center={center} zoom={zoom} scrollWheelZoom className="location-map picker-map">
        <TileLayer
          attribution='&copy; OpenStreetMap contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <MapClickHandler onPickLocation={onPickLocation} />
        <MapViewportSync center={center} zoom={zoom} />

        {hasOriginal && (
          <Marker position={[originalLatitude, originalLongitude]} icon={originalLocationIcon}>
            <Popup>
              <div className="map-popup">
                <div>Localização atual</div>
                <div>Latitude: {originalLatitude.toFixed(6)}</div>
                <div>Longitude: {originalLongitude.toFixed(6)}</div>
              </div>
            </Popup>
          </Marker>
        )}

        {hasSelected && (
          <Marker position={[selectedLatitude, selectedLongitude]} icon={selectedLocationIcon}>
            <Popup>
              <div className="map-popup">
                <div>Nova localização</div>
                <div>Latitude: {selectedLatitude.toFixed(6)}</div>
                <div>Longitude: {selectedLongitude.toFixed(6)}</div>
              </div>
            </Popup>
          </Marker>
        )}
      </MapContainer>
    </div>
  )
}

function MapClickHandler({ onPickLocation }) {
  useMapEvents({
    click(event) {
      onPickLocation(event.latlng.lat, event.latlng.lng)
    },
  })
  return null
}

function MapViewportSync({ center, zoom }) {
  const map = useMap()

  useEffect(() => {
    map.flyTo(center, zoom, { animate: true, duration: 0.4 })
  }, [center, zoom, map])

  return null
}

export default LocationPickerMap
