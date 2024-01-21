## WHY APACHE KAFKA

- Create by Linkedin, now open-source Project mainly maintained by Confluent, IBM, Cloudera

- Distributed, resilient architeture, fault tolerant

- Horizontal scalability
  - Can scale to 100s of brokers
  - Can scale to millions of messages per second
- High performance ( latency of less than 10 ms ) - real time

## Use cases

- Messaging System
- Activity Tracking
- Gather metrics from many different locations
- Application Logs gathering
- Stream processing ( with the kafka Streams API for example )
- De-coupling of system dependencies
- Integration with Spark, Flink, Storm, Hadoop, and many other Big Data technologies
- Micro-services pub/sub

## Kafka Topics

- Topics: A particular stream of data inside of yout cluster kafka
- like a table in database ( without all the constraints )
- You can have as many topics as you want
- A topic is identified by its name
- Any kind of message format
- You cannot query topics, instead use kafka producers to send data and kafka consumers to read the data
- You can have many partitions per topic as you want

## Partitions and offsets

- Topics are split in partitions ( Example: 100 partitions )
  - Messages within each partition are ordered
  - Each message within a partition gets an incremental id, called offset
- Kafka topics are immutable: once data is written to a partition, it cannot be changed
- Data is kept only for a limited time ( default is one week - configurable )

## Producers

- Producers write data to topics ( which are made of partitions )
- Producers know to which partition to write to ( nd which kafka broker has it )
- in case of kafka broker failures, Producers will automatically recover

- Message keys
  - Producers can choose to send a key with the message ( string, number, binary, etc... )
  - if key = null, data is sent round robin( partition 0, then 1, then 2 )
  - A key are typically sent if you need message ordering for a specific field ( ex: truck_id )

## Kafka Messages anatomy

| [ Key - Binary ( Can be null ) ] - [ Value - Binary ( Can be null ) ] |
| --------------------------------------------------------------------- |
| Compression Type ( None, gzip, snappy, lz4, zstd )                    |
| Headers ( Optional )                                                  |
| [ Key ] - [ Value ]                                                   |
| Partiton + Offset                                                     |
| Timestamp ( System or user set )                                      |

## Kafka Message Serializer

- Kafka only accepts bytes as an input from producers and sends bytes out as an output to consumers
- Message Serialization means transforming objects / data into bytes
- They are used on the value and the key
- Common Serializers
  - String ( Incl. JSON )
  - int, Float
  - Avro
  - Protobuf

```
Key Object ( 123 ) ----Int----> KeySerializer=IntegerSerializaer ----Bytes----> Key - Binary ( 01110011 )
```

```
Value Object ( "Hello world" ) ----String----> ValueSerializer=StringSerializaer ----Bytes----> Value - Binary ( 01110011 )
```

## Message Key Hashing

- A kafka partitioner is a code logic that takes a record determines to which partition to send it into.

- Key hashing is the process of determining the mapping of a key to a partition

- In the default kafka partitioner, the keys are hashed using the murmur2 algorithm with the formula bwlow for the curious:

targetPartition = Math.abs( Utils.murmur2(KeyBytes)) % ( numPartitions - 1 )

## Consumers

- Consumers read data from a topic ( identified by name ) - pull model

- Consumers automatically know which broker to read from

- in case of broker failures, consumers know how to recover

- Data is read in order from low to high offset within each partitions

## Consumer Deserializer

- Deserialize indicates how to transform bytes into objects / data

- They are used on the value and the key of the message

- Common Deserializers

  - String ( incl. JSON )
  - int, Float
  - Avro
  - Protobuf

- The serialization / deserialization type must not change during a topic lifecycle ( create a new topic instead )

## Consumer Groups

- All the consumers in an application read data as a consumer groups

- Each consumer within a group reads from exclusive partitions

- If you have more consumers than partitions, some consumers will be inactive

- In Apache kafka it is acceptable to have multiple consumer groups on the same topic

- To create distinct consumer groups, use the consumer property group.id

## Consumers offset

- Kafka stores the offsets at which a consumer group has been reading

- The offsets commited are in kafka topic named \_\_consumer_offsets

- When a consumer in a group has processed data received from kafka, it should be periodically committing the offsets ( the kafka broker will write to \_\_consumer_offsets, not the group itseld )

- If a consumer dies, it will be able to read back from where it left off thanks to the commited consumer offsets

## Delivery semantics for consumers

- By default, java Consumers will automatically commit offsets ( at least once )

- There are 3 delivery semantics if you choose to commit manually

