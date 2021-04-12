//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains a registry of all loaded payload types.  Used internally by the event framework
 * when deserializing the contents of an event.
 */
public class PayloadTypeRegistry {

    private static final PayloadTypeRegistry instance = new PayloadTypeRegistry();

    private Map<String, Class<?>> cache;

    private PayloadTypeRegistry()
    {
        cache = new HashMap<>();
    }

    public static PayloadTypeRegistry getInstance()
    {
        return instance;
    }

    public void registerType(String type, Class<?> clazz)
    {
        cache.put(type,clazz);
    }

    public Class<?> getTypeClass(String type)
    {
        return cache.get(type);
    }

    public boolean isRegisteredType(String type)
    {
        return (type == null ? false : cache.containsKey(type));
    }

    public void clear()
    {
        cache.clear();
    }

}
