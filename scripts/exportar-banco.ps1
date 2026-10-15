$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
if (-not (Test-Path -LiteralPath 'data/pulso.mv.db')) { throw 'Abra o sistema ao menos uma vez para criar o banco.' }
& java -cp 'lib/h2-2.3.232.jar' org.h2.tools.Script -url 'jdbc:h2:file:./data/pulso' -user sa -script 'database/exportacao-local.sql'
if ($LASTEXITCODE -ne 0) { throw 'Feche o aplicativo e tente exportar novamente.' }
Write-Output 'Exportação concluída: database/exportacao-local.sql'
