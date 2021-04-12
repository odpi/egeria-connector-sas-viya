//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.repository.connector.mapping;

import org.odpi.openmetadata.connector.sas.auditlog.ErrorCode;
import org.odpi.openmetadata.connector.sas.repository.connector.RepositoryConnector;
import org.odpi.openmetadata.connector.sas.repository.connector.model.SASCatalogGuid;
import org.odpi.openmetadata.connector.sas.repository.connector.stores.TypeDefStore;
import org.apache.commons.lang3.StringUtils;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.SequencingOrder;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntitySummary;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceProperties;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceProvenanceType;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceStatus;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.Relationship;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDef;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDefAttribute;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.RepositoryErrorException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.TypeErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class that generically handles converting an sas EntityInstance object into an OMRS EntityDetail object.
 */
public class EntityMappingSASCatalog2OMRS {

    private static final Logger log = LoggerFactory.getLogger(EntityMappingSASCatalog2OMRS.class);

    private static final String SASPROPERTY_CONSTANT_PREFIX = "constant.";
    private static final String OMRSPROPERTY_ADDITIONALPROPERTIES_PREFIX = "additionalProperties.";
    private RepositoryConnector sasRepositoryConnector;
    private TypeDefStore typeDefStore;
    //    private AttributeTypeDefStore attributeDefStore;
//    private SASCatalogObject.CatalogEntityWithExtInfo sasEntityWithExtInfo;
    private SASCatalogObject sasEntity;
    private String prefix;
    private String userId;

    /**
     * Mapping itself must be initialized with various objects.
     *
     * @param sasRepositoryConnector connectivity to an sas repository
     * @param typeDefStore           the store of mapped TypeDefs for the Sas repository
     * @param attributeDefStore      the store of mapped AttributeTypeDefs for the Sas repository
     * @param instance               the Sas entity to be mapped
     * @param prefix                 the prefix indicating a generated type (and GUID), or null if not generated
     * @param userId                 the user through which to do the mapping
     */
    public EntityMappingSASCatalog2OMRS(RepositoryConnector sasRepositoryConnector,
                                        TypeDefStore typeDefStore,
                                        Object attributeDefStore,
                                        SASCatalogObject instance,
                                        String prefix,
                                        String userId) {
        this.sasRepositoryConnector = sasRepositoryConnector;
        this.typeDefStore = typeDefStore;
//        this.attributeDefStore = attributeDefStore;
        this.sasEntity = instance;
        this.prefix = prefix;
        this.userId = userId;
    }

    /**
     * Retrieve the mapped OMRS EntitySummary from the sas EntityInstance used to construct this mapping object.
     *
     * @return EntitySummary
     * @throws RepositoryErrorException when unable to retrieve the EntitySummary
     */
    public EntitySummary getEntitySummary() throws RepositoryErrorException {

        String sasTypeDefName = sasEntity.getTypeName();
        String omrsTypeDefName = typeDefStore.getMappedOMRSTypeDefName(sasTypeDefName, prefix);
        log.info("Found mapped type for Sas type '{}' with prefix '{}': {}", sasTypeDefName, prefix, omrsTypeDefName);

        EntitySummary summary = null;
        if (omrsTypeDefName != null) {
            summary = getSkeletonEntitySummary(omrsTypeDefName, prefix);
            if (summary != null) {
                addClassifications(summary);
            }
        } else {
            log.warn("No mapping defined from Sas type '{}' with prefix '{}'", sasTypeDefName, prefix);
        }

        return summary;

    }

