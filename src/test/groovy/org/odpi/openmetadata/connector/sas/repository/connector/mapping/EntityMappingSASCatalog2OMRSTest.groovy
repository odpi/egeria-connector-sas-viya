package org.odpi.openmetadata.connector.sas.repository.connector.mapping


import org.odpi.openmetadata.connector.sas.client.SASCatalogClient
import org.odpi.openmetadata.connector.sas.repository.connector.RepositoryConnector
import org.odpi.openmetadata.connector.sas.repository.connector.stores.TypeDefStore
import org.odpi.openmetadata.frameworks.connectors.properties.ConnectionProperties
import org.odpi.openmetadata.frameworks.connectors.properties.beans.Connection
import org.odpi.openmetadata.repositoryservices.auditlog.OMRSAuditLog
import org.odpi.openmetadata.repositoryservices.auditlog.OMRSAuditLogDestination
import org.odpi.openmetadata.repositoryservices.auditlog.OMRSAuditingComponent
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstancePropertyValue
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.EntityDef
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.PrimitiveDef
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.PrimitiveDefCategory
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDefAttribute
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDefCategory
import org.odpi.openmetadata.repositoryservices.localrepository.repositorycontentmanager.OMRSRepositoryContentHelper
import org.odpi.openmetadata.repositoryservices.localrepository.repositorycontentmanager.OMRSRepositoryContentManager
import spock.lang.Shared
import spock.lang.Specification

class EntityMappingSASCatalog2OMRSTest extends Specification {

    // Mock Catalog client
    @Shared mockClient = Mock(SASCatalogClient)

    // OMRS Repository Connector
    @Shared repositoryConnector = new RepositoryConnector(mockClient)

    // OMRS Content Manager
    @Shared contentManager = new OMRSRepositoryContentManager("steven", new OMRSAuditLog(new OMRSAuditLogDestination("test", "test", "test", new ArrayList<>()), OMRSAuditingComponent.OPERATIONAL_SERVICES).createNewAuditLog(OMRSAuditingComponent.REPOSITORY_CONTENT_MANAGER))

    // Type Definition Store
    @Shared typeDefStore = new TypeDefStore()

    // RelationalTable TypeDef
    @Shared relTableTypeDef = new EntityDef(TypeDefCategory.ENTITY_DEF, "ce7e72b8-396a-4013-8688-f9d973067425", "RelationalTable", 1L, "1")

    // RelationalTableType TypeDef
    @Shared relTableTypeTypeDef = new EntityDef(TypeDefCategory.ENTITY_DEF, "1321bcc0-dc6a-48ed-9ca6-0c6f934b0b98", "RelationalTableType", 1L, "1")

    // RelationalColumn TypeDef
    @Shared relColumnTypeDef = new EntityDef(TypeDefCategory.ENTITY_DEF, "aa8d5470-6dbc-4648-9e2f-045e5df9d2f9", "RelationalColumn", 1L, "1")

    // RelationalColumnType TypeDef
    @Shared relColumnTypeTypeDef = new EntityDef(TypeDefCategory.ENTITY_DEF, "f0438d80-6eb9-4fac-bcc1-5efee5babcfc", "RelationalColumnType", 1L, "1")

    // DataStore TypeDef
    @Shared dataStoreTypeDef = new EntityDef(TypeDefCategory.ENTITY_DEF, "30756d0b-362b-4bfa-a0de-fce6a8f47b47", "DataStore", 1L, "1")

    // DesignModel TypeDef
    @Shared designModelTypeDef = new EntityDef(TypeDefCategory.ENTITY_DEF, "bf17143d-8605-48c2-ba80-64c2ac8f8379", "DesignModel", 1L, "1")

    // DesignModel TypeDef
    @Shared conceptModelElemTypeDef = new EntityDef(TypeDefCategory.ENTITY_DEF, "06659195-3111-4c91-8931-a65f655378d9", "ConceptModelElement", 1L, "1")

    def createStringTypeDefAttr(String name) {
        def attr = new TypeDefAttribute()
        attr.attributeName = name
        attr.attributeType = new PrimitiveDef(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING)
        return attr
    }

