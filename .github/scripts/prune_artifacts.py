"""Conserva artefactos recientes sin borrar los que usan ejecuciones activas."""
import argparse
from collections import defaultdict
from datetime import datetime
import json
import os
from pathlib import Path
import subprocess


def newest(artifact):
    return datetime.fromisoformat(artifact["created_at"].replace("Z", "+00:00")), artifact["id"]


def plan_cleanup(artifacts, runs, policy):
    groups = defaultdict(list)
    for artifact in artifacts:
        if artifact.get("expired") or artifact["name"] not in policy:
            continue
        run = runs.get(artifact.get("workflow_run", {}).get("id"), {})
        if run.get("status") == "completed":
            groups[artifact["name"]].append(artifact)

    keep, delete = [], []
    for name, group in groups.items():
        rule = policy[name]
        if rule["keep"] < 1:
            raise ValueError("La política debe conservar al menos un artefacto")
        candidates = sorted(group, key=newest, reverse=True)
        if rule.get("build"):
            candidates = [a for a in candidates if runs[a["workflow_run"]["id"]].get("event") != "pull_request"]
            successful = [a for a in candidates if runs[a["workflow_run"]["id"]].get("conclusion") == "success"]
            # Si nunca terminó bien CI, conserva el último build disponible de una rama.
            candidates = successful or candidates
        retained = candidates[:rule["keep"]]
        retained_ids = {a["id"] for a in retained}
        keep.extend(retained)
        delete.extend(a for a in group if a["id"] not in retained_ids)
    return keep, delete


def gh(*args):
    result = subprocess.run(["gh", "api", *args], check=True, capture_output=True, text=True)
    return json.loads(result.stdout) if result.stdout.strip() else None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="Borrar; sin este argumento solo muestra el plan")
    args = parser.parse_args()
    repo = os.environ["GITHUB_REPOSITORY"]
    policy = json.loads(os.environ["ARTIFACT_POLICY"])
    pages = gh(f"repos/{repo}/actions/artifacts?per_page=100", "--paginate", "--slurp")
    artifacts = [a for page in pages for a in page["artifacts"]]
    run_ids = {a.get("workflow_run", {}).get("id") for a in artifacts
               if a["name"] in policy and not a.get("expired")}
    runs = {run_id: gh(f"repos/{repo}/actions/runs/{run_id}") for run_id in run_ids if run_id is not None}
    keep, candidates = plan_cleanup(artifacts, runs, policy)
    removed = []
    for artifact in candidates:
        print(f"{'Borrar' if args.apply else 'Candidato'}: {artifact['name']} #{artifact['id']}")
        if not args.apply:
            continue
        # Puede haberse reintentado un workflow después de preparar el plan.
        live = gh(f"repos/{repo}/actions/runs/{artifact['workflow_run']['id']}")
        if live["status"] != "completed":
            print(f"Se conserva #{artifact['id']}: su ejecución volvió a estar activa")
            continue
        gh(f"repos/{repo}/actions/artifacts/{artifact['id']}", "--method", "DELETE")
        removed.append(artifact)

    def mib(items):
        return sum(a["size_in_bytes"] for a in items) / 1048576

    summary = (f"## Limpieza de artefactos\n\n"
               f"Modo: {'aplicar' if args.apply else 'solo vista previa'}.\n\n"
               f"Se conservan {len(keep)} artefactos completados y todos los de ejecuciones activas.\n"
               f"Candidatos: {len(candidates)} ({mib(candidates):.2f} MiB).\n"
               f"Eliminados: {len(removed)} ({mib(removed):.2f} MiB).\n")
    print(summary)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a") as output:
            output.write(summary)


if __name__ == "__main__":
    main()
