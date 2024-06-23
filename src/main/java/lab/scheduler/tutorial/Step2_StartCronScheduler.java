package lab.scheduler.tutorial;

import lab.scheduler.config.ScheduleTemplate;
import lab.scheduler.config.SchedulerConfig;
import lab.scheduler.core.SchedulerManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Step2_StartCronScheduler {

    public static void main(String[] args) throws Exception {
        log.info("Start Tutorial");

        //(1) Create SchedulerConfig
        SchedulerConfig config = new SchedulerConfig();
        config.setAutoAdjustThreadCount(true); // <- The property 'autoAdjustThreadCount=true' enables thread pool size is automatically sized with schedule Job count. Default value is 'true'.
        config.setThreadCount(7); // <- In this case, the effect of using 'setThreadCount(int count)' is ignored because 'autoAdjustThreadCount' is true as above.

        //(2) Create ScheduleTemplate(s)
        //ScheduleTemplate object contains the information about JobDetail and Trigger
        ScheduleTemplate template1 = new ScheduleTemplate(); //Create new ScheduleTemplate()
        template1.setJobClass(Step1_DefineJobClass.class); //Set the job class which implemented 'org.quartz.Job' interface. If it is null or not set, a default job class is retrieved from ScheduleManager
        template1.setCronExpression("0/3 * * * * ?"); //Set cron expression. It is verified internal by CronExpression.isValidExpression(String expression) method.
        template1.setjobName("MyServiceLogic_1"); //Set unique job name. When null, it is created internally.
        template1.setPriority(10); //Set this job's priority. Higher priority number has higher priority in execution. Default value is -1.
        template1.addJobParam("template name", "template1"); //Set job parameter you want to use in job class
        template1.addJobParam("favorite number", 7); //Set job parameter you want to use in job class

        ScheduleTemplate template2 = new ScheduleTemplate();
        template2.setJobClass("lab.scheduler.tutorial.Step1_DefineJobClass"); //Also possible setting job class with string name
        template2.setCronExpression("0/3 * * * * ?");
        template2.setjobName("MyServiceLogic_2");
        template2.setPriority(9);
        template2.addJobParam("template name", "template2");
        template2.addJobParam("favorite fruit", "grape");

        ScheduleTemplate template3 = new ScheduleTemplate();
        template3.setJobClass(Step1_DefineJobClass.class);
        template3.setCronExpression("0/3 * * * * ?");
        template3.setjobName("MyServiceLogic_3");
        template3.setPriority(8);
        template3.addJobParam("template name", "template3");
        template3.addJobParam("hobby", "playing piano");

        //(3) Add ScheduleTemplates to SchedulerConfig
        config.addScheduleTemplate(template1);
        config.addScheduleTemplate(template2);
        //config.addScheduleTemplate(template3); //Note that I didn't add 'template3' to SchedulerConfig. I am going to add this after Scheduler started

        //(4) Start scheduler
        String schdeulerID = SchedulerManager.registerScheduler(config); //Register the SchedulerConfig which we made to SchedulerManager.
                                                                         //This method create new scheduler and returns the unique scheduler ID of it in string.
                                                                         //Scheduler ID must be remembered when you have schedulers more than two, and it used when control a specific scheduler and its jobs.
        SchedulerManager.startScheduler(schdeulerID); //Start the specific scheduler
        //SchedulerManager.startAllSchedulers(); //Start all schedulers

        Thread.sleep(7000);

        SchedulerManager.addScheduleJob(schdeulerID, template3); //Add new ScheduleJob at scheduler's running
        //SchedulerManager.addScheduleJob(template3); //This method can be used when only one scheduler exists in the SchedulerManager

    }

}
