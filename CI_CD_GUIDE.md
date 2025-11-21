# CI/CD Pipeline Guide

## Overview

Gathr uses GitHub Actions for continuous integration and continuous deployment. This document explains how the pipeline works and how to set it up.

## Pipeline Architecture

### Workflow Stages

1. **Backend Tests** - Maven build and JUnit tests
2. **Frontend Tests** - Jest tests with coverage
3. **Code Quality** - Static analysis (optional)
4. **Migration Check** - Flyway migration validation
5. **Security Scan** - Trivy vulnerability scanning
6. **Docker Build** - Multi-stage Docker image build
7. **Deploy Staging** - Auto-deploy to staging (develop branch)
8. **Deploy Production** - Manual approval deploy (main branch)

### Trigger Conditions

- **On Push**: Runs full pipeline on `main` and `develop` branches
- **On Pull Request**: Runs tests and checks (no deployment)

## Setup Instructions

### 1. GitHub Repository Setup

Ensure your repository has the following structure:
```
gathr/
├── .github/
│   └── workflows/
│       └── ci-cd.yml       # Main workflow file
├── src/                    # Backend source
├── frontend/               # Frontend source
├── Dockerfile              # Backend Docker image
├── pom.xml                 # Maven configuration
└── package.json            # (if frontend)
```

### 2. GitHub Secrets Configuration

Navigate to **Settings > Secrets and variables > Actions** and add:

#### Required Secrets

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `GITHUB_TOKEN` | Automatically provided by GitHub | - |

#### Optional Secrets (for deployment)

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `DOCKER_USERNAME` | Docker Hub username (if not using GHCR) | `myuser` |
| `DOCKER_PASSWORD` | Docker Hub token | `dckr_pat_...` |
| `KUBE_CONFIG` | Base64-encoded kubeconfig for Kubernetes | `cat ~/.kube/config \| base64` |
| `AWS_ACCESS_KEY_ID` | AWS access key (if using ECS/ECR) | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` |
| `TWILIO_ACCOUNT_SID` | Twilio credentials for integration tests | `ACxxxx` |
| `TWILIO_AUTH_TOKEN` | Twilio auth token | `xxxxx` |
| `SLACK_WEBHOOK_URL` | Slack webhook for notifications | `https://hooks.slack.com/...` |

### 3. GitHub Environments

Create two environments for deployment protection:

#### Staging Environment

1. Go to **Settings > Environments**
2. Click **New environment**
3. Name: `staging`
4. Add environment URL: `https://staging-api.gathr.com`
5. No protection rules needed (auto-deploy)

#### Production Environment

1. Create environment: `production`
2. Add environment URL: `https://api.gathr.com`
3. Enable protection rules:
   - ✅ Required reviewers (add team members)
   - ✅ Wait timer: 5 minutes (optional)
   - ✅ Deployment branches: `main` only

### 4. Docker Registry Setup

The pipeline uses GitHub Container Registry (GHCR) by default.

Enable GHCR:
1. Go to **Settings > Actions > General**
2. Scroll to **Workflow permissions**
3. Select **Read and write permissions**
4. ✅ Enable **Allow GitHub Actions to create and approve pull requests**

Images will be published to: `ghcr.io/<username>/gathr`

## Running the Pipeline

### Automatic Triggers

```bash
# Push to main → Full pipeline + production deployment (with approval)
git push origin main

# Push to develop → Full pipeline + staging deployment
git push origin develop

# Create PR → Tests only (no deployment)
gh pr create --base main --head feature-branch
```

### Manual Triggers

You can manually run the workflow:

1. Go to **Actions** tab
2. Select **CI/CD Pipeline**
3. Click **Run workflow**
4. Select branch
5. Click **Run workflow**

## Pipeline Stages Explained

### 1. Backend Tests

Runs JUnit tests with PostgreSQL test database:

```yaml
services:
  postgres:
    image: postgres:15
    env:
      POSTGRES_DB: gathr_test
```

Commands:
- `mvn clean test -B` - Run tests
- `mvn clean package -DskipTests -B` - Build JAR

**Artifacts**: `backend-jar` (JAR file for Docker build)

### 2. Frontend Tests

Runs Jest tests with coverage:

```bash
npm ci                    # Install dependencies
npm run test:ci           # Run tests with coverage
```

**Coverage Threshold**: 70% (configured in `jest.config.js`)

**Artifacts**: `frontend-coverage` (HTML coverage report)

### 3. Migration Check

Validates Flyway migrations:

```bash
mvn flyway:migrate        # Apply migrations
mvn flyway:info           # Verify success
```

**Database**: Fresh PostgreSQL 15 instance

**Purpose**: Catch migration errors before production

### 4. Security Scan

Uses Trivy to scan for vulnerabilities:

```bash
trivy fs .                # Scan filesystem
trivy image <image>       # Scan Docker image
```

**Results**: Uploaded to GitHub Security tab

### 5. Docker Build

Multi-stage build with caching:

```dockerfile
# Stage 1: Maven build
FROM maven:3.9-eclipse-temurin-17 AS build

# Stage 2: Runtime with JRE only
FROM eclipse-temurin:17-jre-alpine
```

**Tags**:
- `main` branch → `latest`, `main-<sha>`
- `develop` branch → `develop`, `develop-<sha>`

### 6. Deploy Staging

Auto-deploys to staging when `develop` branch builds successfully.

**Placeholder** - Customize for your infrastructure:

```bash
# Kubernetes
kubectl set image deployment/gathr-backend \
  gathr-backend=ghcr.io/user/gathr:develop-abc123

# Helm
helm upgrade gathr ./charts/gathr \
  --set image.tag=develop-abc123

# AWS ECS
aws ecs update-service \
  --cluster gathr-staging \
  --service gathr-backend \
  --force-new-deployment

# Fly.io
fly deploy --config fly.staging.toml
```

