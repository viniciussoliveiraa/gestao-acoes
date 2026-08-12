package br.com.gestaoacoes.integration.instituicao;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "brasilApiCvm", url = "${app.integrations.brasil-api.base-url}")
interface BrasilApiCvmClient {

    @GetMapping("/cvm/corretoras/v1/{cnpj}")
    BrasilApiCvmResponse consultar(@PathVariable("cnpj") String cnpj);
}