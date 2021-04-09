//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model.catalog.definition;

import org.odpi.openmetadata.connector.sas.event.model.catalog.Base;

public class BaseDefinition extends Base
{
    private int version;
    private String definitionType;
    private String name;
    private String label;
    private String description;
    private AttributeDefinition[] attributes;
    private int revision;

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public String getDefinitionType()
    {
        return definitionType;
    }

    public void setDefinitionType(String definitionType)
    {
        this.definitionType = definitionType;
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

    public AttributeDefinition[] getAttributes()
    {
        return attributes;
    }

    public void setAttributes(AttributeDefinition[] attributes)
    {
        this.attributes = attributes;
    }

    public int getRevision()
    {
        return revision;
    }

    public void setRevision(int definitionRevision)
    {
        this.revision = definitionRevision;
    }

}
