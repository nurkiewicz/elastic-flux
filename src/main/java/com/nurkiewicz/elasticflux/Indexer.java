package com.nurkiewicz.elasticflux;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

@Component
@Slf4j
@RequiredArgsConstructor
class Indexer {

    private final PersonGenerator personGenerator;
    private final RestHighLevelClient client;

    private final Timer indexTimer = Metrics.timer("es.timer");
    private final LongAdder concurrent = Metrics.gauge("es.concurrent", new LongAdder());
    private final Counter successes = Metrics.counter("es.index", "result", "success");
    private final Counter failures = Metrics.counter("es.index", "result", "failure");

    private Flux<IndexResponse> index(int count, int concurrency) {
        return personGenerator
                .infinite()
                .take(count)
                .flatMap(doc -> countConcurrent(measure(indexDocSwallowErrors(doc))), concurrency);
    }

    private <T> Mono<T> countConcurrent(Mono<T> input) {
        return input
                .doOnSubscribe(s -> concurrent.increment())
                .doOnTerminate(concurrent::decrement);
    }

    private <T> Mono<T> measure(Mono<T> input) {
        return Mono
                .fromCallable(System::currentTimeMillis)
                .flatMap(time ->
                        input.doOnSuccess(x -> indexTimer.record(System.currentTimeMillis() - time, TimeUnit.MILLISECONDS))
                );
    }

    private Mono<IndexResponse> indexDocSwallowErrors(Doc doc) {
        return indexDoc(doc)
                .doOnSuccess(response -> successes.increment())
                .doOnError(e -> log.error("Unable to index {}", doc, e))
                .doOnError(e -> failures.increment())
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<IndexResponse> indexDoc(Doc doc) {
        return Mono.create(sink -> {
            final IndexRequest indexRequest = new IndexRequest("people", "person", doc.getUsername());
            indexRequest.source(doc.getJson(), XContentType.JSON);
            client.indexAsync(indexRequest, listenerToSink(sink));
        });
    }

    private ActionListener<IndexResponse> listenerToSink(MonoSink<IndexResponse> sink) {
        return new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse indexResponse) {
                sink.success(indexResponse);
            }

            @Override
            public void onFailure(Exception e) {
                sink.error(e);
            }
        };
    }

    @PostConstruct
    void startIndexing() {
        Flux
                .range(0, 5_000)
                .map(x -> Math.max(1, x * 10))
                .doOnNext(x -> log.debug("Target concurrency: {}", x))
                .concatMap(concurrency -> index(5_000, concurrency))
                .window(Duration.ofSeconds(1))
                .flatMap(Flux::count)
                .subscribe(winSize -> log.debug("Got {} responses in last second", winSize));
    }

}
