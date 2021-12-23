//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.repository.connector.stores;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.odpi.openmetadata.connector.sas.repository.connector.mapping.MappingFromFile;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDef;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDefAttribute;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDefLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Store of implemented TypeDefs for the repository.
 * Source: https://github.com/odpi/egeria-connector-hadoop-ecosystem/blob/master/apache-atlas-adapter/src/main/java/org/odpi/egeria/connectors/apache/atlas/repositoryconnector/stores/TypeDefStore.java
 */
public class TypeDefStore {

    private static final Logger log = LoggerFactory.getLogger(TypeDefStore.class);

    // About the OMRS TypeDefs themselves
    private Map<String, TypeDef> omrsGuidToTypeDef;
    private Map<String, String> omrsNameToGuid;
    private Map<String, Map<String, TypeDefAttribute>> omrsGuidToAttributeMap;
    private Map<String, TypeDef> unimplementedTypeDefs;

    // Mapping details
    private Map<String, String> prefixToOmrsTypeName;
    private Map<String, Map<String, String>> omrsNameToCatalogNamesByPrefix;
    private Map<String, Map<String, String>> catalogNameToOmrsNamesByPrefix;
    private Map<String, Map<String, Map<String, String>>> omrsNameToAttributeMapByPrefix;
    private Map<String, Map<String, Map<String, String>>> catalogNameToAttributeMapByPrefix;
    private Map<String, Map<String, EndpointMapping>> omrsNameToEndpointMapByPrefix;
    private Map<String, Map<String, EndpointMapping>> catalogNameToEndpointMapByPrefix;
    private Set<String> superTypesToAdd;

    private Set<String> unmappedTypes;

    private ObjectMapper mapper;

    public enum Endpoint {
        ONE, TWO, UNDEFINED
    }

    public TypeDefStore() {
        omrsGuidToTypeDef = new HashMap<>();
        omrsNameToGuid = new HashMap<>();
        omrsGuidToAttributeMap = new HashMap<>();
        prefixToOmrsTypeName = new HashMap<>();
        omrsNameToCatalogNamesByPrefix = new HashMap<>();
        catalogNameToOmrsNamesByPrefix = new HashMap<>();
        unimplementedTypeDefs = new HashMap<>();
        omrsNameToAttributeMapByPrefix = new HashMap<>();
        catalogNameToAttributeMapByPrefix = new HashMap<>();
        omrsNameToEndpointMapByPrefix = new HashMap<>();
        catalogNameToEndpointMapByPrefix = new HashMap<>();
        unmappedTypes = new HashSet<>();
        superTypesToAdd = new HashSet<>();
        mapper = new ObjectMapper();
        loadMappings();
    }

