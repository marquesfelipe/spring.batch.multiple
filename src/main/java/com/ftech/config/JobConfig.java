package com.ftech.config;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import com.ftech.dto.EmployeeDTO;
import com.ftech.mapper.EmployeeDBRowMapper;
import com.ftech.mapper.EmployeeFileRowMapper;
import com.ftech.model.Employee;
import com.ftech.processor.EmployeeProcessor;

@Configuration
public class JobConfig {

	private JobBuilderFactory jobBuilderFactory;
	private StepBuilderFactory stepBuilderFactory;
	private EmployeeProcessor employeeProcessor;
	private DataSource dataSource;
	private Resource outputResource = new FileSystemResource("output/employee_output.csv");

	@Autowired
	public JobConfig(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory,
			EmployeeProcessor employeeProcessor, DataSource dataSource) {
		this.jobBuilderFactory = jobBuilderFactory;
		this.stepBuilderFactory = stepBuilderFactory;
		this.employeeProcessor = employeeProcessor;
		this.dataSource = dataSource;
	}

	@Bean
	@StepScope
	Resource inputFileResource(@Value("#{jobParameters[fileName]}") final String fileName) throws Exception {
		return new UrlResource(fileName);
	}

	@Qualifier(value = "toDBJob")
	@Bean
	public Job toDBJob() throws Exception {
		return this.jobBuilderFactory.get("toDBJob").start(steptoDBJob()).build();
	}

	@Qualifier(value = "toFileJob")
	@Bean
	public Job toFileJob() throws Exception {
		return this.jobBuilderFactory.get("toFileJob").start(steptoFileJob()).build();
	}

	@Bean
	public Step steptoDBJob() throws Exception {
		return this.stepBuilderFactory.get("steptoDBJob").<EmployeeDTO, Employee>chunk(10).reader(employeeReader())
				.writer(employeeDBWriterDefault()).processor(employeeProcessor).build();
	}

	@Bean
	public Step steptoFileJob() throws Exception {
		return this.stepBuilderFactory.get("steptoFileJob").<Employee, EmployeeDTO>chunk(10).reader(employeeDBReader())
				.writer(employeeFileWriter()).build();
	}

	@Bean
	@StepScope
	public FlatFileItemReader<EmployeeDTO> employeeReader() throws Exception {
		FlatFileItemReader<EmployeeDTO> reader = new FlatFileItemReader<>();
		reader.setResource(inputFileResource(null));
		reader.setLineMapper(new DefaultLineMapper<EmployeeDTO>() {
			{
				setLineTokenizer(new DelimitedLineTokenizer() {
					{
						setNames("employeeId", "firstName", "lastName", "email", "age");
					}
				});
				setFieldSetMapper(new EmployeeFileRowMapper());
			}
		});
		return reader;
	}

	@Bean
	public JdbcBatchItemWriter<Employee> employeeDBWriterDefault() {
		JdbcBatchItemWriter<Employee> itemWriter = new JdbcBatchItemWriter<Employee>();
		itemWriter.setDataSource(dataSource);
		itemWriter.setSql(
				"insert into employee (employee_id, first_name, last_name, email, age) values (:employeeId, :firstName, :lastName, :email, :age)");
		itemWriter.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Employee>());
		return itemWriter;
	}

	@Bean
	public ItemStreamReader<Employee> employeeDBReader() {
		JdbcCursorItemReader<Employee> reader = new JdbcCursorItemReader<>();
		reader.setDataSource(dataSource);
		reader.setSql("select * from employee");
		reader.setRowMapper(new EmployeeDBRowMapper());
		return reader;
	}

	@Bean
	public ItemWriter<? super EmployeeDTO> employeeFileWriter() throws Exception {
		FlatFileItemWriter<EmployeeDTO> writer = new FlatFileItemWriter<>();
		writer.setResource(outputResource);
		writer.setLineAggregator(new DelimitedLineAggregator<EmployeeDTO>() {
			{
				setFieldExtractor(new BeanWrapperFieldExtractor<EmployeeDTO>() {
					{
						setNames(new String[] { "employeeId", "firstName", "lastName", "email", "age" });
					}
				});
			}
		});
		writer.setShouldDeleteIfExists(true);
		return writer;
	}

}
