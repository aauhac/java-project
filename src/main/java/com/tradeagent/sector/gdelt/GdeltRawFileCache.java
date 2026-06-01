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

    private static final String CACHE_DIR = "./data/gdelt-raw";

    public void enforceMaxFiles(int maxFiles) {
        List<Path> files = listCachedGkgFiles();
        if (files.size() <= maxFiles) {
            return;
        }
        int deleteCount = files.size() - maxFiles;
        for (int i = 0; i < deleteCount; i++) {
            try {
                Files.deleteIfExists(files.get(i));
            } catch (IOException ignored) {
            }
        }
    }

    public List<Path> listCachedGkgFiles() {
        Path dir = Paths.get(CACHE_DIR);
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".gkg.csv.zip"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }
}
