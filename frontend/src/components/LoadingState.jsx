function LoadingState() {
  return (
    <section className="card loading-card" aria-live="polite">
      <div className="spinner" aria-hidden="true"></div>
      <div>
        <h3>Processing metadata<span className="cursor" aria-hidden="true"></span></h3>
        <p className="muted">Aguarde enquanto o motor OSINT processa seu arquivo.</p>
      </div>
    </section>
  )
}

export default LoadingState
