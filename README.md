Elasticsearch Simple Java Client
===========================

## Overview
This library provide easier way to config Elasticsearch Java API Client. It features automatically connection checking with auto re-create connection if connection is closed due to some error.

Support Java 8 and using [Elasticsearch Java API Client 8.11](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/8.11/index.html)



## Maven

    <dependency>
        <groupId>io.github.blaspat</groupId>
        <artifactId>elasticsearchclient</artifactId>
        <version>1.0.0</version>
    </dependency>
    <!-- optional, only if your application fails with ClassNotFoundException: jakarta.json.spi.JsonProvider. -->
    <dependency>
      <groupId>jakarta.json</groupId>
      <artifactId>jakarta.json-api</artifactId>
      <version>2.0.1</version>
    </dependency>


## Configuration
Add the properties below to your application properties file

    elasticsearch:
        scheme: http
        host: localhost:9200,localhost:9201
        username: elastic-username
        password: elastic-password


* `scheme`: your Elasticsearch cluster schem. You can choose one scheme, either **http** or **https**, this `scheme` will be applied to all of your hosts
* `host`: your Elasticsearch hosts with port. You can add multiple hosts, separated by comma
* `username`: your Elasticsearch username
* `password`: your Elasticsearch password
  
## Notes
* For now, this library will skip Elasticsearch certificate verification

## License

This project is licensed under the [Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

The copyright owner is Blasius Patrick.
