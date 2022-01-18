# Deploy to a Viya Kubernetes Cluster
1. Create a new directory named `egeria-connector` in `<your kubernetes deployment root directory>/sas-bases/overlays` and then copy the following YAML files to the new directory.
   * deployment.yaml
   * kustomization.yaml
   * mount-pvc.yaml
   * pvc.yaml
   * tls-transformer.yaml

2. If your Viya deployment uses full-stack (default) or frontdoor TLS modes, you can skip this step.
If your deployment uses the truststores-only mode, comment out the following lines in kustomization.yaml:
```yaml
# transformers:
# - tls-transformer.yaml
```
   
3. Create a new file in the `egeria-connector` directory named `kustomization.yaml` with the contents:
```yaml
resources:
  - deployment.yaml
```

4. Navigate back to the Kubernetes install root directory, and add the following line to `kustomization.yaml` under the `resources` section
```yaml
  - sas-bases/overlays/egeria-connector
```

5. From the install directory, run the command `kustomize build > /tmp/deployment`

6. Ensure your KUBECONFIG is set correctly with admin privileges and then apply the new changes with `kubectl apply -f /tmp/deployment`

7. Run `configure.sh` to configure the connector to connect to your Catalog service.  The script may be run in interactive mode to prompt for the necessary values or the values may be specified on the command line.  NOTE: The configure.sh script need only be run once.  The connector will persist the configureation values and reuse them upon startup.
    * `configure.sh -i`
    * `configure.sh <SAS Viya cluster root url> <Egeria admin username> <Catalog username> <Catalog password>`
    * For example: `configure.sh https://myviyacluster.companyname.com garygeeke sasuser saspassword`

8. For reference, the repository connector starts a local Egeria server named, `SASRepositoryProxy`


