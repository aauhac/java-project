package com.tradeagent.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Deprecated(forRemoval = false)
public final class GdeltSupport {

    private GdeltSupport() {
    }

    public static Path defaultLastRequestFile(String configured) {
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured);
        }
        return Paths.get("data", "gdelt", "gdelt-last-request.txt");
    }

    public static Path defaultCacheDir(String configured) {
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured);
        }
        return Paths.get("data", "gdelt-cache");
    }

    public static String cacheKey(String query, java.time.LocalDate from, java.time.LocalDate to, int maxRecords) {
        String raw = (query == null ? "" : query.trim()) + "|" + from + "|" + to + "|" + maxRecords;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public static synchronized long reserveRequestSlot(Path lastRequestFile, long minIntervalMs) {
        Instant now = Instant.now();
        Instant last = readInstant(lastRequestFile).orElse(null);
        long waitMs = 0L;
        if (last != null) {
            Instant nextAvailable = last.plusMillis(minIntervalMs);
            if (now.isBefore(nextAvailable)) {
                waitMs = Duration.between(now, nextAvailable).toMillis();
                sleep(waitMs);
                now = Instant.now();
            }
        }
        writeInstant(lastRequestFile, now);
        return waitMs;
    }

    public static Optional<CacheSnapshot> loadCache(Path cacheDir, String key, Duration ttl) {
        Path file = cacheDir.resolve(key + ".json");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            FileTime lastModified = Files.getLastModifiedTime(file);
            Instant expiresAt = lastModified.toInstant().plus(ttl);
            if (Instant.now().isAfter(expiresAt)) {
                return Optional.empty();
            }
            return Optional.of(new CacheSnapshot(Files.readString(file, StandardCharsets.UTF_8), lastModified.toInstant()));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read GDELT cache", ex);
        }
    }

    public static Optional<CacheSnapshot> loadCacheAllowExpired(Path cacheDir, String key) {
        Path file = cacheDir.resolve(key + ".json");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            FileTime lastModified = Files.getLastModifiedTime(file);
            return Optional.of(new CacheSnapshot(Files.readString(file, StandardCharsets.UTF_8), lastModified.toInstant()));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read GDELT cache", ex);
        }
    }

    public static void saveCache(Path cacheDir, String key, String body) {
        try {
            Files.createDirectories(cacheDir);
            Files.writeString(cacheDir.resolve(key + ".json"), body, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write GDELT cache", ex);
        }
    }

    public static StatusSnapshot snapshot(Path lastRequestFile, Path cacheDir, long minIntervalMs, Duration ttl) {
        Instant now = Instant.now();
        Instant last = readInstant(lastRequestFile).orElse(null);
        Instant nextAvailable = last == null ? now : last.plusMillis(minIntervalMs);
        boolean canRequestNow = !now.isBefore(nextAvailable);
        long waitMs = canRequestNow ? 0L : Duration.between(now, nextAvailable).toMillis();
        long cacheCount = countCacheFiles(cacheDir, ttl);
        return new StatusSnapshot(last, nextAvailable, canRequestNow, waitMs, minIntervalMs, cacheCount);
    }

    private static Optional<Instant> readInstant(Path file) {
        if (file == null || !Files.exists(file)) {
            return Optional.empty();
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (text.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(Instant.parse(text));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static void writeInstant(Path file, Instant instant) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, instant.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write GDELT rate-limit file", ex);
        }
    }

    private static long countCacheFiles(Path cacheDir, Duration ttl) {
        if (cacheDir == null || !Files.exists(cacheDir)) {
            return 0L;
        }
        try (var stream = Files.list(cacheDir)) {
            Instant now = Instant.now();
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toInstant().plus(ttl).isAfter(now);
                        } catch (IOException ex) {
                            return false;
                        }
                    })
                    .count();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to count GDELT cache files", ex);
        }
    }

    private static void sleep(long waitMs) {
        if (waitMs <= 0) {
            return;
        }
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for GDELT request slot", ex);
        }
    }

    public record CacheSnapshot(String body, Instant savedAt) {
    }

    public record StatusSnapshot(
            Instant lastRequestAt,
            Instant nextAvailableAt,
            boolean canRequestNow,
            long waitMillis,
            long minRequestIntervalMs,
            long cacheCount
    ) {
    }
}
