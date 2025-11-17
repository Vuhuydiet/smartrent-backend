import os
from math import ceil

INFILE = r"d:\DEV\datn\smartrent-backend\smart-rent\src\main\resources\db\migration\V35__Migrate_Street_Mapping.sql"
OUTDIR = os.path.dirname(INFILE)
PART_PREFIX = "V35_Migrate_Street_Mapping_Part_"
PART_COUNT = 10


def split_sql_file():
    with open(INFILE, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # Find first INSERT line index
    insert_idx = None
    for i, ln in enumerate(lines):
        if 'INSERT INTO' in ln.upper():
            insert_idx = i
            break

    if insert_idx is None:
        print('No INSERT statements found in file; aborting')
        return 1

    header = lines[:insert_idx]
    body_lines = lines[insert_idx:]

    # Collect statements (group lines until a line ending with ');')
    stmts = []
    cur = []
    for ln in body_lines:
        cur.append(ln)
        if ln.strip().endswith(');'):
            stmts.append(''.join(cur))
            cur = []
    # catch any trailing
    if cur:
        stmts.append(''.join(cur))

    total = len(stmts)
    per_part = ceil(total / PART_COUNT)

    created = []
    for part in range(PART_COUNT):
        start = part * per_part
        end = start + per_part
        chunk = stmts[start:end]
        part_name = f"{PART_PREFIX}{part+1}.sql"
        out_path = os.path.join(OUTDIR, part_name)
        with open(out_path, 'w', encoding='utf-8') as out:
            # write header
            for h in header:
                out.write(h)
            out.write('\n')
            if chunk:
                for s in chunk:
                    out.write(s)
                    if not s.endswith('\n'):
                        out.write('\n')
            else:
                out.write('-- (no statements in this part)\n')
        created.append((out_path, len(chunk)))

    # Print summary
    print('Split complete. Created parts:')
    for p, n in created:
        print(f' - {p}: {n} statements')
    print(f'Total statements: {total}; per part approx: {per_part}')
    return 0


if __name__ == '__main__':
    exit(split_sql_file())
