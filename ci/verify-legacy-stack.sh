#!/usr/bin/env bash
set -euo pipefail

# These are a deliberate historical stack policy, NOT an upgrade suggestion.
grep -Fq '<maven.compiler.release>11</maven.compiler.release>' pom.xml
grep -Fq '<version>8.2.0</version>' pom.xml
grep -Fq '<version>3.11.0</version>' pom.xml
grep -Fq '<version>3.4.1</version>' pom.xml
grep -Fq '<version>2.0.0</version>' pom.xml

dependency_count="$(grep -c '<dependency>' pom.xml)"
if [ "$dependency_count" != 1 ]; then
  echo 'Unexpected application dependency change; owner approval required.' >&2
  exit 1
fi
if find src -type f \( -name '*.js' -o -name '*.jsx' -o -name '*.ts' -o -name '*.tsx' \) | grep -q .; then
  echo 'Non-Java frontend detected; preserve the Swing-only stack.' >&2
  exit 1
fi
if grep -R -q -E 'Cipher\.getInstance\("AES"\)' src/db; then
  echo 'Unsafe legacy ECB-like encryption detected.' >&2
  exit 1
fi
if grep -R -i -q -E 'Author:[[:space:]]*Eyup[[:space:]]+Celen' src/db; then
  echo 'Outdated source attribution remains.' >&2
  exit 1
fi
echo 'Legacy Java/Swing/JDBC/MySQL technology guard passed.'
