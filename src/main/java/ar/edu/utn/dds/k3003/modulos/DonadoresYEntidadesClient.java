package ar.edu.utn.dds.k3003.modulos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Cliente HTTP hacia el módulo "Donadores y Entidades".
 *
 * Endpoints ya disponibles (según lo que compartió el compañero, 21/08):
 *   GET  /donadores/{id}/estadisticas
 *   POST /donadores
 *   GET  /donadores
 *   GET  /donadores/{id}
 *   POST /entidades
 *   GET  /entidades
 *   GET  /entidades/{id}
 *   POST /necesidades
 *   GET  /necesidades?productoID={id}
 *
 * Endpoints que la consigna pide pero TODAVÍA NO están expuestos del otro lado
 * (quedan como TODO hasta que el compañero los suba):
 *   PUT    /entidades/{id}          (editar entidad)
 *   DELETE /necesidades/{id}        (borrar necesidad)
 *   PUT    /necesidades/{id}        (modificar necesidad)
 *   GET    /necesidades/{id}        (consultar necesidad por ID)
 */
@Component
public class DonadoresYEntidadesClient {

    private final RestClient restClient;

    public DonadoresYEntidadesClient(
        @Value("${donadoresyentidades.url:https://agusb1101-donadores-entidades.onrender.com}") String baseUrl) {
    
    // Le damos una URL segura por si baseUrl llega a venir nulo
    String urlSegura = (baseUrl != null) ? baseUrl : "https://agusb1101-donadores-entidades.onrender.com";
    this.restClient = RestClient.create(urlSegura);
    }

    // ==================== DONADORES ====================

    public String registrarDonador(String nombre, String apellido, Integer edad,
                                    String email, String nroDocumento, String domicilio) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("nombre", nombre);
            body.put("apellido", apellido);
            body.put("edad", edad);
            body.put("email", email);
            body.put("nroDocumento", nroDocumento);
            body.put("domicilio", domicilio);

