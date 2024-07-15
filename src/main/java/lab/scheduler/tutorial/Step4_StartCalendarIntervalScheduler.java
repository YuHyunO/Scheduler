package lab.scheduler.tutorial;

import lab.scheduler.config.ScheduleTemplate;
import lab.scheduler.config.SchedulerConfig;
import lab.scheduler.core.SchedulerManager;
import lab.scheduler.enums.TriggerType;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DateBuilder;

@Slf4j
public class Step4_StartCalendarIntervalScheduler {

    public static void main(String[] args) throws Exception {
        log.info("Start simple trigger");
        //(1) Create SchedulerManager instance
        SchedulerManager manager = SchedulerManager.getInstance();

        //(2) Create SchedulerConfig
        SchedulerConfig config = new SchedulerConfig();
        config.setAutoAdjustThreadCount(true); // <- The property 'autoAdjustThreadCount=true' enables that thread pool size is automatically sized with schedule Job count. Default value is 'true'.
        config.setThreadCount(7); // <- In this case, the effect of using 'setThreadCount(int count)' is ignored because 'autoAdjustThreadCount' is true as above.
        config.setMaxThreadCount(100); //Set max thread count that can be added to thread pool.

        //(3) Create ScheduleTemplate(s)
        //ScheduleTemplate object contains the information about JobDetail and Trigger
        ScheduleTemplate template1 = new ScheduleTemplate(); //Create new ScheduleTemplate()
        template1.setTriggerType(TriggerType.CALENDAR_INTERVAL_TRIGGER); //Set this template's triggerType to CALENDAR_INTERVAL_TRIGGER.
        template1.setJobClass(Step1_DefineJobClass.class); //Set the job class which implemented 'org.quartz.Job' interface. If it is null or not set, a default job class is retrieved from ScheduleManager
        template1.setStartTime("2024-07-15 14:33:55"); //Set start time
        template1.setEndTime("2024-07-16 17:24:20"); //Set end time
        template1.setRepeatInterval(5); //Set repeat interval. Interval unit must be defined by using the method 'setIntervalUnit'.
        template1.setIntervalUnit(DateBuilder.IntervalUnit.SECOND); //Set repeat interval unit.
        template1.setJobName("MyServiceLogic_1"); //Set unique job name. When null, it is created internally.
        template1.setPriority(10); //Set this job's priority. Higher priority number has higher priority in execution. Default value is -1.
        template1.addJobParam("template name", "template1"); //Set job parameter you want to use in job class
        template1.addJobParam("favorite number", 7); //Set job parameter you want to use in job class

        ScheduleTemplate template2 = new ScheduleTemplate();
        template2.setTriggerType(TriggerType.CALENDAR_INTERVAL_TRIGGER);
        template2.setJobClass(Step1_DefineJobClass.class);
        template2.setStartTime("NOW");
        template2.setEndTime("2024-07-15 14:59:20");
        template2.setRepeatInterval(3);
        template2.setIntervalUnit(DateBuilder.IntervalUnit.SECOND);
        template2.setJobName("MyServiceLogic_2");
        template2.setPriority(9);
        template2.addJobParam("template name", "template2");
        template2.addJobParam("favorite fruit", "grape");

        ScheduleTemplate template3 = new ScheduleTemplate();
        template3.setTriggerType(TriggerType.CALENDAR_INTERVAL_TRIGGER);
        template3.setJobClass(Step1_DefineJobClass.class);
        template3.setStartTime("NOW");
        template3.setJobName("MyServiceLogic_3");
        template3.setPriority(8);
        template3.setRepeatInterval(3);
        template3.setIntervalUnit(DateBuilder.IntervalUnit.DAY);
        template3.addJobParam("template name", "template3");
        template3.addJobParam("hobby", "playing piano");

        //(4) Add ScheduleTemplates to SchedulerConfig
        config.addScheduleTemplate(template1);
        config.addScheduleTemplate(template2);
        config.addScheduleTemplate(template3);

        //(5) Start scheduler
        String schdeulerID = manager.registerScheduler(config); //Register the SchedulerConfig we made to the SchedulerManager.
        //This method creates new scheduler and returns the unique scheduler ID of it in string.
        //Scheduler ID must be remembered when you have schedulers more than two, and it used when control a specific scheduler and its jobs.
        manager.startScheduler(schdeulerID); //Start the specific scheduler
        //SchedulerManager.startAllSchedulers(); //Start all schedulers
    }

}
