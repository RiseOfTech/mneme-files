package io.github.ifamneme.mneme_files;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MnemeFilesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MnemeFilesApplication.class, args);
	}

}
