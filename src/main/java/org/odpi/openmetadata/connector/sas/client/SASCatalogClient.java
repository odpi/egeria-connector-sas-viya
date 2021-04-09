//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.client;

import org.odpi.openmetadata.connector.sas.repository.connector.mapping.SASCatalogObject;

import java.util.List;

public interface SASCatalogClient {
    SASCatalogObject getInstanceByGuid(String guid, String type) throws Exception;
    boolean definitionExistsByName(String defName, String type) throws Exception;
    List<SASCatalogObject> getRelationshipsByEntityGuid(String guid) throws Exception;
}
