package masterlazy.satellite;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class BackgroundLogger {
    private static final String FILE_NAME = "satellite.log";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("'['HH:mm:ss']'");
    private final File logFile;

    public BackgroundLogger(String baseDir) {
        logFile = Paths.get(baseDir, FILE_NAME).toFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, false))) {
            writer.flush();
        } catch (Exception ignored) {}
    }

    private void emit(String msg, String level) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            String time = Instant.now().atZone(ZoneId.systemDefault()).format(FORMATTER);
            writer.write(String.join(" ", time, level, msg));
            writer.write("\r\n");
            writer.flush();
        } catch (Exception ignored) {}
    }

    public void debug(String msg, @Nullable Object... args) {
        emit(String.format(msg, args), "[DEBUG]");
    }

    public void info(String msg, @Nullable Object... args) {
        emit(String.format(msg, args), "[INFO]");
    }

    public void warn(String msg, @Nullable Object... args) {
        emit(String.format(msg, args), "[WARN]");
    }

    public void error(String msg, @Nullable Object... args) {
        emit(String.format(msg, args), "[ERROR]");
    }
}
