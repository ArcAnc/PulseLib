#!/usr/bin/env python3
# ArcAnc | 20.11.2026 | Copyright (c) 2026 | Arc's License of Common Sense

"""Launch the real production server and drive its native console over stdin."""

import argparse
import os
from pathlib import Path
import queue
import re
import signal
import subprocess
import threading
import time


def run(loader: str, log: Path) -> None:
    args = [
        "./gradlew", "--init-script", "tests/porting/server-check.gradle",
        f":{loader}:runServer", f"-PportingServerLoader={loader}",
        "--offline", "--no-parallel", "--no-daemon", "--console=plain",
    ]
    started = ticked = saved = stopping = flushed = False
    lines: queue.Queue[str | None] = queue.Queue()

    with log.open("w", encoding="utf-8") as output, subprocess.Popen(
        args, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True, errors="replace", start_new_session=True,
    ) as process:
        def collect() -> None:
            try:
                for line in process.stdout:
                    lines.put(line)
            finally:
                lines.put(None)

        threading.Thread(target=collect, daemon=True).start()
        deadline = time.monotonic() + 210
        try:
            while True:
                line = lines.get(timeout=max(0, deadline - time.monotonic()))
                if line is None:
                    break
                output.write(line)
                output.flush()
                if not started and re.search(r'\bDone \([0-9.]+s\)! For help', line):
                    started = True
                    process.stdin.write("time query gametime\n")
                    process.stdin.flush()
                elif started and not ticked and re.search(r"The game time is [1-9][0-9]* tick", line):
                    ticked = True
                    process.stdin.write("save-all\n")
                    process.stdin.flush()
                elif ticked and not saved and "Saved the game" in line:
                    saved = True
                    process.stdin.write("stop\n")
                    process.stdin.flush()
                elif saved and "Stopping server" in line:
                    stopping = True
                elif stopping and "All dimensions are saved" in line:
                    flushed = True
            if process.wait(timeout=5) != 0 or not flushed:
                raise RuntimeError(f"{loader} failed startup, console query, save or stop. See {log}")
        except (queue.Empty, subprocess.TimeoutExpired) as failure:
            raise RuntimeError(f"{loader} did not finish within the fixture timeout. See {log}") from failure
        finally:
            if process.poll() is None:
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
    print(f"PASS {loader} dedicated production startup, native console, world save and shutdown")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("loader", choices=("fabric", "neoforge"))
    parser.add_argument("log", type=Path)
    options = parser.parse_args()
    run(options.loader, options.log)
