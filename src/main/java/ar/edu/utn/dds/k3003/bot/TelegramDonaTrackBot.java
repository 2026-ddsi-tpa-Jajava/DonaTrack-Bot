package ar.edu.utn.dds.k3003.bot;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ar.edu.utn.dds.k3003.modulos.DonadoresYEntidadesClient;
import ar.edu.utn.dds.k3003.modulos.IncentivosClient;

@Component
public class TelegramDonaTrackBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramDonaTrackBot.class);
    private final String botUsername;
    private final IncentivosClient incentivosClient;
    private final DonadoresYEntidadesClient donadoresYEntidadesClient;

    public TelegramDonaTrackBot(IncentivosClient incentivosClient,
                                 DonadoresYEntidadesClient donadoresYEntidadesClient) {
        // Le pasamos el token directamente al constructor del padre
        super(requireEnv("TOKEN_BOT"));

        this.botUsername = requireEnv("NOMBRE_BOT");
        this.incentivosClient = incentivosClient;
        this.donadoresYEntidadesClient = donadoresYEntidadesClient;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();
        log.info("[TELEGRAM_BOT] Mensaje recibido chatId={} texto={}", chatId, text);

        String response = procesarComando(text);

        SendMessage message = new SendMessage(chatId.toString(), response);
        message.enableMarkdown(true);
        try {
            execute(message);
        } catch (TelegramApiException exception) {
            log.error("[TELEGRAM_BOT] Error enviando mensaje a chatId={}", chatId, exception);
        }
    }

    private String procesarComando(String text) {
        String comando = text.split("\\s+", 2)[0];

        return switch (comando) {
            case "/start" -> mensajeInicio();
            case "/donador" -> menuDonador();
            case "/admin" -> menuAdmin();

            // ---------- Incentivos (estado de gamificación puntual) ----------
            case "/stats" -> procesarComandoStats(text);

            // ---------- Donadores ----------
            case "/registrarme" -> procesarRegistrarme(text);
            case "/misestadisticas" -> procesarMisEstadisticas(text);
            case "/verdonador" -> procesarVerDonador(text);
            case "/donadores" -> donadoresYEntidadesClient.consultarDonadores();

            // ---------- Entidades (admin) ----------
            case "/crearentidad" -> procesarCrearEntidad(text);
            case "/editarentidad" -> procesarEditarEntidad(text);
            case "/entidades" -> donadoresYEntidadesClient.consultarEntidades();
            case "/verentidad" -> procesarVerEntidad(text);

            // ---------- Necesidades (admin) ----------
            case "/crearnecesidad" -> procesarCrearNecesidad(text);
            case "/necesidades" -> procesarNecesidadesPorProducto(text);
            case "/borrarnecesidad" -> procesarBorrarNecesidad(text);
            case "/modificarnecesidad" -> procesarModificarNecesidad(text);
            case "/consultarnecesidad" -> procesarConsultarNecesidad(text);

            default -> "Comando no reconocido. Usá /start para ver opciones.";
        };
    }

    // ==================== Menús ====================

    private String mensajeInicio() {
        return """
            ¡Hola! Soy el bot de DonaTrack.
            ¿Qué tipo de usuario sos?
            - /donador
            - /admin
            """;
    }

    private String menuDonador() {
        return """
            *Modo donador* 🙋
            Comandos disponibles:
            - /registrarme Nombre|Apellido|Edad|Email|NroDocumento|Domicilio
            - /misestadisticas ID
            - /verdonador ID
            - /donadores  (lista todos)

            _Ejemplo:_ `/registrarme Juan|Perez|30|juan@mail.com|30111222|Av Siempre Viva 123`
            """;
    }

    private String menuAdmin() {
        return """
            *Modo admin* 🛠
            Entidades:
            - /crearentidad RazonSocial|Domicilio|Telefono|Correo
            - /editarentidad ID|RazonSocial|Domicilio|Telefono|Correo
            - /entidades  (lista todas)
            - /verentidad ID

            Necesidades:
            - /crearnecesidad EntidadID|NivelUrgencia|Descripcion|CantidadObjetivo|ProductoID|Tipo
            - /necesidades ProductoID
            - /consultarnecesidad ID
            - /modificarnecesidad ID|Descripcion
            - /borrarnecesidad ID

            _Tipo de necesidad: EXTRAORDINARIA o RECURRENTE_
            """;
    }

    // ==================== Incentivos ====================

    private String procesarComandoStats(String text) {
        String[] partes = text.split("\\s+");
        if (partes.length < 2) {
            return "⚠️ Indicá el ID de donador.\nEjemplo: `/stats 1`";
        }
        String donadorId = partes[1];
        log.info("[TELEGRAM_BOT] Consultando estado en Incentivos para donadorId={}", donadorId);
        String resultado = incentivosClient.consultarEstado(donadorId);
        return "📊 Estado de Incentivos:\n\n" + resultado;
    }

    // ==================== Donadores ====================

    private String procesarRegistrarme(String text) {
        String[] campos = extraerArgumentos(text);
        if (campos.length != 6) {
            return "⚠️ Formato incorrecto.\nUsá: `/registrarme Nombre|Apellido|Edad|Email|NroDocumento|Domicilio`";
        }
        try {
            int edad = Integer.parseInt(campos[2].trim());
            return donadoresYEntidadesClient.registrarDonador(
                    campos[0].trim(), campos[1].trim(), edad,
                    campos[3].trim(), campos[4].trim(), campos[5].trim());
        } catch (NumberFormatException e) {
            return "⚠️ La edad tiene que ser un número. Ejemplo: `/registrarme Juan|Perez|30|juan@mail.com|30111222|Av Siempre Viva 123`";
        }
    }

    private String procesarMisEstadisticas(String text) {
        String[] partes = text.split("\\s+");
        if (partes.length < 2) {
            return "⚠️ Indicá tu ID de donador.\nEjemplo: `/misestadisticas 1`";
        }
        return donadoresYEntidadesClient.consultarEstadisticas(partes[1]);
    }

    private String procesarVerDonador(String text) {
        String[] partes = text.split("\\s+");
        if (partes.length < 2) {
            return "⚠️ Indicá el ID del donador.\nEjemplo: `/verdonador 1`";
        }
        return donadoresYEntidadesClient.consultarDonadorPorId(partes[1]);
    }

    // ==================== Entidades ====================

    private String procesarCrearEntidad(String text) {
        String[] campos = extraerArgumentos(text);
        if (campos.length != 4) {
            return "⚠️ Formato incorrecto.\nUsá: `/crearentidad RazonSocial|Domicilio|Telefono|Correo`";
        }
        return donadoresYEntidadesClient.crearEntidad(
                campos[0].trim(), campos[1].trim(), campos[2].trim(), campos[3].trim());
    }

    private String procesarEditarEntidad(String text) {
        String[] partes = text.split("\\s+"); // Separa por espacios
        
        if (partes.length < 3) {
            return "⚠️ Formato incorrecto.\nUsá: `/editarentidad ID campo=valor campo2=valor2`\nEjemplo: `/editarentidad 5 telefono=112233 correo=nuevo@mail.com`";
        }
        
        String id = partes[1].trim();
        Map<String, Object> campos = new java.util.LinkedHashMap<>();
        
        // Recorremos desde el tercer elemento en adelante buscando el "="
        for (int i = 2; i < partes.length; i++) {
            String[] claveValor = partes[i].split("=", 2);
            if (claveValor.length == 2) {
                campos.put(claveValor[0].trim(), claveValor[1].trim());
            }
        }
        
        if (campos.isEmpty()) {
            return "⚠️ No se detectó ningún campo válido para modificar. Usá el formato `campo=valor`.";
        }

        // ¡Acá le mandamos el ID y el Mapa, justo lo que pide el cliente!
        return donadoresYEntidadesClient.editarEntidad(id, campos);
    }

    private String procesarVerEntidad(String text) {
        String[] partes = text.split("\\s+");
        if (partes.length < 2) {
            return "⚠️ Indicá el ID de la entidad.\nEjemplo: `/verentidad 1`";
        }
        return donadoresYEntidadesClient.consultarEntidadPorId(partes[1]);
    }

    // ==================== Necesidades ====================

    private String procesarCrearNecesidad(String text) {
        String[] campos = extraerArgumentos(text);
        if (campos.length != 6) {
            return "⚠️ Formato incorrecto.\nUsá: `/crearnecesidad EntidadID|NivelUrgencia|Descripcion|CantidadObjetivo|ProductoID|Tipo`";
        }
        try {
            int nivelUrgencia = Integer.parseInt(campos[1].trim());
            int cantidadObjetivo = Integer.parseInt(campos[3].trim());
            return donadoresYEntidadesClient.crearNecesidad(
                    campos[0].trim(), nivelUrgencia, campos[2].trim(),
                    cantidadObjetivo, campos[4].trim(), campos[5].trim().toUpperCase());
        } catch (NumberFormatException e) {
            return "⚠️ NivelUrgencia y CantidadObjetivo tienen que ser números.";
        }
    }

    private String procesarNecesidadesPorProducto(String text) {
        String[] partes = text.split("\\s+");
        if (partes.length < 2) {
            return "⚠️ Indicá el ID del producto.\nEjemplo: `/necesidades prod-1`";
        }
        return donadoresYEntidadesClient.consultarNecesidadesPorProducto(partes[1]);
    }

    private String procesarBorrarNecesidad(String text) {
        String[] partes = text.split("\\s+");
        if (partes.length < 2) {
            return "⚠️ Indicá el ID de la necesidad.\nEjemplo: `/borrarnecesidad nec-1`";
        }
        return donadoresYEntidadesClient.borrarNecesidad(partes[1]);
    }

    private String procesarModificarNecesidad(String text) {
        String[] partes = text.split("\\s+");
        
        if (partes.length < 3) {
            return "⚠️ Formato incorrecto.\nUsá: `/modificarnecesidad ID campo=valor`\nEjemplo: `/modificarnecesidad 10 descripcion=NuevasFrazadas`";
        }
        
        String id = partes[1].trim();
        Map<String, Object> campos = new java.util.LinkedHashMap<>();
        
        for (int i = 2; i < partes.length; i++) {
            String[] claveValor = partes[i].split("=", 2);
            if (claveValor.length == 2) {
                campos.put(claveValor[0].trim(), claveValor[1].trim());
            }
        }

        if (campos.isEmpty()) {
            return "⚠️ No se detectó ningún campo válido para modificar. Usá el formato `descripcion=NuevoTexto`.";
        }

        return donadoresYEntidadesClient.modificarNecesidad(id, campos);
    }

    private String procesarConsultarNecesidad(String text) {
        String[] partes = text.split("\\s+");
        if (partes.length < 2) {
            return "⚠️ Indicá el ID de la necesidad.\nEjemplo: `/consultarnecesidad nec-1`";
        }
        return donadoresYEntidadesClient.consultarNecesidadPorId(partes[1]);
    }

    // ==================== Utilidades ====================

    /**
     * Separa el comando de sus argumentos y parte los argumentos por "|".
     * Ej: "/crearentidad Cruz Roja|Av Siempre 123|1122334455|hola@cruzroja.org"
     *  -> ["Cruz Roja", "Av Siempre 123", "1122334455", "hola@cruzroja.org"]
     */
    private String[] extraerArgumentos(String text) {
        String[] partes = text.split("\\s+", 2);
        if (partes.length < 2 || partes[1].isBlank()) {
            return new String[0];
        }
        return partes[1].split("\\|");
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    private static String requireEnv(String variable) {
        String value = System.getenv(variable);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno obligatoria: " + variable);
        }
        return value;
    }
}
