# ---------- Builder ----------
FROM node:20-alpine AS builder

WORKDIR /app

# Add node_modules bin to PATH
ENV PATH /app/node_modules/.bin:$PATH
# Do not generate lots of telemetry logs
#ENV CI=true

# Install dependencies first (caches unless package*.json changes)
COPY package*.json ./
RUN npm ci --silent

# Copy source and build
COPY . .
# Support overrideable build script or env variables
ARG BUILD_COMMAND="npm run build"
RUN $BUILD_COMMAND

# ---------- Production ----------
FROM node:20-alpine as production

RUN npm install -g serve

# Copy built app from builder stage.
COPY --from=builder /app/build /srv/petclinix/html

# Expose HTTP port
EXPOSE 3000

# Simple healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- --spider http://127.0.0.1/ || exit 1

# Run serve to host the built app
CMD ["serve", "/srv/petclinix/html"]
