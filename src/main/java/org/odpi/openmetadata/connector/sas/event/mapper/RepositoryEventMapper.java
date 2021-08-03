//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.event.mapper;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.odpi.openmetadata.connector.sas.auditlog.AuditCode;
import org.odpi.openmetadata.connector.sas.auditlog.ErrorCode;
import org.odpi.openmetadata.connector.sas.repository.connector.MetadataCollection;
import org.odpi.openmetadata.connector.sas.repository.connector.RepositoryConnector;
import org.odpi.openmetadata.connector.sas.repository.connector.mapping.EntityMappingSASCatalog2OMRS;
import org.odpi.openmetadata.connector.sas.repository.connector.mapping.RelationshipMapping;
import org.odpi.openmetadata.connector.sas.repository.connector.mapping.SASCatalogObject;
import org.odpi.openmetadata.connector.sas.repository.connector.model.SASCatalogGuid;
import org.odpi.openmetadata.connector.sas.repository.connector.stores.TypeDefStore;
import org.apache.commons.lang3.StringUtils;
import org.odpi.openmetadata.connector.sas.event.model.catalog.CatalogEventPayload;
import org.odpi.openmetadata.connector.sas.event.model.catalog.CatalogType;
import org.odpi.openmetadata.connector.sas.event.model.catalog.definition.Definition;
import org.odpi.openmetadata.connector.sas.event.model.Event;
import org.odpi.openmetadata.connector.sas.event.model.catalog.instance.Instance;
import org.odpi.openmetadata.frameworks.connectors.ffdc.ConnectorCheckedException;
import org.odpi.openmetadata.frameworks.connectors.properties.ConnectionProperties;
import org.odpi.openmetadata.frameworks.connectors.properties.EndpointProperties;
import org.odpi.openmetadata.repositoryservices.connectors.openmetadatatopic.OpenMetadataTopicListener;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.Relationship;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.AttributeTypeDef;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDef;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryConnector;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryeventmapper.OMRSRepositoryEventMapperBase;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.RepositoryErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RepositoryEventMapper supports the event mapper function for SAS Catalog
 * when used as an open metadata repository.
 */
