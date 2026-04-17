package com.reponse.mvn.core.jobs;

import org.osgi.service.component.annotations.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component(service = CourseImportProgressStore.class, immediate = true)
public class CourseImportProgressStore {

    public static final class ProgressState {
        public final String        status;
        public final int           processedRows;
        public final int           totalRows;
        public final int           created;
        public final int           updated;
        public final int           failed;
        public final int           skipped;
        public final String        filePath;
        public final String        triggeredBy;
        public final String        createdAt;
        public final String        scheduledAt;

        public ProgressState(String status, int processedRows, int totalRows,
                             int created, int updated, int failed, int skipped,
                             String filePath, String triggeredBy,
                             String createdAt, String scheduledAt) {
            this.status        = status;
            this.processedRows = processedRows;
            this.totalRows     = totalRows;
            this.created       = created;
            this.updated       = updated;
            this.failed        = failed;
            this.skipped       = skipped;
            this.filePath      = filePath;
            this.triggeredBy   = triggeredBy;
            this.createdAt     = createdAt;
            this.scheduledAt   = scheduledAt;
        }
    }

    private final ConcurrentHashMap<String, ProgressState> store = new ConcurrentHashMap<>();

    public void put(String jobId, ProgressState state) {
        if (jobId != null) store.put(jobId, state);
    }

    public ProgressState get(String jobId) {
        return jobId != null ? store.get(jobId) : null;
    }

    public void remove(String jobId) {
        if (jobId != null) store.remove(jobId);
    }
}
