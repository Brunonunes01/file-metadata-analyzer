import { useRef } from 'react'
import AnalysisResult from '../components/AnalysisResult'
import LoadingState from '../components/LoadingState'
import UploadPanel from '../components/UploadPanel'
import { useMetaScan } from '../context/MetaScanContext'

function MetadataAnalyzerPage() {
  const fileInputRef = useRef(null)
  const { file, setFile, isLoading, error, setError, result, analyzeSelectedFile } = useMetaScan()

  const handleBrowseClick = () => {
    fileInputRef.current?.click()
  }

  const handleFileChange = (event) => {
    const selectedFile = event.target.files?.[0]
    if (selectedFile) {
      setFile(selectedFile)
      setError('')
    }
  }

  const handleDragOver = (event) => {
    event.preventDefault()
  }

  const handleDragLeave = (event) => {
    event.preventDefault()
  }

  const handleDrop = (event) => {
    event.preventDefault()
    const droppedFile = event.dataTransfer.files?.[0]
    if (droppedFile) {
      setFile(droppedFile)
      setError('')
    }
  }

  return (
    <section className="module-page">
      <header className="hero">
        <h2>Metadata Analyzer</h2>
        <p>Fluxo principal para upload, extração e leitura técnica dos metadados do arquivo.</p>
      </header>

      <input
        ref={fileInputRef}
        type="file"
        onChange={handleFileChange}
        className="hidden-file-input"
        aria-label="Selecionar arquivo para análise"
      />

      <UploadPanel
        file={file}
        loading={isLoading}
        dragActive={false}
        onBrowse={handleBrowseClick}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onAnalyze={analyzeSelectedFile}
      />

      {error && (
        <section className="card error-card" role="alert">
          <strong>Não foi possível concluir a análise.</strong>
          <p>{error}</p>
        </section>
      )}

      {isLoading && <LoadingState />}

      {result && (
        <section className="results-wrapper">
          <AnalysisResult data={result} variant="analyzer" />
        </section>
      )}
    </section>
  )
}

export default MetadataAnalyzerPage
