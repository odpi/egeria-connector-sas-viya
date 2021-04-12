//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.repository.connector.stores;

import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.AttributeTypeDef;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.PrimitiveDef;

import java.util.ArrayList;
import java.util.List;

//TODO: Add support for AttributeTypeDefs
public class AttributeTypeDefStore {

    private List<AttributeTypeDef> attributeTypeDefs;

    public AttributeTypeDefStore() {
        this.attributeTypeDefs = new ArrayList<>();
        // Necessary to prevent exception in Egeria UI
        // If attributeTypeDefs is empty, it's set to null, causing a NullPointer
        this.attributeTypeDefs.add(new PrimitiveDef());
    }

    public List<AttributeTypeDef> getAllAttributeTypeDefs() {
        return this.attributeTypeDefs;
    }
}
