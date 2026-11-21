import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import publish_latest_build as publisher


class PublishTests(unittest.TestCase):
    def test_failed_upload_never_deletes_previous_release(self):
        with patch.object(publisher, 'is_current', return_value=True), \
             patch.object(publisher, 'gh', side_effect=RuntimeError('upload failed')) as gh:
            with self.assertRaises(RuntimeError):
                publisher.publish('org/repo', 'sha', '20', '1', Path('app.apk'))
        self.assertEqual(len(gh.call_args_list), 1)
        self.assertEqual(gh.call_args.args[1:3], ('release', 'create'))

    def test_failed_publication_never_deletes_previous_release(self):
        def execute(repo, *args):
            if args[:2] == ('release', 'edit'):
                raise RuntimeError('publish failed')
            return ''
        with patch.object(publisher, 'is_current', return_value=True), \
             patch.object(publisher, 'gh', side_effect=execute) as gh:
            with self.assertRaises(RuntimeError):
                publisher.publish('org/repo', 'sha', '20', '1', Path('app.apk'))
        self.assertFalse(any(c.args[1:3] == ('release', 'delete') for c in gh.call_args_list))

    def test_old_commit_never_creates_or_deletes_a_release(self):
        with patch.object(publisher, 'is_current', return_value=False), \
             patch.object(publisher, 'gh') as gh:
            publisher.publish('org/repo', 'old-sha', '20', '1', Path('app.apk'))
        gh.assert_not_called()

    def test_changed_main_during_upload_removes_only_its_draft(self):
        with patch.object(publisher, 'is_current', side_effect=[True, False]), \
             patch.object(publisher, 'gh') as gh:
            publisher.publish('org/repo', 'sha', '20', '1', Path('app.apk'))
        self.assertEqual([c.args[2] for c in gh.call_args_list], ['create', 'delete'])
        self.assertEqual(gh.call_args.args[3], 'build-main-20-1')

    def test_cleanup_only_removes_older_managed_releases_after_publication(self):
        releases = [{'tag_name': tag} for tag in ['v1.0', 'build-main-19-1', 'build-main-20-1',
                                                 'build-main-20-2', 'build-main-21-1', 'build-main-manual']]
        def execute(repo, *args):
            return json.dumps([releases]) if args[0] == 'api' else ''
        with patch.object(publisher, 'is_current', return_value=True), \
             patch.object(publisher, 'gh', side_effect=execute) as gh:
            publisher.publish('org/repo', 'sha', '20', '2', Path('app.apk'))
        calls = gh.call_args_list
        self.assertEqual(calls[1].args[2], 'edit')
        deleted = [c.args[3] for c in calls if c.args[1:3] == ('release', 'delete')]
        self.assertEqual(deleted, ['build-main-19-1', 'build-main-20-1'])

    def test_zip_contains_complete_build_at_archive_root(self):
        with tempfile.TemporaryDirectory() as tmp:
            source = Path(tmp) / 'input'
            (source / 'FatFish_Data').mkdir(parents=True)
            (source / 'FatFish.exe').write_bytes(b'exe')
            (source / 'FatFish_Data' / 'data').write_bytes(b'data')
            with patch.object(publisher, 'Path', wraps=Path) as path:
                path.side_effect = lambda value: Path(tmp) / value if value == 'release-output' else Path(value)
                asset = publisher.prepare_asset(source, 'game.zip', True)
            with zipfile.ZipFile(asset) as archive:
                self.assertEqual(archive.read('FatFish.exe'), b'exe')
                self.assertEqual(archive.read('FatFish_Data/data'), b'data')

    def test_missing_or_ambiguous_binaries_fail_before_publication(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(ValueError):
                publisher.prepare_asset(tmp, 'app.apk', False)
            (Path(tmp) / 'first.jar').write_bytes(b'jar')
            with patch.object(publisher, 'Path', wraps=Path) as path:
                path.side_effect = lambda value: Path(tmp) / value if value == 'release-output' else Path(value)
                with self.assertRaises(ValueError):
                    publisher.prepare_asset(tmp, 'app.apk', False)


if __name__ == '__main__':
    unittest.main()
