$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$pointer = Get-Content "$workspace\.verification\current.json" -Raw | ConvertFrom-Json
$state = Get-Content $pointer.stateFile -Raw | ConvertFrom-Json
$schema = 'eduflow_test_restore_' + [guid]::NewGuid().ToString('N')
$baseline = Join-Path $workspace 'database\baseline\schema.sql'
if (Select-String -LiteralPath $baseline -Pattern '^(INSERT INTO|DROP TABLE|CREATE DATABASE|USE )' -Quiet) { throw 'Unexpected destructive or data SQL in baseline' }
$mysql = Join-Path $state.mysqlBin 'mysql.exe'
"CREATE DATABASE $schema; GRANT ALL ON $schema.* TO 'eduflow_test'@'127.0.0.1';" | & $mysql "--defaults-extra-file=$($state.directory)\operator.cnf"
if ($LASTEXITCODE -ne 0) { throw 'Empty baseline test schema creation failed' }
$info = [Diagnostics.ProcessStartInfo]::new($mysql)
$info.Arguments = "--defaults-extra-file=`"$($state.directory)\client.cnf`" --database=$schema"
$info.UseShellExecute = $false; $info.CreateNoWindow = $true; $info.RedirectStandardInput = $true
$process = [Diagnostics.Process]::Start($info)
$inputFile = [IO.File]::OpenRead($baseline)
try { $inputFile.CopyTo($process.StandardInput.BaseStream) } finally { $inputFile.Dispose(); $process.StandardInput.Close() }
$process.WaitForExit(); if ($process.ExitCode -ne 0) { throw 'Baseline import failed' }
$count = & $mysql "--defaults-extra-file=$($state.directory)\client.cnf" --batch --skip-column-names "--execute=SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$schema'"
if ($LASTEXITCODE -ne 0 -or [int]$count -ne 48) { throw 'Baseline table inventory mismatch' }
$state | Add-Member -NotePropertyName baselineSchema -NotePropertyValue $schema -Force
$state | ConvertTo-Json | Set-Content -LiteralPath $pointer.stateFile -Encoding UTF8
@{schema=$schema;tables=[int]$count;sha256=(Get-FileHash $baseline).Hash;import='passed';productionValidation='pending'} | ConvertTo-Json | Set-Content -LiteralPath "$($state.directory)\baseline-result.json" -Encoding UTF8
Write-Output 'Baseline imported into a newly created empty isolated schema (48 tables).'
& "$PSScriptRoot\Start-RestoredVerification.ps1" -Schema $schema
