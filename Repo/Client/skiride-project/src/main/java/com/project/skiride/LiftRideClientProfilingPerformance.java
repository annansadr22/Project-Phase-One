package com.project.skiride;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class LiftRideClientProfilingPerformance {

    private static final String SERVER_URL = "http://140.238.158.223:8080/skiers/123/seasons/2024/days/6900/skiers/12345"; // Ensure this is correct
    private static final int NUM_THREADS = 10;
    private static final int NUM_REQUESTS_PER_THREAD = 1000;
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int MAX_SKIER_ID = 100000;
    private static final int MAX_RESORT_ID = 10;
    private static final int MAX_LIFT_ID = 40;
    private static final int SEASON_ID = 2022;
    private static final int DAY_ID = 1;
    private static final int MAX_TIME = 360;
    private static final Random random = new Random();
    //private static final HttpClient client = HttpClient.newHttpClient(); // Reuse HttpClient

    public static void main(String[] args) {

        List<RequestRecord> records = executeRequests();

        writeRecordsToCSV(records, "request_records.csv");

        calculateMetrics(records, "metrics.txt");
    }
    
    private static List<RequestRecord> executeRequests() {
        
        List<RequestRecord> records = new CopyOnWriteArrayList<>(); // Thread-safe list

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        
        AtomicLong successfulRequests = new AtomicLong(0);

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try {
                    long threadSuccessfulRequests = sendRequests(records); // Pass records list for recording
                    successfulRequests.addAndGet(threadSuccessfulRequests);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
        
        System.out.println("Successful Requests: " + successfulRequests.get());
        
        return records;
    }

    private static long sendRequests(List<RequestRecord> records) {
    	
        HttpClient client = HttpClient.newHttpClient();
        
        AtomicLong successfulRequests = new AtomicLong(0);
        
        // Get the current thread's Name
        String threadName = Thread.currentThread().getName();

        for (int i = 0; i < NUM_REQUESTS_PER_THREAD; i++) {
        	long startTime = System.currentTimeMillis();
        	
            HttpRequest request = buildRequest();
            
            int retryAttempts = 0;

            while (retryAttempts < MAX_RETRY_ATTEMPTS) {
            	
                try {
                	
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    
                    long endTime = System.currentTimeMillis();
                    long latency = endTime - startTime;
                    
                    if (response.statusCode() == 201) {
                    	
                        records.add(new RequestRecord(startTime, "POST", latency, response.statusCode()));
                        
                        successfulRequests.incrementAndGet();
                        
                        System.out.println("Request sent successfully from thread " + threadName);
                        
                        break; //
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

    private static void writeRecordsToCSV(List<RequestRecord> records, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("Start Time,Request Type,Latency (ms),Response Code\n");
            for (RequestRecord record : records) {
                writer.write(String.format("%d,%s,%d,%d\n", record.getStartTime(), record.getRequestType(),
                        record.getLatency(), record.getResponseCode()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    private static void calculateMetrics(List<RequestRecord> records, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
        	
            // Calculate mean, median, p99, min, max, and throughput
            double mean = records.stream().mapToLong(RequestRecord::getLatency).average().orElse(0);
            writer.write("Mean response time: " + mean + " milliseconds\n");
            System.out.println("Mean response time: " + mean + " milliseconds");

            List<Long> latencies = records.stream().map(RequestRecord::getLatency).sorted().collect(Collectors.toList());
            double median = latencies.size() % 2 == 0 ?
                    (latencies.get(latencies.size() / 2 - 1) + latencies.get(latencies.size() / 2)) / 2.0 :
                    latencies.get(latencies.size() / 2);
            writer.write("Median response time: " + median + " milliseconds\n");
            System.out.println("Median response time: " + median + " milliseconds");

            double p99 = latencies.get((int) (latencies.size() * 0.99));
            writer.write("P99 response time: " + p99 + " milliseconds\n");
            System.out.println("P99 response time: " + p99 + " milliseconds");

            long min = latencies.get(0);
            writer.write("Min response time: " + min + " milliseconds\n");
            System.out.println("Min response time: " + min + " milliseconds");

            long max = latencies.get(latencies.size() - 1);
            writer.write("Max response time: " + max + " milliseconds\n");
            System.out.println("Max response time: " + max + " milliseconds");

            long totalResponseTime = records.stream().mapToLong(RequestRecord::getLatency).sum();
            writer.write("Total response time: " + totalResponseTime + " milliseconds\n");
            System.out.println("Total response time: " + totalResponseTime + " milliseconds");

            long totalRequests = records.size();
            long totalTime = records.get(records.size() - 1).getStartTime() - records.get(0).getStartTime();
            double throughput = totalRequests / (totalTime / 1000.0); // Requests per second
            writer.write("Throughput: " + throughput + " requests per second\n");
            System.out.println("Throughput: " + throughput + " requests per second");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class RequestRecord {
        private long startTime;
        private String requestType;
        private long latency;
        private int responseCode;

        public RequestRecord(long startTime, String requestType, long latency, int responseCode) {
            this.startTime = startTime;
            this.requestType = requestType;
            this.latency = latency;
            this.responseCode = responseCode;
        }

        public long getStartTime() {
            return startTime;
        }

        public String getRequestType() {
            return requestType;
        }

        public long getLatency() {
            return latency;
        }

        public int getResponseCode() {
            return responseCode;
        }
    }
}

