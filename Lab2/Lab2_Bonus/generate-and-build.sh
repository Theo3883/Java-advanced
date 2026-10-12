#!/usr/bin/env bash
# =============================================================================
# generate-and-build.sh
#
# Convenience script that:
#  1. Detects the project Java installation (IntelliJ bundled JDK or system)
#  2. Compiles ComponentGenerator
#  3. Runs it to produce the 1000 synthetic classes
#  4. Builds the full project with Maven
#  5. Runs the benchmark
#
# Usage:  ./generate-and-build.sh [project-root]
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${1:-$SCRIPT_DIR}"

# ── Locate Java ───────────────────────────────────────────────────────────────
JAVA_BIN=""
for candidate in \
    "/Users/$(whoami)/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home/bin/java" \
    "/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home/bin/java" \
    "$JAVA_HOME/bin/java" \
    "$(which java 2>/dev/null)"; do
  if [ -x "$candidate" ]; then
    JAVA_BIN="$candidate"
    break
  fi
done

if [ -z "$JAVA_BIN" ]; then
  echo "ERROR: Could not find a Java runtime. Set JAVA_HOME or install Java."
  exit 1
fi

JAVA_HOME_DETECTED="$(dirname "$(dirname "$JAVA_BIN")")"
echo "Using Java: $JAVA_BIN"
"$JAVA_BIN" -version

# ── Locate Maven ──────────────────────────────────────────────────────────────
MVN_BIN=""
for candidate in \
    "$(which mvn 2>/dev/null)" \
    "/usr/local/bin/mvn" \
    "/opt/homebrew/bin/mvn"; do
  if [ -x "$candidate" ]; then
    MVN_BIN="$candidate"
    break
  fi
done

if [ -z "$MVN_BIN" ]; then
  # Fall back to the Maven wrapper if present
  if [ -f "$PROJECT_ROOT/mvnw" ]; then
    MVN_BIN="$PROJECT_ROOT/mvnw"
  else
    echo "ERROR: Maven not found. Install Maven or add it to PATH."
    exit 1
  fi
fi
echo "Using Maven: $MVN_BIN"

export JAVA_HOME="$JAVA_HOME_DETECTED"
export PATH="$JAVA_HOME/bin:$PATH"

# ── Step 1: Compile ComponentGenerator (needs only JDK, no deps) ─────────────
echo ""
echo "=== Step 1: Compiling ComponentGenerator ==="
GENERATOR_SRC="$PROJECT_ROOT/src/main/java/org/example/lab2bonus/generator/ComponentGenerator.java"
ANNOTATION_SRC="$PROJECT_ROOT/src/main/java/org/example/lab2bonus/annotation/MyComponent.java"
GEN_OUT="$PROJECT_ROOT/target/generator-classes"
mkdir -p "$GEN_OUT"

"$JAVA_BIN/../javac" -d "$GEN_OUT" "$ANNOTATION_SRC" "$GENERATOR_SRC"
echo "Compiled."

# ── Step 2: Run generator to produce synthetic classes ────────────────────────
echo ""
echo "=== Step 2: Generating synthetic component classes ==="
"$JAVA_BIN" -cp "$GEN_OUT" org.example.lab2bonus.generator.ComponentGenerator "$PROJECT_ROOT"

# ── Step 3: Full Maven build ──────────────────────────────────────────────────
echo ""
echo "=== Step 3: Building full project with Maven ==="
cd "$PROJECT_ROOT"
"$MVN_BIN" clean package -q

# ── Step 4: Run benchmark ─────────────────────────────────────────────────────
echo ""
echo "=== Step 4: Running benchmark ==="
FAT_JAR="$PROJECT_ROOT/target/Lab2_Bonus-1.0-SNAPSHOT.jar"

"$JAVA_BIN" \
    -Xlog:class+load=info:file="$PROJECT_ROOT/target/classload-full.log" \
    -jar "$FAT_JAR" 2>&1 | tee "$PROJECT_ROOT/target/benchmark-output.txt"

echo ""
echo "=== Done ==="
echo "Benchmark output : $PROJECT_ROOT/target/benchmark-output.txt"
echo "Class-load log   : $PROJECT_ROOT/target/classload-full.log"
