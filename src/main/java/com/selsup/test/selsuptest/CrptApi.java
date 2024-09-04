package com.selsup.test.selsuptest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {

    private final static String API_URL = "https://ismp.crpt.ru/api/v3/1k/documents/create";
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final AtomicInteger requestCount = new AtomicInteger(0);

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit);
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();

        this.scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            semaphore.release(requestLimit - semaphore.availablePermits());
        }, 0, 1, timeUnit);
    }

    public String createDocument(Document document, String signature) throws IOException, InterruptedException {
        semaphore.acquire();

        requestCount.incrementAndGet();

        String requestBody = objectMapper.writeValueAsString(document);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .header("Signature", signature)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    @Getter
    @Setter
    public static class Document {

        @JsonProperty("description")
        private Description description;

        @JsonProperty("doc_id")
        private String docId;

        @JsonProperty("doc_status")
        private String docStatus;

        @JsonProperty("doc_type")
        private String docType;

        @JsonProperty("importRequest")
        private boolean importRequest;

        @JsonProperty("owner_inn")
        private String ownerInn;

        @JsonProperty("participant_inn")
        private String participantInn;

        @JsonProperty("producer_inn")
        private String producerInn;

        @JsonProperty("production_date")
        private String productionDate;

        @JsonProperty("production_type")
        private String productionType;

        @JsonProperty("products")
        private List<Product> products;

        @JsonProperty("reg_date")
        private String regDate;

        @JsonProperty("reg_number")
        private String regNumber;

        @Getter
        @Setter
        public static class Description {
            @JsonProperty("participantInn")
            private String participantInn;
        }

        @Getter
        @Setter
        public static class Product {

            @JsonProperty("certificate_document")
            private String certificateDocument;

            @JsonProperty("certificate_document_date")
            private String certificateDocumentDate;

            @JsonProperty("certificate_document_number")
            private String certificateDocumentNumber;

            @JsonProperty("owner_inn")
            private String ownerInn;

            @JsonProperty("producer_inn")
            private String producerInn;

            @JsonProperty("production_date")
            private String productionDate;

            @JsonProperty("tnved_code")
            private String tnvedCode;

            @JsonProperty("uit_code")
            private String uitCode;

            @JsonProperty("uitu_code")
            private String uituCode;
        }
    }

    public int getRequestCount() {
        return requestCount.get();
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 10);

        CrptApi.Document document = new CrptApi.Document();
        fillDocumentData(document);

        String signature = "your_signature";

        String response = crptApi.createDocument(document, signature);
        System.out.println(response);

        crptApi.shutdown();
    }

    private static void fillDocumentData(Document document) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Document.Description description = new Document.Description();
        description.setParticipantInn("123456789");
        document.setDescription(description);

        document.setDocId("doc123");
        document.setDocStatus("NEW");
        document.setDocType("LP_INTRODUCE_GOODS");
        document.setImportRequest(true);
        document.setOwnerInn("123456789");
        document.setParticipantInn("987654321");
        document.setProducerInn("456123789");

        // Устанавливаем дату как строку через сеттеры
        document.setProductionDate(LocalDate.of(2023, 1, 23).format(formatter));
        document.setProductionType("type1");
        document.setRegDate(LocalDate.of(2023, 1, 25).format(formatter));
        document.setRegNumber("reg123");

        Document.Product product = new Document.Product();
        product.setCertificateDocument("cert123");

        // Преобразуем даты в строки через сеттеры
        product.setCertificateDocumentDate(LocalDate.of(2023, 1, 20).format(formatter));
        product.setCertificateDocumentNumber("cert12345");
        product.setOwnerInn("123456789");
        product.setProducerInn("456123789");
        product.setProductionDate(LocalDate.of(2023, 1, 23).format(formatter));
        product.setTnvedCode("1001");
        product.setUitCode("uit12345");
        product.setUituCode("uitu67890");

        document.setProducts(List.of(product));
    }
}
