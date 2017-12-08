package com.nurkiewicz.elasticflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ElasticFluxApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(ElasticFluxApplication.class, args);
	}

	/*private void run() throws IOException, InterruptedException {
		try (RestHighLevelClient client = new RestHighLevelClient(
				RestClient.builder(
						new HttpHost("localhost", 9200, "http")))) {
			System.out.println(client.search(new SearchRequest("accounts")));
		}
		TimeUnit.SECONDS.sleep(10);
	}*/
}
