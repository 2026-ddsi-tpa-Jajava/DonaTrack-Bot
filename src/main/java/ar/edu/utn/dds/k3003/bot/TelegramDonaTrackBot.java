package ar.edu.utn.dds.k3003.bot;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ar.edu.utn.dds.k3003.modulos.LogisticaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ar.edu.utn.dds.k3003.modulos.DonadoresYEntidadesClient;
import ar.edu.utn.dds.k3003.modulos.IncentivosClient;
import ar.edu.utn.dds.k3003.modulos.DonacionesClient;

@Component
public class TelegramDonaTrackBot extends TelegramLongPollingBot {

    private static final Duration TIEMPO_MAXIMO_INACTIVIDAD = Duration.ofMinutes(10);
    private static final Logger log = LoggerFactory.getLogger(TelegramDonaTrackBot.class);
    private final String botUsername;
    private final IncentivosClient incentivosClient;
    private final DonadoresYEntidadesClient donadoresYEntidadesClient;
    private final LogisticaClient logisticaClient;
    private final DonacionesClient donacionesClient;
    private final SessionManager sessionManager;
    private final ConcurrentHashMap<Integer, Boolean> actualizacionesProcesadas = new ConcurrentHashMap<>();

    public TelegramDonaTrackBot(IncentivosClient incentivosClient,
                                DonadoresYEntidadesClient donadoresYEntidadesClient,
                                LogisticaClient logisticaClient,
                                DonacionesClient donacionesClient,
                                SessionManager sessionManager) {

        // Le pasamos el token directamente al constructor del padre
        super(requireEnv("TOKEN_BOT"));

        this.botUsername = requireEnv("NOMBRE_BOT");
        this.incentivosClient = incentivosClient;
        this.donadoresYEntidadesClient = donadoresYEntidadesClient;
        this.logisticaClient = logisticaClient;
        this.donacionesClient = donacionesClient;
        this.sessionManager = sessionManager;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (actualizacionesProcesadas.putIfAbsent(update.getUpdateId(), Boolean.TRUE) != null) {
            log.debug("[TELEGRAM_BOT] Actualización duplicada ignorada updateId={}", update.getUpdateId());
            return;
        }
        if (actualizacionesProcesadas.size() > 10_000) {
            actualizacionesProcesadas.clear();
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();
        boolean formularioExpirado = sessionManager.limpiarSiInactiva(chatId, TIEMPO_MAXIMO_INACTIVIDAD);
        String rol = text.startsWith("/admin") ? "admin" : "donador";
        sessionManager.registrarInteraccion(chatId, text, rol);
        log.info("[TELEGRAM_BOT] Mensaje recibido chatId={} texto={}", chatId, text);

        String response = formularioExpirado && !text.startsWith("/")
                ? "⌛ La operación venció por inactividad. Escribí `/start` para comenzar de nuevo."
                : procesarComando(chatId, text);
        if (response == null || response.isBlank()) {
            response = "⚠️ El comando no devolvió información.";
        }
        log.info("RESPUESTA BOT = [{}]", response);

        SendMessage message = new SendMessage(chatId.toString(), response);
        message.enableMarkdown(true);
        try {
            execute(message);
        } catch (TelegramApiException exception) {
            log.error("[TELEGRAM_BOT] Error enviando mensaje a chatId={}", chatId, exception);
        }
    }

    private String procesarComando(Long chatId, String text) {
        String comando = extraerComando(text);
        if ("/start".equals(comando) || "/menu".equals(comando)) {
            sessionManager.limpiarChat(chatId);
            return mensajeInicio();
        }
        if ("/salir".equals(comando) || "/apagar".equals(comando)) {
            return salirDelChat(chatId);
        }
        if ("/cancelar".equals(comando)) {
            return cancelarFormulario(chatId);
        }
        if (!text.startsWith("/") && sessionManager.obtenerRegistroDonador(chatId) != null) {
            return procesarRespuestaRegistroDonador(chatId, text);
        }
        if (!text.startsWith("/") && sessionManager.obtenerFormulario(chatId) != null) {
            return procesarRespuestaFormulario(chatId, text);
        }

        if (esFormularioConversacional(comando) && extraerArgumentos(text).length == 0) {
            return iniciarFormulario(chatId, comando);
        }

        return switch (comando) {
            case "/donador" -> menuDonador();
            case "/admin" -> menuAdmin();

            // ---------- Incentivos (estado de gamificación puntual) ----------
            case "/stats" -> procesarComandoStats(text);

            // ---------- Donadores ----------
            case "/registrarme" -> procesarRegistrarme(chatId, text);
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

            // ---------- Catálogo Donaciones (Admin) ----------
            case "/crearidentificador" -> procesarCrearIdentificador(text);
            case "/identificadores" -> donacionesClient.consultarIdentificadores();
            case "/crearcategoria" -> procesarCrearCategoria(text);
            case "/categorias" -> donacionesClient.consultarCategorias();
            case "/crearsubcategoria" -> procesarCrearSubcategoria(text);
            case "/subcategorias" -> procesarConsultarSubcategorias(text);
            case "/crearproducto" -> procesarCrearProducto(text);
            case "/productos" -> donacionesClient.consultarProductos();

            // ---------- Logística ----------
            case "/creardeposito" -> procesarCrearDeposito(text);
            case "/depositos" -> logisticaClient.consultarDepositos();
            case "/deposito" -> procesarConsultarDeposito(text);
            case "/stock" -> procesarConsultarStock(text);
            case "/stockproducto" -> procesarConsultarStockProducto(text);
            case "/asignaciones" -> logisticaClient.consultarAsignaciones();
            case "/asignadas" -> logisticaClient.consultarAsignacionesPorEstado("ASIGNADA");
            case "/completadas" -> logisticaClient.consultarAsignacionesPorEstado("COMPLETADA");
            case "/eliminardepositos" -> logisticaClient.eliminarDepositos();
            case "/eliminarasignaciones" -> logisticaClient.eliminarAsignaciones();
            case "/eliminarpaquetes" -> logisticaClient.eliminarPaquetes();
            case "/vaciarstock" -> procesarVaciarStock(text);
            case "/configuraralgoritmo" -> procesarConfigurarAlgoritmo(text);
            case "/asignacion" -> procesarConsultarAsignacion(text);

            // ---------- Donaciones ----------
            case "/donar" -> procesarDonar(text);
            case "/donaciones" -> donacionesClient.consultarDonaciones();
            case "/verdonacion" -> procesarVerDonacion(text);
            case "/queja" -> procesarQueja(text);

            default -> "Comando no reconocido. Usá /start para ver opciones.";
        };
    }

    // ==================== Menús ====================

    private String mensajeInicio() {
        return """
            👋 *¡Hola! Soy DonaTrack!*

            Elegí una opción:
            🙋 /donador — Menú de donaciones
            🛠️ /admin — Herramientas de administración

            📌 *Comandos útiles*
            🏠 /start o /menu — Volver al inicio
            👋 /salir — Finalizar la sesión
            """;
    }

    private String menuDonador() {
        return """
            🙋 *Menú de donador*

            📝 /registrarme — Crear mi cuenta
            📊 /misestadisticas ID — Ver mis estadísticas
            👤 /verdonador ID — Consultar un donador
            👥 /donadores — Ver todos los donadores
            🎁 /donar — Registrar una donación
            📣 /queja — Registrar una queja

            Elegí un comando para comenzar.
            """;
    }

    private String menuAdmin() {
        return """
            🛠️ *Menú de administración*

            🏢 *Entidades*
            ➕ /crearentidad — Crear una entidad
            ✏️ /editarentidad ID campo=valor
            📋 /entidades — Listar entidades
            🔎 /verentidad ID

            🆘 *Necesidades*
            ➕ /crearnecesidad — Crear una necesidad
            📋 /necesidades ProductoID
            🔎 /consultarnecesidad ID
            ✏️ /modificarnecesidad ID campo=valor
            🗑️ /borrarnecesidad ID
            ℹ️ Tipos: EXTRAORDINARIA o RECURRENTE

            🚚 *Logística*
            ➕ /creardeposito — Crear un depósito
            🏢 /depositos — Listar depósitos
            🔎 /deposito ID
            📦 /stock ID
            📦 /stockproducto ID
            🔗 /asignacion IDPAQUETE
            📋 /asignaciones
            ✅ /asignadas
            ✔️ /completadas
            ⚙️ /configuraralgoritmo
            🧹 /vaciarstock ID
            🗑️ /eliminarpaquetes
            🗑️ /eliminarasignaciones
            🗑️ /eliminardepositos

            🎁 *Donaciones*
            📋 /donaciones — Listar donaciones
            🔎 /verdonacion ID

            🗂️ *Catálogo*
            🏷️ /crearidentificador
            📋 /identificadores
            ➕ /crearcategoria
            📋 /categorias
            ➕ /crearsubcategoria
            📋 /subcategorias CategoriaID
            ➕ /crearproducto
            📋 /productos
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

    private String procesarRegistrarme(Long chatId, String text) {
        String[] campos = extraerArgumentos(text);
        if (campos.length == 0) {
            sessionManager.iniciarRegistroDonador(chatId);
            return "📝 Vamos a registrarte como donador.\n¿Cuál es tu nombre?";
        }
        if (campos.length != 6) {
            return "⚠️ Para registrarte, escribí `/registrarme` y respondé las preguntas una por una.";
        }
        try {
            int edad = Integer.parseInt(campos[2].trim());
            sessionManager.cancelarRegistroDonador(chatId);
            return donadoresYEntidadesClient.registrarDonador(
                    campos[0].trim(), campos[1].trim(), edad,
                    campos[3].trim(), campos[4].trim(), campos[5].trim());
        } catch (NumberFormatException e) {
            return "⚠️ La edad tiene que ser un número.";
        }
    }

    private String procesarRespuestaRegistroDonador(Long chatId, String respuesta) {
        SessionManager.RegistroDonador registro = sessionManager.obtenerRegistroDonador(chatId);
        if (registro == null) {
            return "No hay un registro en curso. Escribí `/registrarme` para comenzar.";
        }

        if (registro.paso() == 2) {
            try {
                Integer.valueOf(respuesta);
            } catch (NumberFormatException e) {
                return "⚠️ La edad tiene que ser un número. ¿Cuál es tu edad?";
            }
        }

        SessionManager.RegistroDonador actualizado =
                sessionManager.guardarRespuestaRegistro(chatId, respuesta);
        if (actualizado == null) {
            return "No hay un registro en curso. Escribí `/registrarme` para comenzar.";
        }

        if (actualizado.paso() == 6) {
            List<String> campos = actualizado.campos();
            try {
                String respuestaRegistro = donadoresYEntidadesClient.registrarDonador(
                        campos.get(0), campos.get(1), Integer.valueOf(campos.get(2)),
                        campos.get(3), campos.get(4), campos.get(5));
                sessionManager.cancelarRegistroDonador(chatId);
                return respuestaRegistro;
            } catch (NumberFormatException e) {
                return "⚠️ La edad tiene que ser un número.";
            }
        }

        return switch (actualizado.paso()) {
            case 1 -> "¿Cuál es tu apellido?";
            case 2 -> "¿Cuál es tu edad?";
            case 3 -> "¿Cuál es tu email?";
            case 4 -> "¿Cuál es tu número de documento?";
            case 5 -> "¿Cuál es tu domicilio?";
            default -> "⚠️ No se pudo continuar el registro. Escribí `/registrarme` para empezar de nuevo.";
        };
    }

    private boolean esFormularioConversacional(String comando) {
        return switch (comando) {
            case "/crearentidad", "/crearnecesidad", "/creardeposito",
                    "/configuraralgoritmo", "/donar", "/queja",
                    "/crearcategoria", "/crearproducto",
                    "/crearidentificador", "/crearsubcategoria" -> true;
            default -> false;
        };
    }

    private String iniciarFormulario(Long chatId, String comando) {
        sessionManager.iniciarFormulario(chatId, comando);
        return preguntaFormulario(comando, 0);
    }

    private String procesarRespuestaFormulario(Long chatId, String respuesta) {
        SessionManager.Formulario formulario = sessionManager.obtenerFormulario(chatId);
        if (formulario == null) {
            return "No hay un formulario en curso.";
        }

        if (requiereNumero(formulario.comando(), formulario.paso())) {
            try {
                Integer.valueOf(respuesta.trim());
            } catch (NumberFormatException e) {
                return "⚠️ Este campo tiene que ser un número.\n" +
                        preguntaFormulario(formulario.comando(), formulario.paso());
            }
        }

        SessionManager.Formulario actualizado =
                sessionManager.guardarRespuestaFormulario(chatId, respuesta.trim());
        int cantidadCampos = cantidadCamposFormulario(actualizado.comando());
        if (actualizado.paso() < cantidadCampos) {
            return preguntaFormulario(actualizado.comando(), actualizado.paso());
        }

        sessionManager.cancelarFormulario(chatId);
        return procesarComando(chatId,
                actualizado.comando() + " " + String.join("|", actualizado.campos()));
    }

    private String cancelarFormulario(Long chatId) {
        sessionManager.cancelarRegistroDonador(chatId);
        sessionManager.cancelarFormulario(chatId);
        return "Operación cancelada. Podés iniciar otra cuando quieras.";
    }

    private String salirDelChat(Long chatId) {
        sessionManager.limpiarChat(chatId);
        return "Sesión finalizada. No hay ninguna operación en curso.\n"
                + "Cuando quieras volver a usar el bot, escribí `/start`.";
    }

    private int cantidadCamposFormulario(String comando) {
        return switch (comando) {
            case "/crearentidad" -> 4;
            case "/crearnecesidad" -> 6;
            case "/creardeposito", "/donar" -> comando.equals("/donar") ? 5 : 3;
            case "/configuraralgoritmo", "/queja", "/crearcategoria",
                    "/crearsubcategoria", "/crearidentificador" -> 2;
            case "/crearproducto" -> 4;
            default -> 0;
        };
    }

    private boolean requiereNumero(String comando, int paso) {
        return ("/crearnecesidad".equals(comando) && (paso == 1 || paso == 3))
                || ("/creardeposito".equals(comando) && paso == 2)
                || ("/donar".equals(comando) && paso == 4);
    }

    private String preguntaFormulario(String comando, int paso) {
        return switch (comando) {
            case "/crearentidad" -> switch (paso) {
                case 0 -> "¿Cuál es la razón social de la entidad?";
                case 1 -> "¿Cuál es el domicilio?";
                case 2 -> "¿Cuál es el teléfono?";
                default -> "¿Cuál es el correo electrónico?";
            };
            case "/crearnecesidad" -> switch (paso) {
                case 0 -> "¿Cuál es el ID de la entidad?";
                case 1 -> "¿Cuál es el nivel de urgencia? (número)";
                case 2 -> "¿Cuál es la descripción?";
                case 3 -> "¿Cuál es la cantidad objetivo? (número)";
                case 4 -> "¿Cuál es el ID del producto?";
                default -> "¿Qué tipo de necesidad es? (EXTRAORDINARIA o RECURRENTE)";
            };
            case "/creardeposito" -> switch (paso) {
                case 0 -> "¿Cuál es el nombre del depósito?";
                case 1 -> "¿Cuál es la dirección?";
                default -> "¿Cuál es la capacidad? (número)";
            };
            case "/configuraralgoritmo" -> paso == 0
                    ? "¿Cuál es el ID del depósito?" : "¿Qué algoritmo querés usar? (SUB_ATENDIDOS o PRIORIDAD_POR_SCORE)";
            case "/donar" -> switch (paso) {
                case 0 -> "¿Cuál es tu ID de donador?";
                case 1 -> "¿Cuál es el ID del depósito?";
                case 2 -> "¿Qué descripción tiene la donación?";
                case 3 -> "¿Cuál es el ID del producto?";
                default -> "¿Qué cantidad vas a donar? (número)";
            };
            case "/queja" -> paso == 0 ? "¿Cuál es el ID de la donación?" : "¿Cuál es la descripción de la queja?";
            case "/crearcategoria" -> paso == 0 ? "¿Cuál es el nombre de la categoría?" : "¿Cuál es la descripción?";
            case "/crearproducto" -> switch (paso) {
                case 0 -> "¿Cuál es el nombre del producto?";
                case 1 -> "¿Cuál es la descripción?";
                case 2 -> "¿Cuál es el ID de la subcategoría?";
                default -> "¿Cuál es el ID del identificador?";
            };
            case "/crearidentificador" -> paso == 0 ? "¿Qué tipo de identificador es? (QR o CODIGODEBARRAS)" : "¿Cuál es la descripción?";
            case "/crearsubcategoria" -> paso == 0 ? "¿Cuál es el nombre de la subcategoría?" : "¿Cuál es el ID de la categoría?";
            default -> "Ingresá el valor solicitado.";
        };
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
            return "⚠️ Para crear una entidad, escribí `/crearentidad` y respondé las preguntas.";
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
            return "⚠️ Para crear una necesidad, escribí `/crearnecesidad` y respondé las preguntas.";
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

    // ==================== Logistica ====================

    private String procesarCrearDeposito(String text) {

        String[] campos = extraerArgumentos(text);

        if (campos.length != 3) {

            return """
                ⚠️ Formato incorrecto.

                Escribí /creardeposito y respondé las preguntas.
                """;
        }

        try {

            return logisticaClient.crearDeposito(
                    campos[0].trim(), campos[1].trim(), Integer.valueOf(campos[2].trim()));

        } catch (NumberFormatException e) {

            return "⚠️ La capacidad debe ser un número.";

        }

    }

    private String procesarConsultarDeposito(String text) {

        String[] partes = text.split("\\s+");

        if (partes.length < 2) {

            return """
                ⚠️ Indicá el ID del depósito.

                Ejemplo:
                /deposito 1
                """;
        }

        return logisticaClient.consultarDepositoPorId(partes[1]);

    }

    private String procesarConsultarStock(String text) {

        String[] partes = text.split("\\s+");

        if (partes.length < 2) {

            return """
                ⚠️ Indicá el ID del depósito.

                Ejemplo:
                /stock 1
                """;
        }

        return logisticaClient.consultarStock(partes[1]);

    }

    private String procesarConsultarStockProducto(String text) {

        String[] partes = text.split("\\s+");

        if (partes.length < 2) {

            return """
                ⚠️ Indicá el ID del producto.

                Ejemplo:
                /stockproducto 4
                """;
        }

        return logisticaClient.consultarStockProducto(partes[1]);

    }

    private String procesarVaciarStock(String text) {

        String[] partes = text.split("\\s+");

        if (partes.length < 2) {

            return """
                ⚠️ Indicá el ID del depósito.

                Ejemplo:
                /vaciarstock 2
                """;
        }

        return logisticaClient.vaciarStock(partes[1]);

    }

    private String procesarConfigurarAlgoritmo(String text) {

        String[] campos = extraerArgumentos(text);

        if (campos.length != 2) {

            return """
                ⚠️ Formato incorrecto.

                Escribí /configuraralgoritmo y respondé las preguntas.
                """;
        }

        return logisticaClient.configurarAlgoritmo(campos[0].trim(), campos[1].trim());

    }

    private String procesarConsultarAsignacion(String text) {

        String[] partes = text.split("\\s+");

        if (partes.length < 2) {

            return """
                ⚠️ Indicá el ID del paquete.

                Ejemplo:
                /asignacion 1
                """;
        }

        return logisticaClient.consultarAsignacionPorPaquete(partes[1]);

    }

    // ==================== Donaciones (Nuevos Métodos) ====================
    private String procesarDonar(String text) {
        String[] campos = extraerArgumentos(text);
        if (campos.length != 5) {
            return "❌ Para registrar una donación, escribí `/donar` y respondé las preguntas.";
        }
        try {
            int cantidad = Integer.parseInt(campos[4].trim());
            return donacionesClient.registrarDonacion(
                    campos[0].trim(), campos[1].trim(), campos[2].trim(),
                    campos[3].trim(), cantidad);
        } catch (NumberFormatException e) {
            return "❌ La cantidad tiene que ser un número válido.";
        }
    }

    private String procesarVerDonacion(String text) {
        String[] partes = text.split("\\s+");
        if (partes.length < 2) {
            return "⚠️ Indicá el ID de la donación.\nEjemplo: `/verdonacion 1`";
        }
        return donacionesClient.consultarDonacionPorId(partes[1].trim());
    }

    private String procesarQueja(String text) {
        String[] campos = extraerArgumentos(text);
        if (campos.length != 2) {
            return "❌ Para registrar una queja, escribí `/queja` y respondé las preguntas.";
        }
        return donacionesClient.registrarQueja(campos[0].trim(), campos[1].trim());
    }

    private String procesarCrearCategoria(String text) {
        String[] campos = extraerArgumentos(text);
        if (campos.length != 2) {
            return "❌ Para crear una categoría, escribí `/crearcategoria` y respondé las preguntas.";
        }
        return donacionesClient.crearCategoria(campos[0].trim(), campos[1].trim());
    }

    private String procesarCrearProducto(String text) {
        String[] campos = extraerArgumentos(text);
        if (campos.length != 4) {
            return "❌ Para crear un producto, escribí `/crearproducto` y respondé las preguntas.";
        }
        return donacionesClient.crearProducto(campos[0].trim(), campos[1].trim(), campos[2].trim(), campos[3].trim());
    }

    private String procesarCrearIdentificador(String text) {
        String[] campos = extraerArgumentos(text);
        if (campos.length != 2) {
            return "❌ Para crear un identificador, escribí `/crearidentificador` y respondé las preguntas.";
        }
        // Aplicamos toUpperCase al tipo (ej. qr -> QR) para alinear con el Enum de la base
        return donacionesClient.crearIdentificador(campos[0].trim().toUpperCase(), campos[1].trim());
    }

    private String procesarCrearSubcategoria(String text) {
        String[] campos = extraerArgumentos(text);
        if (campos.length != 2) {
            return "❌ Para crear una subcategoría, escribí `/crearsubcategoria` y respondé las preguntas.";
        }
        return donacionesClient.crearSubcategoria(campos[0].trim(), campos[1].trim());
    }

    private String procesarConsultarSubcategorias(String text) {
        String[] partes = text.split("\\s+");
        if (partes.length < 2) {
            return "⚠️ Indicá el ID de la categoría padre.\nEjemplo: `/subcategorias 1`";
        }
        return donacionesClient.consultarSubcategorias(partes[1].trim());
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

    private String extraerComando(String text) {
        String comando = text.split("\\s+", 2)[0];
        int separadorUsuario = comando.indexOf('@');
        return separadorUsuario >= 0 ? comando.substring(0, separadorUsuario) : comando;
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
