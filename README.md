# Resilient Elasticsearch Client

A resilient Elasticsearch client for Java applications with connection pooling, thread-safe client management, and configurable timeouts.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.blaspat/elasticsearchclient?color=blue)](https://search.maven.org/artifact/io.github.blaspat/elasticsearchclient)

## Maven

```xml
<dependency>
    <groupId>io.github.blaspat</groupId>
    <artifactId>elasticsearchclient</artifactId>
    <version>1.0.4</version>
</dependency>
```

## Features

- **Thread-safe client pool** — Uses `ConcurrentHashMap` for safe concurrent access
- **Connection pooling** — Configurable number of initial connections
- **Round-robin load balancing** — Clients are selected using atomic round-robin
- **Configurable timeouts** — Connection and socket timeouts via properties
- **Graceful shutdown** — All clients are properly closed via `@PreDestroy`

## Configuration

```yaml
elasticsearch:
  hosts: localhost:9200
  scheme: https
  username: elastic
  password: password
  connection:
    initConnections: 3
    connectTimeout: 5000
    socketTimeout: 30000
```

## Usage

```java
@Autowired
private ElasticsearchClientConfig elasticsearchClientConfig;

public void search() {
    ElasticsearchClient client = elasticsearchClientConfig.client();
    // use the client
}
```

## Notes

- ⚠️ **SSL certificate verification is disabled** — this library trusts all certificates. Do NOT use in production without proper certificate management.
- Clients are properly closed on application shutdown via `@PreDestroy`.
