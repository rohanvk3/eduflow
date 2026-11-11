$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$pointer = Get-Content "$workspace\.verification\current.json" -Raw | ConvertFrom-Json
$mysql = Get-Content $pointer.stateFile -Raw | ConvertFrom-Json
$root = [IO.Path]::GetFullPath((Join-Path $workspace '.verification')) + [IO.Path]::DirectorySeparatorChar
if (![IO.Path]::GetFullPath($mysql.directory).StartsWith($root,[StringComparison]::OrdinalIgnoreCase)) { throw 'Refusing an unowned MySQL instance' }
$process = Get-CimInstance Win32_Process -Filter "ProcessId=$($mysql.mysqlPid)"
if (!$process -or $process.CommandLine -notlike "*$($mysql.directory)*") { throw 'Resume only the isolated verification MySQL instance first' }
$suffix = [guid]::NewGuid().ToString('N')
$schema = 'eduflow_demo_eduprime_' + $suffix
$directory = Join-Path $mysql.directory ('demo-' + $suffix)
New-Item -ItemType Directory -Path $directory | Out-Null
$username = 'ep_demo_' + $suffix.Substring(0,8)
$password = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
"CREATE DATABASE $schema; CREATE USER '$username'@'127.0.0.1' IDENTIFIED BY '$password'; GRANT ALL ON $schema.* TO '$username'@'127.0.0.1';" | & "$($mysql.mysqlBin)\mysql.exe" "--defaults-extra-file=$($mysql.directory)\operator.cnf"
if ($LASTEXITCODE -ne 0) { throw 'New isolated demo provisioning failed; no existing schema was altered' }
@"
[client]
host=127.0.0.1
port=$($mysql.port)
user=$username
password=$password
protocol=TCP
"@ | Set-Content -LiteralPath "$directory\client.cnf" -Encoding Ascii
$info = [Diagnostics.ProcessStartInfo]::new("$($mysql.mysqlBin)\mysql.exe")
$info.Arguments = "--defaults-extra-file=`"$directory\client.cnf`" --database=$schema"
$info.UseShellExecute=$false;$info.CreateNoWindow=$true;$info.RedirectStandardInput=$true
$child=[Diagnostics.Process]::Start($info)
$inputFile=[IO.File]::OpenRead("$workspace\database\baseline\schema.sql")
try { $inputFile.CopyTo($child.StandardInput.BaseStream) } finally { $inputFile.Dispose();$child.StandardInput.Close() }
$child.WaitForExit(); if($child.ExitCode -ne 0){throw 'Fresh demo baseline import failed'}
$bytes=New-Object byte[] 32;$random=[Security.Cryptography.RandomNumberGenerator]::Create();$random.GetBytes($bytes);$random.Dispose()
$state=@{directory=$directory;schema=$schema;port=$mysql.port;username=$username;password=$password;jwtSecret=[Convert]::ToBase64String($bytes);storage="$directory\files";adminPassword=('Demo21!'+[guid]::NewGuid().ToString('N'));teacherPassword=('Demo21!'+[guid]::NewGuid().ToString('N'));studentPassword=('Demo21!'+[guid]::NewGuid().ToString('N'));parentPassword=('Demo21!'+[guid]::NewGuid().ToString('N'))}
$state | ConvertTo-Json | Set-Content -LiteralPath "$directory\state.json" -Encoding UTF8
@{stateFile="$directory\state.json"} | ConvertTo-Json | Set-Content -LiteralPath "$workspace\.verification\demo-current.json" -Encoding UTF8
Write-Output "Created a NEW isolated demo schema: $schema. Private state: $directory. No existing records were modified."