- At least once ( Usually preferred )

  - Offsets are commited after the message is processed
  - If the processing goes wrong, whe message will be read again
  - This can result in duplicate processing of messages. Make sure your processing is idempotent ( processing again the messages won't impact your system )

- At most once
  - Offsets are committed as soon as messages are received
  - If the processing goes wrong, some messages will be lost (
    They won't be read again
    )
- Exactly once
  - For kafka => Kafka workflows: use the Transactional API ( Easy with kafka Streams API )
  - For kafka -> External System workflows: use an idempotent consumer

## Kafka Brokers

- A Kafka clister is composed of multiply brokers ( servers )
- Each broker is identified with its ID ( Integer )
- Each broker contains certain topic partitions
- After connecting to any broker ( called a bootstrap broker ), you will be connected to the entire cluster ( Kafka clients have smart mechanics for that )
- A good number to get starter is 3 brokers, but some big clusters have over 100 brokers
- Every kafka broker is also called a "bootstrap server"

## Topic replication factor

- Topics should have a replication factor > 1 ( Usually between 2 and 3 )
- This away if a broker is down, another broker can serve the data
- Example: Topic-A with 2 partitions and replication factor 2

Example:

- we lose broker 102
- Result: Broker 101 and 103 can still server the data

```
Broker 101
-------------
|Partition 0|
|Topic A    |
-------------
```

```
Broker 102
-------------
|Partition 1|
|Topic A    |
-------------
-------------
|Partition 0|
|Topic A    | Replication from broker 101
-------------
```

```
Broker 103
-------------
|Partition 1|
|Topic A    | Replication from broker 102
-------------
```

## Concept of Leader for a Partion

- At any time only ONE broker can be a leader for a giver partition
- Producers can only send data to the broker that is leader of a partition
- The other brokers will replicate the data
- Therefore, each partition has one leader and multiple ISR ( in-sync replica )
- Kafka producers can only write to the leader broker for a partition
- Kafka consumers by default will be read from the leader broker for a partition
- Since kafka 2.4 it is possible to configure consumers to read from the closest replica
- This may help improve latency, and also decrease network costs if using the cloud

```
Broker 101
-------------
|Partition 0| Leader ⭐
|Topic A    |
-------------
```

```
Broker 102
-------------
|Partition 1| Leader ⭐
|Topic A    |
-------------
-------------
|Partition 0|
|Topic A    | Replication from broker 101
-------------
```

```
Broker 103
-------------
|Partition 1|
|Topic A    | Replication from broker 102
-------------
```

## Producer Acknowledgment

- Producers choose to receive acknowledgment of data writes
  - acks=0: Producers won't wait for acknowledgment ( Possible data loss )
  - acks=1: Producer will wait for leader acknowledgment ( limited data loss )
  - acks=all: Leader + replicas acknowledgment ( no data loss )

## Kafka Topic Durability

- For a topic replication factor of 3, topic data durability can withstand 2 brokers loss.

- As a rule, for a replication factor of N, you can permanently lose up to N-1 brokers and still recover your data.

## Zookeeper

- Manages brokers ( Keeps a list of them )
- Helps in performing leader election for partitions
- Sends notifications to kafka in case of changes (
  new topic, broker dies, broker comes up, delete topics, etc...
  )
- Kafka 2.x can't work without zookeeper
- Kafka 3.x can work without zookeeper ( KIP-500 ), using kafka raft instead
- Kafka 4.x will not have zookeeper
- By design operates with an odd number of servers ( 1,3 3, 5, 7 )
- Has a leader ( writes ) the rest of the servers are fallowers ( reads )
- Does NOT store consumer offsets with kafka > v0.10

## Should you use Zookeeper ?

- With kafka Brokers ?
  - Yes, until kafka 4.0 is out while waiting for kafka without Zookeper to be production-read
- With Kafka Clients ?
  - Over time, the kafka clients and CLI have been migrated to leverage the brokers as a connection endpoint instead of zookeeper
  - Since kafka 0.10, consumers store offset in kafka and zookeeper and must not connect to zookepeper as it is deprecated
  - Since kafka 2.0 the kafka-topics.sh CLI command references kafka brokers and not zookeeper for topic management ( Creation, deletion, etc... ) and the zoopeeper CLI argument is deprecated.
  - All the APIs and commands that were previously leveraging zookeper are migrated to use kafka instead, so that when clusters are migrated to be without zookeeper , the change is invisible to clients.
  - Zookeeper is algo less secure than kafka, and therefore zookeper ports should only be opened to allow traffic from kafka brokers, and not kafka clients.

## Kafka KRaft

- In 2020, the Apache kafka project started to work to remove the Zookeeper dependency from it ( KIP-500 )
- Zookeeper shows scaling issues when kafka clusters have > 100,000 partitions
- By removing Zookeeper, Apache Kafka can
  - Scale to millions of partitions, and becomes easier to maintain and set-up
  - Improve stability, makes it easier to monitor, support and administer
  - Single security model for the whole system
  - Single process to start with Kafka
  - Faster controller shutdown and recovery time
- Kafka 3.x now implements the Raft protocol ( KRaft ) in order to replace Zookeeper
  - Production readt since kafka 3.3.1 ( KIP-883 )
  - Kafka 4.0 will be released only with KRaft ( No Zookeper )
