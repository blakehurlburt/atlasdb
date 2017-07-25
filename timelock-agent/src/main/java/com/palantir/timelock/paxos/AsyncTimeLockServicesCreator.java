/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
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

package com.palantir.timelock.paxos;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.palantir.atlasdb.timelock.AsyncTimelockResource;
import com.palantir.atlasdb.timelock.AsyncTimelockService;
import com.palantir.atlasdb.timelock.AsyncTimelockServiceImpl;
import com.palantir.atlasdb.timelock.TimeLockServices;
import com.palantir.atlasdb.timelock.lock.AsyncLockService;
import com.palantir.atlasdb.timelock.paxos.ManagedTimestampService;
import com.palantir.atlasdb.timelock.util.AsyncOrLegacyTimelockService;
import com.palantir.atlasdb.util.AtlasDbMetrics;
import com.palantir.lock.RemoteLockService;

public class AsyncTimeLockServicesCreator {
    private final PaxosLeadershipCreator leadershipCreator;

    public AsyncTimeLockServicesCreator(PaxosLeadershipCreator leadershipCreator) {
        this.leadershipCreator = leadershipCreator;
    }

    private TimeLockServices createAsyncTimeLockServices(
            String client,
            Supplier<ManagedTimestampService> rawTimestampServiceSupplier,
            RemoteLockService lockService) {
        AsyncOrLegacyTimelockService asyncOrLegacyTimelockService;// create async lock
        AsyncTimelockService asyncTimelockService = instrument(
                AsyncTimelockService.class,
                leadershipCreator.wrapInLeadershipProxy(
                        () -> AsyncTimeLockServicesCreator.createRawAsyncTimelockService(client, rawTimestampServiceSupplier),
                        AsyncTimelockService.class),
                client);
        asyncOrLegacyTimelockService = AsyncOrLegacyTimelockService.createFromAsyncTimelock(
                new AsyncTimelockResource(asyncTimelockService));
        return TimeLockServices.create(
                asyncTimelockService,
                lockService,
                asyncOrLegacyTimelockService,
                asyncTimelockService);
    }



    private static AsyncTimelockService createRawAsyncTimelockService(
            String client,
            Supplier<ManagedTimestampService> timestampServiceSupplier) {
        ScheduledExecutorService reaperExecutor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("async-lock-reaper-" + client + "-%d")
                        .setDaemon(true)
                        .build());
        ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("async-lock-timeouts-" + client + "-%d")
                        .setDaemon(true)
                        .build());
        return new AsyncTimelockServiceImpl(
                AsyncLockService.createDefault(reaperExecutor, timeoutExecutor),
                timestampServiceSupplier.get());
    }

    private <T> T instrumentInLeadershipProxy(Class<T> serviceClass, Supplier<T> serviceSupplier, String client) {
        return instrument(serviceClass, leadershipCreator.wrapInLeadershipProxy(serviceSupplier, serviceClass), client);
    }

    private static <T> T instrument(Class<T> serviceClass, T service, String client) {
        return AtlasDbMetrics.instrument(serviceClass, service, MetricRegistry.name(serviceClass, client));
    }
}