    /**
     * Retrieve the mapped OMRS EntityDetail from the sas EntityInstance used to construct this mapping object.
     *
     * @return EntityDetail
     * @throws RepositoryErrorException when unable to retrieve the EntityDetail
     */
    public EntityDetail getEntityDetail() throws RepositoryErrorException {

        final String attribute = "attribute";
        final String methodName = "getEntityDetail";
        String sasTypeDefName = sasEntity.getTypeName();
        String omrsTypeDefName = typeDefStore.getMappedOMRSTypeDefName(sasTypeDefName, prefix);
        log.info("Found mapped type for Sas type '{}' with prefix '{}': {}", sasTypeDefName, prefix, omrsTypeDefName);

        EntityDetail detail = null;
        if (omrsTypeDefName != null) {

            // Create the basic skeleton
            detail = getSkeletonEntityDetail(omrsTypeDefName, prefix);

            // Then apply the instance-specific mapping
            if (detail != null) {
                InstanceProperties instanceProperties = new InstanceProperties();
                Map<String, String> additionalProperties = new HashMap<>();
                OMRSRepositoryHelper omrsRepositoryHelper = sasRepositoryConnector.getRepositoryHelper();
                String repositoryName = sasRepositoryConnector.getRepositoryName();

                Map<String, TypeDefAttribute> omrsAttributeMap = typeDefStore.getAllTypeDefAttributesForName(omrsTypeDefName);

                // Iterate through the provided mappings to set an OMRS instance property for each one
                Map<String, String> sasToOmrsProperties = typeDefStore.getPropertyMappingsForCatalogTypeDef(sasTypeDefName, prefix);

                if (sasEntity != null) {
                    Set<String> alreadyMapped = new HashSet<>();
                    for (Map.Entry<String, String> property : sasToOmrsProperties.entrySet()) {
                        String sasProperty = property.getKey();
                        String omrsProperty = property.getValue();

                        // If omrsProperty is of the form "additionalProperties.xxxxx" then extract "xxxxx" as the
                        // name of the property to add under additionalProperties (and extract value and actually
                        // add the entry in the following IF block
                        String additionalPropertyName = "";
                        if (omrsProperty.startsWith(OMRSPROPERTY_ADDITIONALPROPERTIES_PREFIX)) {
                            additionalPropertyName = omrsProperty.substring(OMRSPROPERTY_ADDITIONALPROPERTIES_PREFIX.length());
                        }

                        if (sasProperty.startsWith(SASPROPERTY_CONSTANT_PREFIX)) {
                            String constantVal = sasProperty.substring(SASPROPERTY_CONSTANT_PREFIX.length());
                            log.info("Adding constant value: '" + constantVal + "' for property " + omrsProperty);
                            if (StringUtils.isNotEmpty(additionalPropertyName)) {
                                additionalProperties.put(additionalPropertyName, constantVal);
                            } else {
                            instanceProperties = omrsRepositoryHelper.addStringPropertyToInstance(repositoryName,
                                    instanceProperties, omrsProperty, constantVal, methodName);
                            }
                        } else if (StringUtils.isNotEmpty(additionalPropertyName))  {
                            log.info("Mapping {} to additionalProperties '{}'", sasProperty, omrsProperty);
                            Object propertyValue = sasEntity.get(sasProperty);
                            if (propertyValue != null) {
                                additionalProperties.put(additionalPropertyName, propertyValue.toString());
                            } else {
                                log.warn("Null property value for SAS property '{}'.", sasProperty);
                            }
                        } else if (omrsAttributeMap.containsKey(omrsProperty)) {
                            log.info("Mapping {} to {}", sasProperty, omrsProperty);
                            TypeDefAttribute typeDefAttribute = omrsAttributeMap.get(omrsProperty);
                            instanceProperties = AttributeMapping.addPropertyToInstance(omrsRepositoryHelper,
                                    repositoryName,
                                    typeDefAttribute,
                                    instanceProperties,
                                    sasEntity.get(sasProperty),
                                    methodName);
                            if (instanceProperties.getPropertyValue(omrsProperty) != null) {
                                if(sasProperty.startsWith(attribute)){
                                    sasProperty = sasProperty.substring(attribute.length());
                                }
                                alreadyMapped.add(sasProperty);
                            }
                        } else {
                            log.warn("No OMRS attribute {} defined for asset type {} -- skipping mapping.", omrsProperty, omrsTypeDefName);
                        }
                    }

                    // And map any other simple (non-relationship) properties that are not otherwise mapped into 'additionalProperties'

                    Set<String> nonRelationshipSet = sasEntity.getAttributes().keySet();

                    // Remove all of the already-mapped properties from our list of non-relationship properties
                    nonRelationshipSet.removeAll(alreadyMapped);

                    // Iterate through the remaining property names, and add them to a map
                    // Note that because 'additionalProperties' is a string-to-string map, we will just convert everything
                    // to strings (even arrays of values, we'll concatenate into a single string)
                    for (String propertyName : nonRelationshipSet) {
                        Object propertyValue = sasEntity.getAttributes().get(propertyName);
                        if (propertyValue != null) {
                            additionalProperties.put(propertyName, propertyValue.toString());
                        }
                    }

                    // and finally setup the 'additionalProperties' attribute using this map
                    instanceProperties = omrsRepositoryHelper.addStringMapPropertyToInstance(
                            repositoryName,
                            instanceProperties,
                            "additionalProperties",
                            additionalProperties,
                            methodName
                    );
                }

                detail.setProperties(instanceProperties);

                // TODO: detail.setReplicatedBy();
                addClassifications(detail);

            }
        } else {
            log.warn("No mapping defined from Sas type '{}' with prefix '{}'", sasTypeDefName, prefix);
        }

        return detail;

    }

