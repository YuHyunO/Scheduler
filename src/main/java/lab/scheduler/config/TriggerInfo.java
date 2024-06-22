package lab.scheduler.config;

import lab.scheduler.enums.JobClusterOption;
import lab.scheduler.enums.TriggerType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.quartz.DateBuilder;
import org.quartz.Job;
import org.quartz.TimeOfDay;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public class TriggerInfo {
    private String jobName;
    private String cronExpression;
    private String[] simpleExpression = new String[4]; //[0]=startTime, [1]=endTime, [2]=repeatCount, [3]=repeatInterval
    private String[] calendarExpression = new String[4]; //[0]=startTime, [1]=endTime, [2]=intervalUnit, [3]=repeatInterval
    private String[] dailyExpression = new String[6]; //[0]=startTime, [1]=endTime, [2]=startTimeOfDay, [3]=endTimeOfDay, [4]=intervalUnit, [5]=repeatInterval
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

    public static boolean isValidSimpleExpression(String[] simpleExpression) {

        return false;
    }

    public static boolean isValidCalenderExpression(String[] calendarExpression) {

        return false;
    }

    public static boolean isValidDailyExpression(String[] dailyExpression) {

        return false;
    }

    private static Date getFormattedDate(String dateExpression) throws IllegalArgumentException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        try {
            return format.parse(dateExpression);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Could not parse date: " + dateExpression + ", date format must be yyyy-MM-dd HH:mm:ss.SSS");
        }
    }

    private static Date getFormattedTimeOfDay(String timeExpression) throws IllegalArgumentException {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        try {
            return format.parse(timeExpression);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Could not parse time: " + timeExpression + ", time format must be HH:mm:ss");
        }
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
        triggerType = TriggerType.CRON_TRIGGER;
    }

    public void setSimpleExpression(String[] simpleExpression) {
        if (!isValidSimpleExpression(simpleExpression)) {
            throw new IllegalArgumentException(
                    "Invalid simple expression: startTime=" + simpleExpression[0] + ", endTime=" + simpleExpression[1]
                            + ", repeatCount=" + simpleExpression[2] + ", repeatInterval=" + simpleExpression[3]);
        }
        this.simpleExpression = simpleExpression;
        triggerType = TriggerType.SIMPLE_TRIGGER;
    }

    public void setCalenderExpression(String[] calendarExpression) {
        if (!isValidCalenderExpression(calendarExpression)) {
            throw new IllegalArgumentException(
                    "Invalid calendar expression: startTime=" + calendarExpression[0] + ", endTime=" + calendarExpression[1]
                            + ", intervalUnit=" + calendarExpression[2] + ", repeatInterval=" + calendarExpression[3]);
        }
        this.calendarExpression = calendarExpression;
        triggerType = TriggerType.CALENDAR_INTERVAL_TRIGGER;
    }

    public void setDailyExpression(String[] dailyExpression) {
        if (!isValidDailyExpression(dailyExpression)) {
            throw new IllegalArgumentException(
                    "Invalid daily expression: startTime=" + dailyExpression[0] + ", endTime=" + dailyExpression[1]
                            + ", startTimeOfDay=" + dailyExpression[2] + ", endTimeOfDay=" + dailyExpression[3] + ", intervalUnit=" + dailyExpression[4] + ", repeatInterval=" + dailyExpression[5]);
        }
        this.dailyExpression = dailyExpression;
        triggerType = TriggerType.DAILY_TIME_INTERVAL_TRIGGER;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setJobParams(Map<String, Object> jobParams) {
        this.jobParams = jobParams;
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
            case "CALENDAR_INTERVAL_TRIGGER" : case "CALENDAR_TRIGGER" : case "CALENDAR" : this.triggerType = TriggerType.CALENDAR_INTERVAL_TRIGGER; break;
            case "DAILY_TIME_INTERVAL_TRIGGER" : case "DAILY_TIME_INTERVAL" : case "DAILY_TIME" : case "DAILY" : this.triggerType = TriggerType.DAILY_TIME_INTERVAL_TRIGGER; break;
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

    public void setStartTime(String startTime) {
        if (startTime == null)
            return;
        startTime = startTime.trim();
        simpleExpression[0] = startTime;
        calendarExpression[0] = startTime;
        dailyExpression[0] = startTime;
    }

    public void setEndTime(String endTime) {
        if (endTime == null)
            return;
        endTime = endTime.trim();
        if (endTime.isEmpty())
            return;
        simpleExpression[1] = endTime;
        calendarExpression[1] = endTime;
        dailyExpression[1] = endTime;
    }

    public void setRepeatCount(int repeatCount) {
        String repeatCountStr = repeatCount + "";
        simpleExpression[2] = repeatCountStr;
    }

    public void setRepeatCount(String repeatCount) {
        if (repeatCount == null)
            return;
        repeatCount = repeatCount.trim();
        if (repeatCount.isEmpty())
            return;
        simpleExpression[2] = repeatCount;
    }

    public void setStartTimeOfDay(String startTimeOfDay) {
        if (startTimeOfDay == null)
            return;
        startTimeOfDay = startTimeOfDay.trim();
        if (startTimeOfDay.isEmpty())
            return;
        dailyExpression[2] = startTimeOfDay;
    }

    public void setEndTimeOfDay(String endTimeOfDay) {
        if (endTimeOfDay == null)
            return;
        if (endTimeOfDay.isEmpty())
            return;
        endTimeOfDay = endTimeOfDay.trim();
        dailyExpression[3] = endTimeOfDay;
    }

    public void setIntervalUnit(String intervalUnit) {
        if (intervalUnit == null)
            return;
        intervalUnit = intervalUnit.trim();
        if (intervalUnit.isEmpty())
            return;
        calendarExpression[2] = intervalUnit;
        dailyExpression[4] = intervalUnit;
    }

    public void setRepeatInterval(int repeatInterval) {
        String repeatIntervalStr = repeatInterval + "";
        simpleExpression[3] = repeatIntervalStr;
        calendarExpression[3] = repeatIntervalStr;
        dailyExpression[5] = repeatIntervalStr;
    }

    public void setRepeatInterval(String repeatInterval) {
        if (repeatInterval == null)
            return;
        repeatInterval = repeatInterval.trim();
        if (repeatInterval.isEmpty())
            return;
        simpleExpression[3] = repeatInterval;
        calendarExpression[3] = repeatInterval;
        dailyExpression[5] = repeatInterval;
    }
}
