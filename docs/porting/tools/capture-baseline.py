#!/usr/bin/env python3
"""Capture a Git-backed source API baseline without configuring Gradle.

@author ArcAnc
Created at: 12.11.2026
Copyright (c) 2026
This code is licensed under "Arc's License of Common Sense".
Details can be found in LICENSE.txt in the root folder of this project.
"""

import argparse
import hashlib
import io
from pathlib import Path
import subprocess
import tarfile
import tempfile


def main():
	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument("ref", help="Commit containing the three canonical modules")
	parser.add_argument("output", type=Path, help="Output directory (use /tmp to compare)")
	args = parser.parse_args()
	root = Path(subprocess.check_output(["git", "rev-parse", "--show-toplevel"], text=True).strip())
	commit = subprocess.check_output(["git", "rev-parse", f"{args.ref}^{{commit}}"], cwd=root, text=True).strip()
	paths = subprocess.check_output(["git", "ls-tree", "-r", "--name-only", commit], cwd=root, text=True).splitlines()
	paths = [path for path in paths if path.endswith(".java") and any(
		path.startswith(f"{module}/src/main/java/") for module in ("common", "fabric", "neoforge"))]
	if not paths:
		raise SystemExit("No MultiLoader production Java sources in the selected commit")
	output = args.output.resolve()
	output.mkdir(parents=True, exist_ok=True)
	with tempfile.TemporaryDirectory(prefix="pulselib-api-") as temp:
		temp = Path(temp)
		archive = subprocess.check_output(["git", "archive", commit, "common/src/main/java", "fabric/src/main/java", "neoforge/src/main/java"], cwd=root)
		with tarfile.open(fileobj=io.BytesIO(archive)) as source:
			source.extractall(temp, filter="data")
		header = "# @author ArcAnc; Created at: 12.11.2026; Copyright (c) 2026; Arc's License of Common Sense (LICENSE.txt)"
		manifest = [f"# source_commit={commit}", header, "# sha256  repository_path"]
		for path in sorted(paths):
			manifest.append(f"{hashlib.sha256((temp / path).read_bytes()).hexdigest()}  {path}")
		(output / "sources.sha256").write_text("\n".join(manifest) + "\n", encoding="utf-8")
		tool = Path(__file__).with_name("ApiBaseline.java").resolve()
		result = subprocess.run(["java", "--source", "25", str(tool), str(temp)], text=True, capture_output=True, check=True)
		(output / "public-api.txt").write_text(
			f"# source_commit={commit}\n{header}\n# Parse-only JDK 25 signatures; see the porting journal for scope and limitations.\n" + result.stdout.rstrip() + "\n",
			encoding="utf-8")
	print(f"Captured {len(paths)} Java sources at {commit} into {output}")


if __name__ == "__main__":
	main()
