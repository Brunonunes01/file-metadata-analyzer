import MetadataTabs from './MetadataTabs'

function AnalysisResult({ data }) {
  const summary = data?.summary || {}
  const security = data?.security || {}
  const privacyRisk = data?.privacyRisk
  const hasPrivacyRisk = Boolean(privacyRisk && typeof privacyRisk === 'object')
  const location = data?.location
  const hasLocationData = Boolean(location && typeof location === 'object')
  const insights = Array.isArray(data?.insights) ? data.insights : []

  return (
    <div className="results-grid">
      <section className="card">
        <h2>Resumo do Arquivo</h2>
        <dl className="info-grid">
          <InfoRow label="Nome" value={data?.fileName} />
          <InfoRow label="Tamanho" value={formatFileSize(data?.fileSize)} />
          <InfoRow label="Tipo Detectado" value={data?.contentTypeDetectado} />
          <InfoRow label="Tipo Amigável" value={data?.fileType} />
          <InfoRow label="Hash SHA-256" value={data?.hashSha256} />
        </dl>
      </section>

      {hasPrivacyRisk && (
        <section className="card privacy-risk-card">
          <h2>Risco e Privacidade</h2>
          <div className="risk-header">
            <span className={`risk-badge ${privacyRisk.level || 'low'}`}>
              {formatRiskLevel(privacyRisk.level)}
            </span>
            <span className="risk-score">Score: {privacyRisk.score ?? 0}</span>
          </div>

          <h3 className="risk-subtitle">Motivos</h3>
          {Array.isArray(privacyRisk.reasons) && privacyRisk.reasons.length > 0 ? (
            <ul className="risk-list">
              {privacyRisk.reasons.map((reason, index) => (
                <li key={`${index}-${reason}`}>{reason}</li>
              ))}
            </ul>
          ) : (
            <p className="muted">Nenhum motivo sensível detectado.</p>
          )}

          <h3 className="risk-subtitle">Dados sensíveis encontrados</h3>
          {Array.isArray(privacyRisk.sensitiveDataFound) && privacyRisk.sensitiveDataFound.length > 0 ? (
            <ul className="sensitive-tags">
              {privacyRisk.sensitiveDataFound.map((item, index) => (
                <li key={`${index}-${item}`}>{item}</li>
              ))}
            </ul>
          ) : (
            <p className="muted">Nenhum dado sensível identificado.</p>
          )}
        </section>
      )}

      <section className="card">
        <h2>Insights</h2>
        {insights.length === 0 ? (
          <p className="muted">Nenhum insight retornado.</p>
        ) : (
          <ul className="insights-list">
            {insights.map((item, index) => (
              <li key={`${index}-${String(item)}`}>
                {typeof item === 'string' ? item : JSON.stringify(item)}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="card">
        <h2>Resumo Amigável</h2>
        <dl className="info-grid">
          <InfoRow label="Author" value={summary.author} />
          <InfoRow label="Last Author" value={summary.lastAuthor} />
          <InfoRow label="Created At" value={summary.createdAt} />
          <InfoRow label="Last Modified" value={summary.lastModified} />
          <InfoRow label="Revision" value={summary.revision} />
          <InfoRow label="Title" value={summary.title} />
          <InfoRow label="Subject" value={summary.subject} />
          <InfoRow label="Description" value={summary.description} />
          <InfoRow label="Language" value={summary.language} />
        </dl>
      </section>

      <section className="card">
        <h2>Segurança</h2>
        <dl className="info-grid">
          <InfoRow label="hasMetadata" value={formatBoolean(security.hasMetadata)} />
          <InfoRow label="hasText" value={formatBoolean(security.hasText)} />
          <InfoRow label="hasAuthor" value={formatBoolean(security.hasAuthor)} />
          <InfoRow label="hasCreationDate" value={formatBoolean(security.hasCreationDate)} />
        </dl>
      </section>

      {hasLocationData && (
        <section className="card location-card">
          <h2>Localização</h2>
          {location.hasGps ? (
            <>
              <dl className="info-grid">
                <InfoRow label="Latitude" value={formatCoordinate(location.latitudeDecimal)} />
                <InfoRow label="Longitude" value={formatCoordinate(location.longitudeDecimal)} />
                <InfoRow label="Posição Original" value={location.gpsPositionOriginal} />
                {location.gpsDateTime && <InfoRow label="Data/Hora GPS" value={location.gpsDateTime} />}
                {location.gpsAltitude && <InfoRow label="Altitude" value={location.gpsAltitude} />}
              </dl>
              {location.mapsUrl && (
                <a
                  className="map-link"
                  href={location.mapsUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  📍 Abrir no Google Maps
                </a>
              )}
            </>
          ) : (
            <p className="muted">Nenhuma localização GPS encontrada neste arquivo</p>
          )}
        </section>
      )}

      <section className="card full-width">
        <h2>Texto Extraído</h2>
        <div className="text-preview">{data?.textPreview || 'Nenhum texto extraído para exibir.'}</div>
        <p className="muted small">Comprimento do texto: {data?.textLength ?? 0}</p>
      </section>

      <section className="card full-width">
        <h2>Diagnóstico da Extração</h2>
        <dl className="info-grid">
          <InfoRow label="ExifTool Status" value={data?.exiftoolStatus} />
          <InfoRow label="Quantidade de Metadados" value={data?.metadataCount} />
        </dl>
      </section>

      <div className="full-width">
        <MetadataTabs
          tikaMetadata={data?.tikaMetadata}
          exiftoolMetadata={data?.exiftoolMetadata}
          mergedMetadata={data?.mergedMetadata}
        />
      </div>
    </div>
  )
}

function InfoRow({ label, value }) {
  return (
    <>
      <dt>{label}</dt>
      <dd>{value || '-'}</dd>
    </>
  )
}

function formatFileSize(bytes) {
  if (typeof bytes !== 'number' || Number.isNaN(bytes)) {
    return '-'
  }

  if (bytes === 0) {
    return '0 B'
  }

  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const exponent = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  const value = bytes / 1024 ** exponent

  return `${value.toFixed(value >= 10 || exponent === 0 ? 0 : 1)} ${units[exponent]}`
}

function formatBoolean(value) {
  if (typeof value !== 'boolean') {
    return '-'
  }

  return value ? 'Sim' : 'Não'
}

function formatCoordinate(value) {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return '-'
  }
  return value.toFixed(6)
}

function formatRiskLevel(level) {
  if (level === 'high') {
    return 'Alto'
  }
  if (level === 'medium') {
    return 'Médio'
  }
  return 'Baixo'
}

export default AnalysisResult
