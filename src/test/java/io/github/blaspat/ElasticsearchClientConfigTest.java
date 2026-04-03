package io.github.blaspat;

import io.github.blaspat.helper.ElasticsearchProperties;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ElasticsearchClientConfigTest {

    @Test
    public void concurrentHashMap_isThreadSafe_forConcurrentReadsAndWrites() throws Exception {
        ConcurrentHashMap<Integer, ElasticsearchClientHolder> clients = new ConcurrentHashMap<>();
        AtomicInteger counter = new AtomicInteger(0);

        // Pre-populate with mock holders
        clients.put(0, mock(ElasticsearchClientHolder.class));
        clients.put(1, mock(ElasticsearchClientHolder.class));

        int threads = 10;
        int iterations = 100;

        Thread[] threadArr = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            threadArr[t] = new Thread(() -> {
                for (int i = 0; i < iterations; i++) {
                    int index = counter.getAndUpdate(x -> (x + 1) % 2);
                    // Concurrent access to the map
                    ElasticsearchClientHolder holder = clients.get(index);
                    // Concurrent write to the map
                    if (i % 10 == 0) {
                        clients.put(index, mock(ElasticsearchClientHolder.class));
                    }
                }
            });
            threadArr[t].start();
        }

        for (Thread t : threadArr) {
            t.join();
        }

        // Should not throw and map should remain consistent
        assertEquals(2, clients.size());
    }

    @Test
    public void atomicInteger_roundRobin_shouldCycleThroughIndices() {
        AtomicInteger currentIndex = new AtomicInteger(0);
        int poolSize = 3;

        int[] sequence = new int[6];
        for (int i = 0; i < 6; i++) {
            sequence[i] = currentIndex.getAndUpdate(idx -> (idx + 1) % poolSize);
        }

        // Should cycle: 0, 1, 2, 0, 1, 2
        assertEquals(0, sequence[0]);
        assertEquals(1, sequence[1]);
        assertEquals(2, sequence[2]);
        assertEquals(0, sequence[3]);
        assertEquals(1, sequence[4]);
        assertEquals(2, sequence[5]);
    }

    @Test
    public void concurrentHashMap_getAndPut_concurrently_shouldNotThrow() throws InterruptedException {
        ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<>();
        map.put(0, "initial");

        int threads = 20;
        Thread[] threadArr = new Thread[threads];

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            threadArr[t] = new Thread(() -> {
                for (int i = 0; i < 1000; i++) {
                    map.get(0);
                    map.put(threadId, "value-" + threadId);
                }
            });
            threadArr[t].start();
        }

        for (Thread t : threadArr) {
            t.join();
        }

        // All threads should have completed without exception
        assertEquals(20, map.size());
    }
}
