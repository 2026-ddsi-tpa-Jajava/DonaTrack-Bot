package ar.edu.utn.dds.k3003.bot;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class SessionManager {

    public record Sesion(String ultimoComando, String rol, Instant ultimaActividad) {
    }

    private final ConcurrentHashMap<Long, Sesion> sesiones = new ConcurrentHashMap<>();

    public void registrarInteraccion(Long chatId, String comando, String rol) {
        sesiones.put(chatId, new Sesion(comando, rol, Instant.now()));
    }

    public Sesion obtener(Long chatId) {
        return sesiones.get(chatId);
    }
}
