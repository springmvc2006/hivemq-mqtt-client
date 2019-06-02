/*
 * Copyright 2018 dc-square and the HiveMQ MQTT Client Project
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
 *
 */

package com.hivemq.client.internal.mqtt;

import com.hivemq.client.internal.mqtt.advanced.MqttClientAdvancedConfig;
import com.hivemq.client.internal.mqtt.datatypes.MqttClientIdentifierImpl;
import com.hivemq.client.internal.mqtt.lifecycle.MqttClientAutoReconnectImpl;
import com.hivemq.client.internal.mqtt.lifecycle.MqttClientAutoReconnectImplBuilder;
import com.hivemq.client.internal.mqtt.mqtt3.Mqtt3RxClientViewBuilder;
import com.hivemq.client.internal.mqtt.util.MqttChecks;
import com.hivemq.client.internal.util.Checks;
import com.hivemq.client.internal.util.collections.ImmutableList;
import com.hivemq.client.mqtt.*;
import com.hivemq.client.mqtt.datatypes.MqttClientIdentifier;
import com.hivemq.client.mqtt.lifecycle.MqttClientAutoReconnect;
import com.hivemq.client.mqtt.lifecycle.MqttClientConnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static com.hivemq.client.mqtt.MqttClient.*;

/**
 * @author Silvio Giebl
 */
public abstract class MqttRxClientBuilderBase<B extends MqttRxClientBuilderBase<B>> {

    private @NotNull MqttClientIdentifierImpl identifier =
            MqttClientIdentifierImpl.REQUEST_CLIENT_IDENTIFIER_FROM_SERVER;
    private @NotNull Object serverHost = DEFAULT_SERVER_HOST; // String or InetAddress
    private int serverPort = -1;
    private @Nullable InetSocketAddress serverAddress;
    private @Nullable MqttClientSslConfigImpl sslConfig;
    private @Nullable MqttWebSocketConfigImpl webSocketConfig;
    private @NotNull MqttClientExecutorConfigImpl executorConfig = MqttClientExecutorConfigImpl.DEFAULT;
    private @Nullable MqttClientAutoReconnectImpl autoReconnect;
    private @Nullable ImmutableList.Builder<MqttClientConnectedListener> connectedListenersBuilder;
    private @Nullable ImmutableList.Builder<MqttClientDisconnectedListener> disconnectedListenersBuilder;

    protected MqttRxClientBuilderBase() {}

    protected MqttRxClientBuilderBase(final @NotNull MqttRxClientBuilderBase<?> clientBuilder) {
        this.identifier = clientBuilder.identifier;
        this.serverHost = clientBuilder.serverHost;
        this.serverPort = clientBuilder.serverPort;
        this.serverAddress = clientBuilder.serverAddress;
        this.sslConfig = clientBuilder.sslConfig;
        this.webSocketConfig = clientBuilder.webSocketConfig;
        this.executorConfig = clientBuilder.executorConfig;
        this.autoReconnect = clientBuilder.autoReconnect;
        this.connectedListenersBuilder = clientBuilder.connectedListenersBuilder;
        this.disconnectedListenersBuilder = clientBuilder.disconnectedListenersBuilder;
    }

    protected abstract @NotNull B self();

    public @NotNull B identifier(final @Nullable String identifier) {
        this.identifier = MqttClientIdentifierImpl.of(identifier);
        return self();
    }

    public @NotNull B identifier(final @Nullable MqttClientIdentifier identifier) {
        this.identifier = MqttChecks.clientIdentifier(identifier);
        return self();
    }

    public @NotNull B serverAddress(final @NotNull InetSocketAddress address) {
        this.serverAddress = Checks.notNull(address, "Server address");
        return self();
    }

    public @NotNull B serverHost(final @Nullable String host) {
        setServerHost(Checks.notEmpty(host, "Server host"));
        return self();
    }

    public @NotNull B serverHost(final @Nullable InetAddress host) {
        setServerHost(Checks.notNull(host, "Server host"));
        return self();
    }

    private void setServerHost(final @NotNull Object serverHost) {
        this.serverHost = serverHost;
        if (serverAddress != null) {
            serverPort = serverAddress.getPort();
            serverAddress = null;
        }
    }

    public @NotNull B serverPort(final int port) {
        this.serverPort = Checks.unsignedShort(port, "Server port");
        if (serverAddress != null) {
            final InetAddress inetAddress = serverAddress.getAddress();
            if (inetAddress != null) {
                serverHost = inetAddress;
            } else {
                serverHost = serverAddress.getHostString();
            }
            serverAddress = null;
        }
        return self();
    }

    public @NotNull B useSslWithDefaultConfig() {
        this.sslConfig = MqttClientSslConfigImpl.DEFAULT;
        return self();
    }

    public @NotNull B useSsl(final @Nullable MqttClientSslConfig sslConfig) {
        this.sslConfig = Checks.notImplementedOrNull(sslConfig, MqttClientSslConfigImpl.class, "SSL config");
        return self();
    }

    public @NotNull MqttClientSslConfigImplBuilder.Nested<B> useSsl() {
        return new MqttClientSslConfigImplBuilder.Nested<>(sslConfig, this::useSsl);
    }

