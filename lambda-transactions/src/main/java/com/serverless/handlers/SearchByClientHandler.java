package com.serverless.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchByClientHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private static final Gson gson = new Gson();
    private static final String TABLE_NAME = "TransaccionesBancarias";
    private static final String INDEX_NAME = "ClientNameIndex";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            Map<String, String> queryParams = input.getQueryStringParameters();
            if (queryParams == null || !queryParams.containsKey("clientName")) {
                return buildResponse(400, gson.toJson(Map.of("error", "El parametro clientName es requerido")));
            }

            String clientName = queryParams.get("clientName");

            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .indexName(INDEX_NAME)
                    .keyConditionExpression("clientName = :name")
                    .expressionAttributeValues(Map.of(":name", AttributeValue.builder().s(clientName).build()))
                    .build();

            QueryResponse response = dynamoDb.query(queryRequest);
            List<Map<String, String>> items = new ArrayList<>();
            for (Map<String, AttributeValue> item : response.items()) {
                items.add(convertItem(item));
            }

            return buildResponse(200, gson.toJson(items));
        } catch (Exception e) {
            return buildResponse(500, gson.toJson(Map.of("error", e.getMessage())));
        }
    }

    private Map<String, String> convertItem(Map<String, AttributeValue> item) {
        Map<String, String> map = new HashMap<>();
        item.forEach((key, value) -> {
            switch (value.type()) {
                case S -> map.put(key, value.s());
                case N -> map.put(key, value.n());
                default -> map.put(key, value.toString());
            }
        });
        return map;
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(body);
    }
}
