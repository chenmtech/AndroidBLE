package com.cmtech.android.ble.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorServiceUtil {
    public static void shutdownNowAndAwaitTerminate(ExecutorService pool) {
        if (pool != null) {
            pool.shutdownNow();

            try {
                boolean isTerminated = false;

                while (!isTerminated) {
                    isTerminated = pool.awaitTermination(1, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
