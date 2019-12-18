package edu.colorado.cs.epic.geoupdateapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;

public class GeoUpdateConfiguration extends Configuration {
    @NotNull
    private Boolean production;

    public GeoUpdateConfiguration() {
    }

    @JsonProperty
    public Boolean getProduction() {
        return production;
    }

    @JsonProperty
    public void setProduction(Boolean production) {
        this.production = production;
    }
}
