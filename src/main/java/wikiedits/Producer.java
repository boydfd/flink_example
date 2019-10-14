package wikiedits;

import akka.stream.impl.io.FileSink;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.avro.AvroRowSerializationSchema;
import org.apache.flink.formats.avro.typeutils.AvroSerializer;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer011;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Producer {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment envStream = StreamExecutionEnvironment.getExecutionEnvironment();
        envStream.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        getSchema();

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
//        .serealizeAvroHttpRequestBinary(element)

//        produce(envStream, element, element2, element3, element4);
        test(envStream, Arrays.asList(
                element,
                element2,
                buildFromId(id, Active.YES, 10001100),
//                element3,
                element4,
                buildFromId(id, Active.YES, 10000300),
                buildFromId(id, Active.YES, 10000900),
                buildFromId(id, Active.YES, 9999999),
                buildFromId(id, Active.YES, 29999999),
                buildFromId(id, Active.YES, 39999999),
                buildFromId(id, Active.YES, 49999999),
                buildFromId(id, Active.YES, 59999999),
                buildFromId(id, Active.YES, 69999999),
                buildFromId(id, Active.YES, 79999999),
                buildFromId(id, Active.YES, 89999999),
                buildFromId(id, Active.YES, 99999999),
//                buildFromId(id, Active.YES, 109999999),
//                buildFromId(id, Active.YES, 119999999),
//                buildFromId(id, Active.YES, 129999999),
//                buildFromId(id, Active.YES, 229999999),
//                buildFromId(id, Active.YES, 329999999),
//                buildFromId(id, Active.YES, 429999999),
                buildFromId(id, Active.YES, 529999999)
        ));

        envStream.execute();
    }

    public static AvroHttpRequest buildFromId(ClientIdentifier id, Active yes, int i) {
        return AvroHttpRequest.newBuilder()
                .setEmployeeNames(
                        Collections.singletonList("boydfd")
                )
                .setActive(yes)
                .setClientIdentifier(id)
                .setRequestTime(i)
                .build();
    }

    static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private static void test(StreamExecutionEnvironment envStream, List<AvroHttpRequest> elements) {
        deleteDirectory(new File("./test.txt"));
        DataStreamSource<AvroHttpRequest> avroHttpRequestDataStreamSource = envStream.fromElements(elements.toArray(new AvroHttpRequest[0]));
        final StreamingFileSink<String> sink = StreamingFileSink
                .<String>forRowFormat(new Path("./test.txt"), new SimpleStringEncoder<>("UTF-8"))
                .build();
        avroHttpRequestDataStreamSource
                .assignTimestampsAndWatermarks(new WaterMark(Time.seconds(10)))
                .keyBy(AvroHttpRequest::getClientIdentifier)
//                .window(TumblingEventTimeWindows.of(Time.seconds(10)))
//                .window(SlidingEventTimeWindows.of(Time.seconds(10), Time.seconds(5), Time.seconds(1)))
//                .window(EventTimeSessionWindows.withGap(Time.seconds(10)))
//                .window(CustomerWindows.withWindowLength(1000))
//                .reduce((l, r) -> {
//                    long t = l.getRequestTime() + r.getRequestTime();
//                    r.setRequestTime(t);
//                    return r;
//                })
                .process(new TestProcess())
                .map(AvroHttpRequest::toString)
                .setParallelism(1)
                .addSink(sink)
                .setParallelism(1)
        ;
    }

    private static void produce(StreamExecutionEnvironment envStream, AvroHttpRequest element, AvroHttpRequest element2, AvroHttpRequest element3, AvroHttpRequest element4) {
        DataStreamSource<AvroHttpRequest> avroHttpRequestDataStreamSource = envStream.fromElements(element, element2, element3, element4);
        AvroSerealizer avroSerealizer = new AvroSerealizer();
        avroHttpRequestDataStreamSource
//                .map(avroSerealizer::serealizeAvroHttpRequestBinary)
                .addSink(new FlinkKafkaProducer011<>("localhost:9094", "pattern", new AvroSerealizer()));
    }

    private static void getSchema() {
        Schema clientIdentifier = SchemaBuilder.record("ClientIdentifier")
                .namespace("com.baeldung.avro")
                .fields().requiredString("hostName").requiredString("ipAddress")
                .endRecord();
        Schema avroHttpRequest = SchemaBuilder.record("AvroHttpRequest")
                .namespace("com.baeldung.avro")
                .fields().requiredLong("requestTime")
                .name("clientIdentifier")
                .type(clientIdentifier)
                .noDefault()
                .name("employeeNames")
                .type()
                .array()
                .items()
                .stringType()
                .arrayDefault(null)
                .name("active")
                .type()
                .enumeration("Active")
                .symbols("YES", "NO")
                .noDefault()
                .endRecord();
    }

    public byte[] serealizeAvroHttpRequestJSON(AvroHttpRequest request) {

        DatumWriter<AvroHttpRequest> writer = new SpecificDatumWriter<>(
                AvroHttpRequest.class);
        byte[] data = new byte[0];
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JsonEncoder jsonEncoder = null;
        try {
            jsonEncoder = EncoderFactory.get().jsonEncoder(
                    AvroHttpRequest.getClassSchema(), stream);
            writer.write(request, jsonEncoder);
            jsonEncoder.flush();
            data = stream.toByteArray();
        } catch (IOException e) {
            System.out.println("Serialization error:" + e.getMessage());
        }
        return data;
    }
}
