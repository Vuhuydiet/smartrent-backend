# SmartRent Kubernetes Deployment - Setup Summary

## 📦 What Was Created

### 1. Backend Dockerfile
**Location**: `smart-rent/Dockerfile`

A production-ready multi-stage Dockerfile featuring:
- **Build stage**: Gradle 8.10 + JDK 17
- **Runtime stage**: Alpine-based JRE 17 (lightweight)
- **Security**: Non-root user execution
- **Optimization**: Layer caching, optimized JVM settings
- **Health checks**: Spring Boot Actuator integration

### 2. Kubernetes Helm Chart
**Location**: `smartrent-deploy/charts/smartrent/`

Complete Helm chart with:
- ✅ Backend deployment with environment variables, secrets, health checks
- ✅ MySQL StatefulSet with persistent storage
- ✅ Redis deployment
- ✅ Ingress configuration (NGINX)
- ✅ Secrets management template
- ✅ Service definitions for all components

### 3. Environment Configurations

#### Development (`environments/dev/values.yaml`)
- Single replica
- Debug logging enabled
- Lower resource limits
- Uses `latest` tag for continuous deployment

#### Production (`environments/prd/values.yaml`)
- 2 replicas for high availability
- Production logging (WARN/INFO)
- Higher resource limits (2Gi RAM, 2 CPU)
- Redis password enabled
- Uses `stable` tag for controlled releases

### 4. ArgoCD Applications

#### Development (`apps/dev-application.yaml`)
- Auto-sync enabled
- Self-healing enabled
- Deploys to `dev` namespace
- Source: `charts/smartrent` with dev values

#### Production (`apps/prd-application.yaml`)
- Auto-sync enabled
- Self-healing enabled
- Deploys to `production` namespace
- Source: `charts/smartrent` with prd values

### 5. Documentation
- **DEPLOYMENT_GUIDE.md**: Comprehensive 300+ line guide
- **README.md**: Quick reference
- **.dockerignore**: Optimized Docker builds

### 6. Helper Scripts
- **build-and-push.ps1**: PowerShell script for building and pushing Docker images

---

## 🚀 Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        NGINX Ingress                         │
│  dev.smartrent-api.vuhuydiet.xyz                            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Backend Service                          │
│                    (ClusterIP:8080)                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Backend Deployment                         │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │  Backend Pod │  │  Backend Pod │  (2 replicas in prd)   │
│  │   Port 8080  │  │   Port 8080  │                        │
│  └──────────────┘  └──────────────┘                        │
└─────────────────────────────────────────────────────────────┘
          │                          │
          ├──────────────────────────┼──────────────────┐
          ▼                          ▼                  ▼
┌──────────────────┐   ┌──────────────────┐   ┌─────────────┐
│  MySQL Service   │   │  Redis Service   │   │   Secrets   │
│  (Port 3306)     │   │  (Port 6379)     │   │             │
├──────────────────┤   ├──────────────────┤   ├─────────────┤
│ StatefulSet      │   │  Deployment      │   │ - DB creds  │
│ Persistent Vol.  │   │  EmptyDir        │   │ - JWT       │
│ 10Gi Storage     │   │                  │   │ - AWS keys  │
└──────────────────┘   └──────────────────┘   └─────────────┘
```

---

## 🔧 Key Features Implemented

### Backend Deployment
✅ **Environment Variables**:
- Spring profiles (dev/production)
- Database connection (MySQL)
- Redis configuration
- JWT settings
- AWS S3/Cloudflare R2 configuration
- Twilio SMS integration
- Zalo ZNS integration

✅ **Secrets Management**:
- Database credentials
- JWT secret
- AWS access keys
- SMS provider credentials
- All sensitive data externalized

✅ **Health Checks**:
- Liveness probe: `/actuator/health/liveness`
- Readiness probe: `/actuator/health/readiness`
- Proper startup delays and retry configuration

✅ **Resource Management**:
- Memory requests/limits
- CPU requests/limits
- Production-grade resource allocation

### Database (MySQL)
✅ StatefulSet for stable identity
✅ Persistent volume (10Gi)
✅ Health checks
✅ Password management via secrets
✅ Automatic database creation

### Cache (Redis)
✅ Deployment configuration
✅ Optional password protection (enabled in production)
✅ Health checks
✅ EmptyDir storage for development

### Ingress
✅ NGINX Ingress Controller
✅ Path-based routing
✅ Multiple subdomains:
- Main API: `dev.smartrent-api.vuhuydiet.xyz`
- Scraper: `scraper.dev.smartrent-api.vuhuydiet.xyz`
- AI: `ai.dev.smartrent-api.vuhuydiet.xyz`

---

## 🎯 Quick Start Commands

### 1. Build & Push Docker Image

```powershell
cd d:\Personal\Dev\smartrent-backend\smart-rent

