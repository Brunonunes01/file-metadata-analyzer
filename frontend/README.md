# MetaScan Frontend

SPA em React + Vite para envio de arquivos e visualização da análise de metadados retornada pelo backend.

## Requisitos

- Node.js 20+
- NPM 10+
- Backend MetaScan ativo em `http://localhost:8080`

## Instalação

```bash
cd frontend
npm install
```

## Executar em desenvolvimento

```bash
npm run dev
```

- Frontend roda em `http://localhost:5173`
- As chamadas para API usam proxy do Vite:
  - `POST /api/metadata/extract` -> `http://localhost:8080/metadata/extract`
- Isso evita problema de CORS no ambiente local sem alterar o backend.

## Build de produção

```bash
npm run build
```

Arquivos gerados em `frontend/dist`.

## Variável opcional

Se quiser chamar a API sem proxy, crie `frontend/.env`:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

Sem essa variável, o frontend usa `/api` por padrão.
