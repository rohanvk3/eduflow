$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$pointer = Get-Content "$workspace\.verification\current.json" -Raw | ConvertFrom-Json
$state = Get-Content $pointer.stateFile -Raw | ConvertFrom-Json
$schema = 'eduflow_test_browser_' + [guid]::NewGuid().ToString('N')
"CREATE DATABASE $schema; GRANT ALL ON $schema.* TO 'eduflow_test'@'127.0.0.1';" | & "$($state.mysqlBin)\mysql.exe" "--defaults-extra-file=$($state.directory)\operator.cnf"
if ($LASTEXITCODE -ne 0) { throw 'Fresh browser schema creation failed' }
$state | Add-Member -NotePropertyName browserSchema -NotePropertyValue $schema -Force
$state | ConvertTo-Json | Set-Content -LiteralPath $pointer.stateFile -Encoding UTF8
$env:EDUFLOW_TEST_DB_URL = "jdbc:mysql://127.0.0.1:$($state.port)/$schema"
$env:EDUFLOW_TEST_DB_PASSWORD = $state.testPassword
$env:EDUFLOW_VERIFICATION_DIR = $state.directory
$env:EDUFLOW_E2E_PASSWORD = 'E2e!' + [guid]::NewGuid().ToString('N')
Push-Location "$workspace\backend"
try {
    & .\mvnw.cmd test-compile dependency:build-classpath '-Dmdep.outputFile=target/browser-classpath.txt'
    if ($LASTEXITCODE -ne 0) { throw 'Test launcher compilation failed' }
    $dependencies = Get-Content target/browser-classpath.txt -Raw
    & java -cp "target/test-classes;target/classes;$($dependencies.Trim())" com.eduflow.backend.BrowserFixtureServer
    exit $LASTEXITCODE
} finally { Pop-Location }
