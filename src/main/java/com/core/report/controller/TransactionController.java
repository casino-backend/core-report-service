package com.core.report.controller;

import com.core.report.entities.Transaction;
import com.core.report.service.TransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {
    private final TransactionService transactionService;
    private final KafkaTemplate<String,String> kafkaTemplate;

    @Value("${spring.kafka.consumer.topics}")
    private String topics;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    public TransactionController(TransactionService transactionService, KafkaTemplate<String, String> kafkaTemplate) {
        this.transactionService = transactionService;
        this.kafkaTemplate = kafkaTemplate;
    }

  /*  public static TransactionController create(TransactionInputPort transactionInter, String bootstrapServers, String schemaRegistryUrl) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "transaction-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        KafkaConsumer<String, Transaction> kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(Collections.singletonList("transaction"));

        return new TransactionController(transactionInter, transactionService, kafkaConsumer);
    }*/

      public void listenAndProcessGameTransaction(String message) {
        try {
            // You can parse the JSON data into your entity class
            ObjectMapper objectMapper = new ObjectMapper();
            Transaction tx = objectMapper.readValue(message, Transaction.class);

            // Call the method to process the transaction
            transactionService.processTransaction(tx);
        } catch (JsonProcessingException e) {
            // Handle the error, e.g., log or throw an exception
            e.printStackTrace();
        }
    }

    @PostMapping("/kafka")
   public String publishTransactionToKafka() throws JsonProcessingException {
        Transaction transaction = new Transaction();
        transaction.setActionType("DEPOSIT");
        transaction.setAction("User deposit");
        transaction.setBeforeBalance(1000.00);
        transaction.setAmount(200.00);
        transaction.setAfterBalance(1200.00);
        transaction.setRoundId("R12345");
        transaction.setGameId("G12345");
        transaction.setGameName("Starburst");
        transaction.setGameCategory("Slots");
        transaction.setFeatureBuy(false);
        transaction.setEndRound(true);
        transaction.setUsername("user123");
        transaction.setUpline("uplineUser");
        transaction.setRefSale("refSale123");
        transaction.setDescription("User made a deposit");
        transaction.setProductId("P12345");
        transaction.setProductName("ProductName");
        transaction.setProvider("NetEnt");
        transaction.setPercentage(10.0);
        transaction.setSyncDate("2023-11-05T20:01:39Z");
        transaction.setCreatedAt("2023-11-05T20:01:39Z");
        transaction.setCreatedAtIso("2023-11-05T20:01:39Z");
        transaction.setCreated("2023-11-05T20:01:39");
        transaction.setIp("192.168.1.1");
        transaction.setFRunning(false);
        transaction.setFRunningDate("2023-11-05T20:01:39Z");

// Convert Transaction object to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonTransaction = objectMapper.writeValueAsString(transaction);
        kafkaTemplate.send(topics,jsonTransaction);
        return "message published successfully";
    }

}

