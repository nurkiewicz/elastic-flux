package com.nurkiewicz.elasticflux;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

@Configuration
class Config {

    @Bean
    ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    }

    @Bean
    RestHighLevelClient restHighLevelClient(ElasticsearchProperties props) {
        return new RestHighLevelClient(
                RestClient
                        .builder(props.hosts())
                        .setRequestConfigCallback(config -> config
                                .setConnectTimeout(props.getConnectTimeout())
                                .setConnectionRequestTimeout(props.getConnectionRequestTimeout())
                                .setSocketTimeout(props.getSocketTimeout())
                        )
                        .setMaxRetryTimeoutMillis(props.getMaxRetryTimeoutMillis()));
    }

    @Bean
    MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }

    @Bean
    Slf4jReporter slf4jReporter() {
        final Slf4jReporter slf4jReporter = Slf4jReporter.forRegistry(metricRegistry()).build();
        slf4jReporter.start(1, TimeUnit.SECONDS);
        return slf4jReporter;
    }

    @Bean
    GraphiteReporter graphiteReporter(MetricRegistry metricRegistry) {
        final Graphite graphite = new Graphite(new InetSocketAddress("localhost", 2003));
        final GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry)
                .prefixedWith("elastic-flux")
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(graphite);
        reporter.start(1, TimeUnit.SECONDS);
        return reporter;
    }

    @Bean
    EsMetrics esMetrics() {
        return new EsMetrics(
                metricRegistry().timer(name("es", "index")),
                metricRegistry().counter(name("es", "concurrent")),
                metricRegistry().counter(name("es", "successes")),
                metricRegistry().counter(name("es", "failures"))
        );
    }

}
