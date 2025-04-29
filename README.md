# springboot-kubernetes-postgresql-storage #

This repository implements a simple Spring Boot application that demonstrates how to use Kubernetes for file storage.

The Spring Boot application stores entities to PostgreSQL database.

## Application Structure ##

The Spring Boot application implements a single API that can be used for CRUD operations on the `Duck` entities.

The Spring Boot application has layered implementation that consist of controller, service and repository classes.

The repository class needs a PostgreSQL database `pikecape` to store the `Duck` entities.

## Dockerfile ##

Following `Dockerfile` is used to build an image for the Spring Boot application

```dockerfile
FROM openjdk:21-jdk
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Image Creation with Minikube ###

If the used Kubernetes instance is minikube and it is running in Windows, then the Docker image must be created directly to the image repository in minikube, because the image repositories used by the local Docker daemon and minikube are not the same.

`minikube image build -t ducks-json:<version> .`

## Kubernetes Configurations for Database ##

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-configmap
  labels:
    app: postgres
data:
  POSTGRES_DB: "pikecape"
  POSTGRES_HOST: "postgres"
---
apiVersion: v1
kind: Secret
metadata:
  name: postgres-secret
  labels:
    app: postgres
type: Opaque
data:
  POSTGRES_USER: cG9zdGdyZXM= # postgres
  POSTGRES_PASSWORD: cG9zdGdyZXM= # postgres
---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: postgres-volume
  labels:
    type: local
    app: postgres
spec:
  storageClassName: manual
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce
  hostPath:
    path: /data/postgresql
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-volume-claim
  labels:
    app: postgres
spec:
  storageClassName: manual
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
        - name: postgres
          image: postgres:14
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 5432
          env:
            - name: POSTGRES_DB
              valueFrom:
                configMapKeyRef:
                  name: postgres-configmap
                  key: POSTGRES_DB
            - name: POSTGRES_USER
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: POSTGRES_USER
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: POSTGRES_PASSWORD
          volumeMounts:
            - mountPath: /var/lib/postgresql/data
              name: postgresdata
      volumes:
        - name: postgresdata
          persistentVolumeClaim:
            claimName: postgres-volume-claim
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  labels:
    app: postgres
spec:
  type: NodePort
  ports:
    - port: 5432
  selector:
    app: postgres
```

## Kubernetes Configurations for Spring Boot Application ##

The Kubernetes configuration for Spring Boot application requires deployment, service and ingress configurations.

The `application.yml` file contains database connectivity information that is fulfilled with environment variables, and the values are coming from `postgres-secret` and `postgres-configmap` objects; defined in Kubernetes configuration for database.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost:5432}/${POSTGRES_DB:pikecape}
    username: ${POSTGRES_USER:postgres}
    password: ${POSTGRES_PASSWORD:postgres}
```

The configurations are included into a single file `ducks-psql-application.yaml`, which is deployed with following command:

`kubectl create -f ducks-psql-application.yaml`

### Deployment ###

Precondition for the deployment is that the configmap is in place. The config map is a JSON file that contains an empty array.

```json
[]
```

The configmap is crated to Kubernetes with following command:

`kubectl create configmap ducks-empty-json --from-file=ducks.json`

The deployment configuration contains a container definition (`ducks-psql-server`) and a references to `postgres-secret` and `postgres-configmap` for environment variable values.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name:  ducks-psql
  labels:
    name:  ducks-psql
spec:
  replicas: 1
  selector:
    matchLabels:
      server: ducks-psql
  strategy:
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 1
    type: RollingUpdate
  template:
    metadata:
      labels:
        server: ducks-psql
    spec:
      containers:
        - name: ducks-server-psql
          image: ducks-psql:4
          imagePullPolicy: Never
          resources:
            limits:
              memory: "500M"
              cpu: "500m"
          ports:
            - containerPort: 8080
          env:
            - name: POSTGRES_USER
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: POSTGRES_USER
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: POSTGRES_PASSWORD
            - name: POSTGRES_DB
              valueFrom:
                configMapKeyRef:
                  name: postgres-configmap
                  key: POSTGRES_DB
            - name: POSTGRES_HOST
              valueFrom:
                configMapKeyRef:
                  name: postgres-configmap
                  key: POSTGRES_HOST
```

### Service ###

The service configuration has a reference to container (`ducks-psql` → `ducks-psql-server`) and it exposes port 8180 to outside world.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: ducks-psql-service
spec:
  selector:
    server: ducks-psql
  type: ClusterIP
  ports:
  - port: 8180
    targetPort: 8080
```

### Ingress ###

The ingress configuration has a reference to the service (`ducks-psql-service`) and it exposes port 8180 to outside world.

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ducks-psql-ingress
  labels:
    name: ducks-psql-ingress
spec:
  rules:
  - host: ducks-psql.local
    http:
      paths:
      - pathType: Prefix
        path: "/"
        backend:
          service:
            name: ducks-psql-service
            port:
              number: 8180
```

