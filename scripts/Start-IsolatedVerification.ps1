param([string]$MySqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin')
$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$run = Join-Path $workspace ('.verification\' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $run | Out-Null
# Keep generated credentials accessible only to this user and SYSTEM.
$acl = Get-Acl $run
$acl.SetAccessRuleProtection($true, $false)
$identity = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
$acl.AddAccessRule([System.Security.AccessControl.FileSystemAccessRule]::new($identity, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow'))
$acl.AddAccessRule([System.Security.AccessControl.FileSystemAccessRule]::new('SYSTEM', 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow'))
Set-Acl -LiteralPath $run -AclObject $acl
$data = Join-Path $run 'mysql-data'
& "$MySqlBin\mysqld.exe" --no-defaults --initialize-insecure "--datadir=$data" "--log-error=$run\initialize.log"
if ($LASTEXITCODE -ne 0) { throw "Isolated initialization failed; inspect $run\initialize.log" }
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
$listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
$process = Start-Process -WindowStyle Hidden -PassThru -FilePath "$MySqlBin\mysqld.exe" -ArgumentList @('--no-defaults', "--datadir=`"$data`"", '--bind-address=127.0.0.1', "--port=$port", '--mysqlx=0', '--skip-log-bin', "--log-error=`"$run\mysql.log`"")
$ready = $false
for ($attempt = 0; $attempt -lt 100; $attempt++) {
    & "$MySqlBin\mysqladmin.exe" --no-defaults --protocol=TCP --host=127.0.0.1 "--port=$port" --user=root ping 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    Start-Sleep -Milliseconds 200
}
if (!$ready) { throw "Isolated MySQL did not start; inspect $run\mysql.log" }
$password = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
$rootPassword = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
"CREATE DATABASE eduflow_test_suite; CREATE DATABASE eduflow_test_browser; CREATE DATABASE eduflow_test_restore; CREATE USER 'eduflow_test'@'127.0.0.1' IDENTIFIED BY '$password'; GRANT ALL ON eduflow_test_suite.* TO 'eduflow_test'@'127.0.0.1'; GRANT ALL ON eduflow_test_browser.* TO 'eduflow_test'@'127.0.0.1'; GRANT ALL ON eduflow_test_restore.* TO 'eduflow_test'@'127.0.0.1'; ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPassword';" | & "$MySqlBin\mysql.exe" --no-defaults --protocol=TCP --host=127.0.0.1 "--port=$port" --user=root
if ($LASTEXITCODE -ne 0) { throw 'Test schema/account initialization failed' }
@"
[client]
host=127.0.0.1
port=$port
user=eduflow_test
password=$password
protocol=TCP
"@ | Set-Content -LiteralPath "$run\client.cnf" -Encoding Ascii
@"
[client]
host=127.0.0.1
port=$port
user=root
password=$rootPassword
protocol=TCP
"@ | Set-Content -LiteralPath "$run\operator.cnf" -Encoding Ascii
$state = @{ directory=$run; port=$port; mysqlPid=$process.Id; mysqlBin=$MySqlBin; testPassword=$password }
$state | ConvertTo-Json | Set-Content -LiteralPath "$run\state.json" -Encoding UTF8
@{ stateFile="$run\state.json" } | ConvertTo-Json | Set-Content -LiteralPath "$workspace\.verification\current.json" -Encoding UTF8
Write-Output "Isolated MySQL started on loopback port $port; state in $run. No existing database was accessed."
