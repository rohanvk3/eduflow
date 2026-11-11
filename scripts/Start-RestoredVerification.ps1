param([string]$Schema)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$pointer = Get-Content "$workspace\.verification\current.json" -Raw | ConvertFrom-Json
$state = Get-Content $pointer.stateFile -Raw | ConvertFrom-Json
$restore = Get-Content "$($state.directory)\restore-result.json" -Raw | ConvertFrom-Json
if (!$Schema) { $Schema = $restore.restoredSchema }
if ($Schema -notmatch '^eduflow_test_restore_[a-z0-9]+$') { throw 'Refusing non-test schema' }
$root = [IO.Path]::GetFullPath((Join-Path $workspace '.verification')) + [IO.Path]::DirectorySeparatorChar
if (![IO.Path]::GetFullPath($restore.restoredFiles).StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) { throw 'Refusing storage outside isolated verification' }
$env:DB_URL = "jdbc:mysql://127.0.0.1:$($state.port)/$Schema"
$env:DB_USERNAME = 'eduflow_test'
$env:DB_PASSWORD = $state.testPassword
$bytes = New-Object byte[] 32
$random = [Security.Cryptography.RandomNumberGenerator]::Create()
$random.GetBytes($bytes); $random.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:FRONTEND_ORIGINS = 'http://127.0.0.1:15173'
$env:FILE_STORAGE_DIR = $restore.restoredFiles
$env:SERVER_PORT = '18082'
& java -jar "$workspace\backend\target\backend-0.0.1-SNAPSHOT.jar" --spring.profiles.active=prod --app.notifications.scheduler-enabled=false --app.exams.expiry-enabled=false
exit $LASTEXITCODE
