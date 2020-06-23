/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.atlasdb.timelock.adjudicate;

public final class Constants {
    private Constants() {
        // no op
    }

    public static final int MAXIMUM_NUMBER_OF_CLIENTS = 10_000;
    public static final int CLIENT_EXPIRATION_MINUTES = 2;
    public static final int NODE_EXPIRATION_MINUTES = 2;
    public static final int HEALTH_FEEDBACK_REPORT_EXPIRATION_MINUTES = 2;
}
