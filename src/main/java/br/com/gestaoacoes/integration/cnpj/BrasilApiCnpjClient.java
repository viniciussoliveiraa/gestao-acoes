package br.com.gestaoacoes.integration.cnpj;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "brasilApiCnpj", url = "${app.integrations.brasil-api.base-url}")
interface BrasilApiCnpjClient {

    @GetMapping("/cnpj/v1/{cnpj}")
    BrasilApiCnpjResponse consultar(@PathVariable("cnpj") String cnpj);
}