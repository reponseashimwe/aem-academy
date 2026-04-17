package com.reponse.mvn.core.schedulers;

import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Scheduled course import task.
 */
@Component(service = Runnable.class)
@Designate(ocd = CourseImportScheduledTask.Config.class)
public class CourseImportScheduledTask implements Runnable {

    @ObjectClassDefinition(
        name  = "Academy — Course Import Scheduled Task",
        description = "Runs the CSV/XLSX course importer on a cron schedule. " +
                      "Disable by unchecking 'enabled' — no need to remove the config."
    )
    public @interface Config {

        @AttributeDefinition(
            name        = "Enabled",
            description = "Uncheck to suspend the scheduled import without removing the configuration."
        )
        boolean enabled() default false;

        @AttributeDefinition(
            name        = "Cron expression",
            description = "Quartz 6-field cron: sec min hour day month weekday. " +
                          "Examples: '0 0 9 * * ?' = 9 AM daily, '0 0 8 ? * MON' = 8 AM every Monday."
        )
        String scheduler_expression() default "0 0 9 ? * MON";

        @AttributeDefinition(
            name        = "Allow concurrent runs",
            description = "Keep false to prevent overlapping imports if the previous run is still active."
        )
        boolean scheduler_concurrent() default false;

        @AttributeDefinition(
            name        = "DAM file path",
            description = "Repository path to the .csv or .xlsx file in the DAM."
        )
        String filePath() default "/content/dam/mvnreponse/courses-test.csv";

        @AttributeDefinition(
            name        = "Target path",
            description = "Parent page under which course pages are created."
        )
        String targetPath() default "/content/codehills/courses";

        @AttributeDefinition(
            name        = "Duplicate handling",
            description = "SKIP — leave existing pages; OVERRIDE — update them; ALLOW — always create."
        )
        String duplicateHandling() default "SKIP";
    }

    private static final Logger LOG = LoggerFactory.getLogger(CourseImportScheduledTask.class);

    @Reference
    private JobManager jobManager;

    private Config config;

    @Activate
    @Modified
    protected void activate(Config config) {
        this.config = config;
        if (config.enabled()) {
            LOG.info("[SCHEDULER] Course import scheduled: cron='{}' file='{}' target='{}' mode='{}'",
                     config.scheduler_expression(), config.filePath(),
                     config.targetPath(), config.duplicateHandling());
        } else {
            LOG.info("[SCHEDULER] Course import scheduler is disabled.");
        }
    }

    @Override
    public void run() {
        if (!config.enabled()) {
            LOG.debug("[SCHEDULER] Skipping — disabled in OSGi config.");
            return;
        }
        LOG.info("[SCHEDULER] Triggering scheduled import: file='{}' target='{}' mode='{}'",
                 config.filePath(), config.targetPath(), config.duplicateHandling());

        Map<String, Object> props = new HashMap<>();
        props.put("filePath", config.filePath());
        props.put("targetPath", config.targetPath());
        props.put("duplicateHandling", config.duplicateHandling());
        props.put("triggeredBy", "scheduled");

        jobManager.addJob("com/reponse/mvn/course/import", props);
    }
}
