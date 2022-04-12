//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.repository.connector.mapping;

import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EnumPropertyValue;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceProperties;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstancePropertyCategory;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstancePropertyValue;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.PrimitivePropertyValue;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.AttributeTypeDefCategory;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.EnumDef;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.EnumElementDef;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.PrimitiveDef;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.PrimitiveDefCategory;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDefAttribute;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The base class for all mappings between OMRS AttributeTypeDefs and Sas properties.
 */
public abstract class AttributeMapping {

    private static final Logger log = LoggerFactory.getLogger(AttributeMapping.class);

    private AttributeMapping() {
        // Do nothing...
    }

    /**
     * Indicates whether the provided OMRS and Sas values match (true) or not (false).
     *
     * @param omrsValue the OMRS property value to compare
     * @param sasValue the Sas value to compare
     * @return boolean
     */
    public static boolean valuesMatch(InstancePropertyValue omrsValue, Object sasValue) {

        if (omrsValue == null && sasValue == null) {
            return true;
        } else if (omrsValue != null && sasValue != null) {
            boolean bMatch = false;
            switch (omrsValue.getInstancePropertyCategory()) {
                case ENUM:
                    EnumPropertyValue enumValue = (EnumPropertyValue) omrsValue;
                    bMatch = enumValue.getSymbolicName().equals(sasValue);
                    break;
                case PRIMITIVE:
                    PrimitivePropertyValue primitivePropertyValue = (PrimitivePropertyValue) omrsValue;
                    switch (primitivePropertyValue.getPrimitiveDefCategory()) {
                        case OM_PRIMITIVE_TYPE_BIGDECIMAL:
                            BigDecimal bigDecimal = (BigDecimal) primitivePropertyValue.getPrimitiveValue();
                            bMatch = bigDecimal.equals(sasValue);
                            break;
                        case OM_PRIMITIVE_TYPE_BIGINTEGER:
                            BigInteger bigInteger = (BigInteger) primitivePropertyValue.getPrimitiveValue();
                            bMatch = bigInteger.equals(sasValue);
                            break;
                        case OM_PRIMITIVE_TYPE_BOOLEAN:
                            Boolean boolVal = (Boolean) primitivePropertyValue.getPrimitiveValue();
                            bMatch = boolVal.equals(sasValue);
                            break;
                        case OM_PRIMITIVE_TYPE_BYTE:
                            Byte byteVal = (Byte) primitivePropertyValue.getPrimitiveValue();
                            bMatch = byteVal.equals(sasValue);
                            break;
                        case OM_PRIMITIVE_TYPE_CHAR:
                            Character charVal = (Character) primitivePropertyValue.getPrimitiveValue();
                            bMatch = charVal.equals(sasValue);
                            break;
                        case OM_PRIMITIVE_TYPE_DOUBLE:
                            Double doubleVal = (Double) primitivePropertyValue.getPrimitiveValue();
                            bMatch = doubleVal.equals(sasValue);
                            break;
                        case OM_PRIMITIVE_TYPE_FLOAT:
                            Float floatVal = (Float) primitivePropertyValue.getPrimitiveValue();
                            bMatch = floatVal.equals(sasValue);
                            break;
                        case OM_PRIMITIVE_TYPE_INT:
                            Integer intVal = (Integer) primitivePropertyValue.getPrimitiveValue();
                            bMatch = intVal.equals(sasValue);
                            break;
                        case OM_PRIMITIVE_TYPE_LONG:
                            Long longVal = (Long) primitivePropertyValue.getPrimitiveValue();
                            bMatch = longVal.equals(sasValue);
                            break;
                        case OM_PRIMITIVE_TYPE_SHORT:
                            Short shortVal = (Short) primitivePropertyValue.getPrimitiveValue();
                            bMatch = shortVal.equals(sasValue);
                            break;
                        case OM_PRIMITIVE_TYPE_STRING:
                            String stringVal = (String) primitivePropertyValue.getPrimitiveValue();
                            if (stringVal != null) {
                                String toCompare = (String) sasValue;
                                bMatch = toCompare.matches(stringVal);
                            }
                            break;
                        case OM_PRIMITIVE_TYPE_DATE:
                            Long dateVal = (Long) primitivePropertyValue.getPrimitiveValue();
                            if (sasValue instanceof Date) {
                                bMatch = dateVal.equals(((Date) sasValue).getTime());
                            } else {
                                bMatch = dateVal.equals(sasValue);
                            }
                            break;
                        default:
                            log.warn("Unhandled type for mapping: {}", omrsValue);
                            break;
                    }
                    break;
                default:
                    log.warn("Unhandled type for mapping: {}", omrsValue);
                    break;
            }
            return bMatch;
        } else {
            return false;
        }

    }

