package ar.edu.utn.dds.k3003.modulos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class LogisticaClient {

    private final RestClient restClient;

    public LogisticaClient(@Value("${logistica.url:https://logistica-hjaw.onrender.com}") String baseUrl) {

        String urlSegura = (baseUrl != null) ? baseUrl : "https://logistica-hjaw.onrender.com";

        this.restClient = RestClient.create(urlSegura);
    }

    // ==================== CONSULTAS ====================

    public String consultarDepositos() {

        try {

            return restClient.get().uri("/depositos").retrieve().body(String.class);

        } catch (Exception e) {

            return "⚠️ No se pudo consultar Logística.";
        }
    }

    public String consultarDepositoPorId(String id) {

        try {

            return restClient.get().uri("/depositos/{id}", id).retrieve().body(String.class);

        } catch (RestClientResponseException e) {

            if (e.getStatusCode().value() == 404) {

                return "No existe un depósito con ese ID.";
            }

            return "❌ Error consultando el depósito.";
        }
    }

    public String consultarStock(String depositoID) {

        try {

            return restClient.get().uri("/depositos/{id}/stock", depositoID).retrieve().body(String.class);

        } catch (Exception e) {

            return "❌ No se pudo consultar el stock.";
        }
    }

    public String consultarAsignaciones() {

        try {

            return restClient.get().uri("/asignaciones").retrieve().body(String.class);

        } catch (Exception e) {

            return "❌ No se pudieron consultar las asignaciones.";
        }
    }

    public String consultarAsignacionesPorEstado(String estado) {

        try {

            return restClient.get().uri("/asignaciones/estado/{estado}", estado).retrieve().body(String.class);

        } catch (Exception e) {

            return "❌ No se pudieron consultar las asignaciones.";
        }
    }

    public String consultarAsignacionPorPaquete(String paqueteID) {

        try {

            return restClient.get().uri("/asignaciones/{id}", paqueteID).retrieve().body(String.class);

        } catch (Exception e) {

            return "❌ No se pudo consultar la asignación.";
        }
    }

    public String consultarStockProducto(String productoID) {

        try {

            return restClient.get().uri("/depositos/stock/{productoID}", productoID).retrieve().body(String.class);

        } catch (Exception e) {

            return "❌ No se pudo consultar el stock del producto.";
        }
    }

    // ==================== ALTAS ====================

    public String crearDeposito(String nombre, String direccion, Integer capacidadMaxima) {

        try {

            Map<String, Object> body = Map.of("nombre", nombre, "direccion", direccion, "capacidadMaxima", capacidadMaxima);

            return restClient.post().uri("/depositos").body(body).retrieve().body(String.class);

        } catch (Exception e) {

            return "❌ No se pudo crear el depósito.";
        }
    }

    // ==================== MODIFICACIONES ====================

    public String configurarAlgoritmo(String depositoID, String algoritmo) {

        try {

            Map<String, String> body = Map.of("algoritmo", algoritmo.toUpperCase());

            restClient.patch().uri("/depositos/{id}/algoritmo", depositoID).body(body).retrieve().toBodilessEntity();

            return "✅ Algoritmo configurado correctamente.";

        } catch (Exception e) {

            return "❌ No se pudo configurar el algoritmo.";
        }
    }

    // ==================== BAJAS ====================

    public String eliminarDepositos() {

        try {

            restClient.delete().uri("/depositos").retrieve().toBodilessEntity();

            return "🗑️ Depósitos eliminados correctamente.";

        } catch (Exception e) {

            return "❌ No se pudieron eliminar los depósitos.";
        }
    }

    public String eliminarAsignaciones() {

        try {

            restClient.delete().uri("/asignaciones").retrieve().toBodilessEntity();

            return "🗑️ Asignaciones eliminadas correctamente.";

        } catch (Exception e) {

            return "❌ No se pudieron eliminar las asignaciones.";
        }
    }

    public String eliminarPaquetes() {

        try {

            restClient.delete().uri("/depositos/stock").retrieve().toBodilessEntity();

            return "🗑️ Paquetes eliminados correctamente.";

        } catch (Exception e) {

            return "❌ No se pudieron eliminar los paquetes.";
        }
    }

    public String vaciarStock(String depositoID) {

        try {

            restClient.delete().uri("/depositos/{id}/stock", depositoID).retrieve().toBodilessEntity();

            return "🗑️ Stock vaciado correctamente.";

        } catch (Exception e) {

            return "❌ No se pudo vaciar el stock.";
        }
    }
}