    public @NotNull B useWebSocketWithDefaultConfig() {
        this.webSocketConfig = MqttWebSocketConfigImpl.DEFAULT;
        return self();
    }

    public @NotNull B useWebSocket(final @Nullable MqttWebSocketConfig webSocketConfig) {
        this.webSocketConfig =
                Checks.notImplementedOrNull(webSocketConfig, MqttWebSocketConfigImpl.class, "WebSocket config");
        return self();
    }

    public @NotNull MqttWebSocketConfigImplBuilder.Nested<B> useWebSocket() {
        return new MqttWebSocketConfigImplBuilder.Nested<>(webSocketConfig, this::useWebSocket);
    }

    public @NotNull B executorConfig(final @Nullable MqttClientExecutorConfig executorConfig) {
        this.executorConfig =
                Checks.notImplemented(executorConfig, MqttClientExecutorConfigImpl.class, "Executor config");
        return self();
    }

    public @NotNull MqttClientExecutorConfigImplBuilder.Nested<B> executorConfig() {
        return new MqttClientExecutorConfigImplBuilder.Nested<>(executorConfig, this::executorConfig);
    }

    public @NotNull B automaticReconnectWithDefaultConfig() {
        this.autoReconnect = MqttClientAutoReconnectImpl.DEFAULT;
        return self();
    }

    public @NotNull B automaticReconnect(final @Nullable MqttClientAutoReconnect autoReconnect) {
        this.autoReconnect =
                Checks.notImplementedOrNull(autoReconnect, MqttClientAutoReconnectImpl.class, "Automatic reconnect");
        return self();
    }

    public @NotNull MqttClientAutoReconnectImplBuilder.Nested<B> automaticReconnect() {
        return new MqttClientAutoReconnectImplBuilder.Nested<>(autoReconnect, this::automaticReconnect);
    }

    public @NotNull B addConnectedListener(final @Nullable MqttClientConnectedListener connectedListener) {
        Checks.notNull(connectedListener, "Connected listener");
        if (connectedListenersBuilder == null) {
            connectedListenersBuilder = ImmutableList.builder();
        }
        connectedListenersBuilder.add(connectedListener);
        return self();
    }

    public @NotNull B addDisconnectedListener(final @Nullable MqttClientDisconnectedListener disconnectedListener) {
        Checks.notNull(disconnectedListener, "Disconnected listener");
        if (disconnectedListenersBuilder == null) {
            disconnectedListenersBuilder = ImmutableList.builder();
        }
        disconnectedListenersBuilder.add(disconnectedListener);
        return self();
    }

    private @NotNull InetSocketAddress getServerAddress() {
        if (serverAddress != null) {
            return serverAddress;
        }
        if (serverHost instanceof InetAddress) {
            return new InetSocketAddress((InetAddress) serverHost, getServerPort());
        }
        return InetSocketAddress.createUnresolved((String) serverHost, getServerPort());
    }

    private int getServerPort() {
        if (serverPort != -1) {
            return serverPort;
        }
        if (sslConfig == null) {
            if (webSocketConfig == null) {
                return DEFAULT_SERVER_PORT;
            }
            return DEFAULT_SERVER_PORT_WEBSOCKET;
        }
        if (webSocketConfig == null) {
            return DEFAULT_SERVER_PORT_SSL;
        }
        return DEFAULT_SERVER_PORT_WEBSOCKET_SSL;
    }

    private @NotNull ImmutableList<MqttClientConnectedListener> buildConnectedListeners() {
        if (connectedListenersBuilder == null) {
            return ImmutableList.of();
        }
        return connectedListenersBuilder.build();
    }

    private @NotNull ImmutableList<MqttClientDisconnectedListener> buildDisconnectedListeners() {
        if (disconnectedListenersBuilder == null) {
            if (autoReconnect == null) {
                return ImmutableList.of();
            }
            return ImmutableList.of(autoReconnect);
        }
        if (autoReconnect == null) {
            return disconnectedListenersBuilder.build();
        }
        return ImmutableList.<MqttClientDisconnectedListener>builder().add(autoReconnect)
                .addAll(disconnectedListenersBuilder.build())
                .build();
    }

    protected @NotNull MqttClientConfig buildClientConfig(
            final @NotNull MqttVersion mqttVersion, final @NotNull MqttClientAdvancedConfig advancedConfig) {

        return new MqttClientConfig(mqttVersion, identifier, getServerAddress(), executorConfig, sslConfig,
                webSocketConfig, advancedConfig, buildConnectedListeners(), buildDisconnectedListeners());
    }

    public static class Choose extends MqttRxClientBuilderBase<Choose> implements MqttClientBuilder {

        @Override
        protected @NotNull Choose self() {
            return this;
        }

        @Override
        public @NotNull Mqtt3RxClientViewBuilder useMqttVersion3() {
            return new Mqtt3RxClientViewBuilder(this);
        }

        @Override
        public @NotNull MqttRxClientBuilder useMqttVersion5() {
            return new MqttRxClientBuilder(this);
        }
    }
}
