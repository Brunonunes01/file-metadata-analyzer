import { Link } from 'react-router-dom'
import AnalysisResult from '../components/AnalysisResult'
import { useMetaScan } from '../context/MetaScanContext'

function ImageForensicsPage() {
  const { result, hasAnalysis } = useMetaScan()

  return (
    <section className="module-page">
      <header className="hero">
        <h2>Image Forensics</h2>
        <p>
          Módulo dedicado à análise de captura em imagens: localização, device forensics, mapa e
          indicadores de origem da mídia.
        </p>
      </header>

      {!hasAnalysis ? (
        <section className="card">
          <h2>Análise necessária</h2>
          <p className="muted">Realize uma análise primeiro para habilitar os dados de forense de imagem.</p>
          <Link className="button secondary" to="/analyzer">
            [ GO TO ANALYZER ]
          </Link>
        </section>
      ) : (
        <section className="results-wrapper">
          <AnalysisResult data={result} variant="forensics" />
        </section>
      )}
    </section>
  )
}

export default ImageForensicsPage
