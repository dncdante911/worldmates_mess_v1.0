#!/usr/bin/env python3
"""
Аналіз та видалення дублікатів таблиць в базі даних
Знаходить таблиці з маленькими літерами (wo_*), які мають відповідники з великими (Wo_*)
і генерує SQL скрипт для видалення старих/порожніх таблиць
"""

import re
import sys
from collections import defaultdict

def analyze_structure_file(filename):
    """Читає структуру БД і знаходить всі таблиці"""
    tables = []

    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()

    # Знаходимо всі CREATE TABLE
    pattern = r'CREATE TABLE `([^`]+)`'
    matches = re.findall(pattern, content)

    return matches

def find_duplicates(tables):
    """Знаходить дублікати таблиць (різний регістр)"""
    table_groups = defaultdict(list)

    for table in tables:
        # Групуємо за lowercase назвою
        lowercase = table.lower()
        table_groups[lowercase].append(table)

    # Знаходимо тільки ті групи, де є дублікати
    duplicates = {}
    for lowercase, variants in table_groups.items():
        if len(variants) > 1:
            duplicates[lowercase] = variants

    return duplicates

def determine_primary_table(variants):
    """Визначає основну таблицю (з маленьких літер wo_)"""
    # ВИПРАВЛЕНО: Пріоритет wo_* (маленькі) > Wo_* (великі)
    # Причина: в коді проекту використовуються таблиці з маленьких літер!
    for variant in variants:
        if variant.startswith('wo_'):
            return variant

    # Якщо немає wo_*, повертаємо першу
    return variants[0]

def analyze_table_usage(table_name, sql_files):
    """Перевіряє, чи використовується таблиця в коді (приблизно)"""
    usage_count = 0

    for sql_file in sql_files:
        try:
            with open(sql_file, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
                # Рахуємо згадки таблиці (в INSERT, UPDATE, SELECT тощо)
                usage_count += len(re.findall(rf'`{table_name}`|"{table_name}"|\'{table_name}\'', content))
        except:
            pass

    return usage_count

def generate_cleanup_sql(duplicates, structure_file):
    """Генерує SQL скрипт для видалення непотрібних таблиць"""

    sql_lines = []
    sql_lines.append("-- ============================================")
    sql_lines.append("-- Скрипт видалення дублікатів таблиць")
    sql_lines.append("-- Створено автоматично")
    sql_lines.append("-- ============================================")
    sql_lines.append("")
    sql_lines.append("-- ВАЖЛИВО: Перед виконанням створіть бекап БД!")
    sql_lines.append("-- mysqldump -u social -p socialhub > backup_$(date +%Y%m%d_%H%M%S).sql")
    sql_lines.append("")
    sql_lines.append("USE socialhub;")
    sql_lines.append("")

    tables_to_drop = []

    for lowercase, variants in sorted(duplicates.items()):
        primary = determine_primary_table(variants)
        to_remove = [v for v in variants if v != primary]

        sql_lines.append(f"-- Група: {lowercase}")
        sql_lines.append(f"-- ✅ Залишаємо: {primary}")

        for table in to_remove:
            sql_lines.append(f"-- ❌ Видаляємо: {table}")
            tables_to_drop.append(table)

        sql_lines.append("")

    # Генеруємо DROP TABLE команди
    sql_lines.append("-- ============================================")
    sql_lines.append("-- DROP TABLE команди")
    sql_lines.append("-- ============================================")
    sql_lines.append("")

    for table in sorted(tables_to_drop):
        sql_lines.append(f"DROP TABLE IF EXISTS `{table}`;")

    sql_lines.append("")
    sql_lines.append(f"-- Всього таблиць для видалення: {len(tables_to_drop)}")
    sql_lines.append("")

    return "\n".join(sql_lines), tables_to_drop

def main():
    structure_file = 'socialhub-01-structure.sql'
    output_file = 'cleanup_duplicates.sql'
    report_file = 'duplicates_report.txt'

    print("📊 Аналіз структури бази даних...")
    tables = analyze_structure_file(structure_file)
    print(f"   Знайдено таблиць: {len(tables)}")

    print("\n🔍 Пошук дублікатів...")
    duplicates = find_duplicates(tables)
    print(f"   Знайдено груп дублікатів: {len(duplicates)}")

    # Створюємо звіт
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write("=" * 80 + "\n")
        f.write("ЗВІТ ПРО ДУБЛІКАТИ ТАБЛИЦЬ\n")
        f.write("=" * 80 + "\n\n")

        for lowercase, variants in sorted(duplicates.items()):
            primary = determine_primary_table(variants)
            f.write(f"Група: {lowercase}\n")
            f.write(f"  Варіанти:\n")
            for v in variants:
                marker = "✅ ОСНОВНА" if v == primary else "❌ ВИДАЛИТИ"
                f.write(f"    - {v} [{marker}]\n")
            f.write("\n")

        f.write(f"\nВсього груп дублікатів: {len(duplicates)}\n")

    print(f"\n📝 Створено звіт: {report_file}")

    # Генеруємо SQL
    print(f"\n🔧 Генерація SQL скрипта...")
    sql_content, tables_to_drop = generate_cleanup_sql(duplicates, structure_file)

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(sql_content)

    print(f"   ✅ Створено: {output_file}")
    print(f"   📊 Таблиць до видалення: {len(tables_to_drop)}")

    print("\n" + "=" * 80)
    print("ГОТОВО!")
    print("=" * 80)
    print(f"\n📄 Файли створено:")
    print(f"   1. {report_file} - детальний звіт")
    print(f"   2. {output_file} - SQL скрипт для видалення")
    print(f"\n⚠️  ВАЖЛИВО:")
    print(f"   1. Перегляньте {report_file}")
    print(f"   2. Створіть бекап БД перед виконанням!")
    print(f"   3. Виконайте: mysql -u social -p socialhub < {output_file}")
    print("")

if __name__ == '__main__':
    main()
