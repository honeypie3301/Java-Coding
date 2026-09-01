# NeoForge 1.21.1 → 1.21.8 Porting Guide (v2)
Common API changes, compile errors, and fixes — for an AI agent maintaining **two parallel versions** of the same mod.

---

## 0. DUAL-VERSION EDITING PROTOCOL — READ FIRST

You are editing two source trees (or two source sets) for the same mod: one targeting **1.21.1** and one targeting **1.21.8**. Before touching any file:

1. **Identify which tree you're in.** Check the build file (`gradle.properties` / `build.gradle`) for `minecraft_version` or the module/folder name (e.g. `1.21.1/`, `1.21.8/`). Never assume — a snippet that compiles in one tree will silently break the other if copy-pasted blind.
2. **Never "fix" 1.21.1 code with 1.21.8 patterns or vice versa.** The #1 failure mode is applying an `.orElse()` unwrap to a 1.21.1 file (where the getter still returns the raw type) or leaving a raw getter in a 1.21.8 file. If a file doesn't match its version's expected pattern, that itself is the bug to report — don't "helpfully" convert it without checking which tree you're supposed to be in.
3. **When a fix is needed in both trees**, apply the 1.21.1-style fix to the 1.21.1 tree and the 1.21.8-style fix to the 1.21.8 tree — they will look different. Do not try to write one shared snippet unless using a compat shim (see §13).
4. **After editing, sanity-check the getter/lookup style against the rest of the file** — mixed old/new style in one file is the clearest sign a wrong-version edit happened.
5. If genuinely unsure which version's rules apply, check one neighboring line in the same file for `.orElse(` or `.map(ref -> ref.value())` — its presence/absence tells you the version dialect in use.

---

## 1. COMPOUND TAG / NBT READS  *(1.21.8 only)*

In 1.21.8, every `CompoundTag` getter returns `Optional<T>` instead of `T`. In 1.21.1 these all still return the raw type — do not add `.orElse()` there.

| Getter | 1.21.1 (unchanged) | 1.21.8 |
|---|---|---|
| `getBoolean` | `boolean b = tag.getBoolean("key");` | `tag.getBoolean("key").orElse(false)` |
| `getInt` | `int n = tag.getInt("key");` | `tag.getInt("key").orElse(0)` |
| `getString` | `String s = tag.getString("key");` | `tag.getString("key").orElse("")` |
| `getFloat` | `float f = tag.getFloat("key");` | `tag.getFloat("key").orElse(0.0f)` |
| `getDouble` | `double d = tag.getDouble("key");` | `tag.getDouble("key").orElse(0.0)` |
| `getLong` | `long l = tag.getLong("key");` | `tag.getLong("key").orElse(0L)` |
| `getByte` | `byte b = tag.getByte("key");` | `tag.getByte("key").orElse((byte) 0)` |
| `getShort` | `short s = tag.getShort("key");` | `tag.getShort("key").orElse((short) 0)` |
| `getIntArray` | `int[] arr = tag.getIntArray("key");` | `tag.getIntArray("key").orElse(new int[0])` |
| `getLongArray` | `long[] arr = tag.getLongArray("key");` | `tag.getLongArray("key").orElse(new long[0])` |
| `ListTag.getString(i)` | `list.getString(i)` | `list.getString(i).orElse("")` |
| `entity.getPersistentData().getInt(k)` | raw `int` | `.orElse(0)` |

**Compiler error signature:** `incompatible types: Optional<X> cannot be converted to boolean/int/String/...`

**Gotcha:** `.orElse(null)` on a primitive-returning `Optional` (e.g. `OptionalInt`) won't compile — primitives need `.orElse(0)`/`.orElse(false)`, not `null`. Only reference-typed optionals can use `null`.

---

## 2. REGISTRY LOOKUPS  *(1.21.8 only)*

`BuiltInRegistries.X.get(ResourceLocation)` returns `Optional<Reference<T>>` in 1.21.8 instead of `T` directly. Unwrap with `.map(ref -> ref.value())` before `.orElse()`. In 1.21.1 these calls still return `T` directly.

Needed import (1.21.8 unwrap sites): none extra — `Reference<T>` is in `net.minecraft.core`, usually already imported transitively; if the compiler can't resolve `Reference`, add `import net.minecraft.core.Holder.Reference;` (or `net.minecraft.core.Registry.Reference` depending on mapping set — check the existing imports in a working 1.21.8 file first).

