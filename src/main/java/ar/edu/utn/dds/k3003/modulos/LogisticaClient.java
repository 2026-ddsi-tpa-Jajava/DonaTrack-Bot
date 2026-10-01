package ar.edu.utn.dds.k3003.modulos;

import com.fasterxml.jackson.databind.JsonNode;
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

            JsonNode lista = restClient.get().uri("/depositos").retrieve().body(JsonNode.class);

            if (lista == null || !lista.isArray() || lista.isEmpty()) {

                return "🏢 No hay depósitos registrados.";
            }

            StringBuilder sb = new StringBuilder("🏢 *Depósitos registrados*\n\n");

            for (JsonNode deposito : lista) {

                int cantidadStock = 0;

                JsonNode stockActual = deposito.path("stockActual");

                if (stockActual.isArray()) {

                    for (JsonNode paquete : stockActual) {

                        cantidadStock += paquete.path("cantidad").asInt();
                    }
                }

                String algoritmo = deposito.path("algoritmo").isNull() ? "Sub Atendidos (por defecto)" : deposito.path("algoritmo").asText("-");

                algoritmo = switch (algoritmo) {

                    case "SUB_ATENDIDOS" -> "Sub Atendidos";

                    case "PRIORIDAD_POR_SCORE" -> "Prioridad por Score";

                    default -> algoritmo;
                };

                sb.append("• ID: ").append(deposito.path("id").asText("?")).append("\n");

                sb.append("  Nombre: ").append(deposito.path("nombre").asText("-")).append("\n");

                sb.append("  Dirección: ").append(deposito.path("direccion").asText("-")).append("\n");

                sb.append("  Capacidad: ").append(deposito.path("capacidadMaxima").asText("-")).append("\n");

                sb.append("  Algoritmo: ").append(algoritmo).append("\n");

                sb.append("  Stock: ").append(cantidadStock).append("\n\n");
            }

            return sb.toString();

        } catch (Exception e) {

            return "⚠️ No se pudo consultar Logística.";
        }
    }

    public String consultarDepositoPorId(String id) {

        try {

            JsonNode deposito = restClient.get().uri("/depositos/{id}", id).retrieve().body(JsonNode.class);

            int cantidadStock = 0;

            JsonNode stockActual = deposito.path("stockActual");

            if (stockActual.isArray()) {

                for (JsonNode paquete : stockActual) {

                    cantidadStock += paquete.path("cantidad").asInt();
                }
            }

            String algoritmo = deposito.path("algoritmo").isNull() ? "Sub Atendidos (por defecto)" : deposito.path("algoritmo").asText("-");

            algoritmo = switch (algoritmo) {

                case "SUB_ATENDIDOS" -> "Sub Atendidos";

                case "PRIORIDAD_POR_SCORE" -> "Prioridad por Score";

                default -> algoritmo;
            };

            return """
                🏢 *Depósito*
                
                ID: %s
                Nombre: %s
                Dirección: %s
                Capacidad máxima: %s
                Algoritmo: %s
                Cantidad de stock: %s
                """.formatted(
                    deposito.path("id").asText("-"),
                    deposito.path("nombre").asText("-"),
                    deposito.path("direccion").asText("-"),
                    deposito.path("capacidadMaxima").asText("-"),
                    algoritmo,
                    cantidadStock
            );

        } catch (RestClientResponseException e) {

            if (e.getStatusCode().value() == 404) {

                return "❌ No existe un depósito con ese ID.";
            }

            return "❌ Error consultando el depósito.";
        }
    }

    public String consultarStock(String depositoID) {

        try {

            JsonNode stock = restClient.get().uri("/depositos/{id}/stock", depositoID).retrieve().body(JsonNode.class);

            if (stock == null || !stock.isArray() || stock.isEmpty()) {

                return "📦 El depósito no tiene stock.";

            }

            StringBuilder sb = new StringBuilder("📦 *Stock del depósito*\n\n");

            for (JsonNode paquete : stock) {

                sb.append("• Producto: ").append(paquete.path("producto").asText("-")).append("\n");

                sb.append("  Cantidad: ").append(paquete.path("cantidad").asText("-")).append("\n");

                sb.append("  Donación: ").append(paquete.path("donacionID").asText("-")).append("\n\n");
            }

            return sb.toString();

        } catch (Exception e) {

            return "❌ No se pudo consultar el stock.";
        }
    }

    public String consultarAsignaciones() {

        try {

            JsonNode lista = restClient.get().uri("/asignaciones").retrieve().body(JsonNode.class);

            if (lista == null || !lista.isArray() || lista.isEmpty()) {

                return "📦 No hay asignaciones registradas.";
            }

            StringBuilder sb = new StringBuilder("📦 Asignaciones registradas\n\n");

            for (JsonNode asignacion : lista) {

                sb.append("• ID: ").append(asignacion.path("id").asText("-")).append("\n");

                sb.append("  Paquete: ").append(asignacion.path("paqueteID").asText("-")).append("\n");

                sb.append("  Necesidad: ").append(asignacion.path("necesidadID").asText("-")).append("\n");

                sb.append("  Estado: ").append(asignacion.path("estado").asText("-")).append("\n\n");
            }

            return sb.toString();

        } catch (Exception e) {

            return "❌ No se pudieron consultar las asignaciones.";
        }
    }

    public String consultarAsignacionesPorEstado(String estado) {

        try {

            JsonNode lista = restClient.get().uri("/asignaciones/estado/{estado}", estado).retrieve().body(JsonNode.class);

            if (lista == null || !lista.isArray() || lista.isEmpty()) {

                return "📦 No hay asignaciones en estado " + estado + ".";
            }

            StringBuilder sb = new StringBuilder("📦 Asignaciones " + estado + "\n\n");

            for (JsonNode asignacion : lista) {

                sb.append("• ID: ").append(asignacion.path("id").asText("-")).append("\n");

                sb.append("  Paquete: ").append(asignacion.path("paqueteID").asText("-")).append("\n");

                sb.append("  Necesidad: ").append(asignacion.path("necesidadID").asText("-")).append("\n\n");
            }

            return sb.toString();

        } catch (Exception e) {

            return "❌ No se pudieron consultar las asignaciones.";
        }
    }

    public String consultarAsignacionPorPaquete(String paqueteID) {

        try {

            JsonNode asignacion = restClient.get().uri("/asignaciones/{id}", paqueteID).retrieve().body(JsonNode.class);

            return """
                📦 Asignación
                
                ID: %s
                Paquete: %s
                Necesidad: %s
                Estado: %s
                """.formatted(
                    asignacion.path("id").asText("-"),
                    asignacion.path("paqueteID").asText("-"),
                    asignacion.path("necesidadID").asText("-"),
                    asignacion.path("estado").asText("-")
            );

        } catch (Exception e) {

            return "❌ No se pudo consultar la asignación.";
        }
    }

    public String consultarStockProducto(
            String productoID) {

        try {

            String cantidad = restClient.get().uri("/depositos/stock/{productoID}", productoID).retrieve().body(String.class);

            return """
                📦 Stock disponible
                
                Producto: %s
                Cantidad total: %s
                """.formatted(
                    productoID,
                    cantidad
            );

        } catch (Exception e) {

            return "❌ No se pudo consultar el stock del producto.";
        }
    }

    // ==================== ALTAS ====================

    public String crearDeposito(String nombre, String direccion, Integer capacidadMaxima) {

        try {

            Map<String, Object> body = Map.of("nombre", nombre, "direccion", direccion, "capacidadMaxima", capacidadMaxima);

            JsonNode deposito = restClient.post().uri("/depositos").body(body).retrieve().body(JsonNode.class);

            return """
                ✅ Depósito creado
                
                ID: %s
                Nombre: %s
                Dirección: %s
                Capacidad: %s
                """.formatted(
                    deposito.path("id").asText("-"),
                    deposito.path("nombre").asText("-"),
                    deposito.path("direccion").asText("-"),
                    deposito.path("capacidadMaxima").asText("-")
            );

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