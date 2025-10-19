# TinhThanhPho.com Data Import Guide

This guide explains how to import Vietnamese administrative location data from [tinhthanhpho.com](https://tinhthanhpho.com) into your local database.

## Overview

You will import **~11,700+ location records**:
- **63 Provinces** (Tỉnh/Thành phố)
- **~700 Districts** (Quận/Huyện)
- **~11,000 Wards** (Phường/Xã)

## Prerequisites

- Node.js (for data fetching script) OR
- Python 3 (alternative) OR
- Any HTTP client (Postman, curl, etc.)
- Database access

## Method 1: Automated Script (Recommended)

### Step 1: Create Data Fetcher Script

Create `scripts/fetch-tinhthanhpho-data.js`:

```javascript
const fs = require('fs');
const https = require('https');

const BASE_URL = 'https://tinhthanhpho.com/api';

// Fetch JSON from URL
function fetchJson(url) {
    return new Promise((resolve, reject) => {
        https.get(url, (res) => {
            let data = '';
            res.on('data', (chunk) => { data += chunk; });
            res.on('end', () => {
                try {
                    resolve(JSON.parse(data));
                } catch (e) {
                    reject(e);
                }
            });
        }).on('error', reject);
    });
}

// Determine province type from name
function getProvinceType(name) {
    if (name.startsWith('Thành phố') || name.startsWith('TP.')) {
        return 'CITY';
    }
    return 'PROVINCE';
}

// Determine district type from name
function getDistrictType(name) {
    if (name.startsWith('Quận')) return 'DISTRICT';
    if (name.startsWith('Thành phố') || name.startsWith('TP.')) return 'CITY';
    if (name.startsWith('Thị xã')) return 'TOWN';
    return 'TOWN'; // Default for "Huyện"
}

// Determine ward type from name
function getWardType(name) {
    if (name.startsWith('Phường')) return 'WARD';
    if (name.startsWith('Thị trấn')) return 'TOWNSHIP';
    return 'COMMUNE'; // Default for "Xã"
}

// Generate SQL INSERT statements
async function generateSQL() {
    console.log('Fetching provinces...');
    const provinces = await fetchJson(`${BASE_URL}/tinh-thanh-pho`);

    let sql = '-- Generated SQL from tinhthanhpho.com\n\n';
    sql += '-- =====================================================================\n';
    sql += '-- PROVINCES\n';
    sql += '-- =====================================================================\n';

    sql += 'INSERT INTO provinces (name, code, type, is_active, effective_from, is_merged) VALUES\n';

    const provinceValues = provinces.map(p =>
        `('${p.name.replace(/'/g, "''")}', '${p.code}', '${getProvinceType(p.name)}', true, CURRENT_DATE, false)`
    );
    sql += provinceValues.join(',\n');
    sql += '\nON DUPLICATE KEY UPDATE name = VALUES(name);\n\n';

    console.log(`✓ ${provinces.length} provinces fetched`);

    // Fetch districts for each province
    sql += '-- =====================================================================\n';
    sql += '-- DISTRICTS\n';
    sql += '-- =====================================================================\n';

    let districtCount = 0;
    const districtInserts = [];

    for (const province of provinces) {
        console.log(`Fetching districts for ${province.name}...`);
        const districts = await fetchJson(`${BASE_URL}/quan-huyen/tinh-thanh-pho/${province.id}`);

        for (const d of districts) {
            districtInserts.push(
                `SELECT '${d.name.replace(/'/g, "''")}', '${d.code}', '${getDistrictType(d.name)}', province_id, true, CURRENT_DATE FROM provinces WHERE code = '${province.code}'`
            );
            districtCount++;
        }

        // Add small delay to avoid rate limiting
        await new Promise(resolve => setTimeout(resolve, 100));
    }

    sql += 'INSERT INTO districts (name, code, type, province_id, is_active, effective_from)\n';
    sql += districtInserts.join(' UNION ALL\n');
    sql += '\nON DUPLICATE KEY UPDATE name = VALUES(name);\n\n';

    console.log(`✓ ${districtCount} districts fetched`);

    // Fetch wards for each district
    sql += '-- =====================================================================\n';
    sql += '-- WARDS\n';
    sql += '-- =====================================================================\n';

    let wardCount = 0;
    const wardInserts = [];

    for (const province of provinces) {
        const districts = await fetchJson(`${BASE_URL}/quan-huyen/tinh-thanh-pho/${province.id}`);

        for (const district of districts) {
            console.log(`Fetching wards for ${district.name}...`);
            const wards = await fetchJson(`${BASE_URL}/phuong-xa/quan-huyen/${district.id}`);

            for (const w of wards) {
                wardInserts.push(
                    `SELECT '${w.name.replace(/'/g, "''")}', '${w.code}', '${getWardType(w.name)}', district_id, true, CURRENT_DATE FROM districts WHERE code = '${district.code}'`
                );
                wardCount++;
            }

            // Add small delay to avoid rate limiting
            await new Promise(resolve => setTimeout(resolve, 100));
        }
    }

    sql += 'INSERT INTO wards (name, code, type, district_id, is_active, effective_from)\n';
    sql += wardInserts.join(' UNION ALL\n');
    sql += '\nON DUPLICATE KEY UPDATE name = VALUES(name);\n\n';

    console.log(`✓ ${wardCount} wards fetched`);

    // Add verification queries
    sql += '-- =====================================================================\n';
    sql += '-- VERIFICATION\n';
    sql += '-- =====================================================================\n';
    sql += 'SELECT COUNT(*) as total_provinces FROM provinces WHERE is_active = true;\n';
    sql += 'SELECT COUNT(*) as total_districts FROM districts WHERE is_active = true;\n';
    sql += 'SELECT COUNT(*) as total_wards FROM wards WHERE is_active = true;\n';

    return sql;
}

