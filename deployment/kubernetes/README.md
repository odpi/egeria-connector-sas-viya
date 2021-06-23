# Deploy to a Viya Kubernetes Cluster
1. Create a new directory named `egeria-connector` in `<your kubernetes deployment root directory>/sas-bases/overlays` and then copy deployment.yaml to the new directory.
   
2. Create a new file in the `egeria-connector` directory named `kustomization.yaml` with the contents:
```yaml
resources:
  - deployment.yaml
```

3. Navigate back to the Kubernetes install root directory, and add the following line to `kustomization.yaml` under the `resources` section
```yaml
  - sas-bases/overlays/egeria-connector
```

4. From the install directory, run the command `kustomize build > /tmp/deployment`

5. Ensure your KUBECONFIG is set correctly with admin privileges and then apply the new changes with `kubectl apply -f /tmp/deployment`

6. Set the appropriate variables in `configure.sh` from this repository and run it to configure the connector to connect to your Catalog service