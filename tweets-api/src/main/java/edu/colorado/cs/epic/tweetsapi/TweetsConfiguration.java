package edu.colorado.cs.epic.tweetsapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;


public class TweetsConfiguration extends Configuration {

    @NotNull
    private Boolean production;

    @NotNull
    private String projectId;

    @JsonProperty
    public Boolean getProduction() {
        return production;
    }

    @JsonProperty
    public void setProduction(Boolean production) {
        this.production = production;
    }

    @JsonProperty
    public String getProjectId() {
        return projectId;
    }

    @JsonProperty
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
