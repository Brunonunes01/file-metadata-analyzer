import { MapContainer, Marker, Popup, TileLayer } from 'react-leaflet'
import './leafletIcons'

function LocationMap({ latitude, longitude, gpsPositionOriginal }) {
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
    return (
      <div className="map-empty muted">
        Nenhuma localização GPS disponível para exibir no mapa.
      </div>
    )
  }

  const position = [latitude, longitude]

  return (
    <div className="location-map-wrapper">
      <MapContainer center={position} zoom={13} scrollWheelZoom className="location-map">
        <TileLayer
          attribution='&copy; OpenStreetMap contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <Marker position={position}>
          <Popup>
            <div className="map-popup">
              <div>Latitude: {latitude.toFixed(6)}</div>
              <div>Longitude: {longitude.toFixed(6)}</div>
              {gpsPositionOriginal && <div>Original: {gpsPositionOriginal}</div>}
            </div>
          </Popup>
        </Marker>
      </MapContainer>
    </div>
  )
}

export default LocationMap
