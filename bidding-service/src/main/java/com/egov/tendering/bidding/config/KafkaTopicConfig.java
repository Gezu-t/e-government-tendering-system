package com.egov.tendering.bidding.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.bid-events}")
    private String bidEventsTopic;

    @Value("${app.kafka.topics.tender-events:tender-events}")
    private String tenderEventsTopic;

    @Value("${app.kafka.topics.evaluation-events:evaluation-events}")
    private String evaluationEventsTopic;

    @Value("${app.kafka.topics.tender-evaluation-completed:tender-evaluation-completed}")
    private String tenderEvaluationCompletedTopic;

    @Value("${app.kafka.topics.contract-events:contract-events}")
    private String contractEventsTopic;

    /**
     * Creates the bid events topic with partitions for parallelism
     * and replication factor for reliability
     */
    @Bean
    public NewTopic bidEventsTopic() {
        return TopicBuilder.name(bidEventsTopic)
                .partitions(3)
                .replicas(1)  // Set to higher value in production
                .build();
    }

    /**
     * Topic for tender events that bidding service needs to listen to
     */
    @Bean
    public NewTopic tenderEventsTopic() {
        return TopicBuilder.name(tenderEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic for evaluation events that bidding service needs to listen to
     */
    @Bean
    public NewTopic evaluationEventsTopic() {
        return TopicBuilder.name(evaluationEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic tenderEvaluationCompletedTopic() {
        return TopicBuilder.name(tenderEvaluationCompletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic for contract events that bidding service needs to listen to
     */
    @Bean
    public NewTopic contractEventsTopic() {
        return TopicBuilder.name(contractEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
