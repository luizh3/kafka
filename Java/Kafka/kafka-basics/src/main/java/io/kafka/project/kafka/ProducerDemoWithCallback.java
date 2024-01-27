package io.kafka.project.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoWithCallback {

    public static final Logger log = LoggerFactory.getLogger(ProducerDemoWithCallback.class.getSimpleName() );

    public static void main( String[] args ){
        log.info("Test");


        Properties properties = new Properties();

        // Connect property
        properties.setProperty("bootstrap.servers", "172.28.41.24:9092");

        // Set producer propertys
        properties.setProperty("key.serializer", StringSerializer.class.getName() );
        properties.setProperty("value.serializer", StringSerializer.class.getName() );

        properties.setProperty("batch.size", "400" );

//      properties.setProperty("partitioner.class", RoundRobinPartitioner.class.getName() );

        // Create the Producer
        KafkaProducer<String,String> producer = new KafkaProducer<>( properties );

        for( int j = 0; j < 10; j++ ) {

            for ( int i = 0; i < 30; i++ ) {

                // Create a Producer Record
                ProducerRecord<String,String> producerRecord = new ProducerRecord<>("demo_java", "Test Message" + i );

                // Send data
                producer.send(producerRecord, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata recordMetadata, Exception exception) {
                        if( exception == null ){
                            log.info("Received new metadata \n" +
                                    "Topic:" + recordMetadata.topic() + "\n" +
                                    "Partition:" + recordMetadata.partition() + "\n" +
                                    "Offset:" + recordMetadata.offset() + "\n" +
                                    "Timestamp:" + recordMetadata.timestamp() + "\n"

                            );
                        } else {
                            log.error("Error while producing:", exception );
                        }
                    }
                });

            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

        // Tell the producer to send all data anc block until done
        producer.flush();

        // flush and close the producer
        producer.close();
    }
}
