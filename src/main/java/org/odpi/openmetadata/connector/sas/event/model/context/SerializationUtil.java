//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model.context;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.odpi.openmetadata.connector.sas.event.model.Event;

public class SerializationUtil {

    public static ObjectMapper createObjectMapperForEvent(Event<?> event)
    {
        return (event.getVersion() == 1 ? createV1ObjectMapper() : createObjectMapper());
    }

    public static ObjectMapper createObjectMapper()
    {
        ObjectMapper mapper = createMapper();

        // ensure all dates are serialized in ISO-8601 format, not as timestamp values (longs)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // register the deserializer
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Date.class, new DateDeserializer());
        module.addDeserializer(Event.class, new EventDeserializer());
        module.addSerializer(new DateSerializer());
        mapper.registerModule(module);
        return mapper;
    }

    public static ObjectMapper createV1ObjectMapper()
    {
        ObjectMapper mapper = createMapper();

        // enabled default typing for all V1 events
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        return mapper;
    }

    private static ObjectMapper createMapper()
    {
        // a custom object mapper is required here since we need to make sure that the
        // event payload is serialized properly
        ObjectMapper mapper = new ObjectMapper();

        // make sure all null values are removed from event instance
        mapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(Include.NON_NULL, Include.ALWAYS));

        // ensure new event properties don't cause serialization issues for existing consumers.
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

}
