package ar.edu.utn.dds.k3003.modulos;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DonacionesClient {
    private final RestClient restClient;

    public DonacionesClient(@Value("${donaciones.url:https://donaciones-5u8i.onrender.com}") String baseUrl) {
        String urlSegura = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : "https://donaciones-5u8i.onrender.com";
        this.restClient = RestClient.create(urlSegura);
    }

    public String registrarDonacion(String donadorID, String depositoID, String descripcion, String productoID, Integer cantidad) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("donadorID", donadorID);
            body.put("depositoID", depositoID);
            body.put("descripcion", descripcion);
            body.put("productoID", productoID);
            body.put("cantidad", cantidad);

            JsonNode creada = restClient.post()
                    .uri("/donaciones")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String id = (creada != null && creada.has("id")) ? creada.path("id").asText("(sin id)") : "(sin id)";
            return "✅ Donación registrada correctamente.\nID: *" + id + "*\nEstado: INGRESADA";
        } catch (RestClientResponseException e) {
            return "❌ Error al registrar la donación (HTTP " + e.getStatusCode().value() + "). Revisa los datos ingresados.";
        } catch (Exception e) {
            return "❌ Error inesperado al comunicarse con Donaciones.";
        }
    }

    public String consultarDonaciones() {
        try {
            List<JsonNode> lista = restClient.get()
                    .uri("/donaciones")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<JsonNode>>() {});

            if (lista == null || lista.isEmpty()) {
                return "No hay donaciones registradas.";
            }

            StringBuilder sb = new StringBuilder("📦 *Donaciones registradas:*\n\n");
            for (JsonNode d : lista) {
                sb.append("🔹 ID: ").append(d.path("id").asText("?")).append("\n")
                        .append("   Donador: ").append(d.path("donadorID").asText("-")).append("\n")
                        .append("   Producto: ").append(d.path("productoID").asText("-")).append("\n")
                        .append("   Cantidad: ").append(d.path("cantidad").asText("-")).append("\n")
                        .append("   Estado: ").append(d.path("estado").asText("-")).append("\n\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "❌ Error inesperado al comunicarse con Donaciones.";
        }
    }

    public String consultarDonacionPorId(String id) {
        try {
            JsonNode d = restClient.get()
                    .uri("/donaciones/{id}", id)
                    .retrieve()
                    .body(JsonNode.class);

            if (d == null) return "Donación no encontrada.";

            return "📦 *Detalle de Donación*\n"
                    + "ID: " + d.path("id").asText("?") + "\n"
                    + "Donador: " + d.path("donadorID").asText("-") + "\n"
                    + "Depósito: " + d.path("depositoID").asText("-") + "\n"
                    + "Producto: " + d.path("productoID").asText("-") + "\n"
                    + "Cantidad: " + d.path("cantidad").asText("-") + "\n"
                    + "Estado: *" + d.path("estado").asText("-") + "*";
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return "No encontramos ninguna donación con ese ID.";
            }
            return "❌ Error al consultar la donación (HTTP " + e.getStatusCode().value() + ").";
        } catch (Exception e) {
            return "❌ Error inesperado al comunicarse con Donaciones.";
        }
    }

    public String registrarQueja(String id, String descripcion) {
        try {
            Map<String, String> body = Map.of("descripcion", descripcion);
            JsonNode actualizada = restClient.post()
                    .uri("/donaciones/{id}/queja", id)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String estado = (actualizada != null) ? actualizada.path("estado").asText("") : "";
            return "⚠️ Queja registrada para la donación *" + id + "*. Nuevo estado: *" + estado + "*";
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return "No encontramos ninguna donación con ese ID.";
            }
            return "❌ Error al registrar la queja (HTTP " + e.getStatusCode().value() + ").";
        } catch (Exception e) {
            return "❌ Error inesperado al comunicarse con Donaciones.";
        }
    }

    public String crearCategoria(String nombre, String descripcion) {
        try {
            Map<String, String> body = Map.of("nombre", nombre, "descripcion", descripcion);
            JsonNode creada = restClient.post().uri("/categorias").body(body).retrieve().body(JsonNode.class);
            if (creada == null) {
                return "❌ La categoría se creó, pero la API no devolvió sus datos.";
            }
            return "✅ Categoría creada. ID: *" + creada.path("id").asText() + "*";
        } catch (Exception e) {
            return "❌ Error al crear la categoría.";
        }
    }

    public String consultarCategorias() {
        try {
            List<JsonNode> lista = restClient.get().uri("/categorias").retrieve()
                    .body(new ParameterizedTypeReference<List<JsonNode>>() {});
            if (lista == null || lista.isEmpty()) return "No hay categorías registradas.";
            StringBuilder sb = new StringBuilder("📁 *Categorías:*\n");
            for (JsonNode c : lista) {
                sb.append("ID: ").append(c.path("id").asText()).append(" - ").append(c.path("nombre").asText()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "❌ Error al consultar categorías.";
        }
    }

    public String crearProducto(String nombre, String descripcion, String subcategoriaID, String identificadorID) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("nombre", nombre);
            body.put("descripcion", descripcion);
            body.put("subcategoriaID", subcategoriaID);
            body.put("identificadorID", identificadorID);

            JsonNode creado = restClient.post().uri("/productos").body(body).retrieve().body(JsonNode.class);
            if (creado == null) {
                return "❌ El producto se creó, pero la API no devolvió sus datos.";
            }
            return "✅ Producto creado. ID: *" + creado.path("id").asText() + "*";
        } catch (RestClientResponseException e) {
            return "❌ Error al crear producto (HTTP " + e.getStatusCode().value() + "). Verificá IDs de subcategoría e identificador.";
        } catch (Exception e) {
            return "❌ Error inesperado al comunicarse con Donaciones.";
        }
    }

    public String consultarProductos() {
        try {
            List<JsonNode> lista = restClient.get().uri("/productos").retrieve().body(new ParameterizedTypeReference<List<JsonNode>>() {
            });
            if (lista == null || lista.isEmpty()) return "No hay productos registrados.";
            StringBuilder sb = new StringBuilder("🛒 *Productos:*\n");
            for (JsonNode p : lista) {
                sb.append("ID: ").append(p.path("id").asText())
                        .append(" - ").append(p.path("nombre").asText())
                        .append(" (SubCat: ").append(p.path("subcategoriaID").asText()).append(")\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "❌ Error al consultar productos.";
        }
    }

    public String crearIdentificador(String tipo, String descripcion) {
        try {
            Map<String, String> body = Map.of("tipo", tipo, "descripcion", descripcion);
            JsonNode creado = restClient.post().uri("/identificadores").body(body).retrieve().body(JsonNode.class);
            if (creado == null) {
                return "❌ El identificador se creó, pero la API no devolvió sus datos.";
            }
            return "✅ Identificador creado. ID: *" + creado.path("id").asText() + "*";
        } catch (RestClientResponseException e) {
            return "❌ Error al crear identificador (HTTP " + e.getStatusCode().value() + "). Verificá que el tipo sea válido (ej: QR, CODIGODEBARRAS).";
        } catch (Exception e) {
            return "❌ Error inesperado al comunicarse con Donaciones.";
        }
    }

    public String consultarIdentificadores() {
        try {
            List<JsonNode> lista = restClient.get().uri("/identificadores").retrieve()
                    .body(new ParameterizedTypeReference<List<JsonNode>>() {});
            if (lista == null || lista.isEmpty()) return "No hay identificadores registrados.";
            StringBuilder sb = new StringBuilder("🏷️ *Identificadores:*\n");
            for (JsonNode i : lista) {
                sb.append("ID: ").append(i.path("id").asText())
                        .append(" - Tipo: ").append(i.path("tipo").asText())
                        .append(" (").append(i.path("descripcion").asText()).append(")\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "❌ Error al consultar identificadores.";
        }
    }

    public String crearSubcategoria(String nombre, String categoriaID) {
        try {
            Map<String, String> body = Map.of("nombre", nombre);
            JsonNode creada = restClient.post()
                    .uri("/categorias/{categoriaID}/subcategorias", categoriaID)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (creada == null) {
                return "❌ La subcategoría se creó, pero la API no devolvió sus datos.";
            }
            return "✅ Subcategoría creada. ID: *" + creada.path("id").asText() + "*";
        } catch (RestClientResponseException e) {
            return "❌ Error al crear subcategoría (HTTP " + e.getStatusCode().value() + "). Verificá que la categoría padre exista.";
        } catch (Exception e) {
            return "❌ Error inesperado al comunicarse con Donaciones.";
        }
    }

    public String consultarSubcategorias(String categoriaID) {
        try {
            List<JsonNode> lista = restClient.get()
                    .uri("/categorias/{id}/subcategorias", categoriaID)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<JsonNode>>() {});
            if (lista == null || lista.isEmpty()) return "No hay subcategorías registradas para esa categoría.";
            StringBuilder sb = new StringBuilder("📁 *Subcategorías (Categoría Padre " + categoriaID + "):*\n");
            for (JsonNode s : lista) {
                sb.append("ID: ").append(s.path("id").asText())
                        .append(" - Nombre: ").append(s.path("nombre").asText()).append("\n");
            }
            return sb.toString();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) return "No encontramos esa categoría padre.";
            return "❌ Error al consultar subcategorías (HTTP " + e.getStatusCode().value() + ").";
        } catch (Exception e) {
            return "❌ Error inesperado al comunicarse con Donaciones.";
        }
    }
}