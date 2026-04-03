package io.github.blaspat;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.elasticsearch.client.RestClient;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ElasticsearchClientHolderTest {

    @Test
    public void close_shouldCloseRestClient() throws Exception {
        RestClient mockRestClient = mock(RestClient.class);
        ElasticsearchClient mockClient = mock(ElasticsearchClient.class);
        doNothing().when(mockRestClient).close();

        ElasticsearchClientHolder holder = new ElasticsearchClientHolder(mockClient, mockRestClient);

        holder.close();

        verify(mockRestClient, times(1)).close();
    }

    @Test
    public void close_shouldNotThrow_whenRestClientIsNull() {
        ElasticsearchClient mockClient = mock(ElasticsearchClient.class);
        ElasticsearchClientHolder holder = new ElasticsearchClientHolder(mockClient, null);

        // Should not throw
        holder.close();
    }

    @Test
    public void close_shouldNotThrow_whenCloseThrowsException() throws Exception {
        RestClient mockRestClient = mock(RestClient.class);
        ElasticsearchClient mockClient = mock(ElasticsearchClient.class);
        doThrow(new RuntimeException("Close failed")).when(mockRestClient).close();

        ElasticsearchClientHolder holder = new ElasticsearchClientHolder(mockClient, mockRestClient);

        // Should not throw — close is best effort
        holder.close();
    }

    @Test
    public void client_shouldReturnTheElasticsearchClient() {
        ElasticsearchClient mockClient = mock(ElasticsearchClient.class);
        RestClient mockRestClient = mock(RestClient.class);

        ElasticsearchClientHolder holder = new ElasticsearchClientHolder(mockClient, mockRestClient);

        assertSame(mockClient, holder.client());
    }
}
