package com.cinesearch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WatchProvidersResponse(
    String link,
    List<ProviderDto> flatrate,
    List<ProviderDto> rent,
    List<ProviderDto> buy
) {

    public record ProviderDto(
        @JsonProperty("provider_id") Integer provider_id,
        @JsonProperty("provider_name") String provider_name,
        @JsonProperty("logo_path") String logo_path
    ) {}
}
