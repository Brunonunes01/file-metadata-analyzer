# MetaScan

MetaScan processa arquivos com Spring Boot (backend), React/Vite (frontend), ExifTool e ClamAV.

## Docker

### Subir ambiente completo

```bash
docker compose up --build
```

### Parar ambiente

```bash
docker compose down
```

### Serviços e portas

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`
- ClamAV (clamd): `localhost:3310` (uso interno, também exposto para diagnóstico)

### Como os containers se conectam

- O frontend roda em Nginx e faz proxy de `/api/*` para `backend:8080`.
- O backend faz varredura antivírus usando `clamd` no serviço `clamav:3310`.
- ExifTool já está instalado no container do backend.

### Observações

- Em `docker-compose`, o backend usa:
  - `METASCAN_ANTIVIRUS_MODE=clamd`
  - `METASCAN_ANTIVIRUS_CLAMD_HOST=clamav`
  - `METASCAN_ANTIVIRUS_CLAMD_PORT=3310`
- Fora de Docker, o backend continua podendo usar modo local com `clamscan` (padrão).
