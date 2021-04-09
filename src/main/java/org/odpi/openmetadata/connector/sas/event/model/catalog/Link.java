//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model.catalog;

public class Link {
    private String method;
    private String rel;
    private String href;
    private String uri;
    private String type;
    private String responseType;
    private String itemType;
    private String responseItemType;
    private String title;

    public String getMethod()
    {
        return method;
    }

    public void setMethod(String method)
    {
        this.method = method;
    }

    public String getRel()
    {
        return rel;
    }

    public void setRel(String rel)
    {
        this.rel = rel;
    }

    public String getHref()
    {
        return href;
    }

    public void setHref(String href)
    {
        this.href = href;
    }

    public String getUri()
    {
        return uri;
    }

    public void setUri(String uri)
    {
        this.uri = uri;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getResponseType()
    {
        return responseType;
    }

    public void setResponseType(String responseType)
    {
        this.responseType = responseType;
    }

    public String getItemType()
    {
        return itemType;
    }

    public void setItemType(String itemType)
    {
        this.itemType = itemType;
    }

    public String getResponseItemType()
    {
        return responseItemType;
    }

    public void setResponseItemType(String responseItemType)
    {
        this.responseItemType = responseItemType;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }
}
