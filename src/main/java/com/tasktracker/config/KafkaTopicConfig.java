package com.tasktracker.config;

import com.tasktracker.kafka.TaskEventProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic taskCreatedTopic() {
        return TopicBuilder.name(TaskEventProducer.TASK_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic taskAssigneeChangedTopic() {
        return TopicBuilder.name(TaskEventProducer.TASK_ASSIGNEE_CHANGED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
