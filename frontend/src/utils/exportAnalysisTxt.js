import { booleanLabel, formatFileSize, sanitizeFileName, triggerDownloadFromBlob } from './exportUtils'

export function exportAnalysisAsTxt(analysisData) {
  if (!analysisData) {
    return
  }

  const summary = analysisData.summary || {}
  const security = analysisData.security || {}
  const location = analysisData.location
  const insights = Array.isArray(analysisData.insights) ? analysisData.insights : []

  const lines = [
    '==== MetaScan - Relatório de Análise ====',
    '',
    'Arquivo:',
    `- Nome: ${analysisData.fileName || '-'}`,
    `- Tamanho: ${formatFileSize(analysisData.fileSize)}`,
    `- Tipo detectado: ${analysisData.contentTypeDetectado || '-'}`,
    `- Tipo amigável: ${analysisData.fileType || '-'}`,
    `- Hash SHA-256: ${analysisData.hashSha256 || '-'}`,
    '',
    'Insights:',
    ...formatList(insights),
    '',
    'Resumo:',
    `- author: ${summary.author || '-'}`,
    `- lastAuthor: ${summary.lastAuthor || '-'}`,
    `- createdAt: ${summary.createdAt || '-'}`,
    `- lastModified: ${summary.lastModified || '-'}`,
    `- revision: ${summary.revision || '-'}`,
    `- title: ${summary.title || '-'}`,
    `- subject: ${summary.subject || '-'}`,
    `- description: ${summary.description || '-'}`,
    `- language: ${summary.language || '-'}`,
    '',
    'Segurança:',
    `- hasMetadata: ${booleanLabel(security.hasMetadata)}`,
    `- hasText: ${booleanLabel(security.hasText)}`,
    `- hasAuthor: ${booleanLabel(security.hasAuthor)}`,
    `- hasCreationDate: ${booleanLabel(security.hasCreationDate)}`,
  ]

  if (location && typeof location === 'object') {
    lines.push('')
    lines.push('Localização:')
    lines.push(`- latitude: ${formatCoordinate(location.latitudeDecimal)}`)
    lines.push(`- longitude: ${formatCoordinate(location.longitudeDecimal)}`)
    lines.push(`- Google Maps: ${location.mapsUrl || '-'}`)
  }

  const reportText = lines.join('\n')
  const fileName = sanitizeFileName(analysisData.fileName)
  const reportName = `metascan-report-${fileName}.txt`
  const blob = new Blob([reportText], { type: 'text/plain;charset=utf-8' })
  triggerDownloadFromBlob(blob, reportName)
}

function formatList(items) {
  if (!Array.isArray(items) || items.length === 0) {
    return ['- Nenhum insight disponível']
  }

  return items.map((item) => `- ${typeof item === 'string' ? item : JSON.stringify(item)}`)
}

function formatCoordinate(value) {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return '-'
  }
  return value.toFixed(6)
}