    /**
     * Add the provided property value to the set of instance properties.
     *
     * @param repositoryHelper the OMRS repository helper
     * @param repositoryName name of caller
     * @param property the property
     * @param properties properties object to add property to (may be null)
     * @param propertyValue value of property
     * @param methodName calling method
     * @return InstanceProperties
     */
    static InstanceProperties addPropertyToInstance(OMRSRepositoryHelper repositoryHelper,
                                                    String repositoryName,
                                                    TypeDefAttribute property,
                                                    InstanceProperties properties,
                                                    Object propertyValue,
                                                    String methodName) {

        InstanceProperties resultingProperties = properties;

        switch (property.getAttributeType().getCategory()) {
            /*case ENUM_DEF:
                AttributeMapping.addEnumPropertyToInstance(
                        resultingProperties,
                        property,
                        attributeDefStore.getElementMappingsForOMRSTypeDef(property.getAttributeType().getName()),
                        propertyValue
                );
                break;*/
            case PRIMITIVE:
                resultingProperties = AttributeMapping.addPrimitivePropertyToInstance(
                        repositoryHelper,
                        repositoryName,
                        resultingProperties,
                        property,
                        propertyValue,
                        methodName
                );
                break;
            default:
                log.warn("Unhandled type for mapping: {}", property);
                break;
        }

        return resultingProperties;

    }

    /**
     * Retrieves a simple Java object representation for the provided OMRS InstancePropertyValue.
     *
     * @param omrsValue the OMRS value to translate
     * @return Object

    static Object getValueFromInstance(InstancePropertyValue omrsValue,
                                       String omrsTypeDefName,
                                       AttributeTypeDefStore attributeDefStore) {

        Object value = null;

        switch (omrsValue.getInstancePropertyCategory()) {
            case PRIMITIVE:
                PrimitivePropertyValue primitivePropertyValue = (PrimitivePropertyValue) omrsValue;
                value = primitivePropertyValue.getPrimitiveValue();
                break;
            case MAP:
                MapPropertyValue mapPropertyValue = (MapPropertyValue) omrsValue;
                InstanceProperties mapValues = mapPropertyValue.getMapValues();
                if (mapValues != null) {
                    Map<String, InstancePropertyValue> mapOfValues = mapValues.getInstanceProperties();
                    if (mapOfValues != null) {
                        Map<String, Object> mappedValues = new HashMap<>();
                        for (Map.Entry<String, InstancePropertyValue> entry : mapOfValues.entrySet()) {
                            String entryName = entry.getKey();
                            InstancePropertyValue entryValue = entry.getValue();
                            mappedValues.put(entryName, getValueFromInstance(entryValue, omrsTypeDefName, attributeDefStore));
                        }
                        value = mappedValues;
                    }
                }
                break;
            case ENUM:
                Map<String, String> enumElementMap = attributeDefStore.getElementMappingsForOMRSTypeDef(omrsTypeDefName);
                EnumPropertyValue enumPropertyValue = (EnumPropertyValue) omrsValue;
                value = enumElementMap.get(enumPropertyValue.getSymbolicName());
                break;
            case ARRAY:
                ArrayPropertyValue arrayPropertyValue = (ArrayPropertyValue) omrsValue;
                InstanceProperties arrayValues = arrayPropertyValue.getArrayValues();
                if (arrayValues != null) {
                    Map<String, InstancePropertyValue> arrayOfValues = arrayValues.getInstanceProperties();
                    if (arrayOfValues != null) {
                        List<Object> mappedValues = new ArrayList<>(arrayValues.getPropertyCount());
                        for (Map.Entry<String, InstancePropertyValue> entry : arrayOfValues.entrySet()) {
                            String entryKey = entry.getKey();
                            int entryIndex  = Integer.parseInt(entryKey);
                            InstancePropertyValue entryValue = entry.getValue();
                            mappedValues.set(entryIndex, getValueFromInstance(entryValue, omrsTypeDefName, attributeDefStore));
                        }
                        value = mappedValues;
                    }
                }
                break;
            default:
                log.warn("Unhandled type for mapping: {}", omrsValue);
                break;
        }

        return value;

    }
     */

