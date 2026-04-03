function UsernameScanPanel({ username, loading, result, onUsernameChange, onScan }) {
  const profiles = Array.isArray(result?.profiles) ? result.profiles : []

  return (
    <section className="card username-scan-card">
      <h2>OSINT Username Scan</h2>
      <p className="muted">
        Enumera perfis publicos a partir de um username usando Maigret no backend.
      </p>

      <div className="username-scan-form">
        <input
          type="text"
          value={username}
          placeholder="ex: john_doe"
          onChange={(event) => onUsernameChange(event.target.value)}
          disabled={loading}
        />
        <button type="button" className="button primary" onClick={onScan} disabled={loading}>
          {loading ? '[ SCANNING... ]' : '[ RUN USERNAME SCAN ]'}
        </button>
      </div>

      {result && (
        <div className="username-scan-results">
          <p className="muted small">
            Status: <strong>{result.scanStatus || 'unknown'}</strong> | Perfis encontrados:{' '}
            <strong>{result.totalProfilesFound ?? 0}</strong>
          </p>

          {profiles.length === 0 ? (
            <p className="muted small">Nenhum perfil encontrado para este username.</p>
          ) : (
            <div className="username-table-wrapper">
              <table className="username-table">
                <thead>
                  <tr>
                    <th>Plataforma</th>
                    <th>Perfil</th>
                  </tr>
                </thead>
                <tbody>
                  {profiles.map((profile) => (
                    <tr key={`${profile.platform}-${profile.profileUrl}`}>
                      <td>{profile.platform}</td>
                      <td>
                        <a href={profile.profileUrl} target="_blank" rel="noopener noreferrer">
                          {profile.profileUrl}
                        </a>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </section>
  )
}

export default UsernameScanPanel
