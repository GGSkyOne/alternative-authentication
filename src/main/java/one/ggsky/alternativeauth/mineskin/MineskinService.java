package one.ggsky.alternativeauth.mineskin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import one.ggsky.alternativeauth.config.MineSkinConfig;
import one.ggsky.alternativeauth.logger.AlternativeAuthLogger;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerManager;
import one.ggsky.alternativeauth.skin.SkinVariant;
import one.ggsky.alternativeauth.util.AlternativeAuthUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

public class MineskinService {
    private static final AlternativeAuthLogger LOGGER = AlternativeAuthLoggerManager.getLogger();
    private static final MineskinService INSTANCE = new MineskinService();
    private static final String QUEUE_URL = "https://api.mineskin.org/v2/queue";
    private static final String USER_AGENT = "AlternativeAuthentication";

    private final HttpClient httpClient;

    private MineskinService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public static MineskinService getInstance() {
        return INSTANCE;
    }

    public Optional<Property> signSkin(MineSkinConfig config, String skinUrl, SkinVariant variant) throws Exception {
        String targetUrl = skinUrl;

        if (config.isProxyUrlUpload()) {
            targetUrl = downloadAsDataUrl(skinUrl);
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("url", targetUrl);
        requestBody.addProperty("name", "alternative-auth-skin");
        requestBody.addProperty("variant", variant == SkinVariant.SLIM ? "slim" : "classic");
        requestBody.addProperty("visibility", "unlisted");

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(QUEUE_URL))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(AlternativeAuthUtils.GSON.toJson(requestBody)));

        String apiKey = config.getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey.trim());
        }

        HttpResponse<String> response = this.httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            JsonObject json = AlternativeAuthUtils.GSON.fromJson(response.body(), JsonObject.class);
            return extractSkinPropertyFromResponse(json, apiKey);
        } else {
            LOGGER.warn("MineSkin request failed with status " + response.statusCode() + ": " + response.body());
            return Optional.empty();
        }
    }

    private Optional<Property> extractSkinPropertyFromResponse(JsonObject json, String apiKey) throws Exception {
        if (json.has("texture")) {
            return extractFromTextureObject(json.getAsJsonObject("texture"));
        }
        if (json.has("skin") && json.get("skin").isJsonObject()) {
            JsonObject skinObj = json.getAsJsonObject("skin");
            if (skinObj.has("texture")) {
                return extractFromTextureObject(skinObj.getAsJsonObject("texture"));
            }
        }

        if (json.has("job") && json.get("job").isJsonObject()) {
            JsonObject job = json.getAsJsonObject("job");
            if (job.has("id")) {
                String jobId = job.get("id").getAsString();
                return pollJob(jobId, apiKey);
            }
        }

        return Optional.empty();
    }

    private Optional<Property> pollJob(String jobId, String apiKey) throws Exception {
        String jobUrl = QUEUE_URL + "/" + jobId;
        long startTime = System.currentTimeMillis();
        long maxWaitTimeMs = 15000;

        while (System.currentTimeMillis() - startTime < maxWaitTimeMs) {
            Thread.sleep(1500);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(jobUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .GET();

            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey.trim());
            }

            HttpResponse<String> response = this.httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject json = AlternativeAuthUtils.GSON.fromJson(response.body(), JsonObject.class);
                if (json.has("job") && json.get("job").isJsonObject()) {
                    JsonObject job = json.getAsJsonObject("job");
                    String status = job.has("status") ? job.get("status").getAsString() : "";
                    if ("completed".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status)) {
                        if (job.has("skin")) {
                            JsonElement skinEl = job.get("skin");
                            if (skinEl.isJsonObject()) {
                                return extractSkinPropertyFromResponse(skinEl.getAsJsonObject(), apiKey);
                            } else if (skinEl.isJsonPrimitive()) {
                                return fetchSkinById(skinEl.getAsString(), apiKey);
                            }
                        }
                    } else if ("failed".equalsIgnoreCase(status)) {
                        LOGGER.warn("MineSkin job " + jobId + " failed");
                        return Optional.empty();
                    }
                } else if (json.has("texture")) {
                    return extractFromTextureObject(json.getAsJsonObject("texture"));
                }
            }
        }

        LOGGER.warn("MineSkin job " + jobId + " timed out");
        return Optional.empty();
    }

    private Optional<Property> fetchSkinById(String skinId, String apiKey) throws Exception {
        String skinUrl = "https://api.mineskin.org/v2/skins/" + skinId;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(skinUrl))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET();

        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey.trim());
        }

        HttpResponse<String> response = this.httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonObject json = AlternativeAuthUtils.GSON.fromJson(response.body(), JsonObject.class);
            return extractSkinPropertyFromResponse(json, apiKey);
        }
        return Optional.empty();
    }

    private Optional<Property> extractFromTextureObject(JsonObject texture) {
        if (texture.has("data") && texture.get("data").isJsonObject()) {
            JsonObject data = texture.getAsJsonObject("data");
            if (data.has("value") && data.has("signature")) {
                String value = data.get("value").getAsString();
                String signature = data.get("signature").getAsString();
                return Optional.of(new Property("textures", value, signature));
            }
        }
        return Optional.empty();
    }

    private String downloadAsDataUrl(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<byte[]> res = this.httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200) {
            throw new IOException("Failed to download skin image: HTTP " + res.statusCode());
        }
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(res.body());
    }
}