### 7. Deploy Production

Requires manual approval via GitHub Environments.

**Approval Flow**:
1. Pipeline builds Docker image
2. Waits for reviewer approval
3. Deploys after approval
4. Notifications sent

## Customization

### Add New Test Jobs

```yaml
integration-tests:
  name: Integration Tests
  runs-on: ubuntu-latest
  steps:
    - name: Run integration tests
      run: mvn verify -P integration-tests
```

### Add Slack Notifications

```yaml
- name: Notify Slack on failure
  if: failure()
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    webhook_url: ${{ secrets.SLACK_WEBHOOK_URL }}
```

### Add SonarQube Analysis

```yaml
- name: SonarQube Scan
  run: |
    mvn sonar:sonar \
      -Dsonar.projectKey=gathr \
      -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }} \
      -Dsonar.login=${{ secrets.SONAR_TOKEN }}
```

## Deployment Targets

### Option 1: Kubernetes

**Setup**:
1. Create `k8s/` directory with manifests
2. Add `KUBE_CONFIG` secret
3. Update deploy step:

```yaml
- name: Deploy to Kubernetes
  run: |
    echo "${{ secrets.KUBE_CONFIG }}" | base64 -d > kubeconfig
    export KUBECONFIG=kubeconfig
    kubectl set image deployment/gathr-backend \
      gathr-backend=${{ env.DOCKER_REGISTRY }}/${{ env.DOCKER_IMAGE_NAME }}:${{ github.sha }}
    kubectl rollout status deployment/gathr-backend
```

### Option 2: AWS ECS

**Setup**:
1. Create ECS cluster and task definition
2. Add AWS credentials to secrets
3. Update deploy step:

```yaml
- name: Deploy to ECS
  uses: aws-actions/amazon-ecs-deploy-task-definition@v1
  with:
    task-definition: task-definition.json
    service: gathr-backend
    cluster: gathr-production
    wait-for-service-stability: true
```

### Option 3: Google Cloud Run

```yaml
- name: Deploy to Cloud Run
  uses: google-github-actions/deploy-cloudrun@v1
  with:
    service: gathr-backend
    image: ${{ env.DOCKER_REGISTRY }}/${{ env.DOCKER_IMAGE_NAME }}:${{ github.sha }}
    region: us-central1
```

### Option 4: Fly.io

```yaml
- name: Deploy to Fly.io
  uses: superfly/flyctl-actions/setup-flyctl@master
- run: flyctl deploy --remote-only
  env:
    FLY_API_TOKEN: ${{ secrets.FLY_API_TOKEN }}
```

### Option 5: Heroku

```yaml
- name: Deploy to Heroku
  uses: akhileshns/heroku-deploy@v3.12.12
  with:
    heroku_api_key: ${{ secrets.HEROKU_API_KEY }}
    heroku_app_name: gathr-production
    heroku_email: your-email@example.com
```

## Monitoring Pipeline Health

### View Pipeline Status

```bash
# Check latest workflow runs
gh run list --workflow=ci-cd.yml

# View specific run
gh run view <run-id>

# View logs
gh run view <run-id> --log
```

### Pipeline Badges

Add to README.md:

```markdown
![CI/CD](https://github.com/username/gathr/actions/workflows/ci-cd.yml/badge.svg)
```

### Metrics to Monitor

- **Build Duration**: Should be < 10 minutes
- **Test Pass Rate**: Should be 100%
- **Deployment Frequency**: Track via GitHub Insights
- **Mean Time to Recovery**: Track deployment rollbacks

## Troubleshooting

### Build Fails: "No tests found"

**Cause**: Test dependencies not installed

**Fix**: Ensure `pom.xml` has:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Docker Build Fails: "COPY failed"

**Cause**: Artifact not found

**Fix**: Ensure `needs: [backend-test]` and download artifact:
```yaml
- name: Download backend artifact
  uses: actions/download-artifact@v4
  with:
    name: backend-jar
    path: target
```

### Database Connection Error

**Cause**: PostgreSQL service not ready

**Fix**: Increase health check retries:
```yaml
options: >-
  --health-retries 10
```

### Permission Denied on Docker Push

**Cause**: GITHUB_TOKEN doesn't have package write permission

**Fix**: Enable write permissions in workflow:
```yaml
permissions:
  contents: read
  packages: write
```

### Deployment Fails: "Waiting for approval"

**Cause**: No reviewers configured for production environment

**Fix**: Add reviewers in **Settings > Environments > production**

## Best Practices

1. **Branch Protection**
   - Require PR reviews before merging to `main`
   - Require status checks to pass
   - Enforce linear history

2. **Testing**
   - Maintain > 70% code coverage
   - Run integration tests in CI
   - Use test databases, not mocks

3. **Security**
   - Scan for vulnerabilities weekly
   - Rotate secrets every 90 days
   - Never commit secrets to repo

4. **Deployments**
   - Always deploy to staging first
   - Require manual approval for production
   - Use blue-green or canary deployments

5. **Monitoring**
   - Set up alerts for failed pipelines
   - Track deployment metrics
   - Monitor application health post-deployment

## Cost Optimization

GitHub Actions is free for public repos, with limits for private repos:

- **Free tier**: 2,000 minutes/month for private repos
- **Optimization tips**:
  - Use caching for dependencies
  - Skip tests on documentation changes
  - Use matrix builds sparingly
  - Self-hosted runners for high-volume projects

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [Deployment Environments](https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment)
- [Trivy Security Scanner](https://github.com/aquasecurity/trivy)
