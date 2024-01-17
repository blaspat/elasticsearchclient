/*
 * Copyright 2024 Blasius Patrick
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

package io.github.blaspat.global;


import java.util.concurrent.atomic.AtomicBoolean;

public class ClientStatus {
    private static final AtomicBoolean clientConnected = new AtomicBoolean(true);

    public static boolean isClientConnected() {
        return clientConnected.get();
    }

    public static void setClientConnected(boolean isConnected) {
        clientConnected.set(isConnected);
    }
}
