/*
 * Copyright (c) 2017-2019 AxonIQ B.V. and/or licensed to AxonIQ B.V.
 * under one or more contributor license agreements.
 *
 *  Licensed under the AxonIQ Open Source License Agreement v1.0;
 *  you may not use this file except in compliance with the license.
 *
 */

package io.axoniq.axonserver.grpc;

import io.axoniq.axonserver.ProcessingInstructionHelper;
import io.axoniq.axonserver.applicationevents.SubscriptionEvents;
import io.axoniq.axonserver.applicationevents.TopologyEvents;
import io.axoniq.axonserver.grpc.query.QueryProviderInbound;
import io.axoniq.axonserver.grpc.query.QueryProviderOutbound;
import io.axoniq.axonserver.grpc.query.QueryRequest;
import io.axoniq.axonserver.grpc.query.QueryResponse;
import io.axoniq.axonserver.grpc.query.QuerySubscription;
import io.axoniq.axonserver.message.ClientIdentification;
import io.axoniq.axonserver.message.FlowControlQueues;
import io.axoniq.axonserver.message.query.QueryDispatcher;
import io.axoniq.axonserver.message.query.WrappedQuery;
import io.axoniq.axonserver.topology.Topology;
import io.axoniq.axonserver.util.CountingStreamObserver;
import io.grpc.stub.StreamObserver;
import org.junit.*;
import org.springframework.context.ApplicationEventPublisher;

import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.isA;

/**
 * @author Marc Gathier
 */
public class QueryServiceTest {
    private QueryService testSubject;
    private QueryDispatcher queryDispatcher;
    private FlowControlQueues<WrappedQuery> queryQueue;

    private ApplicationEventPublisher eventPublisher;

    @Before
    public void setUp()  {
        queryDispatcher = mock(QueryDispatcher.class);
        queryQueue = new FlowControlQueues<>();
        eventPublisher = mock(ApplicationEventPublisher.class);
        when(queryDispatcher.getQueryQueue()).thenReturn(queryQueue);
        testSubject = new QueryService(queryDispatcher, () -> Topology.DEFAULT_CONTEXT, eventPublisher);
    }

    @Test
    public void flowControl() throws Exception {
        CountingStreamObserver<QueryProviderInbound> countingStreamObserver  = new CountingStreamObserver<>();
        StreamObserver<QueryProviderOutbound> requestStream = testSubject.openStream(countingStreamObserver);
        requestStream.onNext(QueryProviderOutbound.newBuilder().setFlowControl(FlowControl.newBuilder().setPermits(2).setClientId("name").build()).build());
        Thread.sleep(250);
        assertEquals(1, queryQueue.getSegments().size());
        ClientIdentification name = new ClientIdentification(Topology.DEFAULT_CONTEXT, "name");
        queryQueue.put(name.toString(), new WrappedQuery(
                                                        new SerializedQuery(Topology.DEFAULT_CONTEXT, "name",
                                                                     QueryRequest.newBuilder()
                                                                                 .addProcessingInstructions(ProcessingInstructionHelper.timeout(10000))
                                                                                 .build()), System.currentTimeMillis() + 2000));
        Thread.sleep(150);
        assertEquals(1, countingStreamObserver.count);
        queryQueue.put(name.toString(), new WrappedQuery(
                                                        new SerializedQuery(Topology.DEFAULT_CONTEXT, "name", QueryRequest.newBuilder().build()), System.currentTimeMillis() - 2000));
        Thread.sleep(150);
        assertEquals(1, countingStreamObserver.count);
        verify(queryDispatcher).removeFromCache(any(), any());
    }

    @Test
    public void subscribe()  {
        StreamObserver<QueryProviderOutbound> requestStream = testSubject.openStream(new CountingStreamObserver<>());
        requestStream.onNext(QueryProviderOutbound.newBuilder()
                .setSubscribe(QuerySubscription.newBuilder().setClientId("name").setComponentName("component").setQuery("query"))
                .build());
        verify(eventPublisher).publishEvent(isA(SubscriptionEvents.SubscribeQuery.class));
    }

    @Test
    public void unsubscribe()  {
        StreamObserver<QueryProviderOutbound> requestStream = testSubject.openStream(new CountingStreamObserver<>());
        requestStream.onNext(QueryProviderOutbound.newBuilder()
                .setUnsubscribe(QuerySubscription.newBuilder().setClientId("name").setComponentName("component").setQuery("command"))
                .build());
        verify(eventPublisher, times(0)).publishEvent(isA(SubscriptionEvents.UnsubscribeQuery.class));
    }
    @Test
    public void unsubscribeAfterSubscribe() {
        StreamObserver<QueryProviderOutbound> requestStream = testSubject.openStream(new CountingStreamObserver<>());
        requestStream.onNext(QueryProviderOutbound.newBuilder()
                .setSubscribe(QuerySubscription.newBuilder().setClientId("name").setComponentName("component").setQuery("command"))
                .build());
        requestStream.onNext(QueryProviderOutbound.newBuilder()
                .setUnsubscribe(QuerySubscription.newBuilder().setClientId("name").setComponentName("component").setQuery("command"))
                .build());
        verify(eventPublisher).publishEvent(isA(SubscriptionEvents.UnsubscribeQuery.class));
    }

    @Test
    public void cancelAfterSubscribe() {
        StreamObserver<QueryProviderOutbound> requestStream = testSubject.openStream(new CountingStreamObserver<>());
        requestStream.onNext(QueryProviderOutbound.newBuilder()
                .setSubscribe(QuerySubscription.newBuilder().setClientId("name").setComponentName("component").setQuery("command"))
                .build());
        requestStream.onError(new RuntimeException("failed"));
    }

    @Test
    public void cancelBeforeSubscribe() {
        StreamObserver<QueryProviderOutbound> requestStream = testSubject.openStream(new CountingStreamObserver<>());
        requestStream.onError(new RuntimeException("failed"));
    }

    @Test
    public void close() {
        StreamObserver<QueryProviderOutbound> requestStream = testSubject.openStream(new CountingStreamObserver<>());
        requestStream.onNext(QueryProviderOutbound.newBuilder().setFlowControl(FlowControl.newBuilder().setPermits(1).setClientId("name").build()).build());
        requestStream.onCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void dispatch()  {
        doAnswer(invocationOnMock -> {
            Consumer<QueryResponse> callback = (Consumer<QueryResponse>) invocationOnMock.getArguments()[1];
            callback.accept(QueryResponse.newBuilder().build());
            return null;
        }).when(queryDispatcher).query(isA(SerializedQuery.class), isA(Consumer.class), any());
        CountingStreamObserver<QueryResponse> responseObserver = new CountingStreamObserver<>();
        testSubject.query(QueryRequest.newBuilder().build(), responseObserver);
        assertEquals(1, responseObserver.count);
    }

    @Test
    public void queryHandlerDisconnected(){
        StreamObserver<QueryProviderOutbound> requestStream = testSubject.openStream(new CountingStreamObserver<>());
        requestStream.onNext(QueryProviderOutbound.newBuilder()
                                                  .setSubscribe(QuerySubscription.newBuilder().setClientId("name").setComponentName("component").setQuery("command"))
                                                  .build());
        requestStream.onError(new RuntimeException("failed"));
        verify(eventPublisher).publishEvent(isA(TopologyEvents.QueryHandlerDisconnected.class));

    }

}
