function UploadPanel({ file, loading, dragActive, onBrowse, onDrop, onDragOver, onDragLeave, onAnalyze }) {
  return (
    <section className="card upload-card">
      <h2>Enviar Arquivo</h2>
      <p className="muted">Arraste e solte um arquivo ou selecione manualmente para análise.</p>

      <div
        className={`dropzone ${dragActive ? 'active' : ''}`}
        onDrop={onDrop}
        onDragOver={onDragOver}
        onDragLeave={onDragLeave}
      >
        <p>{file ? file.name : 'Solte o arquivo aqui'}</p>
        <p className="muted small">Formatos diversos suportados pelo backend MetaScan</p>
      </div>

      <div className="upload-actions">
        <button type="button" className="button secondary" onClick={onBrowse} disabled={loading}>
          Selecionar Arquivo
        </button>
        <button type="button" className="button primary" onClick={onAnalyze} disabled={loading}>
          {loading ? 'Analisando...' : 'Enviar e Analisar'}
        </button>
      </div>
    </section>
  )
}

export default UploadPanel
