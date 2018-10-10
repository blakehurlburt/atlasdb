/*
 * Copyright 2018 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.timestamp;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

import com.palantir.timestamp.utils.ShufflingTimestampService;

public class CorroboratingTimestampServiceTests {

    private GoBackInTimeTimestampService rawService;
    private TimestampService corroboratingTimestampService;

    @Before
    public void before() {
        rawService = new GoBackInTimeTimestampService();
        corroboratingTimestampService = new CorroboratingTimestampService(rawService);
    }

    @Test
    public void getFreshTimestampShouldFailIfGoesBackInTime() {
        getIndividualTimestamps(20);
        goBackInTimeFor(10);

        assertThatThrownBy(corroboratingTimestampService::getFreshTimestamp).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getFreshTimestampsShouldFailIfGoesBackInTime() {
        getBatchedTimestamps(20);
        goBackInTimeFor(10);

        assertThatThrownBy(() -> corroboratingTimestampService.getFreshTimestamps(10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shufflingTimestampService() throws InterruptedException, ExecutionException{
        GoBackInTimeTimestampService rawService = new GoBackInTimeTimestampService();
        TimestampService shufflingService = new ShufflingTimestampService(rawService);
        TimestampService timestampService = new CorroboratingTimestampService(shufflingService);

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        CompletionService<Long> completionService = new ExecutorCompletionService<>(executorService);
        List<Future<Long>> timestamps = new ArrayList<>();
        for (int i=0; i<6; i++) {
            timestamps.add(completionService.submit(() -> timestampService.getFreshTimestamp()));
        }

        for (int i=0; i<6; i++) {
            System.out.println(completionService.take().get());
        }
    }

    private void getIndividualTimestamps(int numberOfTimestamps) {
        for (int i = 0; i < numberOfTimestamps; i++) {
            corroboratingTimestampService.getFreshTimestamp();
        }
    }

    private void getBatchedTimestamps(int numberOfTimestamps) {
        corroboratingTimestampService.getFreshTimestamps(numberOfTimestamps);
    }

    private void goBackInTimeFor(int numberOfTimestamps) {
        rawService.goBackInTime(numberOfTimestamps);
    }

    static class GoBackInTimeTimestampService implements TimestampService, TimestampManagementService {
        private final AtomicLong counter = new AtomicLong(0);

        public void goBackInTime(long time) {
            counter.getAndUpdate(x -> {
                if (x < time) {
                    return 0;
                }
                return x - time;
            });
        }

        @Override
        public long getFreshTimestamp() {
            return counter.incrementAndGet();
        }

        @Override
        public TimestampRange getFreshTimestamps(int timestampsToGet) {
            if (timestampsToGet <= 0) {
                throw new IllegalArgumentException("Argument must be positive: " + timestampsToGet);
            }
            long topValue = counter.addAndGet(timestampsToGet);
            return TimestampRange.createInclusiveRange(topValue - timestampsToGet + 1, topValue);
        }

        @Override
        public void fastForwardTimestamp(long currentTimestamp) {
            long latestTimestampFromService = counter.get();
            while (latestTimestampFromService < currentTimestamp) {
                counter.compareAndSet(latestTimestampFromService, currentTimestamp);
                latestTimestampFromService = counter.get();
            }
        }

        @Override
        public String ping() {
            return PING_RESPONSE;
        }
    }
}