    def createIntTypeDefAttr(String name) {
        def attr = new TypeDefAttribute()
        attr.attributeName = name
        attr.attributeType = new PrimitiveDef(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_INT)
        return attr
    }

    def createDateTypeDefAttr(String name) {
        def attr = new TypeDefAttribute()
        attr.attributeName = name
        attr.attributeType = new PrimitiveDef(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_DATE)
        return attr
    }

    def getDetailPropAsString(EntityDetail detail, String propName) {
        InstancePropertyValue val =  detail.properties.getPropertyValue(propName)
        if (val != null) {return val.valueAsString()}
        return null
    }

    def getDetailPropAsObject(EntityDetail detail, String propName) {
        InstancePropertyValue val =  detail.properties.getPropertyValue(propName)
        if (val != null) {return val.valueAsObject()}
        return null
    }

    def getAdditionalProperty(EntityDetail detail, String keyName) {
        return ((Map) detail.properties.getPropertyValue("additionalProperties").valueAsObject()).get(keyName)
    }

    def setupSpec() {
        // Set up repository connector
        repositoryConnector.initialize("d8015ec8-f3e5-4331-a320-66a6e850e373", new ConnectionProperties(new Connection()))
        repositoryConnector.metadataCollectionId = "f6fc17c9-49b3-4929-87a8-ba9a4acd7449"

        // Setup type defs
        def authorAttribute           = createStringTypeDefAttr("author")
        def commentAttribute          = createStringTypeDefAttr("comment")
        def createTimeAttribute       = createDateTypeDefAttr("createTime")
        def dataTypeAttribute         = createStringTypeDefAttr("dataType")
        def descriptionAttribute      = createStringTypeDefAttr("description")
        def displayNameAttribute      = createStringTypeDefAttr("displayName")
        def encodingStandardAttribute = createStringTypeDefAttr("encodingStandard")
        def lengthAttribute           = createIntTypeDefAttr("length")
        def modifiedTimeAttribute     = createDateTypeDefAttr("modifiedTime")
        def nativeClassAttribute      = createStringTypeDefAttr("nativeClass")
        def positionAttribute         = createIntTypeDefAttr("position")
        def qualifiedNameAttribute    = createStringTypeDefAttr("qualifiedName")
        def usageAttribute            = createStringTypeDefAttr("usage")

        relTableTypeDef.setPropertiesDefinition(Arrays.asList(qualifiedNameAttribute, displayNameAttribute, descriptionAttribute, nativeClassAttribute, commentAttribute))
        contentManager.addTypeDef("test", relTableTypeDef)

        relTableTypeTypeDef.setPropertiesDefinition(Arrays.asList(qualifiedNameAttribute, displayNameAttribute, authorAttribute))
        contentManager.addTypeDef("test", relTableTypeTypeDef)

        relColumnTypeDef.setPropertiesDefinition(Arrays.asList(qualifiedNameAttribute, displayNameAttribute, descriptionAttribute, lengthAttribute, positionAttribute))
        contentManager.addTypeDef("test", relColumnTypeDef)

        relColumnTypeTypeDef.setPropertiesDefinition(Arrays.asList(qualifiedNameAttribute, displayNameAttribute, descriptionAttribute, authorAttribute, dataTypeAttribute, encodingStandardAttribute, usageAttribute))
        contentManager.addTypeDef("test", relColumnTypeTypeDef)

        dataStoreTypeDef.setPropertiesDefinition(Arrays.asList(qualifiedNameAttribute, descriptionAttribute, createTimeAttribute, modifiedTimeAttribute))
        contentManager.addTypeDef("test", dataStoreTypeDef)

        designModelTypeDef.setPropertiesDefinition(Arrays.asList(qualifiedNameAttribute, descriptionAttribute, authorAttribute))
        contentManager.addTypeDef("test", designModelTypeDef)

        conceptModelElemTypeDef.setPropertiesDefinition(Arrays.asList(qualifiedNameAttribute, descriptionAttribute, displayNameAttribute, authorAttribute))
        contentManager.addTypeDef("test", conceptModelElemTypeDef)

        repositoryConnector.setRepositoryHelper(new OMRSRepositoryContentHelper(contentManager))
        repositoryConnector.start()

        // Set up type def store
        typeDefStore.addTypeDef(relTableTypeDef)
        typeDefStore.addTypeDef(relTableTypeTypeDef)
        typeDefStore.addTypeDef(relColumnTypeDef)
        typeDefStore.addTypeDef(relColumnTypeTypeDef)
        typeDefStore.addTypeDef(dataStoreTypeDef)
        typeDefStore.addTypeDef(designModelTypeDef)
        typeDefStore.addTypeDef(conceptModelElemTypeDef)
    }

