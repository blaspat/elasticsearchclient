package org.blaspat.thread;

import org.blaspat.config.ElasticsearchClientConfig;
import org.blaspat.config.ElasticsearchProperties;
import org.blaspat.global.ClientStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class Thread  {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ElasticsearchClientConfig elasticsearchClientConfig;
    private final ElasticsearchProperties elasticsearchProperties;

    protected Thread(ElasticsearchClientConfig elasticsearchClientConfig, ElasticsearchProperties elasticsearchProperties) {
        this.elasticsearchClientConfig = elasticsearchClientConfig;
        this.elasticsearchProperties = elasticsearchProperties;
    }

    @Scheduled(fixedDelay = 60000)
    private void checkConnection() {
        try {
            elasticsearchClientConfig.getClient().ping();
            ClientStatus.setClientConnected(true);
            log.debug("Success ping to {}, client connected {}", elasticsearchProperties.getHost(), ClientStatus.isClientConnected());
        } catch (Exception e) {
            log.warn("Failed ping Elasticsearch client, set clientConnected to false");
            ClientStatus.setClientConnected(false);
            elasticsearchClientConfig.getClient();
        }
    }
}
