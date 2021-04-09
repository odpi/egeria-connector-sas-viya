//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.repository.connector.mapping;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

/**
 * Represents a single TypeDef mapping, to safely parse TypeDefMappings.json resource.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonTypeName("TypeDefMapping")
public class MappingFromFile {

    /**
     * The name of the TypeDef within  Catalog.
     */
    private String catalog;

    /**
     * The name of the TypeDef within OMRS.
     */
    private String omrs;


    /**
     * The prefix to use for any generated OMRS TypeDefs based on a singular SAS Catalog type.
     */
    private String prefix;


    /**
     * An array of mappings between  Catalog and OMRS property names for the TypeDef.
     */
    private List<MappingFromFile> propertyMappings;

    /**
     * An array of mappings between  Catalog endpoint property names and OMRS endpoing property names,
     * for any relationship TypeDefs
     */
    private List<MappingFromFile> endpointMappings;

    @JsonProperty("sasCat") public String getCatalogName() { return this.catalog; }
    @JsonProperty("sasCat") public void setCatalogName(String catalog) { this.catalog = catalog; }

    @JsonProperty("omrs") public String getOMRSName() { return this.omrs; }
    @JsonProperty("omrs") public void setOMRSName(String omrs) { this.omrs = omrs; }

    @JsonProperty("prefix") public String getPrefix() { return this.prefix; }
    @JsonProperty("prefix") public void setPrefix(String prefix) { this.prefix = prefix; }

    @JsonProperty("propertyMappings") public List<MappingFromFile> getPropertyMappings() { return this.propertyMappings; }
    @JsonProperty("propertyMappings") public void setPropertyMappings(List<MappingFromFile> propertyMappings) { this.propertyMappings = propertyMappings; }

    @JsonProperty("endpointMappings") public List<MappingFromFile> getEndpointMappings() { return this.endpointMappings; }
    @JsonProperty("endpointMappings") public void setEndpointMappings(List<MappingFromFile> endpointMappings) { this.endpointMappings = endpointMappings; }

    @JsonIgnore
    public boolean isGeneratedType() { return this.prefix != null; }
}
