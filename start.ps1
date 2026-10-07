param([switch]$Build)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$frontendPath = Join-Path $projectRoot 'frontend'
$runPath = Join-Path $projectRoot '.run'
New-Item -ItemType Directory -Force -Path $runPath | Out-Null

$nodePath = (Get-Command node.exe -ErrorAction Stop).Source
$npmPath = (Get-Command npm.cmd -ErrorAction Stop).Source

function Get-ManagedProcess([string]$Name) {
    $statePath = Join-Path $runPath "$Name.json"
    if (-not (Test-Path -LiteralPath $statePath)) { return $null }
    $state = Get-Content -Raw -LiteralPath $statePath | ConvertFrom-Json
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($state.processId)" -ErrorAction SilentlyContinue
    if ($process -and $process.CreationDate.ToUniversalTime().ToString('o') -eq $state.createdAt -and
        $process.CommandLine -and $process.CommandLine.IndexOf($projectRoot, [StringComparison]::OrdinalIgnoreCase) -ge 0) { return $process }
    return $null
}

$managedFrontend = Get-ManagedProcess 'frontend'
foreach ($entry in @(@{ Port = 5173; Process = $managedFrontend })) {
    $listener = Get-NetTCPConnection -State Listen -LocalPort $entry.Port -ErrorAction SilentlyContinue
    if ($listener -and (-not $entry.Process -or @($listener.OwningProcess) -notcontains $entry.Process.ProcessId)) {
        throw "Port $($entry.Port) is already in use by another application. Please free that port first."
    }
}
if ($Build -and $managedFrontend) { throw 'Stop the blog with stop.ps1 before rebuilding.' }

if ($Build -or -not (Test-Path -LiteralPath (Join-Path $frontendPath 'node_modules'))) {
    Write-Host 'Installing frontend dependencies...'
    Push-Location $frontendPath
    try { & $npmPath ci --no-fund --no-audit; if ($LASTEXITCODE -ne 0) { throw 'Frontend install failed.' } }
    finally { Pop-Location }
}

function Save-Process([string]$Name, $Process) {
    $actual = Get-CimInstance Win32_Process -Filter "ProcessId = $($Process.Id)"
    @{ processId = $Process.Id; createdAt = $actual.CreationDate.ToUniversalTime().ToString('o') } |
        ConvertTo-Json | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $runPath "$Name.json")
}
if (-not $managedFrontend) {
    $vitePath = Join-Path $frontendPath 'node_modules\vite\bin\vite.js'
    $process = Start-Process -FilePath $nodePath -ArgumentList @("`"$vitePath`"", '--host', '127.0.0.1') -WorkingDirectory $frontendPath -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runPath 'frontend.out.log') -RedirectStandardError (Join-Path $runPath 'frontend.err.log')
    Save-Process 'frontend' $process
}

$deadline = (Get-Date).AddSeconds(45)
$ready = $false
while ((Get-Date) -lt $deadline) {
    try {
        $pageResponse = Invoke-WebRequest 'http://127.0.0.1:5173' -UseBasicParsing -TimeoutSec 2
        if ($pageResponse.StatusCode -eq 200) { $ready = $true; break }
    } catch { Start-Sleep -Milliseconds 600 }
}
if (-not $ready) { throw 'Services did not become ready. See the .run folder for logs; run stop.ps1 to stop them.' }
Write-Host ''
Write-Host 'Blog is ready: http://127.0.0.1:5173' -ForegroundColor Green
Write-Host 'Static read-only blog. No Java or backend service required.'
Write-Host 'Stop: powershell -ExecutionPolicy Bypass -File .\stop.ps1'
