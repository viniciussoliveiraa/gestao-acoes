package br.com.gestaoacoes.integration.cotacao;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "alphaVantage", url = "${app.integrations.alpha-vantage.base-url}")
interface AlphaVantageClient {

    @GetMapping("/query")
    AlphaVantageResponse consultar(@RequestParam("function") String function,
                                    @RequestParam("symbol") String symbol,
                                    @RequestParam("apikey") String apiKey);
}