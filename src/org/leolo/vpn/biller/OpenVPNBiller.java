package org.leolo.vpn.biller;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.DateBuilder.*;
import static org.quartz.JobBuilder.*;

public class OpenVPNBiller {
	private static Logger logger = LoggerFactory.getLogger(OpenVPNBiller.class);

	public static void main(String... args) {
		Properties prop = new Properties();
		try {
			prop.load(new FileReader("setting.conf"));
		} catch (FileNotFoundException e) {
			logger.error(e.getLocalizedMessage(), e);
			System.exit(1);
		} catch (IOException e) {
			logger.error(e.getLocalizedMessage(), e);
			System.exit(1);
		}
		ExecutorService threadPool = new ThreadPoolExecutor(1, 10, 180L, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>());
		new MainThread(prop, threadPool).start();
		DBManager.getInstance();
		JobDetail job = newJob(CleanupJob.class).withIdentity("myJob", "group1").build();
		Trigger trigger = newTrigger().withIdentity("trigger3", "group1").withSchedule(cronSchedule("0 * * * * ?"))
				.forJob("myJob", "group1").build();
		SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();

		Scheduler sched;
		try {
			sched = schedFact.getScheduler();
			sched.start();
			sched.scheduleJob(job, trigger);
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
}
