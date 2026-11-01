#!/usr/bin/env python3
"""Parsira Surefire XML izvjesca i ispisuje SAZETAK PO VJEZBI kao GitHub anotacije.

Koristi se u CI-u (vidi .github/workflows/ci.yml). Jer preuzimanje punih logova iz
sandboxa mozda ne radi, sve bitno se emitira kao ::notice / ::error anotacije koje
se citaju preko `gh api .../check-runs/<id>/annotations`.

Upotreba:
    python3 scripts/ci_summary.py --branch <grana> \
        --functional-dir reports/functional --security-dir reports/security

Pravila (ciljno stanje):
  - grana != rjesenja (npr. main): svaki lab01..lab15 MORA imati >=1 pali sigurnosni
    test (dokaz ranjivosti); svi funkcionalni testovi moraju prolaziti.
  - grana == rjesenja: svi testovi (i sig. i funkc.) moraju prolaziti.
Izlazni kod != 0 ako ciljno stanje nije zadovoljeno (ruzi build).
"""
import argparse
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

LABS = [f"lab{n:02d}" for n in range(1, 16)]


def parse_dir(path):
    """Vraca {lab: {"total": t, "failed": f}} za sva TEST-*.xml u direktoriju."""
    result = {lab: {"total": 0, "failed": 0} for lab in LABS}
    if not path or not os.path.isdir(path):
        return result
    for xml_file in glob.glob(os.path.join(path, "TEST-*.xml")):
        try:
            root = ET.parse(xml_file).getroot()
        except ET.ParseError:
            continue
        name = root.get("name", "")
        m = re.search(r"lab(\d\d)", name)
        if not m:
            continue
        lab = "lab" + m.group(1)
        if lab not in result:
            continue
        tests = int(root.get("tests", "0"))
        failures = int(root.get("failures", "0"))
        errors = int(root.get("errors", "0"))
        skipped = int(root.get("skipped", "0"))
        result[lab]["total"] += (tests - skipped)
        result[lab]["failed"] += (failures + errors)
    return result


def gha(line):
    print(line, flush=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--branch", default=os.environ.get("GITHUB_REF_NAME", ""))
    ap.add_argument("--functional-dir", default="reports/functional")
    ap.add_argument("--security-dir", default="reports/security")
    args = ap.parse_args()

    functional = parse_dir(args.functional_dir)
    security = parse_dir(args.security_dir)
    is_solution = args.branch == "rjesenja"

    summary_lines = []
    problems = []

    for lab in LABS:
        sec = security[lab]
        fun = functional[lab]
        if is_solution:
            # Na grani rjesenja ocekujemo da je sve zeleno.
            if sec["total"] == 0:
                status = "NEMA SIG. TESTOVA"
                problems.append(f"{lab}: nema sigurnosnih testova")
            elif sec["failed"] == 0:
                status = f"ISPRAVLJENO ({sec['total']}/{sec['total']} sig. testova prolazi)"
            else:
                status = f"NIJE ISPRAVLJENO ({sec['failed']}/{sec['total']} sig. testova pada)"
                problems.append(f"{lab}: sigurnosni testovi i dalje padaju ({sec['failed']}/{sec['total']})")
        else:
            # Na grani main ocekujemo da svaki lab ima barem jedan pali sigurnosni test.
            if sec["total"] == 0:
                status = "NEMA SIG. TESTOVA"
                problems.append(f"{lab}: nedostaje sigurnosni test")
            elif sec["failed"] == 0:
                status = f"NIJE RANJIVO?! (0/{sec['total']} sig. testova pada)"
                problems.append(f"{lab}: niti jedan sigurnosni test ne pada - ranjivost nije dokazana")
            else:
                status = f"RANJIVO ({sec['failed']}/{sec['total']} sig. testova pada)"

        if fun["failed"] > 0:
            problems.append(f"{lab}: funkcionalni testovi padaju ({fun['failed']}/{fun['total']})")
            status += f" | FUNKC. PAD ({fun['failed']}/{fun['total']})"
        summary_lines.append(f"{lab}: {status}")

    # Sazetak kao jedna ::notice anotacija (redovi odvojeni s %0A).
    title = "Lab sazetak (rjesenja)" if is_solution else "Lab sazetak (main)"
    body = "%0A".join(summary_lines)
    gha(f"::notice title={title}::{body}")

    # Citljiv ispis u log.
    print("\n===== SAZETAK PO VJEZBI (" + (args.branch or "?") + ") =====")
    for line in summary_lines:
        print("  " + line)
    print("=================================================\n")

    if problems:
        for p in problems:
            gha(f"::error title=Ciljno stanje nije zadovoljeno::{p}")
        print(f"NEUSPJEH: {len(problems)} problema (vidi anotacije iznad).")
        return 1

    print("OK: ciljno stanje zadovoljeno.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