public class RepositoryEventMapper extends OMRSRepositoryEventMapperBase
        implements OpenMetadataTopicListener {

    private final static String EXCHANGE_NAME = "sas.application";

    private static final String CREATE = "created";
    private static final String UPDATE = "modified";
    private static final String DELETE = "removed";

    private static final Logger log = LoggerFactory.getLogger(RepositoryEventMapper.class);

    private String sourceName;
    private RepositoryConnector catalogOMRSRepositoryConnector;
    private MetadataCollection catalogOMRSMetadataCollection;
    private TypeDefStore typeDefStore;
    private String metadataCollectionId;
    private String originatorServerName;
    private String originatorServerType;

    private Channel channel;
    private String queueName;

    private ObjectMapper mapper;

    /**
     * Default constructor
     */
    public RepositoryEventMapper() {
        super();
        this.sourceName = "RepositoryEventMapper";
    }

    @Override
    public void initialize(String repositoryEventMapperName,
                           OMRSRepositoryConnector repositoryConnector) {
        super.initialize(repositoryEventMapperName, repositoryConnector);

        final String methodName = "initialize";

        auditLog.logMessage(methodName, AuditCode.EVENT_MAPPER_INITIALIZING.getMessageDefinition());

        this.mapper = new ObjectMapper();
        this.mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

        try {
            log.debug("Set up connection factory for RabbitMQ");
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.useSslProtocol();
            ConnectionProperties connProperties = this.connectionProperties;
            if (connProperties != null) {
                EndpointProperties connEndpoint = connProperties.getEndpoint();
                Map<String,Object> cfgProperties = connProperties.getConfigurationProperties();
                if (connEndpoint != null) {
                    String host = "";
                    String portAsString = "";
                    String address = connEndpoint.getAddress();
                    if (StringUtils.isNotEmpty(address)) {
                        if (address.contains(":")) {
                            String[] addressParts = address.split(":", 2);
                            host = addressParts[0];
                            portAsString = addressParts[1];
                        } else {
                            host = address;
                        }
                    }
                    if (StringUtils.isNotEmpty(host)) {
                        // RabbitMQ host was configured, so set it in ConnectionFactory
                        log.debug("Setting RabbitMQ host to: " + host);
                        connectionFactory.setHost(host);
                    }
                    if (StringUtils.isNotEmpty(portAsString)) {
                        try {
                            int port = Integer.valueOf(portAsString);
                            // RabbitMQ port was configured, so set it in ConnectionFactory
                            log.debug("Setting RabbitMQ port to: " + portAsString);
                            connectionFactory.setPort(port);
                        }
                        catch (NumberFormatException nfe) {
                            log.error("Could not convert '{}' to a port number.  Default port will be used.", portAsString);
                        }
                    }

                    if (cfgProperties != null) {
                        String username = (String)cfgProperties.getOrDefault("username", "");
                        String password = (String)cfgProperties.getOrDefault("password", "");
                        if (StringUtils.isNotEmpty(username)) {
                            // RabbitMQ username was configured, so set it in ConnectionFactory
                            log.debug("Setting RabbitMQ username");
                            connectionFactory.setUsername(username);
                        }
                        if (StringUtils.isNotEmpty(password)) {
                            // RabbitMQ password was configured, so set it in ConnectionFactory
                            log.debug("Setting RabbitMQ password");
                            connectionFactory.setPassword(password);
                        }
                    }
                }
            }
            log.debug("Create RabbitMQ connection");
            Connection connection = connectionFactory.newConnection();
            log.debug("Create RabbitMQ channel");
            channel = connection.createChannel();
            log.debug("Declare RabbitMQ exchange: " + EXCHANGE_NAME);
            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
            log.debug("Declare RabbitMQ queue");
            queueName = channel.queueDeclare().getQueue();
            log.debug("Bind RabbitMQ queue: {}", queueName);
            channel.queueBind(queueName, EXCHANGE_NAME,"application.integration.catalog.change.*.success");
            log.debug("Finished establishment of RabbitMQ connection");
        }
        catch (Exception e) {
            log.error("Failed to initialize RabbitMQ connection " + e.getMessage());
        }
        auditLog.logMessage(methodName, AuditCode.EVENT_MAPPER_INITIALIZED.getMessageDefinition(repositoryConnector.getServerName()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws ConnectorCheckedException {

        super.start();

        final String methodName = "start";

        auditLog.logMessage(methodName, AuditCode.EVENT_MAPPER_STARTING.getMessageDefinition());

        if ( !(repositoryConnector instanceof RepositoryConnector) ) {
            raiseConnectorCheckedException(ErrorCode.EVENT_MAPPER_IMPROPERLY_INITIALIZED, methodName, null, repositoryConnector.getServerName());
        }
        this.catalogOMRSRepositoryConnector = (RepositoryConnector) this.repositoryConnector;

        try {
            this.catalogOMRSMetadataCollection = (MetadataCollection) catalogOMRSRepositoryConnector.getMetadataCollection();
        } catch (RepositoryErrorException e) {
            raiseConnectorCheckedException(ErrorCode.REST_CLIENT_FAILURE, methodName, e, catalogOMRSRepositoryConnector.getServerName());
        }
        this.typeDefStore = catalogOMRSMetadataCollection.getTypeDefStore();
        catalogOMRSMetadataCollection.setEventMapper(this);
        this.metadataCollectionId = catalogOMRSRepositoryConnector.getMetadataCollectionId();
        this.originatorServerName = catalogOMRSRepositoryConnector.getServerName();
        this.originatorServerType = catalogOMRSRepositoryConnector.getServerType();

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            log.debug("SAS Catalog Event received: '{}':'{}'", delivery.getEnvelope().getRoutingKey(), message);
            try {
                Event<CatalogEventPayload> event = this.mapper.readValue(message, Event.class);
                String eventPayload = event.getPayloadAsString();
                try {
                    processEvent(eventPayload);
                } catch (Exception e) {
                    log.error("Could not process event payload.", e);
                    return;
                }
            } catch (IOException e) {
                log.warn("Could not parse event payload. Reason: {}", e.getLocalizedMessage());
                return;
            }
        };

        try {
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
        }
        catch (Exception e) {
            log.error("RabbitMQ basic consumer failed to start", e);
        }
    }


    /**
     * Method to pass an event received on topic.
     *
     * @param event inbound event
     */
    @Override
    public void processEvent(String event) {
        log.info("Processing event: {}", event);
        CatalogEventPayload eventPayload;
        try {
            eventPayload = this.mapper.readValue(event, CatalogEventPayload.class);
        } catch (IOException e) {
            log.warn("Could not parse event payload", e);
            return;
        }
        log.info("Received integration event for Catalog operation: {}", eventPayload.getOperation());

        // Convert event payload into SASCatalogObject that rest of application uses
        SASCatalogObject catalogObject = new SASCatalogObject();
        String type;

        if(eventPayload.getType().equals(CatalogEventPayload.TYPE_INSTANCE)) {
            Instance instance = eventPayload.getInstance();
            Definition definition = eventPayload.getDefinition();
            type = instance.getInstanceType();
            catalogObject.guid = instance.getId();
            catalogObject.defId = instance.getDefinitionId();
            catalogObject.addInstance(instance);
            catalogObject.addDefinition(definition);
        } else if(eventPayload.getType().equals(CatalogEventPayload.TYPE_DEFINITION)) {
            Definition definition = eventPayload.getDefinition();
            type = definition.getDefinitionType();
            catalogObject.guid = definition.getId();
            catalogObject.defId = definition.getId();
            catalogObject.addDefinition(definition);
        } else {
            log.warn("Invalid catalog object type: " + eventPayload.getType());
            return;
        }

        if(eventPayload.getOperation().startsWith(CREATE) && type.equals(CatalogType.ENTITY)) {
            processNewEntity(catalogObject);
        } else if(eventPayload.getOperation().startsWith(UPDATE) && type.equals(CatalogType.ENTITY)) {
            processUpdatedEntity(catalogObject);
        } else if(eventPayload.getOperation().startsWith(DELETE) && type.equals(CatalogType.ENTITY)) {
            processRemovedEntity(catalogObject);
        } else if(eventPayload.getOperation().startsWith(CREATE) && type.equals(CatalogType.RELATIONSHIP)) {
            processNewRelationship(catalogObject);
        } else if(eventPayload.getOperation().startsWith(UPDATE) && type.equals(CatalogType.RELATIONSHIP)) {
            processUpdatedRelationship(catalogObject);
        } else if(eventPayload.getOperation().startsWith(DELETE) && type.equals(CatalogType.RELATIONSHIP)) {
            processRemovedRelationship(catalogObject);
        } else {
            log.info("Event processing does not support operation, {}, for type, {}", eventPayload.getOperation(), type);
        }

    }

    /**
     * Processes and sends an OMRS event for the new Catalog entity.
     *
     * @param entity the new Catalog entity information
     */
    private void processNewEntity(SASCatalogObject entity) {
        // Send an event for every entity: normal and generated
        String sasTypeName = entity.getTypeName();
        Map<String, String> omrsTypesByPrefix = typeDefStore.getAllMappedOMRSTypeDefNames(sasTypeName);
        if (omrsTypesByPrefix == null) {
            log.info("No mappings found while processing new SAS entity with type name: {}", sasTypeName);
            return;
        }
        for (String prefix : omrsTypesByPrefix.keySet()) {
            EntityDetail entityDetail = getMappedEntity(entity, prefix);
            if (entityDetail != null) {
                repositoryEventProcessor.processNewEntityEvent(
                        sourceName,
                        metadataCollectionId,
                        originatorServerName,
                        originatorServerType,
                        localOrganizationName,
                        entityDetail
                );
                if (prefix != null) {
                    List<Relationship> generatedRelationships = getGeneratedRelationshipsForEntity(entity, entityDetail);
                    for (Relationship generatedRelationship : generatedRelationships) {
                        repositoryEventProcessor.processNewRelationshipEvent(
                                sourceName,
                                metadataCollectionId,
                                originatorServerName,
                                originatorServerType,
                                localOrganizationName,
                                generatedRelationship
                        );
                    }
                }
            }
        }
    }

    /**
     * Processes and sends an OMRS event for the updated Catalog entity.
     *
     * @param updatedEntity the updated Catalog entity information
     */
    private void processUpdatedEntity(SASCatalogObject updatedEntity) {
        // Send an event for every entity: normal and generated
        String sasTypeName = updatedEntity.getTypeName();
        Map<String, String> omrsTypesByPrefix = typeDefStore.getAllMappedOMRSTypeDefNames(sasTypeName);
        if (omrsTypesByPrefix == null) {
            log.info("No mappings found while processing updated SAS entity with type name: {}", sasTypeName);
            return;
        }
        for (String prefix : omrsTypesByPrefix.keySet()) {
            EntityDetail entityDetail = getMappedEntity(updatedEntity, prefix);
            if (entityDetail != null) {
                // TODO: find a way to pull back the old version to send in the update event
                repositoryEventProcessor.processUpdatedEntityEvent(
                        sourceName,
                        metadataCollectionId,
                        originatorServerName,
                        originatorServerType,
                        localOrganizationName,
                        null,
                        entityDetail
                );
                if (prefix != null) {
                    List<Relationship> generatedRelationships = getGeneratedRelationshipsForEntity(updatedEntity, entityDetail);
                    for (Relationship generatedRelationship : generatedRelationships) {
                        // TODO: find a way to pull back the old version to send in the update event
                        repositoryEventProcessor.processUpdatedRelationshipEvent(
                                sourceName,
                                metadataCollectionId,
                                originatorServerName,
                                originatorServerType,
                                localOrganizationName,
                                null,
                                generatedRelationship
                        );
                    }
                }
            }
        }
    }

    /**
     * Processes and sends an OMRS event for the removed Catalog entity.
     *
     * @param entity the deleted Catalog entity information
     */
    private void processRemovedEntity(SASCatalogObject entity) {
        // Send an event for every entity: normal and generated
        String sasTypeName = entity.getTypeName();
        Map<String, String> omrsTypesByPrefix = typeDefStore.getAllMappedOMRSTypeDefNames(sasTypeName);
        if (omrsTypesByPrefix == null) {
            log.info("No mappings found while processing removed SAS entity with type name: {}", sasTypeName);
            return;
        }
        for (String prefix : omrsTypesByPrefix.keySet()) {
            EntityDetail entityDetail = getMappedEntity(entity, prefix);
            if (entityDetail != null) {
                if (prefix != null) {
                    List<Relationship> generatedRelationships = getGeneratedRelationshipsForEntity(entity, entityDetail);
                    for (Relationship generatedRelationship : generatedRelationships) {
                        repositoryEventProcessor.processDeletedRelationshipEvent(
                                sourceName,
                                metadataCollectionId,
                                originatorServerName,
                                originatorServerType,
                                localOrganizationName,
                                generatedRelationship
                        );
                    }
                }
                repositoryEventProcessor.processDeletedEntityEvent(
                        sourceName,
                        metadataCollectionId,
                        originatorServerName,
                        originatorServerType,
                        localOrganizationName,
                        entityDetail
                );
            }
        }
    }

    /**
     * Generate any pseudo-relationships for the provided entity.
     *
     * @param entity the Atlas entity for which to generate pseudo-relationships
     * @param entityDetail the EntityDetail for which to generate pseudo-relationships
     * @return {@code List<Relationship>}
     */
    private List<Relationship> getGeneratedRelationshipsForEntity(SASCatalogObject entity,
                                                                  EntityDetail entityDetail) {

        String catalogTypeName = entity.getTypeName();
        List<Relationship> generatedRelationships = new ArrayList<>();
        Map<String, TypeDefStore.EndpointMapping> mappings = typeDefStore.getAllEndpointMappingsFromCatalogName(catalogTypeName);
        for (Map.Entry<String, TypeDefStore.EndpointMapping> entry : mappings.entrySet()) {
            String relationshipPrefix = entry.getKey();
            if (relationshipPrefix != null) {
                SASCatalogGuid sasGuid = new SASCatalogGuid(entity.getGuid(), relationshipPrefix);
                try {
                    Relationship generatedRelationship = RelationshipMapping.getSelfReferencingRelationship(
                            catalogOMRSRepositoryConnector,
                            typeDefStore,
                            sasGuid,
                            entity
                    );
                    if (generatedRelationship != null) {
                        generatedRelationships.add(generatedRelationship);
                    } else {
                        log.warn("Unable to create generated relationship with prefix {}, for entity: {}", relationshipPrefix, entityDetail.getGUID());
                    }
                } catch(RepositoryErrorException e){
                    log.error("Unable to create generated relationship with prefix {}, for entity: {}", relationshipPrefix, entityDetail.getGUID(), e);
                }
            }
        }
        return generatedRelationships;

    }

    /**
     * Retrieve the mapped OMRS entity for the provided SAS Catalog entity.
     *
     * @param entity the SAS Catalog entity to translate to OMRS
     * @return EntityDetail
     */
    private EntityDetail getMappedEntity(SASCatalogObject entity, String prefix) {
        EntityDetail result = null;
        EntityMappingSASCatalog2OMRS mapping = new EntityMappingSASCatalog2OMRS(
                catalogOMRSRepositoryConnector,
                catalogOMRSMetadataCollection.getTypeDefStore(),
                null, /*catalogOMRSMetadataCollection.getAttributeTypeDefStore(),*/
                entity,
                prefix,
                null
        );
        try {
            result = mapping.getEntityDetail();
        } catch (RepositoryErrorException e) {
            log.error("Unable to map entity to OMRS EntityDetail: {}", entity, e);
        }
        return result;
    }

    /**
     * Processes and sends an OMRS event for the new SAS Catalog relationship.
     *
     * @param catalogRelationship the new SAS Catalog relationship information
     */
    private void processNewRelationship(SASCatalogObject catalogRelationship) {
        Relationship relationship = getMappedRelationship(catalogRelationship);
        if (relationship != null) {
            repositoryEventProcessor.processNewRelationshipEvent(
                    sourceName,
                    metadataCollectionId,
                    originatorServerName,
                    originatorServerType,
                    localOrganizationName,
                    relationship
            );
        }
    }

    /**
     * Processes and sends an OMRS event for the updated SAS Catalog relationship.
     *
     * @param catalogRelationship the updated SAS Catalog relationship information
     */
    private void processUpdatedRelationship(SASCatalogObject catalogRelationship) {
        // TODO: find a way to pull back the old version to send in the update event
        Relationship relationship = getMappedRelationship(catalogRelationship);
        if (relationship != null) {
            repositoryEventProcessor.processUpdatedRelationshipEvent(
                    sourceName,
                    metadataCollectionId,
                    originatorServerName,
                    originatorServerType,
                    localOrganizationName,
                    null,
                    relationship
            );
        }
    }

    /**
     * Processes and sends an OMRS event for the removed SAS Catalog relationship.
     *
     * @param catalogRelationship the removed SAS Catalog relationship information
     */
    private void processRemovedRelationship(SASCatalogObject catalogRelationship) {
        // TODO: find a way to pull back the old version to send in the update event
        Relationship relationship = getMappedRelationship(catalogRelationship);
        if (relationship != null) {
            repositoryEventProcessor.processDeletedRelationshipEvent(
                    sourceName,
                    metadataCollectionId,
                    originatorServerName,
                    originatorServerType,
                    localOrganizationName,
                    relationship
            );
        }
    }

    /**
     * Retrieve the mapped OMRS relationship for the provided SAS Catalog relationship.
     *
     * @param catalogRelationship the SAS Catalog relationship to translate to OMRS
     * @return Relationship
     */
    private Relationship getMappedRelationship(SASCatalogObject catalogRelationship) {
        // TODO: this needs to handle both self-referencing (generated) relationships and "normal" relationships,
        //  currently only handling the latter?
        Relationship result = null;
        RelationshipMapping mapping = new RelationshipMapping(
                catalogOMRSRepositoryConnector,
                catalogOMRSMetadataCollection.getTypeDefStore(),
                null, /*catalogOMRSMetadataCollection.getAttributeTypeDefStore(),*/
                new SASCatalogGuid(catalogRelationship.getGuid(), null),
                catalogRelationship,
                null
        );
        try {
            result = mapping.getRelationship();
        } catch (RepositoryErrorException e) {
            log.error("Unable to map relationship to OMRS Relationship: {}", catalogRelationship, e);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect() throws ConnectorCheckedException {
        super.disconnect();
        final String methodName = "disconnect";
        auditLog.logMessage(methodName, AuditCode.EVENT_MAPPER_SHUTDOWN.getMessageDefinition(catalogOMRSRepositoryConnector.getServerName()));
    }

    /**
      * Sends a new TypeDef event.
      *
      * @param newTypeDef the new TypeDef for which to send an event
      */
    public void sendNewTypeDefEvent(TypeDef newTypeDef) {
        repositoryEventProcessor.processNewTypeDefEvent(
                sourceName,
                metadataCollectionId,
                localServerName,
                localServerType,
                localOrganizationName,
                newTypeDef
        );
    }

    /**
     * Sends a new AttributeTypeDef event.
     *
     * @param newAttributeTypeDef the new AttributeTypeDef for which to send an event
     */
    public void sendNewAttributeTypeDefEvent(AttributeTypeDef newAttributeTypeDef) {
        repositoryEventProcessor.processNewAttributeTypeDefEvent(
                sourceName,
                metadataCollectionId,
                localServerName,
                localServerType,
                localOrganizationName,
                newAttributeTypeDef);
    }

    /**
     * Sends a refresh entity request event.
     *
     * @param typeDefGUID unique identifier of requested entity's TypeDef
     * @param typeDefName unique name of requested entity's TypeDef
     * @param entityGUID unique identifier of requested entity
     * @param homeMetadataCollectionId identifier of the metadata collection that is the home to this entity
     */
    public void sendRefreshEntityRequest(String typeDefGUID,
                                         String typeDefName,
                                         String entityGUID,
                                         String homeMetadataCollectionId) {
        repositoryEventProcessor.processRefreshEntityRequested(
                sourceName,
                metadataCollectionId,
                localServerName,
                localServerType,
                localOrganizationName,
                typeDefGUID,
                typeDefName,
                entityGUID,
                homeMetadataCollectionId);
    }

    /**
     * Sends a refresh relationship request event.
     *
     * @param typeDefGUID the guid of the TypeDef for the relationship used to verify the relationship identity
     * @param typeDefName the name of the TypeDef for the relationship used to verify the relationship identity
     * @param relationshipGUID unique identifier of the relationship
     * @param homeMetadataCollectionId unique identifier for the home repository for this relationship
     */
    public void sendRefreshRelationshipRequest(String typeDefGUID,
                                               String typeDefName,
                                               String relationshipGUID,
                                               String homeMetadataCollectionId) {
        repositoryEventProcessor.processRefreshRelationshipRequest(
                sourceName,
                metadataCollectionId,
                localServerName,
                localServerType,
                localOrganizationName,
                typeDefGUID,
                typeDefName,
                relationshipGUID,
                homeMetadataCollectionId);
    }

    /**
     * Throws a ConnectorCheckedException based on the provided parameters.
     *
     * @param errorCode the error code for the exception
     * @param methodName the method name throwing the exception
     * @param cause the underlying cause of the exception (if any, otherwise null)
     * @param params any additional parameters for formatting the error message
     * @throws org.odpi.openmetadata.frameworks.connectors.ffdc.ConnectorCheckedException always
     */
    private void raiseConnectorCheckedException(ErrorCode errorCode, String methodName, Exception cause, String ...params) throws ConnectorCheckedException {
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
}
