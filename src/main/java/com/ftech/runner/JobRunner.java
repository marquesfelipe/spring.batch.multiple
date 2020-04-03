package com.ftech.runner;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ftech.constants.Constants;

@Component
public class JobRunner {

	private static final Logger logger = LoggerFactory.getLogger(JobRunner.class);

	@Autowired
	private JobLauncher simpleJobLauncher;

	@Autowired
	@Qualifier(value = "toDBJob")
	private Job job1;

	@Autowired
	@Qualifier(value = "toFileJob")
	private Job job2;

	@Scheduled(cron = "*/5 * * * * *")
	public void run1() {
		JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
		jobParametersBuilder.addString(Constants.FILE_NAME_CONTEXT_KEY, "file:C:/home/arquivos/employees.csv");
		jobParametersBuilder.addDate("date", new Date(), true);
		try {
			simpleJobLauncher.run(job1, jobParametersBuilder.toJobParameters());
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
				| JobParametersInvalidException e) {
			logger.error(e.getMessage());
		}
	}

	@Scheduled(cron = "*/15 * * * * *")
	public void run2() {
		JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
		try {
			simpleJobLauncher.run(job2, jobParametersBuilder.toJobParameters());
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
				| JobParametersInvalidException e) {
			logger.error(e.getMessage());
		}
	}

}