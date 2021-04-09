//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.odpi.openmetadata.connector.sas.event.model.EventHeaders;
import org.odpi.openmetadata.connector.sas.event.model.context.SerializationUtil;

/**
 * The common event model class.  This is used to provide a generic, and standard, approach to
 * submitting events to a message broker.  Serves as a wrapper around any generic object
 * instance which can be customized to provide additional information for the specific event.
 */
public class Event<T> {

    public static final int EVENT_VERSION = 2;
    public static final String CONTENT_TYPE = "application/vnd.sas.event+json";

    private int version = EVENT_VERSION;
    private String id;
    private String type;
    private String payloadType;
    private String user;
    private Date timeStamp;

    private EventHeaders headers;
    private T payload;

    /*
     * Default constructor is required for JSON deserialization
     */
    public Event()
    {
    }


    protected Event(T payload, EventHeaders headers, String id, String type,
                    String payloadType, String user, Date timeStamp)
    {
        this.payload = payload;
        this.headers = headers;
        this.id = id;
        this.type = type;
        this.payloadType = payloadType;
        this.user = user;
        this.timeStamp = timeStamp;
    }

    /**
     * Returns the version of the event model
     * @return version
     */
    public int getVersion()
    {
        return version;
    }

    /**
     * Returns the unique identifier of the event
     * @return id
     */
    public String getId()
    {
        return id;
    }

    /**
     * The name of the user associated with the event.
     * <p>
     * This value may differ depending on the context.  For example, this may represent the actual
     * user currently authenticated within the system, or it may represent a system wide user that
     * is executing an operation on behalf of someone.
     * @return user name
     */
    public String getUser()
    {
        return user;
    }

    /**
     * Represents the exact time when the event took place.  This may be equivalent to the current
     * time (i.e. when the event is published) or a specific time that occurred in the past.
     * @return time stamp
     */
    public Date getTimeStamp()
    {
        return timeStamp;
    }

    /**
     * The specific type of event.
     * @return event type
     */
    public String getType()
    {
        return type;
    }

    /**
     * Returns the type of the payload
     * @return payload type
     */
    public String getPayloadType()
    {
        return payloadType;
    }

    /**
     * Returns the collection of headers associated with the event
     * @return headers
     */
    public EventHeaders getHeaders()
    {
        if (headers == null)
            headers = new EventHeaders();
        return headers;
    }

    /**
     * The customized event payload.
     * @return data
     */
    public T getPayload()
    {
        return payload;
    }

    /**
     * Returns the event's payload in a JSON string format.
     * @return payload
     * @throws JsonProcessingException when the event payload fails to serialize to json properly
     */
    @JsonIgnore
    public String getPayloadAsString() throws JsonProcessingException
    {
        if (payload == null) return null;
        ObjectMapper mapper = SerializationUtil.createObjectMapperForEvent(this);
        return mapper.writeValueAsString(payload);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(headers);
        result = prime * result + Objects.hashCode(payload);
        result = prime * result + Objects.hashCode(id);
        result = prime * result + Objects.hashCode(timeStamp);
        result = prime * result + Objects.hashCode(type);
        result = prime * result + Objects.hashCode(payloadType);
        result = prime * result + Objects.hashCode(user);
        result = prime * result + version;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj.getClass() != getClass())
            return false;
        Event<?> rhs = (Event<?>) obj;
        return (Objects.equals(headers, rhs.headers) &&
                Objects.equals(payload, rhs.payload) &&
                Objects.equals(id, rhs.id) &&
                Objects.equals(timeStamp, rhs.timeStamp) &&
                Objects.equals(type, rhs.type) &&
                Objects.equals(payloadType, rhs.payloadType) &&
                Objects.equals(user, rhs.user) &&
                (version == rhs.version));
    }

    @Override
    public String toString()
    {
        return "Event [id=" + id + ", type=" + type + ", payloadType=" + payloadType +", user=" + user + ", timeStamp=" + timeStamp + ", payload=" + payload + "]";
    }

}
