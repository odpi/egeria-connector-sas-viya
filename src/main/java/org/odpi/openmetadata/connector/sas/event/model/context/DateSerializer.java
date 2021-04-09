//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model.context;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Date;

/**
 * This class serializes Date objects into a JSON stream in ISO8601 format
 * "2015-10-02T15:30:00.00Z"
 * using the Z suffix to represent GMT, rather than +0000.
 */
public class DateSerializer extends JsonSerializer<Date>
{
    public DateSerializer()
    {
        super();
    }

    @Override
    public Class<Date> handledType()
    {
        return Date.class;
    }

    @Override
    public void serialize(Date value, JsonGenerator generator, SerializerProvider provider) throws IOException,
            JsonProcessingException
    {
        generator.writeString(Timestamp.timestamp(value));
    }
}