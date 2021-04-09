//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model.catalog.instance;

public class Instance extends BaseInstance
{
    // Entity specific
    private String resourceId;

    // Relationship specific
    private String endpoint1Id;
    private String endpoint1Uri;
    private String endpoint2Id;
    private String endpoint2Uri;

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getEndpoint1Id() {
        return endpoint1Id;
    }

    public void setEndpoint1Id(String endpoint1Id) {
        this.endpoint1Id = endpoint1Id;
    }

    public String getEndpoint1Uri() {
        return endpoint1Uri;
    }

    public void setEndpoint1Uri(String endpoint1Uri) {
        this.endpoint1Uri = endpoint1Uri;
    }

    public String getEndpoint2Id() {
        return endpoint2Id;
    }

    public void setEndpoint2Id(String endpoint2Id) {
        this.endpoint2Id = endpoint2Id;
    }

    public String getEndpoint2Uri() {
        return endpoint2Uri;
    }

    public void setEndpoint2Uri(String endpoint2Uri) {
        this.endpoint2Uri = endpoint2Uri;
    }
}
