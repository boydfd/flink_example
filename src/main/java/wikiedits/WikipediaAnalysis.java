package wikiedits;

import org.apache.flink.formats.avro.AvroDeserializationSchema;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.IterativeStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011;

import java.util.Properties;

public class WikipediaAnalysis {
    void start(String server, String topic) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.getConfig().setAutoWatermarkInterval(1);
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", server);
// only required for Kafka 0.8
//        properties.setProperty("zookeeper.connect", "localhost:2181");
        properties.setProperty("group.id", "test");
        AvroDeserializationSchema<AvroHttpRequest> avroHttpRequestAvroSchema = AvroDeserializationSchema.forSpecific(AvroHttpRequest.class);
        FlinkKafkaConsumer011<AvroHttpRequest> patternConsumer = new FlinkKafkaConsumer011<>(topic, avroHttpRequestAvroSchema, properties);
//        patternConsumer.setStartFromGroupOffsets();
        patternConsumer.setStartFromEarliest();
        DataStream<AvroHttpRequest> stream = env.addSource(patternConsumer);
//        KeyedStream<AvroHttpRequest, ClientIdentifier> keyedAvroHttp = stream

        SingleOutputStreamOperator<AvroHttpRequest> window = stream
                .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<AvroHttpRequest>(Time.seconds(10)) {
                    @Override
                    public long extractTimestamp(AvroHttpRequest event) {
                        return event.getRequestTime();
                    }
                })
                .keyBy(AvroHttpRequest::getClientIdentifier)
                .process(new TestProcess())
//                .timeWindow(Time.seconds(5))
//                .window(CustomerWindows.withWindowLength(1000))
//                .window(EventTimeSessionWindows.withGap(Time.seconds(10)))
//                .window(SlidingEventTimeWindows.of(Time.seconds(10), Time.seconds(5), Time.seconds(1)))
                ;
//        window
//                .sum("requestTime")
//                .process((request, a, d) -> {

//                })
//                .print();
//        SingleOutputStreamOperator<AvroHttpRequest> result = window
//                .window(TumblingEventTimeWindows.of(Time.seconds(10)))
//                .reduce((l, r) -> {
//                            long t = l.getRequestTime() + l.getRequestTime();
//                            r.setRequestTime(t);
//                            return r;
//                        }
//                        (key, context, iterator, out) -> {
//                            AvroHttpRequest next = iterator.iterator().next();
//                            out.collect(new Tuple3<ClientIdentifier, Long, AvroHttpRequest>(key, context.getEnd(), next));
//
//                        }
//                ).iterate().closeWith();
//        result
//                .print();
//                .map((MapFunction<Tuple2<String, Long>, String>) Tuple2::toString)
//                .addSink(new FlinkKafkaProducer011<>("0.0.0.0:9094", "wiki-result", new SimpleStringSchema()));
        env.execute();

    }
    public static void main(String[] args) throws Exception {
        new WikipediaAnalysis().start("localhost:9094", "pattern");
    }
}
