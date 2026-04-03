const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export async function extractMetadata(file) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await request(`${API_BASE_URL}/metadata/extract`, {
    method: 'POST',
    body: formData,
  })

  await ensureOk(response, 'Falha ao analisar o arquivo.')

  return response.json()
}

export async function cleanMetadata(file) {
  const formData = new FormData()
  formData.append('file', file)

  const response = await request(`${API_BASE_URL}/metadata/clean`, {
    method: 'POST',
    body: formData,
  })

  await ensureOk(response, 'Falha ao remover metadados da imagem.')

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

  const response = await request(`${API_BASE_URL}/metadata/location/update`, {
    method: 'POST',
    body: formData,
  })

  await ensureOk(response, 'Falha ao atualizar localização GPS.')

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
  if (payload.cleanupMode) {
    formData.append('cleanupMode', String(payload.cleanupMode))
  }

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

  const response = await request(`${API_BASE_URL}/metadata/spoof`, {
    method: 'POST',
    body: formData,
  })

  await ensureOk(response, 'Falha ao aplicar spoofing de metadados.')

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

async function request(url, options) {
  try {
    return await fetch(url, options)
  } catch {
    throw new Error(
      'Falha de conexão com a API. Verifique se o backend está ativo e se a URL base da API está correta.'
    )
  }
}

async function ensureOk(response, fallbackMessage) {
  if (response.ok) {
    return
  }

  let apiMessage = ''

  try {
    const contentType = response.headers.get('content-type') || ''

    if (contentType.includes('application/json')) {
      const payload = await response.json()
      apiMessage = payload?.message || payload?.error || ''
    } else {
      apiMessage = (await response.text()).trim()
    }
  } catch {
    apiMessage = ''
  }

  throw new Error(apiMessage || fallbackMessage)
}
