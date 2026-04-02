function AntivirusStatus({ status }) {
  if (typeof status !== 'string' || status.trim() === '') {
    return null
  }

  const normalized = status.toLowerCase().trim()
  const view = getStatusView(normalized)

  return (
    <section className={`antivirus-terminal ${view.tone}`} aria-live="polite">
      <p className="antivirus-terminal-title">{view.title}</p>
      <p className="antivirus-terminal-description">{view.description}</p>
    </section>
  )
}

function getStatusView(status) {
  if (status === 'clean') {
    return {
      title: 'ANTIVIRUS: CLEAN',
      description: 'Arquivo verificado e considerado seguro',
      tone: 'clean'
    }
  }

  if (status === 'infected') {
    return {
      title: 'ANTIVIRUS: BLOCKED',
      description: 'Ameaça detectada — análise interrompida',
      tone: 'infected'
    }
  }

  return {
    title: 'ANTIVIRUS: ERROR',
    description: 'Falha na verificação de segurança',
    tone: 'error'
  }
}

export default AntivirusStatus
