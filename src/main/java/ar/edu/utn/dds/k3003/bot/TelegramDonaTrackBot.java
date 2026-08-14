package ar.edu.utn.dds.k3003.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ar.edu.utn.dds.k3003.modulos.IncentivosClient;

@Component
public class TelegramDonaTrackBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramDonaTrackBot.class);
    private final String botUsername;
    private final IncentivosClient incentivosClient;

    public TelegramDonaTrackBot(IncentivosClient incentivosClient) {
        // Le pasamos el token directamente al constructor del padre
        super(requireEnv("TOKEN_BOT")); 
        
        this.botUsername = requireEnv("NOMBRE_BOT");
        this.incentivosClient = incentivosClient;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();
        log.info("[TELEGRAM_BOT] Mensaje recibido chatId={} texto={}", chatId, text);

        String response;

        // Manejo de comandos con parámetros como /stats <donadorId>
        if (text.startsWith("/stats")) {
            response = procesarComandoStats(text);
        } else {
            response = switch (text) {
                case "/start" -> mensajeInicio();
                case "/donador" -> """
                        Modo donador seleccionado. 
                        Comandos disponibles:
                        - /stats <tu_id> : Consultá tus puntos, nivel e insignias.
                        """;
                case "/admin" -> "Modo admin seleccionado. Próximos comandos: ABM entidades y necesidades.";
                default -> "Comando no reconocido. Usá /start para ver opciones.";
            };
        }

        SendMessage message = new SendMessage(chatId.toString(), response);
        try {
            execute(message);
        } catch (TelegramApiException exception) {
            log.error("[TELEGRAM_BOT] Error enviando mensaje a chatId={}", chatId, exception);
        }
    }

    private String procesarComandoStats(String text) {
        String[] partes = text.split("\\s+");
        if (partes.length < 2) {
            return "⚠️ Por favor, indicá tu ID de donador.\nEjemplo: /stats 1";
        }
        String donadorId = partes[1];
        log.info("[TELEGRAM_BOT] Consultando estado en Incentivos para donadorId={}", donadorId);
        
        String resultado = incentivosClient.consultarEstado(donadorId);
        return "📊 Estado de Incentivos:\n\n" + resultado;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    private String mensajeInicio() {
        return """
            ¡Hola! Soy el bot de DonaTrack.
            ¿Qué tipo de usuario sos?
            - /donador
            - /admin
            """;
    }

    private static String requireEnv(String variable) {
        String value = System.getenv(variable);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno obligatoria: " + variable);
        }
        return value;
    }
}