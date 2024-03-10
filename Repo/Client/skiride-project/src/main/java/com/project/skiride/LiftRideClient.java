package com.project.skiride;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class LiftRideClient {

    private static final String SERVER_URL = "http://140.238.158.223:8080/skiers/123/seasons/2024/days/6900/skiers/12345"; // Replace with your server URL
    private static final int INITIAL_NUM_THREADS = 10;
    private static final int NUM_REQUESTS_PER_THREAD = 1000;
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int MAX_SKIER_ID = 100000;
    private static final int MAX_RESORT_ID = 10;
    private static final int MAX_LIFT_ID = 40;
    private static final int SEASON_ID = 2022;
    private static final int DAY_ID = 1;
    private static final int MAX_TIME = 360;

    private static final Random random = new Random();

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(INITIAL_NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(INITIAL_NUM_THREADS);
        // final CountDownLatch latch = new CountDownLatch(INITIAL_NUM_THREADS);
        AtomicLong successfulRequests = new AtomicLong(0);

        // First batch of threads
        for (int i = 0; i < INITIAL_NUM_THREADS; i++) {
            executor.submit(() -> {
                try {
                    long threadSuccessfulRequests = sendRequests();
                    successfulRequests.addAndGet(threadSuccessfulRequests);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for the first batch of threads to finish
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        // Calculate remaining requests
         long remainingRequests = 10000 - successfulRequests.get();

        // Second batch of threads if necessary
        if (remainingRequests > 0) {
            int remainingThreads = Math.min(16, (int) (remainingRequests / NUM_REQUESTS_PER_THREAD));
            ExecutorService additionalExecutor = Executors.newFixedThreadPool(remainingThreads);
            CountDownLatch additionalLatch = new CountDownLatch(remainingThreads);

            for (int i = 0; i < remainingThreads; i++) {
                additionalExecutor.submit(() -> {
                    try {
                        long threadSuccessfulRequests = sendRequests();
                        successfulRequests.addAndGet(threadSuccessfulRequests);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        additionalLatch.countDown();
                    }
                });
            }

            // Wait for the second batch of threads to finish
            try {
                additionalLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                additionalExecutor.shutdown();
            }
        }

        long endTime = System.currentTimeMillis();

        long totalTime = endTime - startTime;
        double throughput = (double) successfulRequests.get() / (totalTime / 1000.0); // Requests per second
        double unsuccessfulRequests = ((double) successfulRequests.get() >= 10000) ? 0 : (10000 - (double) successfulRequests.get());

        System.out.println("Number of successful requests sent: " + successfulRequests.get());
        System.out.println("Number of unsuccessful requests sent: " + unsuccessfulRequests);
        System.out.println("Total run time: " + totalTime + " milliseconds");
        System.out.println("Total throughput: " + throughput + " requests per second");
    }


    private static long sendRequests() {
    	
        HttpClient client = HttpClient.newHttpClient();
        
        AtomicLong successfulRequests = new AtomicLong(0);
        
        // Get the current thread's Name
        String threadName = Thread.currentThread().getName();

        for (int i = 0; i < NUM_REQUESTS_PER_THREAD; i++) {
        	
            HttpRequest request = buildRequest();
            
            int retryAttempts = 0;

            while (retryAttempts < MAX_RETRY_ATTEMPTS) {
            	
                try {
                	
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 201) {
                    	
                        successfulRequests.incrementAndGet(); // Increment successful requests counter
                        
                        System.out.println("Request sent successfully from thread " + threadName);
                        
                        break; // Success
                    } else {
                    	
                    	System.out.println("Request not sent from thread " + threadName);
                    	System.exit(0);
                    }
                } catch (Exception e) {
                	
                    e.printStackTrace();
                    
                }
                retryAttempts++;
            }
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

