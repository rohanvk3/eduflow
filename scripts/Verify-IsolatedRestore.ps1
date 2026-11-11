$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$pointer = Get-Content "$workspace\.verification\current.json" -Raw | ConvertFrom-Json
$state = Get-Content $pointer.stateFile -Raw | ConvertFrom-Json
if ($state.browserSchema -notmatch '^eduflow_test_browser_[a-z0-9]+$') { throw 'No isolated browser database recorded' }
$root = [IO.Path]::GetFullPath((Join-Path $workspace '.verification')) + [IO.Path]::DirectorySeparatorChar
if (![IO.Path]::GetFullPath($state.directory).StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) { throw 'State directory is outside verification workspace' }
$snapshot = Join-Path $state.directory ('backup-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $snapshot | Out-Null
$client = "--defaults-extra-file=$($state.directory)\client.cnf"
$operator = "--defaults-extra-file=$($state.directory)\operator.cnf"
$mysql = Join-Path $state.mysqlBin 'mysql.exe'
$dump = Join-Path $snapshot 'database.sql'
$files = Join-Path $state.directory 'files'
if (!(Test-Path $files)) { throw 'Browser upload evidence is required before restore verification' }
$started = Get-Date
& "$($state.mysqlBin)\mysqldump.exe" $client --single-transaction --quick --routines --events --triggers --no-tablespaces --set-gtid-purged=OFF --skip-add-drop-table --default-character-set=utf8mb4 "--result-file=$dump" $state.browserSchema
if ($LASTEXITCODE -ne 0) { throw 'Isolated dump failed' }
if (Select-String -LiteralPath $dump -Pattern '^CREATE DATABASE|^USE |^DROP TABLE' -Quiet) { throw 'Dump contains forbidden database selection or destructive DDL' }
Compress-Archive -Path "$files\*" -DestinationPath "$snapshot\private-files.zip"
$restore = 'eduflow_test_restore_' + [guid]::NewGuid().ToString('N')
"CREATE DATABASE $restore; GRANT ALL ON $restore.* TO 'eduflow_test'@'127.0.0.1';" | & $mysql $operator
if ($LASTEXITCODE -ne 0) { throw 'Isolated restore schema creation failed' }
# Preserve dump bytes; PowerShell 5 redirection can otherwise produce UTF-16 SQL.
$info = [Diagnostics.ProcessStartInfo]::new($mysql)
$info.Arguments = "--defaults-extra-file=`"$($state.directory)\client.cnf`" --default-character-set=utf8mb4 --database=$restore"
$info.UseShellExecute = $false; $info.CreateNoWindow = $true
$info.RedirectStandardInput = $true; $info.RedirectStandardError = $true
$process = [Diagnostics.Process]::Start($info)
$inputFile = [IO.File]::OpenRead($dump)
try { $inputFile.CopyTo($process.StandardInput.BaseStream) } finally { $inputFile.Dispose(); $process.StandardInput.Close() }
$importError = $process.StandardError.ReadToEnd(); $process.WaitForExit()
if ($process.ExitCode -ne 0) { throw "Restore import failed: $importError" }
$restoredFiles = Join-Path $snapshot 'restored-files'
Expand-Archive -LiteralPath "$snapshot\private-files.zip" -DestinationPath $restoredFiles
$tables = @(& $mysql $client --batch --skip-column-names "--database=$($state.browserSchema)" --execute='SHOW TABLES')
if ($LASTEXITCODE -ne 0 -or $tables.Count -lt 10) { throw 'Schema inventory failed' }
foreach ($table in $tables) {
    if ($table -notmatch '^[a-z0-9_]+$') { throw 'Unexpected table identifier' }
    $sourceChecksum = & $mysql $client --batch --skip-column-names "--execute=CHECKSUM TABLE $($state.browserSchema).$table"
    $restoreChecksum = & $mysql $client --batch --skip-column-names "--execute=CHECKSUM TABLE $restore.$table"
    if ($LASTEXITCODE -ne 0 -or ($sourceChecksum -split "`t")[-1] -ne ($restoreChecksum -split "`t")[-1]) { throw "Table checksum mismatch: $table" }
}
$fileCount = 0
foreach ($file in Get-ChildItem -LiteralPath $files -File) {
    $recovered = Join-Path $restoredFiles $file.Name
    if ((Get-FileHash -LiteralPath $file.FullName).Hash -ne (Get-FileHash -LiteralPath $recovered).Hash) { throw "File checksum mismatch: $($file.Name)" }
    $fileCount++
}
if ($fileCount -eq 0) { throw 'No uploaded files recovered' }
$logo = & $mysql $client --batch --skip-column-names "--database=$restore" --execute='SELECT logo_storage_key FROM institute_settings WHERE id=1'
if (!(Test-Path -LiteralPath (Join-Path $restoredFiles $logo))) { throw 'Restored database logo reference is missing its file' }
$references = @(& $mysql $client --batch --skip-column-names "--database=$restore" --execute='SELECT storage_key FROM assignment_attachments UNION SELECT storage_key FROM submission_attachments UNION SELECT storage_key FROM study_material_versions UNION SELECT logo_storage_key FROM institute_settings WHERE logo_storage_key IS NOT NULL')
if ($LASTEXITCODE -ne 0) { throw 'Restored file reference inventory failed' }
foreach ($key in $references) {
    if ($key -notmatch '^[0-9a-f-]+\.bin$' -or !(Test-Path -LiteralPath (Join-Path $restoredFiles $key))) { throw 'A restored database file reference is missing or invalid' }
}
& "$($state.mysqlBin)\mysqldump.exe" $client --no-data --skip-add-drop-table --no-tablespaces --set-gtid-purged=OFF "--result-file=$snapshot\schema.sql" $restore
if ($LASTEXITCODE -ne 0) { throw 'Validated schema export failed' }
$result = @{ sourceSchema=$state.browserSchema; restoredSchema=$restore; tablesVerified=$tables.Count; filesVerified=$fileCount; databaseFileReferencesVerified=$references.Count; restoredFiles=$restoredFiles; snapshot=$snapshot; elapsedSeconds=[math]::Round(((Get-Date)-$started).TotalSeconds,2); dumpSHA256=(Get-FileHash $dump).Hash; fileArchiveSHA256=(Get-FileHash "$snapshot\private-files.zip").Hash }
$result | ConvertTo-Json | Set-Content -LiteralPath "$($state.directory)\restore-result.json" -Encoding UTF8
[pscustomobject]$result | Select-Object sourceSchema,restoredSchema,tablesVerified,filesVerified,elapsedSeconds | Format-List
Write-Output 'Isolated database and uploaded-file restore checks passed. No existing database or files were overwritten.'
