$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$pointer = Get-Content "$workspace\.verification\current.json" -Raw | ConvertFrom-Json
$state = Get-Content $pointer.stateFile -Raw | ConvertFrom-Json
$restore = Get-Content "$($state.directory)\restore-result.json" -Raw | ConvertFrom-Json
$fixtures = Get-Content "$($state.directory)\fixtures.json" -Raw | ConvertFrom-Json
$base = 'http://127.0.0.1:18082/api'
function Login($role) {
    $body = @{email="$role@e2e.invalid";password=$fixtures.password} | ConvertTo-Json
    $auth = Invoke-RestMethod "$base/auth/login" -Method Post -ContentType application/json -Body $body
    return @{Authorization="Bearer $($auth.token)"}
}
$admin = Login 'admin'
if ((Invoke-RestMethod "$base/health").status -ne 'UP') { throw 'Liveness failed' }
if ((Invoke-RestMethod "$base/health/readiness" -Headers $admin).status -ne 'UP') { throw 'Database readiness failed' }
$accounts = Invoke-RestMethod "$base/fees/accounts?studentId=$($fixtures.studentId)" -Headers $admin
$account = $accounts.content | Select-Object -First 1
if ([decimal]$account.paidAmount -ne 300 -or [decimal]$account.outstandingAmount -ne 700) { throw 'Restored fee ledger does not reconcile' }
$student = Login 'student'
$submissions = Invoke-RestMethod "$base/assignments/me/submissions" -Headers $student
$graded = $submissions.content | Where-Object assignmentTitle -eq 'E2E Algebra Homework' | Select-Object -First 1
if ($graded.feedback -ne 'Verified browser feedback') { throw 'Restored assignment grade is missing' }
$mysql = Join-Path $state.mysqlBin 'mysql.exe'
$logo = & $mysql "--defaults-extra-file=$($state.directory)\client.cnf" --batch --skip-column-names "--database=$($restore.restoredSchema)" --execute='SELECT logo_storage_key FROM institute_settings WHERE id=1'
if ($LASTEXITCODE -ne 0) { throw 'Restored logo reference query failed' }
$download = Join-Path $restore.snapshot 'authorized-recovered-logo.png'
Invoke-WebRequest "$base/institute/logo" -Headers $admin -OutFile $download -UseBasicParsing
if ((Get-FileHash $download).Hash -ne (Get-FileHash (Join-Path $restore.restoredFiles $logo)).Hash) { throw 'Authorized recovered file content mismatch' }
$parent = Login 'parent'
$denied = $false
try { Invoke-RestMethod "$base/parents/children/$($fixtures.studentId)/fees" -Headers $parent | Out-Null }
catch { if ([int]$_.Exception.Response.StatusCode -eq 403) { $denied=$true } else { throw } }
if (!$denied) { throw 'Revoked parent link did not survive restoration' }
@{profile='prod';schemaValidation='passed';liveness='UP';databaseReadiness='UP';feePaid=300;feeOutstanding=700;assignmentFeedback='verified';authorizedRecoveredFile='SHA256 matched';parentRevocation='403'} | ConvertTo-Json | Set-Content -LiteralPath "$($state.directory)\production-restore-check.json" -Encoding UTF8
Write-Output 'Production JAR restored-data smoke checks passed: validated schema, DB health, ledger, grades, authorized recovered file and parent revocation.'
