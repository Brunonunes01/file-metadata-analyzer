import UsernameScanPanel from '../components/UsernameScanPanel'
import { useMetaScan } from '../context/MetaScanContext'

function OsintToolsPage() {
  const { username, setUsername, isUsernameScanning, usernameScanResult, runUsernameScan } = useMetaScan()

  return (
    <section className="module-page">
      <header className="hero">
        <h2>OSINT Tools</h2>
        <p>
          Ambiente de investigação de fontes abertas com base modular para integrações futuras como
          Sherlock, Maigret avançado e Domain OSINT.
        </p>
      </header>

      <UsernameScanPanel
        username={username}
        loading={isUsernameScanning}
        result={usernameScanResult}
        onUsernameChange={setUsername}
        onScan={runUsernameScan}
      />

      <section className="osint-placeholder-grid">
        <article className="card placeholder-card">
          <h3>Sherlock Module</h3>
          <p className="muted">Módulo em preparação.</p>
        </article>
        <article className="card placeholder-card">
          <h3>Domain OSINT</h3>
          <p className="muted">Módulo em preparação.</p>
        </article>
        <article className="card placeholder-card">
          <h3>Threat Intel Feeds</h3>
          <p className="muted">Módulo em preparação.</p>
        </article>
      </section>
    </section>
  )
}

export default OsintToolsPage
