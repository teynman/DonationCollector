package db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import entity.Item;
import entity.User;

public class ElasticSearchConnection {
	private RestHighLevelClient client;

	public void elasticSearchConnection() {
		client = new RestHighLevelClient(RestClient.builder(new HttpHost(ElasticSearchDBUtil.INSTANCE,
				ElasticSearchDBUtil.PORT_NUM, ElasticSearchDBUtil.CONNECTION_TYPE)));
	}

	// Change this type to the actual user type once ready
	public void addUser(User user) {
		JSONObject userObj = user.toJSONObject();
		XContentBuilder builder;
		try {
			builder = XContentFactory.jsonBuilder();
			builder.startObject();
			{
				builder.field("userId", userObj.getString("user_id"));
				builder.field("userName", userObj.getString("name"));
				builder.field("userType", userObj.getString("UserType"));
				builder.field("email", userObj.getString("email"));
				builder.field("address", userObj.getString("address"));
				// to-fix hard-coded for now
				builder.timeField("postDate", "2020-06-30");
			}
			builder.endObject();
			IndexRequest indexRequest = new IndexRequest("users").id(userObj.getString("user_id")).source(builder);
			IndexResponse response = client.index(indexRequest, RequestOptions.DEFAULT);
			System.out.print(response);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// Please change return type according to how you want status code to show up.
	public String addItem(Item item) {

		JSONObject itemObj = item.toJSONObject();
		JSONObject posterObj = itemObj.getJSONObject("poster_user");
		String posterId = posterObj.getString("user_id");
		JSONObject ngoObj = itemObj.getJSONObject("NGO_user");
		String ngoId = ngoObj.getString("user_id");

		String lat = itemObj.getString("lat");
		String lon = itemObj.getString("lon");

		String geoPint = lat + ", " + lon;

		XContentBuilder builder;
		try {
			builder = XContentFactory.jsonBuilder();
			builder.startObject();
			{
				builder.field("itemId", itemObj.getString("item_id"));
				builder.field("urlToImage", itemObj.getString("urlToImage"));
				builder.field("locationLatLon", geoPint);
				builder.field("locationAddress", itemObj.getString("location"));
				builder.field("posterId", posterId);
				builder.field("category", itemObj.getString("category"));
				builder.field("description", itemObj.getString("description"));
				builder.field("availablePickUpTime", itemObj.getJSONArray("schedule"));
				builder.field("itemStatus", itemObj.getString("status"));
				builder.field("pickUpNGOId", ngoId);
				builder.field("pickUpTime", itemObj.getString("pick_up_date"));
				// to-fix hard-coded for now
				builder.timeField("postDate", "2020-06-30");
			}
			builder.endObject();
			IndexRequest indexRequest = new IndexRequest("items").id(itemObj.getString("item_id")).source(builder);
			IndexResponse response = client.index(indexRequest, RequestOptions.DEFAULT);
			return "Success";
		} catch (Exception e) {
			e.printStackTrace();
			return "Error";
		}

	}
	// helpful search API reference:
	// https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-search.html

	// placeholder
	public ArrayList<Map<String, Object>> queryItemByLocation(double lat, double lng, double distance) throws IOException {
		ArrayList<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
		SearchRequest searchRequest = new SearchRequest();
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		QueryBuilder query = QueryBuilders.matchAllQuery();
		QueryBuilder geoDistanceQueryBuilder = QueryBuilders
	            .geoDistanceQuery("geoPoint")
	            .point(lat, lng)
	            .distance(distance, DistanceUnit.KILOMETERS);
		QueryBuilder finalQuery = QueryBuilders.boolQuery().must(query).filter(geoDistanceQueryBuilder);
		sourceBuilder.query(finalQuery);
        searchRequest.source(sourceBuilder);
        
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        
     
        for (SearchHit hit : hits) {
        	resultList.add(hit.getSourceAsMap());
        }
        return resultList;

	}

	public ArrayList<Map<String, Object>> queryItemByPosterId(String userId) {

		ArrayList<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		SearchRequest request = new SearchRequest("items");
		SearchSourceBuilder scb = new SearchSourceBuilder();

		MatchQueryBuilder mqb = new MatchQueryBuilder("posterId", userId);
		scb.query(mqb);

		request.source(scb);
		try {
			SearchResponse response = client.search(request, RequestOptions.DEFAULT);
			SearchHits hits = response.getHits();
			SearchHit[] searchHits = hits.getHits();
			for (SearchHit hit : searchHits) {
				resultList.add(hit.getSourceAsMap());
			}
			return resultList;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return resultList;
		}

	}

	public ArrayList<Map<String, Object>> queryItemByPickUpNGOId(String userId) {

		ArrayList<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		SearchRequest request = new SearchRequest("items");
		SearchSourceBuilder scb = new SearchSourceBuilder();

		MatchQueryBuilder mqb = new MatchQueryBuilder("pickUpNGOId", userId);
		scb.query(mqb);

		request.source(scb);
		try {
			SearchResponse response = client.search(request, RequestOptions.DEFAULT);
			SearchHits hits = response.getHits();
			SearchHit[] searchHits = hits.getHits();
			for (SearchHit hit : searchHits) {
				resultList.add(hit.getSourceAsMap());
			}
			return resultList;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return resultList;
		}

	}

	public Map<String, Object> queryItemByItemId(String itemId) {

		Map<String, Object> result;

		SearchRequest request = new SearchRequest("items");
		SearchSourceBuilder scb = new SearchSourceBuilder();

		IdsQueryBuilder iqb = new IdsQueryBuilder();
		iqb.addIds(itemId);
		scb.query(iqb);
		request.source(scb);
		try {
			SearchResponse response = client.search(request, RequestOptions.DEFAULT);
			SearchHits hits = response.getHits();
			SearchHit[] searchHits = hits.getHits();
			result = searchHits[0].getSourceAsMap();

			return result;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = new HashMap<String, Object>();
			return result;
		}
	}

}