            JsonNode creado = restClient.post()
                    .uri("/donadores")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String id = (creado != null) ? creado.path("id").asText("(sin id)") : "(sin id)";
            return "✅ ¡Listo, te registramos!\nTu ID de donador es: *" + id + "*\n"
                    + "Guardalo, lo vas a necesitar para consultar tus estadísticas más adelante.";
        } catch (RestClientResponseException e) {
            return "❌ No se pudo completar el registro (HTTP " + e.getStatusCode().value() + "). "
                    + "Revisá los datos ingresados.";
        } catch (Exception e) {
            return "⚠️ No nos pudimos comunicar con el módulo de Donadores y Entidades.";
        }
    }

    public String consultarEstadisticas(String donadorID) {
        try {
            JsonNode root = restClient.get()
                    .uri("/donadores/{id}/estadisticas", donadorID)
                    .retrieve()
                    .body(JsonNode.class);
            return formatearEstadisticas(root);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return "No encontramos a ningún donador con ese ID.";
            }
            return "❌ Error al consultar estadísticas (HTTP " + e.getStatusCode().value() + ").";
        } catch (Exception e) {
            return "⚠️ No nos pudimos comunicar con el módulo de Donadores y Entidades.";
        }
    }

    public String consultarDonadorPorId(String id) {
        try {
            JsonNode donador = restClient.get()
                    .uri("/donadores/{id}", id)
                    .retrieve()
                    .body(JsonNode.class);
            return formatearDonador(donador);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return "No encontramos a ningún donador con ese ID.";
            }
            return "❌ Error al consultar el donador (HTTP " + e.getStatusCode().value() + ").";
        } catch (Exception e) {
            return "⚠️ No nos pudimos comunicar con el módulo de Donadores y Entidades.";
        }
    }

    public String consultarDonadores() {
        try {
            List<JsonNode> lista = restClient.get()
                    .uri("/donadores")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<JsonNode>>() {});

            if (lista == null || lista.isEmpty()) {
                return "Todavía no hay donadores registrados.";
            }
            StringBuilder sb = new StringBuilder("👥 *Donadores registrados:*\n\n");
            for (JsonNode d : lista) {
                sb.append("• ").append(d.path("id").asText("?"))
                  .append(" - ").append(d.path("nombre").asText(""))
                  .append(" ").append(d.path("apellido").asText(""))
                  .append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "⚠️ No nos pudimos comunicar con el módulo de Donadores y Entidades.";
        }
    }

    // ==================== ENTIDADES ====================

    public String crearEntidad(String razonSocial, String domicilio, String telefono, String correo) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("razonSocial", razonSocial);
            body.put("domicilio", domicilio);
            body.put("telefono", telefono);
            body.put("correo", correo);

            JsonNode creada = restClient.post()
                    .uri("/entidades")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String id = (creada != null) ? creada.path("id").asText("(sin id)") : "(sin id)";
            return "✅ Entidad creada correctamente.\nID: *" + id + "*";
        } catch (RestClientResponseException e) {
            return "❌ No se pudo crear la entidad (HTTP " + e.getStatusCode().value() + ").";
        } catch (Exception e) {
            return "⚠️ No nos pudimos comunicar con el módulo de Donadores y Entidades.";
        }
    }

    public String editarEntidad(String id, String razonSocial, String domicilio,
                                 String telefono, String correo) {
        // TODO: falta que el compañero exponga PUT /entidades/{id}.
        return "⚠️ Editar entidad todavía no está disponible: falta el endpoint del lado de "
                + "Donadores y Entidades (PUT /entidades/{id}).";
    }

    public String consultarEntidades() {
        try {
            List<JsonNode> lista = restClient.get()
                    .uri("/entidades")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<JsonNode>>() {});

            if (lista == null || lista.isEmpty()) {
                return "Todavía no hay entidades registradas.";
            }
            StringBuilder sb = new StringBuilder("🏢 *Entidades registradas:*\n\n");
            for (JsonNode e : lista) {
                sb.append("• ").append(e.path("id").asText("?"))
                  .append(" - ").append(e.path("razonSocial").asText(""))
                  .append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "⚠️ No nos pudimos comunicar con el módulo de Donadores y Entidades.";
        }
    }

    public String consultarEntidadPorId(String id) {
        try {
            JsonNode entidad = restClient.get()
                    .uri("/entidades/{id}", id)
                    .retrieve()
                    .body(JsonNode.class);
            return formatearEntidad(entidad);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return "No encontramos ninguna entidad con ese ID.";
            }
            return "❌ Error al consultar la entidad (HTTP " + e.getStatusCode().value() + ").";
        } catch (Exception e) {
            return "⚠️ No nos pudimos comunicar con el módulo de Donadores y Entidades.";
        }
    }

    // ==================== NECESIDADES ====================

    public String crearNecesidad(String entidadID, Integer nivelDeUrgencia, String descripcion,
                                  Integer cantidadObjetivo, String productoSolicitadoID, String tipo) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("entidadID", entidadID);
            body.put("nivelDeUrgencia", nivelDeUrgencia);
            body.put("descripcion", descripcion);
            body.put("cantidadObjetivo", cantidadObjetivo);
            body.put("productoSolicitadoID", productoSolicitadoID);
            body.put("tipo", tipo); // EXTRAORDINARIA o RECURRENTE

            JsonNode creada = restClient.post()
                    .uri("/necesidades")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String id = (creada != null) ? creada.path("id").asText("(sin id)") : "(sin id)";
            return "✅ Necesidad creada correctamente.\nID: *" + id + "*";
        } catch (RestClientResponseException e) {
            return "❌ No se pudo crear la necesidad (HTTP " + e.getStatusCode().value() + "). "
                    + "Revisá que el tipo sea EXTRAORDINARIA o RECURRENTE.";
        } catch (Exception e) {
            return "⚠️ No nos pudimos comunicar con el módulo de Donadores y Entidades.";
        }
    }

    public String consultarNecesidadesPorProducto(String productoID) {
        try {
            List<JsonNode> lista = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/necesidades")
                            .queryParam("productoID", productoID)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<JsonNode>>() {});

            if (lista == null || lista.isEmpty()) {
                return "No hay necesidades cargadas para ese producto.";
            }
            StringBuilder sb = new StringBuilder("📋 *Necesidades para producto " + productoID + ":*\n\n");
            for (JsonNode n : lista) {
                sb.append("• ").append(n.path("id").asText("?"))
                  .append(" - ").append(n.path("descripcion").asText(""))
                  .append(" (objetivo: ").append(n.path("cantidadObjetivo").asText("?")).append(")")
                  .append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "⚠️ No nos pudimos comunicar con el módulo de Donadores y Entidades.";
        }
    }

    public String borrarNecesidad(String id) {
        // TODO: falta que el compañero exponga DELETE /necesidades/{id}.
        return "⚠️ Borrar necesidad todavía no está disponible: falta el endpoint del lado de "
                + "Donadores y Entidades (DELETE /necesidades/{id}).";
    }

    public String modificarNecesidad(String id, String descripcion) {
        // TODO: falta que el compañero exponga PUT /necesidades/{id}.
        return "⚠️ Modificar necesidad todavía no está disponible: falta el endpoint del lado de "
                + "Donadores y Entidades (PUT /necesidades/{id}).";
    }

    public String consultarNecesidadPorId(String id) {
        // TODO: falta que el compañero exponga GET /necesidades/{id} (hoy solo existe por productoID).
        return "⚠️ Consultar necesidad por ID todavía no está disponible: falta el endpoint del lado de "
                + "Donadores y Entidades (GET /necesidades/{id}).";
    }

    // ==================== Formateo ====================

    private String formatearDonador(JsonNode d) {
        if (d == null) {
            return "Donador no encontrado.";
        }
        return "👤 *" + d.path("nombre").asText("") + " " + d.path("apellido").asText("") + "*\n"
                + "ID: " + d.path("id").asText("?") + "\n"
                + "Email: " + d.path("email").asText("-") + "\n"
                + "Estado: " + d.path("estado").asText("-") + "\n"
                + "Categoría: " + d.path("categoria").asText("-");
    }

    private String formatearEstadisticas(JsonNode d) {
        if (d == null) {
            return "No se encontraron estadísticas para ese donador.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("📊 *Estadísticas de ").append(d.path("nombre").asText(""))
          .append(" ").append(d.path("apellido").asText("")).append("*\n\n");
        sb.append("🏆 Categoría: ").append(d.path("categoria").asText("-")).append("\n");
        sb.append("🎯 Misión actual: ").append(
                d.path("misionActualID").isNull() ? "Ninguna" : d.path("misionActualID").asText("Ninguna")
        ).append("\n");

        JsonNode insignias = d.path("insigniasID");
        sb.append("🏅 Insignias: ");
        if (insignias.isArray() && !insignias.isEmpty()) {
            List<String> ids = new ArrayList<>();
            insignias.forEach(i -> ids.add(i.asText()));
            sb.append(String.join(", ", ids));
        } else {
            sb.append("ninguna todavía");
        }
        return sb.toString();
    }

    private String formatearEntidad(JsonNode e) {
        if (e == null) {
            return "Entidad no encontrada.";
        }
        return "🏢 *" + e.path("razonSocial").asText("") + "*\n"
                + "ID: " + e.path("id").asText("?") + "\n"
                + "Domicilio: " + e.path("domicilio").asText("-") + "\n"
                + "Teléfono: " + e.path("telefono").asText("-") + "\n"
                + "Correo: " + e.path("correo").asText("-");
    }
}
