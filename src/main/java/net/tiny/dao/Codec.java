package net.tiny.dao;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Function;

public final class Codec {

    public static float[] encodeLong(long value) {
        ByteBuffer buffer = ((ByteBuffer)ByteBuffer.allocate(8).putLong(value).rewind())
                .order(ByteOrder.LITTLE_ENDIAN);
        return new float[] {buffer.getFloat(), buffer.getFloat()};
    }

    public static long decodeLong(float[] value) {
        ByteBuffer buffer = ((ByteBuffer)ByteBuffer.allocate(8).putFloat(value[1]).putFloat(value[0]).rewind())
                .order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getLong();
    }

    public static Function<Long, float[]> LONG_FLOATS = new Function<Long, float[]>() {
        @Override
        public float[] apply(Long id) {
            return encodeLong(id);
        }};
    public static Function<float[], Long> FLOATS_LONG = new Function<float[], Long>() {
        @Override
        public Long apply(float[] entry) {
            return decodeLong(new float[] {entry[entry.length-2], entry[entry.length-1]});
        }};
}