# Build and push with version
.\build-and-push.ps1 -Version "v1.0.0" -Latest

# Or manually
docker build -t vuhuydiet/smartrent-backend:latest .
docker push vuhuydiet/smartrent-backend:latest
```

### 2. Create Kubernetes Secrets

```bash
# Development
kubectl create namespace dev

kubectl create secret generic backend-server-secrets \
  --from-literal=db-username=smartrent_user \
  --from-literal=db-password=YOUR_PASSWORD \
  --from-literal=redis-password= \
  --from-literal=jwt-secret=YOUR_STRONG_JWT_SECRET \
  --from-literal=aws-access-key-id=YOUR_AWS_KEY \
  --from-literal=aws-secret-access-key=YOUR_AWS_SECRET \
  -n dev

kubectl create secret generic mysql-secrets \
  --from-literal=root-password=YOUR_MYSQL_ROOT_PASSWORD \
  --from-literal=username=smartrent_user \
  --from-literal=password=YOUR_PASSWORD \
  -n dev
```

### 3. Deploy with ArgoCD

```bash
cd d:\Personal\Dev\smartrent-deploy

# Deploy development
kubectl apply -f apps/dev-application.yaml

# Deploy production (when ready)
kubectl apply -f apps/prd-application.yaml
```

### 4. Verify Deployment

```bash
# Check all resources
kubectl get all -n dev

# Check pods
kubectl get pods -n dev

# Check logs
kubectl logs -f deployment/backend-server -n dev

# Test backend health
kubectl exec -it deployment/backend-server -n dev -- \
  wget -O- http://localhost:8080/actuator/health
```

### 5. Update Image Version

```bash
# 1. Build new image
cd d:\Personal\Dev\smartrent-backend\smart-rent
.\build-and-push.ps1 -Version "v1.0.1" -Latest

# 2. Update deployment repo
cd d:\Personal\Dev\smartrent-deploy
# Edit: charts/smartrent/environments/dev/values.yaml
# Change: BEServer.image.tag to "v1.0.1"

# 3. Commit and push
git add .
git commit -m "Update backend to v1.0.1"
git push

# ArgoCD will auto-sync and deploy!
```

---

## 🔐 Security Checklist

- [ ] Create strong passwords for all secrets
- [ ] Enable Redis password in production
- [ ] Use SSL for MySQL in production
- [ ] Rotate JWT secret regularly
- [ ] Use External Secrets Operator for production
- [ ] Never commit secrets to git
- [ ] Enable network policies
- [ ] Scan Docker images for vulnerabilities
- [ ] Use RBAC for service accounts
- [ ] Enable pod security policies

---

## 📊 Monitoring & Observability

### Spring Boot Actuator Endpoints
- Health: `http://backend-server:8080/actuator/health`
- Metrics: `http://backend-server:8080/actuator/prometheus`
- Info: `http://backend-server:8080/actuator/info`

### Recommended Tools
- **Prometheus**: Metrics collection
- **Grafana**: Visualization
- **Loki**: Log aggregation
- **Jaeger**: Distributed tracing

---

## 🐛 Troubleshooting

### Backend won't start
```bash
# Check logs
kubectl logs -f deployment/backend-server -n dev

# Check events
kubectl describe pod <pod-name> -n dev

# Common issues:
# 1. Database connection failed → Check MySQL is running
# 2. Secret not found → Create secrets first
# 3. Image pull failed → Check Docker registry credentials
```

### Database connection issues
```bash
# Test from backend pod
kubectl exec -it deployment/backend-server -n dev -- sh
nc -zv mysql 3306
```

### Check resource usage
```bash
kubectl top pods -n dev
kubectl top nodes
```

---

## 📝 Next Steps

1. **Configure External Secrets**
   - Set up External Secrets Operator
   - Integrate with AWS Secrets Manager or HashiCorp Vault

2. **Set up CI/CD Pipeline**
   - GitHub Actions workflow
   - Automated testing
   - Automatic image building and deployment

3. **Configure Monitoring**
   - Deploy Prometheus
   - Set up Grafana dashboards
   - Configure alerts

4. **Production Hardening**
   - Enable network policies
   - Set up pod security policies
   - Configure resource quotas
   - Set up backup strategy for MySQL

5. **Performance Testing**
   - Load testing
   - Stress testing
   - Optimize resource limits

---

## 📚 Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [ArgoCD Documentation](https://argo-cd.readthedocs.io/)
- [Helm Documentation](https://helm.sh/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

**Created**: November 16, 2025
**Stack**: Spring Boot 3.5, Java 17, MySQL 8.0, Redis 8.2, Kubernetes, ArgoCD
