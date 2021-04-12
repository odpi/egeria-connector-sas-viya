//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model.context;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.odpi.openmetadata.connector.sas.event.model.Event;

import org.odpi.openmetadata.connector.sas.event.model.EventBuilder;
import org.odpi.openmetadata.connector.sas.event.model.EventHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;

/**
 * Custom json deserializer used for processing the contents of an event
 */
public class EventDeserializer extends StdDeserializer<Event<?>> {

    static final String EVENT_VERSION = "version";
    static final String EVENT_HEADERS = "headers";
    static final String EVENT_PAYLOAD = "payload";
    static final String EVENT_PAYLOAD_TYPE = "payloadType";
    static final String EVENT_ID = "id";
    static final String EVENT_USER = "user";
    static final String EVENT_TIMESTAMP = "timeStamp";
    static final String EVENT_TYPE = "type";

    static final String VERSION_1 = "1";

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDeserializer.class);

    private PayloadTypeRegistry registry;
    private ObjectMapper v1Mapper;

    public EventDeserializer()
    {
        super(Event.class);
        registry = PayloadTypeRegistry.getInstance();
    }

    @Override
    public Event<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException
    {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode root = mapper.readTree(jp);

        // deserialize the payload using the registered class if possible
        String payloadType = null;
        Object payload = null;
        JsonNode payloadNode = root.get(EVENT_PAYLOAD);
        JsonNode payloadTypeNode = root.get(EVENT_PAYLOAD_TYPE);
        if (payloadTypeNode != null)
        {
            payloadType = root.get(EVENT_PAYLOAD_TYPE).asText();
            Class<?> payloadClass = registry.getTypeClass(payloadType.split(";")[0]);
            if (payloadClass != null)
            {
                // deserialize using the registered class
                payload = mapper.treeToValue(payloadNode, payloadClass);
            }
        }

        if (payload == null && payloadNode != null)
        {
            /*
             * If a payload was found, but we don't have a valid registered type, we really don't know how
             * to deserialize it.  We need to fall back to treating it as plain text.
             *
             * This can happen if we've received an event but the event payload's POJO/representation hasn't
             * been registered yet - without this registration we don't know what class to map it to.
             */
            payload = payloadNode.asText();
        }

        if (payload == null || "".equals(payload))
        {
            // if we still don't have a payload, check the version field in order to determine if a back-level
            // deserializer should be used
            JsonNode versionNode = root.get(EVENT_VERSION);
            String version = (versionNode == null ? "unknown" : versionNode.asText());
            if (versionNode == null || VERSION_1.equalsIgnoreCase(versionNode.asText()))
            {
                // a missing version or version 1 event
                return deserializePreviousVersionEvent(root);
            }
            else
            {
                // a version 2 event - we should only hit here if we're dealing with an event
                // that isn't properly formed or if the payloadType value is unknown to the consumer
                String type = (root.get(EVENT_TYPE) == null ? "unknown" : root.get(EVENT_TYPE).asText());
                LOGGER.debug("Unable to deserialize event contents; version='{}', type='{}', payloadType='{}'",
                        version, type, payloadType);
                return null;
            }
        }

        // build the event
        EventBuilder<?> builder = EventBuilder.withPayload(payload)
                .payloadType(payloadType)
                .headers(deserializeHeaders(mapper, root))
                .idIfAbsent(deserializeTextProperty(root, EVENT_ID))
                .timeStamp(deserializeDateProperty(root, EVENT_TIMESTAMP))
                .user(deserializeTextProperty(root, EVENT_USER))
                .type(deserializeTextProperty(root, EVENT_TYPE));
        return builder.build();
    }

    private EventHeaders deserializeHeaders(ObjectMapper mapper, JsonNode root) throws JsonProcessingException
    {
        JsonNode node = root.get(EVENT_HEADERS);
        return (node == null ? null : mapper.treeToValue(node, EventHeaders.class));
    }

    private String deserializeTextProperty(JsonNode root, String property)
    {
        JsonNode node = root.get(property);
        return (node == null ? null : node.asText());
    }

    private Date deserializeDateProperty(JsonNode root, String property)
    {
        JsonNode node = root.get(property);
        if (node != null)
        {
            String value = node.asText();
            if (value != null && !value.isEmpty())
            {
                try
                {
                    return Timestamp.parseTimestampAsDate(value);
                }
                catch (IllegalArgumentException e) {}
            }
        }
        return null;
    }

    private Event<?> deserializePreviousVersionEvent(JsonNode root) throws JsonProcessingException, IOException
    {
        // verify that we're dealing with an older event version
        JsonNode eventNode = null;
        if (root instanceof ObjectNode)
        {
            eventNode = root;
        }
        else if (root instanceof ArrayNode)
        {
            ArrayNode an = (ArrayNode) root;
            JsonNode first = an.get(0);
            if (first != null && first.asText().equals(Event.class.getName()))
            {
                eventNode = an.get(1);
            }
        }

        if (eventNode == null)
        {
            // incompatible contents
            LOGGER.debug("Unable to deserialize event - incompatible event contents");
            return null;
        }

        // fetch the proper object mapper used to deserialize the event
        try
        {
            ObjectMapper mapper = getV1ObjectMapper();
            return mapper.treeToValue(root, Event.class);
        }
        catch (JsonMappingException e)
        {
            String version = (eventNode.get(EVENT_VERSION) == null ? "unknown" : eventNode.get(EVENT_VERSION).asText());
            String type = (eventNode.get(EVENT_TYPE) == null ? "unknown" : eventNode.get(EVENT_TYPE).asText());
            LOGGER.debug("Unable to deserialize event contents; version='{}', type='{}', payloadType='{}'",
                    version, type, null);
            LOGGER.debug("Deserialization error:", e);
            return null;
        }
    }

    private ObjectMapper getV1ObjectMapper()
    {
        if (v1Mapper == null)
        {
            v1Mapper = new ObjectMapper();
            v1Mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // this is required as it tells the parser to use the type information (i.e. class names)
            // within the json to deserialize it properly
            v1Mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        }
        return v1Mapper;
    }

}