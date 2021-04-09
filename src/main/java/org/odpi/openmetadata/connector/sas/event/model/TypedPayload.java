//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model;

/**
 * Interface for associating a type field with a given payload.  This type field is not
 * designed to be included as part of the payload, but rather it is used when
 * constructing the {@link Event} instance the payload is contained within.
 */
public interface TypedPayload {

    /**
     * Returns the type of event the payload represents.
     * @return event type
     */
    default public String getEventType()
    {
        return null;
    }

    /**
     * Returns the type of the payload.
     * @return payload type
     */
    default public String getPayloadType()
    {
        return null;
    }
}