    /**
     * Retrieves relationships for this entity based on the provided criteria.
     *
     * @param relationships           the Catalog objects for which we wish to return relationships
     * @param relationshipTypeGUID    the OMRS GUID of the relationship TypeDef to which to limit the results
     * @param fromRelationshipElement the starting element for multiple pages of relationships
     * @param sequencingProperty      the property by which to order results (or null)
     * @param sequencingOrder         the ordering sequence to use for ordering results
     * @param pageSize                the number of results to include per page
     * @return {@code List<Relationship>}
     * @throws RepositoryErrorException when unable to retrieve the mapped Relationships
     */
    @SuppressWarnings("unchecked")
    public List<Relationship> getRelationships(List<SASCatalogObject> relationships,
                                               String relationshipTypeGUID,
                                               int fromRelationshipElement,
                                               String sequencingProperty,
                                               SequencingOrder sequencingOrder,
                                               int pageSize) throws RepositoryErrorException {

        final String methodName = "getRelationships";
        List<Relationship> omrsRelationships = new ArrayList<>();
        String repositoryName = sasRepositoryConnector.getRepositoryName();

        for(SASCatalogObject relationship : relationships) {
            String catalogRelationshipType = relationship.getTypeName();
            String relationshipGuid = relationship.getGuid();

            Map<String, String> omrsPrefixToType = typeDefStore.getMappedOMRSTypeDefNameWithPrefixes(catalogRelationshipType);

            for (Map.Entry<String, String> entry : omrsPrefixToType.entrySet()) {

                String relationshipPrefix = entry.getKey();
                String omrsRelationshipType = entry.getValue();

                TypeDef omrsTypeDef = typeDefStore.getTypeDefByName(omrsRelationshipType);
                String omrsTypeDefGuid = omrsTypeDef.getGUID();

                // Only include the relationship if we are including all or those that match this type GUID
                if (relationshipTypeGUID == null || omrsTypeDefGuid.equals(relationshipTypeGUID)) {

                    try {
                        RelationshipMapping mapping = new RelationshipMapping(
                                sasRepositoryConnector,
                                typeDefStore,
                                null,
                                new SASCatalogGuid(relationshipGuid, relationshipPrefix),
                                relationship,
                                userId);

                        Relationship omrsRelationship = mapping.getRelationship();
                        if (omrsRelationship != null) {
                            omrsRelationships.add(omrsRelationship);
                        }
                    } catch (Exception e) {
                        raiseRepositoryErrorException(ErrorCode.RELATIONSHIP_NOT_KNOWN, methodName, e, relationshipGuid, methodName, repositoryName);
                    }

                }
            }
        }

        // Now sort the results, if requested
        Comparator<Relationship> comparator = SequencingUtils.getRelationshipComparator(sequencingOrder, sequencingProperty);
        if (comparator != null) {
            omrsRelationships.sort(comparator);
        }

        // And finally limit the results, if requested
        int endOfPageMarker = Math.min(fromRelationshipElement + pageSize, omrsRelationships.size());
        if (fromRelationshipElement != 0 || endOfPageMarker < omrsRelationships.size()) {
            omrsRelationships = omrsRelationships.subList(fromRelationshipElement, endOfPageMarker);
        }

        return (omrsRelationships.isEmpty() ? null : omrsRelationships);
    }

