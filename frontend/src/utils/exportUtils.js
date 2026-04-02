export function sanitizeFileName(name) {
  if (!name || typeof name !== 'string') {
    return 'analysis'
  }

  return name
    .trim()
    .replace(/\s+/g, '-')
    .replace(/[^a-zA-Z0-9._-]/g, '')
}

export function triggerDownloadFromBlob(blob, fileName) {
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  link.rel = 'noopener'
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  // Evita revogar cedo demais o blob em navegadores que iniciam o download de forma assíncrona.
  window.setTimeout(() => {
    URL.revokeObjectURL(objectUrl)
  }, 30_000)
}

export function formatFileSize(bytes) {
  if (typeof bytes !== 'number' || Number.isNaN(bytes)) {
    return '-'
  }

  if (bytes === 0) {
    return '0 B'
  }

  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const exponent = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  const value = bytes / 1024 ** exponent

  return `${value.toFixed(value >= 10 || exponent === 0 ? 0 : 1)} ${units[exponent]}`
}

export function booleanLabel(value) {
  if (typeof value !== 'boolean') {
    return '-'
  }
  return value ? 'Sim' : 'Não'
}
