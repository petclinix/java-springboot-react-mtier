# Java Spring Boot and React with multitier architecture

The classical tech stack with Java and Spring Boot for backend and single-page application with React for frontend as multitier architecture.
The implementation follows a feature based folder structure.

Project structure:
```
/
├── backend/               # Java Spring Boot backend   
└── frontend/              # React frontend
└── ingress/               # Gateway ingress for routing
```

![C4 System Context](docs/system_context.svg "C4 System Context Diagram")

For details about backend implementation, see [backend/README.md](backend/README.md).

# Usage
## Prerequisites
- Docker or Podman installed on your machine
- (Optional) Kubernetes cluster if you want to deploy using Kubernetes manifests

## Start using docker compose
To build and start the application using Docker Compose, navigate to the root directory of the project and run the following command:

```bash
docker compose up --build
```
Open your web browser and go to `http://localhost:8080` to access the React frontend.
