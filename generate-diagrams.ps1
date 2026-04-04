# generate-diagrams.ps1
# Finds all .puml files in the repository and renders them to SVG.
# Uses PlantUML pipe mode: content is sent via stdin, SVG is read from stdout.
# The output filename is always derived from the .puml filename — PlantUML never controls it.
# Requires Docker — no local Java or PlantUML installation needed.

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker not found on PATH."
    exit 1
}

$pumlFiles = Get-ChildItem -Path $root -Recurse -Filter "*.puml"

if ($pumlFiles.Count -eq 0) {
    Write-Host "No .puml files found."
    exit 0
}

$ok = 0
$fail = 0

foreach ($file in $pumlFiles) {
    $relative = $file.FullName.Substring($root.Length + 1)
    $svgPath  = [System.IO.Path]::ChangeExtension($file.FullName, ".svg")

    try {
        # -pipe reads from stdin and writes SVG to stdout; -i keeps stdin open in Docker
        $svg = Get-Content -Raw $file.FullName |
               docker run --rm -i plantuml/plantuml -tsvg -pipe
        [System.IO.File]::WriteAllText($svgPath, ($svg -join "`n"))
        Write-Host "OK   $relative"
        $ok++
    } catch {
        Write-Host "FAIL $relative -- $_"
        $fail++
    }
}

Write-Host ""
Write-Host "$ok rendered, $fail failed."
if ($fail -gt 0) { exit 1 }
