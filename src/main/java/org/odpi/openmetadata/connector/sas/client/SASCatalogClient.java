//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.client;

import org.odpi.openmetadata.connector.sas.event.model.catalog.instance.Instance;
import org.odpi.openmetadata.connector.sas.repository.connector.mapping.SASCatalogObject;

import java.util.List;
import java.util.Map;

public interface SASCatalogClient {
    SASCatalogObject getInstanceByGuid(String guid, String type) throws Exception;
    List<Instance> getInstancesWithParams(Map<String, String> params) throws Exception;
    List<Instance> getInstancesWithParams(Map<String, String> params, Map<String, String> attributeFilter) throws Exception;
    boolean definitionExistsByName(String defName, String type) throws Exception;
    List<SASCatalogObject> getRelationshipsByEntityGuid(String guid) throws Exception;
}