    /**
     * Loads TypeDef mappings defined through a resources file included in the .jar file.
     */
    private void loadMappings() {

        try {

            InputStream stream = getClass()
                    .getClassLoader().getResourceAsStream("TypeDefMappings.json");

            // Start with the basic mappings from type-to-type
            List<MappingFromFile> mappings = mapper.readValue(stream, new TypeReference<List<MappingFromFile>>(){});
            for (MappingFromFile mapping : mappings) {

                String catalogName = mapping.getCatalogName();
                String omrsName = mapping.getOMRSName();
                String prefix = mapping.getPrefix();

                if (!omrsNameToCatalogNamesByPrefix.containsKey(omrsName)) {
                    omrsNameToCatalogNamesByPrefix.put(omrsName, new HashMap<>());
                }
                omrsNameToCatalogNamesByPrefix.get(omrsName).put(prefix, catalogName);
                if (!catalogNameToOmrsNamesByPrefix.containsKey(catalogName)) {
                    catalogNameToOmrsNamesByPrefix.put(catalogName, new HashMap<>());
                }
                catalogNameToOmrsNamesByPrefix.get(catalogName).put(prefix, omrsName);

                if (prefix != null) {
                    prefixToOmrsTypeName.put(prefix, omrsName);
                }

                // Process any property-to-property mappings within the types
                List<MappingFromFile> properties = mapping.getPropertyMappings();
                if (properties != null) {
                    Map<String, String> propertyMapOmrsToCatalog = new HashMap<>();
                    Map<String, String> propertyMapCatalogToOmrs = new HashMap<>();
                    for (MappingFromFile property : properties) {
                        String catalogProperty = property.getCatalogName();
                        String omrsProperty = property.getOMRSName();
                        propertyMapOmrsToCatalog.put(omrsProperty, catalogProperty);
                        propertyMapCatalogToOmrs.put(catalogProperty, omrsProperty);
                    }
                    if (!omrsNameToAttributeMapByPrefix.containsKey(omrsName)) {
                        omrsNameToAttributeMapByPrefix.put(omrsName, new HashMap<>());
                    }
                    omrsNameToAttributeMapByPrefix.get(omrsName).put(prefix, propertyMapOmrsToCatalog);
                    if (!catalogNameToAttributeMapByPrefix.containsKey(catalogName)) {
                        catalogNameToAttributeMapByPrefix.put(catalogName, new HashMap<>());
                    }
                    catalogNameToAttributeMapByPrefix.get(catalogName).put(prefix, propertyMapCatalogToOmrs);
                }

                // Process any endpoint-to-endpoint mappings within the types (for relationships)
                List<MappingFromFile> endpoints = mapping.getEndpointMappings();
                if (endpoints != null) {
                    if (endpoints.size() != 2) {
                        log.warn("Skipping mapping as found other than exactly 2 endpoints defined for the relationship '{}': {}", catalogName, endpoints);
                    } else {
                        MappingFromFile endpoint1 = endpoints.get(0);
                        MappingFromFile endpoint2 = endpoints.get(1);
                        EndpointMapping endpointMapping = new EndpointMapping(
                                catalogName,
                                omrsName,
                                endpoint1.getCatalogName(),
                                endpoint1.getOMRSName(),
                                endpoint1.getPrefix(),
                                endpoint2.getCatalogName(),
                                endpoint2.getOMRSName(),
                                endpoint2.getPrefix()
                        );
                        if (!omrsNameToEndpointMapByPrefix.containsKey(omrsName)) {
                            omrsNameToEndpointMapByPrefix.put(omrsName, new HashMap<>());
                        }
                        omrsNameToEndpointMapByPrefix.get(omrsName).put(prefix, endpointMapping);
                        if (!catalogNameToEndpointMapByPrefix.containsKey(catalogName)) {
                            catalogNameToEndpointMapByPrefix.put(catalogName, new HashMap<>());
                        }
                        catalogNameToEndpointMapByPrefix.get(catalogName).put(prefix, endpointMapping);
                    }
                }

            }
        } catch (IOException e) {
            log.error("Unable to load mapping file TypeDefMappings.json from jar file -- no mappings will exist. Error: {}", e);
        }
        log.debug("omrsNameToCatalogNamesByPrefix: {}", omrsNameToCatalogNamesByPrefix);
        log.debug("catalogNameToOmrsNamesByPrefix: {}", catalogNameToOmrsNamesByPrefix);
        log.debug("prefixToOmrsTypeName: {}", prefixToOmrsTypeName);
        log.debug("catalogNameToAttributeMapByPrefix: {}", catalogNameToAttributeMapByPrefix);
        log.debug("omrsNameToAttributeMapByPrefix: {}", omrsNameToAttributeMapByPrefix);
        log.debug("omrsNameToEndpointMapByPrefix: {}", omrsNameToEndpointMapByPrefix);
        log.debug("catalogNameToEndpointMapByPrefix: {}", catalogNameToEndpointMapByPrefix);
    }

    /**
     * Indicates whether the provided OMRS TypeDef is mapped to a Catalog TypeDef.
     *
     * @param omrsName name of the OMRS TypeDef
     * @return boolean
     */
    public boolean isTypeDefMapped(String omrsName) {
        return omrsNameToCatalogNamesByPrefix.containsKey(omrsName);
    }

    /**
     * Indicates whether the provided OMRS TypeDef is reserved for later mapping (and therefore should not be created).
     *
     * @param omrsName name of the OMRS TypeDef
     * @return boolean
     */
    public boolean isReserved(String omrsName) {
        return unmappedTypes.contains(omrsName);
    }

