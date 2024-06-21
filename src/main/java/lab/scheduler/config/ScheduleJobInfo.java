package lab.scheduler.config;

import lab.scheduler.enums.JobClusterOption;
import lab.scheduler.enums.TriggerType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.quartz.Job;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public class ScheduleJobInfo {
    private String jobName;
    private String cronExpression;
    private String simpleExpression;
    private String description;
    private String triggerGroupName = "Trigger-group";
    private String jobGroupName = "Job-group";
    private Map<String, Object> jobParams;
    private TriggerType triggerType;
    private Class<? extends Job> jobClass;
    private JobClusterOption jobClusterOption = JobClusterOption.FOLLOW_SCHEDULER_DEFAULT;


    public static boolean isValidCronExpression(String cronExpression) {
        return CronExpression.isValidExpression(cronExpression);
    }

    public static boolean isValidSimpleExpression(String simpleExpression) {

        return false;
    }

    public void setJobName(String jobName) {
        if (jobName != null && jobName.isEmpty()) {
            jobName = null;
        }
        this.jobName = jobName;
    }

    public void setCronExpression(String cronExpression) {
        if (!isValidCronExpression(cronExpression)) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression);
        }
        this.cronExpression = cronExpression;
    }

    public void setSimpleExpression(String simpleExpression) {
        if (!isValidSimpleExpression(simpleExpression)) {
            throw new IllegalArgumentException("Invalid simple expression: " + simpleExpression);
        }
        this.simpleExpression = simpleExpression;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addJobParam(String key, Object value) {
        if (jobParams == null) {
            jobParams = new HashMap<>();
        }
        jobParams.put(key, value);
    }

    public void setTriggerGroupName(String triggerGroupName) {
        if (triggerGroupName == null) {
            throw new IllegalArgumentException("Trigger group name cannot be null");
        }
        triggerGroupName = triggerGroupName.trim();
        if (triggerGroupName.isEmpty()) {
            throw new IllegalArgumentException("Trigger group name cannot be empty");
        }
        this.triggerGroupName = triggerGroupName;
    }

    public void setJobGroupName(String jobGroupName) {
        if (jobGroupName == null) {
            throw new IllegalArgumentException("Job group name cannot be null");
        }
        jobGroupName = jobGroupName.trim();
        if (jobGroupName.isEmpty()) {
            throw new IllegalArgumentException("Job group name cannot be empty");
        }
        this.jobGroupName = jobGroupName;
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public void setTriggerType(String triggerType) {
        triggerType = triggerType.toUpperCase().trim();
        switch (triggerType) {
            case "CRON_TRIGGER" : case "CRON" : this.triggerType = TriggerType.CRON_TRIGGER; break;
            case "SIMPLE_TRIGGER" : case "SIMPLE" : this.triggerType = TriggerType.SIMPLE_TRIGGER; break;
            default: throw new IllegalArgumentException("There is no supported trigger type: " + triggerType);
        }
    }

    public void setJobClass(String jobClass) throws ClassNotFoundException {
        Class clazz = Class.forName(jobClass);
        if (!Job.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("'" + jobClass + "' is not type of Job class");
        }
        this.jobClass = clazz;
    }

    public void setJobClass(Class<? extends Job> jobClass) {
        this.jobClass = jobClass;
    }

    public void setJobClusterOption(JobClusterOption jobClusterOption) {
        this.jobClusterOption = jobClusterOption;
    }

    public void setJobClusterOption(boolean jobHAOption) {
        if (jobHAOption) {
            this.jobClusterOption = JobClusterOption.FOLLOW_SCHEDULER_DEFAULT;
        } else {
            this.jobClusterOption = JobClusterOption.INACTIVE;
        }
    }

}
