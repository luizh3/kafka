package io.kafka.project.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemo {

    public static final Logger log = LoggerFactory.getLogger(ProducerDemo.class.getSimpleName() );

    public static void main( String[] args ){
        log.info("Test");


        Properties properties = new Properties();

        // Connect property
        properties.setProperty("bootstrap.servers", "172.28.41.24:9092");

        // Set producer propertys
        properties.setProperty("key.serializer", StringSerializer.class.getName() );
        properties.setProperty("value.serializer", StringSerializer.class.getName() );

        // Create the Producer
        KafkaProducer<String,String> producer = new KafkaProducer<>( properties );

        // Create a Producer Record
        ProducerRecord<String,String> producerRecord = new ProducerRecord<>("demo_java", "Test Message");

        // Send data
        producer.send( producerRecord );

        // Tell the producer to send all data anc block until done
        producer.flush();

        // flush and close the producer
        producer.close();
    }
}
