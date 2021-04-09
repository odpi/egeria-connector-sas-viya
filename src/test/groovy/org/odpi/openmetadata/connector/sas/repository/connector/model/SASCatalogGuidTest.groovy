package org.odpi.openmetadata.connector.sas.repository.connector.model


import spock.lang.Specification

class SASCatalogGuidTest extends Specification {
    def "GetSASCatalogGuid"() {
        when: "Valid guid is created without prefix"
        SASCatalogGuid guid = new SASCatalogGuid("cfaae74d-5d49-40b6-8707-4a448e5323e9", null)
        then: "getSASCatalogGuid() returns correct guid"
        guid.getSASCatalogGuid() == "cfaae74d-5d49-40b6-8707-4a448e5323e9"

        when: "Valid guid is created with prefix"
        guid = new SASCatalogGuid("cfaae74d-5d49-40b6-8707-4a448e5323e9", "RTT")
        then: "getSASCatalogGuid() returns correct guid"
        guid.getSASCatalogGuid() == "cfaae74d-5d49-40b6-8707-4a448e5323e9"

        when: "Guid is empty"
        guid = new SASCatalogGuid("", null)
        then: "getSASCatalogGuid() returns empty string"
        guid.getSASCatalogGuid() == ""

        when: "Guid is null"
        guid = new SASCatalogGuid(null, null)
        then: "getSASCatalogGuid() returns null"
        guid.getSASCatalogGuid() == null
    }

    def "GetGeneratedPrefix"() {
        when: "Valid guid is created with prefix"
        SASCatalogGuid guid = new SASCatalogGuid("cfaae74d-5d49-40b6-8707-4a448e5323e9", "RTT")
        then: "getGeneratedPrefix() returns correct prefix"
        guid.getGeneratedPrefix() == "RTT"

        when: "guid is created with prefix and no guid"
        guid = new SASCatalogGuid(null, "RTT")
        then: "getGeneratedPrefix() returns correct prefix"
        guid.getGeneratedPrefix() == "RTT"

        when: "prefix is empty"
        guid = new SASCatalogGuid("cfaae74d-5d49-40b6-8707-4a448e5323e9", "")
        then: "getGeneratedPrefix() returns empty string"
        guid.getGeneratedPrefix() == ""

        when: "Prefix is null"
        guid = new SASCatalogGuid("cfaae74d-5d49-40b6-8707-4a448e5323e9", null)
        then: "getGeneratedPrefix() returns null"
        guid.getGeneratedPrefix() == null
    }


    def "FromGuid"() {
        when: "Guid has no prefix"
        SASCatalogGuid guid = SASCatalogGuid.fromGuid("cfaae74d-5d49-40b6-8707-4a448e5323e9")
        then: "Guid should be correct and prefix should be null"
        guid.getSASCatalogGuid() == "cfaae74d-5d49-40b6-8707-4a448e5323e9"
        guid.getGeneratedPrefix() == null

        when: "Guid has prefix with !"
        guid = SASCatalogGuid.fromGuid("RTT!cfaae74d-5d49-40b6-8707-4a448e5323e9")
        then: "Guid should be correct and prefix should be up to !"
        guid.getSASCatalogGuid() == "cfaae74d-5d49-40b6-8707-4a448e5323e9"
        guid.getGeneratedPrefix() == "RTT"

        when: "Guid starts with !"
        guid = SASCatalogGuid.fromGuid("!cfaae74d-5d49-40b6-8707-4a448e5323e9")
        then: "Guid should be correct and prefix should be null"
        guid.getSASCatalogGuid() == "!cfaae74d-5d49-40b6-8707-4a448e5323e9"
        guid.getGeneratedPrefix() == null
    }


    def "IsGeneratedInstanceGuid"() {
        when: "Guid has a prefix"
        SASCatalogGuid guid = new SASCatalogGuid("cfaae74d-5d49-40b6-8707-4a448e5323e9", "RTT")
        then: "IsGeneratedInstanceGuid() is true"
        guid.isGeneratedInstanceGuid()

        when: "Guid does not have a prefix"
        guid = new SASCatalogGuid("cfaae74d-5d49-40b6-8707-4a448e5323e9", null)
        then: "IsGeneratedInstanceGuid() is false"
        !guid.isGeneratedInstanceGuid()
    }

}
