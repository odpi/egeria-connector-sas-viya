//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.repository.connector.model;

import java.util.Objects;

/**
 * Captures the meaning and translation of the various components of an SAS GUID.
 *
 * This is necessary to cover those scenarios where an instance is generated for SAS, because what is a single
 * instance in SAS is actually represented as multiple instances in OMRS. As a result, the GUIDs for these
 * generated entities will require some prefixing to allow them to be properly handled.
 */
public class SASCatalogGuid {

    private static final String GENERATED_TYPE_POSTFIX = "!";

    private String SASCatalogGuid;
    private String generatedPrefix;

    /**
     * Create a new SAS GUID that has a prefix (for an OMRS instance type that does not actually exist in Apache
     * SAS, but is generated from another instance type in SAS)
     *
     * @param SASCatalogGuid the GUID of the SAS asset
     * @param prefix the prefix to use to uniquely identify this generated instance's GUID
     */
    public SASCatalogGuid(String SASCatalogGuid, String prefix) {
        this.generatedPrefix = prefix;
        this.SASCatalogGuid = SASCatalogGuid;
    }

    /**
     * Turn this SAS GUID into a unique String representation of the GUID.
     *
     * The string representation will be something like the following:
     * {@literal database_schema@5e74232d-92df-4b81-a401-b100dbfea73a:RDBST!6662c0f2.ee6a64fe.o1h6eveh1.gbvjvq0.ols3j6.0oadmdn8gknhjvmojr3pt}
     *
     * @return String
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (generatedPrefix != null) {
            sb.append(generatedPrefix);
            sb.append(GENERATED_TYPE_POSTFIX);
        }
        sb.append(SASCatalogGuid);
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SASCatalogGuid)) return false;
        SASCatalogGuid that = (SASCatalogGuid) obj;
        return Objects.equals(getSASCatalogGuid(), that.getSASCatalogGuid()) &&
                Objects.equals(getGeneratedPrefix(), that.getGeneratedPrefix());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(getSASCatalogGuid(), getGeneratedPrefix());
    }

    /**
     * Attempt to create a new SAS GUID from the provided GUID.
     *
     * @param guidToConvert the SAS GUID representation of the provided GUID, or null if it does not appear to
     *                      be a valid SAS GUID
     * @return SASCatalogGuid
     */
    public static SASCatalogGuid fromGuid(String guidToConvert) {

        if (guidToConvert == null) {
            return null;
        }

        String SASGuid = guidToConvert;
        String generatedPrefix = null;
        int indexOfGeneratedPostfix = guidToConvert.indexOf(GENERATED_TYPE_POSTFIX);
        if (indexOfGeneratedPostfix > 0) {
            generatedPrefix = guidToConvert.substring(0, indexOfGeneratedPostfix);
            SASGuid = guidToConvert.substring(indexOfGeneratedPostfix + 1);
        }
        return new SASCatalogGuid(SASGuid, generatedPrefix);

    }

    /**
     * Retrieve the generated prefix component of this SAS GUID, if it is for a generated instance (or null if
     * the instance is not generated).
     *
     * @return String
     */
    public String getGeneratedPrefix() { return generatedPrefix; }

    /**
     * Indicates whether this SAS GUID represents a generated instance (true) or not (false).
     *
     * @return boolean
     */
    public boolean isGeneratedInstanceGuid() { return generatedPrefix != null; }

    /**
     * Retrieve the SAS GUID.
     *
     * @return String
     */
    public String getSASCatalogGuid() { return SASCatalogGuid; }

}
