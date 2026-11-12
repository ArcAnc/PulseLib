#!/usr/bin/env python3
"""Diagnose every production source against real 26.2 classpaths without suppressing failures.

@author ArcAnc
Created at: 12.11.2026
Copyright (c) 2026
This code is licensed under "Arc's License of Common Sense" (LICENSE.txt).
"""

import collections
import json
import os
from pathlib import Path
import re
import subprocess


def main():
	root = Path(__file__).resolve().parents[2]
	output = root / "build/porting/diagnostics"
	output.mkdir(parents=True, exist_ok=True)
	modules = ("common", "fabric", "neoforge")
	command = [str(root / "gradlew"), "--init-script", str(root / "tests/porting/diagnostics.gradle"),
		f"-PportingDiagnosticsDir={output}", *[f":{module}:exportPortingCompilation" for module in modules],
		"--no-parallel", "--no-daemon", "--console=plain"]
	with (output / "gradle-inputs.log").open("w") as log:
		subprocess.run(command, cwd=root, stdout=log, stderr=subprocess.STDOUT, check=True)
	summaries = []
	for module in modules:
		inputs = json.loads((output / f"{module}-inputs.json").read_text())
		# All production Java sources are explicit inputs; stale main classes must never satisfy them.
		classpath = [path for path in inputs["classpath"] if not any(
			str(root / name / "build/classes") in path for name in modules)]
		classes = output / module / "classes"
		classes.mkdir(parents=True, exist_ok=True)
		arguments = ["--release", "25", "-proc:none", "-implicit:none", "-Xmaxerrs", "10000",
			"-XDshould-stop.ifError=FLOW", "-classpath", os.pathsep.join(classpath),
			"-d", str(classes), *inputs["sources"]]
		argfile = output / f"{module}.args"
		argfile.write_text("\n".join('"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"' for value in arguments) + "\n")
		with (output / f"{module}.log").open("w") as log:
			result = subprocess.run(["javac", "@" + str(argfile)], cwd=root, stdout=log, stderr=subprocess.STDOUT)
		text = (output / f"{module}.log").read_text()
		errors = [{"path": str(Path(path).relative_to(root)), "line": int(line), "message": message}
			for path, line, message in re.findall(r"^(.+\.java):(\d+): error: (.+)$", text, re.M)]
		summary = {"module": module, "exit_code": result.returncode, "source_count": len(inputs["sources"]),
			"error_count": len(errors), "files": dict(sorted(collections.Counter(error["path"] for error in errors).items())),
			"errors": errors}
		summaries.append(summary)
		print(f"{module}: exit={result.returncode}, sources={summary['source_count']}, errors={len(errors)}")
	(output / "summary.json").write_text(json.dumps(summaries, indent=2) + "\n")
	print(f"Full diagnostics: {output}")
	return 1 if any(summary["exit_code"] for summary in summaries) else 0


if __name__ == "__main__":
	raise SystemExit(main())
