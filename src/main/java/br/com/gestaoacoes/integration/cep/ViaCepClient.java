package br.com.gestaoacoes.integration.cep;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "viaCep", url = "${app.integrations.via-cep.base-url}")
interface ViaCepClient {

    @GetMapping("/{cep}/json/")
    ViaCepResponse consultar(@PathVariable("cep") String cep);
}