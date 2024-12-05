package com.ke.bella.workflow.service;

import com.ke.bella.workflow.utils.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class CustomKafkaProducer implements AutoCloseable {

    KafkaProducer<String, String> producer;

    public void send(String topic, String key, String message) throws Exception {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, message);
        AtomicReference<Exception> e = new AtomicReference<>();
        producer.send(record, (RecordMetadata metadata, Exception exception) -> {
            String msg = String.format("Message sent to topic %s partition %d with offset %d", metadata.topic(), metadata.partition(), metadata.offset());
            if(exception != null) {
                e.set(exception);
                msg += " exception: " + exception.getMessage();
                LOGGER.warn(msg, exception);
            } else {
                LOGGER.info(msg);
            }
        }).get();
        if(e.get() != null) {
            throw e.get();
        }
    }

    public void send(String topic, String key, Object message) throws Exception {
        String value = JsonUtils.toJson(message);
        send(topic, key, value);
    }

    public static CustomKafkaProducer using(String servers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.RETRIES_CONFIG, 3);//最大重试次数
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000);//发送缓冲区满/元数据不可用时，最大阻塞时间为10秒
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        return CustomKafkaProducer.builder().producer(producer).build();
    }

    @Override
    public void close() throws Exception {
        producer.close();
    }
}
