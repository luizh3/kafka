package io.kafka.project.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemo {

    public static final Logger log = LoggerFactory.getLogger(ConsumerDemo.class.getSimpleName() );

    public static void main( String[] args ){
        log.info("Test");

        String groupId = "my-java-application";
        String topic = "demo_java";

        Properties properties = new Properties();

        // Connect property
        properties.setProperty("bootstrap.servers", "172.28.41.24:9092");

        // Create consumer config
        properties.setProperty("key.deserializer", StringDeserializer.class.getName() );
        properties.setProperty("value.deserializer", StringDeserializer.class.getName() );

        properties.setProperty("group.id", groupId );

        // earliest/latest
        properties.setProperty("auto.offset.reset", "earliest" );

        // Create consumer

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // Subscribe a topic
        consumer.subscribe(Arrays.asList(topic));

        // Poll for data
        while ( true ){

            log.info("Polling");

            ConsumerRecords<String,String> records = consumer.poll(Duration.ofMillis(1000));

            for ( ConsumerRecord<String, String> record  : records ){
                log.info("Key: " + record.key() + ", Value: " + record.value());
                log.info("Partition: " + record.partition() + ", Offset: " + record.offset());
            }

        }

    }
}
