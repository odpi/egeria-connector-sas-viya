package org.odpi.openmetadata.connector.sas.repository.connector.mapping

import spock.lang.Specification

class SASCatalogObjectTest extends Specification {
    def "Get"() {
        SASCatalogObject instance

        when: "I add properties to the instance object"
        instance = new SASCatalogObject()
        instance.addInstanceProperty("test", "instanceVal")
        instance.addDefinitionProperty("test", "definitionVal")
        Map<String, String> attributes = new HashMap<>()
        attributes.put("test", "attributeVal")
        instance.attributes = attributes
        then: "Get() returns the right properties based on the prefix"
        instance.get("instance.test") == "instanceVal"
        instance.get("definition.test") == "definitionVal"
        instance.get("attribute.test") == "attributeVal"
        // No prefix = null
        instance.get("test") == null

    }

    def "GetTypeName"() {
        SASCatalogObject instance

        when: "I add the name definition property"
        instance = new SASCatalogObject()
        instance.addDefinitionProperty("name", "test")
        then: "getTypeName() returns that value"
        instance.getTypeName() == "test"

        when: "The type is reference"
        instance = new SASCatalogObject()
        instance.addDefinitionProperty("name", "reference")
        Map<String, String> attributes = new HashMap<>()
        attributes.put("referencedType", "testRef")
        instance.attributes = attributes

        then: "getTypeName() returns 'reference.<referencedType>'"
        instance.getTypeName() == "reference.testRef"

        when: "The def. name is relatedObjects"
        instance = new SASCatalogObject()
        instance.addDefinitionProperty("name", "relatedObjects")
        instance.addInstanceProperty("type", "testType")

        then: "getTypeName() returns the type property"
        instance.getTypeName() == "testType"
    }

    def "GetAttributes"() {
        SASCatalogObject instance

        when: "I add attributes to the instance object"
        instance = new SASCatalogObject()
        Map<String, String> attributes = new HashMap<>()
        attributes.put("test1", "test1Val")
        attributes.put("test2", "test2Val")
        attributes.put("test3", "test3Val")
        instance.attributes = attributes
        def returnedAttributes = instance.getAttributes()
        then: "getAttributes() returns the correct attributes"
        returnedAttributes.size() == 3
        returnedAttributes.get("test1") == "test1Val"
        returnedAttributes.get("test2") == "test2Val"
        returnedAttributes.get("test3") == "test3Val"
    }

    def "GetGuid"() {
        SASCatalogObject instance

        when: "I set the guid on the instance object"
        instance = new SASCatalogObject()
        instance.guid = "cfaae74d-5d49-40b6-8707-4a448e5323e9"
        then: "getGuid() returns the correct guid"
        instance.getGuid() == "cfaae74d-5d49-40b6-8707-4a448e5323e9"
    }
}
