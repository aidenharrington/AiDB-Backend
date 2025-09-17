package com.aidb.aidb_backend.service.util.sql;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Snowflake ID Generator
 * 
 * Generates unique 64-bit IDs using the Twitter Snowflake algorithm.
 * The ID structure is:
 * - 1 bit: always 0 (reserved for future use)
 * - 41 bits: timestamp (milliseconds since epoch)
 * - 10 bits: machine ID (node ID)
 * - 12 bits: sequence number (increments for same millisecond)
 * 
 * This implementation can generate up to 4096 unique IDs per millisecond per machine.
 */
@Component
public class SnowflakeIdGenerator {
    
    // Epoch start time (January 1, 2023 UTC)
    private static final long EPOCH = 1672531200000L;
    
    // Bit lengths
    private static final long MACHINE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    
    // Maximum values
    private static final long MAX_MACHINE_ID = (1L << MACHINE_ID_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    
    // Bit shifts
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    
    private final long machineId;
    private final AtomicLong sequence = new AtomicLong(0L);
    private volatile long lastTimestamp = -1L;
    
    /**
     * Creates a Snowflake ID generator with the specified machine ID.
     * 
     * @param machineId The machine/node ID (0-1023)
     * @throws IllegalArgumentException if machineId is out of range
     */
    public SnowflakeIdGenerator(long machineId) {
        if (machineId < 0 || machineId > MAX_MACHINE_ID) {
            throw new IllegalArgumentException(
                String.format("Machine ID must be between 0 and %d", MAX_MACHINE_ID)
            );
        }
        this.machineId = machineId;
    }
    
    /**
     * Creates a Snowflake ID generator with machine ID 0.
     * Useful for single-node applications.
     */
    public SnowflakeIdGenerator() {
        this(0L);
    }
    
    /**
     * Generates the next unique ID.
     * 
     * @return A unique 64-bit ID
     * @throws RuntimeException if clock moves backwards
     */
    public synchronized long nextId() {
        long timestamp = getCurrentTimestamp();
        
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate ID.");
        }
        
        if (timestamp == lastTimestamp) {
            // Same millisecond, increment sequence
            long currentSequence = sequence.incrementAndGet() & MAX_SEQUENCE;
            if (currentSequence == 0) {
                // Sequence overflow, wait for next millisecond
                timestamp = waitForNextMillis(lastTimestamp);
            }
        } else {
            // New millisecond, reset sequence
            sequence.set(0L);
        }
        
        lastTimestamp = timestamp;
        
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) |
               (machineId << MACHINE_ID_SHIFT) |
               sequence.get();
    }
    
    /**
     * Generates the next unique ID as a String.
     * 
     * @return A unique ID as a String
     */
    public String nextIdString() {
        return String.valueOf(nextId());
    }
    
    /**
     * Extracts the timestamp from a Snowflake ID.
     * 
     * @param id The Snowflake ID
     * @return The timestamp in milliseconds since epoch
     */
    public static long extractTimestamp(long id) {
        return (id >> TIMESTAMP_SHIFT) + EPOCH;
    }
    
    /**
     * Extracts the machine ID from a Snowflake ID.
     * 
     * @param id The Snowflake ID
     * @return The machine ID
     */
    public static long extractMachineId(long id) {
        return (id >> MACHINE_ID_SHIFT) & MAX_MACHINE_ID;
    }
    
    /**
     * Extracts the sequence number from a Snowflake ID.
     * 
     * @param id The Snowflake ID
     * @return The sequence number
     */
    public static long extractSequence(long id) {
        return id & MAX_SEQUENCE;
    }
    
    /**
     * Gets the current timestamp in milliseconds.
     * 
     * @return Current timestamp
     */
    private long getCurrentTimestamp() {
        return Instant.now().toEpochMilli();
    }
    
    /**
     * Waits for the next millisecond if sequence overflow occurs.
     * 
     * @param lastTimestamp The last timestamp used
     * @return The next timestamp
     */
    private long waitForNextMillis(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }
    
    /**
     * Gets the machine ID of this generator.
     * 
     * @return The machine ID
     */
    public long getMachineId() {
        return machineId;
    }
    
    /**
     * Gets the maximum sequence number.
     * 
     * @return The maximum sequence number
     */
    public static long getMaxSequence() {
        return MAX_SEQUENCE;
    }
    
    /**
     * Gets the maximum machine ID.
     * 
     * @return The maximum machine ID
     */
    public static long getMaxMachineId() {
        return MAX_MACHINE_ID;
    }
} 