    /**
     * Retrieves a map of all OMRS property name to catalogName definitions,
     * or null if there are no mappings (or no properties).
     *
     * @return {@code Map<String, String>}
     */
    public Map<String, Map<String, String>> getAllOmrsNameToCatalogNameMappings() {
        return omrsNameToCatalogNamesByPrefix;
    }

    /**
     * Retrieves a map from  Catalog property name to OMRS property name for the provided  Catalog TypeDef name,
     * or null if there are no mappings (or no properties).
     *
     * @param catalogName the name of the  Catalog TypeDef
     * @param prefix the prefix (if any) when mappings to multiple types exist
     * @return {@code Map<String, String>}
     */
    public Map<String, String> getPropertyMappingsForCatalogTypeDef(String catalogName, String prefix) {
        if (catalogNameToAttributeMapByPrefix.containsKey(catalogName)) {
            if (catalogNameToAttributeMapByPrefix.get(catalogName).containsKey(prefix)) {
                return catalogNameToAttributeMapByPrefix.get(catalogName).get(prefix);
            } else {
                return getPropertyMappingsForOMRSTypeDef(catalogName, prefix);
            }
        } else {
            return getPropertyMappingsForOMRSTypeDef(catalogName, prefix);
        }
    }

    /**
     * Retrieves a map from OMRS property name to  Catalog property name for the provided OMRS TypeDef name, or null
     * if there are no mappings (or no properties).
     *
     * @param omrsName the name of the OMRS TypeDef
     * @param prefix the prefix (if any) when mappings to multiple types exist
     * @return {@code Map<String, String>}
     */
    public Map<String, String> getPropertyMappingsForOMRSTypeDef(String omrsName, String prefix) {
        if (omrsNameToAttributeMapByPrefix.containsKey(omrsName)) {
            return omrsNameToAttributeMapByPrefix.get(omrsName).getOrDefault(prefix, null);
        } else {
            return null;
        }
    }

    /**
     * Retrieve the relationship endpoint mapping from the  Catalog details provided.
     *
     * @param catalogTypeName the name of the  Catalog type definition
     * @param relationshipPrefix the prefix used for the relationship, if it is a generated relationship (null if not generated)
     * @return EndpointMapping
     */
    public EndpointMapping getEndpointMappingFromCatalogName(String catalogTypeName, String relationshipPrefix) {
        if (catalogNameToEndpointMapByPrefix.containsKey(catalogTypeName)) {
            return catalogNameToEndpointMapByPrefix.get(catalogTypeName).getOrDefault(relationshipPrefix, null);
        } else {
            return null;
        }
    }

    /**
     * Retrieve all endpoint mappings (relationships) that are mapped for the provided  Catalog type.
     *
     * @param catalogTypeName the name of the  Catalog type definition
     * @return {@code Map<String, EndpointMapping>}
     */
    public Map<String, EndpointMapping> getAllEndpointMappingsFromCatalogName(String catalogTypeName) {
        return catalogNameToEndpointMapByPrefix.getOrDefault(catalogTypeName, Collections.emptyMap());
    }

