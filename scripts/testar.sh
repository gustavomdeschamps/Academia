#!/usr/bin/env sh
# Equivalente a scripts/testar.ps1 para Linux e macOS: compila aplicação e testes e executa a suíte.
set -e
cd "$(dirname "$0")/.."
mkdir -p out .build
javac -encoding UTF-8 --release 17 -cp lib/h2-2.3.232.jar -d out src/main/java/br/com/pulso/*.java src/test/java/br/com/pulso/*.java || { echo 'A compilação falhou.' >&2; exit 1; }
java -cp 'out:lib/h2-2.3.232.jar' br.com.pulso.SystemTest || { echo 'Um teste falhou.' >&2; exit 1; }
