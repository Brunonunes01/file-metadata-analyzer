import { useRef, useState } from 'react'
import AnalysisResult from './components/AnalysisResult'
import LoadingState from './components/LoadingState'
import UploadPanel from './components/UploadPanel'
import { extractMetadata } from './services/metadataService'
import './App.css'

function App() {
  const [file, setFile] = useState(null)
  const [isLoading, setIsLoading] = useState(false)
  const [dragActive, setDragActive] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState(null)
  const fileInputRef = useRef(null)

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
    setDragActive(true)
  }

  const handleDragLeave = (event) => {
    event.preventDefault()
    setDragActive(false)
  }

  const handleDrop = (event) => {
    event.preventDefault()
    setDragActive(false)

    const droppedFile = event.dataTransfer.files?.[0]

    if (droppedFile) {
      setFile(droppedFile)
      setError('')
    }
  }

  const handleAnalyze = async () => {
    if (!file) {
      setError('Selecione um arquivo antes de iniciar a análise.')
      return
    }

    setIsLoading(true)
    setError('')
    setResult(null)

    try {
      const response = await extractMetadata(file)
      setResult(response)
    } catch (requestError) {
      setError(requestError.message || 'Não foi possível analisar o arquivo. Tente novamente.')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <main className="app-shell">
      <header className="hero">
        <span className="hero-tag">Análise Inteligente de Metadados</span>
        <h1>MetaScan</h1>
        <p>
          Envie um arquivo para extrair informações técnicas, resumo amigável, insights e indicadores de
          segurança em uma única tela.
        </p>
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
        dragActive={dragActive}
        onBrowse={handleBrowseClick}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onAnalyze={handleAnalyze}
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
          <AnalysisResult data={result} />
        </section>
      )}
    </main>
  )
}

export default App
