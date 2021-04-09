//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.repository.connector.mapping;

import org.odpi.openmetadata.connector.sas.auditlog.ErrorCode;
import org.odpi.openmetadata.connector.sas.repository.connector.RepositoryConnector;
import org.odpi.openmetadata.connector.sas.repository.connector.model.SASCatalogGuid;
import org.odpi.openmetadata.connector.sas.repository.connector.stores.AttributeTypeDefStore;
import org.odpi.openmetadata.connector.sas.repository.connector.stores.TypeDefStore;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityProxy;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceProperties;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceProvenanceType;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceStatus;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceType;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.Relationship;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.RelationshipDef;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDefAttribute;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.RepositoryErrorException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.TypeErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * The base class for all mappings between OMRS Relationship instances and SAS relationship instances.
 */
public class RelationshipMapping {

    private static final Logger log = LoggerFactory.getLogger(RelationshipMapping.class);

    private RepositoryConnector SASRepositoryConnector;
    private TypeDefStore typeDefStore;
    private AttributeTypeDefStore attributeDefStore;
    private SASCatalogGuid sasCatalogGuid;
    private SASCatalogObject relationship;
    private String userId;

    /**
     * Mapping itself must be initialized with various objects.
     *
     * @param SASRepositoryConnector connectivity to an SAS repository
     * @param typeDefStore the store of mapped TypeDefs for the SAS repository
     * @param attributeDefStore the store of mapped AttributeTypeDefs for the SAS repository
     * @param sasCatalogGuid the GUID that was used to retrieve this relationship
     * @param relationship the SAS relationship to be mapped
     * @param userId the user through which to do the mapping
     */
    public RelationshipMapping(RepositoryConnector SASRepositoryConnector,
                               TypeDefStore typeDefStore,
                               AttributeTypeDefStore attributeDefStore,
                               SASCatalogGuid sasCatalogGuid,
                               SASCatalogObject relationship,
                               String userId) {
        this.SASRepositoryConnector = SASRepositoryConnector;
        this.typeDefStore = typeDefStore;
        this.attributeDefStore = attributeDefStore;
        this.sasCatalogGuid = sasCatalogGuid;
        this.relationship = relationship;
        this.userId = userId;
    }

    /**
     * Retrieve the mapped OMRS Relationship from the SAS relationship used to construct this mapping object.
     *
     * @return Relationship
     * @throws RepositoryErrorException when unable to retrieve the Relationship
     */
    public Relationship getRelationship() throws RepositoryErrorException {

        final String methodName = "getRelationship";
        String repositoryName = SASRepositoryConnector.getRepositoryName();

        String SASRelationshipType = relationship.getTypeName();
        String relationshipPrefix = sasCatalogGuid.getGeneratedPrefix();

        TypeDefStore.EndpointMapping mapping = typeDefStore.getEndpointMappingFromCatalogName(SASRelationshipType, relationshipPrefix);

        String ep1Id = (String) relationship.get("instance.endpoint1Id");
        String ep2Id = (String) relationship.get("instance.endpoint2Id");

        EntityProxy ep1 = null;
        EntityProxy ep2 = null;

        Relationship omrsRelationship = null;
        try {
            ep1 = RelationshipMapping.getEntityProxyForObject(
                    SASRepositoryConnector,
                    typeDefStore,
                    SASRepositoryConnector.getEntityByGUID(ep1Id),
                    mapping.getPrefixOne(),
                    userId
            );
        } catch (Exception e) {
            raiseRepositoryErrorException(ErrorCode.ENTITY_NOT_KNOWN, methodName, e, ep1Id, methodName, repositoryName);
        }
        try {
            ep2 = RelationshipMapping.getEntityProxyForObject(
                    SASRepositoryConnector,
                    typeDefStore,
                    SASRepositoryConnector.getEntityByGUID(ep2Id),
                    mapping.getPrefixTwo(),
                    userId
            );
        } catch (Exception e) {
            raiseRepositoryErrorException(ErrorCode.ENTITY_NOT_KNOWN, methodName, e, ep2Id, methodName, repositoryName);
        }

        if (ep1 != null && ep2 != null) {
            omrsRelationship = getRelationship(
                    SASRelationshipType,
                    ep1,
                    ep2);
        }

        return omrsRelationship;

    }

