/*
 * Copyright (c) 2017-2019 AxonIQ B.V. and/or licensed to AxonIQ B.V.
 * under one or more contributor license agreements.
 *
 *  Licensed under the AxonIQ Open Source License Agreement v1.0;
 *  you may not use this file except in compliance with the license.
 *
 */

package io.axoniq.axonserver.message.query.subscription;

import io.axoniq.axonserver.applicationevents.SubscriptionQueryEvents.ProxiedSubscriptionQueryRequest;
import io.axoniq.axonserver.applicationevents.SubscriptionQueryEvents.SubscriptionQueryCanceled;
import io.axoniq.axonserver.applicationevents.SubscriptionQueryEvents.SubscriptionQueryRequestEvent;
import io.axoniq.axonserver.applicationevents.SubscriptionQueryEvents.SubscriptionQueryResponseReceived;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Sara Pellegrini on 01/05/2018.
 * sara.pellegrini@gmail.com
 */
@Component
public class QueryUpdateDispatcher {

    private final Map<String, UpdateHandler> handlers = new ConcurrentHashMap<>();

    @EventListener
    public void on(ProxiedSubscriptionQueryRequest event) {
        String subscriptionId = event.subscriptionQuery().getSubscriptionIdentifier();
        if (event.isSubscription()) {
            handlers.put(subscriptionId, event.handler());
        } else {
            handlers.remove(subscriptionId);
        }
    }

    @EventListener
    public void on(SubscriptionQueryRequestEvent event) {
        handlers.put(event.subscriptionId(), event.handler());
    }

    @EventListener
    public void on(SubscriptionQueryCanceled event) {
        handlers.remove(event.subscriptionId());
    }

    @EventListener
    public void on(SubscriptionQueryResponseReceived event) {
        UpdateHandler handler = handlers.get(event.subscriptionId());
        if (handler == null) {
            event.unknownSubscriptionHandler().run();
        } else {
            handler.onSubscriptionQueryResponse(event.response());
        }
    }
}
