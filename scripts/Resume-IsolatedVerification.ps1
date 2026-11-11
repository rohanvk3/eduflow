$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$pointer = Get-Content "$workspace\.verification\current.json" -Raw | ConvertFrom-Json
$state = Get-Content $pointer.stateFile -Raw | ConvertFrom-Json
$root = [IO.Path]::GetFullPath((Join-Path $workspace '.verification')) + [IO.Path]::DirectorySeparatorChar
if (![IO.Path]::GetFullPath($state.directory).StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) { throw 'Refusing a data directory outside isolated verification' }
$data = Join-Path $state.directory 'mysql-data'
if (!(Test-Path -LiteralPath (Join-Path $data 'auto.cnf'))) { throw 'Previously initialized isolated MySQL data is required' }
# Refuse to contact a pre-existing service occupying the recorded port.
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, [int]$state.port)
try { $listener.Start() } finally { $listener.Stop() }
$process = Start-Process -WindowStyle Hidden -PassThru -FilePath "$($state.mysqlBin)\mysqld.exe" -ArgumentList @('--no-defaults', "--datadir=`"$data`"", '--bind-address=127.0.0.1', "--port=$($state.port)", '--mysqlx=0', '--skip-log-bin', "--log-error=`"$($state.directory)\mysql.log`"")
$state.mysqlPid = $process.Id
$state | ConvertTo-Json | Set-Content -LiteralPath $pointer.stateFile -Encoding UTF8
$ready = $false
for ($attempt = 0; $attempt -lt 100; $attempt++) {
    $ErrorActionPreference='Continue'
    & "$($state.mysqlBin)\mysqladmin.exe" "--defaults-extra-file=$($state.directory)\operator.cnf" ping 2>$null | Out-Null
    $ErrorActionPreference='Stop'
    if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    Start-Sleep -Milliseconds 200
}
if (!$ready) { throw 'Isolated MySQL failed to resume; inspect its private log' }
Write-Output "Resumed only the preserved isolated MySQL instance on 127.0.0.1:$($state.port). No schema was reset."