    def "GetEntityDetail - CAS Table"() {
        // Setup test instance
        SASCatalogObject instance = new SASCatalogObject()
        instance.addInstanceProperty("name", "Catalog CAS Table")
        instance.addInstanceProperty("label", "CAS Table Label")
        instance.addInstanceProperty("description", "CAS Table from mock client")
        instance.addInstanceProperty("createdBy", "steven")
        instance.addInstanceProperty("modifiedBy", "ben")
        instance.addInstanceProperty("creationTimeStamp", "2020-07-14T18:40:02.788574Z")
        instance.addInstanceProperty("modifiedTimeStamp", "2020-07-15T18:40:02.788574Z")
        instance.addInstanceProperty("version", 1)
        instance.addDefinitionProperty("name", "casTable")
        Map<String, String> attributes = new HashMap<>()
        attributes.put("isLoaded", "true")
        instance.attributes = attributes
        instance.guid = "1bbc58c1-d350-4a0d-ab88-492705fe448b"

        EntityMappingSASCatalog2OMRS mapper
        EntityDetail detail

        when: "I get entity detail with no prefix"
        mapper = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, null, instance, null, "steven")
        detail = mapper.getEntityDetail()
        then: "Values should be mapped correctly"
        detail.getType().getTypeDefName() == relTableTypeDef.getName()
        detail.createdBy == instance.getInstanceProperty("createdBy")
        detail.updatedBy == instance.getInstanceProperty("modifiedBy")
        detail.createTime.dateTimeString.contains("7/14/20")
        detail.updateTime.dateTimeString.contains("7/15/20")
        getDetailPropAsString(detail, "qualifiedName") == instance.getInstanceProperty("name")
        getDetailPropAsString(detail, "displayName") == instance.getInstanceProperty("label")
        getDetailPropAsString(detail, "description") == instance.getInstanceProperty("description")
        //Constant values
        getDetailPropAsString(detail, "nativeClass") == "Table"
        getDetailPropAsString(detail, "comment") == "CAS Table"
        getAdditionalProperty(detail,"isLoaded") == instance.getAttribute("isLoaded")
        detail.instanceURL.contains("/catalog/instances/" + instance.guid)

