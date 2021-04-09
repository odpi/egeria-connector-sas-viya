//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.repository.connector;

import org.odpi.openmetadata.frameworks.connectors.properties.beans.ConnectorType;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryConnectorProviderBase;

public class RepositoryConnectorProvider extends OMRSRepositoryConnectorProviderBase
{
    static final String  connectorTypeGUID = "8e333e77-c432-4c7a-b08d-30b8c1c1754b";
    static final String  connectorTypeName = "OMRS SAS Information Catalog Repository Connector";
    static final String  connectorTypeDescription = "OMRS SAS Information Catalog Repository Connector that processes events from the SAS Catalog repository stores.";

    public RepositoryConnectorProvider() {
        Class connectorClass = RepositoryConnector.class;
        super.setConnectorClassName(connectorClass.getName());
        ConnectorType connectorType = new ConnectorType();
        connectorType.setType(ConnectorType.getConnectorTypeType());
        connectorType.setGUID(connectorTypeGUID);
        connectorType.setQualifiedName(connectorTypeName);
        connectorType.setDisplayName(connectorTypeName);
        connectorType.setDescription(connectorTypeDescription);
        connectorType.setConnectorProviderClassName(this.getClass().getName());
        super.connectorTypeBean = connectorType;
    }
}