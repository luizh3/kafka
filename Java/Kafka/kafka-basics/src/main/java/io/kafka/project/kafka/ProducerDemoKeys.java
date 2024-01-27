package io.kafka.project.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoKeys {

    public static final Logger log = LoggerFactory.getLogger(ProducerDemoKeys.class.getSimpleName() );

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

        for( int j = 0; j < 10; j++ ) {
            for ( int i = 0; i < 30; i++ ) {

                String topic = "demo_java";
                String key = "id_" + i;
                String value = "Hello World: " + i;

                // Create a Producer Record
                ProducerRecord<String,String> producerRecord = new ProducerRecord<>(topic, key, value );

                // Send data
                producer.send(producerRecord, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata recordMetadata, Exception exception) {
                        if( exception == null ){
                            log.info("Key:" + key + "\n" +
                                    "Partition:" + recordMetadata.partition() + "\n"
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

            }
        }

        // Tell the producer to send all data anc block until done
        producer.flush();

        // flush and close the producer
        producer.close();
    }
}
