package com.foogaro.hotel.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foogaro.hotel.common.exception.NotFoundException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Document store backed by RedisJSON (JSON.SET / JSON.GET / JSON.DEL).
 * Secondary indexes remain Redis Sets; short-lived locks remain Redis Strings.
 */
@Component
public class JsonRedisStore {

    private static final byte[] ROOT_PATH = "$".getBytes(StandardCharsets.UTF_8);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public JsonRedisStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public <T> void save(String key, T value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            redis.execute((RedisCallback<Object>) connection -> {
                connection.execute("JSON.SET", bytes(key), ROOT_PATH, json);
                return null;
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize " + key, e);
        }
    }

    public <T> Optional<T> find(String key, Class<T> type) {
        byte[] raw = redis.execute((RedisCallback<byte[]>) connection ->
                asBytes(connection.execute("JSON.GET", bytes(key), ROOT_PATH)));
        if (raw == null || raw.length == 0) {
            return Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            // JSON.GET with path $ returns a JSON array of matches
            if (node.isArray()) {
                if (node.isEmpty() || node.get(0).isNull()) {
                    return Optional.empty();
                }
                return Optional.of(objectMapper.treeToValue(node.get(0), type));
            }
            return Optional.of(objectMapper.treeToValue(node, type));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize " + key, e);
        }
    }

    public <T> T getRequired(String key, Class<T> type, String notFoundMessage) {
        return find(key, type).orElseThrow(() -> new NotFoundException(notFoundMessage));
    }

    public boolean delete(String key) {
        // DEL works on ReJSON keys; JSON.DEL's integer reply is awkward via Lettuce's default ByteArrayOutput
        return Boolean.TRUE.equals(redis.delete(key));
    }

    public boolean exists(String key) {
        return findRaw(key).isPresent();
    }

    public void addToSet(String setKey, String member) {
        redis.opsForSet().add(setKey, member);
    }

    public void removeFromSet(String setKey, String member) {
        redis.opsForSet().remove(setKey, member);
    }

    public Set<String> members(String setKey) {
        Set<String> members = redis.opsForSet().members(setKey);
        return members == null ? Set.of() : members;
    }

    public <T> List<T> findAll(String setKey, java.util.function.Function<String, String> keyFn, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (String id : members(setKey)) {
            find(keyFn.apply(id), type).ifPresent(result::add);
        }
        return result;
    }

    /** String SET NX used for room availability locks (not RedisJSON documents). */
    public boolean setIfAbsent(String key, String value) {
        Boolean ok = redis.opsForValue().setIfAbsent(key, value);
        return Boolean.TRUE.equals(ok);
    }

    public void setString(String key, String value) {
        redis.opsForValue().set(key, value);
    }

    public Optional<String> getString(String key) {
        return Optional.ofNullable(redis.opsForValue().get(key));
    }

    public boolean deleteString(String key) {
        return Boolean.TRUE.equals(redis.delete(key));
    }

    public boolean stringExists(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    private Optional<byte[]> findRaw(String key) {
        byte[] raw = redis.execute((RedisCallback<byte[]>) connection ->
                asBytes(connection.execute("JSON.GET", bytes(key), ROOT_PATH)));
        if (raw == null || raw.length == 0) {
            return Optional.empty();
        }
        return Optional.of(raw);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] asBytes(Object result) {
        if (result == null) {
            return null;
        }
        if (result instanceof byte[] bytes) {
            return bytes;
        }
        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

}