    /**
     * Create the base skeleton of an EntitySummary, irrespective of the specific sas object.
     *
     * @param omrsTypeDefName the name of the OMRS TypeDef for which to create a skeleton EntitySummary
     * @param prefix          the prefix for a generated entity (if any)
     * @return EntitySummary
     */
    private EntitySummary getSkeletonEntitySummary(String omrsTypeDefName, String prefix) {

        EntitySummary summary = null;
        try {
            summary = sasRepositoryConnector.getRepositoryHelper().getSkeletonEntitySummary(
                    sasRepositoryConnector.getRepositoryName(),
                    sasRepositoryConnector.getMetadataCollectionId(),
                    InstanceProvenanceType.LOCAL_COHORT,
                    userId,
                    omrsTypeDefName
            );
            String guid = sasEntity.getGuid();
            SASCatalogGuid sasCatalogGuid = new SASCatalogGuid(guid, prefix);
            summary.setGUID(sasCatalogGuid.toString());
            summary.setInstanceURL(getInstanceURL(guid));
            setModAndVersionDetails(summary);
        } catch (TypeErrorException e) {
            log.error("Unable to get skeleton summary entity.", e);
        }

        return summary;

    }

    /**
     * Create the base skeleton of an EntityDetail, irrespective of the specific sas object.
     *
     * @param omrsTypeDefName the name of the OMRS TypeDef for which to create a skeleton EntityDetail
     * @param prefix          the prefix for a generated entity (if any)
     * @return EntityDetail
     */
    private EntityDetail getSkeletonEntityDetail(String omrsTypeDefName, String prefix) {

        EntityDetail detail = null;
        try {
            detail = sasRepositoryConnector.getRepositoryHelper().getSkeletonEntity(
                    sasRepositoryConnector.getRepositoryName(),
                    sasRepositoryConnector.getMetadataCollectionId(),
                    InstanceProvenanceType.LOCAL_COHORT,
                    userId,
                    omrsTypeDefName
            );
            String guid = sasEntity.getGuid();
            SASCatalogGuid sasCatalogGuid = new SASCatalogGuid(guid, prefix);
            detail.setGUID(sasCatalogGuid.toString());
            detail.setInstanceURL(getInstanceURL(guid));
            detail.setStatus(InstanceStatus.ACTIVE);
            setModAndVersionDetails(detail);
        } catch (TypeErrorException e) {
            log.error("Unable to get skeleton detail entity.", e);
        }

        return detail;

    }

    /**
     * Retrieve an API-accessible instance URL based on the GUID of an entity.
     *
     * @param guid the guid of the entity
     * @return String
     */
    private String getInstanceURL(String guid) {
        return sasRepositoryConnector.getBaseURL() + RepositoryConnector.EP_ENTITY + guid;
    }

    /**
     * Set the creation, modification and version details of the object.
     *
     * @param omrsObj the OMRS object (EntitySummary or EntityDetail)
     */
    private void setModAndVersionDetails(EntitySummary omrsObj) {


        omrsObj.setCreatedBy((String) sasEntity.get("instance.createdBy"));
        omrsObj.setUpdatedBy((String) sasEntity.get("instance.modifiedBy"));
        Number version = (Number)sasEntity.get("instance.version");
        omrsObj.setVersion(version.longValue());


        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
        try {
            Object creationTimeStampObj = sasEntity.get("instance.creationTimeStamp");
            Object modifiedTimeStampObj = sasEntity.get("instance.modifiedTimeStamp");
            if (creationTimeStampObj instanceof Date) {
                omrsObj.setCreateTime((Date)creationTimeStampObj);
            } else {
                omrsObj.setCreateTime(format.parse(creationTimeStampObj.toString()));
            }
            if (modifiedTimeStampObj instanceof Date) {
                omrsObj.setUpdateTime((Date)modifiedTimeStampObj);
            } else {
                omrsObj.setUpdateTime(format.parse(modifiedTimeStampObj.toString()));
            }
        } catch (ParseException e) {
            log.error("Could not parse instance timestamp");
        }
    }

    /**
     * Add any classifications: since Sas does not come with any pre-defined Classifications we will assume
     * that any that exist are OMRS-created and therefore are one-to-one mappings to OMRS classifications
     * (but we will check that the classification is a known OMRS classification before proceeding)
     *
     * @param omrsObj the OMRS object (EntitySummary or EntityDetail)
     * @throws RepositoryErrorException when unable to add the classifications
     */
    private void addClassifications(EntitySummary omrsObj) throws RepositoryErrorException {
        //TODO: Add mapping of classifications
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
