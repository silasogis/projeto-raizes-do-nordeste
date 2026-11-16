#!/bin/sh
set -e

if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
    if [ -x "/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home/bin/java" ]; then
        export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home"
    elif [ -x "/usr/libexec/java_home" ]; then
        JH="$(/usr/libexec/java_home 2>/dev/null || true)"
        if [ -n "$JH" ] && [ -x "$JH/bin/java" ]; then
            export JAVA_HOME="$JH"
        fi
    fi
fi

if [ -n "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

MVN_BIN="/Users/silasogeo/.m2/wrapper/dists/apache-maven-3.9.16/56ba1f9f/bin/mvn"
if [ ! -f "$MVN_BIN" ]; then
    MVN_BIN="$(command -v mvn || true)"
fi

if [ -x "$MVN_BIN" ]; then
    exec "$MVN_BIN" "$@"
fi

echo "Erro: Maven ou Java não encontrados." >&2
exit 1
