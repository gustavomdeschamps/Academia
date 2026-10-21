#!/usr/bin/env sh
# Equivalente a scripts/executar.ps1 para Linux e macOS: compila e abre o aplicativo.
set -e
cd "$(dirname "$0")/.."
mkdir -p out
javac -encoding UTF-8 --release 17 -cp lib/h2-2.3.232.jar -d out src/main/java/br/com/pulso/*.java || { echo 'A compilação falhou.' >&2; exit 1; }
exec java -cp 'out:lib/h2-2.3.232.jar' br.com.pulso.Main
