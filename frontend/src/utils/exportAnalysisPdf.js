import { jsPDF } from 'jspdf'
import { booleanLabel, formatFileSize, sanitizeFileName } from './exportUtils'

const PAGE_WIDTH = 210
const PAGE_HEIGHT = 297
const MARGIN_X = 14
const MARGIN_Y = 16
const CONTENT_WIDTH = PAGE_WIDTH - MARGIN_X * 2
const LINE_HEIGHT = 6

export function exportAnalysisAsPdf(analysisData) {
  if (!analysisData) {
    return
  }

  const summary = analysisData.summary || {}
  const security = analysisData.security || {}
  const location = analysisData.location
  const insights = Array.isArray(analysisData.insights) ? analysisData.insights : []

  const doc = new jsPDF({ unit: 'mm', format: 'a4' })
  let y = MARGIN_Y

  doc.setFont('helvetica', 'bold')
  doc.setFontSize(18)
  doc.text('MetaScan', MARGIN_X, y)
  y += 8

  doc.setFontSize(12)
  doc.setFont('helvetica', 'normal')
  doc.text('Relatório de Análise', MARGIN_X, y)
  y += 10

  y = addSection(doc, y, 'Informações Básicas', [
    `Nome do arquivo: ${analysisData.fileName || '-'}`,
    `Tamanho: ${formatFileSize(analysisData.fileSize)}`,
    `Tipo detectado: ${analysisData.contentTypeDetectado || '-'}`,
    `Tipo amigável: ${analysisData.fileType || '-'}`,
    `Hash SHA-256: ${analysisData.hashSha256 || '-'}`,
  ])

  const insightLines =
    insights.length > 0
      ? insights.map((item) => `- ${typeof item === 'string' ? item : JSON.stringify(item)}`)
      : ['- Nenhum insight disponível']
  y = addSection(doc, y, 'Insights', insightLines)

  y = addSection(doc, y, 'Resumo', [
    `Author: ${summary.author || '-'}`,
    `LastAuthor: ${summary.lastAuthor || '-'}`,
    `CreatedAt: ${summary.createdAt || '-'}`,
    `LastModified: ${summary.lastModified || '-'}`,
    `Revision: ${summary.revision || '-'}`,
    `Title: ${summary.title || '-'}`,
    `Subject: ${summary.subject || '-'}`,
    `Description: ${summary.description || '-'}`,
    `Language: ${summary.language || '-'}`,
  ])

  y = addSection(doc, y, 'Segurança', [
    `hasMetadata: ${booleanLabel(security.hasMetadata)}`,
    `hasText: ${booleanLabel(security.hasText)}`,
    `hasAuthor: ${booleanLabel(security.hasAuthor)}`,
    `hasCreationDate: ${booleanLabel(security.hasCreationDate)}`,
  ])

  if (location && typeof location === 'object') {
    y = addSection(doc, y, 'Localização', [
      `Latitude: ${formatCoordinate(location.latitudeDecimal)}`,
      `Longitude: ${formatCoordinate(location.longitudeDecimal)}`,
      `Google Maps: ${location.mapsUrl || '-'}`,
    ])
  }

  const fileName = sanitizeFileName(analysisData.fileName)
  doc.save(`metascan-report-${fileName}.pdf`)
}

function addSection(doc, initialY, title, lines) {
  let y = ensureSpace(doc, initialY, LINE_HEIGHT * 2)

  doc.setFont('helvetica', 'bold')
  doc.setFontSize(12)
  doc.text(title, MARGIN_X, y)
  y += 7

  doc.setFont('helvetica', 'normal')
  doc.setFontSize(10.5)

  for (const line of lines) {
    const wrapped = doc.splitTextToSize(line, CONTENT_WIDTH)
    y = ensureSpace(doc, y, wrapped.length * LINE_HEIGHT)
    doc.text(wrapped, MARGIN_X, y)
    y += wrapped.length * LINE_HEIGHT
  }

  return y + 2
}

function ensureSpace(doc, y, requiredHeight) {
  if (y + requiredHeight <= PAGE_HEIGHT - MARGIN_Y) {
    return y
  }

  doc.addPage()
  return MARGIN_Y
}

function formatCoordinate(value) {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return '-'
  }
  return value.toFixed(6)
}
