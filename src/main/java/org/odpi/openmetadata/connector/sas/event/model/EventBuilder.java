//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model;

import java.util.Date;
import java.util.UUID;

/**
 * Builder used for constructing event instances.
 *
 * @param <T> the type of data included within the event
 */
public class EventBuilder<T> {

    private String id;
    private String type;
    private String payloadType;
    private String user;
    private Date timeStamp;
    private EventHeaders headers;
    private T payload;

    private EventBuilder(T payload)
    {
        this.payload = payload;
    }

    private EventBuilder(Event<T> event)
    {
        this(event.getPayload());
        this.id = event.getId();
        this.type = event.getType();
        this.payloadType = event.getPayloadType();
        this.user = event.getUser();
        this.timeStamp = event.getTimeStamp();
        this.headers = event.getHeaders();
    }

    /**
     * Adds a header to the event.
     * @param key the header's name, or key
     * @param value the header's value
     * @return the builder
     */
    public EventBuilder<T> header(String key, Object value)
    {
        if (headers == null)
        {
            headers = new EventHeaders();
        }
        headers.set(key, value);
        return this;
    }

    /**
     * Adds a set of event headers.
     * @param headers the event headers
     * @return the builder
     */
    public EventBuilder<T> headers(EventHeaders headers)
    {
        this.headers = headers;
        return this;
    }

    /**
     * Sets the id of the event, if not already present.
     * @param id the event's id
     * @return the builder
     */
    public EventBuilder<T> idIfAbsent(String id)
    {
        if (this.id == null)
        {
            this.id = id;
        }
        return this;
    }

    /**
     * Sets the specific type of event.  During construction of the event, the event's type can be
     * inferred from the payload assuming the payload is an instance of {@link TypedPayload}, in which
     * case it is not required to set the specific type.  For all other cases though, the event type
     * must be set.
     * @param type the event type
     * @return the builder
     */
    public EventBuilder<T> type(String type)
    {
        this.type = type;
        return this;
    }

    /**
     * Sets the type of payload associated with the event.  The payloadType informs consumers the
     * exact type of the payload object that can be expected, as well as allows a consumer to properly
     * deserialize the event into the correct Java bean.
     * <p>
     * During construction of the event, the payload type can be inferred from the payload assuming
     * the payload is an instance of {@link TypedPayload}, in which case it is not required to set
     * the specific type.
     * @param payloadType the payload type
     * @return the builder
     */
    public EventBuilder<T> payloadType(String payloadType)
    {
        this.payloadType = payloadType;
        return this;
    }

    /**
     * Sets the name of the user associated with the event.
     * <p>
     * This value may differ depending on the context.  For example, this may represent the actual
     * user currently authenticated within the system, or it may represent a system wide user that
     * is executing an operation on behalf of someone.
     * @param user the user id associated with the event
     * @return the builder
     */
    public EventBuilder<T> user(String user)
    {
        this.user = user;
        return this;
    }

    /**
     * Set the date value for which this event represents. Must be less than or equal to the
     * publishedTimeStamp field. Used in situations where an event can be fired based on an
     * action that occurred in the past.
     * @param timeStamp the date of the event
     * @return the builder
     */
    public EventBuilder<T> timeStamp(Date timeStamp)
    {
        this.timeStamp = timeStamp;
        return this;
    }

    /**
     * Builds a new event instance.
     * @return a new event
     */
    public Event<T> build()
    {
        // assign a new id for the event if necessary
        if (id == null)
        {
            id = UUID.randomUUID().toString();
        }

        // infer the event and payload type
        if (payload instanceof TypedPayload)
        {
            if (type == null)
            {
                type = ((TypedPayload) payload).getEventType();
            }
            if (payloadType == null)
            {
                payloadType = ((TypedPayload) payload).getPayloadType();
            }
        }
        return new Event<>(payload, headers, id, type, payloadType, user, timeStamp);
    }

    /**
     * Creates a new event builder with the specified data payload
     * @param payload the event payload
     * @param <T> the payload type
     * @return event builder
     */
    public static <T> EventBuilder<T> withPayload(T payload)
    {
        return new EventBuilder<>(payload);
    }

    /**
     * Creates a builder for a new {@link Event} instance.  The contents of the
     * new event will be pre-populated based on the contents of the original event.
     * @param event the original event
     * @param <T> the payload type
     * @return event builder
     */
    public static <T> EventBuilder<T> fromEvent(Event<T> event)
    {
        return new EventBuilder<>(event);
    }

}
