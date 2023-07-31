package org.prebid.server.spring.config.bidder;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.prebid.server.bidder.BidderDeps;
import org.prebid.server.bidder.admaru.AdmaruBidder;
import org.prebid.server.json.JacksonMapper;
import org.prebid.server.spring.config.bidder.model.BidderConfigurationProperties;
import org.prebid.server.spring.config.bidder.util.BidderDepsAssembler;
import org.prebid.server.spring.config.bidder.util.UsersyncerCreator;
import org.prebid.server.spring.env.YamlPropertySourceFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.validation.constraints.NotBlank;

@Configuration
@PropertySource(value = "classpath:/bidder-config/admaru.yaml", factory = YamlPropertySourceFactory.class)
public class AdmaruConfiguration {

    private static final String BIDDER_NAME = "admaru";

    @Bean("admaruConfigurationProperties")
    @ConfigurationProperties("adapters.admaru")
    AdmaruConfigurationProperties configurationProperties() {
        return new AdmaruConfigurationProperties();
    }

    @Bean
    BidderDeps admaruBidderDeps(@Qualifier("admaruConfigurationProperties")
                                AdmaruConfigurationProperties admaruConfigurationProperties,
                                @NotBlank @Value("${external-url}") String externalUrl,
                                JacksonMapper mapper) {

        return BidderDepsAssembler.<AdmaruConfigurationProperties>forBidder(BIDDER_NAME)
                .withConfig(admaruConfigurationProperties)
                .usersyncerCreator(UsersyncerCreator.create(externalUrl))
                .bidderCreator(config -> new AdmaruBidder(config.getEndpoint(), config.getSecondEndpoint(), mapper))
                .assemble();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    private static class AdmaruConfigurationProperties extends BidderConfigurationProperties {

        private String secondEndpoint;

    }

}
