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
