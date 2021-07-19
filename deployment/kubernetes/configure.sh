# The IP/Hostname for the Kubernetes cluster
CLUSTER_IP=""

# The user to use for Egeria
EGERIA_USER=""

# The name of the Egeria server you're starting
EGERIA_SERVER=""

# The IP/Hostname to connect to for Catalog
CATALOG_IP=""

# Catalog Username/pw credentials
CATALOG_USER=""
CATALOG_PASS=""

set -e

# Configure Catalog connection
curl --location --request POST -k "https://${CLUSTER_IP}:30000/open-metadata/admin-services/users/${EGERIA_USER}/servers/${EGERIA_SERVER}/local-repository/mode/repository-proxy/connection" \
--header 'Content-Type: application/json' \
--data-raw "{
  \"class\": \"Connection\",
  \"connectorType\": {
    \"class\": \"ConnectorType\",
    \"connectorProviderClassName\": \"org.odpi.openmetadata.connector.sas.repository.connector.RepositoryConnectorProvider\"
  },
  \"endpoint\": {
    \"class\": \"Endpoint\",
    \"address\": \"${CATALOG_IP}\",
    \"protocol\": \"https\"
  },
  \"securedProperties\": {
    \"userId\": \"${CATALOG_USER}\",
    \"password\": \"${CATALOG_PASS}\"
  }
}"

# Configure RabbitMQ connection
curl --location --request POST -k "https://${CLUSTER_IP}:30000/open-metadata/admin-services/users/${EGERIA_USER}/servers/${EGERIA_SERVER}/event-bus?connectorProvider=org.odpi.openmetadata.adapters.eventbus.topic.kafka.KafkaOpenMetadataTopicProvider&topicURLRoot=OMRSTopic" \
--header "Content-Type: application/json" \
--data-raw "{
  \"producer\": {
    \"bootstrap.servers\":\"kafkahost:9092\"
  },
  \"consumer\": {
    \"bootstrap.servers\":\"kafkahost:9092\"
  }
}"
curl --location --request POST -k "https://${CLUSTER_IP}:30000/open-metadata/admin-services/users/${EGERIA_USER}/servers/${EGERIA_SERVER}/local-repository/event-mapper-details?connectorProvider=org.odpi.openmetadata.connector.sas.event.mapper.RepositoryEventMapperProvider&eventSource=sas-rabbitmq-server:5672" \
--header "Content-Type: application/json" \
--data-raw "{\"username\":\"$(kubectl get secret sas-rabbitmq-server-secret -o go-template='{{(index .data.RABBITMQ_DEFAULT_USER)}}' | base64 -d)\",
\"password\":\"$(kubectl get secret sas-rabbitmq-server-secret -o go-template='{{(index .data.RABBITMQ_DEFAULT_PASS)}}' | base64 -d)\"}"

# Start Egeria Server
curl --location --request POST -k "https://${CLUSTER_IP}:30000/open-metadata/admin-services/users/${EGERIA_USER}/servers/${EGERIA_SERVER}/instance" \
--header "Content-Type: application/json"