param([switch]$Seed)
$ErrorActionPreference='Stop'
$workspace=Split-Path $PSScriptRoot -Parent
$pointer=Get-Content "$workspace\.verification\demo-current.json" -Raw | ConvertFrom-Json
$state=Get-Content $pointer.stateFile -Raw | ConvertFrom-Json
if($state.schema -notmatch '^eduflow_demo_[a-z0-9_]+$'){throw 'Refusing a non-demo schema'}
$root=[IO.Path]::GetFullPath((Join-Path $workspace '.verification'))+[IO.Path]::DirectorySeparatorChar
if(![IO.Path]::GetFullPath($state.directory).StartsWith($root,[StringComparison]::OrdinalIgnoreCase)){throw 'Private demo state must be in the verification workspace'}
$jdk=Get-Content "$workspace\.verification\jdk21.json" -Raw | ConvertFrom-Json
$env:JAVA_HOME=$jdk.jdkHome;$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:DEMO_DB_URL="jdbc:mysql://127.0.0.1:$($state.port)/$($state.schema)"
$env:DEMO_DB_USERNAME=$state.username;$env:DEMO_DB_PASSWORD=$state.password
$env:JWT_SECRET=$state.jwtSecret;$env:DEMO_STORAGE_DIR=$state.storage
$env:DEMO_ADMIN_PASSWORD=$state.adminPassword;$env:DEMO_TEACHER_PASSWORD=$state.teacherPassword;$env:DEMO_STUDENT_PASSWORD=$state.studentPassword;$env:DEMO_PARENT_PASSWORD=$state.parentPassword
$env:DEMO_SEED_ENABLED=if($Seed){'true'}else{'false'}
$env:DEMO_SEED_CONFIRM=if($Seed){'EDUPRIME_SYNTHETIC_ONLY'}else{''}
$env:DEMO_SEED_EXIT='true';$env:DEMO_REPORT_PATH="$($state.directory)\seed-report.json"
& java -Xmx1024m -jar "$workspace\backend\target\backend-0.0.1-SNAPSHOT.jar" --spring.profiles.active=demo
exit $LASTEXITCODE
