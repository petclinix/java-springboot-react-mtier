# Getting Started with Petclinix SpringBoot App

Directory layout and layers
```text
tech.petclinix
├─ Application.java
├─ web
│  ├─ controller
│  └─ dto
├─ logic
│  └─ service          // business services / use-cases
├─ persistence
│  ├─ entity
│  ├─ jpa              // Spring Data repos
│  └─ mapper
├─ security            // <<-- security lives here
│  ├─ config           // SecurityConfig, CorsConfig etc.
│  ├─ jwt              // JwtUtil, JwtFilter
│  ├─ provider         // AuthenticationProvider / UserDetailsAdapter
└─ exception
```

## Build Docker Image

To build a Docker image for the React app, you can use the following command:

```bash
docker build --target production -t petclinix/spring-backend .
```
