package edu.colorado.cs.epic.eventsapi.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.jackson.JsonSnakeCase;
import org.hibernate.validator.constraints.NotEmpty;

@JsonSnakeCase
public class TweetAnnotation {
    @NotEmpty
    private String tag;

    @NotEmpty
    private String tweetId;
    @NotEmpty
    private String eventName;

    private String authUser;

    public TweetAnnotation(String tag, String tweetId, String eventName) {
        this.tag = tag;

        this.tweetId = tweetId;
        this.eventName = eventName;
    }

    public TweetAnnotation() {
    }

    @JsonProperty
    public String getTag() {
        return tag;
    }

    @JsonProperty
    public void setTag(String tag) {
        this.tag = normalizeTag(tag);
    }


    @JsonProperty
    public String getTweetId() {
        return tweetId;
    }

    @JsonProperty
    public void setTweetId(String tweetId) {
        this.tweetId = tweetId;
    }

    @JsonProperty
    public String getEventName() {
        return eventName;
    }

    @JsonProperty
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    @JsonProperty
    public String getAuthUser() {
        return authUser;
    }

    @JsonProperty
    public void setAuthUser(String authUser) {
        this.authUser = authUser;
    }

    private static String normalizeTag(String tag) {
        return tag.replaceAll("\\s+", "-")
                .replaceAll("[^-a-zA-Z0-9]", "").toLowerCase();
    }
}

