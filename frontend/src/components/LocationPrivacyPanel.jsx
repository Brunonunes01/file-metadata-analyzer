import { useState } from 'react'
import LocationPickerMap from './LocationPickerMap'

function LocationPrivacyPanel({
  loading,
  onRemove,
  onReplace,
  onChangeDate,
  onChangeAuthor,
  canUseGps,
  originalLatitude,
  originalLongitude,
  hasOriginalGps,
}) {
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [newDate, setNewDate] = useState('')
  const [author, setAuthor] = useState('')
  const [formError, setFormError] = useState('')

  const selectedLatitude = parseCoordinate(latitude)
  const selectedLongitude = parseCoordinate(longitude)
  const hasSelectedCoordinates = Number.isFinite(selectedLatitude) && Number.isFinite(selectedLongitude)

  const handleReplace = () => {
    if (!canUseGps) {
      setFormError('A alteração de GPS está disponível apenas para imagens JPEG ou PNG.')
      return
    }

    if (!latitude.trim() || !longitude.trim()) {
      setFormError('Informe latitude e longitude para substituir a localização.')
      return
    }

    setFormError('')
    onReplace(latitude.trim(), longitude.trim())
  }

  const handleDateChange = () => {
    if (!newDate.trim()) {
      setFormError('Informe uma data/hora para alterar os metadados.')
      return
    }

    setFormError('')
    onChangeDate(newDate.trim())
  }

  const handleAuthorChange = () => {
    if (!author.trim()) {
      setFormError('Informe um autor para alterar os metadados.')
      return
    }

    setFormError('')
    onChangeAuthor(author.trim())
  }

  const handleMapPick = (lat, lon) => {
    setLatitude(lat.toFixed(6))
    setLongitude(lon.toFixed(6))
    setFormError('')
  }

  const handleLatitudeChange = (event) => {
    setLatitude(event.target.value)
    setFormError('')
  }

  const handleLongitudeChange = (event) => {
    setLongitude(event.target.value)
    setFormError('')
  }

  const clearSelection = () => {
    setLatitude('')
    setLongitude('')
    setFormError('')
  }

  return (
    <section className="card location-privacy-card">
      <h2>Manipulação Controlada de Metadados</h2>
      <p className="muted">
        Altere metadados de forma controlada para simulação, anonimização e validação de cenários em
        testes de segurança autorizados.
      </p>
      <p className="muted small">
        Uso restrito a atividades autorizadas de assessment, privacidade e pentest.
      </p>

      <div className="spoof-grid">
        <section className="spoof-block">
          <h3>GPS</h3>
          {canUseGps ? (
            <>
              <p className="muted small">Clique no mapa para escolher a nova localização da imagem.</p>

              <div className="location-picker-layout">
                <div className="location-picker-map-panel">
                  <LocationPickerMap
                    originalLatitude={hasOriginalGps ? originalLatitude : null}
                    originalLongitude={hasOriginalGps ? originalLongitude : null}
                    selectedLatitude={selectedLatitude}
                    selectedLongitude={selectedLongitude}
                    onPickLocation={handleMapPick}
                  />
                  <div className="location-marker-legend">
                    <span className="legend-item">
                      <span className="legend-dot legend-dot-original"></span>
                      Localização atual
                    </span>
                    <span className="legend-item">
                      <span className="legend-dot legend-dot-selected"></span>
                      Nova localização
                    </span>
                  </div>
                  <p className="muted small selected-coordinates">
                    {hasSelectedCoordinates
                      ? `Selecionada: LAT ${selectedLatitude.toFixed(6)} | LON ${selectedLongitude.toFixed(6)}`
                      : 'Selecionada: nenhuma'}
                  </p>
                </div>

                <div className="location-picker-controls">
                  <div className="location-privacy-actions">
                    <button type="button" className="button secondary" onClick={onRemove} disabled={loading}>
                      {loading ? '[ PROCESSING... ]' : '[ REMOVE GPS ]'}
                    </button>
                  </div>

                  <div className="location-form">
                    <label htmlFor="gps-latitude">Latitude</label>
                    <input
                      id="gps-latitude"
                      type="number"
                      step="any"
                      placeholder="-23.550520"
                      value={latitude}
                      onChange={handleLatitudeChange}
                      disabled={loading}
                    />

                    <label htmlFor="gps-longitude">Longitude</label>
                    <input
                      id="gps-longitude"
                      type="number"
                      step="any"
                      placeholder="-46.633308"
                      value={longitude}
                      onChange={handleLongitudeChange}
                      disabled={loading}
                    />

                    <div className="location-form-buttons">
                      <button type="button" className="button secondary" onClick={clearSelection} disabled={loading}>
                        [ CLEAR NEW LOCATION ]
                      </button>
                      <button type="button" className="button primary" onClick={handleReplace} disabled={loading}>
                        {loading ? '[ PROCESSING... ]' : '[ APPLY NEW LOCATION ]'}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </>
          ) : (
            <p className="muted small">Ações de GPS disponíveis apenas para imagens JPEG e PNG.</p>
          )}
        </section>

        <section className="spoof-block">
          <h3>Alterar Data</h3>
          <p className="muted small">Suporta ISO ou formato yyyy-MM-dd HH:mm:ss.</p>
          <div className="spoof-inline-form">
            <input
              type="datetime-local"
              value={newDate}
              onChange={(event) => {
                setNewDate(event.target.value)
                setFormError('')
              }}
              disabled={loading}
            />
            <button type="button" className="button primary" onClick={handleDateChange} disabled={loading}>
              {loading ? '[ PROCESSING... ]' : '[ CHANGE DATE ]'}
            </button>
          </div>
        </section>

        <section className="spoof-block">
          <h3>Alterar Autor</h3>
          <div className="spoof-inline-form">
            <input
              type="text"
              placeholder="Nome do autor"
              value={author}
              onChange={(event) => {
                setAuthor(event.target.value)
                setFormError('')
              }}
              disabled={loading}
            />
            <button type="button" className="button primary" onClick={handleAuthorChange} disabled={loading}>
              {loading ? '[ PROCESSING... ]' : '[ CHANGE AUTHOR ]'}
            </button>
          </div>
        </section>
      </div>

      {formError && <p className="location-form-error">{formError}</p>}
    </section>
  )
}

function parseCoordinate(rawValue) {
  if (typeof rawValue !== 'string' || !rawValue.trim()) {
    return null
  }
  const value = Number(rawValue)
  return Number.isFinite(value) ? value : null
}

export default LocationPrivacyPanel
