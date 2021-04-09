//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.model.catalog.definition;

import org.odpi.openmetadata.connector.sas.event.model.catalog.definition.BaseDefinition;

public class Definition extends BaseDefinition
{
    private String baseType;

    //Entity specific
    private String platformTypeName;

    // Relationship specific
    private String category;
    private EndpointDefinition endpointDefinition1;
    private EndpointDefinition endpointDefinition2;

    public String getBaseType() {
        return baseType;
    }

    public void setBaseType(String baseType) {
        this.baseType = baseType;
    }

    public String getPlatformTypeName() {
        return platformTypeName;
    }

    public void setPlatformTypeName(String platformTypeName) {
        this.platformTypeName = platformTypeName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public EndpointDefinition getEndpointDefinition1() {
        return endpointDefinition1;
    }

    public void setEndpointDefinition1(EndpointDefinition endpointDefinition1) {
        this.endpointDefinition1 = endpointDefinition1;
    }

    public EndpointDefinition getEndpointDefinition2() {
        return endpointDefinition2;
    }

    public void setEndpointDefinition2(EndpointDefinition endpointDefinition2) {
        this.endpointDefinition2 = endpointDefinition2;
    }

    public static class EndpointDefinition {
        private String name;
        private String label;
        private String description;
        private String cardinality;
        private String elementType;
        private boolean container;
        private boolean ordered;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCardinality() {
            return cardinality;
        }

        public void setCardinality(String cardinality) {
            this.cardinality = cardinality;
        }

        public String getElementType() {
            return elementType;
        }

        public void setElementType(String elementType) {
            this.elementType = elementType;
        }

        public boolean isContainer() {
            return container;
        }

        public void setContainer(boolean container) {
            this.container = container;
        }

        public boolean isOrdered() {
            return ordered;
        }

        public void setOrdered(boolean ordered) {
            this.ordered = ordered;
        }
    }
}
