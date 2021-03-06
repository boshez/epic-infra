package edu.colorado.cs.epic.filteringapi.resources;

import edu.colorado.cs.epic.filteringapi.api.FilteringQuery;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import io.dropwizard.jersey.params.IntParam;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@Path("/filtering/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class FilteringResource {
	private final Logger logger;
	private final LoadingCache<String, String> queryTempFilesCache;
	private final BigQuery bigquery;

	public FilteringResource(BigQuery bigqueryClient) {
		this.logger = Logger.getLogger(FilteringResource.class.getName());
		queryTempFilesCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES)
				.build(new CacheLoader<String, String>() {
					@Override
					public String load(String key) {
						return "";
					}
				});
		bigquery = bigqueryClient;
	}

	@POST
	@Path("/{eventName}")
	public String getFilteredTweetsByKeywords(@PathParam("eventName") String eventName,
			@QueryParam("page") @DefaultValue("1") @Min(1) IntParam page,
			@QueryParam("count") @DefaultValue("100") @Min(1) @Max(1000) IntParam pageCount,
			@NotNull @Valid FilteringQuery filterQuery) throws InterruptedException {

		int pageNumber = page.get();
		int pageSize = pageCount.get();
		String eventTableName = "tweets." + eventName.replace("-", "_");
		String bqTempFile = "";
		String query = "";

		// When searching for cached results
		// check if the requested query is found in the cache
		String paramString = filterQuery.paramString();
		String cacheName = eventName + paramString;
		try {
			bqTempFile = queryTempFilesCache.get(cacheName);
		} catch (ExecutionException e) {
			logger.error("Issue accessing Google Cloud", e);
			throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
		}

		if (bqTempFile.isEmpty()) {
			// The cached file is empty so create a new query for BigQuery
			query = filterQuery.getQueryString(eventTableName);
		} else {
			// Retrieve results from a temporary file cached by BigQuery
			query = String.format("SELECT * FROM `crypto-eon-164220.%s`", bqTempFile);
		}

		// Build the requested query
		QueryJobConfiguration.Builder queryConfigBuilder = QueryJobConfiguration.newBuilder(query)
				.setUseLegacySql(false).setUseQueryCache(true);
		QueryJobConfiguration queryConfig = queryConfigBuilder.build();

		return runQuery(queryConfig, paramString, eventName, pageNumber, pageSize, bqTempFile.isEmpty());

	}

	private String runQuery(QueryJobConfiguration queryConfig, String paramString, String eventName, Integer pageNumber,
			Integer pageSize, Boolean createCache) {
		try {
			// Create a job ID to safely retry running the query
			JobId jobId = JobId.of(UUID.randomUUID().toString());
			Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

			// Get the results
			QueryJobConfiguration queryJobConfig = queryJob.getConfiguration();

			// Build the final result object with meta data and the requested page of tweets
			StringBuilder tweets = new StringBuilder();

			// Prepare and append tweet list object
			JSONArray tweetArray = new JSONArray();
			TableResult pageToReturn = queryJob.getQueryResults(BigQuery.QueryResultsOption.pageSize(pageSize),
					BigQuery.QueryResultsOption.startIndex((pageNumber - 1) * pageSize));
			
			// Iterate through the requested page and append the tweets to the return object
			for (FieldValueList row : pageToReturn.getValues()) {
				JSONObject userObject = new JSONObject();
				userObject.put("name", row.get("name").getStringValue());
				userObject.put("screen_name", row.get("screen_name").getStringValue());
				userObject.put("verified", row.get("verified").getStringValue());
				userObject.put("created_at", row.get("created_at").getStringValue());
				userObject.put("statuses_count", row.get("statuses_count").getStringValue());
				userObject.put("favourites_count", row.get("favourites_count").getStringValue());
				userObject.put("followers_count", row.get("followers_count").getStringValue());
				userObject.put("friends_count", row.get("friends_count").getStringValue());
				userObject.put("profile_image_url_https", row.get("profile_image_url_https").getStringValue());

				// These values can be null so we cannot assume we can get a string value
				userObject.put("description", row.get("description").getValue());
				userObject.put("location", row.get("location").getValue());
				userObject.put("time_zone", row.get("time_zone").getValue());
				userObject.put("url", row.get("url").getValue());

				JSONObject tweetObject = new JSONObject();
				tweetObject.put("id_str", row.get("id_str").getStringValue());
				tweetObject.put("text", row.get("text").getStringValue());
				tweetObject.put("timestamp_ms", row.get("timestamp_ms").getStringValue());
				tweetObject.put("source", row.get("source").getStringValue());
				tweetObject.put("user", userObject);
				tweetArray.add(tweetObject);
			}
			tweets.append("{\"tweets\":");
			tweets.append(tweetArray.toJSONString());

			// Prepare and append meta data object to return object
			JSONObject metaObject = new JSONObject();
			metaObject.put("event_name", eventName);
			metaObject.put("params", paramString);
			metaObject.put("job_status", queryJob.getStatus().getState().toString());
			metaObject.put("page", pageNumber);
			metaObject.put("count", pageSize);
			metaObject.put("total_count", pageToReturn.getTotalRows());
			metaObject.put("num_pages", (int) Math.ceil((double) pageToReturn.getTotalRows() / pageSize));
			metaObject.put("tweet_count", tweetArray.size());
			tweets.append(",\"meta\":");
			tweets.append(metaObject.toJSONString());
			tweets.append("}");

			if (createCache) {
				// Cache query's temp file if first time requested query, or query not found in
				String destDataset = queryJobConfig.getDestinationTable().getDataset();
				String destTable = queryJobConfig.getDestinationTable().getTable();
				queryTempFilesCache.put(eventName + paramString, destDataset + '.' + destTable);
			}

			return tweets.toString();
		} catch (Exception e) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
	}
}