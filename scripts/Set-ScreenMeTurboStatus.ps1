param(
    [Parameter(Mandatory)][string]$StatusPath,
    [Parameter(Mandatory)][ValidateSet('NEW', 'IN_PROGRESS', 'DONE', 'NEEDS_INFO')][string]$State,
    [string]$Worker = 'Codex',
    [string]$Result = '',
    [string]$Commit = ''
)

$resolved = (Resolve-Path -LiteralPath $StatusPath).Path
if ([IO.Path]::GetFileName($resolved) -ne 'turbo-status.json') {
    throw 'StatusPath mora kazati na turbo-status.json.'
}

$status = Get-Content -LiteralPath $resolved -Raw -Encoding UTF8 | ConvertFrom-Json
$now = [DateTimeOffset]::Now.ToString('o')
$status.state = $State
$status | Add-Member -NotePropertyName worker -NotePropertyValue $Worker -Force
$status | Add-Member -NotePropertyName updatedAt -NotePropertyValue $now -Force

if ($State -eq 'IN_PROGRESS') {
    $status | Add-Member -NotePropertyName startedAt -NotePropertyValue $now -Force
}
if ($State -in @('DONE', 'NEEDS_INFO')) {
    $status | Add-Member -NotePropertyName finishedAt -NotePropertyValue $now -Force
    if ($Result) { $status | Add-Member -NotePropertyName result -NotePropertyValue $Result -Force }
    if ($Commit) { $status | Add-Member -NotePropertyName commit -NotePropertyValue $Commit -Force }
}

$json = $status | ConvertTo-Json -Depth 8
[IO.File]::WriteAllText($resolved, $json + [Environment]::NewLine, [Text.UTF8Encoding]::new($false))
$status
