package lab.scheduler.listener;

import lab.scheduler.core.SchedulerManager;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;

import java.util.Date;

@Slf4j
public class NextFireTimeCheckTriggerListener implements TriggerListener {
    @Override
    public String getName() {
        return "NextFireTimeCheckTriggerListener";
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext jobExecutionContext) {
    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext jobExecutionContext) {
        return false;
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext jobExecutionContext, Trigger.CompletedExecutionInstruction completedExecutionInstruction) {
        Date nextFireTime = trigger.getNextFireTime();
        if (nextFireTime == null) {
            try {
                JobDetail jobDetail = jobExecutionContext.getJobDetail();
                String schedulerId = jobDetail.getJobDataMap().getString("schedulerID");
                String jobId = jobDetail.getKey().getName();
                SchedulerManager.getInstance().removeScheduleJob(schedulerId, jobId);
            } catch (SchedulerException e) {
                log.error("", e);
            }
        }
    }
}
