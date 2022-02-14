# Ares - jeteo backend

## Features

### REST API 
- documentation in near future

### GraphQL
- self documented

## Deployment (VPS)

### Setup
1. Copy `docker-compose.yaml` to selected VPS (home directory)
2. Change variables inside `docker-compose.yaml`
3. Set proper secrets in GitHub environment (see `.github/workflows/deploy.yml` for reference)

### Deploy
1. Deployments are executed when any tag matching `v*` regex is pushed
2. Each deployment must be reviewed by specified reviewers (see GitHub)