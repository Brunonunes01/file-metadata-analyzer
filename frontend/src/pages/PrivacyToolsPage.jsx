import { Link } from 'react-router-dom'
import LocationPrivacyPanel from '../components/LocationPrivacyPanel'
import { useMetaScan } from '../context/MetaScanContext'

function PrivacyToolsPage() {
  const {
    result,
    hasAnalysis,
    error,
    locationActionMessage,
    isSpoofingMetadata,
    removeLocation,
    replaceLocation,
    changeDate,
    changeAuthor,
    canCleanMetadata,
  } = useMetaScan()

  return (
    <section className="module-page">
      <header className="hero">
        <h2>Privacy Tools</h2>
        <p>
          Manipulação controlada de metadados para simulação, anonimização e validação em cenários
          autorizados de segurança.
        </p>
      </header>

      {!hasAnalysis ? (
        <section className="card">
          <h2>Análise necessária</h2>
          <p className="muted">Envie e analise um arquivo antes de aplicar ações de privacidade.</p>
          <Link className="button secondary" to="/analyzer">
            [ GO TO ANALYZER ]
          </Link>
        </section>
      ) : (
        <>
          {error && (
            <section className="card error-card" role="alert">
              <strong>Operação não concluída.</strong>
              <p>{error}</p>
            </section>
          )}

          {locationActionMessage && (
            <section className="card success-card" role="status">
              <strong>Operação concluída.</strong>
              <p>{locationActionMessage}</p>
            </section>
          )}

          <LocationPrivacyPanel
            loading={isSpoofingMetadata}
            onRemove={removeLocation}
            onReplace={replaceLocation}
            onChangeDate={changeDate}
            onChangeAuthor={changeAuthor}
            canUseGps={canCleanMetadata}
            hasOriginalGps={Boolean(result?.location?.hasGps)}
            originalLatitude={result?.location?.latitudeDecimal}
            originalLongitude={result?.location?.longitudeDecimal}
          />
        </>
      )}
    </section>
  )
}

export default PrivacyToolsPage
