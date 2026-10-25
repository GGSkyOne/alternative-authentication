package one.ggsky.alternativeauth.mineskin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import one.ggsky.alternativeauth.config.MineSkinConfig;
import one.ggsky.alternativeauth.logger.AlternativeAuthLogger;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerManager;
import one.ggsky.alternativeauth.skin.SkinVariant;
import one.ggsky.alternativeauth.util.AlternativeAuthUtils;
import one.ggsky.alternativeauth.util.SkinUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MineskinService {
    private static final AlternativeAuthLogger LOGGER = AlternativeAuthLoggerManager.getLogger();
    private static final MineskinService INSTANCE = new MineskinService();
    private static final String QUEUE_URL = "https://api.mineskin.org/v2/queue";
    private static final String CAPES_URL = "https://api.mineskin.org/v2/capes";
    private static final String USER_AGENT = "AlternativeAuthentication";
    private static final long CAPES_CACHE_DURATION_MS = TimeUnit.HOURS.toMillis(12);

    private final HttpClient httpClient;
    private List<MineSkinCape> cachedCapes = null;
    private final Map<String, String> cachedCapeImageHashes = new HashMap<>();
    private long lastCapesFetchTime = 0;

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
        return signSkin(config, skinUrl, variant, null);
    }

    public Optional<Property> signSkin(MineSkinConfig config, String skinUrl, SkinVariant variant, String capeUuid) throws Exception {
        String targetUrl = skinUrl;
        if (targetUrl == null || targetUrl.isBlank()) {
            return Optional.empty();
        }

        if (config.isProxyUrlUpload()) {
            targetUrl = downloadAsDataUrl(targetUrl);
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("url", targetUrl);
        requestBody.addProperty("name", "alternative-auth-skin");
        requestBody.addProperty("variant", variant == SkinVariant.SLIM ? "slim" : "classic");
        requestBody.addProperty("visibility", "unlisted");
        if (capeUuid != null && !capeUuid.isBlank()) {
            requestBody.addProperty("cape", capeUuid.trim());
        }

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
            Optional<Property> property = extractSkinPropertyFromResponse(json, apiKey);
            if (capeUuid != null && !capeUuid.isBlank()) {
                if (property.isPresent() && SkinUtils.getCapeUrl(property.get()) != null) {
                    LOGGER.info("MineSkin returned a signed cape texture for " + capeUuid);
                } else {
                    LOGGER.warn("MineSkin returned a signed skin without the requested cape " + capeUuid
                            + ". Check that the API key can use this cape.");
                }
            }
            return property;
        } else {
            LOGGER.warn("MineSkin request failed with status " + response.statusCode() + ": " + response.body());
            return Optional.empty();
        }
    }

    public synchronized List<MineSkinCape> getCapes(MineSkinConfig config) {
        long now = System.currentTimeMillis();
        if (cachedCapes != null && (now - lastCapesFetchTime < CAPES_CACHE_DURATION_MS)) {
            return cachedCapes;
        }

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(CAPES_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .GET();

            String apiKey = config.getApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey.trim());
            }

            HttpResponse<String> response = this.httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject json = AlternativeAuthUtils.GSON.fromJson(response.body(), JsonObject.class);
                if (json.has("capes") && json.get("capes").isJsonArray()) {
                    JsonArray capesArray = json.getAsJsonArray("capes");
                    List<MineSkinCape> list = new ArrayList<>(capesArray.size());
                    for (JsonElement el : capesArray) {
                        if (el.isJsonObject()) {
                            JsonObject obj = el.getAsJsonObject();
                            String uuid = obj.has("uuid") ? obj.get("uuid").getAsString() : null;
                            String alias = obj.has("alias") ? obj.get("alias").getAsString() : null;
                            String hash = obj.has("hash") ? obj.get("hash").getAsString() : null;
                            String url = obj.has("url") ? obj.get("url").getAsString() : null;
                            String support = obj.has("support") ? obj.get("support").getAsString() : null;
                            if (uuid != null) {
                                list.add(new MineSkinCape(uuid, alias, hash, url, support));
                            }
                        }
                    }
                    this.cachedCapes = list;
                    synchronized (cachedCapeImageHashes) {
                        this.cachedCapeImageHashes.clear();
                    }
                    this.lastCapesFetchTime = now;
                    LOGGER.debug("Loaded " + list.size() + " capes from MineSkin");
                    return list;
                }
            } else {
                LOGGER.warn("Failed to fetch MineSkin capes: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            LOGGER.warn("Error fetching MineSkin capes: " + e.getMessage());
        }

        return cachedCapes != null ? cachedCapes : Collections.emptyList();
    }

    public String resolveCapeUuid(MineSkinConfig config, String capeUrl) {
        List<MineSkinCape> capes = getCapes(config);
        if (capes.isEmpty() && (capeUrl == null || capeUrl.isBlank())) {
            return null;
        }

        if (capeUrl != null && !capeUrl.isBlank()) {
            String trimmed = capeUrl.trim();

            for (MineSkinCape cape : capes) {
                if (cape.uuid().equalsIgnoreCase(trimmed)) {
                    return cape.uuid();
                }
            }

            for (MineSkinCape cape : capes) {
                if (cape.alias() != null && cape.alias().equalsIgnoreCase(trimmed)) {
                    return cape.uuid();
                }
            }

            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                try {
                    byte[] imageBytes = downloadBytes(trimmed);
                    String computedHash = computeSha256(imageBytes);
                    LOGGER.debug("Computed SHA-256 for cape image: " + computedHash);
                    for (MineSkinCape cape : capes) {
                        if (cape.url() != null) {
                            try {
                                String catalogImageHash = getCapeImageHash(cape);
                                LOGGER.debug("Comparing cape image hash " + computedHash + " with MineSkin cape " + cape.alias());
                                if (computedHash.equalsIgnoreCase(catalogImageHash)) {
                                    LOGGER.info("Matched cape image to MineSkin cape " + cape.alias() + " (" + cape.uuid() + ")");
                                    return cape.uuid();
                                }
                            } catch (Exception e) {
                                LOGGER.debug("Could not download catalog cape image " + cape.url() + ": " + e.getMessage());
                            }
                        }
                    }
                    LOGGER.debug("No MineSkin cape matched image SHA-256 " + computedHash);
                } catch (Exception e) {
                    LOGGER.debug("Could not download cape to match hash: " + e.getMessage());
                }
            }
        }

        return null;
    }

    private String getCapeImageHash(MineSkinCape cape) throws IOException, InterruptedException {
        synchronized (cachedCapeImageHashes) {
            String cached = cachedCapeImageHashes.get(cape.uuid());
            if (cached != null) return cached;
        }

        String hash = computeSha256(downloadBytes(cape.url()));
        synchronized (cachedCapeImageHashes) {
            cachedCapeImageHashes.put(cape.uuid(), hash);
        }
        return hash;
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
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

    public byte[] downloadBytes(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<byte[]> res = this.httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200) {
            throw new IOException("Failed to download image from " + url + ": HTTP " + res.statusCode());
        }
        return res.body();
    }

    private String downloadAsDataUrl(String url) throws IOException, InterruptedException {
        byte[] bytes = downloadBytes(url);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
