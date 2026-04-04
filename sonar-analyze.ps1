# sonar-analyze.ps1

# ============================================================
# Load .env file
# ============================================================
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Write-Host "Loading .env file..." -ForegroundColor Cyan
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]*?)\s*=\s*(.*)\s*$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim().Trim('"').Trim("'")
            [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
            Write-Host "  Set: $key" -ForegroundColor DarkGray
        }
    }
} else {
    Write-Error ".env file not found at $envFile"
    exit 1
}

# ============================================================
# Config (read from env)
# ============================================================
$sonarToken     = $env:SONAR_TOKEN
$sonarHostUrl   = if ($env:SONAR_HOST_URL) { $env:SONAR_HOST_URL } else { "http://host.containers.internal:9000" }
$sonarStartup   = $env:SONAR_STARTUP
$backendDir     = "backend"
$frontendDir    = "frontend"

if (-not $sonarToken) {
    Write-Error "SONAR_TOKEN is not set in .env"
    exit 1
}

$repoRoot = $PSScriptRoot
$failed   = @()

# ============================================================
# Helper
# ============================================================
function Write-Step($msg) {
    Write-Host ""
    Write-Host "===> $msg" -ForegroundColor Cyan
}

function Assert-Success($stepName) {
    if ($LASTEXITCODE -ne 0) {
        Write-Host "FAILED: $stepName (exit code $LASTEXITCODE)" -ForegroundColor Red
        $script:failed += $stepName
    } else {
        Write-Host "OK: $stepName" -ForegroundColor Green
    }
}

# ============================================================
# 1. Start SonarQube with Podman (if SONAR_STARTUP=1)
# ============================================================
if ($sonarStartup -eq "1") {
    Write-Step "Starting SonarQube with Podman..."

    $existing = podman ps --filter "name=sonarqube" --format "{{.Names}}"
    if ($existing -match "sonarqube") {
        Write-Host "SonarQube container already running, skipping start." -ForegroundColor Yellow
    } else {
        podman run -d `
            --name sonarqube `
            -p 9000:9000 `
            -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true `
            -v sonarqube_data:/opt/sonarqube/data `
            -v sonarqube_logs:/opt/sonarqube/logs `
            -v sonarqube_extensions:/opt/sonarqube/extensions `
            docker.io/library/sonarqube:community

        Assert-Success "SonarQube start"

        Write-Host "Waiting for SonarQube to become ready..." -ForegroundColor Yellow
        $maxWait  = 120
        $waited   = 0
        $interval = 5
        $ready    = $false

        while ($waited -lt $maxWait) {
            Start-Sleep -Seconds $interval
            $waited += $interval
            try {
                $resp = Invoke-WebRequest -Uri "http://localhost:9000/api/system/status" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
                $status = ($resp.Content | ConvertFrom-Json).status
                if ($status -eq "UP") {
                    $ready = $true
                    break
                }
                Write-Host "  Status: $status (${waited}s elapsed)" -ForegroundColor DarkGray
            } catch {
                Write-Host "  Not yet reachable... (${waited}s elapsed)" -ForegroundColor DarkGray
            }
        }

        if ($ready) {
            Write-Host "SonarQube is UP!" -ForegroundColor Green
        } else {
            Write-Host "SonarQube did not become ready within ${maxWait}s - continuing anyway." -ForegroundColor Yellow
        }
    }
}

# ============================================================
# 2. Backend — Maven tests
# ============================================================
Write-Step "Running backend Maven tests..."

Push-Location (Join-Path $repoRoot $backendDir)
mvn clean verify
Assert-Success "Backend Maven tests"
Pop-Location

# ============================================================
# 3. Frontend — npm tests
# ============================================================
Write-Step "Running frontend npm tests..."

Push-Location (Join-Path $repoRoot $frontendDir)
npm install
Assert-Success "Frontend npm install"

npm test -- --coverage
Assert-Success "Frontend npm tests"
Pop-Location

# ============================================================
# 4. SonarQube analysis with Podman
# ============================================================
Write-Step "Running Sonar analysis..."

podman run --rm `
    -e SONAR_HOST_URL=$sonarHostUrl `
    -e SONAR_TOKEN=$sonarToken `
    -v "${repoRoot}:/usr/src" `
    docker.io/sonarsource/sonar-scanner-cli:latest

Assert-Success "Sonar analysis"

# ============================================================
# Summary
# ============================================================
Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
if ($failed.Count -eq 0) {
    Write-Host "All steps completed successfully!" -ForegroundColor Green
} else {
    Write-Host "Completed with failures:" -ForegroundColor Red
    $failed | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 1
}
