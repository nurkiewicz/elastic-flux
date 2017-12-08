package com.nurkiewicz.elasticflux;

import lombok.Data;
import org.apache.http.HttpHost;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "elasticsearch")
@Data
public class ElasticsearchProperties {

    private List<String> hosts;
    private int connectTimeout;
    private int connectionRequestTimeout;
    private int socketTimeout;
    private int maxRetryTimeoutMillis;

    HttpHost[] hosts() {
        return hosts
                .stream()
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);
    }

}
