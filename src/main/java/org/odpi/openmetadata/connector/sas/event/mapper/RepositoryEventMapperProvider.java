//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.mapper;

import org.odpi.openmetadata.frameworks.connectors.properties.beans.ConnectorType;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryConnectorProviderBase;

/**
 * In the Open Connector Framework (OCF), a ConnectorProvider is a factory for a specific type of connector.
 * The RepositoryEventMapperProvider is the connector provider for the RepositoryEventMapperProvider.
 * It extends OMRSRepositoryEventMapperProviderBase which in turn extends the OCF ConnectorProviderBase.
 * ConnectorProviderBase supports the creation of connector instances.
 *
 * The RepositoryEventMapperProvider must initialize ConnectorProviderBase with the Java class
 * name of the OMRS Connector implementation (by calling super.setConnectorClassName(className)).
 * Then the connector provider will work.
 */
public class RepositoryEventMapperProvider extends OMRSRepositoryConnectorProviderBase {

    static final String CONNECTOR_TYPE_GUID = "e4b21e4f-d147-4f63-95bb-9401099928ff";
    static final String CONNECTOR_TYPE_NAME = "OMRS SAS Catalog Event Mapper Connector";
    static final String CONNECTOR_TYPE_DESC = "OMRS SAS Catalog Event Mapper Connector that processes events from the SAS Catalog repository store.";

    /**
     * Constructor used to initialize the ConnectorProviderBase with the Java class name of the specific
     * OMRS Connector implementation.
     */
    public RepositoryEventMapperProvider() {
        Class<?> connectorClass = RepositoryEventMapper.class;
        super.setConnectorClassName(connectorClass.getName());
        ConnectorType connectorType = new ConnectorType();
        connectorType.setType(ConnectorType.getConnectorTypeType());
        connectorType.setGUID(CONNECTOR_TYPE_GUID);
        connectorType.setQualifiedName(CONNECTOR_TYPE_NAME);
        connectorType.setDisplayName(CONNECTOR_TYPE_NAME);
        connectorType.setDescription(CONNECTOR_TYPE_DESC);
        connectorType.setConnectorProviderClassName(this.getClass().getName());
        super.setConnectorTypeProperties(connectorType);
    }

}
