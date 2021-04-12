//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model.catalog.instance;

import org.odpi.openmetadata.connector.sas.event.model.catalog.Base;

import java.util.Map;

public class BaseInstance extends Base
{
    private int version;
    private String instanceType;
    private String definitionId;
    private int definitionRevision;
    private String name;
    private String label;
    private String description;
    private String type;
    private Map<String, Object> attributes;

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public String getInstanceType()
    {
        return instanceType;
    }

    public void setInstanceType(String instanceType)
    {
        this.instanceType = instanceType;
    }

    public String getDefinitionId()
    {
        return definitionId;
    }

    public void setDefinitionId(String definitionId)
    {
        this.definitionId = definitionId;
    }

    public int getDefinitionRevision()
    {
        return definitionRevision;
    }

    public void setDefinitionRevision(int definitionRevision)
    {
        this.definitionRevision = definitionRevision;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes)
    {
        this.attributes = attributes;
    }
}
