# DonaTrack-Bot
🧩 Variables de entornos usadas:
- `TELEGRAM_BOT_ENABLED`
- `NOMBRE_BOT`
- `TOKEN_BOT`
- `API_GATEWAY_URL`: URL única del API Gateway. Por defecto, `https://jajava-gateway-main-15d96d2.zuplo.app`.

El bot se comunica únicamente con el API Gateway. Los clientes internos conservan
los paths de cada funcionalidad, pero todas sus peticiones HTTP usan
`API_GATEWAY_URL`; el gateway es quien las enruta al módulo correspondiente.

🧩 Base Bot de Telegram (Entrega 4)
- Se agregó la base del bot usando `org.telegram:telegrambots:6.5.0`.
- El bot se registra automáticamente al iniciar la app **solo si** `TELEGRAM_BOT_ENABLED=true`.
- Variables requeridas:
  - `NOMBRE_BOT`
  - `TOKEN_BOT`
- Comandos base implementados:
  - `/start`: muestra menú inicial de tipo de usuario.
  - `/donador`: selecciona modo donador (base).
  - `/admin`: selecciona modo admin (base).