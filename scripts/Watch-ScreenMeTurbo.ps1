param(
    [string]$Inbox = 'G:\Moj disk\ScreenMe Turbo\ScreenMe',
    [ValidateRange(1, 3600)][int]$PollSeconds = 3,
    [switch]$Once
)

$seen = @{}

do {
    if (Test-Path -LiteralPath $Inbox) {
        Get-ChildItem -LiteralPath $Inbox -Recurse -Filter 'turbo-status.json' -File -ErrorAction SilentlyContinue |
            ForEach-Object {
                $statusFile = $_
                try {
                    $status = Get-Content -LiteralPath $statusFile.FullName -Raw -Encoding UTF8 | ConvertFrom-Json
                    $stamp = $statusFile.LastWriteTimeUtc.Ticks
                    if ($status.state -eq 'NEW' -and $seen[$statusFile.FullName] -ne $stamp) {
                        $record = $statusFile.Directory.FullName
                        [pscustomobject]@{
                            statusPath = $statusFile.FullName
                            recordPath = $record
                            recordId = $status.recordId
                            project = $status.project
                            title = $status.title
                            note = Join-Path $record 'note.md'
                            metadata = Join-Path $record 'metadata.json'
                            screenshot = if (Test-Path -LiteralPath (Join-Path $record 'annotated.png')) { Join-Path $record 'annotated.png' } else { Join-Path $record 'screenshot.png' }
                        } | ConvertTo-Json -Compress
                        $seen[$statusFile.FullName] = $stamp
                    }
                } catch {
                    Write-Warning "Neveljaven Turbo zapis: $($statusFile.FullName)"
                }
            }
    } elseif ($Once) {
        Write-Warning "Turbo mapa še ni sinhronizirana: $Inbox"
    }

    if (-not $Once) { Start-Sleep -Seconds $PollSeconds }
} while (-not $Once)
