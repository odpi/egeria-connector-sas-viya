//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.repository.connector;

import org.odpi.openmetadata.connector.sas.auditlog.ErrorCode;
import org.odpi.openmetadata.connector.sas.client.SASCatalogClient;
import org.odpi.openmetadata.connector.sas.client.SASCatalogRestClient;
import org.odpi.openmetadata.connector.sas.repository.connector.mapping.SASCatalogObject;
import org.odpi.openmetadata.frameworks.connectors.ffdc.ConnectorCheckedException;
import org.odpi.openmetadata.frameworks.connectors.properties.ConnectionProperties;
import org.odpi.openmetadata.frameworks.connectors.properties.EndpointProperties;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.OMRSMetadataCollection;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDefCategory;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryConnector;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.RepositoryErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RepositoryConnector extends OMRSRepositoryConnector
{
    private static RepositoryConnector instance;
    private boolean successfulInit = false;
    private SASCatalogClient sasCatalogClient;
    private static final Logger log = LoggerFactory.getLogger(RepositoryConnector.class);
    public static final String EP_ENTITY = "/catalog/instances/";
    private String url;

    public RepositoryConnector() {
        // default constructor
        this(null);
    }

    public RepositoryConnector(SASCatalogClient client)
    {
        this.sasCatalogClient = client;
        instance = this;
    }

    public static RepositoryConnector getInstance() {
        return instance;
    }

    @Override
    public void initialize(String connectorInstanceId, ConnectionProperties connectionProperties) {
        super.initialize(connectorInstanceId, connectionProperties);
        final String methodName = "initialize";
    }

    @Override
    public void start() throws ConnectorCheckedException {
        super.start();
        final String methodName = "start";
        try {
            if (metadataCollection == null) {
                connectToCatalog(methodName);
            }
        } catch (Exception e) {
            throw new ConnectorCheckedException("Could not create a catalog client", null);
        }
    }

    public String getBaseURL() {
        return this.url;
    }

    private void connectToCatalog(String methodName) throws Exception {
        if(this.sasCatalogClient == null) {
            EndpointProperties endpointProperties = connectionProperties.getEndpoint();
            if (endpointProperties == null) {
                raiseConnectorCheckedException(ErrorCode.REST_CLIENT_FAILURE, methodName, null, "null");
            } else {
                this.url = endpointProperties.getProtocol() + "://" + endpointProperties.getAddress();
                this.sasCatalogClient = new SASCatalogRestClient(this.url, this.securedProperties.get("userId"),
                        this.securedProperties.get("password"));
            }
        }
        metadataCollection = new MetadataCollection(this,
                serverName,
                repositoryHelper,
                repositoryValidator,
                metadataCollectionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OMRSMetadataCollection getMetadataCollection() throws RepositoryErrorException {
        final String methodName = "getMetadataCollection";
        if (metadataCollection == null) {
            // If the metadata collection has not yet been created, attempt to create it now
            try {
                connectToCatalog(methodName);
            } catch (Exception e) {
                raiseRepositoryErrorException(ErrorCode.REST_CLIENT_FAILURE, methodName, e, getServerName());
            }
        }
        return super.getMetadataCollection();
    }

    public SASCatalogObject getEntityByGUID(String guid) {
        try {
            return sasCatalogClient.getInstanceByGuid(guid, "entity");
        } catch (Exception e) {
            log.error("Could not fetch entity with guid: " + guid);
            e.printStackTrace();
        }
        return null;
    }

    public SASCatalogObject getRelationshipByGUID(String guid) {
        try {
            return sasCatalogClient.getInstanceByGuid(guid, "relationship");
        } catch (Exception e) {
            log.error("Could not fetch entity with guid: " + guid);
            e.printStackTrace();
        }
        return null;
    }

    public List<SASCatalogObject> getRelationshipsForEntity(String guid) {
        try {
            return sasCatalogClient.getRelationshipsByEntityGuid(guid);
        } catch (Exception e) {
            log.error("Could not fetch relationships for entity with guid: " + guid);
            e.printStackTrace();
        }
        return null;
    }

    public boolean typeDefExistsByName(String omrsTypeDefName, TypeDefCategory typeDefCategory) {
        String typeName;
        switch (typeDefCategory) {
            case RELATIONSHIP_DEF:
                typeName = "relationship";
                break;
            case CLASSIFICATION_DEF:
                typeName = "classification";
                break;
            case ENTITY_DEF:
            case UNKNOWN_DEF:
            default:
                typeName = "entity";
        }

        try {
            return sasCatalogClient.definitionExistsByName(omrsTypeDefName, typeName);
        } catch (Exception e) {
            System.out.println("Could not connect to catalog");
            return false;
        }
    }

    /**
     * Throws a ConnectorCheckedException using the provided parameters.
     * @param errorCode the error code for the exception
     * @param methodName the name of the method throwing the exception
     * @param cause the underlying cause of the exception (if any, null otherwise)
     * @param params any parameters for formatting the error message
     * @throws ConnectorCheckedException always
     */
    private void raiseConnectorCheckedException(ErrorCode errorCode, String methodName, Throwable cause, String ...params) throws ConnectorCheckedException {
        if (cause == null) {
            throw new ConnectorCheckedException(errorCode.getMessageDefinition(params),
                    this.getClass().getName(),
                    methodName);
        } else {
            throw new ConnectorCheckedException(errorCode.getMessageDefinition(params),
                    this.getClass().getName(),
                    methodName,
                    cause);
        }
    }

    /**
     * Throws a RepositoryErrorException using the provided parameters.
     * @param errorCode the error code for the exception
     * @param methodName the name of the method throwing the exception
     * @param cause the underlying cause of the exception (or null if none)
     * @param params any parameters for formatting the error message
     * @throws RepositoryErrorException always
     */
    private void raiseRepositoryErrorException(ErrorCode errorCode, String methodName, Throwable cause, String ...params) throws RepositoryErrorException {
        if (cause == null) {
            throw new RepositoryErrorException(errorCode.getMessageDefinition(params),
                    this.getClass().getName(),
                    methodName);
        } else {
            throw new RepositoryErrorException(errorCode.getMessageDefinition(params),
                    this.getClass().getName(),
                    methodName,
                    cause);
        }
    }
}