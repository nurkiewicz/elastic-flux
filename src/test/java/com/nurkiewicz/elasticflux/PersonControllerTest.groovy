package com.nurkiewicz.elasticflux

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import static org.springframework.http.HttpMethod.PUT
import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.OK
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8

@ContextConfiguration
@SpringBootTest(webEnvironment = RANDOM_PORT)
class PersonControllerTest extends Specification {

    @Value('${local.server.port}')
    int port

    TestRestTemplate rest = new TestRestTemplate()

    def 'should index document'() {
        given:
            HttpEntity<String> request = indexRequest(Samples.DOCUMENT)
        when:
            ResponseEntity<String> response = rest.exchange(url(), PUT, request, String)
        then:
            response.statusCode == CREATED
    }

    def 'should find indexed document'() {
        given:
            assert rest.exchange(url(), PUT, indexRequest(Samples.DOCUMENT), String).statusCode == CREATED
        when:
            ResponseEntity<Map> response = rest.getForEntity(url() + "/" + Samples.USER_NAME, Map)
        then:
            response.statusCode == OK
            response.body.address.street == 'Washington Walk'
    }

    private String url() {
        return "http://localhost:$port/person"
    }

    HttpEntity<String> indexRequest(String document) {
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(APPLICATION_JSON_UTF8)
        return new HttpEntity<String>(document, headers)
    }

}
