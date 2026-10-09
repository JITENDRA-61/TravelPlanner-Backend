package com.example.travelplanner.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.travelplanner.dto.PlaceDto;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class OpenStreetMapService {

    private static final Logger log = LoggerFactory.getLogger(OpenStreetMapService.class);
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    private static final String USER_AGENT = "TravelMate/1.0 (college-project; OpenStreetMap POI Integration)";
    private static final long CACHE_TTL_SECONDS = 1800; // 30 minutes

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache entry wrapper
    private static class CacheEntry<T> {
        final T data;
        final Instant expiresAt;

        CacheEntry(T data, long ttlSeconds) {
            this.data = data;
            this.expiresAt = Instant.now().plusSeconds(ttlSeconds);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final ConcurrentHashMap<String, CacheEntry<List<PlaceDto>>> searchCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry<List<PlaceDto>>> nearbyCache = new ConcurrentHashMap<>();

    // Well-known tourist hubs coordinates (Fast OSM geocoding tier)
    private static final Map<String, double[]> KNOWN_LOCATIONS = new HashMap<>();

    static {
        KNOWN_LOCATIONS.put("hyderabad", new double[]{17.3850, 78.4867});
        KNOWN_LOCATIONS.put("goa", new double[]{15.2993, 74.1240});
        KNOWN_LOCATIONS.put("manali", new double[]{32.2432, 77.1892});
        KNOWN_LOCATIONS.put("bangalore", new double[]{12.9716, 77.5946});
        KNOWN_LOCATIONS.put("bengaluru", new double[]{12.9716, 77.5946});
        KNOWN_LOCATIONS.put("delhi", new double[]{28.6139, 77.2090});
        KNOWN_LOCATIONS.put("new delhi", new double[]{28.6139, 77.2090});
        KNOWN_LOCATIONS.put("mumbai", new double[]{19.0760, 72.8777});
        KNOWN_LOCATIONS.put("paris", new double[]{48.8566, 2.3522});
        KNOWN_LOCATIONS.put("london", new double[]{51.5074, -0.1278});
        KNOWN_LOCATIONS.put("jaipur", new double[]{26.9124, 75.7873});
        KNOWN_LOCATIONS.put("agra", new double[]{27.1767, 78.0081});
        KNOWN_LOCATIONS.put("varanasi", new double[]{25.3176, 82.9739});
        KNOWN_LOCATIONS.put("kolkata", new double[]{22.5726, 88.3639});
        KNOWN_LOCATIONS.put("chennai", new double[]{13.0827, 80.2707});
        KNOWN_LOCATIONS.put("kochi", new double[]{9.9312, 76.2673});
        KNOWN_LOCATIONS.put("kerala", new double[]{10.8505, 76.2711});
        KNOWN_LOCATIONS.put("shimla", new double[]{31.1048, 77.1734});
        KNOWN_LOCATIONS.put("ooty", new double[]{11.4102, 76.6950});
        KNOWN_LOCATIONS.put("rishikesh", new double[]{30.0869, 78.2676});
        KNOWN_LOCATIONS.put("amritsar", new double[]{31.6340, 74.8723});
        KNOWN_LOCATIONS.put("udaipur", new double[]{24.5854, 73.7125});
        KNOWN_LOCATIONS.put("new york", new double[]{40.7128, -74.0060});
        KNOWN_LOCATIONS.put("tokyo", new double[]{35.6762, 139.6503});
        KNOWN_LOCATIONS.put("dubai", new double[]{25.2048, 55.2708});
        KNOWN_LOCATIONS.put("singapore", new double[]{1.3521, 103.8198});
        KNOWN_LOCATIONS.put("rome", new double[]{41.9028, 12.4964});
        KNOWN_LOCATIONS.put("bangkok", new double[]{13.7563, 100.5018});
    }

    public OpenStreetMapService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(20000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Search destinations/POIs in or around a given location/city.
     */
    public List<PlaceDto> searchPlaces(String query, String category) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String cleanQuery = query.trim();
        String normalizedCategory = normalizeCategory(category);
        String cacheKey = (cleanQuery.toLowerCase(Locale.ROOT) + "::" + normalizedCategory).trim();

        CacheEntry<List<PlaceDto>> cached = searchCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("Returning cached search results for '{}'", cacheKey);
            return cached.data;
        }

        try {
            // Step 1 & 2: Resolve coordinates without Nominatim
            double[] coords = resolveLocationCoordinates(cleanQuery);

            List<PlaceDto> results;
            if (coords != null) {
                // Step 3 & 4: Query Overpass for POIs around the resolved coordinates
                int radius = determineSearchRadius(cleanQuery);
                results = fetchPoisAround(coords[0], coords[1], radius, normalizedCategory, 18, cleanQuery);
            } else {
                // Direct POI name query fallback in Overpass
                results = searchPoisDirectlyByName(cleanQuery, normalizedCategory);
            }

            if (results != null && !results.isEmpty()) {
                searchCache.put(cacheKey, new CacheEntry<>(results, CACHE_TTL_SECONDS));
            }
            return results != null ? results : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error searching OpenStreetMap places for query '{}': {}", cleanQuery, e.getMessage());
            // Return cached entry if available even if expired, or empty list
            if (cached != null) {
                return cached.data;
            }
            return Collections.emptyList();
        }
    }

    /**
     * Find POIs nearby a specific coordinate.
     */
    public List<PlaceDto> getNearbyPlaces(double latitude, double longitude, String category, Integer radius) {
        String normalizedCategory = normalizeCategory(category);
        int finalRadius = (radius != null && radius > 0) ? Math.min(radius, 25000) : 10000;
        String cacheKey = String.format(Locale.US, "%.4f,%.4f::%s::%d", latitude, longitude, normalizedCategory, finalRadius);

        CacheEntry<List<PlaceDto>> cached = nearbyCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("Returning cached nearby results for '{}'", cacheKey);
            return cached.data;
        }

        try {
            List<PlaceDto> results = fetchPoisAround(latitude, longitude, finalRadius, normalizedCategory, 20, null);
            if (results != null && !results.isEmpty()) {
                nearbyCache.put(cacheKey, new CacheEntry<>(results, CACHE_TTL_SECONDS));
            }
            return results != null ? results : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching nearby places from OpenStreetMap: {}", e.getMessage());
            if (cached != null) {
                return cached.data;
            }
            return Collections.emptyList();
        }
    }

    /**
     * Resolve location coordinates using OpenStreetMap without using the public Nominatim API.
     */
    private double[] resolveLocationCoordinates(String placeName) {
        String key = placeName.toLowerCase(Locale.ROOT).trim();
        if (KNOWN_LOCATIONS.containsKey(key)) {
            return KNOWN_LOCATIONS.get(key);
        }

        // Dynamic Overpass place resolver (using indexed place & boundary tags)
        try {
            String safePlace = escapeOverpassString(placeName.trim());
            String overpassQl = String.format(
                "[out:json][timeout:10];" +
                "(" +
                "  node['name'='%s']['place'];" +
                "  relation['name'='%s']['place'];" +
                "  relation['name'='%s']['boundary'='administrative']['admin_level'~'^[2-8]$'];" +
                "  node['name:en'='%s']['place'];" +
                ");" +
                "out center 1;",
                safePlace, safePlace, safePlace, safePlace
            );

            JsonNode root = executeOverpassQuery(overpassQl);
            if (root != null && root.has("elements") && root.get("elements").isArray()) {
                JsonNode elements = root.get("elements");
                if (elements.size() > 0) {
                    JsonNode el = elements.get(0);
                    Double lat = extractLat(el);
                    Double lon = extractLon(el);
                    if (lat != null && lon != null) {
                        double[] coords = new double[]{lat, lon};
                        KNOWN_LOCATIONS.put(key, coords);
                        return coords;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not dynamically resolve coordinates for place '{}': {}", placeName, e.getMessage());
        }

        return null;
    }

    private int determineSearchRadius(String placeName) {
        String lower = placeName.toLowerCase(Locale.ROOT);
        if (lower.equals("goa") || lower.equals("kerala")) {
            return 25000; // States / large areas
        }
        return 12000; // Standard city metro radius
    }

    /**
     * Query Overpass for POIs around coordinates with given category.
     */
    private List<PlaceDto> fetchPoisAround(double lat, double lon, int radiusMeters,
                                          String category, int limit, String fallbackCity) {
        String filterQl = buildCategoryFilter(category, radiusMeters, lat, lon);
        String ql = String.format(
            Locale.US,
            "[out:json][timeout:15];" +
            "(" +
            "  %s" +
            ");" +
            "out center %d;",
            filterQl,
            limit
        );

        JsonNode root = executeOverpassQuery(ql);
        return parseOverpassElements(root, fallbackCity);
    }

    /**
     * Direct POI name query fallback in Overpass when location coordinates cannot be resolved.
     */
    private List<PlaceDto> searchPoisDirectlyByName(String query, String category) {
        String safeQuery = escapeOverpassString(query);
        String ql = String.format(
            "[out:json][timeout:15];" +
            "(" +
            "  nwr['name'~'%s',i]['tourism'];" +
            "  nwr['name'~'%s',i]['historic'];" +
            "  nwr['name'~'%s',i]['leisure'];" +
            "  nwr['name'~'%s',i]['natural'];" +
            "  nwr['name'~'%s',i]['amenity'];" +
            ");" +
            "out center 15;",
            safeQuery, safeQuery, safeQuery, safeQuery, safeQuery
        );

        JsonNode root = executeOverpassQuery(ql);
        return parseOverpassElements(root, query);
    }

    private String buildCategoryFilter(String category, int radius, double lat, double lon) {
        String around = String.format(Locale.US, "(around:%d,%.6f,%.6f)", radius, lat, lon);

        switch (category.toLowerCase(Locale.ROOT)) {
            case "museum":
            case "museums":
                return "nwr['tourism'='museum']" + around + ";";

            case "park":
            case "parks":
                return "nwr['leisure'~'park|garden|nature_reserve']" + around + ";";

            case "beach":
            case "beaches":
                return "nwr['natural'='beach']" + around + ";";

            case "restaurant":
            case "restaurants":
                return "nwr['amenity'~'restaurant|cafe']" + around + ";";

            case "hotel":
            case "hotels":
                return "nwr['tourism'~'hotel|guest_house|hostel']" + around + ";";

            case "historic":
            case "historical":
                return "nwr['historic']" + around + ";";

            case "religion":
            case "religious":
                return "nwr['amenity'='place_of_worship']" + around + ";";

            case "shopping":
            case "shop":
                return "nwr['shop']" + around + ";" +
                       "nwr['amenity'='marketplace']" + around + ";";

            case "entertainment":
                return "nwr['amenity'~'cinema|theatre']" + around + ";" +
                       "nwr['tourism'~'theme_park|zoo|aquarium']" + around + ";";

            case "attraction":
            case "attractions":
                return "nwr['tourism'~'attraction|viewpoint|theme_park']" + around + ";" +
                       "nwr['historic'~'monument|fort|castle|ruins|archaeological_site']" + around + ";";

            case "all":
            default:
                return "nwr['tourism'~'attraction|museum|viewpoint|theme_park']" + around + ";" +
                       "nwr['historic'~'monument|fort|castle|memorial|ruins']" + around + ";" +
                       "nwr['leisure'~'park|garden']" + around + ";" +
                       "nwr['natural'='beach']" + around + ";";
        }
    }

    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "all";
        }
        String clean = category.trim().toLowerCase(Locale.ROOT);
        if (clean.equals("tourist_attraction") || clean.equals("attractions")) return "attraction";
        if (clean.equals("museums")) return "museum";
        if (clean.equals("parks")) return "park";
        if (clean.equals("beaches")) return "beach";
        if (clean.equals("restaurants")) return "restaurant";
        if (clean.equals("hotels")) return "hotel";
        if (clean.equals("historical")) return "historic";
        if (clean.equals("religious")) return "religion";
        return clean;
    }

    private JsonNode executeOverpassQuery(String qlQuery) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);

            String body = "data=" + URLEncoder.encode(qlQuery, StandardCharsets.UTF_8);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            String response = restTemplate.postForObject(OVERPASS_URL, entity, String.class);
            if (response != null && response.trim().startsWith("{")) {
                return objectMapper.readTree(response);
            }
        } catch (RestClientException e) {
            log.error("Overpass HTTP call failed: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to parse Overpass response: {}", e.getMessage());
        }
        return null;
    }

    private List<PlaceDto> parseOverpassElements(JsonNode root, String fallbackCity) {
        List<PlaceDto> list = new ArrayList<>();
        if (root == null || !root.has("elements") || !root.get("elements").isArray()) {
            return list;
        }

        JsonNode elements = root.get("elements");
        for (JsonNode el : elements) {
            JsonNode tags = el.get("tags");
            if (tags == null) {
                continue;
            }

            String name = extractName(tags);
            if (name == null || name.trim().isEmpty() || isGenericName(name)) {
                continue;
            }

            Double lat = extractLat(el);
            Double lon = extractLon(el);
            if (lat == null || lon == null) {
                continue;
            }

            String osmType = el.has("type") ? el.get("type").asText() : "node";
            String osmId = el.has("id") ? String.valueOf(el.get("id").asLong()) : "";
            String id = osmType + "_" + osmId;

            String type = determineOsmType(tags);
            String category = determineFriendlyCategory(tags);
            String address = buildAddress(tags, fallbackCity);
            String description = buildDescription(tags, name);

            Map<String, String> tagMap = new LinkedHashMap<>();
            tags.properties().forEach(entry -> {
                String key = entry.getKey();
                if (!key.startsWith("name:") && !key.equals("source") && !key.startsWith("ref")) {
                    tagMap.put(key, entry.getValue().asText());
                }
            });

            PlaceDto dto = new PlaceDto(
                id,
                name,
                type,
                category,
                lat,
                lon,
                address,
                description,
                osmType,
                osmId,
                tagMap
            );

            list.add(dto);
        }

        return list;
    }

    private String extractName(JsonNode tags) {
        if (tags.has("name:en")) {
            return tags.get("name:en").asText();
        }
        if (tags.has("name")) {
            return tags.get("name").asText();
        }
        if (tags.has("int_name")) {
            return tags.get("int_name").asText();
        }
        return null;
    }

    private Double extractLat(JsonNode el) {
        if (el.has("lat")) {
            return el.get("lat").asDouble();
        }
        if (el.has("center") && el.get("center").has("lat")) {
            return el.get("center").get("lat").asDouble();
        }
        return null;
    }

    private Double extractLon(JsonNode el) {
        if (el.has("lon")) {
            return el.get("lon").asDouble();
        }
        if (el.has("center") && el.get("center").has("lon")) {
            return el.get("center").get("lon").asDouble();
        }
        return null;
    }

    private String determineOsmType(JsonNode tags) {
        if (tags.has("tourism")) return "tourism";
        if (tags.has("historic")) return "historic";
        if (tags.has("natural")) return "natural";
        if (tags.has("leisure")) return "leisure";
        if (tags.has("amenity")) return "amenity";
        if (tags.has("shop")) return "shop";
        return "destination";
    }

    private String determineFriendlyCategory(JsonNode tags) {
        if (tags.has("historic")) {
            String h = tags.get("historic").asText();
            return ("yes".equals(h) || "monument".equals(h)) ? "historical" : h;
        }
        if (tags.has("natural") && "beach".equals(tags.get("natural").asText())) {
            return "beach";
        }
        if (tags.has("tourism")) {
            return tags.get("tourism").asText();
        }
        if (tags.has("leisure")) {
            return tags.get("leisure").asText();
        }
        if (tags.has("amenity")) {
            String a = tags.get("amenity").asText();
            if ("place_of_worship".equals(a)) return "religious";
            return a;
        }
        if (tags.has("shop")) {
            return "shopping";
        }
        return "attraction";
    }

    private String buildAddress(JsonNode tags, String fallbackCity) {
        List<String> parts = new ArrayList<>();
        if (tags.has("addr:street")) {
            String street = tags.get("addr:street").asText();
            if (tags.has("addr:housenumber")) {
                street = tags.get("addr:housenumber").asText() + " " + street;
            }
            parts.add(street);
        }
        if (tags.has("addr:suburb")) {
            parts.add(tags.get("addr:suburb").asText());
        }
        if (tags.has("addr:city")) {
            parts.add(tags.get("addr:city").asText());
        } else if (fallbackCity != null && !fallbackCity.trim().isEmpty()) {
            parts.add(fallbackCity.trim());
        }
        if (tags.has("addr:state")) {
            parts.add(tags.get("addr:state").asText());
        }

        if (parts.isEmpty()) {
            return fallbackCity != null ? fallbackCity : "OpenStreetMap location";
        }
        return String.join(", ", parts);
    }

    private String buildDescription(JsonNode tags, String name) {
        if (tags.has("description")) {
            return tags.get("description").asText();
        }
        if (tags.has("description:en")) {
            return tags.get("description:en").asText();
        }
        if (tags.has("historic")) {
            String h = tags.get("historic").asText();
            return "Historic " + ("yes".equals(h) ? "monument" : h) + " recorded in OpenStreetMap";
        }
        if (tags.has("natural") && "beach".equals(tags.get("natural").asText())) {
            return "Natural coastal beach destination";
        }
        if (tags.has("tourism")) {
            return "Popular " + tags.get("tourism").asText().replace('_', ' ') + " destination";
        }
        if (tags.has("amenity")) {
            return tags.get("amenity").asText().replace('_', ' ') + " destination";
        }
        return "Real-world place discovered via OpenStreetMap";
    }

    private boolean isGenericName(String name) {
        String lower = name.toLowerCase(Locale.ROOT).trim();
        return lower.equals("unnamed") || lower.equals("unknown") || lower.equals("none");
    }

    private String escapeOverpassString(String input) {
        return input.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\"", "\\\"");
    }
}
