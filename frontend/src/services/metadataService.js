const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export async function extractMetadata(file) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await fetch(`${API_BASE_URL}/metadata/extract`, {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    let detail = ''

    try {
      const errorData = await response.json()
      detail = errorData.message || errorData.error || JSON.stringify(errorData)
    } catch {
      detail = await response.text()
    }

    const suffix = detail ? ` (${detail})` : ''
    throw new Error(`Falha ao analisar o arquivo${suffix}`)
  }

  return response.json()
}

export async function cleanMetadata(file) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await fetch(`${API_BASE_URL}/metadata/clean`, {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    let detail = ''

    try {
      const errorData = await response.json()
      detail = errorData.message || errorData.error || JSON.stringify(errorData)
    } catch {
      detail = await response.text()
    }

    const suffix = detail ? ` (${detail})` : ''
    throw new Error(`Falha ao remover metadados da imagem${suffix}`)
  }

  const blob = await response.blob()
  const contentDisposition = response.headers.get('content-disposition')
  const fileName = extractFileNameFromHeader(contentDisposition) || 'metascan-clean-image'

  return { blob, fileName }
}

export async function updateImageLocation(file, action, latitude, longitude) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('action', action)

  if (action === 'replace') {
    formData.append('latitude', String(latitude))
    formData.append('longitude', String(longitude))
  }

  const response = await fetch(`${API_BASE_URL}/metadata/location/update`, {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    let detail = ''

    try {
      const errorData = await response.json()
      detail = errorData.message || errorData.error || JSON.stringify(errorData)
    } catch {
      detail = await response.text()
    }

    const suffix = detail ? ` (${detail})` : ''
    throw new Error(`Falha ao atualizar localização GPS${suffix}`)
  }

  const blob = await response.blob()
  const contentDisposition = response.headers.get('content-disposition')
  const fallbackName = action === 'remove' ? 'metascan-no-gps-image' : 'metascan-relocated-image'
  const fileName = extractFileNameFromHeader(contentDisposition) || fallbackName

  return { blob, fileName }
}

export async function spoofMetadata(file, payload) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('action', payload.action)

  if (payload.action === 'replace_gps') {
    formData.append('latitude', String(payload.latitude))
    formData.append('longitude', String(payload.longitude))
  }

  if (payload.action === 'change_date') {
    formData.append('newDate', String(payload.newDate))
  }

  if (payload.action === 'change_author') {
    formData.append('author', String(payload.author))
  }

  const response = await fetch(`${API_BASE_URL}/metadata/spoof`, {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    let detail = ''

    try {
      const errorData = await response.json()
      detail = errorData.message || errorData.error || JSON.stringify(errorData)
    } catch {
      detail = await response.text()
    }

    const suffix = detail ? ` (${detail})` : ''
    throw new Error(`Falha ao aplicar spoofing de metadados${suffix}`)
  }

  const blob = await response.blob()
  const contentDisposition = response.headers.get('content-disposition')
  const fileName = extractFileNameFromHeader(contentDisposition) || 'spoofed-file'

  return { blob, fileName }
}

function extractFileNameFromHeader(contentDisposition) {
  if (!contentDisposition) {
    return null
  }

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1]).replace(/"/g, '')
  }

  const standardMatch = contentDisposition.match(/filename="?([^"]+)"?/i)
  if (standardMatch?.[1]) {
    return standardMatch[1].trim()
  }

  return null
}
