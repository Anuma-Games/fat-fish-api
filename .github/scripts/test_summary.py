#!/usr/bin/env python3
"""Builds a Markdown summary of the Surefire XML reports for the GitHub Actions job summary.

Usage: python3 test_summary.py [reports_dir]   (default: target/surefire-reports)
Prints Markdown to stdout; the workflow appends it to $GITHUB_STEP_SUMMARY.
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET

reports_dir = sys.argv[1] if len(sys.argv) > 1 else "target/surefire-reports"
files = sorted(glob.glob(os.path.join(reports_dir, "TEST-*.xml")))

if not files:
    print("## Resultados de pruebas\n\n:warning: No se encontraron reportes de Surefire.")
    sys.exit(0)

rows = []
failures = []
totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0, "time": 0.0}

for path in files:
    suite = ET.parse(path).getroot()
    tests = int(suite.get("tests", 0))
    failed = int(suite.get("failures", 0))
    errors = int(suite.get("errors", 0))
    skipped = int(suite.get("skipped", 0))
    time = float(suite.get("time", 0) or 0)
    if tests == 0:
        continue  # suites without test cases add only noise

    name = suite.get("name", "").removeprefix("com.fatfish.api.")
    status = ":white_check_mark:" if failed + errors == 0 else ":x:"
    rows.append(f"| {status} | `{name}` | {tests} | {failed} | {errors} | {skipped} | {time:.2f} s |")

    totals["tests"] += tests
    totals["failures"] += failed
    totals["errors"] += errors
    totals["skipped"] += skipped
    totals["time"] += time

    for case in suite.iter("testcase"):
        problem = case.find("failure")
        if problem is None:
            problem = case.find("error")
        if problem is not None:
            message = (problem.get("message") or problem.get("type") or "").splitlines()
            failures.append(f"- `{name}.{case.get('name')}`: {message[0] if message else ''}")

passed = totals["tests"] - totals["failures"] - totals["errors"] - totals["skipped"]
ok = totals["failures"] + totals["errors"] == 0

print("## Resultados de pruebas\n")
print(f"{':white_check_mark:' if ok else ':x:'} **{passed} de {totals['tests']} pruebas aprobadas** "
      f"({totals['failures']} fallos, {totals['errors']} errores, {totals['skipped']} omitidas) "
      f"en {totals['time']:.1f} s\n")
print("| | Clase | Pruebas | Fallos | Errores | Omitidas | Tiempo |")
print("|---|---|---:|---:|---:|---:|---:|")
print("\n".join(rows))

if failures:
    print("\n### Pruebas fallidas\n")
    print("\n".join(failures))
