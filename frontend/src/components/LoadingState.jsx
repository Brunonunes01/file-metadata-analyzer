function LoadingState() {
  return (
    <section className="card loading-card" aria-live="polite">
      <div className="spinner" aria-hidden="true"></div>
      <div>
        <h3>Analisando metadados...</h3>
        <p className="muted">Aguarde alguns segundos enquanto processamos seu arquivo.</p>
      </div>
    </section>
  )
}

export default LoadingState
