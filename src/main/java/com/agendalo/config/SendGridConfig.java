package com.agendalo.config;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Configuración para SendGrid API
 */
@Configuration
public class SendGridConfig {

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Bean
    @ConditionalOnProperty(name = "sendgrid.api.key")
    public SendGrid sendGrid() {
        if (sendGridApiKey == null || sendGridApiKey.isEmpty()) {
            throw new IllegalArgumentException("SendGrid API key is required");
        }
        return new SendGrid(sendGridApiKey);
    }
}
