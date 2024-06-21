package lab.scheduler.config;

import lab.scheduler.enums.JobHAOption;
import lab.scheduler.enums.TriggerType;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;

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
    private JobHAOption jobHAOption = JobHAOption.FOLLOW_SCHEDULER_DEFAULT;


    public static boolean isValidCronExpression(String cronExpression) {
        return false;
    }

    public static boolean isValidSimpleExpression(String simpleExpression) {
        return false;
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

    public void setJobHAOption(JobHAOption jobHAOption) {
        this.jobHAOption = jobHAOption;
    }

    public void setJobHAOption(boolean jobHAOption) {
        if (jobHAOption) {
            this.jobHAOption = JobHAOption.FOLLOW_SCHEDULER_DEFAULT;
        } else {
            this.jobHAOption = JobHAOption.INACTIVE;
        }
    }
}
