param(
    [switch]$StopOnly
)

$ErrorActionPreference = 'Stop'
$host.UI.RawUI.WindowTitle = 'MapleStory 079 Launcher Guard'

Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

public static class MapleStoryGuardNative {
    [StructLayout(LayoutKind.Sequential)]
    public struct POINT {
        public int X;
        public int Y;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct MSG {
        public IntPtr HWnd;
        public uint Message;
        public IntPtr WParam;
        public IntPtr LParam;
        public uint Time;
        public POINT Point;
    }

    [DllImport("user32.dll")]
    public static extern bool RegisterHotKey(IntPtr hWnd, int id, uint modifiers, uint virtualKey);

    [DllImport("user32.dll")]
    public static extern bool UnregisterHotKey(IntPtr hWnd, int id);

    [DllImport("user32.dll")]
    public static extern bool PeekMessage(out MSG message, IntPtr hWnd, uint min, uint max, uint remove);

    [DllImport("user32.dll")]
    public static extern int ChangeDisplaySettings(IntPtr devMode, uint flags);
}
'@

function Reset-DisplayMode {
    [MapleStoryGuardNative]::ChangeDisplaySettings([IntPtr]::Zero, 0) | Out-Null
}

function Stop-MapleStory {
    param([System.Diagnostics.Process]$TrackedProcess)

    Reset-DisplayMode

    if ($null -ne $TrackedProcess) {
        try {
            if (-not $TrackedProcess.HasExited) {
                $TrackedProcess.Kill()
                $TrackedProcess.WaitForExit(5000) | Out-Null
            }
        } catch {
        }
    }

    Get-Process -Name 'MapleStory', 'MapleStory079' -ErrorAction SilentlyContinue |
        Stop-Process -Force -ErrorAction SilentlyContinue

    Start-Sleep -Milliseconds 500
    $remaining = Get-Process -Name 'MapleStory', 'MapleStory079' -ErrorAction SilentlyContinue
    foreach ($item in $remaining) {
        & "$env:WINDIR\System32\taskkill.exe" /PID $item.Id /T /F | Out-Null
    }

    Reset-DisplayMode
}

function Test-LocalPort {
    param([int]$Port)

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $async = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne(750)) {
            return $false
        }
        $client.EndConnect($async)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

if ($StopOnly) {
    Stop-MapleStory
    Write-Host 'The client was stopped and the display mode was restored.'
    exit 0
}

$clientRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$clientExe = Join-Path $clientRoot 'MapleStory079.exe'
if (-not (Test-Path -LiteralPath $clientExe)) {
    throw "Client not found: $clientExe"
}

# The replacement HShield build stores the path it expects to launch.  Use
# that exact spelling (including a junction path) because this old client can
# stall before opening a login connection when the process path differs.
$launchExe = $clientExe
$hshieldConfig = Join-Path $clientRoot 'HShield\ehsvc.ini'
if (Test-Path -LiteralPath $hshieldConfig) {
    $configuredPath = Get-Content -LiteralPath $hshieldConfig |
        Where-Object { $_ -match '^\s*GamePath\s*=' } |
        Select-Object -First 1
    if ($null -ne $configuredPath) {
        $configuredPath = ($configuredPath -split '=', 2)[1].Trim()
        if (Test-Path -LiteralPath $configuredPath) {
            $launchExe = $configuredPath
        }
    }
}
$launchRoot = Split-Path -Parent $launchExe

# A visible, zero-byte downloadinfo.dat makes this client spin forever on
# ReadFile(END_OF_FILE) before it connects to the login server.  The original
# client workaround is to keep this updater marker hidden.
$downloadInfo = Join-Path $clientRoot 'downloadinfo.dat'
if (Test-Path -LiteralPath $downloadInfo) {
    $downloadInfoItem = Get-Item -LiteralPath $downloadInfo -Force
    if (($downloadInfoItem.Attributes -band [System.IO.FileAttributes]::Hidden) -eq 0) {
        $downloadInfoItem.Attributes = $downloadInfoItem.Attributes -bor [System.IO.FileAttributes]::Hidden
    }
}

Stop-MapleStory

$requiredPorts = @(9595, 7576, 7577, 7578, 8600)
$deadline = (Get-Date).AddMinutes(4)
do {
    $missingPorts = @($requiredPorts | Where-Object { -not (Test-LocalPort -Port $_) })
    if ($missingPorts.Count -eq 0) {
        break
    }
    Write-Host "Waiting for the local server. Closed ports: $($missingPorts -join ', ')"
    Start-Sleep -Seconds 3
} while ((Get-Date) -lt $deadline)

if ($missingPorts.Count -ne 0) {
    throw "The local server did not become ready within four minutes. Closed ports: $($missingPorts -join ', ')"
}

$layersKey = 'HKCU:\Software\Microsoft\Windows NT\CurrentVersion\AppCompatFlags\Layers'
if (-not (Test-Path -LiteralPath $layersKey)) {
    New-Item -Path $layersKey -Force | Out-Null
}
$compatibilityLayers = '~ DWM8And16BitMitigation WIN7RTM DISABLEDXMAXIMIZEDWINDOWEDMODE HIGHDPIAWARE'
New-ItemProperty -LiteralPath $layersKey -Name $clientExe -PropertyType String -Value $compatibilityLayers -Force | Out-Null
New-ItemProperty -LiteralPath $layersKey -Name $launchExe -PropertyType String -Value $compatibilityLayers -Force | Out-Null

$env:__COMPAT_LAYER = 'DWM8And16BitMitigation WIN7RTM DISABLEDXMAXIMIZEDWINDOWEDMODE HIGHDPIAWARE'
Write-Host "Launching client through: $launchExe"
$process = Start-Process -FilePath $launchExe -ArgumentList @('127.0.0.1', '9595') -WorkingDirectory $launchRoot -PassThru

$hotKeyId = 0x079
$modifiers = 0x0002 -bor 0x0004 -bor 0x4000
$hotKeyRegistered = [MapleStoryGuardNative]::RegisterHotKey([IntPtr]::Zero, $hotKeyId, $modifiers, 0x7B)

Write-Host 'Client started. Press Ctrl+Shift+F12 to stop it and restore the display.'
Write-Host 'The guard also stops the client after 150 seconds of continuous unresponsiveness.'

$unresponsiveSince = $null
$forcedStop = $false
try {
    while (-not $process.HasExited) {
        $message = New-Object MapleStoryGuardNative+MSG
        while ([MapleStoryGuardNative]::PeekMessage([ref]$message, [IntPtr]::Zero, 0, 0, 1)) {
            if ($message.Message -eq 0x0312 -and $message.WParam.ToInt32() -eq $hotKeyId) {
                Write-Host 'Emergency stop hotkey received.'
                $forcedStop = $true
                Stop-MapleStory -TrackedProcess $process
                break
            }
        }
        if ($forcedStop) {
            break
        }

        $process.Refresh()
        if ($process.Responding) {
            $unresponsiveSince = $null
        } elseif ($null -eq $unresponsiveSince) {
            $unresponsiveSince = Get-Date
        } elseif (((Get-Date) - $unresponsiveSince).TotalSeconds -ge 150) {
            Write-Host 'The client was unresponsive for 150 seconds and will be stopped.'
            $forcedStop = $true
            Stop-MapleStory -TrackedProcess $process
            break
        }

        Start-Sleep -Milliseconds 500
    }
} finally {
    if ($hotKeyRegistered) {
        [MapleStoryGuardNative]::UnregisterHotKey([IntPtr]::Zero, $hotKeyId) | Out-Null
    }
    if ($forcedStop) {
        Reset-DisplayMode
    }
}
