$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
function Get-DependencyHash([string]$filePath) {
    $stream = [IO.File]::OpenRead($filePath)
    $algorithm = [Security.Cryptography.SHA256]::Create()
    try { return [BitConverter]::ToString($algorithm.ComputeHash($stream)).Replace('-', '').ToLowerInvariant() }
    finally { $stream.Dispose(); $algorithm.Dispose() }
}
$dependencies = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'dependencies.json') -Raw | ConvertFrom-Json
$libraryDirectory = Join-Path $PSScriptRoot 'lib'
New-Item -ItemType Directory -Force -Path $libraryDirectory | Out-Null
foreach ($dependency in $dependencies) {
    $destination = Join-Path $libraryDirectory $dependency.name
    if ((Test-Path -LiteralPath $destination) -and (Get-DependencyHash $destination) -eq $dependency.sha256) { continue }
    Write-Output ('Downloading ' + $dependency.name)
    Invoke-WebRequest -Uri $dependency.url -OutFile $destination -UseBasicParsing
    if ((Get-DependencyHash $destination) -ne $dependency.sha256) {
        throw ('Dependency checksum mismatch: ' + $dependency.name)
    }
}
Write-Output 'Dependencies verified.'
