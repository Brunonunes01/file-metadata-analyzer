function UploadPanel({ file, loading, dragActive, onBrowse, onDrop, onDragOver, onDragLeave, onAnalyze }) {
  return (
    <section className="card upload-card">
      <h2>Input</h2>
      <p className="muted">Envie um arquivo para iniciar a varredura de metadados.</p>

      <div
        className={`dropzone ${dragActive ? 'active' : ''}`}
        onDrop={onDrop}
        onDragOver={onDragOver}
        onDragLeave={onDragLeave}
        onClick={onBrowse}
        onKeyDown={(event) => {
          if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault()
            onBrowse()
          }
        }}
        role="button"
        tabIndex={0}
      >
        <p className="dropzone-title">{file ? file.name : '> DROP FILE OR CLICK TO UPLOAD'}</p>
        <p className="muted small">Supported by MetaScan backend parsers</p>
      </div>

      <div className="upload-actions">
        <button type="button" className="button secondary" onClick={onBrowse} disabled={loading}>
          [ SELECT FILE ]
        </button>
        <button type="button" className="button primary" onClick={onAnalyze} disabled={loading}>
          {loading ? '[ ANALYZING... ]' : '[ RUN ANALYSIS ]'}
        </button>
      </div>
    </section>
  )
}

export default UploadPanel
