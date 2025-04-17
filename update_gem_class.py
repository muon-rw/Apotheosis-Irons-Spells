import json
import os
import glob

# Configuration
GEM_DIR = 'src/main/resources/data/irons_apothic/gems' # Adjust if your path differs

def validate_gem_class(data, filename):
    """Validates the gem_class structure within a gem file's data."""
    is_valid = True
    if not isinstance(data, dict):
        print(f"WARN: {filename} - Root is not a JSON object.")
        return False # Cannot process non-dict root

    bonuses = data.get('bonuses')
    if not isinstance(bonuses, list):
        # Not all gems might have bonuses, or the structure might be different
        # print(f"INFO: {filename} - No 'bonuses' list found or is not a list.")
        return True # Assume valid if no bonuses to check

    for idx, bonus in enumerate(bonuses):
        if not isinstance(bonus, dict):
            print(f"WARN: {filename} - Bonus at index {idx} is not an object.")
            is_valid = False
            continue

        gem_class = bonus.get('gem_class')
        if gem_class is None:
            # It's possible some bonus types don't require a gem_class
            # print(f"INFO: {filename} - Bonus at index {idx} has no 'gem_class'.")
            continue

        if isinstance(gem_class, str):
            # Simple string format, assumed valid by the parser if the category exists
            if not gem_class.strip():
                 print(f"ERROR: {filename} - Bonus at index {idx} has an empty string 'gem_class'.")
                 is_valid = False
            continue

        if isinstance(gem_class, dict):
            # Explicit object format
            key = gem_class.get('key')
            types = gem_class.get('types')

            if not isinstance(key, str) or not key.strip():
                print(f"ERROR: {filename} - Bonus at index {idx} has invalid or missing 'key' in 'gem_class' object.")
                is_valid = False

            if not isinstance(types, list):
                print(f"ERROR: {filename} - Bonus at index {idx} has missing or non-list 'types' in 'gem_class' object.")
                is_valid = False
            elif not types:
                print(f"ERROR: {filename} - Bonus at index {idx} has empty 'types' list in 'gem_class' object.")
                is_valid = False
            else:
                 # Optional: Check if all types are non-empty strings
                 for type_idx, type_entry in enumerate(types):
                     if not isinstance(type_entry, str) or not type_entry.strip():
                          print(f"ERROR: {filename} - Bonus at index {idx}, 'gem_class' object, 'types' list contains invalid/empty entry at index {type_idx}.")
                          is_valid = False
            continue

        # If gem_class is neither string nor dict
        print(f"ERROR: {filename} - Bonus at index {idx} has 'gem_class' with unexpected type: {type(gem_class)}.")
        is_valid = False

    return is_valid

# --- Main Execution ---
processed_files = 0
invalid_files = []

# Recursively find all .json files in the GEM_DIR
gem_files = glob.glob(os.path.join(GEM_DIR, '**/*.json'), recursive=True)

if not gem_files:
    print(f"No JSON files found in {GEM_DIR}")
else:
    print(f"Found {len(gem_files)} potential gem files. Validating gem_class structure...")
    for file_path in gem_files:
        relative_path = os.path.relpath(file_path, '.') # Get path relative to workspace root
        processed_files += 1
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            if not validate_gem_class(data, relative_path):
                invalid_files.append(relative_path)
        except json.JSONDecodeError as e:
            print(f"ERROR: Failed to decode JSON in {relative_path}: {e}")
            invalid_files.append(relative_path)
        except Exception as e:
            print(f"ERROR: Unexpected error processing {relative_path}: {e}")
            invalid_files.append(relative_path)

    print("\n--- Validation Summary ---")
    print(f"Total files checked: {processed_files}")
    if invalid_files:
        print(f"Files with validation errors ({len(invalid_files)}):")
        for file in invalid_files:
            print(f"- {file}")
    else:
        print("All checked files seem to have a valid gem_class structure.")
