package wikiedits;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.DynamicEventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.MergingWindowAssigner;
import org.apache.flink.streaming.api.windowing.assigners.SessionWindowTimeGapExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.triggers.EventTimeTrigger;
import org.apache.flink.streaming.api.windowing.triggers.Trigger;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

import java.util.*;

public class CustomerWindows extends MergingWindowAssigner<Object, TimeWindow> {
    private static final long serialVersionUID = 1L;
    protected long sessionTimeout;
    protected long size;

    protected CustomerWindows(long sessionTimeout, long size) {
        if (sessionTimeout < 0L || size <= 0L) {
            throw new IllegalArgumentException("CustomerWindows parameters must satisfy 0 < sessionTimeout; 0 < size");
        } else {
            this.sessionTimeout = sessionTimeout;
            this.size = size;
        }
    }

    public Collection<TimeWindow> assignWindows(Object element, long timestamp, WindowAssignerContext context) {
        return Collections.singletonList(new TimeWindow(timestamp, timestamp + this.sessionTimeout));
    }

    public Trigger<Object, TimeWindow> getDefaultTrigger(StreamExecutionEnvironment env) {
        return EventTimeTrigger.create();
    }

    public String toString() {
        return "EventTimeSessionWindows(" + this.sessionTimeout + ")";
    }

    public static  CustomerWindows withWindowLength(long size) {
        return new CustomerWindows(0, size);
    }

    public TypeSerializer<TimeWindow> getWindowSerializer(ExecutionConfig executionConfig) {
        return new TimeWindow.Serializer();
    }

    public boolean isEventTime() {
        return true;
    }

    public void mergeWindows(Collection<TimeWindow> windows, MergeCallback<TimeWindow> c) {
        mergeWindowsInner(windows, c);
    }
    public void mergeWindowsInner(Collection<TimeWindow> windows, MergeCallback<TimeWindow> c) {
        List<TimeWindow> sortedWindows = new ArrayList<>(windows);
        Collections.sort(sortedWindows, Comparator.comparingLong(TimeWindow::getStart));
        List<Tuple2<TimeWindow, Set<TimeWindow>>> merged = new ArrayList<>();
        Tuple2<TimeWindow, Set<TimeWindow>> currentMerge = null;
        Iterator var5 = sortedWindows.iterator();

        while (var5.hasNext()) {
            TimeWindow candidate = (TimeWindow) var5.next();
            if (currentMerge == null) {
                currentMerge = new Tuple2<>();
                currentMerge.f0 = new TimeWindow(candidate.getStart(), candidate.getStart() + this.size);
                currentMerge.f1 = new HashSet<>();
                currentMerge.f1.add(candidate);
            } else if (currentMerge.f0.getStart() < candidate.getStart() && candidate.getStart() < currentMerge.f0.getStart() + this.size ) {
//                currentMerge.f0 = currentMerge.f0.cover(candidate);
                currentMerge.f1.add(candidate);
            } else {
                merged.add(currentMerge);
                currentMerge = new Tuple2<>();
                currentMerge.f0 = new TimeWindow(candidate.getStart(), candidate.getStart() + this.size);
                currentMerge.f1 = new HashSet<>();
                currentMerge.f1.add(candidate);
            }
        }

        if (currentMerge != null) {
            merged.add(currentMerge);
        }

        var5 = merged.iterator();

        while (var5.hasNext()) {
            Tuple2<TimeWindow, Set<TimeWindow>> m = (Tuple2<TimeWindow, Set<TimeWindow>>) var5.next();
            if (m.f1.size() > 1) {
                c.merge(m.f1, m.f0);
            }
        }
    }
}