package lab.scheduler.tutorial;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;

/*
* (1) Create a class which implements Job interface
* */
@Slf4j
public class Step1_DefineJobClass implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        Trigger trigger = jobExecutionContext.getTrigger();

        StringBuilder logBd = new StringBuilder();
        logBd.append("\n---------------------\n");
        logBd.append(">Trigger Class: " + trigger.getClass() + "\n");
        logBd.append(">Job Name: " + jobDetail.getKey().getName() + "\n");
        logBd.append(">Scheduled Fire Time: " + jobExecutionContext.getScheduledFireTime() + "\n");
        logBd.append(">Fire Time: " + jobExecutionContext.getFireTime() + "\n");
        logBd.append(">Job Class: " + jobDetail.getJobClass().getName() + "\n");

        String[] paramKeys = jobDataMap.getKeys();
        for (int i = 0; i < paramKeys.length; i++) {
            logBd.append(">Job Param(" + (i + 1) +"): [key=" + paramKeys[i] + ", value=" + jobDataMap.get(paramKeys[i]) + "]\n");
        }
        logBd.append("---------------------\n");

        log.info("{}", logBd.toString());
    }

}
