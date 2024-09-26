package one.ggsky.alternativeauth.networking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.MinecraftClientException;

import net.moznion.uribuildertiny.URIBuilderTiny;
import one.ggsky.alternativeauth.AlternativeAuthentication;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerBase;

import java.lang.reflect.Type;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AlternativeAuthClient {
    static GsonBuilder gsonBuilder = new GsonBuilder()
        .registerTypeAdapter(GameProfile.class, new AlternativeAuthGameProfileDeserializer());

    private static final Gson gson = gsonBuilder.create(); 

    static AlternativeAuthLoggerBase LOGGER = AlternativeAuthentication.Logger;

    private static final HttpClient client = HttpClient
        .newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    protected static CompletableFuture<HttpResponse<String>> createGetRequest(URI uri) throws InterruptedException {
        Objects.requireNonNull(uri);
        LOGGER.debug("Sending GET request to " + uri);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    protected static CompletableFuture<HttpResponse<String>> createPostRequest(URI uri, String body) throws InterruptedException {
        Objects.requireNonNull(uri);
        LOGGER.debug("Sending POST request to " + uri);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(10))
                .POST(BodyPublishers.ofString(body))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public static CompletableFuture<String> performGetRequest(URI uri) {
        Objects.requireNonNull(uri);
        
        try {
            return createGetRequest(uri)
                .thenApply(response -> {
                    LOGGER.debug("Reading data from " + uri);
                    String result = response.body();
                    LOGGER.debug("Data read successfully, server replied with " + response.statusCode());
                    LOGGER.debug("Response: " + result);

                    LOGGER.debug("Server response returned");
                    return result;
                });
        } catch (InterruptedException exception) {
            LOGGER.debug("Error occurred while performing request: " + exception.getMessage());
            return null;
        }
    }

    public static CompletableFuture<String> performPostRequest(URI uri, String body) {
        Objects.requireNonNull(uri);
        
        try {
            return createPostRequest(uri, body)
                .thenApply(response -> {
                    LOGGER.debug("Reading data from " + uri);
                    String result = response.body();
                    LOGGER.debug("Data read successfully, server replied with " + response.statusCode());
                    LOGGER.debug("Response: " + result);

                    LOGGER.debug("Server response returned");
                    return result;
                });
        } catch (InterruptedException exception) {
            LOGGER.debug("Error occurred while performing request: " + exception.getMessage());
            return null;
        }
    }

    public static <T> T get(URI uri, Class<T> type) throws AuthenticationException {
        try {
            Objects.requireNonNull(uri);
            String result = performGetRequest(uri).join();
            
            if (result == null) {
                return null;
            }

            return gson.fromJson(result, type);
        } catch (Exception exception) {
            LOGGER.debug(exception.getMessage());
            throw new AuthenticationUnavailableException("Cannot contact authentication server", exception);
        }
    }

    public static <T> T post(URI uri, String body, Type type) throws MinecraftClientException {
        try {
            Objects.requireNonNull(uri);
            String result = performPostRequest(uri, body).join();
            
            if (result == null) {
                return null;
            }

            return gson.fromJson(result, type);
        } catch (Exception exception) {
            LOGGER.debug(exception.getMessage());
            return null;
        }
    }

    public static URI buildUri(String url, Map<String, Object> query) {
        URIBuilderTiny builder = new URIBuilderTiny(url);

        if (query != null) {
            for (Map.Entry<String, Object> entry : query.entrySet()) {
                builder.addQueryParameter(entry.getKey(), entry.getValue().toString());
            }
        }

        return builder.build();
    }
}
