//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model.context;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Provide JSON serialization of date values to SAS standard timestamp
 * representation,"2015-10-02T15:30:00.00Z".
 */
public class DateDeserializer extends JsonDeserializer<Date>
{
    @Override
    public Date deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        String text = jp.getText();
        if(text == null) return null;
        try
        {
            return Timestamp.parseTimestampAsDate(text);
        }
        catch (IllegalArgumentException ex)
        {
            // See if this is a long.  If so, then create the Date assuming
            // it was serialized using the default serialization.
            try
            {
                long value = Long.valueOf(text);
                return new Date(value);
            }
            catch(NumberFormatException e)
            {}
            throw new JsonParseException("Error parsing date '" + text + "'", null, ex);
        }
    }

}
