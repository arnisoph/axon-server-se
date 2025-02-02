/*
 * Copyright (c) 2017-2019 AxonIQ B.V. and/or licensed to AxonIQ B.V.
 * under one or more contributor license agreements.
 *
 *  Licensed under the AxonIQ Open Source License Agreement v1.0;
 *  you may not use this file except in compliance with the license.
 *
 */

package io.axoniq.axonserver.config;

import io.axoniq.axonserver.util.StringUtils;
import io.grpc.internal.GrpcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

import java.net.UnknownHostException;

/**
 * @author Marc Gathier
 */
@Configuration
@ConfigurationProperties(prefix = "axoniq.axonserver")
public class MessagingPlatformConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(MessagingPlatformConfiguration.class);
    private static final int RESERVED = 10000;
    private static final int DEFAULT_MAX_TRANSACTION_SIZE = GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE-RESERVED;
    /**
     * gRPC port for axonserver platform
     */
    private int port = 8124;
    /**
     * gRPC port for communication between messing platform nodes
     */
    private int internalPort = 8224;
    /**
     * Node name of this axonserver platform node, if not set defaults to the hostname
     */
    private String name;
    /**
     * Hostname of this node as communicated to clients, defaults to the result of hostname command
     */
    private String hostname;
    /**
     * Domain of this node as communicated to clients. Optional, if set will be appended to the hostname in communication
     * with clients.
     */
    private String domain;
    /**
     * Hostname as communicated to other nodes of the cluster. Defaults to hostname.
     */
    private String internalHostname;
    /**
     * Domain as communicated to other nodes of the cluster. Optional, if not set, it will use the domain value.
     */
    private String internalDomain;

    /**
     * Internal, used to cache the value for the HTTP port.
     */
    private int httpPort;

    /**
     * Timeout for keep alive messages on gRPC connections.
     */
    private long keepAliveTimeout = 5000;
    /**
     * Interval at which AxonServer will send timeout messages. Set to 0 to disbable gRPC timeout checks
     */
    private long keepAliveTime = 2500;
    /**
     * Minimum keep alive interval accepted by this end of the gRPC connection.
     */
    private long minKeepAliveTime = 1000;



    @NestedConfigurationProperty
    private SslConfiguration ssl = new SslConfiguration();
    @NestedConfigurationProperty
    private AccessControlConfiguration accesscontrol = new AccessControlConfiguration();

    /**
     * Rate for synchronization of metrics information between nodes
     */
    private int metricsSynchronizationRate;

    /**
     * Expiry interval (minutes) of metrics
     */
    private int metricsInterval =15;

    private final SystemInfoProvider systemInfoProvider;
    /**
     * Location where the control DB backups are created.
     */
    private String controldbBackupLocation = ".";
    /*
     * Maximum inbound message size for gRPC
     */
    private int maxMessageSize = GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;
    /**
     * Location where AxonServer creates its pid file.
     */
    private String pidFileLocation = ".";

    /**
     * The initial flow control setting for gRPC level messages. This is the number of messages that may may be en-route
     * before the sender stops emitting messages. This setting is per-request and only affects streaming requests or
     * responses. Application-level flow control settings and buffer restriction settings are still in effect.
     * Defaults to 500.
     */
    private int grpcBufferedMessages = 500;

    /**
     * Number of threads for executing incoming gRPC requests
     */
    private int executorThreadCount = 16;

    public MessagingPlatformConfiguration(SystemInfoProvider systemInfoProvider) {
        this.systemInfoProvider = systemInfoProvider;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getInternalPort() {
        return internalPort;
    }

    public void setInternalPort(int internalPort) {
        this.internalPort = internalPort;
    }

    public String getName() {
        if( name == null) {
            name = getHostname();
        }

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostname() {
        if( StringUtils.isEmpty(hostname )) {
            try {
                hostname = systemInfoProvider.getHostName();
                if(!StringUtils.isEmpty(domain) && hostname.endsWith("." + domain)) {
                        hostname = hostname.substring(0, hostname.length() - domain.length() - 1);
                }
            } catch (UnknownHostException e) {
                logger.warn("Could not determine hostname from inet address: {}", e.getMessage());
            }
        }
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getInternalHostname() {
        if( StringUtils.isEmpty(internalHostname)) {
            internalHostname = getHostname();
        }
        return internalHostname;
    }

    public void setInternalHostname(String internalHostname) {
        this.internalHostname = internalHostname;
    }

    public String getInternalDomain() {
        if( StringUtils.isEmpty(internalDomain)) {
            internalDomain = getDomain();
        }
        return internalDomain;
    }

    public int getHttpPort() {
        if( httpPort == 0) {
            httpPort = systemInfoProvider.getPort();

        }
        return httpPort;
    }

    public void setInternalDomain(String internalDomain) {
        this.internalDomain = internalDomain;
    }

    public String getFullyQualifiedHostname() {
        if( ! StringUtils.isEmpty(getDomain())) return getHostname() + "." + getDomain();

        return getHostname();
    }

    public String getFullyQualifiedInternalHostname() {
        if( ! StringUtils.isEmpty(getInternalDomain()) ) return getInternalHostname() + "." + getInternalDomain();

        return getInternalHostname();
    }

    public SslConfiguration getSsl() {
        return ssl;
    }

    public void setSsl(SslConfiguration ssl) {
        this.ssl = ssl;
    }

    public AccessControlConfiguration getAccesscontrol() {
        return accesscontrol;
    }

    public void setAccesscontrol(AccessControlConfiguration accesscontrol) {
        this.accesscontrol = accesscontrol;
    }

    public int getMetricsInterval() {
        return metricsInterval;
    }

    public void setMetricsInterval(int metricsInterval) {
        this.metricsInterval = metricsInterval;
    }

    public long getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public void setKeepAliveTimeout(long keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public long getMinKeepAliveTime() {
        return minKeepAliveTime;
    }

    public void setMinKeepAliveTime(long minKeepAliveTime) {
        this.minKeepAliveTime = minKeepAliveTime;
    }

    public String getControldbBackupLocation() {
        return controldbBackupLocation;
    }

    public void setControldbBackupLocation(String controldbBackupLocation) {
        this.controldbBackupLocation = controldbBackupLocation;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    public void setMaxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    public int getMaxTransactionSize() {
        if( maxMessageSize == 0) return DEFAULT_MAX_TRANSACTION_SIZE;

        return maxMessageSize - RESERVED;

    }
    public String getPidFileLocation() {
        return pidFileLocation;
    }

    public void setMetricsSynchronizationRate(int metricsSynchronizationRate) {
        this.metricsSynchronizationRate = metricsSynchronizationRate;
    }

    public void setPidFileLocation(String pidFileLocation) {
        this.pidFileLocation = pidFileLocation;
    }

    public int getGrpcBufferedMessages() {
        return grpcBufferedMessages;
    }

    public void setGrpcBufferedMessages(int grpcBufferedMessages) {
        this.grpcBufferedMessages = grpcBufferedMessages;
    }

    public int getExecutorThreadCount() {
        return executorThreadCount;
    }

    public void setExecutorThreadCount(int executorThreadCount) {
        this.executorThreadCount = executorThreadCount;
    }
}