| Lookup | 1.21.1 | 1.21.8 |
|---|---|---|
| Block | `Block b = BuiltInRegistries.BLOCK.get(rl);` | `BuiltInRegistries.BLOCK.get(rl).map(ref -> ref.value()).orElse(Blocks.AIR);` |
| SoundEvent | `SoundEvent se = BuiltInRegistries.SOUND_EVENT.get(rl);` | `.get(rl).map(ref -> ref.value()).orElse(null);` |
| Item | `Item item = BuiltInRegistries.ITEM.get(rl);` | `.get(rl).map(ref -> ref.value()).orElse(Items.AIR);` |
| ParticleType (static fields moved to registry) | `ParticleTypes.TRIAL_SPAWNER_DETECTION` | `BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse("minecraft:trial_spawner_detection")).map(ref -> ref.value()).orElseThrow()` |
| EntityType | `EntityType<?> et = BuiltInRegistries.ENTITY_TYPE.get(rl);` | `.get(rl).map(ref -> ref.value()).orElse(EntityType.PIG);` |
| MobEffect | `MobEffect e = BuiltInRegistries.MOB_EFFECT.get(rl);` | `.get(rl).map(ref -> ref.value()).orElse(null);` |
| `MobEffects.SLOWNESS` etc. | direct static field | `BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse("minecraft:slowness")).map(ref -> ref.value()).orElse(null)` |

`containsKey` is unchanged in both versions — no rewrite needed.

**Compiler error signatures:**
- `incompatible types: Block/Item/SoundEvent cannot be converted to Reference`
- `incompatible types: Optional cannot be converted to Reference`
- `cannot find symbol: variable SLOWNESS location: class MobEffects` (applies to any `MobEffects.X` constant)
- `cannot find symbol: variable TRIAL_SPAWNER_DETECTION in ParticleTypes`

**Choosing the fallback value:** prefer a safe non-null default (`Blocks.AIR`, `Items.AIR`, `EntityType.PIG`) over `null` wherever the surrounding code doesn't already null-check — `orElse(null)` is only safe when the call site immediately checks for null or the effect is genuinely optional (e.g. `ifPresent(...)`).

---

## 3. ENTITY / MOB METHODS  *(1.21.8 only)*

- `doHurtTarget` now requires `ServerLevel` as the first argument.
  - 1.21.1: `mob.doHurtTarget(target);`
  - 1.21.8: `mob.doHurtTarget((ServerLevel) mob.level(), target);`
  - Safer guarded form (works without an unchecked cast):
    ```java
    if (mob.level() instanceof ServerLevel sl) {
        mob.doHurtTarget(sl, target);
    }
    ```

- `hurt(DamageSource, float)` — DamageSource construction changed.
  - 1.21.1: `entity.hurt(DamageSource.FALL, 5.0f);`
  - 1.21.8: `entity.hurt(level.damageSources().fall(), 5.0f);`
  - Other `damageSources()` methods: `.generic()` `.explosion(entity)` `.magic()` `.drown()` `.starve()` `.fall()` `.inFire()` `.onFire()` `.wither()` `.anvil(entity)` `.lightningBolt()` `.outOfWorld()`

- `getEyePosition` — unchanged in both versions.

**Compiler error signature:**
`method doHurtTarget in class Mob cannot be applied to given types; required: ServerLevel,Entity found: Entity`

---

## 4. SOUND PLAYBACK  *(1.21.8 only)*

`ServerLevel.playSound(...)` itself is unchanged in both versions. The only difference is that a `SoundEvent` obtained via registry lookup must be unwrapped first in 1.21.8 (see §2).

```java
// 1.21.8
BuiltInRegistries.SOUND_EVENT
    .get(ResourceLocation.parse("minecraft:entity.blaze.hurt"))
    .map(ref -> ref.value())
    .ifPresent(se -> level.playSound(null, pos, se, SoundSource.HOSTILE, 1.0f, 1.0f));
```

`getAmbientSound` / `getHurtSound` / `getDeathSound` overrides still return plain `SoundEvent` in both versions — only the registry-lookup path needs unwrapping.

---

## 5. DAMAGE TYPES

