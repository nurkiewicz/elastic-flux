package com.nurkiewicz.elasticflux;

import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.index.IndexResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/person")
class PersonController {

    private static final Mono<ResponseEntity<Person>> NOT_FOUND = Mono.just(ResponseEntity.notFound().build());

    private final ElasticAdapter elasticAdapter;

    @PutMapping
    Mono<ResponseEntity<Map<String, Object>>> put(@Valid @RequestBody Person person) {
        return elasticAdapter
                .index(person)
                .map(this::toMap)
                .map(m -> ResponseEntity.status(HttpStatus.CREATED).body(m));
    }

    @GetMapping("/{userName}")
    Mono<ResponseEntity<Person>> get(@PathVariable("userName") String userName) {
        return elasticAdapter
                .findByUserName(userName)
                .map(ResponseEntity::ok)
                .switchIfEmpty(NOT_FOUND);
    }

    private ImmutableMap<String, Object> toMap(IndexResponse response) {
        return ImmutableMap
                .<String, Object>builder()
                .put("id", response.getId())
                .put("index", response.getIndex())
                .put("type", response.getType())
                .put("version", response.getVersion())
                .put("result", response.getResult().getLowercase())
                .put("seqNo", response.getSeqNo())
                .put("primaryTerm", response.getPrimaryTerm())
                .build();
    }

}

