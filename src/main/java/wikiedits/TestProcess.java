package wikiedits;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.logging.Logger;

public class TestProcess extends KeyedProcessFunction<ClientIdentifier, AvroHttpRequest, AvroHttpRequest> {
    Log logger = LogFactory.getLog(TestProcess.class);
    @Override
    public void processElement(AvroHttpRequest avroHttpRequest, Context context, Collector<AvroHttpRequest> collector) throws Exception {
        logger.info(context.timerService().currentWatermark());

    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
    }
}
