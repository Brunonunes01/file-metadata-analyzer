import L from 'leaflet'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})

export const originalLocationIcon = L.divIcon({
  className: 'location-marker location-marker-original',
  html: '<span></span>',
  iconSize: [18, 18],
  iconAnchor: [9, 9],
})

export const selectedLocationIcon = L.divIcon({
  className: 'location-marker location-marker-selected',
  html: '<span></span>',
  iconSize: [18, 18],
  iconAnchor: [9, 9],
})
