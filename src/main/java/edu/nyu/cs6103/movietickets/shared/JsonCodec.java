package edu.nyu.cs6103.movietickets.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.nyu.cs6103.movietickets.shared.dto.ErrorResponse;

import java.util.Objects;
import java.util.UUID;

/** Thread-safe JSON serialization for the newline-delimited network protocol. */
public final class JsonCodec {

    private final ObjectMapper mapper;

    public JsonCodec() {
        ObjectMapper configuredMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        configuredMapper.setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
        mapper = configuredMapper;
    }

    public String encode(Object value) {
        Objects.requireNonNull(value, "value must not be null");
        try {
            String json = mapper.writeValueAsString(value);
            if (json.indexOf('\n') >= 0 || json.indexOf('\r') >= 0) {
                throw new IllegalArgumentException("Encoded network JSON must occupy one line");
            }
            return json;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to encode JSON", exception);
        }
    }

    public <T> T decode(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON must not be blank");
        }
        Objects.requireNonNull(type, "type must not be null");
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid JSON message", exception);
        }
    }

    public NetworkRequest decodeRequest(String json) {
        return decode(json, NetworkRequest.class);
    }

    public NetworkResponse decodeResponse(String json) {
        return decode(json, NetworkResponse.class);
    }

    public <T> T requestDataAs(NetworkRequest request, Class<T> type) {
        Objects.requireNonNull(request, "request must not be null");
        return treeToValue(request.data(), type, "Request data is missing");
    }

    public <T> T responseDataAs(NetworkResponse response, Class<T> type) {
        Objects.requireNonNull(response, "response must not be null");
        return treeToValue(response.data(), type, "Response data is missing");
    }

    public NetworkRequest request(RequestType type, String token, Object data) {
        return new NetworkRequest(
                UUID.randomUUID().toString(), type, token, toTree(data));
    }

    public NetworkResponse success(String requestId, Object data) {
        return new NetworkResponse(
                requestId, ResponseStatus.SUCCESS, toTree(data), null);
    }

    public NetworkResponse error(String requestId, ErrorResponse error) {
        return new NetworkResponse(requestId, ResponseStatus.ERROR, null, error);
    }

    public JsonNode toTree(Object value) {
        return value == null ? null : mapper.valueToTree(value);
    }

    private <T> T treeToValue(JsonNode node, Class<T> type, String missingMessage) {
        Objects.requireNonNull(type, "type must not be null");
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException(missingMessage);
        }
        try {
            return mapper.treeToValue(node, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON data does not match " + type.getSimpleName(), exception);
        }
    }
}
