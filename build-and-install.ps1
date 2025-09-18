# Builds and installs the requested Android variant on a connected device.
Param(
    [string]$Variant = "Debug",
    [switch]$Clean,
    [switch]$SkipDeviceCheck
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = if ($scriptRoot) { $scriptRoot } else { Get-Location }
$gradleWrapper = Join-Path $repoRoot "gradlew.bat"

if (-not (Test-Path -Path $gradleWrapper)) {
    Write-Error "Gradle wrapper not found at $gradleWrapper"
    exit 1
}

if (-not $Variant) {
    Write-Error "Variant must not be empty"
    exit 1
}

$normalizedVariant = $Variant.Trim()
if ($normalizedVariant.Length -eq 0) {
    Write-Error "Variant must not be empty"
    exit 1
}
$normalizedVariant = $normalizedVariant.Substring(0,1).ToUpper() + $normalizedVariant.Substring(1)

if (-not $SkipDeviceCheck) {
    $adbExecutable = "adb"
    $adbAvailable = $true
    try {
        $null = & $adbExecutable "start-server" 2>$null
        $devicesOutput = & $adbExecutable "devices"
    } catch {
        $adbAvailable = $false
    }

    if ($adbAvailable) {
        $connectedDevices = $devicesOutput -split "`n" | Where-Object { $_ -match "`tdevice`r?$" }
        if ($connectedDevices.Count -eq 0) {
            Write-Error "No Android devices or emulators detected. Connect one or rerun with -SkipDeviceCheck."
            exit 1
        }
        Write-Host "Detected device(s):"
        foreach ($device in $connectedDevices) {
            Write-Host "  $device"
        }
    } else {
        Write-Warning "adb not found in PATH. Proceeding without device check."
    }
}

$tasks = @()
if ($Clean) {
    $tasks += "clean"
}
$tasks += "install$normalizedVariant"

Write-Host "Running $($tasks -join ' ') via gradle wrapper..."
$process = Start-Process -FilePath $gradleWrapper -ArgumentList $tasks -WorkingDirectory $repoRoot -NoNewWindow -Wait -PassThru

if ($process.ExitCode -ne 0) {
    Write-Error "Gradle exited with code $($process.ExitCode)"
}

exit $process.ExitCode
