//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Contains the set of headers associated with a particular event instance.
 */
public class EventHeaders {

    /**
     * The address location of where the event is to be published
     */
    public static final String EVENT_ADDRESS = "sas-event-address";

    /**
     * The event's subject, used to determine how the event should be routed
     */
    public static final String EVENT_SUBJECT = "sas-event-subject";

    /**
     * The event's source, typically referring to the application that published the event.
     */
    public static final String EVENT_SOURCE = "sas-event-source";

    /**
     * The content-type of the event
     */
    public static final String CONTENT_TYPE = "sas-content-type";

    /**
     * The timestamp (date-time) value for when the event was published to the message broker
     */
    public static final String PUBLISHED_TIMESTAMP = "sas-published-timestamp";

    /**
     * The remote address of the publisher
     */
    public static final String REMOTE_ADDRESS = "sas-remote-address";

    /**
     * The trace id associated with the event.
     */
    public static final String TRACE_ID = "sas-trace-id";

    /**
     * The deployment id associated with the event.
     */
    public static final String DEPLOYMENT_ID = "sas-deployment-id";


    private Map<String,Object> headers;

    public EventHeaders()
    {
        headers = new LinkedHashMap<>();
    }

    @JsonIgnore
    public Object get(String key)
    {
        return headers.get(key);
    }

    @JsonIgnore
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> typeClass)
    {
        Object value = get(key);
        if (value == null)
        {
            return null;
        }
        else
        {
            if (typeClass == null)
            {
                throw new IllegalArgumentException("The header type class must be specified");
            }
            else if (typeClass.isAssignableFrom(value.getClass()))
            {
                return (T) value;
            }
            else
            {
                throw new IllegalArgumentException("Invalid header type class was specified.");
            }
        }
    }

    public boolean containsHeader(String key)
    {
        return headers.containsKey(key);
    }

    @JsonAnyGetter
    public Map<String, Object> getHeaders()
    {
        return Collections.unmodifiableMap(headers);
    }

    @JsonAnySetter
    public void set(String key, Object value)
    {
        headers.put(key, value);
    }

    public void setHeaders(Map<String, Object> headers)
    {
        this.headers = headers;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(headers);
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EventHeaders rhs = (EventHeaders) obj;
        return (Objects.equals(headers, rhs.headers));
    }

    @Override
    public String toString()
    {
        return "EventHeaders [headers=" + headers + "]";
    }

}
