package wikiedits;


import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer011;
import org.junit.ClassRule;
import org.junit.Test;
//import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;

import static wikiedits.Producer.buildFromId;

//@RunWith(SpringRunner.class)
//@SpringBootTest
//@EmbeddedKafka(topics = "someTopic")
//@EmbeddedKafka
public class ProducerTest {
    private static final String topic = "pattern1";
    @ClassRule
    public static EmbeddedKafkaRule embeddedKafkaRule = new EmbeddedKafkaRule(
            3,
            false,
            5,
            "cat", "hat", topic);

    @Test
    public void test() throws Exception {
        StreamExecutionEnvironment envStream = StreamExecutionEnvironment.getExecutionEnvironment();
        ClientIdentifier id = ClientIdentifier.newBuilder()
                .setHostName("aboydfd.com")
                .setIpAddress("45.xxx.xx.11")
                .build();
        ClientIdentifier id2 = ClientIdentifier.newBuilder()
                .setHostName("aboydfd.com")
                .setIpAddress("45.xxx.xx.1")
                .build();
        AvroHttpRequest element = buildFromId(id, Active.YES, 10000100);
        AvroHttpRequest element2 = buildFromId(id, Active.NO, 10000000);
        AvroHttpRequest element3 = buildFromId(id2, Active.NO, 10000000);
        AvroHttpRequest element4 = buildFromId(id2, Active.NO, 100010000);
        String brokerList = embeddedKafkaRule.getEmbeddedKafka().getBrokerAddresses()[0].toString();
        System.out.println(brokerList);
        produce(envStream, element, element2, element3, element4, brokerList, topic);
        envStream.execute();

        new WikipediaAnalysis().start(brokerList, topic);
//        RequestReplyFuture<String, String, String> replyFuture = template.sendAndReceive(record);
//        SendResult<String, String> sendResult = replyFuture.getSendFuture().get(10, TimeUnit.SECONDS);
//        System.out.println("Sent ok: " + sendResult.getRecordMetadata());
//        ConsumerRecord<String, String> consumerRecord = replyFuture.get(10, TimeUnit.SECONDS);
//        System.out.println("Return value: " + consumerRecord.value());

//        System.out.println(Arrays.toString(embeddedKafkaRule.getEmbeddedKafka().getBrokerAddresses()));

//        System.out.println(embeddedKafkaRule.getEmbeddedKafka()
//                .getBrokerAddress(0));
//        System.out.println(embeddedKafkaRule.getEmbeddedKafka().kafkaPorts(9092, 9093, 9094)
//                        .getKafkaServer(0)
//                .boundPort(new ListenerName("11"))
//                );
//        System.out.println(embeddedKafkaRule.getEmbeddedKafka()
//                .getBrokerAddress(2));
//                .addTopics(new NewTopic("thing1", 10, (short) 1), new NewTopic("thing2", 15, (short) 1));
    }
    private static void produce(StreamExecutionEnvironment envStream, AvroHttpRequest element, AvroHttpRequest element2, AvroHttpRequest element3, AvroHttpRequest element4, String brokerList, String topicId) {
        DataStreamSource<AvroHttpRequest> avroHttpRequestDataStreamSource = envStream.fromElements(element, element2, element3, element4);
        avroHttpRequestDataStreamSource
//                .map(avroSerealizer::serealizeAvroHttpRequestBinary)
                .addSink(new FlinkKafkaProducer011<>(brokerList, topicId, new AvroSerealizer()));
    }
}