// Main execution
(async () => {
    try {
        console.log('Starting data fetch from tinhthanhpho.com...\n');
        const sql = await generateSQL();

        const outputFile = 'V18__Import_tinhthanhpho_location_data_GENERATED.sql';
        fs.writeFileSync(outputFile, sql, 'utf8');

        console.log(`\n✓ SQL generated successfully!`);
        console.log(`\n📄 Output file: ${outputFile}`);
        console.log(`\nNext steps:`);
        console.log(`1. Review the generated SQL file`);
        console.log(`2. Copy it to: src/main/resources/db/migration/`);
        console.log(`3. Run: ./gradlew flywayMigrate`);
    } catch (error) {
        console.error('Error:', error);
        process.exit(1);
    }
})();
```

### Step 2: Run the Script

```bash
# Run the script
node scripts/fetch-tinhthanhpho-data.js

# This will generate: V18__Import_tinhthanhpho_location_data_GENERATED.sql
```

### Step 3: Apply Migration

```bash
# Copy generated file to migrations folder
cp V18__Import_tinhthanhpho_location_data_GENERATED.sql \
   src/main/resources/db/migration/V18__Import_tinhthanhpho_location_data.sql

# Run migration
./gradlew flywayMigrate
```

## Method 2: Python Script

Create `scripts/fetch_tinhthanhpho_data.py`:

```python
import requests
import time

BASE_URL = 'https://tinhthanhpho.com/api'

def get_province_type(name):
    return 'CITY' if name.startswith(('Thành phố', 'TP.')) else 'PROVINCE'

def get_district_type(name):
    if name.startswith('Quận'):
        return 'DISTRICT'
    elif name.startswith(('Thành phố', 'TP.')):
        return 'CITY'
    elif name.startswith('Thị xã'):
        return 'TOWN'
    return 'TOWN'

def get_ward_type(name):
    if name.startswith('Phường'):
        return 'WARD'
    elif name.startswith('Thị trấn'):
        return 'TOWNSHIP'
    return 'COMMUNE'

def escape_sql(text):
    return text.replace("'", "''")

def main():
    print("Fetching data from tinhthanhpho.com...\n")

    # Fetch provinces
    provinces = requests.get(f'{BASE_URL}/tinh-thanh-pho').json()
    print(f"✓ {len(provinces)} provinces fetched")

    with open('V18__Import_tinhthanhpho_location_data_GENERATED.sql', 'w', encoding='utf-8') as f:
        f.write('-- Generated SQL from tinhthanhpho.com\n\n')

        # Write provinces
        f.write('-- PROVINCES\n')
        f.write('INSERT INTO provinces (name, code, type, is_active, effective_from, is_merged) VALUES\n')
        province_values = [
            f"('{escape_sql(p['name'])}', '{p['code']}', '{get_province_type(p['name'])}', true, CURRENT_DATE, false)"
            for p in provinces
        ]
        f.write(',\n'.join(province_values))
        f.write('\nON DUPLICATE KEY UPDATE name = VALUES(name);\n\n')

        # Write districts
        district_count = 0
        f.write('-- DISTRICTS\n')
        f.write('INSERT INTO districts (name, code, type, province_id, is_active, effective_from)\n')
        district_inserts = []

        for province in provinces:
            districts = requests.get(f"{BASE_URL}/quan-huyen/tinh-thanh-pho/{province['id']}").json()
            for d in districts:
                district_inserts.append(
                    f"SELECT '{escape_sql(d['name'])}', '{d['code']}', '{get_district_type(d['name'])}', province_id, true, CURRENT_DATE FROM provinces WHERE code = '{province['code']}'"
                )
                district_count += 1
            time.sleep(0.1)  # Avoid rate limiting

        f.write(' UNION ALL\n'.join(district_inserts))
        f.write('\nON DUPLICATE KEY UPDATE name = VALUES(name);\n\n')
        print(f"✓ {district_count} districts fetched")

        # Write wards
        ward_count = 0
        f.write('-- WARDS\n')
        f.write('INSERT INTO wards (name, code, type, district_id, is_active, effective_from)\n')
        ward_inserts = []

        for province in provinces:
            districts = requests.get(f"{BASE_URL}/quan-huyen/tinh-thanh-pho/{province['id']}").json()
            for district in districts:
                wards = requests.get(f"{BASE_URL}/phuong-xa/quan-huyen/{district['id']}").json()
                for w in wards:
                    ward_inserts.append(
                        f"SELECT '{escape_sql(w['name'])}', '{w['code']}', '{get_ward_type(w['name'])}', district_id, true, CURRENT_DATE FROM districts WHERE code = '{district['code']}'"
                    )
                    ward_count += 1
                time.sleep(0.1)

        f.write(' UNION ALL\n'.join(ward_inserts))
        f.write('\nON DUPLICATE KEY UPDATE name = VALUES(name);\n')
        print(f"✓ {ward_count} wards fetched")

    print(f"\n✓ SQL generated successfully!")
    print(f"📄 Output: V18__Import_tinhthanhpho_location_data_GENERATED.sql")

