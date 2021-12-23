//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.repository.connector.mapping;

import org.odpi.openmetadata.connector.sas.event.model.catalog.CatalogType;
import org.odpi.openmetadata.connector.sas.event.model.catalog.definition.Definition;
import org.odpi.openmetadata.connector.sas.event.model.catalog.instance.Instance;

import java.util.HashMap;
import java.util.Map;

public class SASCatalogObject {
    public String guid;
    public String defId;
    public Map<String, Object> attributes;
    private Map<String, Object> instanceProperties;
    private Map<String, Object> definitionProperties;

    public SASCatalogObject() {
        instanceProperties = new HashMap<>();
        definitionProperties = new HashMap<>();
        attributes = new HashMap<>();
    }

    public Object get(String sasProperty) {
        if(sasProperty.startsWith("instance.")) {
            String propertyName = sasProperty.substring("instance.".length());
            return getInstanceProperty(propertyName);
        } else if(sasProperty.startsWith("definition.")) {
            String propertyName = sasProperty.substring("definition.".length());
            return getDefinitionProperty(propertyName);
        } else if(sasProperty.startsWith("attribute.")) {
            String propertyName = sasProperty.substring("attribute.".length());
            return getAttribute(propertyName);
        }
        return null;
    }

    public String getTypeName() {
        String typeName = (String) definitionProperties.get("name");
        if(typeName.equalsIgnoreCase("reference")) {
            typeName = "reference." + getAttribute("referencedType");
        }
        if(typeName.equalsIgnoreCase("relatedObjects")) {
            typeName = (String) instanceProperties.get("type");
        }
        return typeName;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Object getAttribute(String propertyName) {
        return attributes.get(propertyName);
    }

    public String getGuid() {
        return guid;
    }

    public void addInstanceProperty(String property, Object instanceVal) {
        instanceProperties.put(property, instanceVal);
    }

    public Object getInstanceProperty(String propertyName) {
        return instanceProperties.get(propertyName);
    }

    public void addDefinitionProperty(String property, Object definitionVal) {
        definitionProperties.put(property, definitionVal);
    }

    public Object getDefinitionProperty(String propertyName) {
        return definitionProperties.get(propertyName);
    }

    public void addInstance(Instance instance) {
        addInstanceProperty("id", instance.getId());
        addDefinitionProperty("id", instance.getDefinitionId());
        addInstanceProperty("createdBy", instance.getCreatedBy());
        addInstanceProperty("modifiedBy", instance.getModifiedBy());
        addInstanceProperty("creationTimeStamp", instance.getCreationTimeStamp());
        addInstanceProperty("modifiedTimeStamp", instance.getModifiedTimeStamp());
        addInstanceProperty("name", instance.getName());
        addInstanceProperty("label", instance.getLabel());
        addInstanceProperty("description", instance.getDescription());
        addInstanceProperty("version", instance.getVersion());

        addInstanceProperty("type", instance.getType());

        if(instance.getInstanceType().equals(CatalogType.ENTITY)) {
            addInstanceProperty("resourceId", instance.getResourceId());
        } else if(instance.getInstanceType().equals(CatalogType.RELATIONSHIP)) {
            addInstanceProperty("endpoint1Id", instance.getEndpoint1Id());
            addInstanceProperty("endpoint1Uri", instance.getEndpoint1Uri());
            addInstanceProperty("endpoint2Id", instance.getEndpoint2Id());
            addInstanceProperty("endpoint2Uri", instance.getEndpoint2Uri());
        }

        attributes.putAll(instance.getAttributes());
    }

    public void addDefinition(Definition definition) {
        addDefinitionProperty("id", definition.getId());
        addDefinitionProperty("label", definition.getLabel());
        addDefinitionProperty("description", definition.getDescription());
        addDefinitionProperty("name", definition.getName());
        addDefinitionProperty("createdBy", definition.getCreatedBy());
        addDefinitionProperty("modifiedBy", definition.getModifiedBy());
        addDefinitionProperty("creationTimeStamp", definition.getCreationTimeStamp());
        addDefinitionProperty("modifiedTimeStamp", definition.getModifiedTimeStamp());
        addDefinitionProperty("version", definition.getVersion());

        addDefinitionProperty("baseType", definition.getBaseType());

        if(definition.getDefinitionType().equals(CatalogType.ENTITY)) {
            addDefinitionProperty("platformTypeName", definition.getPlatformTypeName());
        } else if(definition.getDefinitionType().equals(CatalogType.RELATIONSHIP)) {
            addDefinitionProperty("category", definition.getCategory());

            Definition.EndpointDefinition endpointDefinition1 = definition.getEndpointDefinition1();
            addDefinitionProperty("endpoint1Name", endpointDefinition1.getName());
            addDefinitionProperty("endpoint1Label", endpointDefinition1.getLabel());
            addDefinitionProperty("endpoint1Description", endpointDefinition1.getDescription());
            addDefinitionProperty("endpoint1Cardinality", endpointDefinition1.getCardinality());
            addDefinitionProperty("endpoint1ElementType", endpointDefinition1.getElementType());

            Definition.EndpointDefinition endpointDefinition2 = definition.getEndpointDefinition2();
            addDefinitionProperty("endpoint2Name", endpointDefinition2.getName());
            addDefinitionProperty("endpoint2Label", endpointDefinition2.getLabel());
            addDefinitionProperty("endpoint2Description", endpointDefinition2.getDescription());
            addDefinitionProperty("endpoint2Cardinality", endpointDefinition2.getCardinality());
            addDefinitionProperty("endpoint2ElementType", endpointDefinition2.getElementType());
        }
    }
}
