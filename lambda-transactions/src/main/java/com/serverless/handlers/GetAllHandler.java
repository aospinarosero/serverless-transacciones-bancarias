package com.serverless.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetAllHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private static final Gson gson = new Gson();
    private static final String TABLE_NAME = "TransaccionesBancarias";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            List<Map<String, String>> items = new ArrayList<>();
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(TABLE_NAME)
                    .build();

            ScanResponse response;
            do {
                response = dynamoDb.scan(scanRequest);
                for (Map<String, AttributeValue> item : response.items()) {
                    items.add(convertItem(item));
                }
                if (response.hasLastEvaluatedKey()) {
                    scanRequest = ScanRequest.builder()
                            .tableName(TABLE_NAME)
                            .exclusiveStartKey(response.lastEvaluatedKey())
                            .build();
                }
            } while (response.hasLastEvaluatedKey());

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
