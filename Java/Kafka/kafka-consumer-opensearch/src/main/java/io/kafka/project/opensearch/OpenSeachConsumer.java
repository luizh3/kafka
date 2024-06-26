package io.kafka.project.opensearch;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class OpenSeachConsumer {

    public static RestHighLevelClient createOpenSearchClient() {
        String connString = "http://localhost:9200";

        // we build a URI from the connection string
        RestHighLevelClient restHighLevelClient;
        URI connUri = URI.create(connString);
        // extract login information if it exists
        String userInfo = connUri.getUserInfo();

        if (userInfo == null) {
            // REST client without security
            restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), "http")));

        } else {
            // REST client with security
            String[] auth = userInfo.split(":");

            CredentialsProvider cp = new BasicCredentialsProvider();
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));

            restHighLevelClient = new RestHighLevelClient(
                    RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), connUri.getScheme()))
                            .setHttpClientConfigCallback(
                                    httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)
                                            .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())));


        }

        return restHighLevelClient;
    }

    private static String extractId( String json ) {

        return JsonParser
                .parseString( json )
                .getAsJsonObject()
                .get("meta")
                .getAsJsonObject()
                .get("id")
                .getAsString();

    }

    private static KafkaConsumer<String, String> createKafkaConsumer() {

        String groupId = "consumer-openseach-demo";
        String boostrapServer = "127.0.0.1:9092";

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, boostrapServer);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName() );
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName() );
        properties.setProperty( ConsumerConfig.GROUP_ID_CONFIG, groupId );
        properties.setProperty( ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest" );
        properties.setProperty( ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false" );

        return new KafkaConsumer<>(properties);

    }
    public static void main(String[] args) throws IOException {

        Logger log = LoggerFactory.getLogger( OpenSeachConsumer.class.getSimpleName() );

        RestHighLevelClient openSearchClient = createOpenSearchClient();

        KafkaConsumer<String, String> consumer = createKafkaConsumer();

        final Thread mainThread = Thread.currentThread();

        // Adding the shutdown hook

        Runtime.getRuntime().addShutdownHook( new Thread(){
            public void run(){
                log.info("Detected shutdown, let's exit by calling consumer.wakeup()...");
                consumer.wakeup();

                // Join the main thread to allow the execution of the code in the main thread
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        try( openSearchClient; consumer ){

            boolean hasIndex = openSearchClient.indices().exists( new GetIndexRequest("wikimedia" ), RequestOptions.DEFAULT );

            if( !hasIndex ){
                CreateIndexRequest createIndexRequest = new CreateIndexRequest("wikimedia");
                openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                log.info("The wikimedia Index has been created");
            } else {
                log.info("Wikimedia index already exist");
            }

            consumer.subscribe(Collections.singleton("wikimedia.recentchange"));

            while( true ){

                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3000));

                int recordCount = records.count();

                log.info("Received: " + recordCount + " record(s)" );

                BulkRequest bulkRequest = new BulkRequest();

                for ( ConsumerRecord<String, String> record : records ) {
//                    Define on ID using Kafka Record coordiantes, strategy 1
//                    String id = record.topic() + "_" + record.partition() + "_" + record.offset();

                    try {

                        // Strategy 2
                        // We extract the ID from json value
                        String id = extractId( record.value() );

                        IndexRequest indexRequest = new IndexRequest("wikimedia").source( record.value(), XContentType.JSON ).id( id );

//                        IndexResponse response = openSearchClient.index( indexRequest, RequestOptions.DEFAULT );

                        bulkRequest.add( indexRequest );

                    } catch ( Exception e ) {

                    }
                }

                if( bulkRequest.numberOfActions() > 0 ) {
                    BulkResponse bulkResponse = openSearchClient.bulk( bulkRequest, RequestOptions.DEFAULT );
                    log.info("Inserted " + bulkResponse.getItems().length + " record(s).");

                    try {
                        Thread.sleep( 1000 );
                    } catch ( InterruptedException e ) {
                        e.printStackTrace();
                    }

                    // Commit offsets after the batch is consumed
                    consumer.commitSync();
                    log.info("Offsets have been committed!");
                }

            }

        } catch ( WakeupException e )  {
            log.info("Consumer is starting to shut down");
        } catch ( Exception e ) {
            log.info("Unexpected exception in the consumer", e );
        } finally {
            consumer.close();
            openSearchClient.close();
            log.info("The consumer is now gracefully shut down");
        }

    }

}
