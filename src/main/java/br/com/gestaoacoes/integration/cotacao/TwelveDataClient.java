package br.com.gestaoacoes.integration.cotacao;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "twelveData", url = "${app.integrations.twelve-data.base-url}")
interface TwelveDataClient {

    @GetMapping("/quote")
    TwelveDataResponse consultar(@RequestParam("symbol") String symbol, @RequestParam("apikey") String apiKey);
}