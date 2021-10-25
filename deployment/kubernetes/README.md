# Deploy to a Viya Kubernetes Cluster
1. Create a new directory named `egeria-connector` in `<your kubernetes deployment root directory>/sas-bases/overlays` and then copy the following files to this new directory:
    * deployment.yaml
    * kustomization.yaml
    * tls-transformer.yaml

2. If your Viya deployment uses full-stack (default) or frontdoor TLS modes, you can skip this step.
If your deployment uses the truststores-only mode, comment out the following lines in kustomization.yaml:
```yaml
# transformers:
# - tls-transformer.yaml
```

3. Navigate back to the Kubernetes install root directory, and add the following line to `kustomization.yaml` under the `resources` section
```yaml
  - sas-bases/overlays/egeria-connector
```

4. From the install directory, run the command `kustomize build > /tmp/deployment`

5. Ensure your KUBECONFIG is set correctly with admin privileges and then apply the new changes with `kubectl apply -f /tmp/deployment`

6. Set the appropriate variables in `configure.sh` from this repository and run it to configure the connector to connect to your Catalog service