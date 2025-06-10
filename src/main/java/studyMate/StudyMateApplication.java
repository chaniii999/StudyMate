package studyMate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import studyMate.config.JwtProperties;

@EnableConfigurationProperties(JwtProperties.class)
@SpringBootApplication
@ConfigurationPropertiesScan
public class StudyMateApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudyMateApplication.class, args);
	}

}