    /**
     * Retrieves all of the  Catalog TypeDef names that are mapped to the provided OMRS TypeDef name, or null
     * if there is no mapping. The map returned will be keyed by prefix, and values will be the mapped Catalog TypeDef
     * name for that prefix.
     *
     * @param omrsName the name of the OMRS TypeDef
     * @return {@code Map<String, String>}
     */
    public Map<String, String> getAllMappedCatalogTypeDefNames(String omrsName) {
        if (isTypeDefMapped(omrsName)) {
            Map<String, String> returnedMap = new HashMap<>();
            returnedMap = omrsNameToCatalogNamesByPrefix.get(omrsName);
            return returnedMap;
        } else if (omrsNameToGuid.containsKey(omrsName)) {
            Map<String, String> map = new HashMap<>();
            map.put(null, omrsName);
            return map;
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieves the  Catalog TypeDef name that is mapped to the provided OMRS TypeDef name, the same name if
     * there is a one-to-one mapping between Catalog and OMRS TypeDefs, or null if there is no mapping.
     *
     * @param omrsName the name of the OMRS TypeDef
     * @param prefix the prefix (if any) when mappings to multiple types exist
     * @return String
     */
    public String getMappedCatalogTypeDefName(String omrsName, String prefix) {
        if (isTypeDefMapped(omrsName)) {
            return omrsNameToCatalogNamesByPrefix.get(omrsName).getOrDefault(prefix, null);
        } else if (omrsNameToGuid.containsKey(omrsName)) {
            return omrsName;
        } else {
            return null;
        }
    }

    /**
     * Retrieves the OMRS TypeDef that is mapped to the provided prefix, or null if there is no mapping.
     *
     * @param prefix the prefix for the OMRS type to retrieve
     * @return TypeDef
     */
    public TypeDef getTypeDefByPrefix(String prefix) {
        String omrsTypeName = getMappedOMRSTypeDefNameForPrefix(prefix);
        TypeDef typeDef = null;
        if (omrsTypeName != null) {
            typeDef = getTypeDefByName(omrsTypeName);
        }
        return typeDef;
    }

    /**
     * Retrieves the OMRS TypeDef name that is mapped to the provided prefix, or null if there is no mapping.
     *
     * @param prefix the prefix for the OMRS type to retrieve
     * @return String
     */
    private String getMappedOMRSTypeDefNameForPrefix(String prefix) {
        return prefixToOmrsTypeName.getOrDefault(prefix, null);
    }

    /**
     * Retrieves all of the OMRS TypeDef names that are mapped to the provided OMRS TypeDef name, or null if there is
     * no mapping. The map returned will be keyed by prefix, and values will be the mapped OMRS TypeDef name for that
     * prefix.
     *
     * @param catalogName the name of the  Catalog TypeDef
     * @return {@code Map<String, String>}
     */
    public Map<String, String> getAllMappedOMRSTypeDefNames(String catalogName) {
        return catalogNameToOmrsNamesByPrefix.getOrDefault(catalogName, null);
    }

    /**
     * Retrieves the OMRS TypeDef name that is mapped to the provided  Catalog TypeDef name, the same name if
     * there is a one-to-one mapping between Catalog and OMRS TypeDefs, or null if there is no mapping.
     *
     * @param catalogName the name of the  Catalog TypeDef
     * @param prefix the prefix (if any) when mappings to multiple types exist
     * @return String
     */
    public String getMappedOMRSTypeDefName(String catalogName, String prefix) {
        if (catalogNameToOmrsNamesByPrefix.containsKey(catalogName)) {
            return catalogNameToOmrsNamesByPrefix.get(catalogName).getOrDefault(prefix, null);
        } else if (omrsNameToGuid.containsKey(catalogName)) {
            return catalogName;
        } else {
            return null;
        }
    }

    /**
     * Retrieves the OMRS TypeDef name that is mapped to the provided  Catalog TypeDef name, the same name if
     * there is a one-to-one mapping between Catalog and OMRS TypeDefs, or null if there is no mapping. The result is
     * a mapping of prefix to OMRS type.
     *
     * @param catalogName the name of the  Catalog TypeDef
     * @return {@code Map<String, String>}
     */
    public Map<String, String> getMappedOMRSTypeDefNameWithPrefixes(String catalogName) {
        if (catalogNameToOmrsNamesByPrefix.containsKey(catalogName)) {
            return catalogNameToOmrsNamesByPrefix.getOrDefault(catalogName, Collections.emptyMap());
        } else if (omrsNameToGuid.containsKey(catalogName)) {
            Map<String, String> map = new HashMap<>();
            map.put(null, catalogName);
            return map;
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Adds the provided TypeDef to the list of those that are implemented in the repository.
     *
     * @param typeDef an implemented type definition
     */
    public void addTypeDef(TypeDef typeDef) {
        String guid = typeDef.getGUID();
        omrsGuidToTypeDef.put(guid, typeDef);
        omrsNameToGuid.put(typeDef.getName(), guid);
        addAttributes(typeDef.getPropertiesDefinition(), guid, typeDef.getName());

        // No longer need to add this type
        superTypesToAdd.remove(typeDef.getGUID());
        // Add all super types that we've already seen
        TypeDefLink superType = typeDef.getSuperType();
        if(superType != null) {
            if (unimplementedTypeDefs.containsKey(superType.getGUID())) {
                // No longer unimplemented, remove
                TypeDef superTypeDef = unimplementedTypeDefs.remove(superType.getGUID());
                addTypeDef(superTypeDef);
            } else {
                // Haven't seen the super type yet, store it to add later
                superTypesToAdd.add(superType.getGUID());
            }
        }
    }

    public boolean isSuperTypeOfMappedType(TypeDef typeDef) {
        return superTypesToAdd.contains(typeDef.getGUID());
    }

    /**
     * Adds the provided TypeDef to the list of those that are not implemented in the repository.
     * (Still needed for tracking and inheritance.)
     *
     * @param typeDef an unimplemented type definition
     */
    public void addUnimplementedTypeDef(TypeDef typeDef) {
        String guid = typeDef.getGUID();
        unimplementedTypeDefs.put(guid, typeDef);
        addAttributes(typeDef.getPropertiesDefinition(), guid, typeDef.getName());
    }

    /**
     * Adds a mapping between GUID of the OMRS TypeDef and a mapping of its attribute names to definitions.
     *
     * @param attributes the list of attribute definitions for the OMRS TypeDef
     * @param guid of the OMRS TypeDef
     * @param name of the OMRS TypeDef
     */
    private void addAttributes(List<TypeDefAttribute> attributes, String guid, String name) {
        if (!omrsGuidToAttributeMap.containsKey(guid)) {
            omrsGuidToAttributeMap.put(guid, new HashMap<>());
        }
        if (attributes != null) {
            Map<String, String> oneToOne = new HashMap<>();
            for (TypeDefAttribute attribute : attributes) {
                String propertyName = attribute.getAttributeName();
                omrsGuidToAttributeMap.get(guid).put(propertyName, attribute);
                oneToOne.put(propertyName, propertyName);
            }
            if (!omrsNameToAttributeMapByPrefix.containsKey(name)) {
                // If no mapping was loaded for this OMRS type definition, add one-to-one mappings
                omrsNameToAttributeMapByPrefix.put(name, new HashMap<>());
                omrsNameToAttributeMapByPrefix.get(name).put(null, oneToOne);
            }
        }
    }

    /**
     * Retrieves an unimplemented TypeDef by its GUID.
     *
     * @param guid of the type definition
     * @return TypeDef
     */
    public TypeDef getUnimplementedTypeDefByGUID(String guid) {
        if (unimplementedTypeDefs.containsKey(guid)) {
            return unimplementedTypeDefs.get(guid);
        } else {
            log.warn("Unable to find unimplemented OMRS TypeDef: {}", guid);
            return null;
        }
    }

    /**
     * Retrieves an implemented TypeDef by its GUID.
     *
     * @param guid of the type definition
     * @return TypeDef
     */
    public TypeDef getTypeDefByGUID(String guid) {
        return getTypeDefByGUID(guid, true);
    }

    /**
     * Retrieves an implemented TypeDef by its GUID.
     *
     * @param guid of the type definition
     * @param warnIfNotFound whether to log a warning if GUID is not known (true) or not (false).
     * @return TypeDef
     */
    public TypeDef getTypeDefByGUID(String guid, boolean warnIfNotFound) {
        if (omrsGuidToTypeDef.containsKey(guid)) {
            return omrsGuidToTypeDef.get(guid);
        } else {
            if (warnIfNotFound && log.isWarnEnabled()) {
                log.warn("Unable to find OMRS TypeDef by GUID: {}", guid);
            }
            return null;
        }
    }

    /**
     * Retrieves an implemented TypeDef by its name.
     *
     * @param name of the type definition
     * @return TypeDef
     */
    public TypeDef getTypeDefByName(String name) {
        return getTypeDefByName(name, true);
    }

    /**
     * Retrieves an implemented TypeDef by its name.
     *
     * @param name of the type definition
     * @param warnIfNotFound whether to log a warning if name is not known (true) or not (false).
     * @return TypeDef
     */
    private TypeDef getTypeDefByName(String name, boolean warnIfNotFound) {
        if (omrsNameToGuid.containsKey(name)) {
            String guid = omrsNameToGuid.get(name);
            return getTypeDefByGUID(guid, warnIfNotFound);
        } else {
            if (warnIfNotFound && log.isWarnEnabled()) {
                log.warn("Unable to find OMRS TypeDef by Name: {}", name);
            }
            return null;
        }
    }

    /**
     * Retrieves a map from attribute name to attribute definition for all attributes of the specified type definition.
     *
     * @param guid of the type definition
     * @return {@code Map<String, TypeDefAttribute>}
     */
    private Map<String, TypeDefAttribute> getTypeDefAttributesByGUID(String guid) {
        if (omrsGuidToAttributeMap.containsKey(guid)) {
            return omrsGuidToAttributeMap.get(guid);
        } else {
            log.warn("Unable to find attributes for OMRS TypeDef by GUID: {}", guid);
            return null;
        }
    }

    /**
     * Retrieves a map from attribute name to attribute definition for all attributes of the specified type definition,
     * including all of its supertypes' attributes.
     *
     * @param guid of the type definition
     * @return {@code Map<String, TypeDefAttribute>}
     */
    private Map<String, TypeDefAttribute> getAllTypeDefAttributesForGUID(String guid) {
        Map<String, TypeDefAttribute> all = getTypeDefAttributesByGUID(guid);
        if (all != null) {
            TypeDef typeDef = getTypeDefByGUID(guid, false);
            if (typeDef == null) {
                typeDef = getUnimplementedTypeDefByGUID(guid);
            }
            TypeDefLink superType = typeDef.getSuperType();
            if (superType != null) {
                all.putAll(getAllTypeDefAttributesForGUID(superType.getGUID()));
            }
        }
        return all;
    }

    /**
     * Retrieves a map from attribute name to attribute definition for all attributes of the specified type definition,
     * including all of its supertypes' attributes.
     *
     * @param name of the type definition
     * @return {@code Map<String, TypeDefAttribute>}
     */
    public Map<String, TypeDefAttribute> getAllTypeDefAttributesForName(String name) {
        if (omrsNameToGuid.containsKey(name)) {
            String guid = omrsNameToGuid.get(name);
            return getAllTypeDefAttributesForGUID(guid);
        } else {
            log.warn("Unable to find attributes for OMRS TypeDef by Name: {}", name);
            return null;
        }
    }

    /**
     * Retrieves a listing of all of the implemented type definitions for this repository.
     *
     * @return {@code List<TypeDef>}
     */
    public List<TypeDef> getAllTypeDefs() {
        return new ArrayList<>(omrsGuidToTypeDef.values());
    }

    /**
     * For translating between relationship endpoints.
     */
    public final class EndpointMapping {

        private String catalogRelationshipTypeName;
        private String omrsRelationshipTypeName;
        private String catalog1;
        private String catalog2;
        private String omrs1;
        private String omrs2;
        private String prefix1;
        private String prefix2;

        EndpointMapping(String catalogRelationshipTypeName,
                        String omrsRelationshipTypeName,
                        String catalog1,
                        String omrs1,
                        String prefix1,
                        String catalog2,
                        String omrs2,
                        String prefix2) {
            this.catalogRelationshipTypeName = catalogRelationshipTypeName;
            this.omrsRelationshipTypeName = omrsRelationshipTypeName;
            this.catalog1 = catalog1;
            this.catalog2 = catalog2;
            this.omrs1 = omrs1;
            this.omrs2 = omrs2;
            this.prefix1 = prefix1;
            this.prefix2 = prefix2;
        }

        public String getPrefixOne() { return prefix1; }
        public String getPrefixTwo() { return prefix2; }
        public String getCatalogRelationshipTypeName() { return catalogRelationshipTypeName; }
        public String getOmrsRelationshipTypeName() { return omrsRelationshipTypeName; }

    }

}
