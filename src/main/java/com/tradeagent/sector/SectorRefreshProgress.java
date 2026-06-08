package com.tradeagent.sector;

import com.tradeagent.sector.SectorApiModels.RefreshProgressDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class SectorRefreshProgress {

    private static final int MAX_LOG_SIZE = 80;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private boolean running;
    private int totalSteps;
    private int currentStep;
    private String stage = "IDLE";
    private String message = "";
    private String error = "";
    private final List<String> logs = new ArrayList<>();

    public synchronized void start(int totalSteps, String message) {
        this.running = true;
        this.totalSteps = totalSteps;
        this.currentStep = 0;
        this.stage = "START";
        this.message = message;
        this.error = "";
        this.logs.clear();
        addLog("START", message);
    }

    public synchronized void update(String stage, int currentStep, String message) {
        this.running = true;
        this.stage = stage;
        this.currentStep = Math.min(Math.max(currentStep, 0), totalSteps);
        this.message = message;
        addLog(stage, message);
    }

    public synchronized void log(String stage, String message) {
        this.stage = stage;
        this.message = message;
        addLog(stage, message);
    }

    public synchronized void finish(String message) {
        this.running = false;
        this.currentStep = totalSteps;
        this.stage = "DONE";
        this.message = message;
        addLog("DONE", message);
    }

    public synchronized void fail(String error) {
        this.running = false;
        this.stage = "FAILED";
        this.error = error;
        this.message = "뉴스 갱신 실패";
        addLog("FAILED", error);
    }

    public synchronized RefreshProgressDto snapshot() {
        int percent = totalSteps <= 0
                ? 0
                : (int) Math.round((currentStep * 100.0) / totalSteps);

        return new RefreshProgressDto(
                running,
                totalSteps,
                currentStep,
                percent,
                stage,
                message,
                error,
                List.copyOf(logs)
        );
    }

    private void addLog(String stage, String message) {
        String time = LocalDateTime.now().format(TIME_FORMATTER);
        String line = "[" + time + "] [" + stage + "] " + message;

        logs.add(line);

        while (logs.size() > MAX_LOG_SIZE) {
            logs.remove(0);
        }
    }
}