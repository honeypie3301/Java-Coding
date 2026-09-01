
import re

def check_braces(fname):
    with open(fname, "r", encoding="utf-8") as f:
        lines = f.readlines()
    depth = 0
    for line_num, line in enumerate(lines, 1):
        code_line = line.split("//")[0]
        code_line = re.sub(r"\"(?:\\\"|[^\"])*\"", "", code_line)
        for ch in code_line:
            if ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
                if depth < 0:
                    print(f"[{fname}] Negative depth at line {line_num}: {line.strip()}")
                    return
    print(f"[{fname}] Final depth: {depth}")

for ver in ["1_21_1", "1_21_8"]:
    for prefix in ["RotOnEntityTickUpdateProcedure", "RotBrainProcedure"]:
        check_braces(f"Trimmed/{prefix}_{ver}.java")
