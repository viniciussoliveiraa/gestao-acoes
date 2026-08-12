package br.com.gestaoacoes.integration.cotacao;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "brapi", url = "${app.integrations.brapi.base-url}")
interface BrapiClient {

    @GetMapping("/api/v2/stocks/quote")
    BrapiResponse consultar(@RequestParam("symbols") String symbols,
                             @RequestHeader(value = "Authorization", required = false) String authorization);
}