$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$runPath = Join-Path $projectRoot '.run'
foreach ($name in @('frontend', 'backend')) {
    $statePath = Join-Path $runPath "$name.json"
    if (-not (Test-Path -LiteralPath $statePath)) { continue }
    $state = Get-Content -Raw -LiteralPath $statePath | ConvertFrom-Json
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($state.processId)" -ErrorAction SilentlyContinue
    # Verify PID, creation time and project path before stopping our process.
    if ($process -and $process.CreationDate.ToUniversalTime().ToString('o') -eq $state.createdAt -and
        $process.CommandLine -and $process.CommandLine.IndexOf($projectRoot, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
        Stop-Process -Id $process.ProcessId -Force
        Write-Host "Stopped $name."
    }
    Remove-Item -LiteralPath $statePath
}
