package studyMate.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "spring.jwt")
public class JwtProperties {
    private final String secretKey;
    private final long accessTokenValidityInSeconds;
    private final long refreshTokenValidityInSeconds;

    public JwtProperties(String secretKey, 
                        long accessTokenValidityInSeconds, 
                        long refreshTokenValidityInSeconds) {
        this.secretKey = secretKey;
        this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
        this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;
    }
} 