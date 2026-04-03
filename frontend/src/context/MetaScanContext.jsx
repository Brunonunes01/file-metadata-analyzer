import { createContext, useContext, useMemo, useState } from 'react'
import { cleanMetadata, extractMetadata, spoofMetadata } from '../services/metadataService'
import { scanUsername } from '../services/osintService'
import { exportAnalysisAsJson } from '../utils/exportAnalysisJson'
import { exportAnalysisAsTxt } from '../utils/exportAnalysisTxt'
import { exportAnalysisAsPdf } from '../utils/exportAnalysisPdf'
import { triggerDownloadFromBlob } from '../utils/exportUtils'

const MetaScanContext = createContext(null)

export function MetaScanProvider({ children }) {
  const [file, setFile] = useState(null)
  const [isLoading, setIsLoading] = useState(false)
  const [isCleaningMetadata, setIsCleaningMetadata] = useState(false)
  const [isSpoofingMetadata, setIsSpoofingMetadata] = useState(false)
  const [error, setError] = useState('')
  const [locationActionMessage, setLocationActionMessage] = useState('')
  const [result, setResult] = useState(null)
  const [analyzedFile, setAnalyzedFile] = useState(null)
  const [username, setUsername] = useState('')
  const [isUsernameScanning, setIsUsernameScanning] = useState(false)
  const [usernameScanResult, setUsernameScanResult] = useState(null)

  const analyzeSelectedFile = async () => {
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

  const runUsernameScan = async () => {
    setError('')
    setLocationActionMessage('')
    setIsUsernameScanning(true)

    try {
      const response = await scanUsername(username)
      setUsernameScanResult(response)
    } catch (requestError) {
      setUsernameScanResult(null)
      setError(requestError.message || 'Nao foi possivel executar o scan de username.')
    } finally {
      setIsUsernameScanning(false)
    }
  }

  const runCleanMetadata = async () => {
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

  const removeLocation = async (cleanupMode) => {
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

  const replaceLocation = async (latitude, longitude, cleanupMode) => {
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

  const changeDate = async (newDate, cleanupMode) => {
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

  const changeAuthor = async (author, cleanupMode) => {
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

  const clearFeedbackMessages = () => {
    setError('')
    setLocationActionMessage('')
  }

  const exportJson = () => exportAnalysisAsJson(result)
  const exportTxt = () => exportAnalysisAsTxt(result)
  const exportPdf = () => exportAnalysisAsPdf(result)

  const canCleanMetadata =
    Boolean(result && analyzedFile) &&
    (result.contentTypeDetectado === 'image/jpeg' || result.contentTypeDetectado === 'image/png')
  const canSpoofMetadata = Boolean(result && analyzedFile)

  const value = useMemo(
    () => ({
      file,
      setFile,
      result,
      hasAnalysis: Boolean(result),
      analyzedFile,
      error,
      setError,
      isLoading,
      isCleaningMetadata,
      isSpoofingMetadata,
      locationActionMessage,
      clearFeedbackMessages,
      analyzeSelectedFile,
      runCleanMetadata,
      removeLocation,
      replaceLocation,
      changeDate,
      changeAuthor,
      canCleanMetadata,
      canSpoofMetadata,
      exportJson,
      exportTxt,
      exportPdf,
      username,
      setUsername,
      isUsernameScanning,
      usernameScanResult,
      runUsernameScan,
    }),
    [
      file,
      result,
      analyzedFile,
      error,
      isLoading,
      isCleaningMetadata,
      isSpoofingMetadata,
      locationActionMessage,
      canCleanMetadata,
      canSpoofMetadata,
      username,
      isUsernameScanning,
      usernameScanResult,
    ],
  )

  return <MetaScanContext.Provider value={value}>{children}</MetaScanContext.Provider>
}

export function useMetaScan() {
  const context = useContext(MetaScanContext)
  if (!context) {
    throw new Error('useMetaScan deve ser usado dentro de MetaScanProvider')
  }
  return context
}
