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

package io.github.blaspat.helper;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConfigurationProperties(prefix = "elasticsearch")
public class ElasticsearchProperties {
    private String hosts;
    private String host;
    private String username;
    private String scheme;
    private String password;
    private Connection connection;

    public ElasticsearchProperties() {
    }

    @Deprecated
    @DeprecatedConfigurationProperty(replacement = "elasticsearch.hosts")
    public String getHost() {
        return host;
    }

    @Deprecated
    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHosts() {
        if (null != hosts && !hosts.isEmpty()) return hosts;
        return host;
    }

    public Connection getConnection() {
        if (Objects.isNull(connection)) return new Connection();
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    public static class Connection {
        private Integer connectTimeout = 1000;
        private Integer socketTimeout = 30000;
        private Integer initConnections = 1;

        public Integer getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Integer connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Integer getSocketTimeout() {
            return socketTimeout;
        }

        public void setSocketTimeout(Integer socketTimeout) {
            this.socketTimeout = socketTimeout;
        }

        public Integer getInitConnections() {
            return initConnections;
        }

        public void setInitConnections(Integer initConnections) {
            this.initConnections = initConnections;
        }
    }
}