if __name__ == '__main__':
    main()
```

Run it:
```bash
python3 scripts/fetch_tinhthanhpho_data.py
```

## Method 3: Manual Import

If you prefer manual work:

1. Visit API endpoints in browser:
   - https://tinhthanhpho.com/api/tinh-thanh-pho
   - https://tinhthanhpho.com/api/quan-huyen/tinh-thanh-pho/01
   - https://tinhthanhpho.com/api/phuong-xa/quan-huyen/001

2. Copy JSON responses

3. Use online JSON-to-SQL converters or manually write INSERT statements

4. Follow the template in `V18__Import_tinhthanhpho_location_data.sql`

## Verification

After running the migration, verify the data:

```sql
-- Check counts
SELECT
    (SELECT COUNT(*) FROM provinces WHERE is_active = true) as provinces,
    (SELECT COUNT(*) FROM districts WHERE is_active = true) as districts,
    (SELECT COUNT(*) FROM wards WHERE is_active = true) as wards;

-- Expected results:
-- provinces: 63
-- districts: ~700
-- wards: ~11,000

-- Check province details
SELECT p.name, COUNT(d.district_id) as district_count
FROM provinces p
LEFT JOIN districts d ON p.province_id = d.province_id
WHERE p.is_active = true
GROUP BY p.province_id
ORDER BY p.name;

-- Check specific locations
SELECT
    p.name as province,
    d.name as district,
    w.name as ward
FROM wards w
JOIN districts d ON w.district_id = d.district_id
JOIN provinces p ON d.province_id = p.province_id
WHERE p.code = '01'  -- Hanoi
LIMIT 20;
```

## API Testing

After import, test your API endpoints:

```bash
# Get all provinces
curl http://localhost:8080/v1/addresses/provinces

# Get districts by province
curl http://localhost:8080/v1/addresses/provinces/1/districts

# Get wards by district
curl http://localhost:8080/v1/addresses/districts/1/wards

# Search provinces
curl "http://localhost:8080/v1/addresses/provinces/search?q=Hà Nội"
```

## Troubleshooting

### Rate Limiting

If you encounter rate limiting:
- Increase delay in the script: `setTimeout(resolve, 500)` (500ms)
- Run script multiple times for different provinces
- Use VPN or change IP

### SQL Errors

If INSERT fails:
- Check for duplicate codes
- Verify foreign key relationships
- Ensure `is_active` and `effective_from` have values

### Character Encoding

Ensure UTF-8 encoding:
```sql
ALTER TABLE provinces CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE districts CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE wards CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## Performance Tips

1. **Disable indexing temporarily** during bulk insert:
   ```sql
   ALTER TABLE districts DISABLE KEYS;
   ALTER TABLE wards DISABLE KEYS;
   -- Run inserts
   ALTER TABLE districts ENABLE KEYS;
   ALTER TABLE wards ENABLE KEYS;
   ```

2. **Use batch inserts** (already done in scripts)

3. **Run during off-peak hours** if in production

## Next Steps

After successful import:

1. Test address selection in your application
2. Create listings with proper addresses
3. Implement address autocomplete in frontend
4. Add street data (if needed)
5. Set up periodic updates for administrative changes

## Support

- tinhthanhpho.com API docs: https://tinhthanhpho.com/api-docs
- Issues: Check application logs
- Data issues: Verify against official GSO (General Statistics Office) data