Elasticsearch Simple Java Client
===========================

## Overview
This library provide easier way to config Elasticsearch Java API Client.  
It features automatically connection checking with auto re-create connection if connection is closed due to some error.  
This library avoid "Request execution cancelled" error when calling Elasticsearch client.

Support **Java 8 or later** and using [Elasticsearch Java API Client 8.11](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/8.11/index.html)

## Updates
* **1.0.3**
  * Add feature to setting connection timeout and socket timeout
  * Add feature to set up initial standby clients to use
  * Round robin method when selecting client to use
* **1.0.2**
  * Change how to use client by extending **ElasticsearchSimpleClient** class. Then you can use Elasticsearch client by using _**client()**_ syntax
  * Remove unnecessary class and method
* **1.0.1**
  * **DO NOT USE THIS VERSION, CONTAINS ISSUE**
* **1.0.0**
  * Initial release
  * Using [Elasticsearch Java API Client 8.11](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/8.11/index.html)


## Maven

    <dependency>
        <groupId>io.github.blaspat</groupId>
        <artifactId>elasticsearchclient</artifactId>
        <version>1.0.3</version>
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
        hosts: localhost:9200,localhost:9201
        username: elastic-username
        password: elastic-password
      connection:
        init-connections: 5
        connect-timeout: 100
        socket-timeout: 10000



* `scheme`: your Elasticsearch cluster scheme. You can choose one scheme, either **http** or **https**, this `scheme` will be applied to all of your hosts
* `hosts`: your Elasticsearch hosts with port. You can add multiple hosts, separated by comma
* `username`: your Elasticsearch username
* `password`: your Elasticsearch password
* `connection`: your Elasticsearch password
* * `init-connections`: initial client connections
* * `connect-timeout`: connect timeout setting in millis
* * `socket-timeout`: socket timeout setting in millis

## Usage
[Elasticsearch Demo Spring Boot](https://github.com/blaspat/elasticsearch-demo)

## Notes
* This library will skip Elasticsearch certificate verification

## License

This project is licensed under the [Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

The copyright owner is Blasius Patrick.
