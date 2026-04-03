import { Link } from 'react-router-dom'
import { useMetaScan } from '../context/MetaScanContext'

const modules = [
  {
    to: '/analyzer',
    title: 'Metadata Analyzer',
    description: 'Upload, análise de metadados técnicos, insights, segurança e privacidade.',
  },
  {
    to: '/image-forensics',
    title: 'Image Forensics',
    description: 'Localização GPS, mapa, device forensics e informações de captura para imagens.',
  },
  {
    to: '/privacy',
    title: 'Privacy Tools',
    description: 'Manipulação controlada de metadados para cenários de assessment autorizados.',
  },
  {
    to: '/osint',
    title: 'OSINT Tools',
    description: 'Módulos de investigação aberta, incluindo username scan e expansão futura.',
  },
  {
    to: '/reports',
    title: 'Reports',
    description: 'Exportação consolidada em JSON, TXT e PDF com base no resultado da análise.',
  },
]

function DashboardPage() {
  const { hasAnalysis, result } = useMetaScan()

  return (
    <section className="module-page">
      <header className="hero">
        <h2>Dashboard</h2>
        <p>
          Plataforma modular para triagem de metadados, forense de imagem, privacidade operacional e
          inteligência OSINT.
        </p>
      </header>

      <section className="card">
        <h2>Status Operacional</h2>
        <dl className="info-grid">
          <InfoRow label="Última análise" value={hasAnalysis ? result?.fileName : 'Nenhuma análise no momento'} />
          <InfoRow label="Tipo detectado" value={hasAnalysis ? result?.contentTypeDetectado : '-'} />
          <InfoRow label="Módulos ativos" value="Analyzer, Forensics, Privacy, OSINT e Reports" />
        </dl>
      </section>

      <section className="module-grid">
        {modules.map((module) => (
          <article className="card module-card" key={module.to}>
            <h3>{module.title}</h3>
            <p className="muted">{module.description}</p>
            <Link className="button secondary module-link" to={module.to}>
              [ OPEN MODULE ]
            </Link>
          </article>
        ))}
      </section>
    </section>
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

export default DashboardPage
