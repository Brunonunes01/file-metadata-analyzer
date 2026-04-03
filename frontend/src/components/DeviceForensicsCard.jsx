function DeviceForensicsCard({ deviceForensics }) {
  const detected = deviceForensics?.deviceDetected === true

  return (
    <section className="card">
      <h2>Device Forensics</h2>
      {detected ? (
        <dl className="info-grid">
          <InfoRow label="Marca" value={deviceForensics?.brand} />
          <InfoRow label="Modelo" value={deviceForensics?.model} />
          <InfoRow label="Código do modelo" value={deviceForensics?.modelCode} />
          <InfoRow label="Tipo de câmera" value={deviceForensics?.cameraType} />
          <InfoRow label="Abertura" value={deviceForensics?.aperture} />
          <InfoRow label="ISO" value={deviceForensics?.iso} />
          <InfoRow label="Velocidade" value={deviceForensics?.shutterSpeed} />
          <InfoRow label="Distância focal" value={deviceForensics?.focalLength} />
          <InfoRow label="Resolução" value={deviceForensics?.imageSize} />
          <InfoRow label="Megapixels" value={deviceForensics?.megapixels} />
        </dl>
      ) : (
        <p className="muted">Não foi possível identificar o dispositivo de captura.</p>
      )}
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

export default DeviceForensicsCard
