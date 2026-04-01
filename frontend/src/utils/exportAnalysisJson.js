import { sanitizeFileName, triggerDownloadFromBlob } from './exportUtils'

export function exportAnalysisAsJson(analysisData) {
  if (!analysisData) {
    return
  }

  const fileName = sanitizeFileName(analysisData.fileName)
  const reportName = `metascan-report-${fileName}.json`
  const jsonContent = JSON.stringify(analysisData, null, 2)
  const blob = new Blob([jsonContent], { type: 'application/json;charset=utf-8' })
  triggerDownloadFromBlob(blob, reportName)
}
