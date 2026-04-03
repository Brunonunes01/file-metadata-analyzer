import { Link } from 'react-router-dom'
import { useMetaScan } from '../context/MetaScanContext'

function ReportsPage() {
  const { hasAnalysis, result, exportJson, exportTxt, exportPdf } = useMetaScan()

  return (
    <section className="module-page">
      <header className="hero">
        <h2>Reports & Exportações</h2>
        <p>Central de exportação de resultados analíticos para compartilhamento e documentação técnica.</p>
      </header>

      {!hasAnalysis ? (
        <section className="card">
          <h2>Nenhum resultado disponível</h2>
          <p className="muted">As exportações dependem de uma análise concluída no módulo Analyzer.</p>
          <Link className="button secondary" to="/analyzer">
            [ GO TO ANALYZER ]
          </Link>
        </section>
      ) : (
        <>
          <section className="card">
            <h2>Resultado carregado</h2>
            <dl className="info-grid">
              <InfoRow label="Arquivo" value={result?.fileName} />
              <InfoRow label="Tipo" value={result?.contentTypeDetectado} />
              <InfoRow label="Hash" value={result?.hashSha256} />
            </dl>
          </section>

          <section className="card export-card">
            <div className="export-actions export-actions-left">
              <button type="button" className="button secondary export-button" onClick={exportTxt}>
                [ EXPORT TXT ]
              </button>
              <button type="button" className="button secondary export-button" onClick={exportPdf}>
                [ EXPORT PDF ]
              </button>
              <button type="button" className="button primary export-button" onClick={exportJson}>
                [ EXPORT JSON ]
              </button>
            </div>
          </section>
        </>
      )}
    </section>
  )
}

function InfoRow({ label, value }) {
  return (
    <>
      <dt>{label}</dt>
      <dd className={label === 'Hash' ? 'hash-value' : ''}>{value || '-'}</dd>
    </>
  )
}

export default ReportsPage
