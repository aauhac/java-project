package com.tradeagent.sector.gdelt;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
public class GdeltRawFileCache {

    private static final String ZIP_DIR = "./data/gdelt-raw/zip";
    private static final String CSV_DIR = "./data/gdelt-raw/csv";

    public void enforceMaxFiles(int maxFiles) {
        enforce(ZIP_DIR, ".gkg.csv.zip", maxFiles);
        enforce(CSV_DIR, ".gkg.csv", maxFiles);
    }

    private void enforce(String dirPath, String suffix, int maxFiles) {
        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir)) {
            return;
        }

        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(suffix))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();

            if (files.size() <= maxFiles) {
                return;
            }

            int deleteCount = files.size() - maxFiles;
            for (int i = 0; i < deleteCount; i++) {
                Files.deleteIfExists(files.get(i));
            }
        } catch (IOException ex) {
            org.slf4j.LoggerFactory.getLogger(GdeltRawFileCache.class)
                    .warn("Failed to clean GDELT cache: {}", ex.getMessage());
        }
    }
}