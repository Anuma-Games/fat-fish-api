import unittest
from prune_artifacts import plan_cleanup

POLICY = {"build": {"keep": 1, "build": True}, "report": {"keep": 2}}


def artifact(identifier, name="build", **extra):
    return dict(id=identifier, name=name, created_at=f"2026-11-{identifier:02d}T00:00:00Z",
                workflow_run={"id": identifier}, size_in_bytes=1024, **extra)


def run(status="completed", conclusion="success", event="push"):
    return dict(status=status, conclusion=conclusion, event=event)


class CleanupTests(unittest.TestCase):
    def test_preserves_latest_successful_build_and_in_progress_consumer(self):
        rows = [artifact(i) for i in range(1, 6)]
        runs = {1: run(), 2: run(), 3: run(conclusion="failure"),
                4: run(event="pull_request"), 5: run(status="in_progress")}
        keep, delete = plan_cleanup(rows, runs, POLICY)
        self.assertEqual([a["id"] for a in keep], [2])
        self.assertEqual({a["id"] for a in delete}, {1, 3, 4})

    def test_keeps_two_reports_including_failed_tests(self):
        rows = [artifact(i, "report") for i in range(1, 4)]
        keep, delete = plan_cleanup(rows, {i: run(conclusion="failure") for i in range(1, 4)}, POLICY)
        self.assertEqual([a["id"] for a in keep], [3, 2])
        self.assertEqual([a["id"] for a in delete], [1])

    def test_preserves_unknown_expired_and_unverifiable_artifacts(self):
        rows = [artifact(1, "other"), artifact(2, expired=True), artifact(3)]
        keep, delete = plan_cleanup(rows, {1: run(), 2: run()}, POLICY)
        self.assertEqual(keep, [])
        self.assertEqual(delete, [])

    def test_fallback_keeps_latest_branch_build_if_all_runs_failed(self):
        keep, delete = plan_cleanup([artifact(1), artifact(2)],
                                    {1: run(conclusion="failure"), 2: run(conclusion="cancelled")}, POLICY)
        self.assertEqual([a["id"] for a in keep], [2])
        self.assertEqual([a["id"] for a in delete], [1])

    def test_completed_pr_transfer_artifact_is_disposable(self):
        keep, delete = plan_cleanup([artifact(1)], {1: run(event="pull_request")}, POLICY)
        self.assertEqual(keep, [])
        self.assertEqual([a["id"] for a in delete], [1])

    def test_latest_build_is_not_deleted(self):
        keep, delete = plan_cleanup([artifact(1)], {1: run()}, POLICY)
        self.assertEqual(len(keep), 1)
        self.assertEqual(delete, [])

    def test_invalid_keep_count_cannot_produce_a_deletion_plan(self):
        with self.assertRaises(ValueError):
            plan_cleanup([artifact(1)], {1: run()}, {"build": {"keep": 0}})


if __name__ == "__main__":
    unittest.main()
