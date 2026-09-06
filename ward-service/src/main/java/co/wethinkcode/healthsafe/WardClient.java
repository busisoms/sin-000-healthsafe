package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An HTTP client that fetches cleaned ward records from ingestion-service
 * ({@code GET /wards}) and caches them in memory for ward-service to serve.
 */
public class WardClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<Ward> wards;

    public WardClient() {
        this.wards = new ArrayList<>();
    }

    /**
     * Returns the wards fetched so far.
     *
     * @return an unmodifiable view of the cached {@link Ward} records; empty if
     *         {@link #fetchWards()} hasn't been called yet or has always failed
     */
    public List<Ward> wards(){
        return Collections.unmodifiableList(wards);
    }

    /**
     * Looks up a cached ward by id, case-insensitively (ingestion-service
     * normalizes every stored {@code wardId} to uppercase, so the given id is
     * uppercased/stripped the same way before comparing).
     *
     * @param id the ward id to look up, in any case
     * @return the matching {@link Ward}, or {@code null} if no cached ward has that id
     */
    public Ward findById(String id){
        id = id.toUpperCase().strip();
        for (Ward ward : wards){
            if (ward.wardId().equals(id)) return ward;
        }
        return null;
    }

    /**
     * Fetches the current ward list from ingestion-service and replaces the cache
     * with it. On any failure (connection error, non-200 status, or a body that
     * doesn't parse as the expected {@code { "data": [...] } } shape), logs to
     * stderr and returns without touching the existing cache — callers don't need
     * to catch anything.
     */
    public void fetchWards() {
        String URL = "http://localhost:7030/wards";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Unexpected status: " + response.statusCode());
                return;
            }

            Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> rawWards = (List<Map<String, Object>>) body.get("data");

            wards = objectMapper.convertValue(rawWards,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Ward.class));

        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to fetch wards: " + e.getMessage());
        }
    }

}
