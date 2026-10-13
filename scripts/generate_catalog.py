#!/usr/bin/env python3
"""Generate an allowlisted GitHub plugin index from plugins/{system,app}/*.json.

Usage: python scripts/generate_catalog.py          # update plugins/index.json
       python scripts/generate_catalog.py --check  # fail unless already current
No third-party packages and no network access required.
"""
import json
import re
import sys
from pathlib import Path

BASE = Path(__file__).resolve().parents[1]
ROOT = BASE / 'plugins'
ALLOWED = {
    'system': ('System', {'storage','apps','display','network','mouse'}),
    'app': ('App', {'dark','compact','diagnostics','scanner','roblox'}),
}
ID_RE = re.compile(r'^[a-z0-9._-]{1,40}$')


def main() -> int:
    plugins, seen = [], set()
    for folder, (category, actions) in ALLOWED.items():
        for file in sorted((ROOT / folder).glob('*.json')):
            if not ID_RE.fullmatch(file.stem):
                raise ValueError(f'Invalid plugin filename: {file.name}')
            if file.stat().st_size > 12 * 1024:
                raise ValueError(f'Plugin too large: {file.name}')
            data = json.loads(file.read_text(encoding='utf-8'))
            if not isinstance(data, dict):
                raise ValueError(f'Expected a JSON object in {file}')
            required = {'schema','id','name','description','category','action','version'}
            if not required <= data.keys():
                raise ValueError(f'Missing fields: {file}')
            if data['schema'] != 1 or data['category'] != category or data['action'] not in actions:
                raise ValueError(f'Unsafe category/action/schema: {file}')
            if not isinstance(data['id'], str) or not ID_RE.fullmatch(data['id']):
                raise ValueError(f'Invalid plugin ID: {file}')
            if not isinstance(data['name'], str) or not 1 <= len(data['name']) <= 64:
                raise ValueError(f'Invalid plugin name: {file}')
            if not isinstance(data['description'], str) or len(data['description']) > 160:
                raise ValueError(f'Invalid plugin description: {file}')
            if type(data['version']) is not int or not 1 <= data['version'] <= 100000:
                raise ValueError(f'Invalid plugin version: {file}')
            if data['id'] in seen:
                raise ValueError(f'Duplicate plugin ID: {data["id"]}')
            seen.add(data['id'])
            plugins.append({key: data[key] for key in (
                'schema', 'id', 'name', 'description', 'category', 'action', 'version')}
                | {'path': f'{folder}/{file.name}'})
    if len(plugins) > 20:
        raise ValueError('Catalog can contain at most 20 plugins for low-RAM boxes')
    catalog = json.dumps({'schema': 1, 'plugins': plugins}, ensure_ascii=False, indent=2) + '\n'
    path = ROOT / 'index.json'
    previous = path.read_text(encoding='utf-8') if path.exists() else ''
    if '--check' in sys.argv:
        if catalog != previous:
            print('Catalog is stale. Run: python scripts/generate_catalog.py')
            return 1
        print(f'Catalog is current ({len(plugins)} plugins)')
        return 0
    path.write_text(catalog, encoding='utf-8')
    print(f'Updated {path.relative_to(BASE)}: {len(plugins)} plugins')
    return 0


if __name__ == '__main__':
    try:
        raise SystemExit(main())
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print(f'Catalog validation failed: {exc}', file=sys.stderr)
        raise SystemExit(1)
