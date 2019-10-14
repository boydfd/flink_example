package wikiedits;

import org.apache.flink.streaming.api.functions.AssignerWithPunctuatedWatermarks;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.time.Time;

import javax.annotation.Nullable;

public class WaterMark implements AssignerWithPunctuatedWatermarks<AvroHttpRequest> {
    private static final long serialVersionUID = 1L;
    private final long maxOutOfOrderness;
    private long currentMaxTimestamp;
    private long lastEmittedWatermark = -9223372036854775808L;

    public WaterMark(Time maxOutOfOrderness) {
        if (maxOutOfOrderness.toMilliseconds() < 0L) {
            throw new RuntimeException("Tried to set the maximum allowed lateness to " + maxOutOfOrderness + ". This parameter cannot be negative.");
        } else {
            this.maxOutOfOrderness = maxOutOfOrderness.toMilliseconds();
            this.currentMaxTimestamp = -9223372036854775808L + this.maxOutOfOrderness;
        }
    }
    public long getMaxOutOfOrdernessInMillis() {
        return this.maxOutOfOrderness;
    }


    @Nullable
    @Override
    public Watermark checkAndGetNextWatermark(AvroHttpRequest avroHttpRequest, long l) {
        this.extractTimestamp(avroHttpRequest);
        long potentialWM = this.currentMaxTimestamp - this.maxOutOfOrderness;
        if (potentialWM >= this.lastEmittedWatermark) {
            this.lastEmittedWatermark = potentialWM;
        }

        return new Watermark(this.lastEmittedWatermark);
    }

    public  long extractTimestamp(AvroHttpRequest avroHttpRequest) {
        return avroHttpRequest.getRequestTime();
    }

    @Override
    public long extractTimestamp(AvroHttpRequest avroHttpRequest, long l) {
        long timestamp = this.extractTimestamp(avroHttpRequest);
        if (timestamp > this.currentMaxTimestamp) {
            this.currentMaxTimestamp = timestamp;
        }

        return timestamp;
    }
}
