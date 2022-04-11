//---------------------------------------------------------------------------
// Copyright (c) 2020, SAS Institute Inc., Cary, NC, USA.  All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
//---------------------------------------------------------------------------

package org.odpi.openmetadata.connector.sas.client;

import com.google.gson.Gson;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.odpi.openmetadata.connector.sas.event.model.catalog.instance.Instance;
import org.odpi.openmetadata.connector.sas.repository.connector.mapping.SASCatalogObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SASCatalogRestClient implements SASCatalogClient {

    private static final String SYSPROP_TRUST_ALL       = "sas.egeria.repositoryconnector.ssl.trustAll";
    private static final String SYSPROP_TRUSTSTORE_NAME = "sas.egeria.repositoryconnector.ssl.trustStore";
    private static final String SYSPROP_TRUSTSTORE_PWD  = "sas.egeria.repositoryconnector.ssl.trustStorePassword";

    private static final int MAX_RETRIES = 1;
    private CloseableHttpClient httpClient;
    private String baseURL;
    private String username;
    private String password;
    private String scheme;
    private String token;

    private static final Logger log = LoggerFactory.getLogger(SASCatalogRestClient.class);

    public SASCatalogRestClient(String baseURL, String username, String password) throws Exception {
        boolean trustAllCerts = System.getProperty(SYSPROP_TRUST_ALL,"false").equalsIgnoreCase("true");
        String trustStorePath = System.getProperty(SYSPROP_TRUSTSTORE_NAME, "");

        if (trustAllCerts) {
            // ********************************************************************
            // NOTE: THIS DISABLES SSL CERTIFICATE VERIFICATION (Use with caution)
            // ********************************************************************
            this.httpClient = HttpClients
                    .custom()
                    .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
        } else if (StringUtils.isNotEmpty(trustStorePath)) {
            // **************************************************************************************************
            // SSL is being used and we wish to install a set of certificates we accept when talking to SAS Viya
            // **************************************************************************************************
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(new FileInputStream(trustStorePath), System.getProperty(SYSPROP_TRUSTSTORE_PWD,"").toCharArray());
            this.httpClient = HttpClients
                    .custom()
                    .setSSLContext(new SSLContextBuilder().loadTrustMaterial(keystore, null).build())
                    .build();
        } else {
            // **********************
            // Use default HttpClient
            // **********************
            this.httpClient = HttpClients.createDefault();
        }

        this.baseURL = baseURL;
        this.username = username;
        this.password = password;
        URIBuilder builder = new URIBuilder(this.baseURL);
        this.scheme = builder.getScheme();
        log.info("Creating catalog client with base URL: " + this.baseURL);
        // Get initial token
        setAuthToken(username, password);
    }

    private void setAuthToken(String username, String password) throws Exception {
        URIBuilder builder = new URIBuilder(this.scheme + "://sas-logon-app/SASLogon/oauth/token");

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", "password"));
        params.add(new BasicNameValuePair("username", username));
        params.add(new BasicNameValuePair("password", password));

        HttpPost httpPost = new HttpPost(builder.build());
        httpPost.setEntity(new UrlEncodedFormEntity(params));

        httpPost.addHeader("Authorization", "Basic c2FzLmVjOg==");
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            log.info("Get Auth Token: " + response.getStatusLine());
            HttpEntity entity = response.getEntity();
            InputStreamReader reader = new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8);
            Map map = new Gson().fromJson(reader, Map.class);
            if(map.containsKey("access_token")) {
                this.token = (String) map.get("access_token");
            } else {
                throw new RuntimeException("SASLogon response does not contain access token.");
            }
        }
    }

    @Override
    public SASCatalogObject getInstanceByGuid(String guid, String type) throws Exception {
        return getInstanceByGuid(guid, type, 0);
    }

    private SASCatalogObject getInstanceByGuid(String guid, String type, int retries) throws Exception {

        if(retries > MAX_RETRIES) {
            throw new RuntimeException("Could not complete request after " + retries + " retries.");
        }

        SASCatalogObject instanceInfo = new SASCatalogObject();

        URIBuilder builder = new URIBuilder(this.scheme + "://sas-catalog/catalog/instances/" + guid);
        HttpGet httpGet = new HttpGet(builder.build());
        addAuthHeader(httpGet);
        httpGet.addHeader("Accept", String.format("application/vnd.sas.metadata.instance.%s+json", type));
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            log.info("Get Instance (" + guid + "): " + response.getStatusLine());

            if(response.getStatusLine().getStatusCode() == 401) {
                response.close();
                setAuthToken(username, password);
                return getInstanceByGuid(guid, type, retries+1);
            } else if (response.getStatusLine().getStatusCode() != 200) {
                return null;
            }

            HttpEntity entity = response.getEntity();
            InputStreamReader reader = new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8);
            Map instance = new Gson().fromJson(reader, Map.class);

            instanceInfo = instanceMapToInstance(instance, guid, type);
        }

        return instanceInfo;
    }

    @Override
    public List<Instance> getInstancesWithParams(Map<String, String> params) throws Exception {
        return getInstancesWithFilter(params, null, 0);
    }

    @Override
    public List<Instance> getInstancesWithParams(Map<String, String> params, Map<String, String> attributeFilter) throws Exception {
        return getInstancesWithFilter(params, attributeFilter, 0);
    }

    private List<Instance> getInstancesWithFilter(Map<String, String> params, Map<String, String> attributeFilter, int retries) throws Exception {

        if(retries > MAX_RETRIES) {
            throw new RuntimeException("Could not complete request after " + retries + " retries.");
        }

        List<Instance> instances = new ArrayList<>();

        URIBuilder builder = new URIBuilder(this.scheme + "://sas-catalog/catalog/instances");
        for(Map.Entry<String, String> param : params.entrySet()) {
            log.info("Param: " + param.getKey() + " : " + param.getValue());
            builder.addParameter(param.getKey(), param.getValue());
        }
        HttpGet httpGet = new HttpGet(builder.build());
        addAuthHeader(httpGet);
//        httpGet.addHeader("Accept", String.format("application/vnd.sas.metadata.instance.%s+json", type));
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            log.info("Get Instances with filter (" + params.toString() + "): " + response.getStatusLine());

            if(response.getStatusLine().getStatusCode() == 401) {
                response.close();
                setAuthToken(username, password);
                return getInstancesWithFilter(params, attributeFilter, retries+1);
            }

            HttpEntity entity = response.getEntity();
            InputStreamReader reader = new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8);
            Map results = new Gson().fromJson(reader, Map.class);
            List<Map<String, Object>> items = (List<Map<String, Object>>) results.get("items");
            for(Map<String, Object> instance : items) {
                Map<String, String> attributes = (Map<String, String>) instance.get("attributes");
                // Not currently possible to filter on attributes in Catalog, have to do it after the fact
                if(attributeFilter == null || matchesAttributes(attributes, attributeFilter)) {
                    Instance inst = new Instance();
                    inst.setId((String) instance.get("id"));
                    instances.add(inst);
                }
            }
        }

        return instances;
    }

    private boolean matchesAttributes(Map<String, String> attributes, Map<String, String> attributeFilter) {
        for(Map.Entry<String, String> attribute : attributeFilter.entrySet()) {
            if(!(attributes.containsKey(attribute.getKey()) && attributes.get(attribute.getKey()).equals(attribute.getValue()))) {
                return false;
            }
        }
        return true;
    }

    private void addDefinitionInfo(SASCatalogObject instanceInfo, String definitionId, String type) throws Exception {
        addDefinitionInfo(instanceInfo, definitionId, type, 0);
    }

    private void addDefinitionInfo(SASCatalogObject instanceInfo, String definitionId, String type, int retries) throws Exception {

        if(retries > MAX_RETRIES) {
            throw new RuntimeException("Could not complete request after " + retries + " retries.");
        }

        URIBuilder builder = new URIBuilder(this.scheme + "://sas-catalog/catalog/definitions/" + definitionId);
        HttpGet httpGet = new HttpGet(builder.build());
        httpGet.addHeader("Accept", String.format("application/vnd.sas.metadata.definition.%s+json", type));
        addAuthHeader(httpGet);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            log.info("Get Definition (" + definitionId + "): " + response.getStatusLine());

            if(response.getStatusLine().getStatusCode() == 401) {
                setAuthToken(username, password);
                addDefinitionInfo(instanceInfo, definitionId, type, retries+1);
                return;
            }

            HttpEntity entity = response.getEntity();
            InputStreamReader reader = new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8);
            Map definition = new Gson().fromJson(reader, Map.class);

            instanceInfo.defId = definitionId;
            instanceInfo.addDefinitionProperty("definitionType", definition.get("definitionType"));
            instanceInfo.addDefinitionProperty("name", definition.get("name"));
            instanceInfo.addDefinitionProperty("label", definition.get("label"));
            instanceInfo.addDefinitionProperty("description", definition.get("description"));
            instanceInfo.addDefinitionProperty("name", definition.get("name"));
            instanceInfo.addDefinitionProperty("baseType", definition.get("baseType"));

            instanceInfo.addDefinitionProperty("version", definition.get("version"));
            instanceInfo.addDefinitionProperty("createdBy", definition.get("createdBy"));
            instanceInfo.addDefinitionProperty("modifiedBy", definition.get("modifiedBy"));
            instanceInfo.addDefinitionProperty("creationTimeStamp", definition.get("creationTimeStamp"));
            instanceInfo.addDefinitionProperty("modifiedTimeStamp", definition.get("modifiedTimeStamp"));
        }
    }

    private void addAuthHeader(HttpRequestBase request) {
        String authHeader = String.format("Bearer %s", this.token);
        request.addHeader("Authorization", authHeader);
    }

    @Override
    public boolean definitionExistsByName(String defName, String type) throws Exception {
        return definitionExistsByName(defName, type, 0);
    }

    private boolean definitionExistsByName(String defName, String type, int retries) throws Exception {
        if(retries > MAX_RETRIES) {
            throw new RuntimeException("Could not complete request after " + retries + " retries.");
        }

        // Handle reference definitions
        if(defName.startsWith("reference.")) {
            defName = "reference";
        }

        URIBuilder builder = new URIBuilder(this.scheme + "://sas-catalog/catalog/definitions");
        builder.addParameter("filter", String.format("and(eq(name,%s),eq(definitionType,%s))", defName, type));
        HttpGet httpGet = new HttpGet(builder.build());
        addAuthHeader(httpGet);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            log.info("Definition Exists by Name (" + defName + "): " + response.getStatusLine());

            if(response.getStatusLine().getStatusCode() == 401) {
                response.close();
                setAuthToken(username, password);
                return definitionExistsByName(defName, type, retries+1);
            }

            HttpEntity entity = response.getEntity();
            InputStreamReader reader = new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8);
            Map map = new Gson().fromJson(reader, Map.class);
            return (double) map.get("count") >= 1;
        }
    }

    @Override
    public List<SASCatalogObject> getRelationshipsByEntityGuid(String guid) throws Exception {
        return getRelationshipsByEntityGuid(guid, 0);
    }

    private List<SASCatalogObject> getRelationshipsByEntityGuid(String guid, int retries) throws Exception {
        if(retries > MAX_RETRIES) {
            throw new RuntimeException("Could not complete request after " + retries + " retries.");
        }

        List<SASCatalogObject> relationships = new ArrayList<>();

        URIBuilder builder = new URIBuilder(this.scheme + "://sas-catalog/catalog/instances");
        String filter = String.format("or(eq(endpoint1Id,'%s'),eq(endpoint2Id,'%s'))", guid, guid);
        builder.addParameter("filter", filter);
        HttpGet httpGet = new HttpGet(builder.build());
        httpGet.addHeader("Accept-Item", "application/vnd.sas.metadata.instance.relationship+json");
        addAuthHeader(httpGet);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            log.info("Get relationship for entity (" + guid + "): " + response.getStatusLine());

            if(response.getStatusLine().getStatusCode() == 401) {
                response.close();
                setAuthToken(username, password);
                return getRelationshipsByEntityGuid(guid, retries+1);
            }

            HttpEntity entity = response.getEntity();
            InputStreamReader reader = new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8);
            Map map = new Gson().fromJson(reader, Map.class);
 

            List relMaps = (List) map.get("items");

            for(Object rel : relMaps) {
                Map relMap = (Map) rel;
                relationships.add(instanceMapToInstance(relMap, guid, "relationship"));
            }

            if (((double)map.get("count")) > ((double)map.get("limit"))) {
                String limit = String.format("%,.0f", (double)map.get("count"));
                String start = String.format("%,.0f", (double)map.get("limit"));
                builder.addParameter("limit", limit);
                builder.addParameter("start", start);
                httpGet = new HttpGet(builder.build());
                httpGet.addHeader("Accept-Item", "application/vnd.sas.metadata.instance.relationship+json");
                addAuthHeader(httpGet);
                try (CloseableHttpResponse responsePaged = httpClient.execute(httpGet)) {
                    log.info("Get relationship for entity (" + guid + "): " + response.getStatusLine());

                    entity = responsePaged.getEntity();
                    reader = new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8);
                    map = new Gson().fromJson(reader, Map.class);

                    relMaps = (List) map.get("items");

                    for(Object rel : relMaps) {
                        Map relMap = (Map) rel;
                        relationships.add(instanceMapToInstance(relMap, guid, "relationship"));
                    }
            }
        }
        }

        return relationships;
    }

    private SASCatalogObject instanceMapToInstance(Map instance, String guid, String type) throws Exception {
        SASCatalogObject instanceInfo = new SASCatalogObject();

        String definitionId = (String) instance.get("definitionId");

        Map<String, Object> attributes = (Map<String, Object>) instance.get("attributes");

        instanceInfo.guid = (String) instance.get("id");
        instanceInfo.addInstanceProperty("instanceType", instance.get("instanceType"));
        instanceInfo.addInstanceProperty("name", instance.get("name"));
        instanceInfo.addInstanceProperty("label", instance.get("label"));
        instanceInfo.addInstanceProperty("description", instance.get("description"));

        // Prevent null pointer if type is not present
        Object instanceType = instance.get("type");
        if(instanceType == null) {
            instanceType = "";
        }
        instanceInfo.addInstanceProperty("type", instanceType);
        instanceInfo.addInstanceProperty("version", instance.get("version"));
        instanceInfo.addInstanceProperty("createdBy", instance.get("createdBy"));
        instanceInfo.addInstanceProperty("modifiedBy", instance.get("modifiedBy"));
        instanceInfo.addInstanceProperty("creationTimeStamp", instance.get("creationTimeStamp"));
        instanceInfo.addInstanceProperty("modifiedTimeStamp", instance.get("modifiedTimeStamp"));

        if(type.equalsIgnoreCase("relationship")) {
            instanceInfo.addInstanceProperty("endpoint1Id", instance.get("endpoint1Id"));
            instanceInfo.addInstanceProperty("endpoint1Uri", instance.get("endpoint1Uri"));
            instanceInfo.addInstanceProperty("endpoint2Id", instance.get("endpoint2Id"));
            instanceInfo.addInstanceProperty("endpoint2Uri", instance.get("endpoint2Uri"));
            //adding role to type if 'relatedObjects'
            if (instanceType.equals("relatedObjects")) {
                instanceInfo.addInstanceProperty("type", String.format("%s.%s", instanceType, attributes.get("relationshipRole")) );
            }
        } else if(type.equalsIgnoreCase("entity")) {
            instanceInfo.addInstanceProperty("resourceId", instance.get("resourceId"));
        }

        instanceInfo.attributes = attributes;
        addDefinitionInfo(instanceInfo, definitionId, type);

        return instanceInfo;
    }
}
