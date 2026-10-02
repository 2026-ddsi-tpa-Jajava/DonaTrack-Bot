package ar.edu.utn.dds.k3003.bot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class SessionManager {

    public record Sesion(String ultimoComando, String rol, Instant ultimaActividad) {
    }

    public record RegistroDonador(int paso, List<String> campos) {
        public RegistroDonador {
            campos = List.copyOf(campos);
        }
    }

    public record Formulario(String comando, int paso, List<String> campos) {
        public Formulario {
            campos = List.copyOf(campos);
        }
    }

    private final ConcurrentHashMap<Long, Sesion> sesiones = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, RegistroDonador> registrosDonadores = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Formulario> formularios = new ConcurrentHashMap<>();

    public void registrarInteraccion(Long chatId, String comando, String rol) {
        sesiones.put(chatId, new Sesion(comando, rol, Instant.now()));
    }

    public Sesion obtener(Long chatId) {
        return sesiones.get(chatId);
    }

    public void iniciarRegistroDonador(Long chatId) {
        registrosDonadores.put(chatId, new RegistroDonador(0, List.of()));
    }

    public RegistroDonador obtenerRegistroDonador(Long chatId) {
        return registrosDonadores.get(chatId);
    }

    public RegistroDonador guardarRespuestaRegistro(Long chatId, String respuesta) {
        RegistroDonador actual = registrosDonadores.get(chatId);
        if (actual == null) {
            return null;
        }

        List<String> campos = new ArrayList<>(actual.campos());
        campos.add(respuesta);
        RegistroDonador actualizado = new RegistroDonador(actual.paso() + 1, campos);
        registrosDonadores.put(chatId, actualizado);
        return actualizado;
    }

    public void cancelarRegistroDonador(Long chatId) {
        registrosDonadores.remove(chatId);
    }

    public void iniciarFormulario(Long chatId, String comando) {
        formularios.put(chatId, new Formulario(comando, 0, List.of()));
    }

    public Formulario obtenerFormulario(Long chatId) {
        return formularios.get(chatId);
    }

    public Formulario guardarRespuestaFormulario(Long chatId, String respuesta) {
        Formulario actual = formularios.get(chatId);
        if (actual == null) {
            return null;
        }
        List<String> campos = new ArrayList<>(actual.campos());
        campos.add(respuesta);
        Formulario actualizado = new Formulario(actual.comando(), actual.paso() + 1, campos);
        formularios.put(chatId, actualizado);
        return actualizado;
    }

    public void cancelarFormulario(Long chatId) {
        formularios.remove(chatId);
    }
}
