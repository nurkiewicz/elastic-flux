package com.nurkiewicz.elasticflux;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

@Component
@Slf4j
@RequiredArgsConstructor
class ElasticAdapter {

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;

    private final Timer indexTimer = Metrics.timer("es.timer");
    private final LongAdder concurrent = Metrics.gauge("es.concurrent", new LongAdder());
    private final Counter successes = Metrics.counter("es.index", "result", "success");
    private final Counter failures = Metrics.counter("es.index", "result", "failure");

    Mono<Person> findByUserName(String userName) {
        return Mono
                .<GetResponse>create(sink ->
                        client.getAsync(new GetRequest("people", "person", userName), listenerToSink(sink))
                )
                .filter(GetResponse::isExists)
                .map(GetResponse::getSource)
                .map(map -> objectMapper.convertValue(map, Person.class));
    }

    Mono<IndexResponse> index(Person doc) {
        return indexDoc(doc)
                .compose(this::countSuccFail)
                .compose(this::countConcurrent)
                .compose(this::measureTime)
                .doOnError(e -> log.error("Unable to index {}", doc, e));
    }

    private Mono<IndexResponse> countConcurrent(Mono<IndexResponse> mono) {
        return mono
                .doOnSubscribe(s -> concurrent.increment())
                .doOnTerminate(concurrent::decrement);
    }

    private Mono<IndexResponse> measureTime(Mono<IndexResponse> mono) {
        return Mono
                .fromCallable(System::currentTimeMillis)
                .flatMap(time ->
                        mono.doOnSuccess(response ->
                                indexTimer.record(System.currentTimeMillis() - time, TimeUnit.MILLISECONDS))
                );
    }

    private Mono<IndexResponse> countSuccFail(Mono<IndexResponse> mono) {
        return mono
                .doOnError(e -> failures.increment())
                .doOnSuccess(response -> successes.increment());
    }

    private Mono<IndexResponse> indexDoc(Person doc) {
        return Mono.create(sink -> {
            try {
                doIndex(doc, listenerToSink(sink));
            } catch (JsonProcessingException e) {
                sink.error(e);
            }
        });
    }

    private void doIndex(Person doc, ActionListener<IndexResponse> listener) throws JsonProcessingException {
        final IndexRequest indexRequest = new IndexRequest("people", "person", doc.getUsername());
        final String json = objectMapper.writeValueAsString(doc);
        indexRequest.source(json, XContentType.JSON);
        client.indexAsync(indexRequest, listener);
    }

    private <T> ActionListener<T> listenerToSink(MonoSink<T> sink) {
        return new ActionListener<T>() {
            @Override
            public void onResponse(T response) {
                sink.success(response);
            }

            @Override
            public void onFailure(Exception e) {
                sink.error(e);
            }
        };
    }

}
