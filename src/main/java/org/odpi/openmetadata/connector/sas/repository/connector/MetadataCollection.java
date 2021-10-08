//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.repository.connector;

import org.odpi.openmetadata.connector.sas.auditlog.ErrorCode;
import org.odpi.openmetadata.connector.sas.event.mapper.RepositoryEventMapper;
import org.odpi.openmetadata.connector.sas.repository.connector.mapping.EntityMappingSASCatalog2OMRS;
import org.odpi.openmetadata.connector.sas.repository.connector.mapping.RelationshipMapping;
import org.odpi.openmetadata.connector.sas.repository.connector.mapping.SASCatalogObject;
import org.odpi.openmetadata.connector.sas.repository.connector.model.SASCatalogGuid;
import org.odpi.openmetadata.connector.sas.repository.connector.stores.AttributeTypeDefStore;
import org.odpi.openmetadata.connector.sas.repository.connector.stores.TypeDefStore;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.OMRSMetadataCollectionBase;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.SequencingOrder;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntitySummary;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceStatus;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.Relationship;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.AttributeTypeDef;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDef;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDefCategory;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDefGallery;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryValidator;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.EntityNotKnownException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.FunctionNotSupportedException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.InvalidParameterException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.InvalidTypeDefException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.PagingErrorException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.RelationshipNotKnownException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.RepositoryErrorException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.TypeDefConflictException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.TypeDefKnownException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.TypeDefNotSupportedException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.TypeErrorException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.UserNotAuthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

public class MetadataCollection extends OMRSMetadataCollectionBase {

    private static final Logger log = LoggerFactory.getLogger(MetadataCollection.class);
    private final RepositoryConnector repositoryConnector;
    private TypeDefStore typeDefStore;
    private AttributeTypeDefStore attributeTypeDefStore;
    private RepositoryEventMapper eventMapper;

