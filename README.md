# SAS-Egeria Connector

## Overview
The purpose of this project is to create an Egeria connector for SAS Information Catalog.

Much of this code is based off of the Atlas-Egeria connector, which you can find [here](https://github.com/odpi/egeria-connector-hadoop-ecosystem/tree/master/apache-atlas-adapter).

## Starting Egeria Server with Connector
1. Run `gradlew clean build install` in this repository
2. Bring up the docker containers in `src/test/resources/compose` with the command `docker-compose -f egeria-tutorial.yaml up`.<br/>
If you'd prefer to only bring up the connector by itself, use `docker-compose -f connector-minimal.yaml up` instead.
Make sure you have allocated sufficient RAM to the Docker image.  The default of 2 GB will almost certain result in out of memory conditions (e.g. error 137 from compose_ui_1).  Try 6 GB.
3. Bring up the Jupyter notebook at http://localhost:18888 and run through all of the cells in egeria-server-config.ipynb after changing the Catalog connection settings for the connector<br><br>
    Alternatively, you can use the Postman collection in `src/test/resources` to configure and start the server if you aren't interested in starting the other servers and setting up cohorts. (See "Testing via Postman")

### Testing via UI
After starting up the server(s), you can access the Egeria UI at https://localhost:18443 with the username `garygeeke` and password `admin`.

Then, in Type or Repository explorer use the server name `test` , and the server URL root `http://connector:8080` to access the Connector via the UI.

### Testing via Postman
To have more control over your requests and to be able to see the unaltered response, use Postman to make your Egeria requests instead. Import the Egeria collection mentioned in step 3 above.

After importing this collection into Postman, you need to set three global or collection variables:
* `baseURL`: http://localhost:18084
* `server`: test
* `user`: garygeeke

Then, to configure the server, open the first request: "00.0 - Set Catalog Connector". You will need to modify the connection properties to point at the Catalog server you will be connecting to. Then send the request.

Once the server is configured, start it by sending the second request: "00.1 - Start Server".

Now that the server is up and running, you are free to make any of the other requests. Note that many of these are not yet implemented.

Also keep in mind that you will need to change the GUIDs in the requests to Catalog instance IDs for the Catalog server you have access to.

## Type mappings
Mappings can be found in `src/main/resources/TypeDefMappings.json`.

This file contains a list of mapping objects that tell the connector how to map SAS Catalog objects into Egeria objects.

The format for a mapping object is as follows.

For entities:
```json
{
  "sasCat": "<Catalog definition name>",
  "prefix": "<ID Prefix if one catalog type maps to multiple types>",
  "omrs": "<Egeria type name>",
  "propertyMappings": [
    {
      "sasCat": "[instance|definition|attribute|constant].<Catalog property name>",
      "omrs": "<Egeria property name>"
    }, 
    {
      "sasCat": "instance.name",
      "omrs": "qualifiedName"
    },
    {
      "sasCat": "instance.label",
      "omrs": "displayName"
    }
  ]
}
```

For relationships:
```json
{
  "sasCat": "<Catalog rel. definition name>",
  "omrs": "<Egeria rel. type name>",
  "endpointMappings": [
    {
      "sasCat": "[OPTIONAL] <Catalog entity def. name (must be mapped)>",
      "prefix": "[OPTIONAL] <Prefix to use for this entity (must match entity mapping prefixes)>",
      "omrs": "<Egeria name for this endpoint>"
    },
    {
      "sasCat": "[OPTIONAL] <Catalog entity def. name (must be mapped)>",
      "prefix": "[OPTIONAL] <Prefix to use for this entity (must match entity mapping prefixes)>",
      "omrs": "<Egeria name for this endpoint>"
    }
  ]
}
```

The only required field for endpoint mappings is "omrs". 

You can also specify "prefix" without specifying "sasCat". This will use the mapping for that entity type with the specified prefix, but will allow different entity types to be used.

For example, if one of the endpoints is of type "SchemaType", and you had entity mappings with prefix "RTT" for both RelationalTableType and RelationalColumnType (which both extend SchemaType), you could specify the prefix "RTT" in the relationship mapping and be able to map a Catalog table and Catalog column, assuming these map to RelationalTableType and RelationalColumnType respectively, to that endpoint.

Catalog definition names and instance attributes can be found in `archive/definition_archive.go` in the Catalog repository.

For the Catalog property name, you can use one of four prefixes:
* instance: This is a field on the instance object, such as "name", "description", "createdBy". You can see these by sending a GET instance request to Catalog.
* definition: This is a field on the instance object, such as "name", "description", "createdBy", **"type"**. You can see these by sending a GET definition request to Catalog.
* attribute: This allows you to specify the name of an attribute on the instance. You can see the available attributes in the `definition_archive.go` file as mentioned above.
* constant: This allows you to specify a constant string to use instead of doing any sort of property mapping. e.g. "constant.This is a table!"

To send requests to Catalog, you can use the Catalog Postman collection in `src/test/resources`. You will need to set the collection variable `ip` to the IP of the Catalog server you are using.

Egeria type and property names can most easily be found by using Types Explorer in the Egeria UI.<br/>
To do this, go to the UI and login as before. Go to Types Explorer, and use `cocoMDS1` as the server name and `http://datalake:8080` as the server URL root.<br/>
From there, you will see a list of the Egeria types and the attributes on them.

## Debugging
After starting the server with the steps above, you can attach a debugger remotely via localhost:17084.
###IntelliJ
- Create a new `Remote` run configuration, and set the port to 17084.
###VSCode
- Follow steps for "Attach" [here](https://code.visualstudio.com/docs/java/java-debugging) with `hostName`="localhost" and `port`=17084

## Contributing
We welcome your contributions! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on how to submit contributions to this project. 

## License
This project is licensed under the [Apache 2.0 License](LICENSE).
