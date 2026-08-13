package ar.edu.utn.dds.k3003.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true")
public class TelegramDonaTrackBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramDonaTrackBot.class);
    private final String botUsername;

    public TelegramDonaTrackBot() {
        // Le pasamos el token directamente al constructor del padre
        super(requireEnv("TOKEN_BOT")); 
        
        this.botUsername = requireEnv("NOMBRE_BOT");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();
        log.info("[TELEGRAM_BOT] Mensaje recibido chatId={} texto={}", chatId, text);

        String response = switch (text) {
            case "/start" -> mensajeInicio();
            case "/donador" -> "Modo donador seleccionado. Próximos comandos: registro, stats y consultas.";
            case "/admin" -> "Modo admin seleccionado. Próximos comandos: ABM entidades y necesidades.";
            default -> "Comando no reconocido. Usá /start para ver opciones.";
        };

        SendMessage message = new SendMessage(chatId.toString(), response);
        try {
            execute(message);
        } catch (TelegramApiException exception) {
            log.error("[TELEGRAM_BOT] Error enviando mensaje a chatId={}", chatId, exception);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }



    private String mensajeInicio() {
        return """
            ¡Hola! Soy el bot del módulo Incentivos.
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
