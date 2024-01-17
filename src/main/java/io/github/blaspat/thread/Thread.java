/*
 * Copyright 2024 Blasius Patrick
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.blaspat.thread;

import io.github.blaspat.config.ElasticsearchProperties;
import io.github.blaspat.config.ElasticsearchClientConfig;
import io.github.blaspat.global.ClientStatus;
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
