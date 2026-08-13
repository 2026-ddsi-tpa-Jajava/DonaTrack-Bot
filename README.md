# DonaTrack-Bot
🧩 Variables de entornos usadas:
- `TELEGRAM_BOT_ENABLED`
- `NOMBRE_BOT`
- `TOKEN_BOT`

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