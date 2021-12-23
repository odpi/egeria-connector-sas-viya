//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.repository.connector;

import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceGraph;
import org.odpi.openmetadata.connector.sas.auditlog.ErrorCode;
import org.odpi.openmetadata.connector.sas.event.mapper.RepositoryEventMapper;
import org.odpi.openmetadata.connector.sas.event.model.catalog.instance.Instance;
import org.odpi.openmetadata.connector.sas.repository.connector.mapping.EntityMappingSASCatalog2OMRS;
import org.odpi.openmetadata.connector.sas.repository.connector.mapping.RelationshipMapping;
import org.odpi.openmetadata.connector.sas.repository.connector.mapping.SASCatalogObject;
import org.odpi.openmetadata.connector.sas.repository.connector.mapping.SequencingUtils;
import org.odpi.openmetadata.connector.sas.repository.connector.model.SASCatalogGuid;
import org.odpi.openmetadata.connector.sas.repository.connector.stores.AttributeTypeDefStore;
import org.odpi.openmetadata.connector.sas.repository.connector.stores.TypeDefStore;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.OMRSMetadataCollectionBase;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.MatchCriteria;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.SequencingOrder;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntitySummary;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceProperties;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstancePropertyValue;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceStatus;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.Relationship;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.*;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryValidator;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.EntityNotKnownException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.FunctionNotSupportedException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.InvalidParameterException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.InvalidTypeDefException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.PagingErrorException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.PropertyErrorException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.RelationshipNotKnownException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.RepositoryErrorException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.TypeDefConflictException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.TypeDefKnownException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.TypeDefNotSupportedException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.TypeErrorException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.UserNotAuthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        String defName = entity.getTypeName();
        Map<String, String> mappedOMRSTypeDefs = typeDefStore.getMappedOMRSTypeDefNameWithPrefixes(defName);
        for (Map.Entry<String, String> entry : mappedOMRSTypeDefs.entrySet())
        {
            prefix = entry.getKey();
            // TODO: Do we need an attributeTypeDefStore like Atlas?
            EntityMappingSASCatalog2OMRS mapping = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, null /*attributeTypeDefStore */, entity, prefix, userId);
            EntityDetail entityDetail = mapping.getEntityDetail();
            if (entityDetail != null) {
                return entityDetail;
            }
        }
        // TODO: Do we need an attributeTypeDefStore like Atlas?
        //EntityMappingSASCatalog2OMRS mapping = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, null /*attributeTypeDefStore */, entity, prefix, userId);
        //return mapping.getEntityDetail();
        return null;
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

    public  List<EntityDetail> findEntitiesByProperty(String                    userId,
                                                      String                    entityTypeGUID,
                                                      InstanceProperties matchProperties,
                                                      MatchCriteria matchCriteria,
                                                      int                       fromEntityElement,
                                                      List<InstanceStatus>      limitResultsByStatus,
                                                      List<String>              limitResultsByClassification,
                                                      Date                      asOfTime,
                                                      String                    sequencingProperty,
                                                      SequencingOrder           sequencingOrder,
                                                      int                       pageSize) throws InvalidParameterException,
            TypeErrorException,
            RepositoryErrorException,
            PropertyErrorException,
            PagingErrorException,
            FunctionNotSupportedException,
            UserNotAuthorizedException {

        final String methodName = "findEntitiesByProperty";
        findEntitiesByPropertyParameterValidation(
                userId,
                entityTypeGUID,
                matchProperties,
                matchCriteria,
                fromEntityElement,
                limitResultsByStatus,
                limitResultsByClassification,
                asOfTime,
                sequencingProperty,
                sequencingOrder,
                pageSize
        );

        List<Instance> results = null;

        // Immediately throw unimplemented exception if trying to retrieve historical view
        if (asOfTime != null) {
            raiseFunctionNotSupportedException(ErrorCode.NO_HISTORY, methodName, repositoryName);
        }

        results = buildAndRunDSLSearch(
                methodName,
                entityTypeGUID,
                entityTypeGUID,
                limitResultsByClassification,
                matchProperties,
                matchCriteria,
                fromEntityElement,
                limitResultsByStatus,
                sequencingProperty,
                sequencingOrder,
                pageSize,
                userId
        );

        List<EntityDetail> entityDetails = null;
        if (results != null) {
            entityDetails = sortAndLimitFinalResults(
                    results,
                    entityTypeGUID,
                    fromEntityElement,
                    sequencingProperty,
                    sequencingOrder,
                    pageSize,
                    userId
            );
        }
        return (entityDetails == null || entityDetails.isEmpty()) ? null : entityDetails;

    }

    @Override
    public List<EntityDetail> findEntitiesByPropertyValue(String userId,
                                                          String entityTypeGUID,
                                                          String searchCriteria,
                                                          int fromEntityElement,
                                                          List<InstanceStatus> limitResultsByStatus,
                                                          List<String> limitResultsByClassification,
                                                          Date asOfTime,
                                                          String sequencingProperty,
                                                          SequencingOrder sequencingOrder,
                                                          int pageSize) throws
            InvalidParameterException,
            TypeErrorException,
            RepositoryErrorException,
            PropertyErrorException,
            PagingErrorException,
            FunctionNotSupportedException,
            UserNotAuthorizedException {

        final String  methodName = "findEntitiesByPropertyValue";
        findEntitiesByPropertyValueParameterValidation(
                userId,
                entityTypeGUID,
                searchCriteria,
                fromEntityElement,
                limitResultsByStatus,
                limitResultsByClassification,
                asOfTime,
                sequencingProperty,
                sequencingOrder,
                pageSize
        );

        List<Instance> results = new ArrayList<Instance>();

        // Immediately throw unimplemented exception if trying to retrieve historical view
        if (asOfTime != null) {
            raiseFunctionNotSupportedException(ErrorCode.NO_HISTORY, methodName, repositoryName);
        }

        // Search criteria is not allowed to be empty for this method, so cannot be null or empty string.
        if (!searchCriteria.isEmpty()) {

            // Otherwise we need to do an OR-based search across all string properties in Atlas, using whatever the
            // regex of searchCriteria contains for each property
            // Add all textual properties of the provided entity as matchProperties,
            //  for an OR-based search of their values
            Map<String, Map<String, String>> mappingsToSearch = new HashMap<>();
            if (entityTypeGUID != null) {
                // We are searching for a particular entity type, get the associated mappings
                mappingsToSearch = getMappingsToSearch(entityTypeGUID, userId);
            } else {
                // We are searching across all entity types, get all mappings
                // We will need to send the request only once, so we'll only use the first mapping
                mappingsToSearch = typeDefStore.getAllOmrsNameToCatalogNameMappings();
            }
            for (Map.Entry<String, Map<String, String>> entryToSearch : mappingsToSearch.entrySet()) {
                InstanceProperties matchProperties = new InstanceProperties();
                String omrsTypeName = entryToSearch.getKey();
                String omrsTypeGUID = typeDefStore.getTypeDefByName(omrsTypeName).getGUID();

                Map<String, TypeDefAttribute> typeDefAttributeMap = typeDefStore.getAllTypeDefAttributesForName(omrsTypeName);
                if (typeDefAttributeMap != null) {
                    // This will look at all OMRS attributes, but buildAndRunDSLSearch (later) should limit to only those mapped to catalog
                    for (Map.Entry<String, TypeDefAttribute> attributeEntry : typeDefAttributeMap.entrySet()) {
                        String attributeName = attributeEntry.getKey();
                        // Only supporting search by name value for now
                        if (attributeName == "qualifiedName") {
                            TypeDefAttribute typeDefAttribute = attributeEntry.getValue();
                            // Only need to retain string-based attributes for the full text search
                            AttributeTypeDef attributeTypeDef = typeDefAttribute.getAttributeType();
                            if (attributeTypeDef.getCategory().equals(AttributeTypeDefCategory.PRIMITIVE)) {
                                PrimitiveDefCategory primitiveDefCategory = ((PrimitiveDef) attributeTypeDef).getPrimitiveDefCategory();
                                if (primitiveDefCategory.equals(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING)
                                        || primitiveDefCategory.equals(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_BYTE)
                                        || primitiveDefCategory.equals(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_CHAR)) {
                                    matchProperties = repositoryHelper.addStringPropertyToInstance(
                                            repositoryName,
                                            matchProperties,
                                            attributeName,
                                            searchCriteria,
                                            methodName
                                    );
                                } else {
                                    log.debug("Skipping inclusion of non-string attribute: {}", attributeName);
                                }
                            } else {
                                log.debug("Skipping inclusion of non-string attribute: {}", attributeName);
                            }
                        }
                    }
                }

                List<Instance> innerResults = new ArrayList<Instance>();
                try {
                    innerResults = buildAndRunDSLSearch(
                            methodName,
                            entityTypeGUID,
                            omrsTypeGUID,
                            limitResultsByClassification,
                            matchProperties,
                            MatchCriteria.ANY,
                            fromEntityElement,
                            limitResultsByStatus,
                            sequencingProperty,
                            sequencingOrder,
                            pageSize,
                            userId
                    );
                } catch (Exception e) {
                    log.error("Exception from findEntitiesByPropertyValue inner search for omrsTypeName {}: {}", omrsTypeName, e.getMessage());
                }
                if (innerResults != null) {
                    results.addAll(innerResults);
                }
                // If entityTypeGUID is null, we are searching across all entity types
                // We'll only need to send search request once, which we have above
                // so can break out of the loop
                if (entityTypeGUID == null) {
                    break;
                }
            }
        }

        List<EntityDetail> entityDetails = null;
        if (results != null) {
            entityDetails = sortAndLimitFinalResults(
                    results,
                    entityTypeGUID,
                    fromEntityElement,
                    sequencingProperty,
                    sequencingOrder,
                    pageSize,
                    userId
            );
        }
        return (entityDetails == null || entityDetails.isEmpty()) ? null : entityDetails;

    }

       /**
     * Return the entities and relationships that radiate out from the supplied entity GUID.
     * The results are scoped both the instance type guids and the level.
     *
     * @param userId unique identifier for requesting user.
     * @param entityGUID the starting point of the query.
     * @param entityTypeGUIDs list of entity types to include in the query results.  Null means include
     *                          all entities found, irrespective of their type.
     * @param relationshipTypeGUIDs list of relationship types to include in the query results.  Null means include
     *                                all relationships found, irrespective of their type.
     * @param limitResultsByStatus By default, relationships in all non-DELETED statuses are returned.  However, it is possible
     *                             to specify a list of statuses (eg ACTIVE) to restrict the results to.  Null means all
     *                             status values except DELETED.
     * @param limitResultsByClassification List of classifications that must be present on all returned entities.
     * @param asOfTime Requests a historical query of the relationships for the entity.  Null means return the
     *                 present values.
     * @param level the number of the relationships out from the starting entity that the query will traverse to
     *              gather results.
     * @return InstanceGraph the sub-graph that represents the returned linked entities and their relationships.
     * @throws InvalidParameterException one of the parameters is invalid or null.
     * @throws TypeErrorException the type guid passed on the request is not known by the
     *                              metadata collection.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws EntityNotKnownException the entity identified by the entityGUID is not found in the metadata collection.
     * @throws PropertyErrorException there is a problem with one of the other parameters.
     * @throws FunctionNotSupportedException the repository does not support this call.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @Override
    public  InstanceGraph getEntityNeighborhood(String               userId,
                                                String               entityGUID,
                                                List<String>         entityTypeGUIDs,
                                                List<String>         relationshipTypeGUIDs,
                                                List<InstanceStatus> limitResultsByStatus,
                                                List<String>         limitResultsByClassification,
                                                Date                 asOfTime,
                                                int                  level) throws InvalidParameterException,
                                                                                   TypeErrorException,
                                                                                   RepositoryErrorException,
                                                                                   EntityNotKnownException,
                                                                                   PropertyErrorException,
                                                                                   FunctionNotSupportedException,
                                                                                   UserNotAuthorizedException
    {
        final String methodName  = "getEntityNeighborhood";

        /*
         * Validate parameters
         */
        this.getEntityNeighborhoodParameterValidation(userId,
                                                      entityGUID,
                                                      entityTypeGUIDs,
                                                      relationshipTypeGUIDs,
                                                      limitResultsByStatus,
                                                      limitResultsByClassification,
                                                      asOfTime,
                                                      level);

        InstanceGraph instanceGraph = new InstanceGraph();
        try {
            List<Relationship> relationships = getRelationshipsForEntity(userId, entityGUID, null, 0, limitResultsByStatus, asOfTime, null, null, 0);
            if (relationships == null) {
                return instanceGraph;
            }
            List<EntityDetail> entityList = new ArrayList<EntityDetail>(1);

            //Getting a unique set of GUIDs to retrieve the entity details
            Set<String> entitiesToRetrieve = new HashSet<String>();
            //entitiesToRetrieve.add(entityGUID); //the original entitiy
            for (Relationship r : relationships) {
                //Right now getEntityDetail returns a prefix but it is not consistent which one.
                //This needs to be looked at but for now, I want to make sure we are
                //only retriving an object one time.
                SASCatalogGuid sasCatalogGuid = SASCatalogGuid.fromGuid(r.getEntityOneProxy().getGUID());
                entitiesToRetrieve.add(sasCatalogGuid.getSASCatalogGuid());
                sasCatalogGuid = SASCatalogGuid.fromGuid(r.getEntityTwoProxy().getGUID());
                entitiesToRetrieve.add(sasCatalogGuid.getSASCatalogGuid());
            }
            for (String entityId : entitiesToRetrieve) {
                entityList.add(getEntityDetail(userId, entityId));
            }

            instanceGraph.setEntities(entityList);
            instanceGraph.setRelationships(relationships);

        } catch (PagingErrorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return instanceGraph;

    }

    /**
     * Build an Atlas domain-specific language (DSL) query based on the provided parameters, and return its results.
     *
     * @param methodName the name of the calling method
     * @param entityTypeGUID unique identifier for the type of entity requested.  Null means any type of entity
     *                       (but could be slow so not recommended.
     * @param limitResultsByClassification list of classifications by which to limit the results.
     * @param matchProperties Optional list of entity properties to match (contains wildcards).
     * @param matchCriteria Enum defining how the match properties should be matched to the classifications in the repository.
     * @param fromEntityElement the starting element number of the entities to return.
     *                                This is used when retrieving elements
     *                                beyond the first page of results. Zero means start from the first element.
     * @param limitResultsByStatus By default, entities in all statuses are returned.  However, it is possible
     *                             to specify a list of statuses (eg ACTIVE) to restrict the results to.  Null means all
     *                             status values.
     * @param sequencingProperty String name of the entity property that is to be used to sequence the results.
     *                           Null means do not sequence on a property name (see SequencingOrder).
     * @param sequencingOrder Enum defining how the results should be ordered.
     * @param pageSize the maximum number of result entities that can be returned on this request.  Zero means
     *                 unrestricted return results size.
     * @param userId the user through which to run the search
     * @return {@code List<AtlasEntityHeader>}
     * @throws FunctionNotSupportedException when trying to search using a status that is not supported in Atlas
     * @throws RepositoryErrorException when there is some error running the search against Atlas
     */
    private List<Instance> buildAndRunDSLSearch(String methodName,
                                                          String incomingEntityTypeGUID,
                                                          String entityTypeGUID,
                                                          List<String> limitResultsByClassification,
                                                          InstanceProperties matchProperties,
                                                          MatchCriteria matchCriteria,
                                                          int fromEntityElement,
                                                          List<InstanceStatus> limitResultsByStatus,
                                                          String sequencingProperty,
                                                          SequencingOrder sequencingOrder,
                                                          int pageSize,
                                                          String userId) throws
            FunctionNotSupportedException,
            RepositoryErrorException
    {

        List<Instance> results = null;
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> attributeFilter = new HashMap<>();

        String catalogTypeName = "";
        String propertyMatchDelim = "and";
        if (matchCriteria != null && matchCriteria.equals(MatchCriteria.ANY)) {
            propertyMatchDelim = "or";
        }

        // Run multiple searches, if there are multiple types mapped to the OMRS type...
        Map<String, Map<String, String>> mappingsToSearch = getMappingsToSearch(entityTypeGUID, userId);
        for (Map.Entry<String, Map<String, String>> entryToSearch : mappingsToSearch.entrySet()) {
            String filter = "";
            String typeFilter = "";
            String propertyFilter = "";
            String omrsTypeName = entryToSearch.getKey();
            Map<String, String> catalogTypeNamesByPrefix = entryToSearch.getValue();
            int typeCount = catalogTypeNamesByPrefix.size();
            Map<String, InstancePropertyValue> properties = matchProperties.getInstanceProperties();
            Map<String, TypeDefAttribute> omrsAttrTypeDefs = typeDefStore.getAllTypeDefAttributesForName(omrsTypeName);

            for (Map.Entry<String, String> entry : catalogTypeNamesByPrefix.entrySet()) {
                propertyFilter = "";
                String prefix = entry.getKey();
                catalogTypeName = entry.getValue();
                Map<String, String> omrsPropertyMap = typeDefStore.getPropertyMappingsForOMRSTypeDef(omrsTypeName, prefix);

                //TODO: Add Classification support

                // Add match properties, if requested
                if (matchProperties != null) {
//                    List<String> propertyCriteria = new ArrayList<>();//
                    // By default, include only Referenceable's properties (as these will be the only properties that exist
                    // across ALL entity types)//
                    if (properties != null) {
                        for (Map.Entry<String, InstancePropertyValue> property : properties.entrySet()) {
                            String omrsPropertyName = property.getKey();
                            String catalogName = omrsPropertyMap.get(omrsPropertyName);
                            String catalogPropertyName = catalogName.substring(catalogName.indexOf(".") + 1);
                            InstancePropertyValue value = property.getValue();

                            if(catalogName.startsWith("attribute.")) {
                                attributeFilter.put(catalogPropertyName, value.valueAsString());
                            } else {
                                if (propertyFilter.isEmpty()) {
                                    propertyFilter = String.format("contains(%s,\"%s\")", catalogPropertyName, value.valueAsString());
                                } else {
                                    propertyFilter = String.format("%s(%s,contains(%s,\"%s\"))", propertyMatchDelim, propertyFilter, catalogPropertyName, value.valueAsString());
                                }
                            }
                        }
                    }
                }

                String typeFilterStr = String.format("eq(type,\"%s\")", catalogTypeName);
                // Handle reference types differently since they all have a type of "reference"
                if(catalogTypeName.startsWith("reference.")) {
                    // Extract reference name after "reference."
                    String refName = catalogTypeName.substring(catalogTypeName.indexOf(".") + 1);
                    typeFilterStr = "eq(type,\"reference\")";
                    attributeFilter.put("referencedType", refName);
                }

                if (typeFilter.isEmpty() && propertyFilter.isEmpty()) {
                    typeFilter = typeFilterStr;
                } else if (typeFilter.isEmpty() && !propertyFilter.isEmpty()) {
                    typeFilter = String.format("and(%s,%s)", typeFilterStr, propertyFilter);
                } else if (!typeFilter.isEmpty() && propertyFilter.isEmpty()) {
                    typeFilter = String.format("and(%s,%s)", typeFilter, typeFilter);
                } else {
                    typeFilter = String.format("or(and(%s,%s), %s)", typeFilterStr, propertyFilter, typeFilter);
                }

                // If searching by property value across all entity types, we'll only need property filter
                if (incomingEntityTypeGUID == null && methodName == "findEntitiesByPropertyValue") {
                    queryParams.put("filter", propertyFilter);
                } else {
                    queryParams.put("filter", typeFilter);
                }
            }

            // TODO: Add status limiters, if requested

            // TODO: Apply sequencing order, if requested

            // Add paging criteria, if requested
            if (pageSize > 0) {
                queryParams.put("limit", pageSize+"");
            }
            if (fromEntityElement > 0) {
                queryParams.put("offset", fromEntityElement+"");
            }

            try {
                results = repositoryConnector.getInstancesWithParams(queryParams, attributeFilter);
            } catch (Exception e) {
                raiseRepositoryErrorException(ErrorCode.INVALID_SEARCH, methodName, e, filter);
                log.error("Repository error exception for method {} and filter {} : {}", methodName, filter, e);
            }
        }
        return results;
    }

    /**
     * Retrieve the listing of implemented mappings that should be used for an entity search, including navigating
     * subtypes when a supertype is the entity type provided.  The result will be a map of OMRS type name to a map
     * keyed by prefix (which could be null) to the mapped atlas type names.
     *
     * @param entityTypeGUID the GUID of the OMRS entity type for which to search
     * @param userId the userId through which to search
     * @return {@code Map<String, Map<String, String>>}
     * @throws RepositoryErrorException on any unexpected error
     */
    private Map<String, Map<String, String>> getMappingsToSearch(String entityTypeGUID, String userId) throws
            RepositoryErrorException {
        Map<String, Map<String, String>> results = new HashMap<>();
        Map<String, String> atlasTypeNamesByPrefix = new HashMap<>();
        String requestedTypeName = null;
        if (entityTypeGUID != null) {
            TypeDef typeDef = typeDefStore.getTypeDefByGUID(entityTypeGUID, false);
            if (typeDef != null) {
                String omrsTypeName = typeDef.getName();
                atlasTypeNamesByPrefix = typeDefStore.getAllMappedCatalogTypeDefNames(omrsTypeName);
                results.put(omrsTypeName, atlasTypeNamesByPrefix);
            } else {
                TypeDef unimplemented = typeDefStore.getUnimplementedTypeDefByGUID(entityTypeGUID);
                requestedTypeName = unimplemented.getName();
                log.warn("Unable to search for type, unknown to repository: {}", entityTypeGUID);
            }
        } else {
            atlasTypeNamesByPrefix.put(null, "Referenceable");
            results.put("Referenceable", atlasTypeNamesByPrefix);
        }

        // If the requested type is one that is not directly mapped, we need to scan whether we have implemented
        // any of its sub-types to include in the results
        if (requestedTypeName != null) {
            try {
                List<TypeDef> allEntityTypes = new ArrayList<>();//findTypeDefsByCategory(userId, TypeDefCategory.ENTITY_DEF);
                for (TypeDef typeDef : allEntityTypes) {
                    String typeDefName = typeDef.getName();
                    if (!typeDefName.equals("Referenceable") && repositoryHelper.isTypeOf(metadataCollectionId, typeDefName, requestedTypeName)) {
                        Map<String, String> mappings = typeDefStore.getAllMappedCatalogTypeDefNames(typeDefName);
                        if (!mappings.isEmpty()) {
                            results.put(typeDefName, mappings);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("An invalid parameter was provided to findTypeDefsByCategory, cannot determine types to include in search.", e);
            }
        }

        return results;

    }


    /**
     * Sort the list of results and limit based on the provided parameters.
     *
     * @param results the Apache Atlas results to sort and limit
     * @param entityTypeGUID the type of entity that was requested (or null for all)
     * @param fromElement the starting element to include in the limited results
     * @param sequencingProperty the property by which to sort the results (or null, if not sorting by property)
     * @param sequencingOrder the order by which to sort the results
     * @param pageSize the number of results to include in this page
     * @param userId the user through which to translate the results
     * @return
     * @throws InvalidParameterException the guid is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    private List<EntityDetail> sortAndLimitFinalResults(List<Instance> results,
                                                        String entityTypeGUID,
                                                        int fromElement,
                                                        String sequencingProperty,
                                                        SequencingOrder sequencingOrder,
                                                        int pageSize,
                                                        String userId) throws
            InvalidParameterException,
            RepositoryErrorException,
            UserNotAuthorizedException {

        // If no entity type GUID was provided (search was done with 'null' originally for all types), then set it here
        // to the GUID for Referenceable, so that we can properly do subtype checking in the subsequent steps.
        if (entityTypeGUID == null) {
            entityTypeGUID = typeDefStore.getTypeDefByName("Referenceable").getGUID();
        }
        List<EntityDetail> totalResults = new ArrayList<>(getEntityDetailsFromCatalogResults(results, entityTypeGUID, userId));
        // TODO: send something in that determines whether re-sorting the results is actually necessary?
        // Need to potentially re-sort and re-limit the results, if we ran the search against more than one type
        Comparator<EntityDetail> comparator = SequencingUtils.getEntityDetailComparator(sequencingOrder, sequencingProperty);
        if (comparator != null) {
            totalResults.sort(comparator);
        }
        int endOfPageMarker = Math.min(fromElement + pageSize, totalResults.size());
        if ((fromElement != 0 || endOfPageMarker < totalResults.size()) && (endOfPageMarker != 0)) {
            totalResults = totalResults.subList(fromElement, endOfPageMarker);
        }
        return totalResults;
    }

    /**
     * Retrieves a list of EntityDetail objects given a list of AtlasEntityHeader objects.
     *
     * @param instances the Atlas entities for which to retrieve details
     * @param entityTypeGUID the type of entity that was requested (or null for all)
     * @param userId the user through which to do the retrieval
     * @return {@code List<EntityDetail>}
     * @throws InvalidParameterException the guid is null.
     * @throws RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored.
     * @throws UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    private List<EntityDetail> getEntityDetailsFromCatalogResults(List<Instance> instances,
                                                                    String entityTypeGUID,
                                                                    String userId) throws
            InvalidParameterException,
            RepositoryErrorException,
            UserNotAuthorizedException {

        List<EntityDetail> entityDetails = new ArrayList<>();
        if (instances != null) {
            for (Instance instance : instances) {
                try {
                    // TODO: See if we can do this without making another REST request
                    EntityDetail detail = getEntityDetail(userId, instance.getId());
                    if (detail != null) {
                            String typeName = detail.getType().getTypeDefName();
                            log.debug("getEntityDetailsFromCatalogResults: typeName {}", typeName);
                            try {
                                TypeDef typeDef = repositoryHelper.getTypeDef(repositoryName, "entityTypeGUID", entityTypeGUID, "getEntityDetailsFromAtlasResults");
                                if (repositoryHelper.isTypeOf(repositoryName, typeName, typeDef.getName())) {
                                    entityDetails.add(detail);
                                }
                            } catch (TypeErrorException e) {
                                log.error("Unable to find any TypeDef for entityTypeGUID: {}", entityTypeGUID);
                            }
                        } else {
                            log.error("Entity with GUID {} not known -- excluding from results.", instance.getId());
                        }
                } catch (EntityNotKnownException e) {
                        log.error("Entity with GUID {} not known -- excluding from results.", instance.getId());
                }
            }
        }
        return entityDetails;

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


    /**
     * Throw a RepositoryErrorException using the provided parameters.
     * @param errorCode the error code for the exception
     * @param methodName the method throwing the exception
     * @param cause the underlying cause of the exception (if any, otherwise null)
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
