package services.grecru;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AIClientService {
    public static final String AI_SERVER_URL = "http://127.0.0.1:5000/evaluate";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EvaluationResponse evaluatePythonCode(String code, String missionType) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("code", code);
        request.put("tests", getTestCasesForMission(missionType));

        String jsonPayload = objectMapper.writeValueAsString(request);
        System.out.println("📤 Sending to AI Server: " + jsonPayload);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(AI_SERVER_URL);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(jsonPayload));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                System.out.println("📥 Response from AI Server: " + responseBody);

                if (response.getStatusLine().getStatusCode() == 200) {
                    return objectMapper.readValue(responseBody, EvaluationResponse.class);
                } else {
                    throw new Exception("AI Server error: " + response.getStatusLine().getStatusCode() +
                            " - " + responseBody);
                }
            }
        }
    }

    private List<Map<String, String>> getTestCasesForMission(String missionType) {
        List<Map<String, String>> tests = new ArrayList<>();

        switch (missionType.toUpperCase()) {
            case "ADDITION":
                addTest(tests, "5\n3\n", "8");
                addTest(tests, "-2\n7\n", "5");
                addTest(tests, "0\n0\n", "0");
                break;

            case "FACTORIAL":
                addTest(tests, "5\n", "120");
                addTest(tests, "0\n", "1");
                addTest(tests, "7\n", "5040");
                break;

            case "FIBONACCI":
                addTest(tests, "5\n", "5");
                addTest(tests, "7\n", "13");
                addTest(tests, "10\n", "55");
                break;

            case "PRIME_CHECK":
                addTest(tests, "7\n", "True");
                addTest(tests, "10\n", "False");
                addTest(tests, "17\n", "True");
                break;

            default:
                addTest(tests, "test\n", "test output");
        }

        return tests;
    }

    private void addTest(List<Map<String, String>> tests, String input, String expected) {
        Map<String, String> test = new HashMap<>();
        test.put("input", input);
        test.put("expected", expected);
        tests.add(test);
    }
}