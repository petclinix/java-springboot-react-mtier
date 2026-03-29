Write-Host "=== Backend ===" -ForegroundColor Cyan
Push-Location backend
mvn test
$backendOk = $LASTEXITCODE -eq 0
Pop-Location

Write-Host ""
Write-Host "=== Frontend ===" -ForegroundColor Cyan
Push-Location frontend
npm test
$frontendOk = $LASTEXITCODE -eq 0
Pop-Location

Write-Host ""
if ($backendOk -and $frontendOk) {
    Write-Host "All tests passed." -ForegroundColor Green
} else {
    Write-Host "Some tests failed." -ForegroundColor Red
    exit 1
}
