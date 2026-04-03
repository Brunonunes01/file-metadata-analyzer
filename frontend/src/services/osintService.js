const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export async function scanUsername(username) {
  const normalizedUsername = (username || '').trim()

  if (!normalizedUsername) {
    throw new Error('Informe um username para iniciar o scan.')
  }

  const query = new URLSearchParams({ username: normalizedUsername })
  const response = await request(`${API_BASE_URL}/osint/username/scan?${query.toString()}`, {
    method: 'POST',
  })

  await ensureOk(response, 'Falha ao executar o scan de username.')
  return response.json()
}

async function request(url, options) {
  try {
    return await fetch(url, options)
  } catch {
    throw new Error('Falha de conexao com a API.')
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