    /**
     * Constructor ensures the metadata collection is linked to its connector and knows its metadata collection Id.
     *
     * @param parentConnector      connector that this metadata collection supports.  The connector has the information
     *                             to call the metadata repository.
     * @param repositoryName       name of this repository.
     * @param repositoryHelper     helper class for building types and instances
     * @param repositoryValidator  validator class for checking open metadata repository objects and parameters.
     * @param metadataCollectionId unique identifier of the metadata collection Id.
     */
    public MetadataCollection(RepositoryConnector parentConnector, String repositoryName, OMRSRepositoryHelper repositoryHelper, OMRSRepositoryValidator repositoryValidator, String metadataCollectionId) {
        super(parentConnector, repositoryName, repositoryHelper, repositoryValidator, metadataCollectionId);

        this.repositoryConnector = parentConnector;
        this.typeDefStore = new TypeDefStore();
        this.attributeTypeDefStore = new AttributeTypeDefStore();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypeDefGallery getAllTypes(String userId) throws
            RepositoryErrorException,
            InvalidParameterException {

        final String methodName = "getAllTypes";
        super.basicRequestValidation(userId, methodName);

        TypeDefGallery typeDefGallery = new TypeDefGallery();
        List<TypeDef> typeDefs = typeDefStore.getAllTypeDefs();
        log.info("Retrieved {} implemented TypeDefs for this repository.", typeDefs.size());
        typeDefGallery.setTypeDefs(typeDefs);

        List<AttributeTypeDef> attributeTypeDefs = attributeTypeDefStore.getAllAttributeTypeDefs();
        log.info("Retrieved {} implemented AttributeTypeDefs for this repository.", attributeTypeDefs.size());
        typeDefGallery.setAttributeTypeDefs(attributeTypeDefs);

        return typeDefGallery;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTypeDef(String userId,
                           TypeDef newTypeDef) throws
            InvalidParameterException,
            RepositoryErrorException,
            TypeDefNotSupportedException,
            TypeDefKnownException,
            TypeDefConflictException,
            InvalidTypeDefException {

        final String methodName = "addTypeDef";
        final String typeDefParameterName = "newTypeDef";
        super.newTypeDefParameterValidation(userId, newTypeDef, typeDefParameterName, methodName);

        TypeDefCategory typeDefCategory = newTypeDef.getCategory();
        String omrsTypeDefName = newTypeDef.getName();
        log.info("Looking for mapping for {} of type {}", omrsTypeDefName, typeDefCategory.getName());

        if (typeDefStore.isTypeDefMapped(omrsTypeDefName)) {

            //TODO: When adding write support, convert this from Atlas code
            /* If it is a mapped TypeDef, retrieve the mapped type from Catalog and validate it covers what we require
            // (no longer needed for a read-only connector)
            String atlasTypeName = typeDefStore.getMappedAtlasTypeDefName(omrsTypeDefName);
            List<String> gaps = validateTypeDefCoverage(newTypeDef, atlasRepositoryConnector.getTypeDefByName(atlasTypeName, newTypeDef.getCategory()));
            if (gaps != null) {
                // If there were gaps, drop the typedef as unimplemented
                typeDefStore.addUnimplementedTypeDef(newTypeDef);
                throw new TypeDefNotSupportedException(
                        404,
                        MetadataCollection.class.getName(),
                        methodName,
                        omrsTypeDefName + " is not supported.",
                        String.join(", ", gaps),
                        "Request support through Egeria GitHub issue."
                );
            } else {
                // Otherwise add it as implemented
                typeDefStore.addTypeDef(newTypeDef);
            }
            */

            typeDefStore.addTypeDef(newTypeDef);

        } else if (!typeDefStore.isReserved(omrsTypeDefName)) {

            if (repositoryConnector.typeDefExistsByName(omrsTypeDefName, typeDefCategory)) {
                //TODO: Is this necessary?
                /* If the TypeDef already exists in Atlas, add it to our store
                List<String> gaps = validateTypeDefCoverage(newTypeDef, atlasRepositoryConnector.getTypeDefByName(omrsTypeDefName, newTypeDef.getCategory()));
                if (gaps != null) {
                    // If there were gaps, drop the typedef as unimplemented
                    typeDefStore.addUnimplementedTypeDef(newTypeDef);
                    throw new TypeDefNotSupportedException(
                            404,
                            MetadataCollection.class.getName(),
                            methodName,
                            omrsTypeDefName + " is not supported.",
                            String.join(", ", gaps),
                            "Request support through Egeria GitHub issue."
                    );
                } else {
                    // Otherwise add it as implemented
                    typeDefStore.addTypeDef(newTypeDef);
                }
                */
                typeDefStore.addTypeDef(newTypeDef);
            } else if (typeDefStore.isSuperTypeOfMappedType(newTypeDef)) {
                // Need to add super types of mapped types to work in the UI
                typeDefStore.addTypeDef(newTypeDef);
            } else {
                //switch(newTypeDef.getCategory()) {
                //TODO: When adding write support, convert this from Atlas code
                    /*case ENTITY_DEF:
                        EntityDefMapping.addEntityTypeToAtlas(
                                (EntityDef) newTypeDef,
                                typeDefStore,
                                attributeTypeDefStore,
                                atlasRepositoryConnector
                        );
                        break;*/
                // For now, only create classifications and relationships (no new entity types)
                   /* case RELATIONSHIP_DEF:
                        RelationshipDefMapping.addRelationshipTypeToAtlas(
                                (RelationshipDef) newTypeDef,
                                typeDefStore,
                                attributeTypeDefStore,
                                atlasRepositoryConnector
                        );
                        break;
                    case CLASSIFICATION_DEF:
                        ClassificationDefMapping.addClassificationTypeToAtlas(
                                (ClassificationDef) newTypeDef,
                                typeDefStore,
                                attributeTypeDefStore,
                                atlasRepositoryConnector
                        );
                        break;*/
                //   default:
                typeDefStore.addUnimplementedTypeDef(newTypeDef);
                raiseTypeDefNotSupportedException(ErrorCode.TYPEDEF_NOT_SUPPORTED, methodName, null, omrsTypeDefName, repositoryName);
                //  }
            }

        } else {
            // Otherwise, we'll drop it as unimplemented
            typeDefStore.addUnimplementedTypeDef(newTypeDef);
            raiseTypeDefNotSupportedException(ErrorCode.TYPEDEF_NOT_SUPPORTED, methodName, null, omrsTypeDefName, repositoryName);
        }

//        checkEventMapperIsConfigured(methodName);
//        eventMapper.sendNewTypeDefEvent(newTypeDef);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean verifyTypeDef(String  userId,
                                 TypeDef typeDef) throws InvalidParameterException,
            RepositoryErrorException,
            TypeDefNotSupportedException,
            InvalidTypeDefException {

        final String  methodName           = "verifyTypeDef";
        final String  typeDefParameterName = "typeDef";

        log.info("VerifyTypeDef: " + typeDef.getName() + " : " + typeDef.getGUID());

        /*
         * Validate parameters
         */
        this.validateRepositoryConnector(methodName);
        parentConnector.validateRepositoryIsActive(methodName);
        repositoryValidator.validateUserId(repositoryName, userId, methodName);
        repositoryValidator.validateTypeDef(repositoryName, typeDefParameterName, typeDef, methodName);

        String guid = typeDef.getGUID();

        // If we know the TypeDef is unimplemented, immediately throw an exception stating as much
        if (typeDefStore.getUnimplementedTypeDefByGUID(guid) != null) {
            raiseTypeDefNotSupportedException(ErrorCode.TYPEDEF_NOT_SUPPORTED, methodName, null, typeDef.getName(), repositoryName);
        }

        return typeDefStore.getTypeDefByGUID(guid) != null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityDetail isEntityKnown(String userId,
                                      String guid) throws
            InvalidParameterException,
            RepositoryErrorException {

        final String methodName = "isEntityKnown";
        super.getInstanceParameterValidation(userId, guid, methodName);

        EntityDetail detail = null;
        try {
            detail = getEntityDetail(userId, guid);
        } catch (EntityNotKnownException e) {
            log.info("Entity {} not known to the repository, or only a proxy.", guid, e);
        }
        return detail;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntitySummary getEntitySummary(String userId,
                                          String guid) throws
            InvalidParameterException,
            RepositoryErrorException,
            EntityNotKnownException {

        final String methodName = "getEntitySummary";
        super.getInstanceParameterValidation(userId, guid, methodName);

        // Guid cannot be null here, as validation above ensures it is non-null
        SASCatalogGuid catalogGuid = SASCatalogGuid.fromGuid(guid);
        String prefix = catalogGuid.getGeneratedPrefix();
        SASCatalogObject entity = getSASCatalogEntitySafe(catalogGuid.getSASCatalogGuid(), methodName);
        EntityMappingSASCatalog2OMRS mapping = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, attributeTypeDefStore, entity, prefix, userId);
        return mapping.getEntitySummary();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityDetail getEntityDetail(String userId,
                                        String guid) throws
            InvalidParameterException,
            RepositoryErrorException,
            EntityNotKnownException {

        final String methodName = "getEntityDetail";

        log.info("Get Entity Detail: " + guid);

        super.getInstanceParameterValidation(userId, guid, methodName);

        // Guid cannot be null here, as validation above ensures it is non-null
        SASCatalogGuid sasCatalogGuid = SASCatalogGuid.fromGuid(guid);
        String prefix = sasCatalogGuid.getGeneratedPrefix();
        SASCatalogObject entity = getSASCatalogEntitySafe(sasCatalogGuid.getSASCatalogGuid(), methodName);
        // TODO: Do we need an attributeTypeDefStore like Atlas?
        EntityMappingSASCatalog2OMRS mapping = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, null /*attributeTypeDefStore */, entity, prefix, userId);
        return mapping.getEntityDetail();
    }

    @Override
    public List<Relationship> getRelationshipsForEntity(String userId,
                                                        String entityGUID,
                                                        String relationshipTypeGUID,
                                                        int fromRelationshipElement,
                                                        List<InstanceStatus> limitResultsByStatus,
                                                        Date asOfTime,
                                                        String sequencingProperty,
                                                        SequencingOrder sequencingOrder,
                                                        int pageSize) throws
            InvalidParameterException,
            TypeErrorException,
            RepositoryErrorException,
            EntityNotKnownException,
            PagingErrorException,
            FunctionNotSupportedException,
            UserNotAuthorizedException {

        final String  methodName = "getRelationshipsForEntity";
        getRelationshipsForEntityParameterValidation(
                userId,
                entityGUID,
                relationshipTypeGUID,
                fromRelationshipElement,
                limitResultsByStatus,
                asOfTime,
                sequencingProperty,
                sequencingOrder,
                pageSize
        );

        List<Relationship> alRelationships = null;

        // Immediately throw unimplemented exception if trying to retrieve historical view or sequence by property
        if (asOfTime != null) {
            raiseFunctionNotSupportedException(ErrorCode.NO_HISTORY, methodName, repositoryName);
        } else if (limitResultsByStatus == null
                || (limitResultsByStatus.size() == 1 && limitResultsByStatus.contains(InstanceStatus.ACTIVE))) {

            // Otherwise, only bother searching if we are after ACTIVE (or "all") entities -- non-ACTIVE means we
            // will just return an empty list

            // Guid cannot be null here, as validation above ensures it is non-null
            SASCatalogGuid sasCatalogGuid = SASCatalogGuid.fromGuid(entityGUID);
            String prefix = sasCatalogGuid.getGeneratedPrefix();

            // 1. retrieve entity from Catalog by GUID (and its relationships)
            SASCatalogObject asset = null;
            List<SASCatalogObject> relationships = null;
            try {
                asset = repositoryConnector.getEntityByGUID(sasCatalogGuid.getSASCatalogGuid());
                relationships = repositoryConnector.getRelationshipsForEntity(sasCatalogGuid.getSASCatalogGuid());
            } catch (Exception e) {
                raiseEntityNotKnownException(ErrorCode.ENTITY_NOT_KNOWN, methodName, e, entityGUID, methodName, repositoryName);
            }

            // Ensure the entity actually exists (if not, throw error to that effect)
            if (asset == null || relationships == null) {
                raiseEntityNotKnownException(ErrorCode.ENTITY_NOT_KNOWN, methodName, null, entityGUID, methodName, repositoryName);
            } else {

                EntityMappingSASCatalog2OMRS entityMap = new EntityMappingSASCatalog2OMRS(
                        repositoryConnector,
                        typeDefStore,
                        attributeTypeDefStore,
                        asset,
                        prefix,
                        userId
                );

                // 2. Apply the mapping to the object, and retrieve the resulting relationships
                alRelationships = entityMap.getRelationships(
                        relationships,
                        relationshipTypeGUID,
                        fromRelationshipElement,
                        sequencingProperty,
                        sequencingOrder,
                        pageSize
                );

            }

        }

        return alRelationships;

    }

    @Override
    public Relationship getRelationship(String userId,
                                        String guid) throws InvalidParameterException,
            RepositoryErrorException,
            RelationshipNotKnownException {

        final String methodName = "getRelationship";
        super.getInstanceParameterValidation(userId, guid, methodName);

        // Guid cannot be null here, as validation above ensures it is non-null
        SASCatalogGuid sasCatalogGuid = SASCatalogGuid.fromGuid(guid);
        if (sasCatalogGuid.isGeneratedInstanceGuid()) {
            // If this is a self-referencing relationship, we need to construct it by retrieving the entity (not
            // a relationship) from Catalog
            Relationship relationship = null;
            try {
                SASCatalogObject entity = repositoryConnector.getEntityByGUID(sasCatalogGuid.getSASCatalogGuid());
                if (entity != null) {
                    relationship = RelationshipMapping.getSelfReferencingRelationship(
                            repositoryConnector,
                            typeDefStore,
                            sasCatalogGuid,
                            entity);
                } else {
                    raiseRelationshipNotKnownException(ErrorCode.RELATIONSHIP_NOT_KNOWN, methodName, null, guid, methodName, repositoryName);
                }
            } catch (Exception e) {
                raiseRelationshipNotKnownException(ErrorCode.RELATIONSHIP_NOT_KNOWN, methodName, e, guid, methodName, repositoryName);
            }
            return relationship;
        } else {
            // Otherwise we should be able to directly retrieve a relationship from Catalog
            SASCatalogObject relationship = null;
            try {
                relationship = this.repositoryConnector.getRelationshipByGUID(sasCatalogGuid.getSASCatalogGuid());
            } catch (Exception e) {
                raiseRelationshipNotKnownException(ErrorCode.RELATIONSHIP_NOT_KNOWN, methodName, e, guid, methodName, repositoryName);
            }
            if (relationship == null) {
                raiseRelationshipNotKnownException(ErrorCode.RELATIONSHIP_NOT_KNOWN, methodName, null, guid, methodName, repositoryName);
            }
            RelationshipMapping mapping = new RelationshipMapping(repositoryConnector, typeDefStore, attributeTypeDefStore, sasCatalogGuid, relationship, userId);
            return mapping.getRelationship();
        }

    }

    @Override
    public boolean verifyAttributeTypeDef(String userId, AttributeTypeDef attributeTypeDef) throws InvalidParameterException, RepositoryErrorException, TypeDefNotSupportedException, TypeDefConflictException, InvalidTypeDefException, UserNotAuthorizedException {
        // Method stub to prevent exceptions
        return false;
    }

    @Override
    public Relationship isRelationshipKnown(String userId, String guid) throws InvalidParameterException, RepositoryErrorException, UserNotAuthorizedException {
        // Method stub to prevent exceptions
        return null;
    }

    /**
     * Try to retrieve an Catalog entity using the provided GUID, and if not found throw an EntityNotKnownException.
     * @param guid the GUID for the entity to retrieve
     * @param methodName the name of the method retrieving the entity
     * @return SASCatalogObject
     * @throws EntityNotKnownException if the entity cannot be found in Catalog
     */
    private SASCatalogObject getSASCatalogEntitySafe(String guid, String methodName) throws EntityNotKnownException {
        SASCatalogObject entity = null;
        try {
            entity = this.repositoryConnector.getEntityByGUID(guid);
        } catch (Exception e) {
            raiseEntityNotKnownException(ErrorCode.ENTITY_NOT_KNOWN, methodName, e, guid, methodName, repositoryName);
        }
        if (entity == null) {
            raiseEntityNotKnownException(ErrorCode.ENTITY_NOT_KNOWN, methodName, null, guid, methodName, repositoryName);
        }
        return entity;
    }

    /**
     * Throw an EntityNotKnownException using the provided parameters.
     * @param errorCode the error code for the exception
     * @param methodName the method throwing the exception
     * @param cause the underlying cause of the exception (if any, otherwise null)
     * @param params any parameters for formatting the error message
     * @throws EntityNotKnownException always
     */
    private void raiseEntityNotKnownException(ErrorCode errorCode, String methodName, Throwable cause, String ...params) throws EntityNotKnownException {
        if (cause == null) {
            throw new EntityNotKnownException(errorCode.getMessageDefinition(params),
                    this.getClass().getName(),
                    methodName);
        } else {
            throw new EntityNotKnownException(errorCode.getMessageDefinition(params),
                    this.getClass().getName(),
                    methodName,
                    cause);
        }
    }

    /**
     * Throw a TypeDefNotSupportedException using the provided parameters.
     * @param errorCode the error code for the exception
     * @param methodName the method throwing the exception
     * @param cause the underlying cause of the exception (if any, otherwise null)
     * @param params any parameters for formatting the error message
     * @throws TypeDefNotSupportedException always
     */
    private void raiseTypeDefNotSupportedException(ErrorCode errorCode, String methodName, Throwable cause, String ...params) throws TypeDefNotSupportedException {
        if (cause == null) {
            throw new TypeDefNotSupportedException(errorCode.getMessageDefinition(params),
                    this.getClass().getName(),
                    methodName);
        } else {
            throw new TypeDefNotSupportedException(errorCode.getMessageDefinition(params),
                    this.getClass().getName(),
                    methodName,
                    cause);
        }
    }

    /**
     * Throw a RelationshipNotKnownException using the provided parameters.
     * @param errorCode the error code for the exception
     * @param methodName the method throwing the exception
     * @param cause the underlying cause of the exception (if any, otherwise null)
     * @param params any parameters for formatting the error message
     * @throws RelationshipNotKnownException always
     */
    private void raiseRelationshipNotKnownException(ErrorCode errorCode, String methodName, Throwable cause, String ...params) throws RelationshipNotKnownException {
        if (cause == null) {
            throw new RelationshipNotKnownException(errorCode.getMessageDefinition(params),
                    this.getClass().getName(),
                    methodName);
        } else {
            throw new RelationshipNotKnownException(errorCode.getMessageDefinition(params),
                    this.getClass().getName(),
                    methodName,
                    cause);
        }
    }

    public TypeDefStore getTypeDefStore() {
        return this.typeDefStore;
    }

    public void setEventMapper(RepositoryEventMapper RepositoryEventMapper) {
        this.eventMapper = RepositoryEventMapper;
    }

    public RepositoryEventMapper getEventMapper() {
        return eventMapper;
    }

    /**
     * Throw a FunctionNotSupportedException using the provided parameters.
     * @param errorCode the error code for the exception
     * @param methodName the method throwing the exception
     * @param params any parameters for formatting the error message
     * @throws FunctionNotSupportedException always
     */
    private void raiseFunctionNotSupportedException(ErrorCode errorCode, String methodName, String ...params) throws FunctionNotSupportedException {
        throw new FunctionNotSupportedException(errorCode.getMessageDefinition(params),
                this.getClass().getName(),
                methodName);
    }

}