        when: "I get entity detail with a prefix"
        mapper = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, null, instance, "RT", "steven")
        detail = mapper.getEntityDetail()
        then: "Values should be mapped correctly"
        detail.getType().getTypeDefName() == relTableTypeTypeDef.getName()
        detail.createdBy == instance.getInstanceProperty("createdBy")
        detail.updatedBy == instance.getInstanceProperty("modifiedBy")
        detail.createTime.dateTimeString.contains("7/14/20")
        detail.updateTime.dateTimeString.contains("7/15/20")
        getDetailPropAsString(detail, "qualifiedName") == instance.getInstanceProperty("name")
        getDetailPropAsString(detail, "displayName") == instance.getInstanceProperty("label")
        getDetailPropAsString(detail, "author") == instance.getInstanceProperty("createdBy")
        getAdditionalProperty(detail, "isLoaded") == instance.getAttribute("isLoaded")
        detail.instanceURL.contains("/catalog/instances/" + instance.guid)
    }

    def "GetEntityDetail - SAS Table"() {
        // Setup test instance
        SASCatalogObject instance = new SASCatalogObject()
        instance.addInstanceProperty("name", "Catalog SAS Table")
        instance.addInstanceProperty("label", "SAS Table Label")
        instance.addInstanceProperty("description", "SAS Table from mock client")
        instance.addInstanceProperty("createdBy", "steven")
        instance.addInstanceProperty("modifiedBy", "ben")
        instance.addInstanceProperty("creationTimeStamp", "2020-07-14T18:40:02.788574Z")
        instance.addInstanceProperty("modifiedTimeStamp", "2020-07-15T18:40:02.788574Z")
        instance.addInstanceProperty("version", 1)
        instance.addDefinitionProperty("name", "sasTable")
        Map<String, String> attributes = new HashMap<>()
        instance.attributes = attributes
        instance.guid = "3a429b03-ee47-4787-af15-ffdc8a43fc06"

        EntityMappingSASCatalog2OMRS mapper
        EntityDetail detail

        when: "I get entity detail with no prefix"
        mapper = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, null, instance, null, "steven")
        detail = mapper.getEntityDetail()
        then: "Values should be mapped correctly"
        detail.getType().getTypeDefName() == relTableTypeDef.getName()
        detail.createdBy == instance.getInstanceProperty("createdBy")
        detail.updatedBy == instance.getInstanceProperty("modifiedBy")
        detail.createTime.dateTimeString.contains("7/14/20")
        detail.updateTime.dateTimeString.contains("7/15/20")
        getDetailPropAsString(detail, "qualifiedName") == instance.getInstanceProperty("name")
        getDetailPropAsString(detail, "displayName") == instance.getInstanceProperty("label")
        getDetailPropAsString(detail, "description") == instance.getInstanceProperty("description")
        //Constant values
        getDetailPropAsString(detail, "nativeClass") == "Table"
        getDetailPropAsString(detail, "comment") == "SAS Table"
        detail.instanceURL.contains("/catalog/instances/" + instance.guid)

        when: "I get entity detail with a prefix"
        mapper = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, null, instance, "RT", "steven")
        detail = mapper.getEntityDetail()
        then: "Values should be mapped correctly"
        detail.getType().getTypeDefName() == relTableTypeTypeDef.getName()
        detail.createdBy == instance.getInstanceProperty("createdBy")
        detail.updatedBy == instance.getInstanceProperty("modifiedBy")
        detail.createTime.dateTimeString.contains("7/14/20")
        detail.updateTime.dateTimeString.contains("7/15/20")
        getDetailPropAsString(detail, "qualifiedName") == instance.getInstanceProperty("name")
        getDetailPropAsString(detail, "displayName") == instance.getInstanceProperty("label")
        getDetailPropAsString(detail, "author") == instance.getInstanceProperty("createdBy")
        detail.instanceURL.contains("/catalog/instances/" + instance.guid)
    }

    def "GetEntityDetail - CAS Column" () {
        // Setup test instance
        SASCatalogObject instance = new SASCatalogObject()
        instance.addInstanceProperty("name", "ZIPCODE")
        instance.addInstanceProperty("label", "ZIP CODE")
        instance.addInstanceProperty("description", "Customer ZIP Code")
        instance.addInstanceProperty("createdBy", "steven")
        instance.addInstanceProperty("modifiedBy", "ben")
        instance.addInstanceProperty("creationTimeStamp", "2020-09-14T18:40:02.788574Z")
        instance.addInstanceProperty("modifiedTimeStamp", "2020-09-15T18:40:02.788574Z")
        instance.addInstanceProperty("version", (double) 1)
        instance.addDefinitionProperty("name", "casColumn")
        Map<String, String> attributes = new HashMap<>()
        attributes.put("sasDataType", "num")
        attributes.put("analysisTimeStamp", "2020-10-05T21:48:39.131Z")
        attributes.put("bestChartType", "box")
        attributes.put("casDataType", "double")
        attributes.put("dataType", "num")
        attributes.put("ordinalPosition", 24)
        attributes.put("rawLength", 8)
        attributes.put("sasDataType", "num")
        attributes.put("sasFormat", "BEST")
        instance.attributes = attributes
        instance.guid = "803daa54-8fe7-402f-ab1b-77055c45f30c"

        EntityMappingSASCatalog2OMRS mapper
        EntityDetail detail

        when: "I get entity detail with no prefix"
        mapper = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, null, instance, null, "steven")
        detail = mapper.getEntityDetail()
        then: "Values should be mapped correctly"
        detail.getType().getTypeDefName() == relColumnTypeDef.getName()
        detail.createdBy == instance.getInstanceProperty("createdBy")
        detail.updatedBy == instance.getInstanceProperty("modifiedBy")
        detail.createTime.dateTimeString.contains("9/14/20")
        detail.updateTime.dateTimeString.contains("9/15/20")
        getDetailPropAsString(detail, "qualifiedName") == instance.getInstanceProperty("name")
        getDetailPropAsString(detail, "displayName") == instance.getInstanceProperty("label")
        getDetailPropAsString(detail, "description") == instance.getInstanceProperty("description")
        getDetailPropAsString(detail, "nativeClass") == "Column"
        ((Integer)(getDetailPropAsObject(detail, "length"))).intValue() == instance.getAttribute("rawLength")
        ((Integer)(getDetailPropAsObject(detail, "position"))).intValue() == instance.getAttribute("ordinalPosition")
        getAdditionalProperty(detail, "sasColumnSource") == "CAS Column"
        getAdditionalProperty(detail, "bestChartType") == instance.getAttribute("bestChartType")
        detail.instanceURL.contains("/catalog/instances/" + instance.guid)

        when: "I get entity detail with a prefix"
        mapper = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, null, instance, "RT", "steven")
        detail = mapper.getEntityDetail()
        then: "Values should be mapped correctly"
        detail.getType().getTypeDefName() == relColumnTypeTypeDef.getName()
        detail.createdBy == instance.getInstanceProperty("createdBy")
        detail.updatedBy == instance.getInstanceProperty("modifiedBy")
        detail.createTime.dateTimeString.contains("9/14/20")
        detail.updateTime.dateTimeString.contains("9/15/20")
        getDetailPropAsString(detail, "qualifiedName") == instance.getInstanceProperty("name")
        getDetailPropAsString(detail, "displayName") == instance.getInstanceProperty("label")
        getDetailPropAsString(detail, "author") == instance.getInstanceProperty("createdBy")
        getDetailPropAsString(detail, "dataType") == instance.getAttribute("casDataType")
        getDetailPropAsString(detail, "encodingStandard") == instance.getAttribute("sasInFormat")
        getDetailPropAsString(detail, "usage") == instance.getAttribute("sasFormat")
        detail.instanceURL.contains("/catalog/instances/" + instance.guid)
    }

    def "GetEntityDetail - SAS Column" () {
        // Setup test instance
        SASCatalogObject instance = new SASCatalogObject()
        instance.addInstanceProperty("name", "ZIPCODE")
        instance.addInstanceProperty("label", "ZIP CODE")
        instance.addInstanceProperty("description", "Customer ZIP Code")
        instance.addInstanceProperty("createdBy", "steven")
        instance.addInstanceProperty("modifiedBy", "ben")
        instance.addInstanceProperty("creationTimeStamp", "2020-09-14T18:40:02.788574Z")
        instance.addInstanceProperty("modifiedTimeStamp", "2020-09-15T18:40:02.788574Z")
        instance.addInstanceProperty("version", (double) 1)
        instance.addDefinitionProperty("name", "sasColumn")
        Map<String, String> attributes = new HashMap<>()
        attributes.put("analysisTimeStamp", "2020-10-09T06:15:13.610Z")
        attributes.put("bestChartType", "frequency")
        attributes.put("dataType", "num")
        attributes.put("ordinalPosition", 1)
        attributes.put("rawLength", 5)
        attributes.put("sasDataType", "num")
        instance.attributes = attributes
        instance.guid = "803daa54-8fe7-402f-ab1b-77055c45f30c"

        EntityMappingSASCatalog2OMRS mapper
        EntityDetail detail

        when: "I get entity detail with no prefix"
        mapper = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, null, instance, null, "steven")
        detail = mapper.getEntityDetail()
        then: "Values should be mapped correctly"
        detail.getType().getTypeDefName() == relColumnTypeDef.getName()
        detail.createdBy == instance.getInstanceProperty("createdBy")
        detail.updatedBy == instance.getInstanceProperty("modifiedBy")
        detail.createTime.dateTimeString.contains("9/14/20")
        detail.updateTime.dateTimeString.contains("9/15/20")
        getDetailPropAsString(detail, "qualifiedName") == instance.getInstanceProperty("name")
        getDetailPropAsString(detail, "displayName") == instance.getInstanceProperty("label")
        getDetailPropAsString(detail, "description") == instance.getInstanceProperty("description")
        ((Integer)(getDetailPropAsObject(detail, "length"))).intValue() == instance.getAttribute("rawLength")
        ((Integer)(getDetailPropAsObject(detail, "position"))).intValue() == instance.getAttribute("ordinalPosition")
        getDetailPropAsString(detail, "nativeClass") == "Column"
        getAdditionalProperty(detail, "sasColumnSource") == "SAS Column"
        getAdditionalProperty(detail, "bestChartType") == instance.getAttribute("bestChartType")
        detail.instanceURL.contains("/catalog/instances/" + instance.guid)

        when: "I get entity detail with a prefix"
        mapper = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, null, instance, "RT", "steven")
        detail = mapper.getEntityDetail()
        then: "Values should be mapped correctly"
        detail.getType().getTypeDefName() == relColumnTypeTypeDef.getName()
        detail.createdBy == instance.getInstanceProperty("createdBy")
        detail.updatedBy == instance.getInstanceProperty("modifiedBy")
        detail.createTime.dateTimeString.contains("9/14/20")
        detail.updateTime.dateTimeString.contains("9/15/20")
        getDetailPropAsString(detail, "qualifiedName") == instance.getInstanceProperty("name")
        getDetailPropAsString(detail, "displayName") == instance.getInstanceProperty("label")
        getDetailPropAsString(detail, "author") == instance.getInstanceProperty("createdBy")
        getDetailPropAsString(detail, "dataType") == instance.getAttribute("sasDataType")
        getDetailPropAsString(detail, "encodingStandard") == instance.getAttribute("sasInFormat")
        getDetailPropAsString(detail, "usage") == instance.getAttribute("sasFormat")
        detail.instanceURL.contains("/catalog/instances/" + instance.guid)
    }

    def "GetEntityDetail - CAS Library"() {
        // Setup test instance
        SASCatalogObject instance = new SASCatalogObject()
        instance.addInstanceProperty("name", "Catalog CAS Library")
        instance.addInstanceProperty("label", "CAS Library")
        instance.addInstanceProperty("description", "CAS Library from mock client")
        instance.addInstanceProperty("createdBy", "steven")
        instance.addInstanceProperty("modifiedBy", "ben")
        instance.addInstanceProperty("creationTimeStamp", "2020-09-28T18:40:02.788574Z")
        instance.addInstanceProperty("modifiedTimeStamp", "2020-09-29T18:40:02.788574Z")
        instance.addInstanceProperty("version", (double) 1)
        instance.addDefinitionProperty("name", "casLibrary")
        Map<String, Object> attributes = new HashMap<>()
        attributes.put("dateCreated", Date.from(java.time.Instant.parse("2020-08-28T18:40:02.788574Z")))
        attributes.put("dateModified", Date.from(java.time.Instant.parse("2020-08-29T18:40:02.788574Z")))
        instance.attributes = attributes
        instance.guid = "cc38a25f-dc5f-413a-949c-5e0b5514bd8b"

        EntityMappingSASCatalog2OMRS mapper
        EntityDetail detail

        when: "I get entity detail with no prefix"
        mapper = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, null, instance, null, "steven")
        detail = mapper.getEntityDetail()
        then: "Values should be mapped correctly"
        detail.getType().getTypeDefName() == dataStoreTypeDef.getName()
        detail.createdBy == instance.getInstanceProperty("createdBy")
        detail.updatedBy == instance.getInstanceProperty("modifiedBy")
        detail.createTime.dateTimeString.contains("9/28/20")
        detail.updateTime.dateTimeString.contains("9/29/20")
        getDetailPropAsString(detail, "qualifiedName") == instance.getInstanceProperty("name")
        getDetailPropAsString(detail, "description") == instance.getInstanceProperty("description")
        getAdditionalProperty(detail, "sasLabel") == instance.getInstanceProperty("label")
        getAdditionalProperty(detail, "sasLibrarySource") == "CAS Library"
        ((Long)(getDetailPropAsObject(detail, "createTime"))).longValue() == ((Date)(instance.getAttribute("dateCreated"))).time
        ((Long)(getDetailPropAsObject(detail, "modifiedTime"))).longValue() == ((Date)(instance.getAttribute("dateModified"))).time
        detail.instanceURL.contains("/catalog/instances/" + instance.guid)
    }

    def "GetEntityDetail - SAS Library"() {
        // Setup test instance
        SASCatalogObject instance = new SASCatalogObject()
        instance.addInstanceProperty("name", "Catalog SAS Library")
        instance.addInstanceProperty("label", "SAS Library")
        instance.addInstanceProperty("description", "SAS Library from mock client")
        instance.addInstanceProperty("createdBy", "steven")
        instance.addInstanceProperty("modifiedBy", "ben")
        instance.addInstanceProperty("creationTimeStamp", "2020-09-28T18:40:02.788574Z")
        instance.addInstanceProperty("modifiedTimeStamp", "2020-09-29T18:40:02.788574Z")
        instance.addInstanceProperty("version", (double) 1)
        instance.addDefinitionProperty("name", "sasLibrary")
        Map<String, Object> attributes = new HashMap<>()
        attributes.put("dateCreated", Date.from(java.time.Instant.parse("2020-08-28T18:40:02.788574Z")))
        attributes.put("dateModified", Date.from(java.time.Instant.parse("2020-08-29T18:40:02.788574Z")))
        instance.attributes = attributes
        instance.guid = "eea445e3-dcd1-4d7c-a9aa-b2e9c82a3ff0"

        EntityMappingSASCatalog2OMRS mapper
        EntityDetail detail

        when: "I get entity detail with no prefix"
        mapper = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, null, instance, null, "steven")
        detail = mapper.getEntityDetail()
        then: "Values should be mapped correctly"
        detail.getType().getTypeDefName() == dataStoreTypeDef.getName()
        detail.createdBy == instance.getInstanceProperty("createdBy")
        detail.updatedBy == instance.getInstanceProperty("modifiedBy")
        detail.createTime.dateTimeString.contains("9/28/20")
        detail.updateTime.dateTimeString.contains("9/29/20")
        getDetailPropAsString(detail, "qualifiedName") == instance.getInstanceProperty("name")
        getDetailPropAsString(detail, "description") == instance.getInstanceProperty("description")
        getAdditionalProperty(detail, "sasLabel") == instance.getInstanceProperty("label")
        getAdditionalProperty(detail, "sasLibrarySource") == "SAS Library"
        ((Long)(getDetailPropAsObject(detail, "createTime"))).longValue() == ((Date)(instance.getAttribute("dateCreated"))).time
        ((Long)(getDetailPropAsObject(detail, "modifiedTime"))).longValue() == ((Date)(instance.getAttribute("dateModified"))).time
        detail.instanceURL.contains("/catalog/instances/" + instance.guid)
    }

    def "GetEntityDetail - Managed Model"() {
        // Setup test instance
        SASCatalogObject instance = new SASCatalogObject()
        instance.addInstanceProperty("name", "Catalog SAS Managed Model")
        instance.addInstanceProperty("label", "SAS Managed Model")
        instance.addInstanceProperty("description", "SAS Managed Model from mock client")
        instance.addInstanceProperty("createdBy", "steven")
        instance.addInstanceProperty("modifiedBy", "ben")
        instance.addInstanceProperty("creationTimeStamp", "2020-09-28T18:40:02.788574Z")
        instance.addInstanceProperty("modifiedTimeStamp", "2020-09-29T18:40:02.788574Z")
        instance.addInstanceProperty("version", (double) 1)
        instance.addDefinitionProperty("name", "managedModel")
        Map<String, Object> attributes = new HashMap<>()
        attributes.put("dateCreated", Date.from(java.time.Instant.parse("2020-08-28T18:40:02.788574Z")))
        attributes.put("dateModified", Date.from(java.time.Instant.parse("2020-08-29T18:40:02.788574Z")))
        instance.attributes = attributes
        instance.guid = "394e5467-2f9f-4e85-ac97-f1b69f0b3ec7"

        EntityMappingSASCatalog2OMRS mapper
        EntityDetail detail

        when: "I get entity detail with no prefix"
        mapper = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, null, instance, null, "steven")
        detail = mapper.getEntityDetail()
        then: "Values should be mapped correctly"
        detail.getType().getTypeDefName() == designModelTypeDef.getName()
        detail.createdBy == instance.getInstanceProperty("createdBy")
        detail.updatedBy == instance.getInstanceProperty("modifiedBy")
        detail.createTime.dateTimeString.contains("9/28/20")
        detail.updateTime.dateTimeString.contains("9/29/20")

        getDetailPropAsString(detail, "qualifiedName") == instance.getInstanceProperty("name")
        getDetailPropAsString(detail, "description") == instance.getInstanceProperty("description")
        getDetailPropAsString(detail, "author") == instance.getInstanceProperty("createdBy")
        getAdditionalProperty(detail, "sasLabel") == instance.getInstanceProperty("label")
        getAdditionalProperty(detail, "sasModelSource") == "Design Model"
        detail.instanceURL.contains("/catalog/instances/" + instance.guid)
    }

    def "GetEntityDetail - Analytic Store"() {
        // Setup test instance
        SASCatalogObject instance = new SASCatalogObject()
        instance.addInstanceProperty("name", "Catalog SAS Analytic Store")
        instance.addInstanceProperty("label", "SAS Analytic Store")
        instance.addInstanceProperty("description", "SAS Analytic Store from mock client")
        instance.addInstanceProperty("createdBy", "steven")
        instance.addInstanceProperty("modifiedBy", "ben")
        instance.addInstanceProperty("creationTimeStamp", "2020-09-28T18:40:02.788574Z")
        instance.addInstanceProperty("modifiedTimeStamp", "2020-09-29T18:40:02.788574Z")
        instance.addInstanceProperty("version", (double) 1)
        instance.addDefinitionProperty("name", "analyticStore")
        Map<String, Object> attributes = new HashMap<>()
        attributes.put("dateCreated", Date.from(java.time.Instant.parse("2020-08-28T18:40:02.788574Z")))
        attributes.put("dateModified", Date.from(java.time.Instant.parse("2020-08-29T18:40:02.788574Z")))
        instance.attributes = attributes
        instance.guid = "2dbeed13-362d-49da-ab86-01b79e33e8bf"

        EntityMappingSASCatalog2OMRS mapper
        EntityDetail detail

        when: "I get entity detail with no prefix"
        mapper = new EntityMappingSASCatalog2OMRS(repositoryConnector, typeDefStore, null, instance, null, "steven")
        detail = mapper.getEntityDetail()
        then: "Values should be mapped correctly"
        detail.getType().getTypeDefName() == conceptModelElemTypeDef.getName()
        detail.createdBy == instance.getInstanceProperty("createdBy")
        detail.updatedBy == instance.getInstanceProperty("modifiedBy")
        detail.createTime.dateTimeString.contains("9/28/20")
        detail.updateTime.dateTimeString.contains("9/29/20")

        getDetailPropAsString(detail, "qualifiedName") == instance.getInstanceProperty("name")
        getDetailPropAsString(detail, "description") == instance.getInstanceProperty("description")
        getDetailPropAsString(detail, "displayName") == instance.getInstanceProperty("label")
        getDetailPropAsString(detail, "author") == instance.getInstanceProperty("createdBy")
        getAdditionalProperty(detail, "sasModelSource") == "Analytic Store"
        detail.instanceURL.contains("/catalog/instances/" + instance.guid)
    }
}
