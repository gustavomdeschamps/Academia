$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
New-Item -ItemType Directory -Force -Path out,'.build' | Out-Null
$taskSources = Get-ChildItem 'src/main/java/br/com/pulso','src/test/java/br/com/pulso' -Filter '*.java' | ForEach-Object { $_.FullName }
& javac -encoding UTF-8 --release 17 -cp 'lib/h2-2.3.232.jar' -d out @taskSources
if ($LASTEXITCODE -ne 0) { throw 'A compilação falhou.' }
& java -cp 'out;lib/h2-2.3.232.jar' br.com.pulso.SystemTest
if ($LASTEXITCODE -ne 0) { throw 'Um teste falhou.' }
