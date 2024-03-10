package com.project.skiride;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class LiftRideClientLittleLawTest {

    private static final String SERVER_URL = "http://140.238.158.223:8080/skiers/123/seasons/2024/days/6900/skiers/12345";
    private static final int NUM_REQUESTS = 500; // Step 1: Number of requests to send
    private static final Random random = new Random();
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int MAX_SKIER_ID = 100000;
    private static final int MAX_RESORT_ID = 10;
    private static final int MAX_LIFT_ID = 40;
    private static final int MAX_TIME = 360;
    private static final int DAY_ID = 1;
    private static final int SEASON_ID = 2022;

    public static void main(String[] args) {
        
        // Step 1: Send 500 requests from a single thread
        long startTime = System.currentTimeMillis();
        AtomicLong successfulRequests = new AtomicLong(0);
        
        for (int i = 0; i < NUM_REQUESTS; i++) {
            long threadSuccessfulRequests = sendRequests(); // Assuming this function returns the number of successful requests (e.g., 1 or 0)
            successfulRequests.addAndGet(threadSuccessfulRequests);
        }
        
        long endTime = System.currentTimeMillis();
        
        long totalTimeMillis = endTime - startTime;
        double totalTimeSeconds = totalTimeMillis / 1000.0; // Convert milliseconds to seconds

        // Step 2: Calculate the average time for a single request (W) in seconds
        double averageTimePerRequestSeconds = totalTimeSeconds / successfulRequests.get();

        // Step 3: Calculate the arrival rate (λ) in requests per second
        double arrivalRatePerSecond = successfulRequests.get() / totalTimeSeconds;

        // Step 4: Calculate the average number of requests in the system (L)
        double averageNumRequests = arrivalRatePerSecond * averageTimePerRequestSeconds;

        // Step 5: Calculate the throughput, which should equal the arrival rate in a stable system
        double throughput = arrivalRatePerSecond; // Throughput equals the arrival rate in requests per second
        
        double unsuccessfulRequests = NUM_REQUESTS - successfulRequests.get();

        // Print results
        // System.out.println("Number of successful requests sent: " + successfulRequests.get());
        System.out.println("Number of unsuccessful requests sent: " + unsuccessfulRequests);
        System.out.println("Total run time: " + totalTimeMillis + " milliseconds");
        System.out.println("Average Time per Request (W): " + averageTimePerRequestSeconds + " seconds");
        System.out.println("Arrival Rate (λ): " + arrivalRatePerSecond + " requests/second");
        System.out.println("Average Number of Requests in the System (L): " + averageNumRequests);
        System.out.println("Throughput: " + throughput + " requests/second");
    }


    private static long sendRequests() {
        HttpClient client = HttpClient.newHttpClient();
        AtomicLong successfulRequests = new AtomicLong(0);

        // Get the current thread's Name
        String threadName = Thread.currentThread().getName();

        HttpRequest request = buildRequest();
        int retryAttempts = 0;

        while (retryAttempts < MAX_RETRY_ATTEMPTS) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 201) {
                    successfulRequests.incrementAndGet(); // Increment successful requests counter
                    System.out.println("Request sent successfully from thread " + threadName);
                    break; // Success
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            retryAttempts++;
        }

        return successfulRequests.get();
    }



    private static HttpRequest buildRequest() {
        int skierId = random.nextInt(MAX_SKIER_ID) + 1;
        int resortId = random.nextInt(MAX_RESORT_ID) + 1;
        int liftId = random.nextInt(MAX_LIFT_ID) + 1;
        int time = random.nextInt(MAX_TIME) + 1;

        String requestBody = String.format("{\"skierID\":%d,\"resortID\":%d,\"liftID\":%d,\"seasonID\":%d,\"dayID\":%d,\"time\":%d}",
                skierId, resortId, liftId, SEASON_ID, DAY_ID, time);

        return HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

}
