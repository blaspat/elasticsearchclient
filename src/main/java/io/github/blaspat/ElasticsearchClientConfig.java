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

package io.github.blaspat;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import io.github.blaspat.helper.ElasticsearchProperties;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.NodeSelector;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import javax.net.ssl.SSLContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Configuration
public class ElasticsearchClientConfig {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private final ElasticsearchProperties elasticsearchProperties;

    public ElasticsearchClientConfig(ElasticsearchProperties elasticsearchProperties) {
        this.elasticsearchProperties = elasticsearchProperties;
    }

    List<ElasticsearchClient> clients = new ArrayList<>();

    protected ElasticsearchClient getClient() {
        int index = currentIndex.getAndUpdate(i -> (i + 1) % clients.size());
        ElasticsearchClient elasticsearchClient = clients.get(index);

        if (Objects.isNull(elasticsearchClient)) {
            elasticsearchClient = constructClient(index+1);
            clients.add(index, elasticsearchClient);
        }

        if (!ping(elasticsearchClient)) {
            elasticsearchClient = constructClient(index+1);
            clients.add(index, elasticsearchClient);
        }
        return elasticsearchClient;
    }

    private boolean ping(ElasticsearchClient client) {
        try {
            client.ping();
            return true;
        } catch (Exception e) {
            log.error("Failed ping hosts : {}", e.getMessage(), e);
            return false;
        }
    }

    protected ElasticsearchClient constructClient(int clientNumber) {
        try {
            List<String> arr = Arrays.stream(getHostUrlArr()).map(host -> elasticsearchProperties.getScheme() + "://" + host).collect(Collectors.toList());
            log.info("Starting Elasticsearch client {} with hosts {}",clientNumber , arr);
            // Create the transport with a Jackson mapper
            ElasticsearchTransport transport = new RestClientTransport(buildElasticsearchLowLevelClient(), new JacksonJsonpMapper());
            // And create the API client
            return new ElasticsearchClient(transport);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    private void init() {
        log.info("Starting Elasticsearch client with configuration : {} clients, {}ms connect timeout {}ms socket timeout", elasticsearchProperties.getConnection().getInitConnections(), elasticsearchProperties.getConnection().getConnectTimeout(), elasticsearchProperties.getConnection().getSocketTimeout());
        for (int i = 0; i < elasticsearchProperties.getConnection().getInitConnections(); i++) {
            clients.add(constructClient(i+1));
        }
    }

    private String[] getHostUrlArr() {
        return elasticsearchProperties.getHosts().split(",");
    }

    private RestClient buildElasticsearchLowLevelClient() throws Exception {
        final SSLContext sslContext = getSSLContext();

        if (null == elasticsearchProperties.getHosts()) {
            throw new RuntimeException("elasticsearch.hosts not set");
        }

        String[] httpHostUrlArr = getHostUrlArr();
        HttpHost[] httpHostArr = new HttpHost[httpHostUrlArr.length];
        for (int i = 0; i < httpHostUrlArr.length; i++) {
            String host = (httpHostUrlArr[i]).trim();
            if (!host.isEmpty()) {
                String[] split = host.split(":");
                String esHost = split[0];
                int esPort = 9200;
                if (split.length == 1) {
                    log.warn("No Elasticsearch port found for host {}, automatically use default port 9200", esHost);
                } else {
                    esPort = Integer.parseInt(split[1]);
                }

                httpHostArr[i] = new HttpHost(esHost, esPort, elasticsearchProperties.getScheme());
            }
        }

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(elasticsearchProperties.getUsername(), elasticsearchProperties.getPassword()));
        return RestClient.builder(httpHostArr)
                .setCompressionEnabled(true)
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                                .setConnectTimeout(elasticsearchProperties.getConnection().getConnectTimeout())
                                .setSocketTimeout(elasticsearchProperties.getConnection().getSocketTimeout()))
                .setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier((hostname, session) -> true)
                        .setDefaultIOReactorConfig(IOReactorConfig.custom().setSoKeepAlive(true).build())
                )
                .setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS)
                .build();
    }

    private SSLContext getSSLContext() throws Exception {
        SSLContextBuilder builder = SSLContexts.custom();
        builder.loadTrustMaterial(null, (x509Certificates, s) -> true);
        return builder.build();
    }
}
