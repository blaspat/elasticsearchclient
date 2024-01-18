package io.github.blaspat;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.github.blaspat.config.ElasticsearchClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchSimpleClient {
    @Autowired
    private ElasticsearchClientConfig simpleClient;

    public ElasticsearchClient client() {
        return simpleClient.getClient();
    }
}
