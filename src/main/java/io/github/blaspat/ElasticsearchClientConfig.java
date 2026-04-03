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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Configuration
public class ElasticsearchClientConfig {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private final ElasticsearchProperties elasticsearchProperties;

    // Thread-safe client pool
    private final Map<Integer, ElasticsearchClientHolder> clients = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    @Autowired
    public ElasticsearchClientConfig(ElasticsearchProperties elasticsearchProperties) {
        this.elasticsearchProperties = elasticsearchProperties;
    }

    public ElasticsearchClient client() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    init();
                    initialized = true;
                }
            }
        }
        int size = clients.size();
        if (size == 0) {
            throw new IllegalStateException("No Elasticsearch clients available");
        }
        int index = currentIndex.getAndUpdate(i -> (i + 1) % size);
        ElasticsearchClientHolder holder = clients.get(index);
        if (Objects.isNull(holder)) {
            holder = createClient(index);
            clients.put(index, holder);
        }
        return holder.client();
    }

    private boolean ping(ElasticsearchClient client) {
        try {
            return client.ping().value();
        } catch (Exception e) {
            log.warn("Failed to ping Elasticsearch client: {}", e.getMessage());
            return false;
        }
    }

    private ElasticsearchClientHolder createClient(int clientNumber) {
        List<String> hostUrls = Arrays.stream(getHostUrlArr())
                .map(host -> elasticsearchProperties.getScheme() + "://" + host)
                .collect(Collectors.toList());
        log.info("Creating Elasticsearch client {} with hosts {}", clientNumber, hostUrls);

        RestClient restClient = buildRestClient();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchClient esClient = new ElasticsearchClient(transport);
        return new ElasticsearchClientHolder(esClient, restClient);
    }

    private void init() {
        log.info("Initializing Elasticsearch client pool: {} initial connections, {}ms connect timeout, {}ms socket timeout",
                elasticsearchProperties.getConnection().getInitConnections(),
                elasticsearchProperties.getConnection().getConnectTimeout(),
                elasticsearchProperties.getConnection().getSocketTimeout());

        for (int i = 0; i < elasticsearchProperties.getConnection().getInitConnections(); i++) {
            clients.put(i, createClient(i));
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Elasticsearch client pool");
        for (Map.Entry<Integer, ElasticsearchClientHolder> entry : clients.entrySet()) {
            try {
                entry.getValue().close();
                log.debug("Closed Elasticsearch client {}", entry.getKey());
            } catch (Exception e) {
                log.warn("Failed to close Elasticsearch client {}: {}", entry.getKey(), e.getMessage());
            }
        }
        clients.clear();
    }

    private String[] getHostUrlArr() {
        return elasticsearchProperties.getHosts().split(",");
    }

    private RestClient buildRestClient() {
        final SSLContext sslContext;
        try {
            SSLContextBuilder builder = SSLContexts.custom();
            builder.loadTrustMaterial(null, (x509Certificates, s) -> true);
            sslContext = builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build SSL context", e);
        }

        if (null == elasticsearchProperties.getHosts()) {
            throw new RuntimeException("elasticsearch.hosts not set");
        }

        String[] httpHostUrlArr = getHostUrlArr();
        HttpHost[] httpHostArr = new HttpHost[httpHostUrlArr.length];
        for (int i = 0; i < httpHostUrlArr.length; i++) {
            String host = httpHostUrlArr[i].trim();
            if (!host.isEmpty()) {
                String[] split = host.split(":");
                String esHost = split[0];
                int esPort = 9200;
                if (split.length == 1) {
                    log.warn("No Elasticsearch port found for host {}, automatically using default port 9200", esHost);
                } else {
                    esPort = Integer.parseInt(split[1]);
                }
                httpHostArr[i] = new HttpHost(esHost, esPort, elasticsearchProperties.getScheme());
            }
        }

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(elasticsearchProperties.getUsername(), elasticsearchProperties.getPassword()));

        int connectTimeout = elasticsearchProperties.getConnection().getConnectTimeout();
        int socketTimeout = elasticsearchProperties.getConnection().getSocketTimeout();

        return RestClient.builder(httpHostArr)
                .setCompressionEnabled(true)
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(connectTimeout)
                        .setSocketTimeout(socketTimeout)
                        .setConnectionRequestTimeout(connectTimeout))
                .setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier((hostname, session) -> true)
                        .setDefaultIOReactorConfig(IOReactorConfig.custom()
                                .setSoKeepAlive(true)
                                .setConnectTimeout(connectTimeout)
                                .setSoTimeout(socketTimeout)
                                .build()))
                .setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS)
                .build();
    }
}