- `damageSource.is(DamageTypes.FALL)` — unchanged in both versions.
- Custom damage source construction changed in 1.21.8:
  - 1.21.1: `new DamageSource("my_damage")`
  - 1.21.8: must come from a registry holder — register a custom `DamageType` under `data/modid/damage_type/` and look it up via:
    ```java
    level.registryAccess()
        .lookupOrThrow(Registries.DAMAGE_TYPE)
        .getOrThrow(MY_DAMAGE_TYPE_KEY)
    ```

---

## 6. ATTRIBUTE MODIFIERS  *(1.21.8 only)*

Modifier ID changes from `UUID` to `ResourceLocation`; `Operation` enum constant names also changed.

```java
// 1.21.1
entity.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(
    new AttributeModifier(UUID.randomUUID(), "my_modifier", 0.1, AttributeModifier.Operation.ADDITION));

// 1.21.8
entity.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(
    new AttributeModifier(ResourceLocation.parse("mymod:my_modifier"), 0.1, AttributeModifier.Operation.ADD_VALUE));
```

| Old (1.21.1) | New (1.21.8) |
|---|---|
| `ADDITION` | `ADD_VALUE` |
| `MULTIPLY_BASE` | `ADD_MULTIPLIED_BASE` |
| `MULTIPLY_TOTAL` | `ADD_MULTIPLIED_TOTAL` |

Removing a modifier: 1.21.1 uses `.removeModifier(uuid)`; 1.21.8 uses `.removeModifier(ResourceLocation.parse("mymod:my_modifier"))`.

**Gotcha:** if you store modifier IDs anywhere (config, saved NBT, a constants class), that storage format also has to change from UUID to ResourceLocation in the 1.21.8 tree — this is easy to miss because it's not a compile error, it's a silent runtime mismatch (modifier never found/removed).

---

## 7. RENDER EVENTS  *(1.21.8 only)*

- `RenderLivingEvent.Pre` entity accessor: `getEntity()` may be removed/moved behind a render-state object in some 1.21.8 builds.
  - If `cannot find symbol: method getEntity()`, try `event.getRenderer()` for entity access via renderer context, or cast generics explicitly: `RenderLivingEvent.Pre<YourEntity, YourModel, YourRenderState>`.
  - Treat this one as version-dependent even within 1.21.8 builds — check the actual NeoForge event class on the classpath rather than assuming.

- `RenderLevelStageEvent` replaces `RenderLevelLastEvent`.
  ```java
  // 1.21.1
  @SubscribeEvent
  public void onRenderLast(RenderLevelLastEvent event) { ... }

  // 1.21.8
  @SubscribeEvent
  public void onRenderStage(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
      ...
  }
  ```
  Other useful `Stage` values: `AFTER_SKY`, `AFTER_SOLID_BLOCKS`, `AFTER_PARTICLES`, `AFTER_WEATHER` — pick the one matching what the old `RenderLevelLastEvent` callback was drawing.

---

## 8. SUMMON COMMAND NBT (IN-GAME)  *(changed in 1.21.4, applies to 1.21.8; 1.21.1 uses old format)*

```
# 1.21.1
{HandItems:[{id:"minecraft:bow",count:1},{}],
 ArmorItems:[{},{},{},{id:"minecraft:iron_helmet",count:1}]}

# 1.21.8
{equipment:{
  mainhand:{id:"minecraft:bow",count:1},
  head:{id:"minecraft:iron_helmet",count:1}
}}
```

Slot names: `mainhand` `offhand` `head` `chest` `legs` `feet`

Preventing drops on death: 1.21.1 `DropChances:[0.0f,0.0f,0.0f,0.0f,0.0f]` → 1.21.8 `drop_chances:{head:0.0f, mainhand:0.0f}`

`Health` on summon — unchanged in both versions.

---

## 9. DIMENSION / LEVEL KEYS  *(no change)*

`ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("modid:dimname"))` and `level.dimension().equals(MY_KEY)` work identically in both versions.

---

## 10. WORLD GEN / FEATURES  *(no change)*

`FeaturePlaceContext`, `ChunkAccess.setBlockState`, custom `Feature` registration via `DeferredRegister`, and the `data/modid/neoforge/biome_modifiers/*.json` location are all identical in both versions.

---