    /**
     * Add the supplied property to an instance properties object.  If the instance property object
     * supplied is null, a new instance properties object is created.
     *
     * @param omrsRepositoryHelper the OMRS repository helper
     * @param sourceName  name of caller
     * @param properties  properties object to add property to may be null.
     * @param property  the property
     * @param propertyValue  value of property
     * @param methodName  calling method name
     * @return instance properties object.
     */
    private static InstanceProperties addPrimitivePropertyToInstance(OMRSRepositoryHelper omrsRepositoryHelper,
                                                                     String sourceName,
                                                                     InstanceProperties properties,
                                                                     TypeDefAttribute property,
                                                                     Object propertyValue,
                                                                     String methodName) {

        InstanceProperties resultingProperties = properties;

        if (propertyValue != null) {
            String propertyName = property.getAttributeName();
            log.debug("Adding property {} for {}", propertyName, methodName);

            if (property.getAttributeType().getCategory() == AttributeTypeDefCategory.PRIMITIVE) {
                try {
                    PrimitiveDef primitiveDef = (PrimitiveDef) property.getAttributeType();
                    switch (primitiveDef.getPrimitiveDefCategory()) {
                        case OM_PRIMITIVE_TYPE_BOOLEAN:
                            boolean booleanValue;
                            if (propertyValue instanceof Boolean) {
                                booleanValue = (Boolean) propertyValue;
                            } else {
                                booleanValue = Boolean.valueOf(propertyValue.toString());
                            }
                            resultingProperties = omrsRepositoryHelper.addBooleanPropertyToInstance(
                                    sourceName,
                                    properties,
                                    propertyName,
                                    booleanValue,
                                    methodName
                            );
                            break;
                        case OM_PRIMITIVE_TYPE_INT:
                            int intValue;
                            if (propertyValue instanceof Integer) {
                                intValue = (Integer) propertyValue;
                            } else if (propertyValue instanceof Number) {
                                intValue = ((Number) propertyValue).intValue();
                            } else {
                                intValue = Integer.valueOf(propertyValue.toString());
                            }
                            resultingProperties = omrsRepositoryHelper.addIntPropertyToInstance(
                                    sourceName,
                                    properties,
                                    propertyName,
                                    intValue,
                                    methodName
                            );
                            break;
                        case OM_PRIMITIVE_TYPE_LONG:
                            long longValue;
                            if (propertyValue instanceof Long) {
                                longValue = (Long) propertyValue;
                            } else if (propertyValue instanceof Number) {
                                longValue = ((Number) propertyValue).longValue();
                            } else {
                                longValue = Long.valueOf(propertyValue.toString());
                            }
                            resultingProperties = omrsRepositoryHelper.addLongPropertyToInstance(
                                    sourceName,
                                    properties,
                                    propertyName,
                                    longValue,
                                    methodName
                            );
                            break;
                        case OM_PRIMITIVE_TYPE_FLOAT:
                            float floatValue;
                            if (propertyValue instanceof Float) {
                                floatValue = (Float) propertyValue;
                            } else if (propertyValue instanceof Number) {
                                floatValue = ((Number) propertyValue).floatValue();
                            } else {
                                floatValue = Float.valueOf(propertyValue.toString());
                            }
                            resultingProperties = omrsRepositoryHelper.addFloatPropertyToInstance(
                                    sourceName,
                                    properties,
                                    propertyName,
                                    floatValue,
                                    methodName
                            );
                            break;
                        case OM_PRIMITIVE_TYPE_STRING:
                            String stringValue;
                            if (propertyValue instanceof String) {
                                stringValue = (String) propertyValue;
                            } else {
                                stringValue = propertyValue.toString();
                            }
                            resultingProperties = omrsRepositoryHelper.addStringPropertyToInstance(
                                    sourceName,
                                    properties,
                                    propertyName,
                                    stringValue,
                                    methodName
                            );
                            break;
                        case OM_PRIMITIVE_TYPE_DATE:
                            Date date;
                            if (propertyValue instanceof Date) {
                                date = (Date) propertyValue;
                            } else if(propertyValue instanceof String && ((String)propertyValue).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{2,3}Z")) {
                                // Timestamp is ISO-8601
                                // https://stackoverflow.com/a/60214805
                                TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse((String) propertyValue);
                                date = Date.from(Instant.from(ta));
                            } else {
                                date = new Date((Long)propertyValue);
                            }

                            resultingProperties = omrsRepositoryHelper.addDatePropertyToInstance(
                                    sourceName,
                                    properties,
                                    propertyName,
                                    date,
                                    methodName
                            );
                            break;
                        default:
                            log.error("Unhandled primitive type {} for {}", primitiveDef.getPrimitiveDefCategory(), propertyName);
                    }
                } catch (ClassCastException e) {
                    log.error("Unable to cast {} to {} for {}", propertyValue, property.getAttributeType(), propertyName);
                } catch (NumberFormatException e) {
                    log.warn("Unable to convert {} to {} for {}", propertyValue, property.getAttributeType(), propertyName);
                }
            } else {
                log.error("Cannot translate non-primitive property {} this way.", propertyName);
            }
        } else {
            log.debug("Null property");
        }

        return resultingProperties;

    }

