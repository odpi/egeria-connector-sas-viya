//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.odpi.openmetadata.connector.sas.event.model.TypedPayload;
import org.odpi.openmetadata.connector.sas.event.model.catalog.definition.Definition;
import org.odpi.openmetadata.connector.sas.event.model.catalog.instance.Instance;

public class CatalogEventPayload implements TypedPayload {
    public static final String PAYLOAD_TYPE = "application/vnd.sas.catalog.event";

    private String action;
    private String actionState;
    private String objectType;
    private String operation;
    private Instance instance;
    private Definition definition;

    @JsonIgnore
    public static final String TYPE_INSTANCE = "instance";
    @JsonIgnore
    public static final String TYPE_DEFINITION = "definition";

    @JsonIgnore
    public String getPayloadType()
    {
        return PAYLOAD_TYPE;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActionState() {
        return actionState;
    }

    public void setActionState(String actionState) {
        this.actionState = actionState;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String type) {
        this.objectType = type;
    }

    public String getType() {
        return getObjectType();
    }

    public void setType(String type) {
        setObjectType(type);
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public Definition getDefinition() {
        return definition;
    }

    public void setDefinition(Definition definition) {
        this.definition = definition;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

}