    /**
     * Create a mapped relationship based on the provided criteria
     *
     * @param SASRelationshipType the type of the SAS relationship to map
     * @param ep1 the proxy to map to endpoint 1
     * @param ep2 the proxy to map to endpoint 2
     * @return Relationship
     * @throws RepositoryErrorException when unable to map the Relationship
     */
    private Relationship getRelationship(String SASRelationshipType,
                                         EntityProxy ep1,
                                         EntityProxy ep2) throws RepositoryErrorException {

        final String methodName = "getRelationship";
        OMRSRepositoryHelper omrsRepositoryHelper = SASRepositoryConnector.getRepositoryHelper();
        String repositoryName = SASRepositoryConnector.getRepositoryName();

        String omrsRelationshipType = typeDefStore.getMappedOMRSTypeDefName(SASRelationshipType, sasCatalogGuid.getGeneratedPrefix());

        InstanceStatus omrsRelationshipStatus = InstanceStatus.ACTIVE;

        Map<String, Object> SASRelationshipProperties = relationship.getAttributes();
        InstanceProperties omrsRelationshipProperties = new InstanceProperties();
        if (SASRelationshipProperties != null) {

            Map<String, TypeDefAttribute> relationshipAttributeMap = typeDefStore.getAllTypeDefAttributesForName(omrsRelationshipType);
            Map<String, String> SASToOmrsProperties = typeDefStore.getPropertyMappingsForCatalogTypeDef(SASRelationshipType, sasCatalogGuid.getGeneratedPrefix());
            if (SASToOmrsProperties != null) {

                for (Map.Entry<String, String> property : SASToOmrsProperties.entrySet()) {
                    String SASProperty = property.getKey();
                    String omrsProperty = property.getValue();
                    if (relationshipAttributeMap.containsKey(omrsProperty)) {
                        TypeDefAttribute typeDefAttribute = relationshipAttributeMap.get(omrsProperty);
                        omrsRelationshipProperties = AttributeMapping.addPropertyToInstance(omrsRepositoryHelper,
                                repositoryName,
                                typeDefAttribute,
                                omrsRelationshipProperties,
                                /*attributeDefStore,*/
                                SASRelationshipProperties.get(SASProperty),
                                methodName);
                    } else {
                        log.warn("No OMRS attribute {} defined for asset type {} -- skipping mapping.", omrsProperty, omrsRelationshipType);
                    }
                }

            }

        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

        try {
            return RelationshipMapping.getRelationship(
                    SASRepositoryConnector,
                    typeDefStore,
                    omrsRelationshipType,
                    sasCatalogGuid,
                    omrsRelationshipStatus,
                    ep1,
                    ep2,
                    (String) relationship.get("instance.createdBy"),
                    (String) relationship.get("instance.modifiedBy"),
                    format.parse((String) relationship.get("instance.creationTimeStamp")),
                    format.parse((String) relationship.get("instance.modifiedTimeStamp")),
                    omrsRelationshipProperties);
        } catch (ParseException e) {
            log.error("Could not parse relationship timestamp");
            return null;
        }

    }

    /**
     * Setup a self-referencing relationship using the provided prefix and SAS entity.
     *
     * @param SASRepositoryConnector connectivity to a SAS environment
     * @param typeDefStore store of TypeDef mappings
     * @param relationshipGUID the GUID of the relationship
     * @param entity the entity for which the self-referencing relationship should be generated
     * @return Relationship
     * @throws RepositoryErrorException when unable to map the Relationship
     */
    public static Relationship getSelfReferencingRelationship(RepositoryConnector SASRepositoryConnector,
                                                              TypeDefStore typeDefStore,
                                                              SASCatalogGuid relationshipGUID,
                                                              SASCatalogObject entity) throws RepositoryErrorException {

        Relationship omrsRelationship = null;
        String prefix = relationshipGUID.getGeneratedPrefix();
        if (prefix != null) {

            RelationshipDef relationshipDef = (RelationshipDef) typeDefStore.getTypeDefByPrefix(prefix);
            String omrsRelationshipType = relationshipDef.getName();
            TypeDefStore.EndpointMapping mapping = typeDefStore.getEndpointMappingFromCatalogName(entity.getTypeName(), prefix);
            EntityProxy ep1 = RelationshipMapping.getEntityProxyForObject(
                    SASRepositoryConnector,
                    typeDefStore,
                    entity,
                    mapping.getPrefixOne(),
                    null
            );
            EntityProxy ep2 = RelationshipMapping.getEntityProxyForObject(
                    SASRepositoryConnector,
                    typeDefStore,
                    entity,
                    mapping.getPrefixTwo(),
                    null
            );
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
            // TODO: assumes that properties on a self-generated relationship are always empty
            try {
                omrsRelationship = getRelationship(SASRepositoryConnector,
                        typeDefStore,
                        omrsRelationshipType,
                        relationshipGUID,
                        InstanceStatus.ACTIVE,
                        ep1,
                        ep2,
                        (String) entity.get("instance.createdBy"),
                        (String) entity.get("instance.modifiedBy"),
                        format.parse((String) entity.get("instance.creationTimeStamp")),
                        format.parse((String) entity.get("instance.modifiedTimeStamp")),
                        new InstanceProperties());
            } catch (ParseException e) {
                log.error("Relationship entity timestamp could not be parsed");
            }
        } else {
            log.error("A self-referencing, generated relationship was requested, but there is no prefix: {}", relationshipGUID);
        }

        return omrsRelationship;

    }

    /**
     * Create a mapped relationship based on the provided criteria
     *
     * @param SASRepositoryConnector connectivity to an SAS environment
     * @param typeDefStore store of TypeDef mappings
     * @param omrsRelationshipType the type of the OMRS relationship to map
     * @param relationshipGUID the GUID of the relationship
     * @param relationshipStatus the status of the relationship
     * @param ep1 the proxy to map to endpoint 1
     * @param ep2 the proxy to map to endpoint 2
     * @param createdBy the relationship creator
     * @param updatedBy the relationship updator
     * @param createTime the time the relationship was created
     * @param updateTime the time the relationship was updated
     * @param omrsRelationshipProperties the properties to set on the relationship
     * @return Relationship
     * @throws RepositoryErrorException when unable to map the Relationship
     */
    private static Relationship getRelationship(RepositoryConnector SASRepositoryConnector,
                                                TypeDefStore typeDefStore,
                                                String omrsRelationshipType,
                                                SASCatalogGuid relationshipGUID,
                                                InstanceStatus relationshipStatus,
                                                EntityProxy ep1,
                                                EntityProxy ep2,
                                                String createdBy,
                                                String updatedBy,
                                                Date createTime,
                                                Date updateTime,
                                                InstanceProperties omrsRelationshipProperties) throws RepositoryErrorException {

        final String methodName = "getRelationship";
        String repositoryName = SASRepositoryConnector.getRepositoryName();

        Relationship omrsRelationship = RelationshipMapping.getSkeletonRelationship(
                SASRepositoryConnector,
                (RelationshipDef) typeDefStore.getTypeDefByName(omrsRelationshipType)
        );

        omrsRelationship.setGUID(relationshipGUID.toString());
        omrsRelationship.setMetadataCollectionId(SASRepositoryConnector.getMetadataCollectionId());
        omrsRelationship.setStatus(relationshipStatus);
        omrsRelationship.setInstanceProvenanceType(InstanceProvenanceType.LOCAL_COHORT);
        omrsRelationship.setVersion(updateTime.getTime());
        omrsRelationship.setCreateTime(createTime);
        omrsRelationship.setCreatedBy(createdBy);
        omrsRelationship.setUpdatedBy(updatedBy);
        omrsRelationship.setUpdateTime(updateTime);

        if (ep1 != null && ep2 != null) {
            omrsRelationship.setEntityOneProxy(ep1);
            omrsRelationship.setEntityTwoProxy(ep2);
        } else {
            throw new RepositoryErrorException(ErrorCode.INVALID_RELATIONSHIP_ENDS.getMessageDefinition(methodName, repositoryName, omrsRelationshipType, ep1 == null ? "null" : ep1.getGUID(), ep2 == null ? "null" : ep2.getGUID()),
                    RelationshipMapping.class.getName(),
                    methodName);
        }

        if (omrsRelationshipProperties != null) {
            omrsRelationship.setProperties(omrsRelationshipProperties);
        }

        return omrsRelationship;

    }

    /**
     * Retrieves an EntityProxy object for the provided SAS object.
     *
     * @param SASRepositoryConnector OMRS connector to the SAS repository
     * @param typeDefStore store of mapped TypeDefs
     * @param SASObj the SAS object for which to retrieve an EntityProxy
     * @param entityPrefix the prefix used for the entity, if it is a generated entity (null if not generated)
     * @param userId the user through which to retrieve the EntityProxy (unused)
     * @return EntityProxy
     */
    public static EntityProxy getEntityProxyForObject(RepositoryConnector SASRepositoryConnector,
                                                      TypeDefStore typeDefStore,
                                                      SASCatalogObject SASObj,
                                                      String entityPrefix,
                                                      String userId) {

        final String methodName = "getEntityProxyForObject";

        EntityProxy entityProxy = null;
        if (SASObj != null) {

            String repositoryName = SASRepositoryConnector.getRepositoryName();
            OMRSRepositoryHelper repositoryHelper = SASRepositoryConnector.getRepositoryHelper();
            String metadataCollectionId = SASRepositoryConnector.getMetadataCollectionId();

            String SASTypeName = SASObj.getTypeName();
            String omrsTypeDefName = typeDefStore.getMappedOMRSTypeDefName(SASTypeName, entityPrefix);

            String qualifiedName;
            Map<String, Object> attributes = SASObj.getAttributes();
            if (SASObj.get("instance.name") != null) {
                qualifiedName = (String) SASObj.get("instance.name");
            } else {
                log.error("No qualifiedName found for object -- cannot create EntityProxy: {}", SASObj);
                throw new NullPointerException("No qualifiedName found for object -- cannot create EntityProxy.");
            }

            InstanceProperties uniqueProperties = repositoryHelper.addStringPropertyToInstance(
                    repositoryName,
                    null,
                    "qualifiedName",
                    qualifiedName,
                    methodName
            );

            try {
                entityProxy = repositoryHelper.getNewEntityProxy(
                        repositoryName,
                        metadataCollectionId,
                        InstanceProvenanceType.LOCAL_COHORT,
                        userId,
                        omrsTypeDefName,
                        uniqueProperties,
                        null
                );
                SASCatalogGuid sasCatalogGuid = new SASCatalogGuid(SASObj.getGuid(), entityPrefix);
                entityProxy.setGUID(sasCatalogGuid.toString());
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
                entityProxy.setCreatedBy((String) SASObj.get("instance.createdBy"));
                entityProxy.setUpdatedBy((String) SASObj.get("instance.modifiedBy"));
                entityProxy.setVersion((long) ((double) SASObj.get("instance.version")));
                entityProxy.setCreateTime(format.parse((String) SASObj.get("instance.creationTimeStamp")));
                entityProxy.setUpdateTime(format.parse((String) SASObj.get("instance.modifiedTimeStamp")));
            } catch (TypeErrorException e) {
                log.error("Unable to create new EntityProxy.", e);
            } catch (ParseException e) {
                log.error("Unable to create new EntityProxy due to timestamp parse error", e);
            }

        } else {
            log.error("No SAS object provided (was null).");
        }

        return entityProxy;

    }

    /**
     * Create the base skeleton of a Relationship, irrespective of the specific SAS object.
     *
     * @param SASRepositoryConnector connectivity to an SAS environment
     * @param omrsRelationshipDef the OMRS RelationshipDef for which to create a skeleton Relationship
     * @return Relationship
     * @throws RepositoryErrorException when unable to create a new skeletal Relationship
     */
    static Relationship getSkeletonRelationship(RepositoryConnector SASRepositoryConnector,
                                                RelationshipDef omrsRelationshipDef) throws RepositoryErrorException {

        final String methodName = "getSkeletonRelationship";
        Relationship relationship = new Relationship();

        try {
            InstanceType instanceType = SASRepositoryConnector.getRepositoryHelper().getNewInstanceType(
                    SASRepositoryConnector.getRepositoryName(),
                    omrsRelationshipDef
            );
            relationship.setType(instanceType);
        } catch (TypeErrorException e) {
            throw new RepositoryErrorException(ErrorCode.INVALID_INSTANCE.getMessageDefinition(methodName, omrsRelationshipDef.getName()),
                    RelationshipMapping.class.getName(),
                    methodName,
                    e);
        }

        return relationship;

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
        throw new RepositoryErrorException(errorCode.getMessageDefinition(params),
                RelationshipMapping.class.getName(),
                methodName,
                cause);
    }

}
