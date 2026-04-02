import { useRef, useState } from 'react'
import AnalysisResult from './components/AnalysisResult'
import LoadingState from './components/LoadingState'
import LocationPrivacyPanel from './components/LocationPrivacyPanel'
import UploadPanel from './components/UploadPanel'
import { cleanMetadata, extractMetadata, spoofMetadata } from './services/metadataService'
import { exportAnalysisAsJson } from './utils/exportAnalysisJson'
import { exportAnalysisAsTxt } from './utils/exportAnalysisTxt'
import { exportAnalysisAsPdf } from './utils/exportAnalysisPdf'
import { triggerDownloadFromBlob } from './utils/exportUtils'
import './App.css'

function App() {
  const [file, setFile] = useState(null)
  const [isLoading, setIsLoading] = useState(false)
  const [isCleaningMetadata, setIsCleaningMetadata] = useState(false)
  const [isSpoofingMetadata, setIsSpoofingMetadata] = useState(false)
  const [dragActive, setDragActive] = useState(false)
  const [error, setError] = useState('')
  const [locationActionMessage, setLocationActionMessage] = useState('')
  const [result, setResult] = useState(null)
  const [analyzedFile, setAnalyzedFile] = useState(null)
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
      setAnalyzedFile(file)
      setLocationActionMessage('')
    } catch (requestError) {
      setAnalyzedFile(null)
      setError(requestError.message || 'Não foi possível analisar o arquivo. Tente novamente.')
      setLocationActionMessage('')
    } finally {
      setIsLoading(false)
    }
  }

  const handleExportJson = () => {
    exportAnalysisAsJson(result)
  }

  const handleExportTxt = () => {
    exportAnalysisAsTxt(result)
  }

  const handleExportPdf = () => {
    exportAnalysisAsPdf(result)
  }

  const handleCleanMetadata = async () => {
    if (!analyzedFile) {
      setError('Selecione e analise uma imagem antes de remover metadados.')
      return
    }

    setIsCleaningMetadata(true)
    setError('')

    try {
      const cleanedFile = await cleanMetadata(analyzedFile)
      triggerDownloadFromBlob(cleanedFile.blob, cleanedFile.fileName)
    } catch (requestError) {
      setError(requestError.message || 'Não foi possível limpar os metadados da imagem.')
    } finally {
      setIsCleaningMetadata(false)
    }
  }

  const handleRemoveLocation = async (cleanupMode) => {
    if (!analyzedFile) {
      setError('Selecione e analise uma imagem antes de remover a localização GPS.')
      return
    }

    setIsSpoofingMetadata(true)
    setError('')
    setLocationActionMessage('')

    try {
      const spoofedFile = await spoofMetadata(analyzedFile, { action: 'remove_gps', cleanupMode })
      triggerDownloadFromBlob(spoofedFile.blob, spoofedFile.fileName)
      setLocationActionMessage('Localização GPS removida com sucesso. O download da nova cópia foi iniciado.')
    } catch (requestError) {
      setError(requestError.message || 'Não foi possível remover a localização GPS da imagem.')
    } finally {
      setIsSpoofingMetadata(false)
    }
  }

  const handleReplaceLocation = async (latitude, longitude, cleanupMode) => {
    if (!analyzedFile) {
      setError('Selecione e analise uma imagem antes de substituir a localização GPS.')
      return
    }

    setIsSpoofingMetadata(true)
    setError('')
    setLocationActionMessage('')

    try {
      const spoofedFile = await spoofMetadata(analyzedFile, {
        action: 'replace_gps',
        latitude,
        longitude,
        cleanupMode,
      })
      triggerDownloadFromBlob(spoofedFile.blob, spoofedFile.fileName)
      setLocationActionMessage('Localização GPS substituída com sucesso. O download da nova cópia foi iniciado.')
    } catch (requestError) {
      setError(requestError.message || 'Não foi possível substituir a localização GPS da imagem.')
    } finally {
      setIsSpoofingMetadata(false)
    }
  }

  const handleChangeDate = async (newDate, cleanupMode) => {
    if (!analyzedFile) {
      setError('Selecione e analise um arquivo antes de alterar a data dos metadados.')
      return
    }

    setIsSpoofingMetadata(true)
    setError('')
    setLocationActionMessage('')

    try {
      const spoofedFile = await spoofMetadata(analyzedFile, {
        action: 'change_date',
        newDate,
        cleanupMode,
      })
      triggerDownloadFromBlob(spoofedFile.blob, spoofedFile.fileName)
      setLocationActionMessage('Data dos metadados alterada com sucesso. O download da nova cópia foi iniciado.')
    } catch (requestError) {
      setError(requestError.message || 'Não foi possível alterar a data dos metadados.')
    } finally {
      setIsSpoofingMetadata(false)
    }
  }

  const handleChangeAuthor = async (author, cleanupMode) => {
    if (!analyzedFile) {
      setError('Selecione e analise um arquivo antes de alterar o autor dos metadados.')
      return
    }

    setIsSpoofingMetadata(true)
    setError('')
    setLocationActionMessage('')

    try {
      const spoofedFile = await spoofMetadata(analyzedFile, {
        action: 'change_author',
        author,
        cleanupMode,
      })
      triggerDownloadFromBlob(spoofedFile.blob, spoofedFile.fileName)
      setLocationActionMessage('Autor dos metadados alterado com sucesso. O download da nova cópia foi iniciado.')
    } catch (requestError) {
      setError(requestError.message || 'Não foi possível alterar o autor dos metadados.')
    } finally {
      setIsSpoofingMetadata(false)
    }
  }

  const canCleanMetadata =
    result &&
    analyzedFile &&
    (result.contentTypeDetectado === 'image/jpeg' || result.contentTypeDetectado === 'image/png')
  const canSpoofMetadata = Boolean(result && analyzedFile)

  return (
    <main className="app-shell">
      <header className="hero">
        <span className="hero-tag">[ SYSTEM ONLINE ]</span>
        <h1>MetaScan</h1>
        <p className="hero-subtitle">OSINT Metadata Analyzer</p>
        <p>Extraia sinais técnicos, contexto de segurança e indicadores de privacidade em uma única interface.</p>
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
        <>
          <section className="card export-card">
            <div className="export-actions">
              <button type="button" className="button secondary export-button" onClick={handleExportTxt}>
                [ EXPORT TXT ]
              </button>
              <button type="button" className="button secondary export-button" onClick={handleExportPdf}>
                [ EXPORT PDF ]
              </button>
              <button type="button" className="button primary export-button" onClick={handleExportJson}>
                [ EXPORT JSON ]
              </button>
              {canCleanMetadata && (
                <button
                  type="button"
                  className="button clean-button export-button"
                  onClick={handleCleanMetadata}
                  disabled={isCleaningMetadata}
                >
                  {isCleaningMetadata ? '[ CLEANING... ]' : '[ DOWNLOAD CLEAN IMAGE ]'}
                </button>
              )}
            </div>
          </section>

          {canSpoofMetadata && (
            <LocationPrivacyPanel
              loading={isSpoofingMetadata}
              onRemove={handleRemoveLocation}
              onReplace={handleReplaceLocation}
              onChangeDate={handleChangeDate}
              onChangeAuthor={handleChangeAuthor}
              canUseGps={canCleanMetadata}
              hasOriginalGps={Boolean(result?.location?.hasGps)}
              originalLatitude={result?.location?.latitudeDecimal}
              originalLongitude={result?.location?.longitudeDecimal}
            />
          )}

          {locationActionMessage && (
            <section className="card success-card" role="status">
              <strong>Operação concluída.</strong>
              <p>{locationActionMessage}</p>
            </section>
          )}

          <section className="results-wrapper">
            <AnalysisResult data={result} />
          </section>
        </>
      )}
    </main>
  )
}

export default App
