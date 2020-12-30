package me.devsaki.hentoid.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.threeten.bp.Instant;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import me.devsaki.hentoid.BuildConfig;
import timber.log.Timber;

public class LogUtil {

    private LogUtil() {
        throw new IllegalStateException("Utility class");
    }

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public static class LogEntry {
        private final Instant timestamp;
        private final String message;
        private final int chapter;
        private final boolean isError;

        public LogEntry(@NonNull String message, int chapter, boolean isError) {
            this.timestamp = Instant.now();
            this.message = message;
            this.chapter = chapter;
            this.isError = isError;
        }

        public LogEntry(@NonNull Instant timestamp, @NonNull String message) {
            this.timestamp = timestamp;
            this.message = message;
            this.chapter = 0;
            this.isError = false;
        }

        public Integer getChapter() {
            return chapter;
        }
    }

    public static class LogInfo {
        private String fileName = "";
        private String logName = "";
        private String noDataMessage = "";
        private String header = "";
        private List<LogEntry> log = Collections.emptyList();

        public void setFileName(@NonNull String fileName) {
            this.fileName = fileName;
        }

        public void setLogName(@NonNull String logName) {
            this.logName = logName;
        }

        public void setNoDataMessage(@NonNull String noDataMessage) {
            this.noDataMessage = noDataMessage;
        }

        public void setHeader(@NonNull String header) {
            this.header = header;
        }

        public void setLog(@NonNull List<LogEntry> log) {
            this.log = log;
        }
    }

    private static String buildLog(@Nonnull LogInfo info) {
        StringBuilder logStr = new StringBuilder();
        logStr.append(info.logName).append(" log : begin").append(LINE_SEPARATOR);
        logStr.append(String.format("Hentoid ver: %s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)).append(LINE_SEPARATOR);
        if (info.log.isEmpty())
            logStr.append("No activity to report - ").append(info.noDataMessage).append(LINE_SEPARATOR);
        else {
            // Log beginning, end and duration
            Instant beginning = Stream.of(info.log).withoutNulls().min((a, b) -> a.timestamp.compareTo(b.timestamp)).get().timestamp;
            Instant end = Stream.of(info.log).withoutNulls().max((a, b) -> a.timestamp.compareTo(b.timestamp)).get().timestamp;
            long durationMs = end.toEpochMilli() - beginning.toEpochMilli();
            logStr.append("Start : ").append(beginning.toString()).append(LINE_SEPARATOR);
            logStr.append("End : ").append(end.toString()).append(" (").append(Helper.formatDuration(durationMs)).append(")").append(LINE_SEPARATOR);
            logStr.append("-----").append(LINE_SEPARATOR);

            // Log header
            if (!info.header.isEmpty()) logStr.append(info.header).append(LINE_SEPARATOR);
            // Log entries in chapter order, with errors first
            Map<Integer, List<LogEntry>> logChapters = Stream.of(info.log).collect(Collectors.groupingBy(LogEntry::getChapter));
            for (List<LogEntry> chapter : logChapters.values()) {
                List<LogEntry> logChapterWithErrorsFirst = Stream.of(chapter).sortBy(l -> !l.isError).toList();
                for (LogEntry entry : logChapterWithErrorsFirst)
                    logStr.append(entry.message).append(LINE_SEPARATOR);
            }
        }

        logStr.append(info.logName).append(" log : end");

        return logStr.toString();
    }

    @Nullable
    public static DocumentFile writeLog(@Nonnull Context context, @Nonnull LogInfo info) {
        try {
            // Create the log
            String logFileName = info.fileName + ".txt";
            String log = buildLog(info);

            // Save it
            DocumentFile folder = FileHelper.getFolderFromTreeUriString(context, Preferences.getStorageUri());
            if (null == folder) return null;

            DocumentFile logDocumentFile = FileHelper.findOrCreateDocumentFile(context, folder, "text/plain", logFileName);
            if (logDocumentFile != null)
                FileHelper.saveBinary(context, logDocumentFile.getUri(), log.getBytes());
            return logDocumentFile;
        } catch (Exception e) {
            Timber.e(e);
        }

        return null;
    }

    public static void writeLog(@Nonnull Context context, @Nonnull byte[] content, @NonNull String name) {
        try {
            DocumentFile root = FileHelper.getFolderFromTreeUriString(context, Preferences.getStorageUri());
            if (root != null) {
                DocumentFile cookiesLog = FileHelper.findOrCreateDocumentFile(context, root, "text/plain", name + "_log.txt");
                if (cookiesLog != null)
                    FileHelper.saveBinary(context, cookiesLog.getUri(), content);
            }
        } catch (IOException e) {
            Timber.e(e);
        }
    }
}