    /**
     * Add the supplied property to an instance properties object.  If the instance property object
     * supplied is null, a new instance properties object is created.
     *
     * @param properties properties object to add property to may be null.
     * @param property the property
     * @param sasElementValueToOmrsElementValue mapping from Sas enumeration values to OMRS enumeration values
     * @param propertyValue value of property
     * @return InstanceProperties
     */
    private static InstanceProperties addEnumPropertyToInstance(InstanceProperties properties,
                                                                TypeDefAttribute property,
                                                                Map<String, Set<String>> sasElementValueToOmrsElementValue,
                                                                Object propertyValue) {

        String propertyName = property.getAttributeName();

        if (propertyValue != null) {

            String omrsValue = null;
            if (sasElementValueToOmrsElementValue != null) {
                for (Map.Entry<String, Set<String>> entry : sasElementValueToOmrsElementValue.entrySet()) {
                    String cSas = entry.getKey();
                    if (cSas.equals(propertyValue)) {
                        Set<String> possibleValues = entry.getValue();
                        // We will just take the first value we find in the set, since we can only map to one
                        omrsValue = possibleValues.iterator().next();
                        break;
                    }
                }
            }
            if (omrsValue != null) {
                EnumDef omrsEnumProperty = (EnumDef) property.getAttributeType();
                List<EnumElementDef> omrsElements = omrsEnumProperty.getElementDefs();
                EnumElementDef omrsEnumValue = null;
                for (EnumElementDef omrsElement : omrsElements) {
                    String cOmrs = omrsElement.getValue();
                    if (cOmrs.equals(omrsValue)) {
                        omrsEnumValue = omrsElement;
                        break;
                    }
                }
                if (omrsEnumValue != null) {
                    EnumPropertyValue enumPropertyValue = new EnumPropertyValue();
                    enumPropertyValue.setDescription(omrsEnumValue.getDescription());
                    enumPropertyValue.setOrdinal(omrsEnumValue.getOrdinal());
                    enumPropertyValue.setSymbolicName(omrsEnumValue.getValue());
                    properties.setProperty(propertyName, enumPropertyValue);
                } else {
                    log.warn("Unable to find mapped enumeration value for property '{}': {}", propertyName, propertyValue);
                }
            } else {
                log.warn("Unable to find mapped enumeration value for property '{}': {}", propertyName, propertyValue);
            }

        } else {
            log.debug("Null property");
        }

        return properties;

    }

