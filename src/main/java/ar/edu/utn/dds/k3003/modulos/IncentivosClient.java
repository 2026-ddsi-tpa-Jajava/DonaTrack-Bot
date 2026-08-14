package ar.edu.utn.dds.k3003.modulos;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class IncentivosClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public IncentivosClient(@Value("${incentivos.url:https://incentivos-yuse.onrender.com}") String baseUrl) {
        String urlValida = (baseUrl != null) ? baseUrl : "https://incentivos-yuse.onrender.com";
        this.restClient = RestClient.create(urlValida);
        this.objectMapper = new ObjectMapper();
    }

    public String consultarEstado(String donadorID) {
        try {
            String jsonBody = restClient.get()
                    .uri("/donadores/{id}/estado", donadorID)
                    .retrieve()
                    .body(String.class);

            return formatearMensaje(jsonBody);

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return "No encontramos estadísticas para ese donador.";
            }
            return "Error al consultar la API de Incentivos: " + e.getStatusCode().value();
        } catch (Exception e) {
            return "No nos pudimos comunicar con el módulo de Incentivos.";
        }
    }

    private String formatearMensaje(String jsonBody) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            
            String categoria = root.path("categoria").asText("Sin categoría");
            JsonNode misionNode = root.path("misionActualID");
            String mision = (misionNode.isNull() || misionNode.isMissingNode()) ? "Ninguna" : misionNode.asText();
            
            StringBuilder sb = new StringBuilder();
            sb.append("🏆 *Categoría:* ").append(categoria).append("\n");
            sb.append("🎯 *Misión Activa:* ").append(mision).append("\n\n");
            
            JsonNode insigniasNode = root.path("insignias");
            sb.append("🏅 *Insignias obtenidas:*");
            
            if (insigniasNode.isArray() && !insigniasNode.isEmpty()) {
                sb.append("\n\n");
                
                List<String> listaInsignias = new ArrayList<>();
                for (JsonNode insignia : insigniasNode) {
                    listaInsignias.add(insignia.asText());
                }

                // Orden numérico (ins-1, ins-2, etc.)
                listaInsignias.sort((a, b) -> {
                    try {
                        int numA = Integer.parseInt(a.replaceAll("\\D+", ""));
                        int numB = Integer.parseInt(b.replaceAll("\\D+", ""));
                        return Integer.compare(numA, numB);
                    } catch (NumberFormatException e) {
                        return a.compareTo(b);
                    }
                });

                for (String insigniaId : listaInsignias) {
                    sb.append("• ").append(obtenerDetalleInsignia(insigniaId)).append("\n\n");
                }
            } else {
                sb.append("\nNinguna todavía.\n");
            }
            
            return sb.toString().trim();
        } catch (JsonProcessingException e) {
            return jsonBody;
        }
    }

    private String obtenerDetalleInsignia(String id) {
        // Extrae el número del ID (ejemplo: "ins-1" -> "1")
        String numero = id.replaceAll("\\D+", "");
        if (numero.isBlank()) {
            numero = id;
        }

        try {
            JsonNode insigniaJson = restClient.get()
                    .uri("/insignias/{id}", id)
                    .retrieve()
                    .body(JsonNode.class);

            if (insigniaJson != null) {
                String nombre = insigniaJson.path("nombre").asText(id);
                String descripcion = insigniaJson.path("descripcion").asText("");
                
                if (!descripcion.isBlank()) {
                    return "*Insignia " + numero + ":* " + nombre + "\n  ↳ _" + descripcion + "_";
                }
                return "*Insignia " + numero + ":* " + nombre;
            }
        } catch (Exception e) {
            return "*Insignia " + numero + ":* " + id;
        }
        return "*Insignia " + numero + ":* " + id;
    }
}