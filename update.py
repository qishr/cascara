#!/usr/bin/env python3
import sys
import os
import argparse
import re

def main():
    parser = argparse.ArgumentParser(
        description="Update key-value variables in gradle.properties files recursively."
    )
    parser.add_argument("var_name", help="Name of the property variable (e.g., cascara_version)")
    parser.add_argument("old_value", help="Expected old value (e.g., 0.1.12)")
    parser.add_argument("new_value", help="New value to set (e.g., 0.1.13)")
    parser.add_argument("search_path", help="Directory path to recursively search for gradle.properties")

    parser.add_argument(
        "-f", "--force", action="store_true",
        help="Update matching key regardless of current value (ignores old_value match check)"
    )
    parser.add_argument(
        "-l", "--list", action="store_true",
        help="List file paths that are updated or would be updated"
    )
    parser.add_argument(
        "-d", "--dry-run", action="store_true",
        help="Simulate the changes without modifying files on disk"
    )

    args = parser.parse_args()

    search_dir = os.path.abspath(args.search_path)
    if not os.path.exists(search_dir):
        print(f"Error: Path '{args.search_path}' does not exist.", file=sys.stderr)
        sys.exit(1)

    # Regex matching key=value while preserving original spacing around '='
    # Match group 1: key, group 2: assignment spacing, group 3: value
    pattern = re.compile(rf"^(\s*{re.escape(args.var_name)}\s*=\s*)(.*)$")

    updated_files_count = 0

    for root, _, files in os.walk(search_dir):
        if "gradle.properties" in files:
            file_path = os.path.join(root, "gradle.properties")

            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    lines = f.readlines()
            except Exception as e:
                print(f"Warning: Could not read {file_path}: {e}", file=sys.stderr)
                continue

            file_modified = False
            new_lines = []

            for line in lines:
                match = pattern.match(line)
                if match:
                    prefix = match.group(1)
                    current_value = match.group(2).strip()

                    # Check if target condition is satisfied
                    if args.force or current_value == args.old_value:
                        if current_value != args.new_value:
                            # Construct updated line retaining trailing comments or newline if applicable
                            line = f"{prefix}{args.new_value}\n"
                            file_modified = True

                new_lines.append(line)

            if file_modified:
                updated_files_count += 1
                if args.list:
                    action_prefix = "[DRY-RUN] Would update:" if args.dry_run else "Updated:"
                    print(f"{action_prefix} {file_path}")

                if not args.dry_run:
                    with open(file_path, "w", encoding="utf-8") as f:
                        f.writelines(new_lines)

    if args.dry_run:
        print(f"\nDry run complete. {updated_files_count} file(s) would be updated.")
    else:
        print(f"\nFinished. Updated {updated_files_count} file(s).")

if __name__ == "__main__":
    main()