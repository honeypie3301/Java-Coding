import re

def update_rot_ai(fname):
    with open(fname, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Remove WITHER_QUOTES and TOTEM_QUOTES arrays
    content = re.sub(r'	*private static final String\[\] WITHER_QUOTES = \{.*?
	*\};
*', '', content, flags=re.DOTALL)
    content = re.sub(r'	*private static final String\[\] TOTEM_QUOTES = \{.*?
	*\};
*', '', content, flags=re.DOTALL)

    # 2. Replace infinity totem and regular totem quotes
    content = content.replace(
        'p.displayClientMessage(Component.literal("Prepare thyself."), true);',
        'RotDialoguesProcedure.sendInfinityTotemQuote(p);'
    )
    content = re.sub(
        r'String quote = TOTEM_QUOTES\[RandomSource\.create\(\)\.nextInt\(TOTEM_QUOTES\.length\)\];\s*p\.displayClientMessage\(Component\.literal\(quote\), true\);',
        'RotDialoguesProcedure.sendRandomTotemQuote(p);',
        content
    )

    # 3. Replace Wither quotes
    content = re.sub(
        r'String quote = WITHER_QUOTES\[RandomSource\.create\(\)\.nextInt\(WITHER_QUOTES\.length\)\];\s*sendActionBarToNearbyPlayers\(world, entity\.position\(\), 32\.0, quote\);',
        'RotDialoguesProcedure.sendRandomWitherQuote(world, entity, 32.0);',
        content
    )

    # 4. Replace other quotes
    content = content.replace(
        'p.displayClientMessage(Component.literal("§cThe Rot observes the relic in your hands..."), true);',
        'RotDialoguesProcedure.sendTotemObserved(p);'
    )
    content = content.replace(
        'p.displayClientMessage(Component.literal("§cThe Rot witnesses your resurrection and analyzes the artifact..."), true);',
        'RotDialoguesProcedure.sendTotemPopped(p);'
    )
    content = content.replace(
        'p.displayClientMessage(Component.literal("§7Intriguing artifact..."), true);',
        'RotDialoguesProcedure.sendTotemStolen(p);'
    )

    with open(fname, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'[{fname}] Saved successfully.')

update_rot_ai('Trimmed/RotAI1_21_1_final.java')
update_rot_ai('Trimmed/RotAI1_21_8_final.java')