## 11. REUSABLE HELPER FOR REPEATED LOOKUPS  *(1.21.8 tree only)*

Since MCreator regenerates procedures often, and each regenerated file re-triggers the same inline `.map(ref -> ref.value()).orElse(...)` boilerplate, centralize lookups in a small static holder class that MCreator won't overwrite (because it isn't a generated procedure file):

```java
public class ModRegistryHelper {
    public static final MobEffect SLOWNESS = lookup(BuiltInRegistries.MOB_EFFECT, "minecraft:slowness");
    public static final SoundEvent SHIELD_BLOCK = lookup(BuiltInRegistries.SOUND_EVENT, "minecraft:item.shield.block");

    private static <T> T lookup(Registry<T> registry, String id) {
        return registry.get(ResourceLocation.parse(id))
            .map(ref -> ref.value())
            .orElse(null);
    }
}
```

Reference `ModRegistryHelper.SLOWNESS` in procedures instead of `MobEffects.SLOWNESS`. One place to fix if an API renames something again.

**Important for the dual-version case:** put this class in the 1.21.8 tree only, or gate its contents behind a version check — a 1.21.1 build doesn't need it and the `Registry<T>` generic signature may not even resolve the same way there.

---

## 12. QUICK COMPILER ERROR LOOKUP

| Error text | Cause | Fix |
|---|---|---|
| `incompatible types: Optional cannot be converted to boolean/int/String/float...` | 1.21.8 CompoundTag getter | Add `.orElse(defaultValue)` |
| `incompatible types: Optional cannot be converted to Reference` | 1.21.8 registry lookup | Add `.map(ref -> ref.value()).orElse(...)` |
| `incompatible types: Block/Item/SoundEvent cannot be converted to Reference` | 1.21.8 registry lookup | Same as above |
| `method doHurtTarget in class Mob cannot be applied to given types; required: ServerLevel,Entity` | 1.21.8 signature change | Add `(ServerLevel)` as first arg |
| `cannot find symbol: method getEntity() in RenderLivingEvent.Pre` | 1.21.8 event API change | Check renamed accessor / use `event.getRenderer()` |
| `cannot find symbol: variable TRIAL_SPAWNER_DETECTION in ParticleTypes` | Static field moved to registry | Look up via `BuiltInRegistries.PARTICLE_TYPE` |
| `cannot find symbol: variable SLOWNESS (or any effect) in MobEffects` | Static field moved to registry | Look up via `BuiltInRegistries.MOB_EFFECT` |
| `constructor AttributeModifier(UUID, String, double, Operation) not found` | 1.21.8 signature change | Replace UUID with ResourceLocation, update Operation enum name |
| `cannot find symbol: class DamageSource` constructor form | 1.21.8 DamageSource change | Use `level.damageSources().X()` or registry lookup (§5) |

---

## 13. OPTIONAL: SHARED-CODE COMPAT SHIM

If the two version trees share any common module, consider a small interface implemented once per version instead of scattering version checks through gameplay code:

```java
public interface CompatBridge {
    int getTagInt(CompoundTag tag, String key, int def);
    Block getBlock(ResourceLocation rl, Block fallback);
    void doHurtTarget(Mob mob, Level level, Entity target);
}
```

Implement one `CompatBridge` per version tree (`CompatBridge121_1`, `CompatBridge121_8`), wire the right one in per-module DI/service loading. This is worth doing only if the shared/common code volume is large enough to justify the extra indirection — for a small mod, per-tree inline fixes (§1–§8) are simpler to keep in sync.

---

## 14. BEFORE YOU SUBMIT A FIX — CHECKLIST

- [ ] Confirmed which version tree the file belongs to
- [ ] Fix style (raw vs Optional-unwrap) matches the rest of the file
- [ ] Chosen fallback (`.orElse(...)`) is a safe value, not blindly `null`, unless the call site already null-checks
- [ ] If the fix touches attribute modifier IDs, checked whether the ID is persisted anywhere (NBT/config) that also needs updating
- [ ] If the fix touches a registry static field (MobEffects/ParticleTypes/etc.), searched the file for other uses of the same field — these tend to appear multiple times
- [ ] Didn't copy a 1.21.8-style snippet into a 1.21.1 file or vice versa

================================================================
END OF GUIDE
================================================================
