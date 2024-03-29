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

package io.github.blaspat.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class ElasticsearchClientConfig implements DisposableBean {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ElasticsearchProperties elasticsearchProperties;
    private RestClient lowLevelClient;
    private ElasticsearchClient client;

    public ElasticsearchClientConfig(ElasticsearchProperties elasticsearchProperties) {
        this.elasticsearchProperties = elasticsearchProperties;
    }

    @Override
    public void destroy() {
        try {
            if (lowLevelClient != null) {
                log.info("Closing Elasticsearch client");
                lowLevelClient.close();
            }
        } catch (final Exception e) {
            log.error("Error closing Elasticsearch client: ", e);
        }
    }

    public ElasticsearchClient getClient() {
        if (null == client) {
            try {
                destroy();
                return constructClient();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (!lowLevelClient.isRunning() || !ping()) {
            try {
                return constructClient();
            } catch (Exception e1) {
                log.error("Failed construct Elasticsearch client", e1);
                throw new RuntimeException(e1);
            }
        } else {
            return client;
        }
    }

    private boolean ping() {
        try {
            client.ping();
            return true;
        } catch (Exception e) {
            log.error("Failed ping hosts : {}", e.getMessage(), e);
            return false;
        }
    }

    private ElasticsearchClient constructClient() {
        try {
            List<String> arr = Arrays.stream(getHostUrlArr()).map(host -> elasticsearchProperties.getScheme() + "://" + host).collect(Collectors.toList());
            log.info("Starting Elasticsearch client with hosts {}", arr);
            lowLevelClient = buildElasticsearchLowLevelClient();
            // Create the transport with a Jackson mapper
            ElasticsearchTransport transport = new RestClientTransport(lowLevelClient, new JacksonJsonpMapper());
            // And create the API client
            client = new ElasticsearchClient(transport);
            return client;
        } catch (Exception e) {
            throw new RuntimeException(e);
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
