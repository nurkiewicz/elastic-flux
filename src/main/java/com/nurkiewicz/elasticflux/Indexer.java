package com.nurkiewicz.elasticflux;

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

@Component
@Slf4j
@RequiredArgsConstructor
class Indexer {

    private final PersonGenerator personGenerator;
    private final RestHighLevelClient client;

    private void index(int count) {
        personGenerator
                .infinite()
                .take(count)
                .flatMap(this::indexDocSwallowErrors, 1)
                .window(Duration.ofSeconds(1))
                .flatMap(Flux::count)
                .subscribe(winSize -> log.debug("Got {} responses in last second", winSize));
    }

    private Mono<IndexResponse> indexDocSwallowErrors(Doc doc) {
        return indexDoc(doc)
                .doOnError(e -> log.error("Unable to index {}", doc, e))
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
        index(1_000_000);
    }

}
