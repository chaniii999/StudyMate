package studyMate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {
    private String apikey;
    private RateLimit rateLimit = new RateLimit();

    @Getter @Setter
    public static class RateLimit {
        private int requestsPerMinute = 20;
        private int retryDelaySeconds = 60;
    }
}