    /**
     * Comparator input for sorting based on an InstancePropertyValue. Note that this will assume that both v1 and v2
     * are the same type of property value (eg. both same type of primitive)
     *
     * @param v1 first value to compare
     * @param v2 second value to compare
     * @return int
     */
    public static int compareInstanceProperty(InstancePropertyValue v1, InstancePropertyValue v2) {

        int result = 0;
        if (v1 == v2) {
            result = 0;
        } else if (v1 == null) {
            result = -1;
        } else if (v2 == null) {
            result = 1;
        } else {

            InstancePropertyCategory category = v1.getInstancePropertyCategory();
            if (category.equals(InstancePropertyCategory.PRIMITIVE)) {
                PrimitivePropertyValue pv1 = (PrimitivePropertyValue) v1;
                PrimitivePropertyValue pv2 = (PrimitivePropertyValue) v2;
                PrimitiveDefCategory primitiveCategory = pv1.getPrimitiveDefCategory();
                switch (primitiveCategory) {
                    case OM_PRIMITIVE_TYPE_INT:
                        result = ((Integer) pv1.getPrimitiveValue() - (Integer) pv2.getPrimitiveValue());
                        break;
                    case OM_PRIMITIVE_TYPE_BYTE:
                        result = ((Byte) pv1.getPrimitiveValue()).compareTo((Byte) pv2.getPrimitiveValue());
                        break;
                    case OM_PRIMITIVE_TYPE_CHAR:
                        result = ((Character) pv1.getPrimitiveValue()).compareTo((Character) pv2.getPrimitiveValue());
                        break;
                    case OM_PRIMITIVE_TYPE_STRING:
                        result = ((String) pv1.getPrimitiveValue()).compareTo((String) pv2.getPrimitiveValue());
                        break;
                    case OM_PRIMITIVE_TYPE_DATE:
                    case OM_PRIMITIVE_TYPE_LONG:
                        result = ((Long) pv1.getPrimitiveValue()).compareTo((Long) pv2.getPrimitiveValue());
                        break;
                    case OM_PRIMITIVE_TYPE_FLOAT:
                        result = ((Float) pv1.getPrimitiveValue()).compareTo((Float) pv2.getPrimitiveValue());
                        break;
                    case OM_PRIMITIVE_TYPE_SHORT:
                        result = ((Short) pv1.getPrimitiveValue()).compareTo((Short) pv2.getPrimitiveValue());
                        break;
                    case OM_PRIMITIVE_TYPE_DOUBLE:
                        result = ((Double) pv1.getPrimitiveValue()).compareTo((Double) pv2.getPrimitiveValue());
                        break;
                    case OM_PRIMITIVE_TYPE_BOOLEAN:
                        result = ((Boolean) pv1.getPrimitiveValue()).compareTo((Boolean) pv2.getPrimitiveValue());
                        break;
                    case OM_PRIMITIVE_TYPE_BIGDECIMAL:
                        result = ((BigDecimal) pv1.getPrimitiveValue()).compareTo((BigDecimal) pv2.getPrimitiveValue());
                        break;
                    case OM_PRIMITIVE_TYPE_BIGINTEGER:
                        result = ((BigInteger) pv1.getPrimitiveValue()).compareTo((BigInteger) pv2.getPrimitiveValue());
                        break;
                    default:
                        result = pv1.getPrimitiveValue().toString().compareTo(pv2.getPrimitiveValue().toString());
                        break;
                }
            } else {
                log.warn("Unhandled instance value type for comparison: {}", category);
            }

        }
        return result;

    }

}
