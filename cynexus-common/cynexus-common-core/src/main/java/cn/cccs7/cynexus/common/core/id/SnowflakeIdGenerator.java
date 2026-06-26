package cn.cccs7.cynexus.common.core.id;

import java.util.concurrent.atomic.AtomicLong;

public class SnowflakeIdGenerator implements IdGenerator {

    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis());

    @Override
    public long nextId() {
        return sequence.incrementAndGet();
    }
}
