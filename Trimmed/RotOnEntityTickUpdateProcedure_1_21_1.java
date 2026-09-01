package net.mcreator.thebackwoods.procedures;
// 1.21.1 - Sentinel Adaptive Overhaul
import net.mcreator.thebackwoods.BlackHole;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.EntityAnchorArgument;

import net.mcreator.thebackwoods.entity.RotEntity;

import javax.annotation.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

@EventBusSubscriber
public class RotOnEntityTickUpdateProcedure {
	public static boolean dealTrueDamageToBosses(net.minecraft.world.entity.Entity target, net.minecraft.world.damagesource.DamageSource ds, float amount) {
		if (target == null || !target.isAlive()) return false;
		if (target instanceof net.minecraft.world.entity.player.Player player) {
			if (player.isCreative() || player.isSpectator() || player.getAbilities().invulnerable) return false;
			return player.hurt(ds, amount);
		}
		if (target.isInvulnerable()) return false;

		String targetType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().toLowerCase(java.util.Locale.ROOT);
		if ("alexscaves:ferrouswroughtnaut".equals(targetType) || "alexscaves:ferrous_wroughtnaut".equals(targetType)) return false;

		if (target instanceof net.minecraft.world.entity.LivingEntity living) {
			living.invulnerableTime = 0;

			living.getActiveEffects().removeIf(effect -> {
				String effectId = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString().toLowerCase(java.util.Locale.ROOT);
				return effectId.contains("invincib") || effectId.contains("immunity") || effectId.contains("invulnerab");
			});

			float oldHealth = living.getHealth();
			boolean hurtSuccess = living.hurt(ds, amount);

			if ((!hurtSuccess || living.getHealth() >= oldHealth) && amount > 0) {
				float targetHealth = Math.max(0.0F, oldHealth - amount);
				living.setHealth(targetHealth);
				living.hurtTime = 10;
				living.hurtDuration = 10;
				living.hurtMarked = true;
				if (targetHealth <= 0.0F && !living.isDeadOrDying()) {
					living.die(ds);
				}
				return true;
			}
			return hurtSuccess;
		}
		return target.hurt(ds, amount);
	}

	public static boolean ENABLE_EXTRACTION_GRAPPLE = false;
	public static boolean ENABLE_TELEKINESIS = false;
	public static boolean ENABLE_BLOCKING = true;
	public static boolean ENABLE_CONTROLLED_ADAPTATION = true;
	public static boolean ENABLE_PHASE_SHIFT = false;
	public static double PARTICLE_QUALITY = 1.0;
	public static double COOLDOWN_MULTIPLIER = 1.5;
	private static final double TARGET_RANGE = 128.0;

	public static double ROT_PLAYER_BACK_OFF_DISTANCE = 0.95;
	public static double ROT_PILLAR_BACK_OFF_DISTANCE = 15.0;
	public static double ROT_PILLAR_INITIAL_ATTACK_DELAY = 1000.0;
	public static double ROT_PILLAR_ATTACK_CHANCE = 0.005;
	public static double ROT_PILLAR_CIRCLING_SPEED = 0.8;
	public static double ROT_PILLAR_CIRCLING_CHANCE = 0.15;

	public static double SONIC_SCREAM_COOLDOWN = 1200.0;

	public static double SUPERHEAT_EVAPORATION_COOLDOWN = 700.0;
	public static double SUPERHEAT_EVAPORATION_RADIUS = 45.0;
	public static double SUPERHEAT_EVAPORATION_DAMAGE = 26.0;
	public static double SUPERHEAT_EVAPORATION_WAVE_TICKS = 90.0;

	public static double SONIC_BOOM_RANGE = 24.0;
	public static double SONIC_BOOM_MIN_DIST = 2.5;
	public static double SONIC_BOOM_COOLDOWN = 324.0;
	public static double SONIC_BOOM_TORSO_Y_FACTOR = 1;
	public static double WARDEN_LEARN_REQUIRED_TICKS = 405.0;
	public static double SONIC_BOOM_ANIMATION_TICKS = 90.0;
	public static double SONIC_BOOM_TRIGGER_TICK = 36.0;

	private static final double LASER_Y_OFFSET = 0.25;
	private static final double LASER_CLOSING_TICKS = 80.0;
	private static final double TOTEM_LASER_MIN_DIST = 2.5;

	private static final double TELEPORT_MIN_GAP = 4.0;
	private static final double TELEPORT_BACK_OFFSET = 2.4;
	private static final double TELEPORT_SIDE_MIN = 1.6;
	private static final double TELEPORT_SIDE_MAX = 3.0;
	private static final double TELEPORT_MAX_VERTICAL_DIFF = 6.0;

	private static final double DODGE_TRIGGER_DIST = 6.0;
	private static final double DODGE_SWING_CHANCE = 0.85;

	private static final int MINE_REACH = 3;
	private static final int MINE_HEIGHT = 3;
	private static final int MINE_HALF_WIDTH = 1;
	private static final float MAX_BREAKABLE_HARDNESS = 60f;
	private static final float MINE_SPEED_MULTIPLIER = 16.665f;
	private static final float MINE_SPEED_BASE = 16.665f;
	private static final double MINE_RAY_DISTANCE = 2.0;

	private static final double DIE_KICK_SPEED = 10.0;

	public static int TP_DODGE_CD = 40;
	public static int TP_FLANK_CD = 100;
	public static int SOLAR_CD = 360;
	public static int ADAPT_CD = 320;
	public static int GRAPPLE_CD = 220;
	public static int TK_CD = 220;

	public static double MELEE_ATTACK_CD = 20.0;
	public static double COMBO_GLOBAL_CD = 50.0;
	public static double COMBO_GLOBAL_CD_TOTEM = 20.0;
	public static int TELEPORT_DODGE_COOLDOWN = 18;
	public static int TELEPORT_FLANK_COOLDOWN = 30;
	public static int SOLAR_BEAM_COOLDOWN = 360;
	public static int CRYO_BEAM_COOLDOWN = 360;
	public static int MUTANT_GRAPPLE_COOLDOWN = 220;
	public static int TELEKINESIS_COOLDOWN = 220;

	public static double COMBO_TRIPLE_THREAT_CD = 1000.0;
	public static double COMBO_TRIPLE_THREAT_CD_TOTEM = 800.0;
	public static double COMBO_HIGH_SKY_SLAM_CD = 1000.0;
	public static double COMBO_HIGH_SKY_SLAM_CD_TOTEM = 800.0;
	public static double COMBO_PUNCH_DROPKICK_CD = 1000.0;
	public static double COMBO_PUNCH_DROPKICK_CD_TOTEM = 800.0;
	public static double COMBO_PUNCH_RIDER_KICK_CD = 1000.0;
	public static double COMBO_PUNCH_RIDER_KICK_CD_TOTEM = 800.0;
	public static double COMBO_HEAVENLY_REPENTANCE_PLUS_CD = 1000.0;
	public static double COMBO_HEAVENLY_REPENTANCE_PLUS_CD_TOTEM = 800.0;

	public static double DIVE_COUNTER_MIN_HEIGHT = 4.0;
	public static double DIVE_COUNTER_TRIGGER_RANGE = 12.0;
	public static double DIVE_COUNTER_COOLDOWN = 240.0;

	public static double OMNI_SONIC_BOOM_CD = 600.0;
	public static double OMNI_SONIC_BOOM_ANIMATION_TICKS = 270.0;
	public static double OMNI_SONIC_BOOM_TRIGGER_TICK = 140.0;
	public static double OMNI_SONIC_BOOM_RANGE = 24.0;
	public static boolean OMNI_SONIC_BOOM_SHOW_PARTICLES = true;

	public static double ADAPTATION_REGEN_BASE_HEAL = 1.0;
	public static double ADAPTATION_REGEN_MAX_HEALTH_RATIO = 0.0075;
	public static double ADAPTATION_REGEN_COMBAT_MULTIPLIER = 2.0;
	public static double ADAPTATION_REGEN_HEALTH_LOW_BURST = 3.5;
	public static double ADAPTATION_REGEN_HEALTH_MID_BURST = 2.0;

	public static double ADAPTATION_RESISTANCE_DECAY = 0.95;
	public static double ADAPTATION_RESISTANCE_HIGH_THRESHOLD = 70.0;
	public static double ADAPTATION_RESISTANCE_MID_THRESHOLD = 40.0;
	public static double ADAPTATION_RESISTANCE_LOW_THRESHOLD = 16.0;

	public static double ADAPTATION_SPEED_MIN_MULTIPLIER = -0.45;
	public static double ADAPTATION_SPEED_MAX_MULTIPLIER = 0.10;
	public static double ADAPTATION_SPEED_MAX_FALLBACK = 0.25;
	public static double ADAPTATION_SPEED_SCALING_TELEPORT = 1000.0;
	public static double ADAPTATION_SPEED_SCALING_FALLBACK = 600.0;

	public static double ROT_WALK_SPEED = 1.0;
	public static double ROT_RUN_SPEED = 1.45;

	public static double MELEE_PUNCH_DAMAGE = 18.0;
	public static double SONIC_BOOM_DMG = 38.0;
	public static double SONIC_BOOM_DMG_TOTEM = 65.0;
	public static double SONIC_BOOM_SPLASH_DAMAGE = 10.0;
	public static double SONIC_BOOM_SPLASH_TOTEM_DMG = 22.0;
	public static double SOLAR_BEAM_DMG_BASE = 8.0;
	public static double SOLAR_BEAM_DMG_BOOST = 18.0;
	public static double CRYO_BEAM_DMG_BASE = 8.0;
	public static double CRYO_BEAM_DMG_BOOST = 18.0;
	public static double MUTANT_DNA_GRAPPLE_DMG = 4.0;

	public static double COMBO_SEISMIC_SLAM_DMG = 35.0;
	public static double COMBO_OVERHEAD_SLAM_DMG = 40.0;

	public static double OVERHEAD_TOTAL_TICKS = 56.0;
	public static double OVERHEAD_PREP_THRESHOLD = 31.0;
	public static double OVERHEAD_STRIKE_TICK = 30.0;
	public static double OVERHEAD_Y_OFFSET_1 = 0.72;
	public static double OVERHEAD_Y_OFFSET_2 = 0.84;
	public static double OVERHEAD_STRIKE_FALL_VELOCITY = -10.0;
	public static double OVERHEAD_POST_STRIKE_FALL_VELOCITY = -3.2;

	public static double HEAVY_PUNCH_TOTAL_TICKS = 61.0;
	public static double HEAVY_PUNCH_STRIKE_TICK = 35.0;

	public static double UPPERCUT_TOTAL_TICKS = 60.0;
	public static double UPPERCUT_LAUNCH_TICK = 29.0;
	public static double UPPERCUT_DAMAGE = 30.0;

	public static double JUDGMENT_KICK_IMPACT_DIST = 0.7;
	public static double DIE_KICK_IMPACT_DIST = 0.7;
	public static double DIE_KICK_GROUND_OFFSET = 0.6;

	public static double COMBO_JUDGMENT_KICK_DMG = 50.0;
	public static double COMBO_DIE_RIDER_KICK_DMG = 65.0;
	public static double COMBO_GRAPPLE_SIPHON_DMG = 12.0;
	public static double COMBO_TK_SLAM_DMG = 15.0;
	public static double COMBO_NANITE_BLITZ_DMG = 8.0;
	public static double COMBO_THERMAL_SHOCK_DMG = 25.0;

	public static double BETA_VARIANCE_GATE_THRESHOLD = 0.08;
	public static double NN_LEARNING_RATE = 0.05;
	public static int NN_HIDDEN_NEURONS = 8;

	public static double PERSONALITY_DRIFT_RATE = 0.02;

	public static double SURPRISE_Z_SCORE_THRESHOLD = 2.5;
	public static double ANOMALY_MIN_SAMPLES = 5.0;

	public static int ROLE_AUCTION_DURATION_TICKS = 60;
	public static double ROLE_AUCTION_RANGE = 32.0;

	public static boolean ENABLE_ARMOR_RIP = true;
	public static double ARMOR_RIP_COOLDOWN = 600.0;
	public static double ARMOR_RIP_TICKS = 120.0;
	public static double ARMOR_RIP_TRIGGER_DISTANCE = 1.5;
	public static double ARMOR_RIP_MAX_DISTANCE = 16.0;
	public static double ARMOR_RIP_HOLD_DISTANCE = 0.8;
	public static double ARMOR_RIP_RIGHT_OFFSET = 0.55;
	public static double ARMOR_RIP_HEIGHT_OFFSET = 0.50;
	public static double ARMOR_RIP_CHANCE_REGULAR = 0.001;
	public static double ARMOR_RIP_CHANCE_INDESTRUCTIBLE = 0.02;
	public static double TOTEM_STEAL_TIME_MIN = 150.0;
	public static double TOTEM_STEAL_TIME_MAX = 1000.0;
	public static double BLOCK_MIN_TICKS = 20.0;
	public static double BLOCK_MAX_TICKS = 45.0;
	public static int CHOKE_MIN_HITS = 5;
	public static int CHOKE_MAX_HITS = 20;
	public static int CHOKE_TOTEM_MIN_HITS = 15;
	public static int CHOKE_TOTEM_MAX_HITS = 30;
	public static double CHOKE_DAMAGE = 2.0;
	public static int CHOKE_DAMAGE_INTERVAL = 15;
	public static int CHOKE_ARMOR_DURABILITY_LOSS = 100;
	public static int CHOKE_INDESTRUCTIBLE_DROP_INTERVAL = 30;

	public static final String K_WOODBOUND = "the_backwoods:woodbound_entities";

	public static final String K_TP_DODGE_CD = "sentinel_dodge_cd";
	public static final String K_TP_FLANK_CD = "sentinel_flank_cd";
	public static final String K_SOLAR_CD = "sentinel_solar_cd";
	public static final String K_SOLAR_CHARGE = "sentinel_solar_charge";
	public static final String K_ADAPT_MODE = "sentinel_adapt_mode";
	public static final String K_ADAPT_CD = "sentinel_adapt_cd";
	public static final String K_GRAPPLE_CD = "sentinel_grapple_cd";
	public static final String K_GRAPPLE_TICKS = "sentinel_grapple_ticks";
	public static final String K_CREATIVE_MSG = "creative_msg_fired";
	public static final String K_AGE = "Age";
	public static final String K_TK_CD = "sentinel_tk_cd";
	public static final String K_TK_TICKS = "sentinel_tk_ticks";
	public static final String K_SWSOT = "sentinel_wither_skull_outcome_ticks";
	public static final String K_SWSOB = "sentinel_wither_skull_outcome_baseline";
	public static final String K_RSC = "rot_superheat_charging";
	public static final String K_RSA = "rot_superheat_active";
	public static final String K_RART = "rot_armor_rip_ticks";
	public static final String K_RBAT = "rot_block_active_ticks";
	public static final String K_STIT = "sentinel_totem_inspect_ticks";
	public static final String K_SSRA = "sentinel_sonic_reposition_attempts";
	public static final String K_SJDX = "sentinel_judgment_dir_x";
	public static final String K_SJDY = "sentinel_judgment_dir_y";
	public static final String K_SJDZ = "sentinel_judgment_dir_z";
	public static final String K_SSFT = "sentinel_solar_fire_ticks";
	public static final String K_SSCT = "sentinel_solar_charge_ticks";
	public static final String K_SCFT = "sentinel_cryo_fire_ticks";
	public static final String K_SCCT = "sentinel_cryo_charge_ticks";
	public static final String K_SEPC = "sentinel_eat_punish_cooldown";
	public static final String K_SJT = "sentinel_judgment_ticks";
	public static final String K_SAOS = "sentinel_attack_outcome_score";
	public static final String K_SMS = "sentinel_minos_stage";
	public static final String K_SMT = "sentinel_minos_ticks";
	public static final String K_SWSFT = "sentinel_wither_skull_fire_ticks";
	public static final String K_SLAX = "sentinel_laser_aim_x";
	public static final String K_SLAY = "sentinel_laser_aim_y";
	public static final String K_SLAZ = "sentinel_laser_aim_z";
	public static final String K_SLTI = "sentinel_laser_target_id";
	public static final String K_SUC = "sentinel_uppercut_cd";
	public static final String K_SDKP = "sentinel_die_kick_phase";
	public static final String K_SLT = "sentinel_landing_ticks";
	public static final String K_SSBH = "sentinel_sustained_bullet_hits";
	public static final String K_SWSC = "sentinel_warden_sonic_cooldown";
	public static final String K_RLX = "rot_last_x";
	public static final String K_RLZ = "rot_last_z";
	public static final String K_SESC = "sentinel_evasive_spacing_cd";
	public static final String K_ROT = "rot_overhead_ticks";
	public static final String K_SDCC = "sentinel_dive_counter_cd";
	public static final String K_SHPM = "sentinel_heavy_punch_misses";
	public static final String K_SDKDX = "sentinel_die_kick_dir_x";
	public static final String K_SDKDY = "sentinel_die_kick_dir_y";
	public static final String K_SDKDZ = "sentinel_die_kick_dir_z";
	public static final String K_STST = "sentinel_totem_steal_timer";
	public static final String K_SCT = "sentinel_combat_ticks";
	public static final String K_STOP = "sentinel_totem_observe_progress";
	public static final String K_STA = "sentinel_totem_awareness";
	public static final String K_STTST = "sentinel_totem_target_steal_time";
	public static final String K_SGAC = "sentinel_global_ability_cooldown";
	public static final String K_SLH = "sentinel_laser_heat";
	public static final String K_STES = "sentinel_target_escape_score";
	public static final String K_RPC = "rot_phase_cooldown";
	public static final String K_RPM = "rot_phase_mastery";
	public static final String K_ATST = "ai_target_shield_ticks";
	public static final String K_ATSAT = "ai_target_sprint_away_ticks";
	public static final String K_ADT = "ai_distance_trend";
	public static final String K_SRD = "sentinel_recent_damage";
	public static final String K_SLTDT = "sentinel_last_target_damage_tick";
	public static final String K_STRR = "sentinel_target_regen_rate";
	public static final String K_SDSR = "sentinel_defense_success_rate";
	public static final String K_RSC2 = "rot_superheat_cd";
	public static final String K_SPTS = "sentinel_predicted_threat_score";
	public static final String K_RBC = "rot_block_cooldown";
	public static final String K_SMC = "sentinel_melee_cooldown";
	public static final String K_SMW = "sentinel_melee_windup";
	public static final String K_SST = "sentinel_sonic_ticks";
	public static final String K_SOSCT = "sentinel_omni_sonic_charge_ticks";
	public static final String K_SSST = "sentinel_sonic_scream_ticks";
	public static final String K_SSWST = "sentinel_sky_warp_slam_ticks";
	public static final String K_SSP = "sentinel_slam_phase";
	public static final String K_SDKT = "sentinel_die_kick_ticks";
	public static final String K_SST2 = "sentinel_slam_ticks";
	public static final String K_SCS = "sentinel_cc2_stage";
	public static final String K_SCT2 = "sentinel_cc2_ticks";
	public static final String K_RPT = "rot_phase_ticks";
	public static final String K_SCS2 = "sentinel_cc1_stage";
	public static final String K_SCS3 = "sentinel_cc3_stage";
	public static final String K_SCS4 = "sentinel_cc4_stage";
	public static final String K_SCS5 = "sentinel_cc5_stage";
	public static final String K_SCAT = "sentinel_combo_active_ticks";
	public static final String K_SLCT = "sentinel_laser_closing_ticks";
	public static final String K_STA2 = "sentinel_totem_active";
	public static final String K_SIIT = "sentinel_is_infinity_totem";
	public static final String K_CHL = "client_had_laser";
	public static final String K_USB = "unlocked_solar_beam";
	public static final String K_UCB = "unlocked_cryo_beam";
	public static final String K_DFO = "debug_force_overhead";
	public static final String K_DFR = "debug_force_rider";
	public static final String K_AGM = "adapted_gravitational_mass";
	public static final String K_SSI = "sentinel_spawn_initialized";
	public static final String K_SSS = "sentinel_should_scan";
	public static final String K_DFB = "debug_force_block";
	public static final String K_IB = "is_blocking";
	public static final String K_SISLA = "sentinel_immune_slam_landing_active";
	public static final String K_SSV = "sentinel_shockwave_vertical";
	public static final String K_SCICB = "sentinel_cached_in_cold_biome";
	public static final String K_IAR = "is_armor_ripping";
	public static final String K_UOC = "unlocked_overhead_combo";
	public static final String K_UEB = "unlocked_explosion_boom";
	public static final String K_SST3 = "sentinel_sonic_triggered";
	public static final String K_SSWSI = "sentinel_sky_warp_slam_impact";
	public static final String K_SRHO = "sentinel_rider_hold_onground";
	public static final String K_MGM = "master_guard_mode";
	public static final String K_MFE = "master_follow_enabled";
	public static final String K_AS = "analyzed_species_";
	public static final String K_UT = "unlocked_teleportation";
	public static final String K_AFR = "adapted_forcefield_repulsion";
	public static final String K_USB2 = "unlocked_sonic_boom";
	public static final String K_IFH = "is_falling_heavy";
	public static final String K_UG = "unlocked_grapple";
	public static final String K_SWI = "sentinel_waiting_intercept";
	public static final String K_SPHT = "sentinel_punch_hand_toggle";
	public static final String K_UHSSC = "unlocked_high_sky_slam_combo";
	public static final String K_UDC = "unlocked_dropkick_combo";
	public static final String K_UTTC = "unlocked_triple_threat_combo";
	public static final String K_UKRC = "unlocked_knockback_rider_combo";
	public static final String K_ID = "is_dueling";
	public static final String K_SCLT = "sentinel_cached_learn_tp";
	public static final String K_UR = "unlocked_regen";
	public static final String K_SPAI = "sentinel_predicted_attack_imminent";
	public static final String K_UWS = "unlocked_wither_skulls";
	public static final String K_IU = "is_uppercutting";
	public static final String K_DFUL = "debug_force_uppercut_left";
	public static final String K_DFUR = "debug_force_uppercut_right";
	public static final String K_SISL = "sentinel_is_slam_landing";
	public static final String K_ISBL = "is_sonic_boom_large";
	public static final String K_IBF = "is_blocking_finish";
	public static final String K_IUL = "is_uppercutting_left";
	public static final String K_IUR = "is_uppercutting_right";
	public static final String K_IDC = "is_dropkick_charging";
	public static final String K_I = "isLand";
	public static final String K_I2 = "isLand2";
	public static final String K_UWE = "unlocked_water_evaporation";
	public static final String K_TFD = "taken_fire_damage";
	public static final String K_ROS = "rot_overhead_started";
	public static final String K_SRTA = "sentinel_reacting_to_attacker";
	public static final String K_SDCA = "sentinel_dive_counter_active";
	public static final String K_SCL = "sentinel_cc2_launched";
	public static final String K_SUD = "sentinel_uppercut_dodged";
	public static final String K_IUS = "is_uppercut_standalone";
	public static final String K_STL = "sentinel_totem_learned";
	public static final String K_STS = "sentinel_totem_stolen";
	public static final String K_SJST = "sentinel_just_stole_totem";
	public static final String K_RPS = "rot_phase_shifting";
	public static final String K_SWSHF = "sentinel_wither_skull_has_fired";
	public static final String K_ISB = "is_sonic_boom";
	public static final String K_SBA = "sonic_boom_active";
	public static final String K_SWD = "sentinel_windup_detected";
	public static final String K_RDTI = "rot_deprioritize_target_id";
	public static final String K_RDT = "rot_deprioritize_ticks";
	public static final String K_RLMP = "rot_last_mine_prog";
	public static final String K_RTNM = "rot_ticks_no_movement";
	public static final String K_RTNM2 = "rot_ticks_no_mining";
	public static final String K_RST = "rot_stuck_tier";
	public static final String K_RSTT = "rot_stuck_tier_ticks";
	public static final String K_SLTC = "sentinel_last_totem_count";
	public static final String K_STPW = "sentinel_totem_pops_witnessed";
	public static final String K_MFTU = "master_follow_target_uuid";
	public static final String K_CASI = "controlled_adapt_start_item";
	public static final String K_SM1 = "sentinel_mem_1";
	public static final String K_SM2 = "sentinel_mem_2";
	public static final String K_RAH = "recent_attack_history";
	public static final String K_STP = "sentinel_tactical_plan";
	public static final String K_SAR = "sentinel_assigned_role";
	public static final String K_AABI = "ai_active_bias_indices";
	public static final String K_SPTL = "sentinel_predicted_threat_level";
	public static final String K_SWSOT2 = "sentinel_wither_skull_outcome_target";
	public static final String K_SCPD = "sentinel_cd_phase_duration";
	public static final String K_CLCT = "client_laser_closing_ticks";
	public static final String K_RDT2 = "rot_death_ticks";
	public static final String K_RDSY = "rot_death_start_y";
	public static final String K_RDTH = "rot_death_target_height";
	public static final String K_RDHS = "rot_death_hole_size";
	public static final String K_RIFT = "rot_involuntary_float_ticks";
	public static final String K_RBFT = "rot_block_finish_ticks";
	public static final String K_RDTC = "rot_dps_tick_counter";
	public static final String K_RDTS = "rot_dmg_this_sec";
	public static final String K_RDS0 = "rot_dmg_sec_0";
	public static final String K_RDS1 = "rot_dmg_sec_1";
	public static final String K_RDS2 = "rot_dmg_sec_2";
	public static final String K_RDS3 = "rot_dmg_sec_3";
	public static final String K_RLT = "rot_land_timer";
	public static final String K_SSS2 = "sentinel_shockwave_stage";
	public static final String K_SSX = "sentinel_shockwave_x";
	public static final String K_SSY = "sentinel_shockwave_y";
	public static final String K_SSZ = "sentinel_shockwave_z";
	public static final String K_SSY2 = "sentinel_shockwave_yaw";
	public static final String K_STICB = "sentinel_time_in_cold_biome";
	public static final String K_STIN = "sentinel_time_in_nether";
	public static final String K_TUFT = "target_unreachable_flying_ticks";
	public static final String K_APD = "adapted_punch_damage";
	public static final String K_SRHT = "sentinel_rider_hold_ticks";
	public static final String K_SHLPT = "sentinel_heavy_left_punch_ticks";
	public static final String K_SHRPT = "sentinel_heavy_right_punch_ticks";
	public static final String K_SMWT = "sentinel_minos_wait_ticks";
	public static final String K_SMPC = "sentinel_minos_punch_count";
	public static final String K_SAT = "sentinel_analyzing_ticks";
	public static final String K_ACPT = "ai_combat_plan_ticks";
	public static final String K_ATAT = "ai_target_air_ticks";
	public static final String K_ALD = "ai_last_dist";
	public static final String K_ALTH = "ai_last_target_health";
	public static final String K_AB = "ai_bias_";
	public static final String K_ATCPT = "ai_tp_combo_penalty_ticks";
	public static final String K_AFPT = "ai_fake_pressure_ticks";
	public static final String K_CAT = "controlled_adaptation_ticks";
	public static final String K_CAC = "controlled_adaptation_cooldown";
	public static final String K_SRPT = "sentinel_repulsion_push_ticks";
	public static final String K_SFIC = "sentinel_flight_intercept_cooldown";
	public static final String K_RWDC = "rot_wither_dialogue_cooldown";
	public static final String K_SSSC = "sentinel_sonic_scream_cooldown";
	public static final String K_RARC = "rot_armor_rip_cooldown";
	public static final String K_SLDP = "sentinel_laser_drill_progress";
	public static final String K_SCC = "sentinel_cc1_cd";
	public static final String K_SCC2 = "sentinel_cc2_cd";
	public static final String K_SCC3 = "sentinel_cc3_cd";
	public static final String K_SCC4 = "sentinel_cc4_cd";
	public static final String K_SCC5 = "sentinel_cc5_cd";
	public static final String K_MKTI = "master_kill_target_id";
	public static final String K_SFTT = "sentinel_flying_target_ticks";
	public static final String K_STLP = "sentinel_teleport_learning_progress";
	public static final String K_SRT = "sentinel_regen_timer";
	public static final String K_RPCT = "rot_pillar_circle_ticks";
	public static final String K_RPST = "rot_pillar_state_timer";
	public static final String K_RPIC = "rot_pillar_is_circling";
	public static final String K_RPA = "rot_pillar_angle";
	public static final String K_SLPT = "sentinel_left_punch_ticks";
	public static final String K_SRPT2 = "sentinel_right_punch_ticks";
	public static final String K_DULT = "debug_uppercut_left_ticks";
	public static final String K_DURT = "debug_uppercut_right_ticks";
	public static final String K_SUAT = "sentinel_uppercut_anim_ticks";
	public static final String K_RSMC = "rot_superheat_max_charge";
	public static final String K_RSCR = "rot_superheat_current_radius";
	public static final String K_SST4 = "sentinel_scanning_ticks";
	public static final String K_SSBY = "sentinel_scanning_base_yaw";
	public static final String K_SSMT = "sentinel_scan_max_ticks";
	public static final String K_SCT3 = "sentinel_cc1_ticks";
	public static final String K_SCAT2 = "sentinel_cc2_air_ticks";
	public static final String K_SCT4 = "sentinel_cc3_ticks";
	public static final String K_SCT5 = "sentinel_cc4_ticks";
	public static final String K_SCT6 = "sentinel_cc5_ticks";
	public static final String K_RCLY = "rot_choke_locked_yaw";
	public static final String K_SWSF = "sentinel_wither_skull_failures";
	public static final String K_RDSA = "rot_death_sequence_active";
	public static final String K_RDHS2 = "rot_death_hole_spawned";
	public static final String K_RFGA = "rot_forced_gravity_active";
	public static final String K_SSPT = "sentinel_said_prepare_thyself";
	public static final String K_LC = "laser_charging";
	public static final String K_ILC = "is_laser_charging";
	public static final String K_LF = "laser_firing";
	public static final String K_ILF = "is_laser_firing";
	public static final String K_LC2 = "laser_closing";
	public static final String K_ILC2 = "is_laser_closing";
	public static final String K_ILP = "is_left_punching";
	public static final String K_IRP = "is_right_punching";
	public static final String K_IAS = "is_airborne_state";
	public static final String K_IAT = "is_air_time";
	public static final String K_SIAS = "sentinel_is_airborne_state";
	public static final String K_IOP = "is_overhead_preparing";
	public static final String K_IO = "is_overhead";
	public static final String K_ISC = "is_slam_charge";
	public static final String K_IGC = "is_ground_crushing";
	public static final String K_SFT = "sentinel_frozen_ticks";
	public static final String K_SFBT = "sentinel_frozen_break_ticks";
	public static final String K_SATI = "sentinel_analysis_target_id";
	public static final String K_SLDX = "sentinel_laser_drill_x";
	public static final String K_SLDY = "sentinel_laser_drill_y";
	public static final String K_SLDZ = "sentinel_laser_drill_z";
	public static final String K_SCTI = "sentinel_combo_target_id";
	public static final String K_SLTI2 = "sentinel_locked_target_id";
	public static final String K_STLT = "sentinel_target_lock_ticks";
	public static final String K_RMX = "rot_mine_x";
	public static final String K_RMY = "rot_mine_y";
	public static final String K_RMZ = "rot_mine_z";
	public static final String K_SLTS = "sentinel_last_totem_slot";
	public static final String K_MTQ = "master_target_queue";
	public static final String K_OTU = "overhead_target_uuid";
	public static final String K_SM3 = "sentinel_mem_3";
	public static final String K_SWSIT = "sentinel_wither_skull_immune_target";
	public static final String K_SWSFT2 = "sentinel_wither_skull_failure_target";
	public static final String K_SRS = "sentinel_regen_state";
	public static final String K_SRDR = "sentinel_regen_delay_reason";

	
	private static double getDynamicGlobalCooldown(Entity entity) {
		if (getRotPersistentBoolean(entity, K_STA2, false) && !getRotPersistentBoolean(entity, K_SIIT, false)) {
			return 20.0;
		}
		double phaseDuration = entity.getPersistentData().getDouble(K_SCPD);
		if (phaseDuration <= 0) {
			phaseDuration = 900.0 + entity.level().getRandom().nextDouble() * 1500.0;
			entity.getPersistentData().putDouble(K_SCPD, phaseDuration);
		}
		
		double rawCombatTicks = entity.getPersistentData().getDouble(K_SCT);
		double t = Math.min(rawCombatTicks / phaseDuration, 1.0);
		double ease = -(Math.cos(Math.PI * t) - 1.0) / 2.0;

		return 160.0 - (ease * 140.0);
	}

private static float lerpAngle(float pct, float start, float end) {
		float delta = Mth.wrapDegrees(end - start);
		return start + delta * pct;
	}

	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
	}

	private static String formatTargetNames(List<? extends LivingEntity> entities) {
		Map<String, Integer> counts = new java.util.LinkedHashMap<>();
		for (LivingEntity e : entities) {
			String dName = e.getDisplayName().getString();
			counts.put(dName, counts.getOrDefault(dName, 0) + 1);
		}
		StringBuilder namesSb = new StringBuilder();
		int cIdx = 0;
		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			if (cIdx > 0) namesSb.append(", ");
			if (entry.getValue() > 1) {
				namesSb.append(entry.getValue()).append(" ").append(entry.getKey());
			} else {
				namesSb.append(entry.getKey());
			}
			cIdx++;
		}
		return namesSb.toString();
	}

	public static double getAdaptationMultiplier(Entity entity) {
		double combatTicks = entity.getPersistentData().getDouble(K_SCT);
		double targetMaxTicks = 12000.0;
		if (entity instanceof Mob mob && mob.getTarget() instanceof LivingEntity target) {
			double hpRatio = target.getHealth() / Math.max(1.0f, target.getMaxHealth());
			if (hpRatio > 0.70) {
				targetMaxTicks = 18000.0;
			} else if (hpRatio < 0.25) {
				targetMaxTicks = 3600.0;
			} else if (hpRatio < 0.50) {
				targetMaxTicks = 6000.0;
			}
			if (target.isUsingItem()) {
				targetMaxTicks *= 0.6;
			}
		}
		double fraction = Math.min(1.0, combatTicks / Math.max(600.0, targetMaxTicks));
		double easeInQuart = fraction * fraction * fraction * fraction;
		return 1.0 + (easeInQuart * 49.0);
	}

	private static Player getFollowPlayer(LevelAccessor world, Entity self) {
		String targetUuid = getRotPersistentString(self, K_MFTU, "");
		if (!(world instanceof ServerLevel level)) return null;
		if (targetUuid.isEmpty()) {
			List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, self.getBoundingBox().inflate(128.0), player -> player.isAlive());
			nearbyPlayers.sort(Comparator.comparingDouble(self::distanceToSqr));
			if (nearbyPlayers.isEmpty()) return null;
			Player nearestMaster = nearbyPlayers.get(0);
			targetUuid = nearestMaster.getUUID().toString();
			self.getPersistentData().putString(K_MFTU, targetUuid);
		}
		try {
			return level.getPlayerByUUID(UUID.fromString(targetUuid));
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static Player getGuardPlayer(LevelAccessor world, Entity self) {
		String targetUuid = getRotPersistentString(self, "master_guard_target_uuid", "");
		if (targetUuid.isEmpty() || !(world instanceof ServerLevel level)) return null;
		try {
			return level.getPlayerByUUID(UUID.fromString(targetUuid));
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static Entity findGuardThreat(LevelAccessor world, Entity self, Player guardPlayer) {
		AABB box = guardPlayer.getBoundingBox().inflate(128.0);
		List<LivingEntity> threats = world.getEntitiesOfClass(LivingEntity.class, box, candidate -> {
			if (candidate == self || candidate == guardPlayer || !candidate.isAlive() || candidate instanceof Player) return false;
			if (candidate instanceof Mob mob && mob.getTarget() == guardPlayer) return true;
			return candidate.getLastHurtByMob() == guardPlayer || candidate.getLastHurtMob() == guardPlayer
				|| guardPlayer.getLastHurtByMob() == candidate || guardPlayer.getLastHurtMob() == candidate;
		});
		threats.sort(java.util.Comparator.comparingDouble(candidate -> guardPlayer.distanceToSqr(candidate)));
		return threats.isEmpty() ? null : threats.get(0);
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}



	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (world != null && world.isClientSide()) {
			if (entity instanceof RotEntity rot) {
				int solarCharge = rot.getEntityData().get(RotEntity.DATA_sentinel_solar_charge_ticks);
				int cryoCharge = rot.getEntityData().get(RotEntity.DATA_sentinel_cryo_charge_ticks);
				boolean firingOrCharging = rot.getEntityData().get(RotEntity.DATA_is_laser_firing);
				boolean charging = solarCharge > 0 || cryoCharge > 0;
				boolean firing = firingOrCharging && !charging;

				boolean hadLaser = getRotPersistentBoolean(entity, K_CHL, false);
				double closingTicks = entity.getPersistentData().getDouble(K_CLCT);
				if (hadLaser && !firingOrCharging) {
					closingTicks = LASER_CLOSING_TICKS;
				} else if (closingTicks > 0) {
					closingTicks = Math.max(0, closingTicks - 1);
				}
				entity.getPersistentData().putBoolean(K_CHL, firingOrCharging);
				entity.getPersistentData().putDouble(K_CLCT, closingTicks);
				boolean closing = closingTicks > 0 && !firingOrCharging;

				entity.getPersistentData().putBoolean(K_LC, charging);
				entity.getPersistentData().putBoolean(K_ILC, charging);
				entity.getPersistentData().putBoolean(K_LF, firing);
				entity.getPersistentData().putBoolean(K_ILF, firing);
				entity.getPersistentData().putBoolean(K_LC2, closing);
				entity.getPersistentData().putBoolean(K_ILC2, closing);
				try {
					rot.getEntityData().set(RotEntity.DATA_is_laser_closing, closing);
				} catch (Exception e) {}

				boolean leftPunching = rot.getEntityData().get(RotEntity.DATA_is_left_punching);
				entity.getPersistentData().putBoolean(K_ILP, leftPunching);

				boolean rightPunching = rot.getEntityData().get(RotEntity.DATA_is_right_punching);
				entity.getPersistentData().putBoolean(K_IRP, rightPunching);

				boolean airborneState = rot.getEntityData().get(RotEntity.DATA_is_airborne_state);
				entity.getPersistentData().putBoolean(K_IAS, airborneState);
				entity.getPersistentData().putBoolean(K_IAT, airborneState);
				entity.getPersistentData().putBoolean(K_SIAS, airborneState);

				boolean fallingHeavy = rot.getEntityData().get(RotEntity.DATA_is_falling_heavy);
				entity.getPersistentData().putBoolean(K_IFH, fallingHeavy);

				boolean sonicBoom = rot.getEntityData().get(RotEntity.DATA_is_sonic_boom);
				entity.getPersistentData().putBoolean(K_ISB, sonicBoom);

				boolean sonicBoomLarge = rot.getEntityData().get(RotEntity.DATA_is_sonic_boom_large);
				entity.getPersistentData().putBoolean(K_ISBL, sonicBoomLarge);

				boolean overheadPrep = rot.getEntityData().get(RotEntity.DATA_is_overhead_preparing);
				entity.getPersistentData().putBoolean(K_IOP, overheadPrep);

				boolean overhead = rot.getEntityData().get(RotEntity.DATA_is_overhead);
				entity.getPersistentData().putBoolean(K_IO, overhead);

				boolean slamCharge = rot.getEntityData().get(RotEntity.DATA_is_slam_charge);
				entity.getPersistentData().putBoolean(K_ISC, slamCharge);

				boolean groundCrush = rot.getEntityData().get(RotEntity.DATA_is_ground_crushing);
				entity.getPersistentData().putBoolean(K_IGC, groundCrush);

				if (entity.tickCount <= 1) {
					rot.getEntityData().set(RotEntity.DATA_is_rider_charging, false);
					rot.getEntityData().set(RotEntity.DATA_is_rider_kick, false);
				}
				boolean riderCharging = rot.getEntityData().get(RotEntity.DATA_is_rider_charging);
				entity.getPersistentData().putBoolean("is_rider_charging", riderCharging);
				boolean riderKick = rot.getEntityData().get(RotEntity.DATA_is_rider_kick);
				entity.getPersistentData().putBoolean("is_rider_kick", riderKick);

				boolean armorRipping = rot.getEntityData().get(RotEntity.DATA_is_armor_ripping);
				entity.getPersistentData().putBoolean(K_IAR, armorRipping);

				boolean isBlocking = rot.getEntityData().get(RotEntity.DATA_is_blocking);
				entity.getPersistentData().putBoolean(K_IB, isBlocking);

				boolean isBlockingFinish = rot.getEntityData().get(RotEntity.DATA_is_blocking_finish);
				entity.getPersistentData().putBoolean(K_IBF, isBlockingFinish);

				boolean isLand = rot.getEntityData().get(RotEntity.DATA_isLand);
				entity.getPersistentData().putBoolean(K_I, isLand);

				boolean isLand2 = rot.getEntityData().get(RotEntity.DATA_isLand2);
				entity.getPersistentData().putBoolean(K_I2, isLand2);

			}
			return;
		}
		if (!(entity instanceof RotEntity)) return;
		if (world instanceof ServerLevel serverLevel) monitorWitherSkullOutcome(serverLevel, entity);
		removeIrradiatedEffect(entity);
		try {
			executeInternal(event, world, x, y, z, entity);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			syncNBTFlags(entity);
		}
	}

	private static void removeIrradiatedEffect(Entity entity) {
		if (!(entity instanceof LivingEntity living)) return;
		BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("alexscaves:irradiated"))
			.ifPresent(living::removeEffect);
	}

	private static void monitorWitherSkullOutcome(ServerLevel level, Entity rot) {
		double checkTicks = getRotPersistentDouble(rot, K_SWSOT, 0.0);
		if (checkTicks <= 0.0) return;
		String targetUuid = rot.getPersistentData().getString(K_SWSOT2);
		Entity targetEntity;
		try {
			targetEntity = level.getEntity(UUID.fromString(targetUuid));
		} catch (IllegalArgumentException exception) {
			setRotPersistentDouble(rot, K_SWSOT, 0.0);
			return;
		}
		if (targetEntity instanceof LivingEntity target && target.isAlive()) {
			MobEffectInstance wither = target.getEffect(MobEffects.WITHER);
			double baselineDuration = getRotPersistentDouble(rot, K_SWSOB, 0.0);
			if (wither != null && wither.getDuration() > baselineDuration) {
				setRotPersistentDouble(rot, K_SWSOT, 0.0);
				return;
			}
		}
		checkTicks--;
		setRotPersistentDouble(rot, K_SWSOT, checkTicks);
		if (checkTicks <= 0.0 && targetEntity instanceof LivingEntity target && target.isAlive()) {
			recordWitherSkullFailure(rot, target);
		}
	}

	private static void executeInternal(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		boolean isDying = false;
		isDying = entity.getEntityData().get(net.mcreator.thebackwoods.entity.RotEntity.DATA_isDeath);

		if (!isDying) {
			isDying = entity.getPersistentData().getBoolean(K_RDSA);
		}
		if (!isDying && entity instanceof LivingEntity living && (living.getHealth() <= 20.0F || living.isDeadOrDying())) {
			isDying = true;
			entity.getEntityData().set(net.mcreator.thebackwoods.entity.RotEntity.DATA_isDeath, true);

			entity.getPersistentData().putBoolean(K_RDSA, true);
			entity.getPersistentData().putDouble(K_RDT2, 240.0);
			entity.getPersistentData().putDouble(K_RDSY, entity.getY());

			double randomTargetHeight = 7.0 + (Math.random() * 3.0);
			entity.getPersistentData().putDouble(K_RDTH, randomTargetHeight);

			double randomHoleSize = 8.0 + (Math.random() * 0.5);
			entity.getPersistentData().putDouble(K_RDHS, randomHoleSize);
		}

		if (isDying) {
			entity.getEntityData().set(net.mcreator.thebackwoods.entity.RotEntity.DATA_isDeath, true);

			if (entity instanceof LivingEntity living) {
				living.setHealth(1.0F);
				living.deathTime = 0;
				living.setInvulnerable(true);
				if (living instanceof Mob mob) {
					mob.setNoAi(true);
					mob.getNavigation().stop();
					mob.setTarget(null);
					mob.setLastHurtByMob(null);
				}
			}
			double deathTicks = entity.getPersistentData().getDouble(K_RDT2);
			if (deathTicks <= 0) {
				deathTicks = 240.0;
				entity.getPersistentData().putDouble(K_RDT2, 240.0);
			}
			
			cancelActiveCombosAndAbilities(entity);
			cleanupCombatFlags(entity);
			if (entity instanceof Mob mob) {
				mob.setTarget(null);
				mob.setLastHurtByMob(null);
			}

			double startY = entity.getPersistentData().getDouble(K_RDSY);
			if (startY == 0.0) {
				startY = entity.getY();
				entity.getPersistentData().putDouble(K_RDSY, startY);
			}
			double targetHeight = entity.getPersistentData().getDouble(K_RDTH);
			if (targetHeight < 7.0) {
				targetHeight = 7.0 + (Math.random() * 3.0);
				entity.getPersistentData().putDouble(K_RDTH, targetHeight);
			}

			double elapsedLevTicks = Math.min(100.0, 240.0 - deathTicks);
			if (elapsedLevTicks >= 0.0) {
				double levProgress = Math.min(1.0, elapsedLevTicks / 100.0);
				double smoothEase = levProgress * levProgress * (3.0 - 2.0 * levProgress);
				double desiredY = startY + (targetHeight * smoothEase);

				if (world instanceof Level lvl) {
					BlockPos abovePos = BlockPos.containing(entity.getX(), desiredY + entity.getBbHeight() + 0.2, entity.getZ());
					if (lvl.getBlockState(abovePos).isSolid()) {
						desiredY = Math.min(desiredY, entity.getY());
					}
				}

				entity.setPos(entity.getX(), desiredY, entity.getZ());
				entity.setDeltaMovement(0.0, 0.0, 0.0);
				entity.hasImpulse = true;
			}

			if (deathTicks <= 140.0) {
				if (!entity.getPersistentData().getBoolean(K_RDHS2)) {
					entity.getPersistentData().putBoolean(K_RDHS2, true);
					if (world instanceof Level lvl && !lvl.isClientSide()) {
						double holeSize = entity.getPersistentData().getDouble(K_RDHS);
						if (holeSize < 5.0) holeSize = 8.0 + (Math.random() * 0.5);
						
						double torsoY = entity.getY() + (entity.getBbHeight() * 0.5);
						BlackHole.spawnRotDeathHole(lvl, entity.getX(), torsoY, entity.getZ(), (float) holeSize, 7.0f);
					}
				}
			}

			if (deathTicks <= 1.0) {
				entity.getPersistentData().putDouble(K_RDT2, 0.0);
				if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
					double torsoY = entity.getY() + (entity.getBbHeight() * 0.5);
					serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, entity.getX(), torsoY, entity.getZ(), 4, 0.2, 0.2, 0.2, 0.0);
					serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM, entity.getX(), torsoY, entity.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
				}
				if (!entity.level().isClientSide()) {
					entity.discard();
				}
				return;
			} else {
				entity.getPersistentData().putDouble(K_RDT2, deathTicks - 1.0);
			}
			return;
		}

		double heat = entity.getPersistentData().getDouble(K_SLH);
		if (heat > 0) {
			entity.getPersistentData().putDouble(K_SLH, Math.max(0.0, heat - 0.25));
		}

		if (entity instanceof LivingEntity living) {
			BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("legendary_monsters:soul_fracture")).ifPresent(soulFractureHolder -> {
				if (living.hasEffect(soulFractureHolder)) {
					living.removeEffect(soulFractureHolder);
					if (world instanceof ServerLevel level) {
						level.sendParticles(ParticleTypes.SOUL, living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(), 15, 0.3, 0.3, 0.3, 0.03);
						level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
					}
				}
			});

			if (getRotPersistentBoolean(living, K_USB, false)) {
				living.clearFire();
				living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0, false, false));

				BlockPos feetPos = living.blockPosition();
				BlockPos belowPos = feetPos.below();
				BlockState feetState = living.level().getBlockState(feetPos);
				BlockState belowState = living.level().getBlockState(belowPos);
				boolean inLava = feetState.getFluidState().is(FluidTags.LAVA);
				boolean onLava = belowState.getFluidState().is(FluidTags.LAVA);

				if (inLava || onLava) {
					living.setOnGround(true);
					living.fallDistance = 0.0F;
					Vec3 curDelta = living.getDeltaMovement();
					if (inLava) {
						living.setDeltaMovement(curDelta.x * 1.15, Math.max(0.12, curDelta.y + 0.15), curDelta.z * 1.15);
					} else if (onLava && curDelta.y < 0) {
						living.setDeltaMovement(curDelta.x * 1.05, 0.0, curDelta.z * 1.05);
					}
				}
			}
			if (getRotPersistentBoolean(living, K_UCB, false)) {
				living.setTicksFrozen(0);

				BlockPos feetPos = living.blockPosition();
				BlockPos belowPos = feetPos.below();
				BlockState feetState = living.level().getBlockState(feetPos);
				BlockState belowState = living.level().getBlockState(belowPos);
				boolean inPowderSnow = feetState.is(Blocks.POWDER_SNOW);
				boolean onPowderSnow = belowState.is(Blocks.POWDER_SNOW);

				if (inPowderSnow || onPowderSnow) {
					living.setOnGround(true);
					living.fallDistance = 0.0F;
					Vec3 curDelta = living.getDeltaMovement();
					if (inPowderSnow) {
						living.setDeltaMovement(curDelta.x * 1.25, Math.max(0.1, curDelta.y + 0.12), curDelta.z * 1.25);
					} else if (onPowderSnow && curDelta.y < 0) {
						living.setDeltaMovement(curDelta.x, 0.0, curDelta.z);
					}
				}
			}

			boolean isExecutingAirAbility = living.getPersistentData().getDouble(K_SSP) > 0
				|| living.getPersistentData().getDouble(K_SJT) > 0
				|| living.getPersistentData().getDouble(K_SDKP) > 0
				|| living.getPersistentData().getDouble(K_ROT) > 0
				|| living.getPersistentData().getDouble("sentinel_uppercut_launch_ticks") > 0
				|| living.getPersistentData().getDouble(K_SCS2) > 0
				|| living.getPersistentData().getDouble(K_SCS) > 0
				|| living.getPersistentData().getDouble(K_SCS3) > 0
				|| living.getPersistentData().getDouble(K_SCS4) > 0
				|| living.getPersistentData().getDouble(K_SCS5) > 0
				|| living.getPersistentData().getBoolean("sentinel_is_air_maneuvering")
				|| living.getPersistentData().getBoolean(K_IU)
				|| getRotPersistentBoolean(living, K_DFO, false)
				|| getRotPersistentBoolean(living, K_DFR, false);

			if (!isExecutingAirAbility && !living.isInWater() && !living.isInLava()) {
				boolean hasLevitation = living.hasEffect(MobEffects.LEVITATION);
				boolean hasInvoluntaryLift = !living.onGround() && living.getDeltaMovement().y() > 0.08;
				boolean isPermanentlyAdapted = getRotPersistentBoolean(living, K_AGM, false);

				if (hasLevitation || hasInvoluntaryLift) {
					double floatAdaptTicks = living.getPersistentData().getDouble(K_RIFT) + 1;
					living.getPersistentData().putDouble(K_RIFT, floatAdaptTicks);

					if (isPermanentlyAdapted) {
						if (hasLevitation) {
							living.removeEffect(MobEffects.LEVITATION);
						}
						Vec3 curDelta = living.getDeltaMovement();
						living.setDeltaMovement(curDelta.x() * 0.94, -0.90, curDelta.z() * 0.94);
						living.hasImpulse = true;
						living.getPersistentData().putBoolean(K_RFGA, true);
					} else {
						if (floatAdaptTicks >= 25) {
							double progress = Math.min(1.0, (floatAdaptTicks - 25) / 25.0);
							Vec3 curDelta = living.getDeltaMovement();
							double pullY = -0.15 - (0.75 * progress);
							living.setDeltaMovement(curDelta.x() * 0.95, Math.min(curDelta.y(), pullY), curDelta.z() * 0.95);
							living.hasImpulse = true;
							living.getPersistentData().putBoolean(K_RFGA, true);

							if (floatAdaptTicks >= 45) {
								if (hasLevitation) living.removeEffect(MobEffects.LEVITATION);
								setRotPersistentBoolean(living, K_AGM, true);
								announceLearnedAbility(living);
							}
						}
					}
				} else {
					living.getPersistentData().putDouble(K_RIFT, Math.max(0, living.getPersistentData().getDouble(K_RIFT) - 2));
				}
			} else {
				living.getPersistentData().putDouble(K_RIFT, 0);
			}

			if (living.onGround()) {
				if (living.getPersistentData().getBoolean(K_RFGA)) {
					living.getPersistentData().putBoolean(K_RFGA, false);
					living.getPersistentData().putDouble(K_RIFT, 0);
					if (world instanceof ServerLevel sLevel) {
						sLevel.sendParticles(ParticleTypes.CRIT, living.getX(), living.getY() + 0.1, living.getZ(), 16, 0.4, 0.1, 0.4, 0.15);
						BlockState groundState = living.level().getBlockState(living.blockPosition().below());
						if (!groundState.isAir()) {
							try {
								sLevel.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.DUST_PILLAR, groundState), living.getX(), living.getY() + 0.1, living.getZ(), 12, 0.3, 0.1, 0.3, 0.05);
							} catch (Exception ignored) {}
						}
						playHostileSound(sLevel, living.getX(), living.getY(), living.getZ(), "entity.wind_charge.wind_burst", 1.2F, 0.6F);
						
						AABB miniAABB = living.getBoundingBox().inflate(3.5, 1.5, 3.5);
						List<LivingEntity> nearbyVictims = sLevel.getEntitiesOfClass(LivingEntity.class, miniAABB, e -> e != living && !isWoodboundEntity(e, living));
						for (LivingEntity v : nearbyVictims) {
							dealTrueDamageToBosses(v, new DamageSource(sLevel.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_seismic_slam"))), living), 8.0F * (float) getAdaptationMultiplier(living));
							v.setDeltaMovement(v.getDeltaMovement().x() * 0.5, 0.35, v.getDeltaMovement().z() * 0.5);
						}
					}
				}
			}
		}

			if (entity instanceof net.minecraft.world.entity.Mob mob) {
				BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("mowziesmobs:frozen")).ifPresent(frozenHolder -> {
					if (mob.hasEffect(frozenHolder)) {
						if (getRotPersistentBoolean(mob, K_UCB, false)) {
							mob.removeEffect(frozenHolder);
							mob.setNoAi(false);
							return;
						}
						int frozenTicks = mob.getPersistentData().getInt(K_SFT);
						int breakTicks = mob.getPersistentData().getInt(K_SFBT);
						if (breakTicks == 0) {
							breakTicks = 60 + mob.getRandom().nextInt(141);
							mob.getPersistentData().putInt(K_SFBT, breakTicks);
						}
						
						frozenTicks++;
						mob.getPersistentData().putInt(K_SFT, frozenTicks);

						if (world instanceof ServerLevel level) {
							double progress = (double) frozenTicks / breakTicks;
							if (progress > 0.3) {
								int frequency = (int) (20 * (1.0 - progress)) + 2;
								if (frozenTicks % frequency == 0) {
									float volume = (float) (0.2 + 0.8 * progress);
									float pitch = (float) (0.8 + 0.4 * progress);
									playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "block.fire.extinguish", volume, pitch);
									level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX(), entity.getY() + 1.0 + mob.getRandom().nextDouble(), entity.getZ(), (int)(5 * progress) + 1, 0.4, 0.4, 0.4, 0.02);
								}
							}
						}

						if (frozenTicks >= breakTicks) {
							mob.removeEffect(frozenHolder);
							mob.setNoAi(false);
							mob.getPersistentData().putInt(K_SFT, 0);
							mob.getPersistentData().putInt(K_SFBT, 0);
							if (world instanceof ServerLevel level) {
								playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "block.fire.extinguish", 1.0F, 1.0F);
								level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX(), entity.getY() + 1.5, entity.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
								level.sendParticles(ParticleTypes.LAVA, entity.getX(), entity.getY() + 1.5, entity.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
							}
						}
					} else {
						mob.getPersistentData().putInt(K_SFT, 0);
						mob.getPersistentData().putInt(K_SFBT, 0);
					}
				});
				if (mob.isNoAi() && BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("mowziesmobs:frozen")).map(frozenHolder -> !mob.hasEffect(frozenHolder)).orElse(true)) {
					if (entity.tickCount > 20 && !getRotPersistentBoolean(entity, "mapmaker_noai", false)) {
						mob.setNoAi(false);
					}
				}
			}

			if (!getRotPersistentBoolean(entity, K_SSI, false)) {
			entity.getPersistentData().putBoolean(K_SSI, true);
			if (getRotPersistentBoolean(entity, K_SSS, false)) {
				if (entity.getPersistentData().getDouble(K_SSP) == 0) {
					entity.getPersistentData().putDouble(K_SST4, 60);
					entity.getPersistentData().putDouble(K_SSMT, 60.0);
					entity.getPersistentData().putDouble(K_SSBY, entity.getYRot());
				}
			}
		}

		if (handleScanningState(entity)) {
			return;
		}

		if (getRotPersistentBoolean(entity, K_STA2, false) && entity.tickCount % 4 == 0) {
			if (world instanceof ServerLevel level) {
				for (int i = 0; i < 3; i++) {
					double theta = Math.random() * Math.PI * 2;
					double phi = Math.acos(Math.random() * 2 - 1);
					double radius = 1.8;
					double px = Math.sin(phi) * Math.cos(theta) * radius;
					double py = Math.sin(phi) * Math.sin(theta) * radius + 1.2;
					double pz = Math.cos(phi) * radius;
					double vx = -px * 0.08;
					double vy = -(py - 1.2) * 0.08;
					double vz = -pz * 0.08;
					level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX() + px, entity.getY() + py, entity.getZ() + pz, 0, vx, vy, vz, 0.4);
				}
			}
		}

		if (entity instanceof LivingEntity living) {
			if (living.getLastHurtByMob() != null) {
				Entity attacker = living.getLastHurtByMob();
				if (BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).toString().equals("spore:scent")) {
					living.setLastHurtByMob(null);
				} else if (!shouldIgnoreCombatFilter(entity) && !shouldIgnoreCombatFilter(attacker) && (attacker instanceof RotEntity || attacker.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(K_WOODBOUND))))) {
					living.setLastHurtByMob(null);
				}
			}
			if (living.isInWater() || living.isInLava()) {
				boolean isSuperheating = getRotPersistentDouble(living, K_RSC, 0.0) > 0 || getRotPersistentDouble(living, K_RSA, 0.0) > 0;
				if (!isSuperheating && living instanceof Mob mob && mob.getTarget() != null) {
					Entity target = mob.getTarget();
					Vec3 targetEye = target.getEyePosition();
					Vec3 livingEye = living.getEyePosition();
					Vec3 dir = targetEye.subtract(livingEye);
					double dist = dir.length();

					if (dist > 0.001) {
						Vec3 look = dir.normalize();
						Vec3 mv = living.getDeltaMovement();
						double horizDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);

						double swimSpeedHoriz = 0.04;
						double swimSpeedVert = 0.04;

						double newX = mv.x * 0.82 + look.x * swimSpeedHoriz;
						double newZ = mv.z * 0.82 + look.z * swimSpeedHoriz;
						double newY = mv.y * 0.82 + look.y * swimSpeedVert;

						if (dir.y > 0.8) {
							living.setJumping(true);
						} else {
							living.setJumping(false);
						}

						if (horizDist < 0.6) {
							newX = mv.x * 0.5;
							newZ = mv.z * 0.5;
						}

						living.setDeltaMovement(newX, newY, newZ);
					}
				} else {
					living.setJumping(false);
				}
			}
		}
		if (entity instanceof Mob mob) {
			if (mob.getTarget() != null) {
				Entity t = mob.getTarget();
				boolean isRetaliation = (mob.getLastHurtByMob() == t || (t instanceof Mob tm && tm.getTarget() == mob));
				if (!shouldIgnoreCombatFilter(entity) && !shouldIgnoreCombatFilter(t) && (t instanceof RotEntity || t.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(K_WOODBOUND))))) {
					mob.setTarget(null);
				} else if (!isRetaliation && !shouldIgnoreCombatFilter(entity) && !shouldIgnoreCombatFilter(t) && (t instanceof Villager || t instanceof AmbientCreature || t instanceof Animal || t instanceof Slime || t instanceof net.minecraft.world.entity.animal.WaterAnimal)) {
					mob.setTarget(null);
				}
			}
		}

		double landingTicksBefore = entity.getPersistentData().getDouble(K_SLT);

		tickCooldowns(entity);

		if (ENABLE_PHASE_SHIFT) {
			double phaseTicks = entity.getPersistentData().getDouble(K_RPT);
			if (phaseTicks > 0) {
				entity.getPersistentData().putBoolean(K_RPS, true);
				if (entity.tickCount % 20 == 0) {
					double currentMastery = entity.getPersistentData().getDouble(K_RPM);
					if (currentMastery < 1.0) {
						entity.getPersistentData().putDouble(K_RPM, Math.min(1.0, currentMastery + 0.010));
					}
				}
			} else if (entity.getPersistentData().getBoolean(K_RPS)) {
				entity.getPersistentData().putBoolean(K_RPS, false);
				entity.noPhysics = false;
				entity.getPersistentData().putDouble(K_RPC, 200.0);
			}
		} else if (entity.getPersistentData().getBoolean(K_RPS)) {
			entity.getPersistentData().putBoolean(K_RPS, false);
			entity.noPhysics = false;
		}

		if (ENABLE_BLOCKING) {
			if (getRotPersistentBoolean(entity, K_DFB, false)) {
				entity.getPersistentData().putBoolean(K_DFB, false);
				double minTicks = BLOCK_MIN_TICKS;
				double maxTicks = BLOCK_MAX_TICKS;
				double blockTicks = minTicks + Math.random() * (maxTicks - minTicks);
				entity.getPersistentData().putDouble(K_RBAT, blockTicks);
				entity.getPersistentData().putBoolean(K_IB, true);
			}

			double blockActiveTicks = entity.getPersistentData().getDouble(K_RBAT);
			if (blockActiveTicks > 0) {
				blockActiveTicks--;
				entity.getPersistentData().putDouble(K_RBAT, blockActiveTicks);
				entity.getPersistentData().putBoolean(K_IB, true);
				if (blockActiveTicks == 0) {
					entity.getPersistentData().putBoolean(K_IB, false);
					entity.getPersistentData().putBoolean(K_IBF, true);
					entity.getPersistentData().putDouble(K_RBFT, 5);
					entity.getPersistentData().putDouble(K_RBC, 300);

					if (Math.random() < 0.5) {
						Entity followTarget = acquireTarget(world, entity, entity.getX(), entity.getY(), entity.getZ());
						if (followTarget instanceof LivingEntity targetLiv && targetLiv.isAlive()) {
							double moveRand = Math.random();
							if (moveRand < 0.33) {
								entity.getPersistentData().putDouble(K_SSP, 1);
								entity.getPersistentData().putDouble(K_SST2, 22);
								entity.getPersistentData().putDouble(K_ROT, 0);
								entity.setDeltaMovement(0.0, 1.9, 0.0);
								entity.hasImpulse = true;
								if (world instanceof ServerLevel level) {
									playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.iron_golem.attack", 1.5F, 0.8F);
									level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
								}
							} else if (moveRand < 0.66) {
								if (entity.distanceTo(targetLiv) >= 6.0) {
									entity.getPersistentData().putDouble(K_SJT, 60);
									entity.getPersistentData().putDouble(K_SSP, 0);
									entity.getPersistentData().putDouble(K_ROT, 0);
									entity.getPersistentData().putDouble(K_SDKP, 0);
									entity.setDeltaMovement(0.0, 0.0, 0.0);
									entity.hasImpulse = true;
								} else {
									executeMinosHeavyPunchBlink(world, entity, targetLiv, true);
								}
							} else {
								entity.getPersistentData().putDouble(K_ROT, OVERHEAD_TOTAL_TICKS);
								entity.getPersistentData().putDouble(K_SSP, 0);
								entity.getPersistentData().putString(K_OTU, targetLiv.getUUID().toString());
							}
						}
					}
				}
			} else {
				if (getRotPersistentBoolean(entity, K_IB, false)) {
					entity.getPersistentData().putBoolean(K_IB, false);
				}
			}
		} else {
			if (getRotPersistentBoolean(entity, K_IB, false)) {
				entity.getPersistentData().putBoolean(K_IB, false);
				entity.getPersistentData().putDouble(K_RBAT, 0.0);
			}
			entity.getPersistentData().putBoolean(K_DFB, false);
		}

		double blockFinishTicks = entity.getPersistentData().getDouble(K_RBFT);
		if (blockFinishTicks > 0) {
			blockFinishTicks--;
			entity.getPersistentData().putDouble(K_RBFT, blockFinishTicks);
			if (blockFinishTicks == 0) {
				entity.getPersistentData().putBoolean(K_IBF, false);
			}
		}

		double dpsTickCounter = entity.getPersistentData().getDouble(K_RDTC) + 1;
		if (dpsTickCounter >= 20) {
			dpsTickCounter = 0;
			double sec0 = entity.getPersistentData().getDouble(K_RDTS);
			double sec1 = entity.getPersistentData().getDouble(K_RDS0);
			double sec2 = entity.getPersistentData().getDouble(K_RDS1);
			double sec3 = entity.getPersistentData().getDouble(K_RDS2);
			double sec4 = entity.getPersistentData().getDouble(K_RDS3);

			entity.getPersistentData().putDouble("rot_dmg_sec_4", sec4);
			entity.getPersistentData().putDouble(K_RDS3, sec3);
			entity.getPersistentData().putDouble(K_RDS2, sec2);
			entity.getPersistentData().putDouble(K_RDS1, sec1);
			entity.getPersistentData().putDouble(K_RDS0, sec0);
			entity.getPersistentData().putDouble(K_RDTS, 0);
		}
		entity.getPersistentData().putDouble(K_RDTC, dpsTickCounter);

		double landingTicksAfter = entity.getPersistentData().getDouble(K_SLT);
		if (landingTicksBefore > 0 && landingTicksAfter <= 0 && getRotPersistentBoolean(entity, K_SISLA, false)) {
			entity.getPersistentData().putBoolean(K_SISLA, false);
			if (getRotPersistentBoolean(entity, K_SSS, false)) {
				int summons = entity.getPersistentData().getInt("sentinel_spore_summons_at_birth");
				double scanTicks = Math.max(15.0, 60.0 - (summons - 1) * 15.0);
				entity.getPersistentData().putDouble(K_SST4, scanTicks);
				entity.getPersistentData().putDouble(K_SSMT, scanTicks);
				entity.getPersistentData().putDouble(K_SSBY, entity.getYRot());
			}
		}

		double rotLandTimer = entity.getPersistentData().getDouble(K_RLT);
		if (rotLandTimer > 0) {
			rotLandTimer--;
			entity.getPersistentData().putDouble(K_RLT, rotLandTimer);
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
				mob.setSpeed(0.0F);
				mob.xxa = 0.0F;
				mob.zza = 0.0F;
			}
			entity.setDeltaMovement(0, Math.min(0, entity.getDeltaMovement().y()), 0);
			entity.hasImpulse = true;
			if (rotLandTimer <= 0) {
				if (entity instanceof RotEntity rot) {
					rot.getEntityData().set(RotEntity.DATA_isLand, false);
					rot.getEntityData().set(RotEntity.DATA_isLand2, false);
				}
				entity.getPersistentData().putBoolean(K_I, false);
				entity.getPersistentData().putBoolean(K_I2, false);
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double shockwaveStage = entity.getPersistentData().getDouble(K_SSS2);
		if (shockwaveStage > 0) {
			double originX = entity.getPersistentData().getDouble(K_SSX);
			double originY = entity.getPersistentData().getDouble(K_SSY);
			double originZ = entity.getPersistentData().getDouble(K_SSZ);
			double radius = shockwaveStage * 2.2;
			boolean isVertical = getRotPersistentBoolean(entity, K_SSV, false);

			if (world instanceof ServerLevel level) {
				int particleCount = Math.max(1, (int) (2 * Math.PI * radius * 7.5 * Mth.clamp(PARTICLE_QUALITY, 0.1, 1.0)));
				net.minecraft.world.level.block.state.BlockState floorState = level.getBlockState(net.minecraft.core.BlockPos.containing(originX, originY - 0.5, originZ));
				if (floorState.isAir()) {
					floorState = level.getBlockState(net.minecraft.core.BlockPos.containing(originX, originY - 1.5, originZ));
				}
				if (floorState.isAir()) {
					floorState = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
				}
				net.minecraft.core.particles.BlockParticleOption dustPillarOptions = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.DUST_PILLAR, floorState);
				net.minecraft.core.particles.ParticleType<?> _tsdType = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse("trial_spawner_detection"));
				net.minecraft.core.particles.ParticleOptions trialSpawnerDetection = _tsdType instanceof net.minecraft.core.particles.ParticleOptions _tsdOpt ? _tsdOpt : net.minecraft.core.particles.ParticleTypes.EFFECT;

				for (int i = 0; i < particleCount; i++) {
					double angle = (2 * Math.PI / particleCount) * i;
					double cos = Math.cos(angle);
					double sin = Math.sin(angle);

					if (isVertical) {
						double yaw = entity.getPersistentData().contains(K_SSY2)
							? entity.getPersistentData().getDouble(K_SSY2)
							: entity.getYRot();
						double yawRad = Math.toRadians(yaw);
						double cosYaw = Math.cos(yawRad);
						double sinYaw = Math.sin(yawRad);

						double r = radius + (Math.random() - 0.5) * 0.8;
						double px = originX + cos * r * sinYaw;
						double py = originY + sin * r;
						double pz = originZ - cos * r * cosYaw;

						double spread = 0.85 + Math.random() * 0.3;
						double vx = cos * sinYaw * (2.2 * spread);
						double vy = sin * (2.2 * spread);
						double vz = -cos * cosYaw * (2.2 * spread);

						level.sendParticles(dustPillarOptions, px, py, pz, 0, vx, vy, vz, 1.0);
						level.sendParticles(trialSpawnerDetection, px, py, pz, 0, vx * 0.5, vy * 0.5, vz * 0.5, 1.0);
					} else {
						double r = radius + (Math.random() - 0.5) * 0.8;
						double px = originX + cos * r;
						double pz = originZ + sin * r;

						double spread = 0.85 + Math.random() * 0.3;
						double vx = cos * (2.2 * spread);
						double vz = sin * (2.2 * spread);

						level.sendParticles(dustPillarOptions, px, originY + 0.15, pz, 0, vx, 0.02, vz, 1.0);
						level.sendParticles(trialSpawnerDetection, px, originY + 0.15, pz, 0, vx * 0.5, 0.02, vz * 0.5, 1.0);

						if (i % 2 == 0) {
							double rCloud = radius + (Math.random() - 0.5) * 0.8;
							double pxCloud = originX + cos * rCloud;
							double pzCloud = originZ + sin * rCloud;

							double spreadCloud = 0.45 + Math.random() * 0.2;
							double vxCloud = cos * (2.2 * spreadCloud);
							double vzCloud = sin * (2.2 * spreadCloud);

							level.sendParticles(dustPillarOptions, pxCloud, originY + 0.1, pzCloud, 0, vxCloud, 0.02, vzCloud, 1.0);
						}
					}
				}
				if (shockwaveStage % 2 == 1) {
					playHostileSound(level, originX, originY, originZ, "entity.wind_charge.wind_burst", 1.4F, 0.7F - ((float) shockwaveStage * 0.03F));
				}

				AABB checkArea = new AABB(originX - radius - 1.5, originY - radius - 1.5, originZ - radius - 1.5, originX + radius + 1.5, originY + radius + 1.5, originZ + radius + 1.5);

				java.util.List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, checkArea, e -> e != entity && !isWoodboundEntity(e, entity));
				for (LivingEntity victim : targets) {
					double dist = Math.sqrt(victim.distanceToSqr(originX, originY, originZ));
					if (Math.abs(dist - radius) <= 1.5) {
						dealTrueDamageToBosses(victim, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_seismic_slam"))), entity), 12.0F * (float) getAdaptationMultiplier(entity));
						victim.setDeltaMovement(victim.getDeltaMovement().x(), 0.65, victim.getDeltaMovement().z());
						victim.hasImpulse = true;
						level.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + 0.5, victim.getZ(), 1, 0, 0, 0, 0);
					}
				}
			}

			if (shockwaveStage >= 8) {
				entity.getPersistentData().putDouble(K_SSS2, 0);
				entity.getPersistentData().putBoolean(K_SSV, false);
			} else {
				entity.getPersistentData().putDouble(K_SSS2, shockwaveStage + 1);
			}
		}

		double landingTicks = entity.getPersistentData().getDouble(K_SLT);
		if (landingTicks > 0) {
			entity.setDeltaMovement(0, -0.05, 0);
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			if (entity instanceof LivingEntity living) {
				living.setYBodyRot(living.getYRot());
			}
			double cc1 = entity.getPersistentData().getDouble(K_SCS2);
			double cc2 = entity.getPersistentData().getDouble(K_SCS);
			double cc3 = entity.getPersistentData().getDouble(K_SCS3);
			double cc4 = entity.getPersistentData().getDouble(K_SCS4);
			double cc5 = entity.getPersistentData().getDouble(K_SCS5);
			if (cc1 <= 0 && cc2 <= 0 && cc3 <= 0 && cc4 <= 0 && cc5 <= 0) {
				return;
			}
		}

		if (entity instanceof LivingEntity living) {
			boolean inColdBiome = false;
			if (living.tickCount % 40 == 0 || !living.getPersistentData().contains(K_SCICB)) {
				inColdBiome = living.level().getBiome(living.blockPosition()).unwrapKey().map(key -> {
					String path = key.location().getPath().toLowerCase(java.util.Locale.ROOT);
					return path.contains("snow") || path.contains("frozen") || path.contains("ice") || path.contains("cold");
				}).orElse(false);
				living.getPersistentData().putBoolean(K_SCICB, inColdBiome);

			} else {
				inColdBiome = getRotPersistentBoolean(living, K_SCICB, false);
			}
			if (inColdBiome) {
				double coldTime = living.getPersistentData().getDouble(K_STICB);
				living.getPersistentData().putDouble(K_STICB, coldTime + 1.0);
			}
			if (living.level().dimension() == net.minecraft.world.level.Level.NETHER) {
				double netherTime = living.getPersistentData().getDouble(K_STIN);
				living.getPersistentData().putDouble(K_STIN, netherTime + 1.0);
			}
		}

		Entity combatTarget = acquireTarget(world, entity, x, y, z);
		if (combatTarget == null || !combatTarget.isAlive() || combatTarget.isRemoved()) {
			cleanupCombatFlags(entity);
		}

		boolean isRipping = getRotPersistentDouble(entity, K_RART, 0.0) > 0 || getRotPersistentBoolean(entity, K_IAR, false);
		boolean isBlocking = getRotPersistentDouble(entity, K_RBAT, 0.0) > 0 || getRotPersistentBoolean(entity, K_IB, false);
		boolean isTotemInspecting = getRotPersistentDouble(entity, K_STIT, 0.0) > 0;

		if (isRipping) {
			setRotPersistentBoolean(entity, K_IB, false);
			setRotPersistentDouble(entity, K_RBAT, 0);
			setRotPersistentDouble(entity, K_STIT, 0);
		} else if (isBlocking) {
			setRotPersistentDouble(entity, K_RART, 0);
			setRotPersistentBoolean(entity, K_IAR, false);
			setRotPersistentDouble(entity, K_STIT, 0);
		} else if (isTotemInspecting) {
			setRotPersistentBoolean(entity, K_IB, false);
			setRotPersistentDouble(entity, K_RBAT, 0);
			setRotPersistentDouble(entity, K_RART, 0);
			setRotPersistentBoolean(entity, K_IAR, false);
		}

		boolean inCombat = combatTarget != null || (entity instanceof LivingEntity living ? living.getLastHurtByMob() != null : false) || entity.getPersistentData().getDouble(K_SRD) > 0.0 || getRotPersistentBoolean(entity, K_IB, false) || !"NONE".equals(entity.getPersistentData().getString(K_SPTL));
		handleAdaptationScaling(entity, inCombat);

		if (combatTarget instanceof LivingEntity livTarget) {
			UniversalCombatPredictionEngine.tickPrediction(world, entity, livTarget);
		} else {
			UniversalCombatPredictionEngine.clearPrediction(entity);
		}

		if (combatTarget instanceof LivingEntity livTarget) {
			boolean isFlying = livTarget.isFallFlying() || (!livTarget.onGround() && livTarget.getY() > entity.getY() + 2.0 && !livTarget.isInWater() && !livTarget.isInLava());
			boolean isPillaring = isTargetPillaring(world, livTarget, entity);
			boolean isUnreachable = (isFlying || isPillaring) && (entity.distanceTo(livTarget) > 4.5 || livTarget.getY() > entity.getY() + 2.5);
			if (isUnreachable) {
				double flyTicks = entity.getPersistentData().getDouble(K_TUFT) + 1.0;
				entity.getPersistentData().putDouble(K_TUFT, flyTicks);
				
				if (flyTicks >= 120.0 && !isChannelingAbility(entity) && getRotPersistentBoolean(entity, K_UOC, false)) {
					double chance = Math.min(0.50, (flyTicks - 120.0) * 0.002);
					if (entity.getRandom().nextDouble() < chance) {
						entity.getPersistentData().putDouble(K_ROT, OVERHEAD_TOTAL_TICKS);
						entity.getPersistentData().putString(K_OTU, livTarget.getUUID().toString());
						entity.getPersistentData().putDouble(K_TUFT, 0.0);
					}
				}
			} else {
				entity.getPersistentData().putDouble(K_TUFT, 0.0);
			}
		} else {
			entity.getPersistentData().putDouble(K_TUFT, 0.0);
		}

		if (interceptEnderPearlsPipeline(world, entity, combatTarget)) {
			handlePassengerAndGrowth(entity);
			return;
		}

		double totemInspectTicks = getRotPersistentDouble(entity, K_STIT, 0.0);
		if (totemInspectTicks > 0) {
			if (entity instanceof LivingEntity living) {
				boolean isInfinity = getRotPersistentBoolean(entity, K_SIIT, false);
				net.minecraft.world.item.Item infinityTotemItem = isInfinity ? BuiltInRegistries.ITEM.get(ResourceLocation.parse("avaritia:infinity_totem")) : null;
				net.minecraft.world.item.ItemStack stackToHold = (infinityTotemItem != null) ? new net.minecraft.world.item.ItemStack(infinityTotemItem) : new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
				living.setItemInHand(InteractionHand.MAIN_HAND, stackToHold);
			}
			entity.getPersistentData().putDouble(K_STIT, totemInspectTicks - 1.0);
			entity.setDeltaMovement(0.0, entity.getDeltaMovement().y(), 0.0);
			entity.hurtMarked = true;
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
				mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0.0);
			}

			Vec3 lookDownTarget = entity.position().add(entity.getViewVector(1.0F).scale(0.5)).add(0, entity.getBbHeight() * 0.35, 0);
			Vec3 targetPos = null;
			if (combatTarget != null) {
				targetPos = new Vec3(combatTarget.getX(), combatTarget.getY() + combatTarget.getBbHeight() * 0.75, combatTarget.getZ());
			} else {
				targetPos = entity.position().add(entity.getViewVector(1.0F).scale(4.0)).add(0, entity.getBbHeight() * 0.75, 0);
			}
			double w = 1.0;
			if (totemInspectTicks > 150.0) {
				w = (180.0 - totemInspectTicks) / 30.0;
			} else if (totemInspectTicks < 40.0) {
				w = totemInspectTicks / 40.0;
			}
			Vec3 finalLookTarget = new Vec3(
				targetPos.x + (lookDownTarget.x - targetPos.x) * w,
				targetPos.y + (lookDownTarget.y - targetPos.y) * w,
				targetPos.z + (lookDownTarget.z - targetPos.z) * w
			);
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, finalLookTarget);

			if (totemInspectTicks > 60.0) {
				if (totemInspectTicks % 20 == 0) {
					playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sniff", 1.2F, 0.65F);
				}
			} else {
				if (totemInspectTicks % 10 == 0) {
					playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "block.amethyst_block.hit", 1.4F, 0.75F);
				}
			}

			boolean totemPoppedEarly = false;
			if (entity instanceof LivingEntity living && totemInspectTicks < 178.0) {
				boolean isInfinity = getRotPersistentBoolean(entity, K_SIIT, false);
				net.minecraft.world.item.Item infinityTotemItem = isInfinity ? BuiltInRegistries.ITEM.get(ResourceLocation.parse("avaritia:infinity_totem")) : null;
				net.minecraft.world.item.Item expectedItem = (infinityTotemItem != null) ? infinityTotemItem : net.minecraft.world.item.Items.TOTEM_OF_UNDYING;
				if (living.getItemInHand(InteractionHand.MAIN_HAND).getItem() != expectedItem) {
					totemPoppedEarly = true;
				}
			}

			if (totemInspectTicks == 1.0 || totemPoppedEarly) {
				entity.getPersistentData().putBoolean(K_STA2, true);
				entity.getPersistentData().putDouble(K_STIT, 0.0);
				if (entity instanceof LivingEntity living) {
					living.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
				}
				if (world instanceof ServerLevel level) {
					boolean isInfinity = getRotPersistentBoolean(entity, K_SIIT, false);
					level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + 1.0, entity.getZ(), 45, 0.6, 0.6, 0.6, 0.25);
					if (isInfinity) {
						level.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + 1.0, entity.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
						level.sendParticles(ParticleTypes.DRAGON_BREATH, entity.getX(), entity.getY() + 1.0, entity.getZ(), 100, 0.8, 0.8, 0.8, 0.15);
						level.sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getY() + 1.0, entity.getZ(), 80, 0.8, 0.8, 0.8, 0.15);
					}
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "item.totem.use", 2.0F, isInfinity ? 0.45F : 0.85F);
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.player.hurt_on_fire", 1.2F, 0.5F);
					if (isInfinity) {
						playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.generic.explode", 2.0F, 0.5F);
						playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sonic_boom", 2.0F, 0.4F);
					}
					if (combatTarget instanceof Player p) {
						if (isInfinity) {
							boolean alreadySaid = entity.getPersistentData().getBoolean(K_SSPT);
							if (!alreadySaid) {
								entity.getPersistentData().putBoolean(K_SSPT, true);
								RotDialoguesProcedure.sendInfinityTotemQuote(p);
							}
						} else {
							RotDialoguesProcedure.sendRandomTotemQuote(p);
						}
					}
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		if (checkAndSeekDroppedTotems(world, entity)) {
			return;
		}

		if (handleTotemStealing(world, entity, combatTarget)) {
			return;
		}

		if (handleSuperheatEvaporationState(world, entity, combatTarget)) {
			handlePassengerAndGrowth(entity);
			return;
		}

		handleHeavyPunchState(world, entity, combatTarget);

		if (handleThreatAwareEvasiveSpacing(world, entity, combatTarget)) {
			return;
		}

		if (handleDiveCounterState(world, entity, combatTarget)) {
			return;
		}

		if (handleCustomCombos(world, entity, combatTarget)) {
			return;
		}

		if (handleOverheadState(world, entity, combatTarget)) {
			return;
		}

		if (handleSlamState(world, entity, combatTarget)) {
			return;
		}

		if (handleDieKickState(world, entity, combatTarget)) {
			return;
		}

		double sonicScreamTicksFirstCheck = entity.getPersistentData().getDouble(K_SSST);
		if (sonicScreamTicksFirstCheck > 0) {
			executeSentinelSonicScream(world, entity, combatTarget, (int) sonicScreamTicksFirstCheck);
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double omniSonicTicks = entity.getPersistentData().getDouble(K_SOSCT);
		if (omniSonicTicks > 0) {
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			if (entity.getDeltaMovement().y() > 0) {
				entity.setDeltaMovement(entity.getDeltaMovement().x(), 0.0, entity.getDeltaMovement().z());
			}
			entity.getPersistentData().putBoolean(K_ISBL, true);

			if (world instanceof ServerLevel level) {
				Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
				
				if (OMNI_SONIC_BOOM_SHOW_PARTICLES) {
					if (omniSonicTicks > OMNI_SONIC_BOOM_TRIGGER_TICK) {
						double chargeTotal = OMNI_SONIC_BOOM_ANIMATION_TICKS - OMNI_SONIC_BOOM_TRIGGER_TICK;
						double elapsed = OMNI_SONIC_BOOM_ANIMATION_TICKS - omniSonicTicks;
						double progress = elapsed / chargeTotal;
						
						double radius = 6.0 - progress * 5.0;
						
						int count = (int) Math.max(4, (4.0 * Math.PI * radius * radius * 1.0));
						if (count > 60) count = 60;
						double goldenRatio = (1.0 + Math.sqrt(5.0)) / 2.0;
						for (int i = 0; i < count; i++) {
							double theta = 2 * Math.PI * i / goldenRatio;
							double phi = Math.acos(1.0 - 2.0 * (i + 0.5) / count);
							double sx = Math.cos(theta) * Math.sin(phi);
							double sy = Math.sin(theta) * Math.sin(phi);
							double sz = Math.cos(phi);
							
							double px = center.x + sx * radius;
							double py = center.y + sy * radius;
							double pz = center.z + sz * radius;
							
							double rx = (level.getRandom().nextDouble() - 0.5) * 0.02;
							double ry = (level.getRandom().nextDouble() - 0.5) * 0.02;
							double rz = (level.getRandom().nextDouble() - 0.5) * 0.02;
							
							level.sendParticles(ParticleTypes.CRIT, px + rx, py + ry, pz + rz, 1, 0.0, 0.0, 0.0, 0.0);
							if (level.getRandom().nextDouble() < 0.10) {
								level.sendParticles(ParticleTypes.SONIC_BOOM, px + rx, py + ry, pz + rz, 1, 0.0, 0.0, 0.0, 0.0);
							}
						}
						
						if (level.getRandom().nextDouble() < progress * 0.85) {
							level.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + 1.2, entity.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
						}
					} else {
						double decayTotal = OMNI_SONIC_BOOM_TRIGGER_TICK;
						double elapsedDecay = OMNI_SONIC_BOOM_TRIGGER_TICK - omniSonicTicks;
						double progressDecay = elapsedDecay / decayTotal;
						
						double currentRadius = 1.0 + progressDecay * OMNI_SONIC_BOOM_RANGE;
						
						int count = (int) Math.max(6, (4.0 * Math.PI * currentRadius * currentRadius * 0.3));
						if (count > 35) count = 35;
						double goldenRatio = (1.0 + Math.sqrt(5.0)) / 2.0;
						for (int i = 0; i < count; i++) {
							double theta = 2 * Math.PI * i / goldenRatio;
							double phi = Math.acos(1.0 - 2.0 * (i + 0.5) / count);
							double sx = Math.cos(theta) * Math.sin(phi);
							double sy = Math.sin(theta) * Math.sin(phi);
							double sz = Math.cos(phi);
							
							double px = center.x + sx * currentRadius;
							double py = center.y + sy * currentRadius;
							double pz = center.z + sz * currentRadius;
							
							if (level.getRandom().nextDouble() < 0.25) {
								level.sendParticles(ParticleTypes.SONIC_BOOM, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
							}
							if (level.getRandom().nextDouble() < 0.2) {
								level.sendParticles(ParticleTypes.CLOUD, px, py, pz, 1, 0.1, 0.1, 0.1, 0.02);
							}
						}
					}
				}
			}

			if (omniSonicTicks == OMNI_SONIC_BOOM_TRIGGER_TICK) {
				if (world instanceof ServerLevel level) {
					boolean totemActive = getRotPersistentBoolean(entity, K_STA2, false);
					boolean unlockedExplosion = getRotPersistentBoolean(entity, K_UEB, false);
					double radius = totemActive ? OMNI_SONIC_BOOM_RANGE * 1.5 : OMNI_SONIC_BOOM_RANGE;
					
					if (unlockedExplosion) {
						level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, entity.getX(), entity.getY() + 1.2, entity.getZ(), 2, 1.0, 1.0, 1.0, 0.1);
						playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.generic.explode", 2.0F, 0.8F);
					}

					if (OMNI_SONIC_BOOM_SHOW_PARTICLES) {
						for (int angleDeg = 0; angleDeg < 360; angleDeg += (totemActive ? 12 : 24)) {
							double rad = Math.toRadians(angleDeg);
							double dx = Math.sin(rad);
							double dz = Math.cos(rad);
							for (double r = 1.0; r <= radius; r += 4.0) {
								double px = entity.getX() + dx * r;
								double py = entity.getY() + 1.2;
								double pz = entity.getZ() + dz * r;
								level.sendParticles(ParticleTypes.SONIC_BOOM, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
								if (unlockedExplosion) {
									level.sendParticles(ParticleTypes.EXPLOSION, px, py, pz, 1, 0.2, 0.2, 0.2, 0.02);
								}
							}
						}
					}
					
					java.util.List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius), e -> e != entity && !isWoodboundEntity(e, entity));
					for (LivingEntity targetVictim : targets) {
						float damageVal = totemActive ? 85.0F : 48.0F;
						if (unlockedExplosion) {
							damageVal = totemActive ? 130.0F : 78.0F;
						}
						dealTrueDamageToBosses(targetVictim, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, net.minecraft.resources.ResourceLocation.parse("the_backwoods:rot_sonic_boom"))), entity), damageVal * (float) getAdaptationMultiplier(entity));
						Vec3 push = targetVictim.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0).normalize();
						double pushMult = totemActive ? 8.5 : 6.0;
						double pushUp = totemActive ? 2.25 : 1.5;
						targetVictim.setDeltaMovement(push.x * pushMult, pushUp, push.z * pushMult);
						targetVictim.hasImpulse = true;
					}
					
					BlockPos posCenter = BlockPos.containing(entity.position());
					double baseRadius = totemActive ? 12.0 : 6.5;
					int rangeBound = totemActive ? 12 : 6;
					int heightBoundUpper = totemActive ? 6 : 4;
					int heightBoundLower = totemActive ? -3 : -1;
					for (BlockPos bp : BlockPos.betweenClosed(posCenter.offset(-rangeBound, heightBoundLower, -rangeBound), posCenter.offset(rangeBound, heightBoundUpper, rangeBound))) {
						double dx = bp.getX() - posCenter.getX();
						double dz = bp.getZ() - posCenter.getZ();
						double distSq = dx * dx + dz * dz;
						double randomRadius = baseRadius + (Math.random() * 2.0 - 1.0);
						if (distSq <= randomRadius * randomRadius) {
							BlockState bs = level.getBlockState(bp);
							if (!bs.isAir() && bs.getDestroySpeed(level, bp) >= 0 && bs.getDestroySpeed(level, bp) < (totemActive ? 80.0F : 50.0F)) {
								level.destroyBlock(bp, false);
							}
						}
					}
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sonic_boom", 2.5F, 0.40F);
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.generic.explode", 2.0F, 0.5F);
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		} else {
			entity.getPersistentData().putBoolean(K_ISBL, false);
		}

		double sonicTicksFirstCheck = entity.getPersistentData().getDouble(K_SST);
		if (sonicTicksFirstCheck > 0) {
			if (combatTarget == null || !combatTarget.isAlive()) {
				cleanupSonicBoomState(entity);
				handlePassengerAndGrowth(entity);
				return;
			}
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			if (entity.getDeltaMovement().y() > 0) {
				entity.setDeltaMovement(entity.getDeltaMovement().x(), 0.0, entity.getDeltaMovement().z());
			}
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}
			double triggerTick = getTelegraphJitter(entity, "sonic_boom", SONIC_BOOM_TRIGGER_TICK, -4.0, 4.0);
			boolean alreadyTriggered = getRotPersistentBoolean(entity, K_SST3, false);
			if (!alreadyTriggered && sonicTicksFirstCheck <= triggerTick) {
				double tdx = combatTarget.getX() - entity.getX();
				double tdz = combatTarget.getZ() - entity.getZ();
				double tdy = (combatTarget.getY() + combatTarget.getBbHeight() * 0.5) - (entity.getY() + entity.getBbHeight() * SONIC_BOOM_TORSO_Y_FACTOR);
				double flatD = Math.sqrt(tdx * tdx + tdz * tdz);
				double pitchAngle = -Math.toDegrees(Math.atan2(tdy, flatD));

				int repositionAttempts = (int) getRotPersistentDouble(entity, K_SSRA, 0.0);
				if ((pitchAngle > 45.0 || (flatD < 2.2 && tdy < -1.0)) && repositionAttempts < 2) {
					double normDist = flatD < 0.001 ? 1.0 : flatD;
					double backX = entity.getX() - (tdx / normDist) * 3.5;
					double backZ = entity.getZ() - (tdz / normDist) * 3.5;
					if (entity instanceof Mob mob) {
						mob.getNavigation().moveTo(backX, entity.getY(), backZ, ROT_WALK_SPEED * 1.3);
					}
					setRotPersistentDouble(entity, K_SSRA, repositionAttempts + 1.0);
					entity.getPersistentData().putDouble(K_SST, sonicTicksFirstCheck + 8.0);
				} else if (entity.distanceTo(combatTarget) <= SONIC_BOOM_RANGE + 3.0) {
					setRotPersistentBoolean(entity, K_SST3, true);
					fireSuperchargedSonicBoomEffectAndDamage(world, entity, combatTarget);
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double skyWarpTicks = entity.getPersistentData().getDouble(K_SSWST);
		if (skyWarpTicks > 0) {
			if (skyWarpTicks > 1) {
				entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.1, Math.max(0, entity.getDeltaMovement().y() * 0.5), entity.getDeltaMovement().z() * 0.1);
			}
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
				if (skyWarpTicks == 1) {
					double targetX = combatTarget.getX();
					double targetY = combatTarget.getY() + 3.8;
					double targetZ = combatTarget.getZ();
					if (world instanceof ServerLevel level) {
						level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1.1, entity.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
						teleportEntity(entity, targetX, targetY, targetZ);
						level.sendParticles(ParticleTypes.PORTAL, targetX, targetY + 1.1, targetZ, 8, 0.2, 0.2, 0.2, 0.05);
						playHostileSound(level, targetX, targetY, targetZ, "item.chorus_fruit.teleport", 1.3F, 1.1F);
						entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(combatTarget.getX(), combatTarget.getY() + combatTarget.getBbHeight() * 0.5, combatTarget.getZ()));
						playHostileSound(level, targetX, targetY, targetZ, "entity.iron_golem.attack", 1.4F, 0.5F);
						playHostileSound(level, targetX, targetY, targetZ, "entity.player.attack.sweep", 1.4F, 0.7F);
						level.sendParticles(ParticleTypes.SWEEP_ATTACK, targetX, targetY - 1.0, targetZ, 3, 0.4, 0.4, 0.4, 0.0);
						if (combatTarget instanceof LivingEntity livTarget) {
							livTarget.setDeltaMovement(0.0, -4.5, 0.0);
							livTarget.hasImpulse = true;
							livTarget.getPersistentData().putBoolean(K_SSWSI, true);
							double punchDmg = entity.getPersistentData().getDouble(K_APD);
							if (punchDmg < 8.0) punchDmg = 8.0;
							dealTrueDamageToBosses(livTarget, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_overhead_slam"))), entity), (float) (punchDmg * 2.2) * (float) getAdaptationMultiplier(entity));
						}
					}
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		if (combatTarget instanceof LivingEntity livTarget && getRotPersistentBoolean(livTarget, K_SSWSI, false)) {
			if (livTarget.onGround() || livTarget.getDeltaMovement().y > -0.1) {
				livTarget.getPersistentData().putBoolean(K_SSWSI, false);
				if (world instanceof ServerLevel level) {
					double tx = livTarget.getX();
					double ty = livTarget.getY();
					double tz = livTarget.getZ();
					boolean totemActive = getRotPersistentBoolean(entity, K_STA2, false);
					float explosionForce = totemActive ? 6.2F : 3.5F;
					level.explode(entity, null, new net.minecraft.world.level.ExplosionDamageCalculator() {
						@Override
						public boolean shouldDamageEntity(net.minecraft.world.level.Explosion explosion, Entity ent) {
							return !isWoodboundEntity(ent, entity);
						}
					}, tx, ty + 0.5, tz, explosionForce * (float) Math.min(5.0, getAdaptationMultiplier(entity)), false, Level.ExplosionInteraction.MOB);
					level.sendParticles(ParticleTypes.EXPLOSION, tx, ty + 0.5, tz, totemActive ? 15 : 6, 0.6, 0.2, 0.6, 0.15);
					level.sendParticles(ParticleTypes.CRIT, tx, ty + 0.5, tz, totemActive ? 40 : 20, 0.5, 0.5, 0.5, 0.3);
					level.sendParticles(ParticleTypes.SONIC_BOOM, tx, ty + 0.5, tz, totemActive ? 3 : 1, 0.1, 0.1, 0.1, 0.0);
					playHostileSound(level, tx, ty, tz, "entity.generic.explode", 1.4F, 0.85F);
					playHostileSound(level, tx, ty, tz, "entity.iron_golem.death", 1.1F, 0.65F);
					List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(new Vec3(tx, ty, tz), totemActive ? 10 : 5, totemActive ? 8 : 5, totemActive ? 10 : 5), e -> e != entity && e != livTarget && !isWoodboundEntity(e, entity));
					for (LivingEntity near : nearby) {
						if (near.isAlive()) {
							float nearDmg = totemActive ? 22.0F : 10.0F;
							dealTrueDamageToBosses(near, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_overhead_slam"))), entity), nearDmg * (float) getAdaptationMultiplier(entity));
							Vec3 pushAway = near.position().subtract(livTarget.position()).normalize();
							double pushMult = totemActive ? 2.5 : 1.5;
							near.setDeltaMovement(pushAway.x * pushMult, 0.5, pushAway.z * pushMult);
							near.hasImpulse = true;
						}
					}
				}
			}
		}

		double judgmentTicks = entity.getPersistentData().getDouble(K_SJT);
		if (judgmentTicks > 0) {
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}

			if (judgmentTicks > 20) {
				entity.setDeltaMovement(0.0, 0.0, 0.0);
				if (world instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 0.2, entity.getZ(), 3, 0.3, 0.1, 0.3, 0.02);
					if (judgmentTicks % 5 == 0) {
						playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.heartbeat", 1.2F, 0.7F);
					}
				}
			} else if (judgmentTicks == 20) {
				Entity targetLoc = combatTarget != null ? combatTarget : entity;
				double targetYaw = targetLoc.getYRot();
				double radians = Math.toRadians(targetYaw);
				double kickStartX = targetLoc.getX() - 12.0 * Math.sin(radians);
				double kickStartY = targetLoc.getY() + 2.8;
				double kickStartZ = targetLoc.getZ() + 12.0 * Math.cos(radians);

				teleportEntity(entity, kickStartX, kickStartY, kickStartZ);
				if (combatTarget != null) {
					snapLookAtTarget(entity, combatTarget);
				}

				Vec3 dir;
				if (combatTarget != null) {
					Vec3 rotCenter = entity.getBoundingBox().getCenter();
					Vec3 targetCenter = combatTarget.getBoundingBox().getCenter();
					double dist = rotCenter.distanceTo(targetCenter);
					double speed = 5.5;
					double flightTicks = Math.max(1.0, dist / speed);

					if (!combatTarget.onGround()) {
						Vec3 targetVel = combatTarget.getDeltaMovement();
						double grav = 0.08;
						double predX = targetCenter.x + targetVel.x * flightTicks;
						double predY = targetCenter.y + targetVel.y * flightTicks - 0.5 * grav * flightTicks * flightTicks;
						double predZ = targetCenter.z + targetVel.z * flightTicks;

						double groundY = findGroundY(world, combatTarget) + combatTarget.getBbHeight() * 0.5;
						if (predY < groundY) {
							predY = groundY;
						}
						Vec3 predictedCenter = new Vec3(predX, predY, predZ);
						dir = predictedCenter.subtract(rotCenter);
					} else {
						dir = targetCenter.subtract(rotCenter);
					}

					if (dir.length() > 0.1) {
						dir = dir.normalize();
					} else {
						dir = entity.getLookAngle().normalize();
					}
				} else {
					dir = entity.getLookAngle().normalize();
				}
				setRotPersistentDouble(entity, K_SJDX, dir.x);
				setRotPersistentDouble(entity, K_SJDY, dir.y);
				setRotPersistentDouble(entity, K_SJDZ, dir.z);

				playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sonic_charge", 1.8F, 0.65F);
				if (world instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + 0.5, entity.getZ(), 1, 0, 0, 0, 0);
				}
			} else if (judgmentTicks > 1) {
				double dirX = getRotPersistentDouble(entity, K_SJDX, 0.0);
				double dirY = getRotPersistentDouble(entity, K_SJDY, 0.0);
				double dirZ = getRotPersistentDouble(entity, K_SJDZ, 0.0);
				double speed = 5.5;

				entity.setDeltaMovement(dirX * speed, dirY * speed, dirZ * speed);
				entity.hasImpulse = true;
				entity.fallDistance = 0;

				double dh = Math.sqrt(dirX * dirX + dirZ * dirZ);
				float targetYRot = (float) (Mth.atan2(dirZ, dirX) * (180F / Math.PI)) - 90F;
				float targetXRot = (float) (-(Mth.atan2(dirY, dh) * (180F / Math.PI)));
				entity.setYRot(targetYRot);
				entity.setXRot(targetXRot);
				if (entity instanceof Mob mob) {
					mob.yBodyRot = targetYRot;
					mob.yHeadRot = targetYRot;
				}

				if (world instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.GUST, entity.getX(), entity.getY() + 0.3, entity.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
					level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.3, entity.getZ(), 3, 0.1, 0.1, 0.1, 0.02);
				}

				boolean hitTarget = false;
				Vec3 impactPoint = null;

				if (combatTarget != null && combatTarget.isAlive()) {
					AABB targetBox = combatTarget.getBoundingBox();
					double rotHalfWidth = entity.getBbWidth() * 0.5;
					double rotHalfHeight = entity.getBbHeight() * 0.5;
					AABB wallBox = targetBox.inflate(rotHalfWidth, rotHalfHeight, rotHalfWidth);
					Vec3 startPos = entity.getBoundingBox().getCenter();
					Vec3 endPos = startPos.add(dirX * speed, dirY * speed, dirZ * speed);

					if (wallBox.contains(startPos) || entity.getBoundingBox().intersects(targetBox)) {
						hitTarget = true;
						impactPoint = startPos;
					} else {
						java.util.Optional<Vec3> clipOpt = wallBox.clip(startPos, endPos);
						if (clipOpt.isPresent()) {
							hitTarget = true;
							impactPoint = clipOpt.get();
						}
					}
				}

				double distToTarget = combatTarget != null ? entity.distanceTo(combatTarget) : 999.0;
				if (hitTarget || distToTarget < JUDGMENT_KICK_IMPACT_DIST || entity.onGround()) {
					if (hitTarget && impactPoint != null) {
						double stopX = impactPoint.x;
						double stopY = impactPoint.y - entity.getBbHeight() * 0.5;
						double stopZ = impactPoint.z;
						entity.teleportTo(stopX, stopY, stopZ);
						entity.setDeltaMovement(0, 0, 0);
					}
					entity.getPersistentData().putDouble(K_SJT, 2);
					judgmentTicks = 2;
				}
			} else if (judgmentTicks == 1) {
				double impactX = entity.getX();
				double impactY = entity.getY();
				double impactZ = entity.getZ();

				if (world instanceof ServerLevel level) {
					double groundY = findGroundY(level, entity);
					if (impactY > groundY + 0.5 && entity.onGround()) {
						impactY = groundY;
					}
					teleportEntity(entity, impactX, impactY, impactZ);

					net.minecraft.world.level.block.state.BlockState floorState = level.getBlockState(net.minecraft.core.BlockPos.containing(impactX, impactY - 0.5, impactZ));
					if (floorState.isAir()) {
						floorState = level.getBlockState(net.minecraft.core.BlockPos.containing(impactX, impactY - 1.5, impactZ));
					}
					if (floorState.isAir()) {
						floorState = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
					}
					net.minecraft.core.particles.BlockParticleOption dustPillarOptions = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.DUST_PILLAR, floorState);
					net.minecraft.core.particles.ParticleType<?> _tsdType = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse("trial_spawner_detection"));
					net.minecraft.core.particles.ParticleOptions trialSpawnerDetection = _tsdType instanceof net.minecraft.core.particles.ParticleOptions _tsdOpt ? _tsdOpt : net.minecraft.core.particles.ParticleTypes.EFFECT;

					for (int r = 1; r <= 3; r++) {
						double radiusVal = r * 1.2;
						for (int deg = 0; deg < 360; deg += 30) {
							double rads = Math.toRadians(deg);
							double px = impactX + Math.cos(rads) * radiusVal;
							double pz = impactZ + Math.sin(rads) * radiusVal;
							level.sendParticles(dustPillarOptions, px, impactY + 0.1, pz, 1, 0.0, 0.05, 0.0, 0.01);
							level.sendParticles(trialSpawnerDetection, px, impactY + 0.1, pz, 1, 0.0, 0.05, 0.0, 0.01);
						}
					}

					for (int r = 1; r <= 5; r++) {
						final double rVal = r;
						int particleCount = (int) (12 * rVal);
						for (int i = 0; i < particleCount; i++) {
							double angle = (2 * Math.PI * i) / particleCount;
							double px = impactX + Math.cos(angle) * rVal;
							double py = impactY + 0.15;
							double pz = impactZ + Math.sin(angle) * rVal;
							double vx = Math.cos(angle) * 0.18;
							double vy = 0.05 + 0.02 * rVal;
							double vz = Math.sin(angle) * 0.18;

							level.sendParticles(dustPillarOptions, px, py, pz, 0, vx, vy, vz, 1.0);
							level.sendParticles(trialSpawnerDetection, px, py, pz, 0, vx * 0.5, vy, vz * 0.5, 1.0);
						}
					}

					playHostileSound(level, impactX, impactY, impactZ, "entity.generic.explode", 1.8F, 0.6F);
					playHostileSound(level, impactX, impactY, impactZ, "entity.warden.sonic_boom", 1.8F, 0.55F);
					playHostileSound(level, impactX, impactY, impactZ, "entity.iron_golem.death", 1.3F, 0.45F);

					boolean totemActive = getRotPersistentBoolean(entity, K_STA2, false);
					boolean isInfinity = getRotPersistentBoolean(entity, K_SIIT, false);
					sendCameraShake(totemActive ? 1.5F : 1.0F, totemActive ? 25 : 15, totemActive ? 30.0F : 20.0F);

					if (isInfinity) {
						level.explode(entity, null, new net.minecraft.world.level.ExplosionDamageCalculator() {
							@Override
							public boolean shouldDamageEntity(net.minecraft.world.level.Explosion explosion, Entity ent) {
								return !isWoodboundEntity(ent, entity);
							}
						}, impactX, impactY + 0.5, impactZ, 6.0F * (float) Math.min(5.0, getAdaptationMultiplier(entity)), false, Level.ExplosionInteraction.MOB);
					} else if (totemActive) {
						level.explode(entity, null, new net.minecraft.world.level.ExplosionDamageCalculator() {
							@Override
							public boolean shouldDamageEntity(net.minecraft.world.level.Explosion explosion, Entity ent) {
								return !isWoodboundEntity(ent, entity);
							}
						}, impactX, impactY + 0.5, impactZ, 5.5F * (float) Math.min(5.0, getAdaptationMultiplier(entity)), false, Level.ExplosionInteraction.MOB);
					}
					level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impactX, impactY + 0.5, impactZ, totemActive ? 15 : 6, 0.5, 0.2, 0.5, 0.1);
					level.sendParticles(ParticleTypes.CRIT, impactX, impactY + 0.5, impactZ, totemActive ? 40 : 22, 0.4, 0.4, 0.4, 0.2);

					if (combatTarget instanceof LivingEntity targetLiv) {
						double distToTarget = entity.distanceTo(targetLiv);
						if (distToTarget <= 3.5 || entity.getBoundingBox().inflate(1.5).intersects(targetLiv.getBoundingBox())) {
							double punchDmg = entity.getPersistentData().getDouble(K_APD);
							if (punchDmg < 8.0) punchDmg = 8.0;
							double dropkickDamageMult = 3.5;
							if (isInfinity) {
								dropkickDamageMult = 3.5 * 2.5;
							}
							dealTrueDamageToBosses(targetLiv, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_judgement_dropkick"))), entity), (float) (punchDmg * dropkickDamageMult) * (float) getAdaptationMultiplier(entity));

							Vec3 pushVec = targetLiv.position().subtract(entity.position());
							double horizontalDist = Math.sqrt(pushVec.x * pushVec.x + pushVec.z * pushVec.z);
							if (horizontalDist < 0.1) {
								pushVec = entity.getLookAngle();
								horizontalDist = Math.sqrt(pushVec.x * pushVec.x + pushVec.z * pushVec.z);
							}
							if (horizontalDist > 0.01) {
								pushVec = new Vec3(pushVec.x / horizontalDist, 0, pushVec.z / horizontalDist);
							} else {
								pushVec = new Vec3(1, 0, 0);
							}

							double pushForce = totemActive ? 3.0 : 2.2;
							double pushUp = 0.6;
							if (isInfinity) {
								pushForce = 7.5;
								pushUp = 1.4;
							}
							applyKnockbackAndSync(targetLiv, pushVec.x * pushForce, pushUp, pushVec.z * pushForce);
						}
					}

					entity.getPersistentData().putDouble(K_SSS2, 1);
					entity.getPersistentData().putDouble(K_SSX, impactX);
					entity.getPersistentData().putDouble(K_SSY, impactY);
					entity.getPersistentData().putDouble(K_SSZ, impactZ);
					entity.getPersistentData().putBoolean(K_SSV, true);
					entity.getPersistentData().putDouble(K_SSY2, entity.getYRot());

					List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(new Vec3(impactX, impactY, impactZ), 8, 6, 8), e -> e != entity && e != combatTarget && !isWoodboundEntity(e, entity));
					for (LivingEntity near : nearby) {
						if (near.isAlive()) {
							float baseNearDmg = 15.0F;
							if (isInfinity) baseNearDmg *= 2.5F;
							dealTrueDamageToBosses(near, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_judgement_dropkick"))), entity), baseNearDmg * (float) getAdaptationMultiplier(entity));
							Vec3 pushAway = near.position().subtract(entity.position());
							double nearHorizontalDist = Math.sqrt(pushAway.x * pushAway.x + pushAway.z * pushAway.z);
							if (nearHorizontalDist > 0.01) {
								pushAway = new Vec3(pushAway.x / nearHorizontalDist, 0, pushAway.z / nearHorizontalDist);
							} else {
								pushAway = new Vec3(1, 0, 0);
							}
							double nearPush = 2.2;
							double nearPushUp = 0.45;
							if (isInfinity) {
								nearPush = 5.5;
								nearPushUp = 1.0;
							}
							applyKnockbackAndSync(near, pushAway.x * nearPush, nearPushUp, pushAway.z * nearPush);
						}
					}
				}
				entity.getPersistentData().putDouble(K_SRHT, 12);
				entity.getPersistentData().putBoolean(K_SRHO, entity.onGround());
				entity.getPersistentData().putDouble(K_SLT, 0);
				entity.getPersistentData().putBoolean(K_SISL, false);
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double riderHoldTicks = entity.getPersistentData().getDouble(K_SRHT);
		if (riderHoldTicks > 0) {
			boolean isGroundHold = getRotPersistentBoolean(entity, K_SRHO, false);
			if (isGroundHold || entity.onGround()) {
				entity.setDeltaMovement(0, -0.05, 0);
			}
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}
			if (riderHoldTicks == 1) {
				if (isGroundHold || entity.onGround()) {
					entity.getPersistentData().putDouble(K_SLT, 20);
					entity.getPersistentData().putBoolean(K_SISL, true);
				} else {
					entity.getPersistentData().putDouble(K_SLT, 0);
					entity.getPersistentData().putBoolean(K_SISL, false);
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double minosStage = entity.getPersistentData().getDouble(K_SMS);
		double minosTicks = entity.getPersistentData().getDouble(K_SMT);
		if (minosStage == 0 && minosTicks > 0) {
			minosStage = 1;
			entity.getPersistentData().putDouble(K_SMS, 1);
		}
		if (minosStage > 0 || minosTicks > 0) {
			if (getRotPersistentDouble(entity, K_SSFT, 0.0) > 0 || getRotPersistentDouble(entity, K_SSCT, 0.0) > 0
				|| getRotPersistentDouble(entity, K_SCFT, 0.0) > 0 || getRotPersistentDouble(entity, K_SCCT, 0.0) > 0) {
				entity.getPersistentData().putDouble(K_SSFT, 0);
				entity.getPersistentData().putDouble(K_SSCT, 0);
				entity.getPersistentData().putDouble(K_SCFT, 0);
				entity.getPersistentData().putDouble(K_SCCT, 0);
				entity.getPersistentData().putDouble(K_SLCT, LASER_CLOSING_TICKS);
				stopHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
			}
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
				if (entity instanceof Mob mob) {
					mob.getNavigation().stop();
				}
			}
			entity.setDeltaMovement(0, entity.getDeltaMovement().y(), 0);

			if (minosStage == 1) {
				if (combatTarget instanceof LivingEntity targetLiv) {
					if (!hasHeavyPunchSupport(world, targetLiv)) {
						return;
					}
					entity.getPersistentData().putDouble(K_SMPC, 1);
					executeMinosHeavyPunchBlink(world, entity, targetLiv, true);
					entity.getPersistentData().putDouble(K_SMS, 2);
				} else {
					entity.getPersistentData().putDouble(K_SMS, 0);
					entity.getPersistentData().putDouble(K_SMT, 0);
				}
			} else if (minosStage == 2) {
				double heavyLeft = entity.getPersistentData().getDouble(K_SHLPT);
				double heavyRight = entity.getPersistentData().getDouble(K_SHRPT);

				if (heavyLeft == 0 && heavyRight == 0) {
					boolean isCornered = (combatTarget != null) && isTargetCornered(world, combatTarget, entity);
					double baseWait = isCornered ? 12.0 : 32.0;
					double adaptation = getAdaptationMultiplier(entity);
					double adaptedWait = baseWait / Math.max(0.8, adaptation);
					double waitTicks = Math.max(6.0, getTelegraphJitter(entity, "minos_wait", adaptedWait, -22.0, 22.0));

					if (isCornered && combatTarget != null && world instanceof ServerLevel level) {
						level.sendParticles(ParticleTypes.ANGRY_VILLAGER, combatTarget.getX(), combatTarget.getY() + 1.2, combatTarget.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
						level.sendParticles(ParticleTypes.CRIT, combatTarget.getX(), combatTarget.getY() + 1.0, combatTarget.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
						playHostileSound(level, combatTarget.getX(), combatTarget.getY(), combatTarget.getZ(), "entity.warden.heartbeat", 1.5F, 1.2F);
					}

					entity.getPersistentData().putDouble(K_SMWT, waitTicks);
					entity.getPersistentData().putDouble(K_SMS, 3);
				}
			} else if (minosStage == 3) {
				double waitTicks = entity.getPersistentData().getDouble(K_SMWT);
				if (waitTicks <= 0) {
					double punchCount = entity.getPersistentData().getDouble(K_SMPC);
					if (punchCount < 4 && combatTarget instanceof LivingEntity targetLiv) {
						if (!hasHeavyPunchSupport(world, targetLiv)) {
							return;
						}
						punchCount++;
						entity.getPersistentData().putDouble(K_SMPC, punchCount);

						boolean isLeftHand = (punchCount % 2 != 0);
						executeMinosHeavyPunchBlink(world, entity, targetLiv, isLeftHand);

						entity.getPersistentData().putDouble(K_SMS, 2);
					} else {
						entity.getPersistentData().putDouble(K_SMS, 0);
						entity.getPersistentData().putDouble(K_SMT, 0);
						entity.getPersistentData().putDouble(K_SMWT, 0);
					}
				}
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		double closingTicksFirstCheck = entity.getPersistentData().getDouble(K_SLCT);
		if (closingTicksFirstCheck > 0) {
			entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}
			handlePassengerAndGrowth(entity);
			return;
		}

		if (combatTarget == null) {
			entity.getPersistentData().putDouble(K_ADAPT_MODE, 0);
			if (entity.getPersistentData().getDouble(K_SSFT) > 0 || entity.getPersistentData().getDouble(K_SSCT) > 0
				|| entity.getPersistentData().getDouble(K_SCFT) > 0 || entity.getPersistentData().getDouble(K_SCCT) > 0) {
				entity.getPersistentData().putDouble(K_SSFT, 0);
				entity.getPersistentData().putDouble(K_SSCT, 0);
				entity.getPersistentData().putDouble(K_SCFT, 0);
				entity.getPersistentData().putDouble(K_SCCT, 0);
				entity.getPersistentData().putDouble(K_SLCT, LASER_CLOSING_TICKS);
				stopHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
			}

			if (entity instanceof Mob mob) {
				Player master = null;
				if (getRotPersistentBoolean(entity, K_MGM, false)) {
					master = getGuardPlayer(world, entity);
				} else if (getRotPersistentBoolean(entity, K_MFE, false)) {
					master = getFollowPlayer(world, entity);
				}
				if (master != null) {
					double distToMaster = mob.distanceTo(master);
					double dy = Math.abs(mob.getY() - master.getY());
					if (distToMaster > 24.0 || dy > 8.0) {
						double tx = master.getX() + (mob.getRandom().nextDouble() - 0.5) * 4.0;
						double tz = master.getZ() + (mob.getRandom().nextDouble() - 0.5) * 4.0;
						double ty = master.getY();
						teleportEntity(mob, tx, ty, tz);
					} else if (distToMaster > 5.0 && mob.tickCount % 5 == 0) {
						mob.getNavigation().moveTo(master, 1.25);
					} else if (distToMaster <= 3.0) {
						mob.getNavigation().stop();
					}
				}
			}

			handlePassengerAndGrowth(entity);
			return;
		}

		if (combatTarget instanceof Player p && p.getAbilities().instabuild && entity.getPersistentData().getDouble(K_CREATIVE_MSG) == 0) {
			entity.getPersistentData().putDouble(K_CREATIVE_MSG, 1);
			handlePassengerAndGrowth(entity);
			return;
		}

		String speciesKey = BuiltInRegistries.ENTITY_TYPE.getKey(combatTarget.getType()).toString().toLowerCase(java.util.Locale.ROOT);
		if (combatTarget instanceof Player) speciesKey = "minecraft:player";

		boolean hasAnalyzed = getRotPersistentBoolean(entity, K_AS + speciesKey, false);
		if (!hasAnalyzed) {
			double analyzingTicks = entity.getPersistentData().getDouble(K_SAT);
			int currentAnalysisTargetId = entity.getPersistentData().getInt(K_SATI);

			if (analyzingTicks <= 0 && currentAnalysisTargetId != combatTarget.getId()) {
				analyzingTicks = 40.0 + entity.getRandom().nextDouble() * 100.0;
				entity.getPersistentData().putDouble(K_SAT, analyzingTicks);
				entity.getPersistentData().putInt(K_SATI, combatTarget.getId());
			} else if (analyzingTicks > 0 && currentAnalysisTargetId == combatTarget.getId()) {
				entity.getPersistentData().putDouble(K_SAT, analyzingTicks - 1);
				entity.setDeltaMovement(0.0, entity.getDeltaMovement().y(), 0.0);
				
				lockLookAtTarget(entity, combatTarget);
				if (entity instanceof Mob _mob) {
					_mob.getNavigation().stop();
				}
				
				if (entity.tickCount % 20 == 0 && world instanceof ServerLevel _level) {
					_level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight(), entity.getZ(), 3, 0.5, 0.5, 0.5, 0.05);
				}

				if (analyzingTicks - 1 <= 0) {
					entity.getPersistentData().putBoolean(K_AS + speciesKey, true);
					entity.getPersistentData().putInt(K_SATI, 0);
					if (world instanceof ServerLevel _level) {
						_level.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL, entity.getX(), entity.getY() + entity.getBbHeight(), entity.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
					}
					playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.agitated", 1.0F, 0.8F);
				}
				handlePassengerAndGrowth(entity);
				return;
			}
		} else {
			if (entity.getPersistentData().getDouble(K_SAT) > 0) {
				entity.getPersistentData().putDouble(K_SAT, 0);
				entity.getPersistentData().putInt(K_SATI, 0);
			}
		}

		lockLookAtTarget(entity, combatTarget);

		TargetIntent currentIntent = inferTargetIntent(entity, combatTarget);
		entity.getPersistentData().putString("sentinel_target_intent", currentIntent.name());
		adaptCapabilitiesToIntent(entity, currentIntent);

		if (interceptEnderPearlsPipeline(world, entity, combatTarget)) {
			handlePassengerAndGrowth(entity);
			return;
		}

		InterceptionPrediction currentInterception = evaluateInterceptionPipeline(entity, combatTarget, currentIntent);
		if (currentInterception.recommendWait) {
			entity.getPersistentData().putBoolean(K_SWI, true);
			if (currentInterception.repositionTargetPos != null) {
				lockLookAtTarget(entity, currentInterception.repositionTargetPos);
				if (entity instanceof Mob mob) {
					mob.getNavigation().moveTo(currentInterception.repositionTargetPos.x, currentInterception.repositionTargetPos.y, currentInterception.repositionTargetPos.z, ROT_RUN_SPEED);
				}
			}
		} else {
			entity.getPersistentData().putBoolean(K_SWI, false);
		}

		double distToTarget = entity.distanceTo(combatTarget);

		if (!isRotChannelingAbility(entity) && getRotPersistentDouble(entity, K_SEPC, 0.0) <= 0.0) {
			CombatContext eatCtx = getCombatContext(entity, combatTarget);
			if (eatCtx.isEatingHealingItem && eatCtx.eatingTicksRemaining >= 6 && distToTarget <= 10.0) {
				if (distToTarget <= 3.0) {
					setRotPersistentDouble(entity, K_SMW, 4.0);
					setRotPersistentDouble(entity, K_SEPC, 100.0);
				} else if (getRotPersistentBoolean(entity, K_UT, false) && getRotPersistentDouble(entity, K_SJT, 0.0) <= 0.0) {
					setRotPersistentDouble(entity, K_SJT, 60.0);
					setRotPersistentDouble(entity, K_SEPC, 100.0);
				}
			}
		}

		double aiPlanTicks = entity.getPersistentData().getDouble(K_ACPT);
		if (aiPlanTicks <= 0) {
			entity.getPersistentData().putInt("ai_combat_plan", Math.random() < 0.3 ? 1 : 0);
			entity.getPersistentData().putDouble(K_ACPT, 40.0 + Math.random() * 60.0);
		} else {
			entity.getPersistentData().putDouble(K_ACPT, aiPlanTicks - 1);
		}

		if (combatTarget instanceof LivingEntity lsTarget) {
			if (!lsTarget.onGround() && !lsTarget.isInWater()) {
				entity.getPersistentData().putDouble(K_ATAT, entity.getPersistentData().getDouble(K_ATAT) + 1.0);
			} else {
				entity.getPersistentData().putDouble(K_ATAT, Math.max(0.0, entity.getPersistentData().getDouble(K_ATAT) - 2.0));
			}
			if (lsTarget.isBlocking()) {
				entity.getPersistentData().putDouble(K_ATST, entity.getPersistentData().getDouble(K_ATST) + 1.0);
			} else {
				entity.getPersistentData().putDouble(K_ATST, Math.max(0.0, entity.getPersistentData().getDouble(K_ATST) - 2.0));
			}
			double lastDist = entity.getPersistentData().getDouble(K_ALD);
			entity.getPersistentData().putDouble(K_ADT, Math.max(-1.0, Math.min(1.0, (lastDist - distToTarget) / 0.35)));
			if (lsTarget.isSprinting() && distToTarget > lastDist + 0.05) {
				entity.getPersistentData().putDouble(K_ATSAT, entity.getPersistentData().getDouble(K_ATSAT) + 1.0);
			} else {
				entity.getPersistentData().putDouble(K_ATSAT, Math.max(0.0, entity.getPersistentData().getDouble(K_ATSAT) - 1.0));
			}
			entity.getPersistentData().putDouble(K_ALD, distToTarget);
			float lastH = (float) entity.getPersistentData().getDouble(K_ALTH);
			float currentH = lsTarget.getHealth();
			if (currentH < lastH) {
				String lastMove = entity.getPersistentData().getString(K_SM1);
				if (!lastMove.isEmpty()) {
					double currentBias = entity.getPersistentData().getDouble(K_AB + lastMove);
					entity.getPersistentData().putDouble(K_AB + lastMove, Math.min(2.5, currentBias + 0.3));
					recordBiasIndexUpdate(entity, lastMove);
				}
			}
			entity.getPersistentData().putDouble(K_ALTH, currentH);
		}
		if (entity.tickCount % 5 == 0) {
			Set<Integer> activeIndices = getActiveBiasIndices(entity);
			if (!activeIndices.isEmpty()) {
				Set<Integer> toRemove = new HashSet<>();
				for (int i : activeIndices) {
					String bKey = "ai_bias_combo_" + i;
					double val = entity.getPersistentData().contains(bKey) ? entity.getPersistentData().getDouble(bKey) : 1.0;
					if (val > 1.0) val = Math.max(1.0, val - 0.015);
					else if (val < 1.0) val = Math.min(1.0, val + 0.015);

					if (Math.abs(val - 1.0) < 0.001) {
						entity.getPersistentData().remove(bKey);
						toRemove.add(i);
					} else {
						entity.getPersistentData().putDouble(bKey, val);
					}
				}
				if (!toRemove.isEmpty()) {
					activeIndices.removeAll(toRemove);
					setActiveBiasIndices(entity, activeIndices);
				}
			}
		}
		double tpComboPenalty = entity.getPersistentData().getDouble(K_ATCPT);
		if (tpComboPenalty > 0) {
			entity.getPersistentData().putDouble(K_ATCPT, tpComboPenalty - 1);
		}
		double fakePressureTicks = entity.getPersistentData().getDouble(K_AFPT);
		if (fakePressureTicks > 0) {
			entity.getPersistentData().putDouble(K_AFPT, fakePressureTicks - 1);
		}
		double attackOutcomeScore = getRotPersistentDouble(entity, K_SAOS, 0.0);
		if (attackOutcomeScore > 0.0) entity.getPersistentData().putDouble(K_SAOS, Math.max(0.0, attackOutcomeScore - 0.05));

		double controlledAdaptationTicks = entity.getPersistentData().getDouble(K_CAT);
		if (controlledAdaptationTicks > 0) {
			entity.getPersistentData().putDouble(K_CAT, controlledAdaptationTicks - 1);
		}
		double controlledAdaptationCooldown = entity.getPersistentData().getDouble(K_CAC);
		if (controlledAdaptationCooldown > 0) {
			entity.getPersistentData().putDouble(K_CAC, controlledAdaptationCooldown - 1);
		}

		if (ENABLE_CONTROLLED_ADAPTATION && controlledAdaptationTicks <= 0 && controlledAdaptationCooldown <= 0 && entity instanceof LivingEntity rotLiv && rotLiv.getHealth() >= rotLiv.getMaxHealth() * 0.75f && combatTarget instanceof LivingEntity ltTarget && ltTarget.isAlive()) {
			double distToTgt = entity.distanceTo(combatTarget);
			if (distToTgt >= 4.5 && distToTgt <= 7.5 && !ltTarget.isUsingItem() && !isTargetHighlyDangerous(ltTarget)) {
				if (rotLiv.getRandom().nextDouble() < 0.008) {
					entity.getPersistentData().putDouble(K_CAT, 80.0 + rotLiv.getRandom().nextInt(41));
					entity.getPersistentData().putDouble(K_CAC, 500.0);
					String currentItem = BuiltInRegistries.ITEM.getKey(ltTarget.getMainHandItem().getItem()).toString();
					entity.getPersistentData().putString(K_CASI, currentItem);
					if (world instanceof ServerLevel _level) {
						_level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1.0, entity.getZ(), 15, 0.3, 0.4, 0.3, 0.05);
					}
				}
			}
		}

		boolean targetOnPillar = false;
		if (combatTarget != null && isTargetPillaring(world, combatTarget, entity)) {
			double tdx = combatTarget.getX() - entity.getX();
			double tdz = combatTarget.getZ() - entity.getZ();
			double distSqXZ = tdx * tdx + tdz * tdz;
			double maxPillarDist = ROT_PILLAR_BACK_OFF_DISTANCE + 2.0;
			if (distSqXZ < maxPillarDist * maxPillarDist) {
				targetOnPillar = true;
			}
		}

		if (!targetOnPillar && combatTarget instanceof Player && distToTarget < ROT_PLAYER_BACK_OFF_DISTANCE) {
			Vec3 pushBack = entity.position().subtract(combatTarget.position());
			if (pushBack.horizontalDistanceSqr() > 0.0) {
				pushBack = new Vec3(pushBack.x, 0, pushBack.z).normalize().scale(0.12);
				entity.setDeltaMovement(pushBack.x, entity.getDeltaMovement().y, pushBack.z);
			} else {
				entity.setDeltaMovement(0.1, entity.getDeltaMovement().y, 0.1);
			}
		}

		if (entity instanceof Mob mob) {
			double activeAdaptTicks = entity.getPersistentData().getDouble(K_CAT);
			if (ENABLE_CONTROLLED_ADAPTATION && activeAdaptTicks > 0) {
				mob.getNavigation().stop();
				mob.getLookControl().setLookAt(entity.getX() + entity.getLookAngle().x * 2.0, entity.getY() + entity.getEyeHeight(), entity.getZ() + entity.getLookAngle().z * 2.0, 0.0F, 0.0F);
				if ((entity instanceof LivingEntity _liv ? _liv.hurtTime : 0) <= 0) {
					if (entity.onGround()) {
						entity.setDeltaMovement(0.0, entity.getDeltaMovement().y, 0.0);
					}
				}
				if (world instanceof ServerLevel _level && entity.tickCount % 2 == 0) {
					_level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), 3, 0.2, 0.1, 0.2, 0.01);
				}
				if (combatTarget instanceof LivingEntity ltTarget) {
					String currentItem = BuiltInRegistries.ITEM.getKey(ltTarget.getMainHandItem().getItem()).toString();
					String startItem = getRotPersistentString(entity, K_CASI, "");
					boolean itemHotSwapped = !startItem.isEmpty() && !currentItem.equals(startItem);
					boolean targetDangerous = isTargetHighlyDangerous(ltTarget);
					boolean isUsingItem = ltTarget.isUsingItem();

					if (itemHotSwapped || targetDangerous || isUsingItem) {
						entity.getPersistentData().putDouble(K_CAT, 0.0);
						entity.getPersistentData().putDouble(K_CAC, 500.0);
						if (world instanceof ServerLevel _sLevel) {
							_sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 15, 0.2, 0.4, 0.2, 0.05);
							_sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1.0, entity.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
						}
						boolean unlockedTP = entity.getPersistentData().getBoolean(K_UT);
						double tpCd = entity.getPersistentData().getDouble(K_TP_DODGE_CD);
						if (unlockedTP && tpCd <= 0) {
							tryPredictiveDodge(world, entity, ltTarget, distToTarget);
						} else {
							entity.getPersistentData().putBoolean(K_IB, true);
							entity.getPersistentData().putDouble("sentinel_block_ticks", 20.0);
						}
						return;
					}
				}
				return;
			}

			boolean isHeavyPunchingAI = false;
			isHeavyPunchingAI = entity.getEntityData().get(RotEntity.DATA_is_heavy_left_punching) || entity.getEntityData().get(RotEntity.DATA_is_heavy_right_punching);

			if (isHeavyPunchingAI) {
				mob.getNavigation().stop();
				entity.setDeltaMovement(entity.getDeltaMovement().x * 0.05, entity.getDeltaMovement().y, entity.getDeltaMovement().z * 0.05);
			} else if (entity.getPersistentData().getDouble(K_SSST) > 0) {
				mob.getNavigation().stop();
			} else {
				double pathSpeed = ROT_WALK_SPEED;
				
				boolean isWroughtnaut = false;
				boolean isStuck = false;
				if (combatTarget instanceof LivingEntity ltTarget) {
					String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(ltTarget.getType()).toString().toLowerCase();
					if (typeKey.contains("wroughtnaut") || typeKey.contains("ferrous_wroughtnaut")) {
						isWroughtnaut = true;
						isStuck = isWroughtnautStuck(ltTarget);
					}
				}

				if (isWroughtnaut) {
					if (isStuck) {
						float tYaw = combatTarget.getYRot() * ((float) Math.PI / 180F);
						double behindX = combatTarget.getX() + Math.sin(tYaw) * 1.8;
						double behindZ = combatTarget.getZ() - Math.cos(tYaw) * 1.8;
						double behindY = combatTarget.getY();

						if (getRotPersistentBoolean(entity, K_UT, false) && entity.getPersistentData().getDouble(K_TP_FLANK_CD) <= 0) {
							entity.getPersistentData().putDouble(K_TP_FLANK_CD, 50.0);
							entity.teleportTo(behindX, behindY, behindZ);
							if (world instanceof ServerLevel sLevel) {
								sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1.0, entity.getZ(), 20, 0.2, 0.5, 0.2, 0.1);
								playHostileSound(sLevel, entity.getX(), entity.getY(), entity.getZ(), "entity.enderman.teleport", 1.0F, 1.0F);
							}
						}

						pathSpeed = ROT_RUN_SPEED * 1.4;
						if (!targetOnPillar && mob.tickCount % 2 == 0) {
							mob.getNavigation().moveTo(behindX, behindY, behindZ, pathSpeed);
						}
					} else {
						boolean isAttacking = false;
						if (combatTarget instanceof LivingEntity ltTarget) {
							isAttacking = isWroughtnautAttacking(ltTarget);
						}

						if (isAttacking) {
							boolean needsRetreat = distToTarget < 6.5;
							if (needsRetreat) {
								mob.getNavigation().stop();

								double lookDx = combatTarget.getX() - entity.getX();
								double lookDz = combatTarget.getZ() - entity.getZ();
								double lookDy = combatTarget.getEyeY() - entity.getEyeY();
								double flatDist = Math.sqrt(lookDx * lookDx + lookDz * lookDz);

								if (flatDist > 1.0e-5) {
									float targetYaw = (float) (net.minecraft.util.Mth.atan2(lookDz, lookDx) * (180.0D / Math.PI)) - 90.0F;
									float targetPitch = (float) (-(net.minecraft.util.Mth.atan2(lookDy, flatDist) * (180.0D / Math.PI)));

									entity.setYRot(targetYaw);
									entity.yRotO = targetYaw;
									if (entity instanceof LivingEntity le) {
										le.setYHeadRot(targetYaw);
										le.yHeadRotO = targetYaw;
										le.setYBodyRot(targetYaw);
										le.yBodyRotO = targetYaw;
									}
									entity.setXRot(targetPitch);
									entity.xRotO = targetPitch;

									mob.getLookControl().setLookAt(combatTarget.getX(), combatTarget.getEyeY(), combatTarget.getZ(), 30.0F, 30.0F);
								}

								double dx = entity.getX() - combatTarget.getX();
								double dz = entity.getZ() - combatTarget.getZ();
								double dist = Math.sqrt(dx * dx + dz * dz);
								if (dist < 0.1) {
									dx = 1.0;
									dz = 0.0;
									dist = 1.0;
								}
								double nx = dx / dist;
								double nz = dz / dist;

								double speed = 0.28;
								if ((entity instanceof LivingEntity _liv ? _liv.hurtTime : 0) <= 0) {
									entity.setDeltaMovement(nx * speed, entity.getDeltaMovement().y, nz * speed);
								}
								entity.hasImpulse = true;
							} else {
								mob.getNavigation().stop();
								if ((entity instanceof LivingEntity _liv ? _liv.hurtTime : 0) <= 0 && entity.onGround()) {
									entity.setDeltaMovement(0.0, entity.getDeltaMovement().y, 0.0);
								}
								mob.getLookControl().setLookAt(combatTarget.getX(), combatTarget.getEyeY(), combatTarget.getZ(), 30.0F, 30.0F);
							}
						} else {
							if (mob.tickCount % 2 == 0) {
								mob.getNavigation().moveTo(combatTarget, ROT_WALK_SPEED);
							}
						}
					}
				} else {
					boolean isLowHealth = entity instanceof LivingEntity liv && liv.getHealth() < (liv.getMaxHealth() * 0.5f);
					boolean isAnalyzed = true;
					if (combatTarget instanceof Player p) {
						isAnalyzed = getRotPersistentBoolean(entity, "analyzed_species_minecraft:player", false);
					}
					
					if (distToTarget > 6.0 || isLowHealth || (combatTarget instanceof LivingEntity lt && lt.isUsingItem())) {
						pathSpeed = ROT_RUN_SPEED;
					} else if (distToTarget > ROT_PLAYER_BACK_OFF_DISTANCE + 2.0) {
						pathSpeed = ROT_WALK_SPEED;
					} else if (distToTarget > ROT_PLAYER_BACK_OFF_DISTANCE + 0.4) {
						pathSpeed = 0.4;
					} else {
						pathSpeed = 0.0;
					}

					if (entity.getPersistentData().getDouble(K_SSCT) > 0 || entity.getPersistentData().getDouble(K_SSFT) > 0
						|| entity.getPersistentData().getDouble(K_SCCT) > 0 || entity.getPersistentData().getDouble(K_SCFT) > 0) {
						pathSpeed = 0.1;
					}
					boolean rotInWater = entity instanceof LivingEntity _rliv && (_rliv.isInWater() || _rliv.isInLava());
					if (!rotInWater && !targetOnPillar && mob.tickCount % 5 == 0) {
						if (distToTarget > ROT_PLAYER_BACK_OFF_DISTANCE + 0.4) {
							mob.getNavigation().moveTo(combatTarget, pathSpeed);
						} else {
							mob.getNavigation().stop();
						}
					} else if (rotInWater) {
						mob.getNavigation().stop();
					}
				}
			}

		checkTrenchAndJump(world, entity, combatTarget);

		if (!isRotChannelingAbility(entity) && combatTarget instanceof LivingEntity livTgt && getRotPersistentBoolean(entity, K_UT, false)) {
			double distToTargetNow = entity.distanceTo(livTgt);
			double lastRecordedDist = entity.getPersistentData().getDouble(K_ALD);
			Vec3 velocity = entity.getDeltaMovement();
			Vec3 toTarget = livTgt.position().subtract(entity.position()).normalize();
			double dotProduct = velocity.x * toTarget.x + velocity.z * toTarget.z;
			boolean isPermanentlyLearned = getRotPersistentBoolean(entity, K_AFR, false);

			boolean isWrought = false;
			String typeKeyStr = BuiltInRegistries.ENTITY_TYPE.getKey(livTgt.getType()).toString().toLowerCase();
			if (typeKeyStr.contains("wroughtnaut") || typeKeyStr.contains("ferrous_wroughtnaut")) {
				isWrought = true;
			}

			boolean isRepelled = !isWrought && ((distToTargetNow < 14.0 && dotProduct < -0.15) || (distToTargetNow < 14.0 && distToTargetNow > lastRecordedDist + 0.15 && entity.getPersistentData().getDouble(K_SRPT) > 5));
			if (isRepelled) {
				entity.getPersistentData().putDouble(K_SRPT, entity.getPersistentData().getDouble(K_SRPT) + 1);
				double neededPushTicks = isPermanentlyLearned ? 6.0 : 35.0;

				if (entity.getPersistentData().getDouble(K_SRPT) >= neededPushTicks && entity.getPersistentData().getDouble(K_TP_FLANK_CD) <= 0) {
					float tYaw = livTgt.getYRot() * ((float) Math.PI / 180F);
					double behindX = livTgt.getX() + Math.sin(tYaw) * 1.5;
					double behindZ = livTgt.getZ() - Math.cos(tYaw) * 1.5;
					double behindY = livTgt.getY();
					
					teleportEntity(entity, behindX, behindY, behindZ);
					entity.setDeltaMovement(0, 0, 0);
					entity.getPersistentData().putDouble(K_TP_FLANK_CD, 80);
					entity.getPersistentData().putDouble(K_SRPT, 0);

					if (!isPermanentlyLearned) {
						setRotPersistentBoolean(entity, K_AFR, true);
						announceLearnedAbility(entity);
					}

					if (world instanceof ServerLevel level) {
						level.sendParticles(ParticleTypes.PORTAL, behindX, behindY + 1.0, behindZ, 15, 0.3, 0.3, 0.3, 0.1);
						playHostileSound(level, behindX, behindY, behindZ, "entity.enderman.teleport", 1.2F, 0.7F);
					}
				}
			} else {
				entity.getPersistentData().putDouble(K_SRPT, Math.max(0, entity.getPersistentData().getDouble(K_SRPT) - 1));
			}
		}

		double combatTicks = entity.getPersistentData().getDouble(K_SCT);
		boolean isWardenCombatTarget = "minecraft:warden".equals(BuiltInRegistries.ENTITY_TYPE.getKey(combatTarget.getType()).toString());
		boolean unlockedSonicBoom = getRotPersistentBoolean(entity, K_USB2, false);
		boolean isFalling = !entity.onGround() || getRotPersistentBoolean(entity, K_IFH, false) || entity.getDeltaMovement().y() < -0.2;
		boolean targetInRange = distToTarget <= SONIC_BOOM_RANGE && distToTarget >= SONIC_BOOM_MIN_DIST;
		boolean hasLos = entity instanceof LivingEntity ls ? ls.hasLineOfSight(combatTarget) : true;

		if (!isFalling && hasLos && targetInRange && !isRotChannelingAbility(entity) && ((isWardenCombatTarget && combatTicks >= 600) || unlockedSonicBoom) && entity.getPersistentData().getDouble(K_SWSC) <= 0
			&& scoreAbility(getAbilityById("sonic_boom"), getCombatContext(entity, combatTarget), entity, combatTarget) > 12.0) {
			fireSuperchargedSonicBoom(world, entity, combatTarget);
			entity.getPersistentData().putDouble(K_SWSC, SONIC_BOOM_COOLDOWN);
			return;
		}

		boolean unlockedTP = getRotPersistentBoolean(entity, K_UT, false);
		if (!isRotChannelingAbility(entity) && combatTarget instanceof LivingEntity livTarget && (unlockedSonicBoom && unlockedTP)) {
			boolean isFlying = livTarget.isFallFlying() || (!livTarget.onGround() && livTarget.getY() > entity.getY() + 2.0 && !livTarget.isInWater() && !livTarget.isInLava());
			if (isFlying) {
				double flightCd = entity.getPersistentData().getDouble(K_SFIC);
				if (flightCd <= 0) {
					if (livTarget instanceof Player p) {
						p.stopFallFlying();
					}
					livTarget.setDeltaMovement(Vec3.ZERO);
					livTarget.hasImpulse = true;
					livTarget.setDeltaMovement(0, -2.5, 0);
					livTarget.hasImpulse = true;
					if (world instanceof ServerLevel level) {
						dealTrueDamageToBosses(livTarget, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_seismic_slam"))), entity), 15.0F * (float) getAdaptationMultiplier(entity));
						double targetNewX = livTarget.getX();
						double targetNewY = livTarget.getY();
						double targetNewZ = livTarget.getZ();
						level.sendParticles(ParticleTypes.EXPLOSION, targetNewX, targetNewY, targetNewZ, 4, 0.4, 0.4, 0.4, 0.05);
						level.sendParticles(ParticleTypes.SONIC_BOOM, targetNewX, targetNewY, targetNewZ, 1, 0.1, 0.1, 0.1, 0.0);
						teleportEntity(entity, targetNewX, targetNewY + 2.0, targetNewZ);
						playHostileSound(level, targetNewX, targetNewY, targetNewZ, "entity.warden.sonic_boom", 1.2F, 0.4F);
					}
					entity.getPersistentData().putDouble(K_SFIC, 80);
					return;
				}
			}
		}

		if (isWither(combatTarget)) {
			double witherCd = entity.getPersistentData().getDouble(K_RWDC);
			if (witherCd <= 0 && Math.random() < 0.015) {
				RotDialoguesProcedure.sendRandomWitherQuote(world, entity, 32.0);
				entity.getPersistentData().putDouble(K_RWDC, 400);
			}
		}

		double dist = combatTarget.position().distanceTo(entity.position());

		int activeMode = 0;
		entity.getPersistentData().putDouble(K_ADAPT_MODE, 0);

		if (ENABLE_BLOCKING && getRotPersistentBoolean(entity, K_IB, false)) {
			double activeTicks = entity.getPersistentData().getDouble(K_RBAT);
			if (activeTicks <= 0) {
				entity.getPersistentData().putBoolean(K_IB, false);
			} else {
				if (entity instanceof Mob _mob2152) {
					_mob2152.getNavigation().stop();
					if (_mob2152.getDeltaMovement().y() > 0) {
						_mob2152.setDeltaMovement(_mob2152.getDeltaMovement().x(), 0.0, _mob2152.getDeltaMovement().z());
					}
				}
				handlePassengerAndGrowth(entity);
				return;
			}
		}

		int armorRipTicks = (int) entity.getPersistentData().getDouble(K_RART);
		if (armorRipTicks > 0) {
			entity.getPersistentData().putDouble(K_RART, armorRipTicks - 1);
			executeArmorRipChoke(world, entity, combatTarget, armorRipTicks - 1);
			handlePassengerAndGrowth(entity);
			return;
		}

		int grappleTicks = (int) entity.getPersistentData().getDouble(K_GRAPPLE_TICKS);
		if (ENABLE_EXTRACTION_GRAPPLE && grappleTicks > 0) {
			entity.getPersistentData().putDouble(K_GRAPPLE_TICKS, grappleTicks - 1);
			executeGrappleSiphon(world, entity, combatTarget, grappleTicks - 1);
			lockLookAtTarget(entity, combatTarget);
			handlePassengerAndGrowth(entity);
			return;
		}

		int tkTicks = (int) entity.getPersistentData().getDouble(K_TK_TICKS);
		if (ENABLE_TELEKINESIS && tkTicks > 0) {
			entity.getPersistentData().putDouble(K_TK_TICKS, tkTicks - 1);
			executeTelekinesis(world, entity, combatTarget, tkTicks - 1);
			lockLookAtTarget(entity, combatTarget);
			handlePassengerAndGrowth(entity);
			return;
		}

		double solarCharge = entity.getPersistentData().getDouble(K_SSCT);
		double solarFire = entity.getPersistentData().getDouble(K_SSFT);

		if (solarFire > 0) {
			if (getRotPersistentDouble(entity, K_SMS, 0.0) > 0 || getRotPersistentDouble(entity, K_SMT, 0.0) > 0) {
				entity.getPersistentData().putDouble(K_SSFT, 0);
				entity.getPersistentData().putDouble(K_SLCT, LASER_CLOSING_TICKS);
				stopHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
			} else {
				entity.getPersistentData().putDouble(K_SSFT, solarFire - 1);
				executeSentinelFaceLaserFiring(world, entity, combatTarget, (int) solarFire);
				handlePassengerAndGrowth(entity);
				if (solarFire == 1.0) {
					entity.getPersistentData().putDouble(K_SLCT, LASER_CLOSING_TICKS);
				}
				return;
			}
		}

		if (solarCharge > 0) {
			if (getRotPersistentDouble(entity, K_SMS, 0.0) > 0 || getRotPersistentDouble(entity, K_SMT, 0.0) > 0) {
				entity.getPersistentData().putDouble(K_SSCT, 0);
				entity.getPersistentData().putDouble(K_SLCT, LASER_CLOSING_TICKS);
				stopHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
			} else {
				entity.getPersistentData().putDouble(K_SSCT, solarCharge + 1);
				executeSentinelFaceLaserCharging(world, entity, combatTarget, (int) solarCharge);
				handlePassengerAndGrowth(entity);
				return;
			}
		}

		double cryoCharge = entity.getPersistentData().getDouble(K_SCCT);
		double cryoFire = entity.getPersistentData().getDouble(K_SCFT);

		if (cryoFire > 0) {
			if (getRotPersistentDouble(entity, K_SMS, 0.0) > 0 || getRotPersistentDouble(entity, K_SMT, 0.0) > 0) {
				entity.getPersistentData().putDouble(K_SCFT, 0);
				entity.getPersistentData().putDouble(K_SLCT, LASER_CLOSING_TICKS);
				stopHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
			} else {
				entity.getPersistentData().putDouble(K_SCFT, cryoFire - 1);
				executeSentinelCryoLaserFiring(world, entity, combatTarget, (int) cryoFire);
				handlePassengerAndGrowth(entity);
				if (cryoFire == 1.0) {
					entity.getPersistentData().putDouble(K_SLCT, LASER_CLOSING_TICKS);
				}
				return;
			}
		}

		if (cryoCharge > 0) {
			if (getRotPersistentDouble(entity, K_SMS, 0.0) > 0 || getRotPersistentDouble(entity, K_SMT, 0.0) > 0) {
				entity.getPersistentData().putDouble(K_SCCT, 0);
				entity.getPersistentData().putDouble(K_SLCT, LASER_CLOSING_TICKS);
				stopHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
			} else {
				entity.getPersistentData().putDouble(K_SCCT, cryoCharge + 1);
				executeSentinelCryoLaserCharging(world, entity, combatTarget, (int) cryoCharge);
				handlePassengerAndGrowth(entity);
				return;
			}
		}

		double witherSkullFire = getRotPersistentDouble(entity, K_SWSFT, 0.0);
		if (witherSkullFire > 0) {
			setRotPersistentDouble(entity, K_SWSFT, witherSkullFire - 1);
			executeSentinelWitherSkullFiring(world, entity, combatTarget, (int) witherSkullFire);
			handlePassengerAndGrowth(entity);
			return;
		}

		interceptEnderPearls(world, entity);

		handleAdaptiveEffects(world, entity, combatTarget, activeMode, dist);

		double globalCd = entity.getPersistentData().getDouble(K_SGAC);
		double coreCd = getDynamicGlobalCooldown(entity);

		if (globalCd <= 0) {
			if (combatTarget instanceof LivingEntity livTarget) {
				boolean isFlying = false;
				if (livTarget instanceof Player p) {
					isFlying = p.isFallFlying() || p.getAbilities().flying;
				} else {
					isFlying = livTarget instanceof net.minecraft.world.entity.animal.FlyingAnimal
						|| livTarget instanceof net.minecraft.world.entity.monster.Phantom
						|| livTarget instanceof net.minecraft.world.entity.monster.Ghast
						|| livTarget instanceof net.minecraft.world.entity.monster.Vex
						|| livTarget instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
						|| isWither(livTarget)
						|| livTarget instanceof net.minecraft.world.entity.ambient.Bat;
				}
				boolean isEligibleScreamTarget = true;
				if (livTarget instanceof Player p) {
					boolean hasElytra = p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(net.minecraft.world.item.Items.ELYTRA);
					if (!hasElytra) {
						isEligibleScreamTarget = false;
					}
				}
				if (isFlying && isEligibleScreamTarget && getRotPersistentBoolean(entity, "unlocked_sonic_scream", false) && entity.getPersistentData().getDouble(K_SSSC) <= 0.0 && dist <= 24.0 && Math.random() < 0.15) {
					entity.getPersistentData().putDouble(K_SSST, 240.0);
					entity.getPersistentData().putDouble(K_SSSC, SONIC_SCREAM_COOLDOWN);
					entity.getPersistentData().putDouble(K_SGAC, coreCd);
					return;
				}
			}

			CombatContext ctx = getCombatContext(entity, combatTarget);
			boolean totemActive = getRotPersistentBoolean(entity, K_STA2, false);

			List<AbilityInfo> availableAbilities = getAvailableAbilities(entity);
			AbilityInfo bestAbility = null;
			double maxScore = 0.0;
			for (AbilityInfo ab : availableAbilities) {
				if ("wither_skulls".equals(ab.id) || "omni_sonic_boom".equals(ab.id) || "solar_beam".equals(ab.id) || "cryo_beam".equals(ab.id) || "telekinesis".equals(ab.id) || "grapple".equals(ab.id)) {
					if ("wither_skulls".equals(ab.id) && shouldAvoidWitherSkulls(entity, combatTarget)) continue;
					double s = scoreAbility(ab, ctx, entity, combatTarget);
					if (s > maxScore) {
						maxScore = s;
						bestAbility = ab;
					}
				}
			}

			double rangedChance = totemActive ? 0.35 : 0.15;
			double _cTicks = entity.getPersistentData().getDouble(K_SCT);
			if (_cTicks > 3000.0) {
				rangedChance *= Math.max(0.1, 1.0 - ((_cTicks - 3000.0) / 12000.0));
			}

			if (maxScore > 5.0 && bestAbility != null && Math.random() < rangedChance) {
				String abId = bestAbility.id;
				if ("wither_skulls".equals(abId)) {
					recordAttack(entity, "wither_skulls");
					setRotPersistentDouble(entity, K_SWSFT, 18.0);
					setRotPersistentBoolean(entity, K_SWSHF, false);
					setRotPersistentDouble(entity, "sentinel_wither_skull_cd", 60.0);
					setRotPersistentDouble(entity, K_SGAC, coreCd);
					playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.wither.ambient", 1.0F, 0.8F);
					return;
				} else if ("omni_sonic_boom".equals(abId)) {
					recordAttack(entity, "omni_sonic_boom");
					entity.getPersistentData().putDouble("sentinel_omni_sonic_cooldown", OMNI_SONIC_BOOM_CD);
					entity.getPersistentData().putDouble(K_SOSCT, OMNI_SONIC_BOOM_ANIMATION_TICKS);
					entity.getPersistentData().putDouble(K_SGAC, coreCd);
					if (world instanceof ServerLevel level) {
						playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sonic_charge", 1.8F, 0.45F);
					}
					return;
				} else if ("solar_beam".equals(abId) || "cryo_beam".equals(abId)) {
					recordAttack(entity, abId);
					double currentHeat = entity.getPersistentData().getDouble(K_SLH);
					entity.getPersistentData().putDouble(K_SLH, Math.min(200.0, currentHeat + 80.0));
					
					entity.getPersistentData().putInt(K_SLTI, combatTarget.getId());
					entity.getPersistentData().putDouble(K_SOLAR_CD, SOLAR_CD + 180);
					entity.getPersistentData().putDouble(K_SGAC, coreCd);
					
					String combatTargetId = BuiltInRegistries.ENTITY_TYPE.getKey(combatTarget.getType()).toString();
					boolean isWardenTarget = combatTargetId.contains("warden");
					boolean isHotTarget = (combatTarget.fireImmune() && !isWardenTarget) || combatTarget.level().dimension() == net.minecraft.world.level.Level.NETHER;
					boolean unlockedSolar = getRotPersistentBoolean(entity, K_USB, false);
					boolean unlockedCryo = getRotPersistentBoolean(entity, K_UCB, false);

					if (getRotPersistentDouble(entity, K_SMS, 0.0) == 0 && getRotPersistentDouble(entity, K_SMT, 0.0) == 0) {
						if ("cryo_beam".equals(abId) || (isHotTarget && unlockedCryo)) {
							entity.getPersistentData().putDouble(K_SCCT, 1);
						} else {
							entity.getPersistentData().putDouble(K_SSCT, 1);
						}
					}
					return;
				} else if ("telekinesis".equals(abId)) {
					recordAttack(entity, "telekinesis");
					entity.getPersistentData().putDouble(K_TK_TICKS, 25);
					entity.getPersistentData().putDouble(K_TK_CD, TK_CD);
					entity.getPersistentData().putDouble(K_SGAC, coreCd);
					playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.heartbeat", 0.8F, 0.50F);
					return;
				} else if ("grapple".equals(abId)) {
					recordAttack(entity, "grapple");
					entity.getPersistentData().putDouble(K_GRAPPLE_TICKS, 40);
					entity.getPersistentData().putDouble(K_GRAPPLE_CD, GRAPPLE_CD);
					entity.getPersistentData().putDouble(K_SGAC, coreCd);
					playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.spider.ambient", 1.2F, 0.6F);
					return;
				}
			}
		}

		if (!isChannelingAbility(entity) && ENABLE_ARMOR_RIP && combatTicks >= 240 && globalCd <= 0 && entity.getPersistentData().getDouble(K_RARC) <= 0 && dist <= 2.2 && combatTarget instanceof LivingEntity livingTarget && entity.getPersistentData().getDouble(K_STIT) <= 0) {
			if (livingTarget.getBbWidth() < entity.getBbWidth() && livingTarget.getBbHeight() < entity.getBbHeight()) {
				if (entity instanceof Mob _mob2377 && _mob2377.hasLineOfSight(livingTarget)) {
					boolean hasIndestructibleArmor = false;
					boolean hasAnyArmor = false;
					for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
						if (slot.isArmor()) {
							ItemStack armorStack = livingTarget.getItemBySlot(slot);
							if (!armorStack.isEmpty()) {
								hasAnyArmor = true;
								if (isIndestructibleArmorStack(armorStack)) {
									hasIndestructibleArmor = true;
								}
							}
						}
					}
					boolean holdsTotem = livingTarget instanceof Player pCheck && (!pCheck.getMainHandItem().isEmpty() && (pCheck.getMainHandItem().getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING || BuiltInRegistries.ITEM.getKey(pCheck.getMainHandItem().getItem()).toString().equals("avaritia:infinity_totem")) || (!pCheck.getOffhandItem().isEmpty() && (pCheck.getOffhandItem().getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING || BuiltInRegistries.ITEM.getKey(pCheck.getOffhandItem().getItem()).toString().equals("avaritia:infinity_totem"))));
					if (hasAnyArmor || holdsTotem) {
						double baseRipChance = holdsTotem ? 0.20 : (hasIndestructibleArmor ? ARMOR_RIP_CHANCE_INDESTRUCTIBLE : ARMOR_RIP_CHANCE_REGULAR);
						double contextMultiplier = 1.0;
						if (livingTarget.isUsingItem()) contextMultiplier *= 5.0;
						if (livingTarget.getHealth() < livingTarget.getMaxHealth() * 0.35) contextMultiplier *= 3.0;
						double ripChance = Math.min(0.60, baseRipChance * contextMultiplier);
						if (Math.random() < ripChance) {
							entity.getPersistentData().putDouble(K_RART, ARMOR_RIP_TICKS);
							entity.getPersistentData().putBoolean(K_IAR, true);
							entity.getPersistentData().putDouble(K_RCLY, entity.getYRot());
							entity.getPersistentData().putDouble(K_RARC, ARMOR_RIP_COOLDOWN);
							entity.getPersistentData().putDouble(K_SGAC, coreCd);
							entity.getPersistentData().putDouble("rot_choke_hits_taken", 0);
							boolean totemActive = getRotPersistentBoolean(entity, K_STA2, false);
							int minHits = totemActive ? CHOKE_TOTEM_MIN_HITS : CHOKE_MIN_HITS;
							int maxHits = totemActive ? CHOKE_TOTEM_MAX_HITS : CHOKE_MAX_HITS;
							int requiredHits = minHits + net.minecraft.util.RandomSource.create().nextInt(maxHits - minHits + 1);
							entity.getPersistentData().putDouble("rot_choke_break_hits", requiredHits);

							if (combatTarget instanceof LivingEntity livTarget && livTarget.isBlocking()) {
								if (livTarget instanceof Player player) {
									player.getCooldowns().addCooldown(net.minecraft.world.item.Items.SHIELD, 100);
									player.stopUsingItem();
									if (world instanceof ServerLevel level) {
										level.broadcastEntityEvent(player, (byte) 30);
									}
								} else {
									livTarget.stopUsingItem();
								}
							}

							playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sniff", 1.2F, 0.5F);
							return;
						}
					}
				}
			}
		}

		if (ENABLE_EXTRACTION_GRAPPLE && !isRotChannelingAbility(entity) && globalCd <= 0 && getRotPersistentBoolean(entity, K_UG, false) && entity.getPersistentData().getDouble(K_GRAPPLE_CD) <= 0 && dist <= 8.5 && Math.random() < 0.03) {
			entity.getPersistentData().putDouble(K_GRAPPLE_TICKS, 32);
			entity.getPersistentData().putDouble(K_GRAPPLE_CD, GRAPPLE_CD);
			entity.getPersistentData().putDouble(K_SGAC, coreCd);
			playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sniff", 1.1F, 0.85F);
			return;
		}

		tryPredictiveDodge(world, entity, combatTarget, dist);
		tryFlankTeleport(world, entity, combatTarget, dist);

		double meleeWindup = entity.getPersistentData().getDouble(K_SMW);
		double meleeCooldown = entity.getPersistentData().getDouble(K_SMC);

		if (meleeWindup > 0) {
			lockLookAtTarget(entity, combatTarget);
			if (world instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1.2, entity.getZ(), 2, 0.3, 0.3, 0.3, 0.05);
			}
			if (meleeWindup == 1) {
				executeSentinelPunch(world, entity, combatTarget);
				entity.getPersistentData().putDouble(K_SMC, 20);
			}
		} else if (!getRotPersistentBoolean(entity, K_SWI, false) && entity.getPersistentData().getDouble(K_AFPT) <= 0 && meleeCooldown <= 0 && dist <= ((combatTarget instanceof Player) ? 2.5 : 3.2)) {
			entity.getPersistentData().putDouble(K_SMW, 8);
			playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.player.attack.weak", 0.9F, 0.85F);
			if (world instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + 1.2, entity.getZ(), 5, 0.4, 0.4, 0.4, 0.1);
			}
		}

		if (combatTarget != null) {
			lockLookAtTarget(entity, combatTarget);
		}

		handleForwardCarveMining(world, entity, combatTarget);
		handlePassengerAndGrowth(entity);
	}
	}

	private static void handleAdaptiveEffects(LevelAccessor world, Entity self, Entity target, int mode, double dist) {
		if (!(world instanceof ServerLevel level) || !(self instanceof LivingEntity ls)) return;

		if (mode == 1) {
			BlockPos centerPos = BlockPos.containing(ls.position());
			for (BlockPos bp : BlockPos.betweenClosed(centerPos.offset(-2, -1, -2), centerPos.offset(2, 1, 2))) {
				BlockState st = level.getBlockState(bp);
				if (st.is(net.minecraft.world.level.block.Blocks.ICE) || st.is(net.minecraft.world.level.block.Blocks.SNOW)) {
					level.destroyBlock(bp, false);
				}
			}

			if (dist <= 1.5 && self.tickCount % 4 == 0) {
				target.setRemainingFireTicks(60);
				dealTrueDamageToBosses(target, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_inferno_laser"))), self), 4.0F * (float) getAdaptationMultiplier(self));
			}

			ls.setRemainingFireTicks(0);
			ls.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

		} else if (mode == 2) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, ls.getX(), ls.getY() + 1.3, ls.getZ(), 1, 0.4, 0.5, 0.4, 0.005);

			if (self.tickCount % 8 == 0) {
				List<Entity> nearby = level.getEntitiesOfClass(Entity.class, new AABB(self.position(), self.position()).inflate(6.0), e -> e != self && (e instanceof LivingEntity));
				for (Entity ent : nearby) {
					if (ent instanceof Player p) {
						if (p instanceof ServerPlayer sp) {
							if (sp.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.SURVIVAL) continue;
						} else {
							if (p.isCreative() || p.isSpectator()) continue;
						}
					}
					if (ent instanceof LivingEntity liv) {
					}
				}
			}
		}
	}

	private static void disablePlayerShield(LivingEntity victim, int cooldownTicks) {
		if (victim instanceof Player player && player.isBlocking()) {
			player.disableShield();
			net.minecraft.world.item.ItemStack useItem = player.getUseItem();
			if (!useItem.isEmpty()) {
				player.getCooldowns().addCooldown(useItem.getItem(), cooldownTicks);
			}
			player.stopUsingItem();
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.SHIELD_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
			if (player.level() instanceof ServerLevel sLevel) {
				sLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getEyeY(), player.getZ(), 12, 0.2, 0.2, 0.2, 0.1);
			}
		}
	}

	private static Vec3 updateRotLaserAim(Entity entity, Vec3 facePos, Entity target, boolean firing) {
		double curAimX = getRotPersistentDouble(entity, K_SLAX, 0.0);
		double curAimY = getRotPersistentDouble(entity, K_SLAY, 0.0);
		double curAimZ = getRotPersistentDouble(entity, K_SLAZ, 0.0);
		Vec3 currentAim = (curAimX != 0.0 || curAimY != 0.0 || curAimZ != 0.0)
			? new Vec3(curAimX, curAimY, curAimZ).normalize()
			: Vec3.ZERO;

		Vec3 targetPos = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
		Vec3 desiredAim = targetPos.subtract(facePos);
		if (desiredAim.lengthSqr() < 0.0001) {
			desiredAim = entity.getLookAngle();
		}
		desiredAim = desiredAim.normalize();

		if (currentAim.lengthSqr() < 0.0001) {
			entity.getPersistentData().putDouble(K_SLAX, desiredAim.x);
			entity.getPersistentData().putDouble(K_SLAY, desiredAim.y);
			entity.getPersistentData().putDouble(K_SLAZ, desiredAim.z);
			return desiredAim;
		}

		double turnRate = firing ? 0.055 : 0.095;
		Vec3 newAim = currentAim.lerp(desiredAim, turnRate).normalize();
		entity.getPersistentData().putDouble(K_SLAX, newAim.x);
		entity.getPersistentData().putDouble(K_SLAY, newAim.y);
		entity.getPersistentData().putDouble(K_SLAZ, newAim.z);

		if (entity instanceof Mob mob) {
			double dx = newAim.x;
			double dy = newAim.y;
			double dz = newAim.z;
			double dh = Math.sqrt(dx * dx + dz * dz);
			float targetYRot = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
			float targetXRot = (float) (-(Mth.atan2(dy, Math.max(0.001, dh)) * (180F / Math.PI)));
			mob.setYRot(targetYRot);
			mob.setXRot(targetXRot);
			mob.setYHeadRot(targetYRot);
			mob.yBodyRot = targetYRot;
		}

		return newAim;
	}

	private static void spawnLaserChargeRingParticles(ServerLevel level, Entity entity, Vec3 facePos, double radius, int particleCount, net.minecraft.core.particles.ParticleOptions particle) {
		for (int i = 0; i < particleCount; i++) {
			double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
			double yOffset = (entity.getRandom().nextDouble() - 0.5) * radius * 0.5;
			Vec3 pPos = facePos.add(Math.cos(angle) * radius, yOffset, Math.sin(angle) * radius);
			level.sendParticles(particle, pPos.x, pPos.y, pPos.z, 1, 0.01, 0.01, 0.01, 0.0);
		}
	}

	private static void spawnFaceJitterBurst(ServerLevel level, LivingEntity living, Vec3 facePos, net.minecraft.core.particles.ParticleOptions particle) {
		for (int i = 0; i < 4; i++) {
			double oX = (living.getRandom().nextDouble() - 0.5) * 0.42;
			double oY = (living.getRandom().nextDouble() - 0.5) * 0.42;
			double oZ = (living.getRandom().nextDouble() - 0.5) * 0.42;
			level.sendParticles(particle, facePos.x + oX, facePos.y + oY, facePos.z + oZ, 1, 0.01, 0.01, 0.01, 0.0);
		}
	}

	private static void spawnBeamCylinderHelix(ServerLevel level, Vec3 facePos, Vec3 direction, Vec3 beamEnd, net.minecraft.core.particles.ParticleOptions coreParticle, net.minecraft.core.particles.ParticleOptions helixParticle) {
		double spacing = 0.15 / Mth.clamp(PARTICLE_QUALITY, 0.1, 1.0);
		Vec3 upVec = Math.abs(direction.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
		Vec3 sideVec = direction.cross(upVec).normalize();
		Vec3 vertVec = direction.cross(sideVec).normalize();
		double time = level.getGameTime() * 0.38;
		double beamLength = beamEnd.distanceTo(facePos);

		for (double d = 0.0; d <= beamLength; d += spacing) {
			Vec3 pos = facePos.add(direction.scale(d));

			level.sendParticles(coreParticle, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);

			double angle = (d * 5.0) + time;
			double cylinderRadius = 0.15;

			Vec3 offset1 = sideVec.scale(Math.cos(angle) * cylinderRadius).add(vertVec.scale(Math.sin(angle) * cylinderRadius));
			Vec3 p1 = pos.add(offset1);
			level.sendParticles(helixParticle, p1.x, p1.y, p1.z, 1, 0.01, 0.01, 0.01, 0.0);

			Vec3 offset2 = sideVec.scale(Math.cos(angle + Math.PI) * cylinderRadius).add(vertVec.scale(Math.sin(angle + Math.PI) * cylinderRadius));
			Vec3 p2 = pos.add(offset2);
			level.sendParticles(helixParticle, p2.x, p2.y, p2.z, 1, 0.01, 0.01, 0.01, 0.0);
		}
	}

	private static void executeSentinelFaceLaserCharging(LevelAccessor world, Entity entity, Entity target, int chargeTicks) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);

		Vec3 facePos = living.getEyePosition(1.0F).add(0.0, LASER_Y_OFFSET, 0.0);
		if (target != null) {
			updateRotLaserAim(entity, facePos, target, false);
		}

		net.minecraft.core.particles.ParticleOptions beamPart = getAdaptiveBeamParticle(entity);

		double progress = (double) chargeTicks / 40.0;
		double radius = 1.9 - progress * 1.6;
		int particleCount = 2 + (int) (progress * 5.0);
		spawnLaserChargeRingParticles(level, entity, facePos, radius, particleCount, beamPart);

		level.sendParticles(ParticleTypes.LAVA, facePos.x, facePos.y, facePos.z, 1, 0.15, 0.15, 0.15, 0.01);

		if (chargeTicks % 6 == 0) {
			playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "item.firecharge.use", 0.6F, 1.5F);
		}

		if (chargeTicks >= 40) {
			entity.getPersistentData().putDouble(K_SSCT, 0);
			entity.getPersistentData().putDouble(K_SSFT, 123);
			playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 4.5F, 0.65F);
		}
	}

	private static void executeSentinelFaceLaserFiring(LevelAccessor world, Entity entity, Entity target, int fireTicks) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);

		int targetId = (int) getRotPersistentDouble(entity, K_SLTI, 0.0);
		if (targetId == 0 && entity.getPersistentData().contains(K_SLTI)) {
			targetId = entity.getPersistentData().getInt(K_SLTI);
		}
		if (targetId != 0) {
			Entity storedTarget = level.getEntity(targetId);
			if (storedTarget != null && storedTarget.isAlive()) {
				target = storedTarget;
			}
		}

		if (target == null || !target.isAlive()) {
			entity.getPersistentData().putDouble(K_SSFT, 0);
			entity.getPersistentData().putDouble(K_SLCT, LASER_CLOSING_TICKS);
			entity.getPersistentData().putDouble(K_SOLAR_CD, SOLAR_CD + RandomSource.create().nextInt(40));
			entity.getPersistentData().remove(K_SLTI);
			entity.getPersistentData().remove(K_SLAX);
			entity.getPersistentData().remove(K_SLAY);
			entity.getPersistentData().remove(K_SLAZ);
			stopHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
			return;
		}

		Vec3 facePos = living.getEyePosition(1.0F).add(0.0, LASER_Y_OFFSET, 0.0);
		Vec3 direction = updateRotLaserAim(entity, facePos, target, true);

		double maxRange = 96.0;
		Vec3 beamEnd = facePos.add(direction.scale(maxRange));

		BlockHitResult blockHit = level.clip(new ClipContext(facePos, beamEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
		Vec3 firstIntersectionPos = beamEnd;
		boolean hitBlock = false;
		if (blockHit.getType() != HitResult.Type.MISS) {
			firstIntersectionPos = blockHit.getLocation();
			hitBlock = true;
		}

		AABB searchRange = new AABB(facePos, firstIntersectionPos).inflate(1.5);
		List<Entity> possibleEntities = level.getEntitiesOfClass(Entity.class, searchRange, e -> e != entity && e != living && e.isAlive());

		Entity hitEntity = null;
		double closestDist = facePos.distanceTo(firstIntersectionPos);
		Vec3 finalBeamEnd = firstIntersectionPos;

		for (Entity possible : possibleEntities) {
			if (possible instanceof Player p && p.isCreative()) continue;
			if (possible instanceof net.minecraft.world.entity.projectile.ThrownEnderpearl pearl && pearl.isAlive()) {
				level.sendParticles(ParticleTypes.EXPLOSION, pearl.getX(), pearl.getY(), pearl.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
				level.sendParticles(ParticleTypes.LAVA, pearl.getX(), pearl.getY(), pearl.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
				playHostileSound(level, pearl.getX(), pearl.getY(), pearl.getZ(), "entity.generic.explode", 0.8F, 1.8F);
				pearl.discard();
				continue;
			}
			AABB possibleBb = possible.getBoundingBox().inflate(0.3);
			java.util.Optional<Vec3> clipResult = possibleBb.clip(facePos, firstIntersectionPos);
			if (clipResult.isPresent()) {
				double distToClip = facePos.distanceTo(clipResult.get());
				if (distToClip < closestDist) {
					closestDist = distToClip;
					finalBeamEnd = clipResult.get();
					hitEntity = possible;
					hitBlock = false;
				}
			}
		}

		beamEnd = finalBeamEnd;

		net.minecraft.core.particles.ParticleOptions beamPart = getAdaptiveBeamParticle(entity);

		spawnFaceJitterBurst(level, living, facePos, beamPart);
		if (living.getRandom().nextFloat() < 0.25F) {
			level.sendParticles(ParticleTypes.LAVA, facePos.x, facePos.y, facePos.z, 1, 0.2, 0.2, 0.2, 0.1);
		}

		spawnBeamCylinderHelix(level, facePos, direction, beamEnd, beamPart, beamPart);

		if (hitBlock && blockHit.getType() == HitResult.Type.BLOCK) {
			BlockPos hitPos = blockHit.getBlockPos();
			BlockState hitState = level.getBlockState(hitPos);
			float hardness = hitState.getDestroySpeed(level, hitPos);
			if (hardness >= 0.0F && hardness <= 50.0F) {
				int lastDrillX = entity.getPersistentData().getInt(K_SLDX);
				int lastDrillY = entity.getPersistentData().getInt(K_SLDY);
				int lastDrillZ = entity.getPersistentData().getInt(K_SLDZ);
				double drillProgress = entity.getPersistentData().getDouble(K_SLDP);

				if (lastDrillX != hitPos.getX() || lastDrillY != hitPos.getY() || lastDrillZ != hitPos.getZ()) {
					drillProgress = 0.0;
					entity.getPersistentData().putInt(K_SLDX, hitPos.getX());
					entity.getPersistentData().putInt(K_SLDY, hitPos.getY());
					entity.getPersistentData().putInt(K_SLDZ, hitPos.getZ());
				}

				double progressAdd = 1.0 / Math.max(1.0, hardness * 2.5);
				if (getRotPersistentBoolean(entity, K_STA2, false)) {
					progressAdd *= 3.0;
				}
				drillProgress += progressAdd;
				entity.getPersistentData().putDouble(K_SLDP, drillProgress);

				if (level.getRandom().nextFloat() < 0.45F) {
					level.sendParticles(ParticleTypes.CRIT, hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.05);
					level.sendParticles(ParticleTypes.LAVA, hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
				}

				if (drillProgress >= 1.0) {
					level.destroyBlock(hitPos, false);
					playHostileSound(level, hitPos.getX(), hitPos.getY(), hitPos.getZ(), "entity.item.break", 0.8F, 0.8F);

					for (int dy = -1; dy <= 2; dy++) {
						for (int dx = -1; dx <= 1; dx++) {
							for (int dz = -1; dz <= 1; dz++) {
								if (dx*dx + dy*dy + dz*dz <= 2.5) {
									BlockPos adj = hitPos.offset(dx, dy, dz);
									BlockState adjState = level.getBlockState(adj);
									float adjHard = adjState.getDestroySpeed(level, adj);
									if (adjHard >= 0.0F && adjHard <= hardness + 1.5F && !adjState.isAir()) {
										level.destroyBlock(adj, false);
									}
								}
							}
						}
					}
					entity.getPersistentData().putDouble(K_SLDP, 0.0);
				}
			}

			BlockPos firePos = hitPos.relative(blockHit.getDirection());
			if (level.isEmptyBlock(firePos) && level.getBlockState(firePos.below()).isSolidRender(level, firePos.below())) {
				level.setBlock(firePos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 3);
			}
		}

		if (hitBlock && level.getRandom().nextFloat() < 0.3F) {
			BlockPos nearBeam = BlockPos.containing(
				beamEnd.x + (level.getRandom().nextDouble() - 0.5) * 4.0,
				beamEnd.y + (level.getRandom().nextDouble() - 0.5) * 4.0,
				beamEnd.z + (level.getRandom().nextDouble() - 0.5) * 4.0
			);
			if (level.isEmptyBlock(nearBeam) && level.getBlockState(nearBeam.below()).isSolidRender(level, nearBeam.below())) {
				level.setBlock(nearBeam, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 3);
			}
		}

		if (fireTicks % 3 == 0) {
			level.sendParticles(ParticleTypes.EXPLOSION, beamEnd.x, beamEnd.y, beamEnd.z, 1, 0.1, 0.1, 0.1, 0.05);
		}

		level.sendParticles(ParticleTypes.DRIPPING_LAVA, beamEnd.x, beamEnd.y, beamEnd.z, 3, 0.1, 0.1, 0.1, 0.02);

		Entity targetVictim = (hitEntity != null) ? hitEntity : target;
		if (targetVictim instanceof net.minecraft.world.entity.projectile.ThrownEnderpearl pearl && pearl.isAlive()) {
			level.sendParticles(ParticleTypes.EXPLOSION, pearl.getX(), pearl.getY(), pearl.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
			level.sendParticles(ParticleTypes.LAVA, pearl.getX(), pearl.getY(), pearl.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
			playHostileSound(level, pearl.getX(), pearl.getY(), pearl.getZ(), "entity.generic.explode", 0.8F, 1.8F);
			pearl.discard();
		}

		if (entity.tickCount % 6 == 0) {
			if (targetVictim instanceof LivingEntity livVictim && livVictim.isAlive() && facePos.distanceTo(livVictim.position()) <= maxRange) {
				if (livVictim instanceof Player p) {
					if (p instanceof ServerPlayer sp) {
						if (sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.CREATIVE || sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SPECTATOR) {
							targetVictim = null;
						}
					}
				}
				if (targetVictim != null) {
					String victimId = BuiltInRegistries.ENTITY_TYPE.getKey(targetVictim.getType()).toString();
					boolean isColdEntity = victimId.contains("stray") || victimId.contains("snow_golem") || victimId.contains("snowman") || (targetVictim instanceof LivingEntity lv && lv.getTicksFrozen() > 0);
					float sonicDmg = 8.0F;
					float explosionDmg = 4.0F;
					if (isColdEntity) {
						sonicDmg = 18.0F;
						explosionDmg = 8.0F;
					}
					if (getRotPersistentBoolean(entity, K_STA2, false)) {
						sonicDmg *= 2.0F;
						explosionDmg *= 2.0F;
					}
					dealTrueDamageToBosses(targetVictim, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_inferno_laser"))), entity), (sonicDmg + explosionDmg) * (float) getAdaptationMultiplier(entity));
					targetVictim.setRemainingFireTicks(120);

					Vec3 push = targetVictim.position().subtract(entity.position()).normalize();
					targetVictim.setDeltaMovement(push.x * 0.425, 0.10625, push.z * 0.425);

					breakBlocksBehindTarget(level, targetVictim, push, getRotPersistentBoolean(entity, K_STA2, false));
				}
			}
		}

		if (fireTicks <= 1) {
			entity.getPersistentData().putDouble(K_SSFT, 0);
			entity.getPersistentData().putDouble(K_SLCT, LASER_CLOSING_TICKS);
			entity.getPersistentData().putDouble(K_SOLAR_CD, SOLAR_CD + RandomSource.create().nextInt(40));
			entity.getPersistentData().remove(K_SLTI);
			stopHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
		}
	}

	private static void executeSentinelCryoLaserCharging(LevelAccessor world, Entity entity, Entity target, int chargeTicks) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);

		Vec3 facePos = living.getEyePosition(1.0F).add(0.0, LASER_Y_OFFSET, 0.0);
		if (target != null) {
			updateRotLaserAim(entity, facePos, target, false);
		}

		double progress = (double) chargeTicks / 40.0;
		double radius = 1.9 - progress * 1.6;
		int particleCount = 2 + (int) (progress * 5.0);
		spawnLaserChargeRingParticles(level, entity, facePos, radius, particleCount, ParticleTypes.SNOWFLAKE);

		level.sendParticles(ParticleTypes.INSTANT_EFFECT, facePos.x, facePos.y, facePos.z, 1, 0.15, 0.15, 0.15, 0.01);

		if (chargeTicks % 6 == 0) {
			playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "block.powder_snow.break", 1.2F, 0.8F);
		}

		if (chargeTicks >= 40) {
			entity.getPersistentData().putDouble(K_SCCT, 0);
			entity.getPersistentData().putDouble(K_SCFT, 123);
			playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 4.5F, 1.25F);
		}
	}

	private static void executeSentinelCryoLaserFiring(LevelAccessor world, Entity entity, Entity target, int fireTicks) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.05, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.05);

		int targetId = (int) getRotPersistentDouble(entity, K_SLTI, 0.0);
		if (targetId == 0 && entity.getPersistentData().contains(K_SLTI)) {
			targetId = entity.getPersistentData().getInt(K_SLTI);
		}
		if (targetId != 0) {
			Entity storedTarget = level.getEntity(targetId);
			if (storedTarget != null && storedTarget.isAlive()) {
				target = storedTarget;
			}
		}

		if (target == null || !target.isAlive()) {
			entity.getPersistentData().putDouble(K_SCFT, 0);
			entity.getPersistentData().putDouble(K_SLCT, LASER_CLOSING_TICKS);
			entity.getPersistentData().putDouble(K_SOLAR_CD, SOLAR_CD + RandomSource.create().nextInt(40));
			entity.getPersistentData().remove(K_SLTI);
			entity.getPersistentData().remove(K_SLAX);
			entity.getPersistentData().remove(K_SLAY);
			entity.getPersistentData().remove(K_SLAZ);
			stopHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
			return;
		}

		Vec3 facePos = living.getEyePosition(1.0F).add(0.0, LASER_Y_OFFSET, 0.0);
		Vec3 direction = updateRotLaserAim(entity, facePos, target, true);

		double maxRange = 96.0;
		Vec3 beamEnd = facePos.add(direction.scale(maxRange));

		BlockHitResult blockHit = level.clip(new ClipContext(facePos, beamEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
		Vec3 firstIntersectionPos = beamEnd;
		boolean hitBlock = false;
		if (blockHit.getType() != HitResult.Type.MISS) {
			firstIntersectionPos = blockHit.getLocation();
			hitBlock = true;
		}

		AABB searchRange = new AABB(facePos, firstIntersectionPos).inflate(1.5);
		List<Entity> possibleEntities = level.getEntitiesOfClass(Entity.class, searchRange, e -> e != entity && e != living && e.isAlive());

		Entity hitEntity = null;
		double closestDist = facePos.distanceTo(firstIntersectionPos);
		Vec3 finalBeamEnd = firstIntersectionPos;

		for (Entity possible : possibleEntities) {
			if (possible instanceof Player p && p.isCreative()) continue;
			if (possible instanceof net.minecraft.world.entity.projectile.ThrownEnderpearl pearl && pearl.isAlive()) {
				level.sendParticles(ParticleTypes.SNOWFLAKE, pearl.getX(), pearl.getY(), pearl.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
				level.sendParticles(ParticleTypes.INSTANT_EFFECT, pearl.getX(), pearl.getY(), pearl.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
				playHostileSound(level, pearl.getX(), pearl.getY(), pearl.getZ(), "block.glass.break", 1.0F, 1.5F);
				pearl.discard();
				continue;
			}
			AABB possibleBb = possible.getBoundingBox().inflate(0.3);
			java.util.Optional<Vec3> clipResult = possibleBb.clip(facePos, firstIntersectionPos);
			if (clipResult.isPresent()) {
				double distToClip = facePos.distanceTo(clipResult.get());
				if (distToClip < closestDist) {
					closestDist = distToClip;
					finalBeamEnd = clipResult.get();
					hitEntity = possible;
					hitBlock = false;
				}
			}
		}

		beamEnd = finalBeamEnd;

		spawnFaceJitterBurst(level, living, facePos, ParticleTypes.SNOWFLAKE);
		if (living.getRandom().nextFloat() < 0.25F) {
			level.sendParticles(ParticleTypes.INSTANT_EFFECT, facePos.x, facePos.y, facePos.z, 2, 0.2, 0.2, 0.2, 0.1);
		}

		spawnBeamCylinderHelix(level, facePos, direction, beamEnd, ParticleTypes.SNOWFLAKE, ParticleTypes.INSTANT_EFFECT);

		if (hitBlock && blockHit.getType() == HitResult.Type.BLOCK) {
			BlockPos hitPos = blockHit.getBlockPos();
			BlockState hitState = level.getBlockState(hitPos);
			float hardness = hitState.getDestroySpeed(level, hitPos);
			if (hardness >= 0.0F && hardness <= 50.0F) {
				int lastDrillX = entity.getPersistentData().getInt(K_SLDX);
				int lastDrillY = entity.getPersistentData().getInt(K_SLDY);
				int lastDrillZ = entity.getPersistentData().getInt(K_SLDZ);
				double drillProgress = entity.getPersistentData().getDouble(K_SLDP);

				if (lastDrillX != hitPos.getX() || lastDrillY != hitPos.getY() || lastDrillZ != hitPos.getZ()) {
					drillProgress = 0.0;
					entity.getPersistentData().putInt(K_SLDX, hitPos.getX());
					entity.getPersistentData().putInt(K_SLDY, hitPos.getY());
					entity.getPersistentData().putInt(K_SLDZ, hitPos.getZ());
				}

				double progressAdd = 1.0 / Math.max(1.0, hardness * 2.5);
				if (getRotPersistentBoolean(entity, K_STA2, false)) {
					progressAdd *= 3.0;
				}
				drillProgress += progressAdd;
				entity.getPersistentData().putDouble(K_SLDP, drillProgress);

				if (level.getRandom().nextFloat() < 0.45F) {
					level.sendParticles(ParticleTypes.SNOWFLAKE, hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.05);
					level.sendParticles(ParticleTypes.INSTANT_EFFECT, hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
				}

				if (drillProgress >= 1.0) {
					level.destroyBlock(hitPos, false);
					playHostileSound(level, hitPos.getX(), hitPos.getY(), hitPos.getZ(), "block.glass.break", 1.0F, 1.2F);

					for (int dy = -1; dy <= 2; dy++) {
						for (int dx = -1; dx <= 1; dx++) {
							for (int dz = -1; dz <= 1; dz++) {
								if (dx*dx + dy*dy + dz*dz <= 2.5) {
									BlockPos adj = hitPos.offset(dx, dy, dz);
									BlockState adjState = level.getBlockState(adj);
									float adjHard = adjState.getDestroySpeed(level, adj);
									if (adjHard >= 0.0F && adjHard <= hardness + 1.5F && !adjState.isAir()) {
										level.destroyBlock(adj, false);
									}
								}
							}
						}
					}
					entity.getPersistentData().putDouble(K_SLDP, 0.0);
				}
			}

			BlockPos surfacePos = hitPos.relative(blockHit.getDirection());
			if (level.isEmptyBlock(surfacePos) && level.getBlockState(surfacePos.below()).isSolidRender(level, surfacePos.below())) {
				level.setBlock(surfacePos, net.minecraft.world.level.block.Blocks.SNOW.defaultBlockState(), 3);
			}
		}

		if (hitBlock && level.getRandom().nextFloat() < 0.3F) {
			BlockPos nearBeam = BlockPos.containing(
				beamEnd.x + (level.getRandom().nextDouble() - 0.5) * 4.0,
				beamEnd.y + (level.getRandom().nextDouble() - 0.5) * 4.0,
				beamEnd.z + (level.getRandom().nextDouble() - 0.5) * 4.0
			);
			if (level.isEmptyBlock(nearBeam) && level.getBlockState(nearBeam.below()).isSolidRender(level, nearBeam.below())) {
				level.setBlock(nearBeam, net.minecraft.world.level.block.Blocks.SNOW.defaultBlockState(), 3);
			}
		}

		if (fireTicks % 3 == 0) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, beamEnd.x, beamEnd.y, beamEnd.z, 3, 0.15, 0.15, 0.15, 0.02);
		}

		Entity targetVictim = (hitEntity != null) ? hitEntity : target;
		if (targetVictim instanceof net.minecraft.world.entity.projectile.ThrownEnderpearl pearl && pearl.isAlive()) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, pearl.getX(), pearl.getY(), pearl.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
			level.sendParticles(ParticleTypes.INSTANT_EFFECT, pearl.getX(), pearl.getY(), pearl.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
			playHostileSound(level, pearl.getX(), pearl.getY(), pearl.getZ(), "block.glass.break", 1.0F, 1.5F);
			pearl.discard();
		}

		if (entity.tickCount % 6 == 0) {
			if (targetVictim instanceof LivingEntity livVictim && livVictim.isAlive() && facePos.distanceTo(livVictim.position()) <= maxRange) {
				if (livVictim instanceof Player p) {
					if (p instanceof ServerPlayer sp) {
						if (sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.CREATIVE || sp.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SPECTATOR) {
							targetVictim = null;
						}
					}
				}
				if (targetVictim != null) {
					float dmg = 8.0F;
					String victimId = BuiltInRegistries.ENTITY_TYPE.getKey(targetVictim.getType()).toString();
					boolean isNetherEntity = (targetVictim.fireImmune() && !victimId.contains("warden")) || targetVictim.level().dimension() == net.minecraft.world.level.Level.NETHER || victimId.contains("piglin") || victimId.contains("wither_skeleton");
					if (isNetherEntity) {
						dmg = 18.0F;
					}
					if (getRotPersistentBoolean(entity, K_STA2, false)) {
						dmg *= 2.0F;
					}
					dealTrueDamageToBosses(targetVictim, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_cryo_laser"))), entity), dmg * (float) getAdaptationMultiplier(entity));
					((LivingEntity) targetVictim).setTicksFrozen(((LivingEntity) targetVictim).getTicksFrozen() + 180);
					((LivingEntity) targetVictim).addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 4, false, false));
					playHostileSound(level, targetVictim.getX(), targetVictim.getY(), targetVictim.getZ(), "entity.player.hurt_freeze", 1.2F, 0.8F);

					Vec3 push = targetVictim.position().subtract(entity.position()).normalize();
					targetVictim.setDeltaMovement(push.x * 0.17, 0.0425, push.z * 0.17);

					breakBlocksBehindTarget(level, targetVictim, push, getRotPersistentBoolean(entity, K_STA2, false));
				}
			}
		}

		if (fireTicks <= 1) {
			entity.getPersistentData().putDouble(K_SCFT, 0);
			entity.getPersistentData().putDouble(K_SLCT, LASER_CLOSING_TICKS);
			entity.getPersistentData().putDouble(K_SOLAR_CD, SOLAR_CD + RandomSource.create().nextInt(40));
			entity.getPersistentData().remove(K_SLTI);
			stopHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
		}
	}

	private static void executeSentinelSonicScream(LevelAccessor world, Entity entity, Entity target, int ticksLeft) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
		}
		if (entity.getDeltaMovement().y() > 0) {
			entity.setDeltaMovement(entity.getDeltaMovement().x(), 0.0, entity.getDeltaMovement().z());
		}

		if (target != null && target.isAlive()) {
			lockLookAtTarget(entity, target);
		}

		if (ticksLeft == 200) {
			playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:sonic_scream", 4.0F, 1.0F);
		}

		if (ticksLeft > 20 && ticksLeft <= 200) {
			List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(24.0), e -> e != entity && !isWoodboundEntity(e, entity));
			for (LivingEntity victim : targets) {
				victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 4, false, false));
				victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 9, false, false));

				if (victim instanceof Player player) {
					player.setYRot(player.getYRot() + (float)(entity.getRandom().nextDouble() - 0.5) * 15.0F);
					player.setXRot(player.getXRot() + (float)(entity.getRandom().nextDouble() - 0.5) * 10.0F);
					
					Vec3 motion = player.getDeltaMovement();
					player.setDeltaMovement(motion.x() + (entity.getRandom().nextDouble() - 0.5) * 0.1, motion.y(), motion.z() + (entity.getRandom().nextDouble() - 0.5) * 0.1);
					player.hurtMarked = true;

					if (player.getAbilities().flying || player.isFallFlying()) {
						player.getAbilities().flying = false;
						player.stopFallFlying();
						player.onUpdateAbilities();
						player.setDeltaMovement(player.getDeltaMovement().x(), -0.6, player.getDeltaMovement().z());
						player.hurtMarked = true;
					}
				} else {
					victim.setDeltaMovement(victim.getDeltaMovement().multiply(0.0, 1.0, 0.0).add((entity.getRandom().nextDouble() - 0.5) * 0.05, 0.0, (entity.getRandom().nextDouble() - 0.5) * 0.05));
					if (victim instanceof Mob mob) {
						mob.setTarget(null);
						mob.getNavigation().stop();
						mob.setYRot(mob.getYRot() + (float)(entity.getRandom().nextDouble() - 0.5) * 20.0F);
					}

					boolean targetIsFlying = victim.isFallFlying() || (!victim.onGround() && victim.getY() > entity.getY() + 2.0 && !victim.isInWater() && !victim.isInLava());
					if (targetIsFlying) {
						victim.setDeltaMovement(0.0, -0.6, 0.0);
						victim.hurtMarked = true;
					}
				}
			}
		}

		if (ticksLeft <= 1) {
			entity.getPersistentData().putDouble(K_SSST, 0);
			entity.getPersistentData().putDouble(K_SSSC, SONIC_SCREAM_COOLDOWN);
			entity.getPersistentData().putDouble(K_SGAC, getDynamicGlobalCooldown(entity));
		}
	}

	private static void checkLearnedMilestone(LivingEntity self, double combatTicks, boolean inCombat) {
		net.minecraft.nbt.CompoundTag data = self.getPersistentData();

		Entity target = (self instanceof Mob mob) ? mob.getTarget() : null;
		if (target != null) {
			String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().toLowerCase(java.util.Locale.ROOT);
			boolean isColdEntity = targetId.contains("stray") || targetId.contains("snow") || targetId.contains("ice") || targetId.contains("polar") || targetId.contains("frost") || targetId.contains("freeze");
			if (isColdEntity) {
				data.putBoolean("fought_cold_entity", true);
			}
			boolean isNetherEntity = targetId.contains("wither") || targetId.contains("ghast") || targetId.contains("piglin") || targetId.contains("blaze") || targetId.contains("magma") || targetId.contains("hoglin") || targetId.contains("strider") || targetId.contains("skeleton");
			if (isNetherEntity || target.level().dimension() == net.minecraft.world.level.Level.NETHER) {
				data.putBoolean("fought_nether_entity", true);
			}
			boolean isWardenEntity = targetId.contains("warden");
			if (isWardenEntity) {
				double wardenTicks = data.getDouble("sentinel_warden_combat_ticks") + 1.0;
				data.putDouble("sentinel_warden_combat_ticks", wardenTicks);
				if (wardenTicks >= WARDEN_LEARN_REQUIRED_TICKS) {
					data.putBoolean("fought_warden_entity", true);
				}
			}
		}

		double totalDamageTaken = data.getDouble("sentinel_total_damage_taken");

		if (totalDamageTaken >= 50.0 && !data.getBoolean(K_UR)) {
			data.putBoolean(K_UR, true);
			announceLearnedAbility(self);
		}

		if (ENABLE_TELEKINESIS && combatTicks >= 400 && !data.getBoolean("unlocked_telekinesis")) {
			data.putBoolean("unlocked_telekinesis", true);
			announceLearnedAbility(self);
		}
		double reqGrappleTicks = data.getDouble("sentinel_required_grapple_ticks");
		if (reqGrappleTicks <= 0) {
			reqGrappleTicks = 200.0 + self.getRandom().nextDouble() * 300.0;
			data.putDouble("sentinel_required_grapple_ticks", reqGrappleTicks);
		}
		if (ENABLE_EXTRACTION_GRAPPLE && combatTicks >= reqGrappleTicks && !data.getBoolean(K_UG)) {
			data.putBoolean(K_UG, true);
			announceLearnedAbility(self);
		}
		if (!data.getBoolean(K_USB2)) {
			boolean foughtWarden = data.getBoolean("fought_warden_entity");
			if (foughtWarden) {
				data.putBoolean(K_USB2, true);
				announceLearnedAbility(self);
			}
		}
		if (!data.getBoolean("unlocked_sonic_scream")) {
			double flyingTicks = data.getDouble(K_SFTT);
			if (flyingTicks >= 160.0) {
				data.putBoolean("unlocked_sonic_scream", true);
				announceLearnedAbility(self);
			}
		}
		if (!data.getBoolean(K_USB)) {
			boolean foughtNether = data.getBoolean("fought_nether_entity");
			boolean inNetherLong = data.getDouble(K_STIN) >= 600.0;
			boolean tookFireDmg = data.getBoolean(K_TFD);
			if (foughtNether || inNetherLong || tookFireDmg) {
				data.putBoolean(K_USB, true);
				data.putBoolean(K_UWE, true);
				announceLearnedAbility(self);
			}
		} else if (!data.getBoolean(K_UWE)) {
			data.putBoolean(K_UWE, true);
		}
		if (!data.getBoolean(K_UCB)) {
			boolean foughtCold = data.getBoolean("fought_cold_entity");
			boolean inColdLong = data.getDouble(K_STICB) >= 600.0;
			boolean tookFreezeDmg = data.getBoolean("taken_freeze_damage");
			if (foughtCold || inColdLong || tookFreezeDmg) {
				data.putBoolean(K_UCB, true);
				announceLearnedAbility(self);
			}
		}
		double prog = data.getDouble(K_STLP);
		if (prog >= 160.0 && !data.getBoolean(K_UT)) {
			data.putBoolean(K_UT, true);
			announceLearnedAbility(self);
		}

		double reqOverheadTicks = data.getDouble("sentinel_required_overhead_ticks");
		if (reqOverheadTicks <= 0) {
			reqOverheadTicks = 200.0 + self.getRandom().nextDouble() * 300.0;
			data.putDouble("sentinel_required_overhead_ticks", reqOverheadTicks);
		}
		if (combatTicks >= reqOverheadTicks && !data.getBoolean(K_UOC)) {
			data.putBoolean(K_UOC, true);
			announceLearnedAbility(self);
		}

		double reqDropkickTicks = data.getDouble("sentinel_required_dropkick_ticks");
		if (reqDropkickTicks <= 0) {
			reqDropkickTicks = 200.0 + self.getRandom().nextDouble() * 300.0;
			data.putDouble("sentinel_required_dropkick_ticks", reqDropkickTicks);
		}
		if (combatTicks >= reqDropkickTicks && !data.getBoolean(K_UDC)) {
			data.putBoolean(K_UDC, true);
			announceLearnedAbility(self);
		}

		double reqMinosTicks = data.getDouble("sentinel_required_minos_ticks");
		if (reqMinosTicks <= 0) {
			reqMinosTicks = 400.0 + self.getRandom().nextDouble() * 400.0;
			data.putDouble("sentinel_required_minos_ticks", reqMinosTicks);
		}
		if (combatTicks >= reqMinosTicks && !data.getBoolean("unlocked_minos_combo")) {
			data.putBoolean("unlocked_minos_combo", true);
			announceLearnedAbility(self);
		}

		double reqCombo1Ticks = data.getDouble("sentinel_required_cc1_ticks_learn");
		if (reqCombo1Ticks <= 0) {
			reqCombo1Ticks = 400.0 + self.getRandom().nextDouble() * 200.0;
			data.putDouble("sentinel_required_cc1_ticks_learn", reqCombo1Ticks);
		}
		if (combatTicks >= reqCombo1Ticks && !data.getBoolean(K_UTTC)) {
			data.putBoolean(K_UTTC, true);
			announceLearnedAbility(self);
		}

		double reqCombo2Ticks = data.getDouble("sentinel_required_cc2_ticks_learn");
		if (reqCombo2Ticks <= 0) {
			reqCombo2Ticks = 400.0 + self.getRandom().nextDouble() * 200.0;
			data.putDouble("sentinel_required_cc2_ticks_learn", reqCombo2Ticks);
		}
		if (combatTicks >= reqCombo2Ticks && !data.getBoolean(K_UHSSC)) {
			data.putBoolean(K_UHSSC, true);
			announceLearnedAbility(self);
		}

		double reqCombo3Ticks = data.getDouble("sentinel_required_cc3_ticks_learn");
		if (reqCombo3Ticks <= 0) {
			reqCombo3Ticks = 400.0 + self.getRandom().nextDouble() * 200.0;
			data.putDouble("sentinel_required_cc3_ticks_learn", reqCombo3Ticks);
		}
		if (combatTicks >= reqCombo3Ticks && !data.getBoolean("unlocked_knockback_dropkick_combo")) {
			data.putBoolean("unlocked_knockback_dropkick_combo", true);
			announceLearnedAbility(self);
		}

		double reqCombo4Ticks = data.getDouble("sentinel_required_cc4_ticks_learn");
		if (reqCombo4Ticks <= 0) {
			reqCombo4Ticks = 600.0 + self.getRandom().nextDouble() * 300.0;
			data.putDouble("sentinel_required_cc4_ticks_learn", reqCombo4Ticks);
		}
		if (combatTicks >= reqCombo4Ticks && !data.getBoolean(K_UKRC)) {
			data.putBoolean(K_UKRC, true);
			announceLearnedAbility(self);
		}

		double reqCombo5Ticks = data.getDouble("sentinel_required_cc5_ticks_learn");
		if (reqCombo5Ticks <= 0) {
			reqCombo5Ticks = 650.0 + self.getRandom().nextDouble() * 250.0;
			data.putDouble("sentinel_required_cc5_ticks_learn", reqCombo5Ticks);
		}
		if (combatTicks >= reqCombo5Ticks && !data.getBoolean("unlocked_heavenly_repentance_plus")) {
			data.putBoolean("unlocked_heavenly_repentance_plus", true);
			announceLearnedAbility(self);
		}
	}

	private static void announceLearnedAbility(Entity self) {
		if (self == null) return;
		playHostileSound(self.level(), self.getX(), self.getY(), self.getZ(), "entity.warden.heartbeat", 1.2F, 1.4F);
	}

	private static void executeGrappleSiphon(LevelAccessor world, Entity self, Entity target, int ticksLeft) {
		if (!(world instanceof ServerLevel level) || !(self instanceof LivingEntity rawSelf)) return;

		Vec3 vectorToSelf = self.position().add(0, 1.0, 0).subtract(target.position()).normalize();
		double distance = target.position().distanceTo(self.position());

		if (distance > 2.0) {
			target.setDeltaMovement(vectorToSelf.x * 0.82, 0.15, vectorToSelf.z * 0.82);
			level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0.2, 0.2, 0.2, 0.1);
		} else {
			target.setDeltaMovement(0, -0.05, 0);
			if (target instanceof LivingEntity liv) {
				liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4, false, false));
				liv.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 1, false, false));
			}

			if (ticksLeft % 4 == 0) {
				dealTrueDamageToBosses(target, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_extraction_grapple"))), self), 4.0F * (float) getAdaptationMultiplier(self));
				rawSelf.setHealth(Math.min(rawSelf.getMaxHealth(), rawSelf.getHealth() + 4.5F));

				level.sendParticles(ParticleTypes.SWEEP_ATTACK, self.getX(), self.getY() + 1.2, self.getZ(), 1, 0.3, 0.3, 0.3, 0.0);
				level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0.1, 0.1, 0.1, 0.1);

				playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.warden.attack_impact", 0.7F, 0.85F);
				if (self instanceof LivingEntity ls) ls.swing(InteractionHand.MAIN_HAND, true);
			}
		}
	}

	private static void executeTelekinesis(LevelAccessor world, Entity self, Entity target, int ticksLeft) {
		if (!(world instanceof ServerLevel level) || target == null) return;

		Vec3 hoverPos = self.position().add(0, 3.5, 0);
		Vec3 diff = hoverPos.subtract(target.position());
		double liftStrength = 0.22;
		target.setDeltaMovement(diff.x * liftStrength, 0.15, diff.z * liftStrength);
		target.hasImpulse = true;

		if (target instanceof LivingEntity liv) {
			liv.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 3, false, false));
		}

		if (ticksLeft <= 0) {
			if (Math.random() < 0.5) {
				target.setDeltaMovement(Vec3.ZERO);
				target.hasImpulse = true;
				target.setDeltaMovement(0, -2.4, 0);
				target.hasImpulse = true;
				dealTrueDamageToBosses(target, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_telekinesis"))), self), 12.0F * (float) getAdaptationMultiplier(self));
				playHostileSound(level, target.getX(), target.getY(), target.getZ(), "entity.iron_golem.damage", 1.0F, 0.65F);
			} else {
				Vec3 throwDir = target.position().subtract(self.position()).normalize();
				double ty = Math.max(throwDir.y, 0.2);
				target.setDeltaMovement(throwDir.x * 2.0, ty * 1.5 + 0.3, throwDir.z * 2.0);
				dealTrueDamageToBosses(target, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_telekinesis"))), self), 8.0F * (float) getAdaptationMultiplier(self));
				playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.player.attack.sweep", 1.0F, 0.6F);
			}
		}
	}

	private static void executeSentinelPunch(LevelAccessor world, Entity self, Entity target) {
		if (!(world instanceof ServerLevel level)) return;
		LivingEntity ls = (self instanceof LivingEntity) ? (LivingEntity) self : null;

		if (target instanceof LivingEntity targetLiving) {
			String targetTypeName = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().toLowerCase();
			if (targetTypeName.contains("wroughtnaut") || targetTypeName.contains("ferrous_wroughtnaut")) {
				if (!isWroughtnautStuck(targetLiving)) {
					return;
				}
			}
		}

		if (ls != null) {
			boolean lastHandLeft = getRotPersistentBoolean(self, K_SPHT, false);
			ls.swing(InteractionHand.MAIN_HAND, true);
			self.getPersistentData().putBoolean(K_SPHT, !lastHandLeft);

			if (lastHandLeft) {
				self.getPersistentData().putDouble(K_SLPT, 18);
				self.getPersistentData().putDouble(K_SRPT2, 0);
			} else {
				self.getPersistentData().putDouble(K_SRPT2, 18);
				self.getPersistentData().putDouble(K_SLPT, 0);
			}
		}

		double PUNCH_LEAD_TICKS = 8.0;
		Vec3 targetVel = target.getDeltaMovement();
		double predictedX = target.getX() + targetVel.x * PUNCH_LEAD_TICKS;
		double predictedZ = target.getZ() + targetVel.z * PUNCH_LEAD_TICKS;
		double pDx = self.getX() - predictedX;
		double pDy = self.getY() - target.getY();
		double pDz = self.getZ() - predictedZ;
		double predictedDist = Math.sqrt(pDx * pDx + pDy * pDy + pDz * pDz);
		double liveDist = self.distanceTo(target);

		double finalDist = Math.min(liveDist, predictedDist);
		double maxReach = (target instanceof Player) ? 2.5 : 3.2;
		if (finalDist > maxReach) {
			return;
		}

		double combatTicks = self.getPersistentData().getDouble(K_SCT);
		boolean inContinuousMelee = (combatTicks > 30.0 && finalDist <= 3.0);
		boolean isMoving = (target.getDeltaMovement().horizontalDistance() > 0.08);

		if (inContinuousMelee && isMoving && Math.random() < 0.25) {
			playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.player.attack.sweep", 1.2F, 1.4F);
			level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1.0, target.getZ(), 3, 0.2, 0.2, 0.2, 0.02);
			return;
		}

		double targetCurrentHp = (target instanceof LivingEntity tLiv) ? tLiv.getHealth() : 999.0;
		double rotPunchDmg = MELEE_PUNCH_DAMAGE * getAdaptationMultiplier(self);
		boolean isLowHpTarget = !(target instanceof Player) && (targetCurrentHp <= rotPunchDmg || targetCurrentHp <= 20.0);

		boolean totemActive = getRotPersistentBoolean(self, K_STA2, false);
		double uppercutChance = totemActive ? 0.06 : 0.03;
		if (!isLowHpTarget && getRotPersistentBoolean(self, K_UHSSC, false) && getRotPersistentDouble(self, K_SUC, 0.0) <= 0 && Math.random() < uppercutChance) {
			self.getPersistentData().putBoolean(K_IU, true);
			boolean isLeftUppercut = Math.random() < 0.5;
			self.getPersistentData().putBoolean(K_IUL, isLeftUppercut);
			self.getPersistentData().putBoolean(K_IUR, !isLeftUppercut);
			self.getPersistentData().putBoolean(K_IUS, true);
			setRotPersistentDouble(self, K_SUC, 120.0);
			self.getPersistentData().putDouble(K_SCS, 1);
			self.getPersistentData().putDouble(K_SCT2, 0);
			playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.iron_golem.attack", 1.5F, 0.7F);
			return;
		}

		double basePunchDmg = MELEE_PUNCH_DAMAGE;

		boolean unlockedSonicBoom = getRotPersistentBoolean(self, K_USB2, false);
		boolean unlockedSolar = getRotPersistentBoolean(self, K_USB, false);
		boolean unlockedCryo = getRotPersistentBoolean(self, K_UCB, false);
		boolean unlockedGrapple = ENABLE_EXTRACTION_GRAPPLE && getRotPersistentBoolean(self, K_UG, false);
		boolean unlockedTK = ENABLE_TELEKINESIS && getRotPersistentBoolean(self, "unlocked_telekinesis", false);
		boolean unlockedTP = getRotPersistentBoolean(self, K_UT, false);
		boolean unlockedOverhead = getRotPersistentBoolean(self, K_UOC, false);
		boolean unlockedDropkick = getRotPersistentBoolean(self, K_UDC, false);
		boolean unlockedMinos = getRotPersistentBoolean(self, "unlocked_minos_combo", false);

		int selectCombo = 0;
		double globalCd = self.getPersistentData().getDouble(K_SGAC);
		if (globalCd <= 0 && target instanceof LivingEntity && Math.random() < (getRotPersistentBoolean(self, K_STA2, false) ? 0.85 : 0.28)) {
			java.util.List<Integer> availableCombos = new java.util.ArrayList<>();
			if (unlockedSonicBoom || unlockedSolar || unlockedCryo) availableCombos.add(1);
			if (unlockedGrapple) availableCombos.add(2);
			if (unlockedTK) availableCombos.add(3);
			if (unlockedTP) availableCombos.add(4);
			if (!isLowHpTarget && unlockedTK) availableCombos.add(5);
			if (unlockedSolar || unlockedCryo) availableCombos.add(6);
			if (unlockedTP) availableCombos.add(7);
			if (!isLowHpTarget && unlockedTP && unlockedOverhead) availableCombos.add(8);
			if (unlockedTP) availableCombos.add(9);
			if (unlockedTP && unlockedDropkick && self.distanceTo(target) >= 6.0) availableCombos.add(10);
			if (unlockedTP) availableCombos.add(11);
			if (unlockedTK) availableCombos.add(12);
			if (unlockedDropkick && self.distanceTo(target) >= 6.0) availableCombos.add(13);
			if (!isLowHpTarget && unlockedMinos) availableCombos.add(14);

			if (!isLowHpTarget && getRotPersistentBoolean(self, K_UTTC, false) && self.getPersistentData().getDouble(K_SCC) <= 0) availableCombos.add(101);
			if (!isLowHpTarget && getRotPersistentBoolean(self, K_UHSSC, false) && self.getPersistentData().getDouble(K_SCC2) <= 0) availableCombos.add(102);
			if (!isLowHpTarget && getRotPersistentBoolean(self, "unlocked_knockback_dropkick_combo", false) && self.getPersistentData().getDouble(K_SCC3) <= 0 && self.distanceTo(target) >= 5.5) availableCombos.add(103);
			if (!isLowHpTarget && getRotPersistentBoolean(self, K_UKRC, false) && self.getPersistentData().getDouble(K_SCC4) <= 0 && self.distanceTo(target) >= 5.5) availableCombos.add(104);
			if (!isLowHpTarget && getRotPersistentBoolean(self, "unlocked_heavenly_repentance_plus", false) && self.getPersistentData().getDouble(K_SCC5) <= 0) availableCombos.add(105);

			if (!availableCombos.isEmpty()) {
				CombatContext ctx = getCombatContext(self, target);
				boolean favorHeavy = getRotPersistentBoolean(target, "bw_threat_high_armor", false);
				if (favorHeavy && Math.random() < 0.6) {
					java.util.List<Integer> heavyCombos = new java.util.ArrayList<>();
					for (int c : availableCombos) {
						if (c == 5 || c == 8 || c == 12 || c == 13 || c == 14 || c == 102) heavyCombos.add(c);
					}
					if (!heavyCombos.isEmpty()) {
						selectCombo = evaluateComboUtility(self, target, ctx, heavyCombos);
					} else {
						selectCombo = evaluateComboUtility(self, target, ctx, availableCombos);
					}
				} else {
					selectCombo = evaluateComboUtility(self, target, ctx, availableCombos);
				}
				double coreCd = getDynamicGlobalCooldown(self);
				self.getPersistentData().putDouble(K_SGAC, coreCd);

				
				if (selectCombo == 101) {
					self.getPersistentData().putDouble(K_SCS2, 1);
					self.getPersistentData().putDouble(K_SCT3, 20);
					self.getPersistentData().putDouble(K_SCAT, 120);
				} else if (selectCombo == 102) {
					self.getPersistentData().putDouble(K_SCS, 1);
					self.getPersistentData().putDouble(K_SCAT, 180);
				} else if (selectCombo == 103) {
					self.getPersistentData().putDouble(K_SCS3, 1);
					self.getPersistentData().putDouble(K_SCAT, 120);
				} else if (selectCombo == 104) {
					self.getPersistentData().putDouble(K_SCS4, 1);
					self.getPersistentData().putDouble(K_SCAT, 120);
				} else if (selectCombo == 105) {
					self.getPersistentData().putDouble(K_SCS5, 1);
					self.getPersistentData().putDouble(K_SCAT, 180);
				} else {
					self.getPersistentData().putDouble(K_SCAT, 40);
				}

				if (selectCombo >= 101) {
					return;
				}
			}
		}

		boolean targetHasArmor = false;
		boolean targetHasMace = false;
		double targetDmg = 2.0;

		if (target instanceof LivingEntity targetLiving) {
			if (targetLiving.getArmorValue() > 0) {
				targetHasArmor = true;
			}
			var mainHand = targetLiving.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
			var offHand = targetLiving.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND);
			String mainHandName = BuiltInRegistries.ITEM.getKey(mainHand.getItem()).toString();
			String offHandName = BuiltInRegistries.ITEM.getKey(offHand.getItem()).toString();
			if (mainHandName.contains("mace") || offHandName.contains("mace")) {
				targetHasMace = true;
			}

			var attr = targetLiving.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
			if (attr != null) {
				targetDmg = attr.getValue();
			}
		}

		double adaptedPunchDmg = self.getPersistentData().getDouble(K_APD);
		if (adaptedPunchDmg < basePunchDmg) {
			adaptedPunchDmg = basePunchDmg;
		}

		if (targetDmg > adaptedPunchDmg) {
			adaptedPunchDmg += (targetDmg - adaptedPunchDmg) * 0.45;
			self.getPersistentData().putDouble(K_APD, adaptedPunchDmg);
			level.sendParticles(ParticleTypes.ENCHANT, self.getX(), self.getY() + 1.2, self.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
		}

		double finalDamage = adaptedPunchDmg;

		if (targetHasMace && target instanceof LivingEntity targetLiving) {
			for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
				if (slot.isArmor()) {
					var stack = targetLiving.getItemBySlot(slot);
					if (!stack.isEmpty()) {
						stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + 25));
					}
				}
			}
			finalDamage += targetLiving.getArmorValue() * 0.65;
		}

		if (selectCombo != 4 && selectCombo != 5 && selectCombo != 6 && selectCombo != 7 && selectCombo != 9) {
			if (ls != null) {
				dealTrueDamageToBosses(target, ls.damageSources().mobAttack(ls), (float) finalDamage * (float) getAdaptationMultiplier(ls));
			} else {
				dealTrueDamageToBosses(target, new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) finalDamage * (float) getAdaptationMultiplier(self));
			}
		}

		Vec3 sweepPush = target.position().subtract(self.position()).normalize();
		double knockbackStrength = 1.05;

		if (adaptedPunchDmg > basePunchDmg) {
			knockbackStrength += (adaptedPunchDmg - basePunchDmg) * 0.045;
		}

		if (selectCombo == 1 && target instanceof LivingEntity liv) {
			liv.setDeltaMovement(sweepPush.x * 0.3, 1.35, sweepPush.z * 0.3);
			liv.hasImpulse = true;

			self.getPersistentData().putBoolean(K_IU, true);
			boolean isLeftUppercut = Math.random() < 0.5;
			self.getPersistentData().putBoolean(K_IUL, isLeftUppercut);
			self.getPersistentData().putBoolean(K_IUR, !isLeftUppercut);
			self.getPersistentData().putDouble(K_SUAT, 20.0);
			self.getPersistentData().putDouble(K_SLPT, 0);
			self.getPersistentData().putDouble(K_SRPT2, 0);

			level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.5, target.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
			playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.wind_charge.throw", 1.2F, 0.75F);

			double rng = Math.random();
			if (rng < 0.45 && unlockedSonicBoom && self.onGround() && self.distanceTo(target) <= SONIC_BOOM_RANGE) {
				self.getPersistentData().putDouble(K_SST, SONIC_BOOM_ANIMATION_TICKS);
				self.getPersistentData().putDouble(K_SWSC, SONIC_BOOM_COOLDOWN);
				playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.warden.sonic_charge", 1.4F, 0.4F);
			} else if (rng < 0.25 && (unlockedSolar || unlockedCryo) && getRotPersistentDouble(self, K_SOLAR_CD, 0.0) <= 0 && getRotPersistentDouble(self, K_SMS, 0.0) == 0 && getRotPersistentDouble(self, K_SMT, 0.0) == 0) {
				java.util.List<String> lasers = new java.util.ArrayList<>();
				if (unlockedSolar) lasers.add("solar");
				if (unlockedCryo) lasers.add("cryo");
				if (!lasers.isEmpty()) {
					String chosen = lasers.get((int) (Math.random() * lasers.size()));
					if ("solar".equals(chosen)) {
						self.getPersistentData().putDouble(K_SSCT, 1);
					} else {
						self.getPersistentData().putDouble(K_SCCT, 1);
					}
					self.getPersistentData().putDouble(K_SOLAR_CD, SOLAR_CD);
				}
			}
		} else if (selectCombo == 2 && target instanceof LivingEntity liv) {
			self.getPersistentData().putDouble(K_GRAPPLE_CD, 20);
			self.getPersistentData().putDouble(K_GRAPPLE_TICKS, 32);
			Vec3 vectorToSelf = self.position().subtract(target.position()).normalize();
			liv.setDeltaMovement(vectorToSelf.x * 1.25, 0.22, vectorToSelf.z * 1.25);
			liv.hasImpulse = true;
			playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.warden.attack_impact", 1.2F, 0.85F);
			level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
		} else if (selectCombo == 3 && target instanceof LivingEntity liv) {
			self.getPersistentData().putDouble(K_TK_CD, 20);
			self.getPersistentData().putDouble(K_TK_TICKS, 25);
			liv.setDeltaMovement(0.0, 0.85, 0.0);
			liv.hasImpulse = true;
			playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.warden.sonic_boom", 0.9F, 1.4F);
			level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1.2, target.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
		} else if (selectCombo == 4 && target instanceof LivingEntity liv) {
			double angle = target.getYRot() * (Math.PI / 180.0);
			double spawnX = target.getX() - Math.sin(angle) * 1.5;
			double spawnZ = target.getZ() + Math.cos(angle) * 1.5;
			double spawnY = target.getY();

			level.sendParticles(ParticleTypes.PORTAL, self.getX(), self.getY() + 1.1, self.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
			teleportEntity(self, spawnX, spawnY, spawnZ);
			level.sendParticles(ParticleTypes.PORTAL, spawnX, spawnY + 1.1, spawnZ, 8, 0.2, 0.2, 0.2, 0.05);
			playHostileSound(level, spawnX, spawnY, spawnZ, "item.chorus_fruit.teleport", 1.1F, 0.95F);

			dealTrueDamageToBosses(liv, ls != null ? ls.damageSources().mobAttack(ls) : new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) (finalDamage * 1.4) * (float) getAdaptationMultiplier(ls));
			liv.setDeltaMovement(liv.getDeltaMovement().add(self.getViewVector(1.0F).scale(1.8)));
			liv.hasImpulse = true;
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "entity.player.attack.sweep", 1.2F, 0.65F);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, self.getX(), self.getY() + 1.2, self.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
		} else if (selectCombo == 5 && target instanceof LivingEntity liv) {
			liv.setDeltaMovement(0.0, 0.45, 0.0);
			liv.hasImpulse = true;
			liv.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 4, false, false));
			dealTrueDamageToBosses(liv, ls != null ? ls.damageSources().mobAttack(ls) : new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) (finalDamage * 1.5) * (float) getAdaptationMultiplier(ls));
			liv.setDeltaMovement(sweepPush.x * 0.2, -1.8, sweepPush.z * 0.2);
			liv.hasImpulse = true;

			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "entity.iron_golem.damage", 1.4F, 0.5F);
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "entity.iron_golem.death", 0.9F, 0.6F);
			level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(), target.getZ(), 5, 0.5, 0.1, 0.5, 0.1);
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(), target.getZ(), 10, 0.4, 0.4, 0.4, 0.2);
		} else if (selectCombo == 6 && target instanceof LivingEntity liv) {
			liv.setRemainingFireTicks(80);
			liv.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false));
			dealTrueDamageToBosses(liv, ls != null ? ls.damageSources().mobAttack(ls) : new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) (finalDamage * 1.35) * (float) getAdaptationMultiplier(ls));

			level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
			level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "item.firecharge.use", 1.2F, 0.8F);
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "block.powder_snow.break", 1.2F, 1.1F);
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "entity.generic.explode", 0.8F, 1.4F);
		} else if (selectCombo == 7 && target instanceof LivingEntity liv) {
			for (int i = 0; i < 3; i++) {
				double oX = (Math.random() - 0.5) * 3.0;
				double oZ = (Math.random() - 0.5) * 3.0;
				level.sendParticles(ParticleTypes.PORTAL, target.getX() + oX, target.getY() + 1.0, target.getZ() + oZ, 4, 0.1, 0.1, 0.1, 0.05);
			}
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "item.chorus_fruit.teleport", 1.3F, 1.2F);
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "entity.player.attack.sweep", 1.4F, 1.5F);

			dealTrueDamageToBosses(liv, ls != null ? ls.damageSources().mobAttack(ls) : new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) (finalDamage * 1.6) * (float) getAdaptationMultiplier(ls));
			liv.setDeltaMovement(sweepPush.x * 1.6, 0.45, sweepPush.z * 1.6);
			liv.hasImpulse = true;

			level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 2, 0.3, 0.3, 0.3, 0.0);
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.3, 0.3, 0.3, 0.08);
		} else if (selectCombo == 8 && target instanceof LivingEntity liv) {
			self.getPersistentData().putDouble(K_ROT, OVERHEAD_TOTAL_TICKS);
			self.getPersistentData().putString(K_OTU, target.getUUID().toString());
		} else if (selectCombo == 9 && target instanceof LivingEntity liv) {
			for (int hit = 1; hit <= 3; hit++) {
				double targetYRot = target.getYRot() * (Math.PI / 180.0);
				double tx = target.getX() - Math.sin(targetYRot) * 1.6;
				double tz = target.getZ() + Math.cos(targetYRot) * 1.6;
				double ty = target.getY();
				level.sendParticles(ParticleTypes.PORTAL, self.getX(), self.getY() + 0.5, self.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
				teleportEntity(self, tx, ty, tz);
				level.sendParticles(ParticleTypes.PORTAL, tx, ty + 0.5, tz, 6, 0.2, 0.2, 0.2, 0.05);
				playHostileSound(level, tx, ty, tz, "item.chorus_fruit.teleport", 1.1F, 1.0F + hit * 0.1F);
				playHostileSound(level, tx, ty, tz, "entity.player.attack.knockback", 1.3F, 0.7F + hit * 0.1F);
				dealTrueDamageToBosses(target, ls != null ? ls.damageSources().mobAttack(ls) : new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) (finalDamage * 1.2) * (float) getAdaptationMultiplier(ls));
				Vec3 pushVec = target.position().subtract(self.position()).normalize();
				target.setDeltaMovement(pushVec.x * 1.9, 0.35, pushVec.z * 1.9);
				target.hasImpulse = true;
			}
		} else if (selectCombo == 10 && target instanceof LivingEntity liv) {
			self.setDeltaMovement(0.0, 0.0, 0.0);
			self.hasImpulse = true;
			playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.warden.sonic_charge", 1.5F, 0.5F);
			self.getPersistentData().putDouble(K_SJT, 60);
		} else if (selectCombo == 11 && target instanceof LivingEntity liv) {
			double startX = self.getX();
			double startY = self.getY();
			double startZ = self.getZ();

			double targetYRot = liv.getYRot() * (Math.PI / 180.0);
			double tx = liv.getX() - Math.sin(targetYRot) * 1.2;
			double tz = liv.getZ() + Math.cos(targetYRot) * 1.2;
			double ty = liv.getY();

			BlockPos selfPos = self.blockPosition();
			for (int h = 1; h <= 10; h++) {
				BlockPos above = selfPos.above(h);
				BlockState aboveState = level.getBlockState(above);
				float hardness = aboveState.getDestroySpeed(level, above);
				if (!aboveState.isAir() && hardness >= 0 && hardness < 40f) {
					level.destroyBlock(above, false);
				}
			}

			level.sendParticles(ParticleTypes.EXPLOSION, self.getX(), self.getY() + 0.5, self.getZ(), 8, 1.0, 0.2, 1.0, 0.1);
			playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.generic.explode", 1.2F, 1.2F);
			playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.warden.sonic_boom", 0.8F, 1.4F);

			Vec3 jumpVec = target.position().subtract(self.position());
			double dx = jumpVec.x;
			double dz = jumpVec.z;
			double distXZ = Math.sqrt(dx * dx + dz * dz);
			if (distXZ < 0.1) distXZ = 0.1;
			double forwardMultiplier = 2.4;
			double upwardVelocity = 2.0;
			self.setDeltaMovement(dx / distXZ * forwardMultiplier, upwardVelocity, dz / distXZ * forwardMultiplier);
			self.hasImpulse = true;

			Vec3 start = new Vec3(startX, startY + self.getBbHeight() * 0.5, startZ);
			Vec3 end = new Vec3(tx, ty + liv.getBbHeight() * 0.5, tz);
			double distance = start.distanceTo(end);

			int steps = (int) Math.ceil(distance * 1.5);
			for (int i = 0; i <= steps; i++) {
				double pct = (double) i / steps;
				double px = startX + (tx - startX) * pct;
				double py = startY + (ty - startY) * pct;
				double pz = startZ + (tz - startZ) * pct;

				BlockPos centerPos = BlockPos.containing(px, py + 1.0, pz);
				for (BlockPos bp : BlockPos.betweenClosed(centerPos.offset(-1, -1, -1), centerPos.offset(1, 2, 1))) {
					BlockState state = level.getBlockState(bp);
					float hardness = state.getDestroySpeed(level, bp);
					if (!state.isAir() && hardness >= 0 && hardness < 40f) {
						level.destroyBlock(bp, false);
					}
				}

				if (i % 2 == 0) {
					level.sendParticles(ParticleTypes.EXPLOSION, px, py + 1.0, pz, 1, 0.2, 0.2, 0.2, 0.0);
				}
				level.sendParticles(ParticleTypes.CRIT, px, py + 1.0, pz, 3, 0.1, 0.1, 0.1, 0.1);
			}

			playHostileSound(level, tx, ty, tz, "entity.generic.explode", 1.4F, 1.1F);
			playHostileSound(level, tx, ty, tz, "entity.warden.attack_impact", 1.6F, 0.6F);

			dealTrueDamageToBosses(liv, ls != null ? ls.damageSources().mobAttack(ls) : new DamageSource(level.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) (finalDamage * 2.2) * (float) getAdaptationMultiplier(ls));
			liv.setDeltaMovement(sweepPush.x * 3.5, 0.95, sweepPush.z * 3.5);
			liv.hasImpulse = true;
		} else if (selectCombo == 12 && target instanceof LivingEntity liv) {
			liv.setDeltaMovement(0.0, 1.45, 0.0);
			liv.hasImpulse = true;
			playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.iron_golem.attack", 1.5F, 0.8F);
			level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.5, target.getZ(), 10, 0.2, 0.2, 0.2, 0.05);

			self.setDeltaMovement(0.0, 1.9, 0.0);
			self.hasImpulse = true;

			self.getPersistentData().putDouble(K_SSP, 1);
			self.getPersistentData().putDouble(K_SST2, 22);
		} else if (selectCombo == 13 && target instanceof LivingEntity liv) {
			self.setDeltaMovement(0.0, 1.95, 0.0);
			self.hasImpulse = true;

			self.getPersistentData().putDouble(K_SDKP, 1);
			self.getPersistentData().putDouble(K_SDKT, 22);
			playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.iron_golem.attack", 1.5F, 0.8F);
		} else if (selectCombo == 14 && target instanceof LivingEntity liv) {
			if (getRotPersistentDouble(self, K_SSFT, 0.0) > 0 || getRotPersistentDouble(self, K_SSCT, 0.0) > 0
				|| getRotPersistentDouble(self, K_SCFT, 0.0) > 0 || getRotPersistentDouble(self, K_SCCT, 0.0) > 0) {
				self.getPersistentData().putDouble(K_SSFT, 0);
				self.getPersistentData().putDouble(K_SSCT, 0);
				self.getPersistentData().putDouble(K_SCFT, 0);
				self.getPersistentData().putDouble(K_SCCT, 0);
				self.getPersistentData().putDouble(K_SLCT, LASER_CLOSING_TICKS);
				stopHostileSound(level, self.getX(), self.getY(), self.getZ(), "the_backwoods:fractus_laser", 256.0);
			}
			self.getPersistentData().putDouble(K_SMT, 50);
			self.getPersistentData().putDouble(K_SMS, 1);
		} else {
		}

		if (selectCombo != 1 && target instanceof LivingEntity liv && !liv.isOnFire() && Math.random() < 0.4) {
			liv.setRemainingFireTicks(100);
			playHostileSound(level, target.getX(), target.getY(), target.getZ(), "item.firecharge.use", 1.1F, 0.9F);
			level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
		} else {
			if (targetHasArmor) {
				playHostileSound(level, target.getX(), target.getY(), target.getZ(), "item.mace.heavy_smash", 1.2F, 0.65F);
			} else if (adaptedPunchDmg > basePunchDmg) {
				playHostileSound(level, target.getX(), target.getY(), target.getZ(), "item.mace.knockback", 1.1F, 0.95F);
			} else {
				playHostileSound(level, target.getX(), target.getY(), target.getZ(), "entity.iron_golem.damage", 1.0F, 0.85F);
			}
		}

		level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.2, target.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.0, target.getZ(), 3, 0.1, 0.1, 0.1, 0.02);
	}

	private static boolean isWither(Entity entity) {
		if (entity == null) return false;
		String key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
		return "minecraft:wither".equals(key);
	}

	private static void sendActionBarToNearbyPlayers(LevelAccessor world, Vec3 pos, double range, String text) {
		if (world instanceof ServerLevel s) {
			s.players().stream()
				.filter(p -> p.position().distanceToSqr(pos) <= range * range)
				.forEach(p -> p.displayClientMessage(Component.literal(text), true));
		}
	}

	private static boolean isTargetPillaring(LevelAccessor level, Entity target, Entity self) {
		if (target == null || self == null) return false;
		if (target.getY() <= self.getY() + 2.2) return false;
		net.minecraft.core.BlockPos targetPos = target.blockPosition();
		int targetY = targetPos.getY() - 1;
		int selfY = self.blockPosition().getY();
		int startY = targetY;
		int endY = Math.max(selfY + 1, targetY - 5);
		if (startY < endY) return false;
		int totalSolidCount = 0;
		int layersChecked = 0;
		for (int y = startY; y >= endY; y--) {
			int layerSolidCount = 0;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					net.minecraft.core.BlockPos p = targetPos.offset(dx, y - targetPos.getY(), dz);
					if (level.getBlockState(p).isCollisionShapeFullBlock(level, p)) {
						layerSolidCount++;
					}
				}
			}
			totalSolidCount += layerSolidCount;
			layersChecked++;
		}
		if (layersChecked == 0) return false;
		double avgSolid = (double) totalSolidCount / layersChecked;
		return avgSolid < 4.5;
	}

	private static void snapLookAtTarget(Entity entity, Entity target) {
		if (entity == null || target == null) return;
		double dx = target.getX() - entity.getX();
		double dy = (target.getY() + target.getBbHeight() * 0.5) - (entity.getY() + entity.getEyeHeight());
		double dz = target.getZ() - entity.getZ();
		double dh = Math.sqrt(dx * dx + dz * dz);
		if (dh > 0.001) {
			float targetYRot = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
			float targetXRot = (float) (-(Mth.atan2(dy, Math.max(0.001, dh)) * (180F / Math.PI)));
			float currentY = entity.getYRot();
			float currentX = entity.getXRot();
			float maxTurn = 35.0F;
			float yawDelta = Mth.wrapDegrees(targetYRot - currentY);
			float pitchDelta = Mth.wrapDegrees(targetXRot - currentX);
			float newYRot = currentY + Mth.clamp(yawDelta, -maxTurn, maxTurn);
			float newXRot = currentX + Mth.clamp(pitchDelta, -maxTurn, maxTurn);
			entity.setYRot(newYRot);
			entity.setXRot(newXRot);
			if (entity instanceof Mob mob) {
				mob.yBodyRot = newYRot;
				mob.yHeadRot = newYRot;
				mob.setYRot(newYRot);
				double dieKickPhase = getRotPersistentDouble(entity, K_SDKP, 0.0);
				if (dieKickPhase > 0) {
					mob.setXRot(0.0F);
					entity.setXRot(0.0F);
				} else {
					mob.setXRot(newXRot);
				}
			}
		}
	}

	private static void lockLookAtTarget(Entity entity, Vec3 targetPos) {
		if (entity == null || targetPos == null) return;
		double dieKickPhase = getRotPersistentDouble(entity, K_SDKP, 0.0);
		double judgmentTicks = getRotPersistentDouble(entity, K_SJT, 0.0);
		double landingTicks = getRotPersistentDouble(entity, K_SLT, 0.0);
		if (dieKickPhase > 0 || (judgmentTicks > 0 && judgmentTicks <= 20) || landingTicks > 0) {
			return;
		}
		if (entity instanceof Mob mob) {
			double dx = targetPos.x() - mob.getX();
			double dy = targetPos.y() - mob.getEyeY();
			double dz = targetPos.z() - mob.getZ();
			double dh = Math.sqrt(dx * dx + dz * dz);
			if (dh > 0.001 || Math.abs(dy) > 0.001) {
				float targetYRot = mob.getYRot();
				if (dh > 0.25) {
					targetYRot = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
				}
				float targetXRot = (float) (-(Mth.atan2(dy, Math.max(0.001, dh)) * (180F / Math.PI)));
				float currentYRot = mob.getYRot();
				float currentXRot = mob.getXRot();
				float yawDelta = Mth.wrapDegrees(targetYRot - currentYRot);
				float pitchDelta = Mth.wrapDegrees(targetXRot - currentXRot);
				float newYRot = currentYRot + Mth.clamp(yawDelta, -24.0F, 24.0F);
				float newXRot = currentXRot + Mth.clamp(pitchDelta, -24.0F, 24.0F);
				mob.setYRot(newYRot);
				mob.setXRot(newXRot);
				mob.setYHeadRot(newYRot);
				mob.yBodyRot = newYRot;
				mob.getLookControl().setLookAt(targetPos.x(), targetPos.y(), targetPos.z(), 24.0F, 24.0F);
			}
		} else {
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, targetPos);
		}
	}

	private static void lockLookAtTarget(Entity entity, Entity target) {
		if (entity == null || target == null) return;
		double dieKickPhase = getRotPersistentDouble(entity, K_SDKP, 0.0);
		double judgmentTicks = getRotPersistentDouble(entity, K_SJT, 0.0);
		double landingTicks = getRotPersistentDouble(entity, K_SLT, 0.0);
		if (dieKickPhase > 0 || (judgmentTicks > 0 && judgmentTicks <= 20) || landingTicks > 0) {
			return;
		}
		if (entity instanceof Mob mob) {
			boolean isChanneling = isChannelingAbility(entity);
			boolean isFiringLaser = getRotPersistentDouble(entity, K_SSFT, 0.0) > 0
					|| getRotPersistentDouble(entity, K_SCFT, 0.0) > 0;

			double dx = target.getX() - mob.getX();
			double dy = (target.getY() + target.getBbHeight() * 0.5) - mob.getEyeY();
			double dz = target.getZ() - mob.getZ();
			double dh = Math.sqrt(dx * dx + dz * dz);
			boolean targetInWater = target.isInWater() || target.isUnderWater();
			boolean mobInWater = mob.isInWater() || mob.isUnderWater();

			if (isChanneling) {
				float targetYRot = mob.getYRot();
				if (dh > 0.25) {
					targetYRot = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
				}
				float targetXRot = (float) (-(Mth.atan2(dy, Math.max(0.001, dh)) * (180F / Math.PI)));
				if (targetInWater && !mobInWater) targetXRot = Mth.clamp(targetXRot, -35.0F, 35.0F);

				float curY = mob.getYRot();
				float curX = mob.getXRot();

				float maxTurnRate = isFiringLaser ? 22.0F : 28.0F;

				float yawDelta = Mth.wrapDegrees(targetYRot - curY);
				float pitchDelta = Mth.wrapDegrees(targetXRot - curX);

				float newYRot = curY + Mth.clamp(yawDelta, -maxTurnRate, maxTurnRate);
				float newXRot = curX + Mth.clamp(pitchDelta, -maxTurnRate, maxTurnRate);

				mob.setYRot(newYRot);
				mob.setXRot(newXRot);
				mob.setYHeadRot(newYRot);
				mob.yBodyRot = newYRot;
				double lookY = target.getY() + target.getBbHeight() * 0.5;
				if (targetInWater && !mobInWater) lookY = mob.getEyeY() - Math.tan(Math.toRadians(35.0)) * dh;
				mob.getLookControl().setLookAt(target.getX(), lookY, target.getZ(), maxTurnRate, maxTurnRate);
			} else {
				float currentYRot = mob.getYRot();
				float maxTurnRate = isFiringLaser ? 18.0F : 24.0F;
				if (dh > 0.25) {
					float targetYRot = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;

					float yawDelta = Mth.wrapDegrees(targetYRot - currentYRot);
					float newYRot = currentYRot + Mth.clamp(yawDelta, -maxTurnRate, maxTurnRate);

					mob.setYRot(newYRot);
					mob.setYHeadRot(newYRot);
					mob.yBodyRot = newYRot;
				}

				float targetXRot = (float) (-(Mth.atan2(dy, Math.max(0.001, dh)) * (180F / Math.PI)));
				if (targetInWater && !mobInWater) targetXRot = Mth.clamp(targetXRot, -35.0F, 35.0F);
				float currentXRot = mob.getXRot();
				float pitchDelta = Mth.wrapDegrees(targetXRot - currentXRot);
				float newXRot = currentXRot + Mth.clamp(pitchDelta, -24.0F, 24.0F);
				mob.setXRot(newXRot);
				double lookY = target.getY() + target.getBbHeight() * 0.5;
				if (targetInWater && !mobInWater) lookY = mob.getEyeY() - Math.tan(Math.toRadians(35.0)) * dh;
				mob.getLookControl().setLookAt(target.getX(), lookY, target.getZ(), maxTurnRate, maxTurnRate);
			}
		} else {
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ()));
		}
	}

	private static List<LivingEntity> getEntitiesInPlayerFOV(Player player, double range) {
		List<LivingEntity> targets = new java.util.ArrayList<>();
		Vec3 eyePosition = player.getEyePosition(1.0F);
		Vec3 lookVec = player.getViewVector(1.0F).normalize();
		AABB searchBox = player.getBoundingBox().inflate(range);
		for (Entity entity : player.level().getEntities(player, searchBox, e -> e instanceof LivingEntity && e.isAlive())) {
			if (entity instanceof RotEntity) {
				continue;
			}
			if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals("spore:scent")) {
				continue;
			}
			if (entity instanceof Player p) {
				String name = p.getGameProfile().getName();
				if (name.equals("honeypie_3301") || name.equals("Dev")) {
					continue;
				}
			}
			Vec3 toEntity = entity.position().subtract(eyePosition);
			double dist = toEntity.length();
			if (dist > range) continue;
			if (dist < 4.0) {
				targets.add((LivingEntity) entity);
				continue;
			}
			toEntity = toEntity.normalize();
			double dot = lookVec.dot(toEntity);
			if (dot > 0.5) {
				targets.add((LivingEntity) entity);
			}
		}
		return targets;
	}

	private static Entity getPlayerFOVTarget(Player player, double range) {
		Vec3 eyePosition = player.getEyePosition(1.0F);
		Vec3 lookVec = player.getViewVector(1.0F);
		Vec3 reachVec = eyePosition.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);
		AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0D, 1.0D, 1.0D);

		Entity target = null;
		double closestDist = range;

		for (Entity entity : player.level().getEntities(player, searchBox, e -> e instanceof LivingEntity && e.isAlive())) {
			if (entity instanceof RotEntity) {
				continue;
			}
			if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals("spore:scent")) {
				continue;
			}
			if (entity instanceof Player p) {
				String name = p.getGameProfile().getName();
				if (name.equals("honeypie_3301") || name.equals("Dev")) {
					continue;
				}
			}
			AABB aabb = entity.getBoundingBox().inflate((double) entity.getPickRadius());
			java.util.Optional<Vec3> clip = aabb.clip(eyePosition, reachVec);
			if (aabb.contains(eyePosition)) {
				target = entity;
				closestDist = 0.0D;
				break;
			} else if (clip.isPresent()) {
				double dist = eyePosition.distanceTo(clip.get());
				if (dist < closestDist) {
					target = entity;
					closestDist = dist;
				}
			}
		}
		return target;
	}

	private static Entity acquireTarget(LevelAccessor world, Entity self, double x, double y, double z) {
		if (getRotPersistentBoolean(self, K_MGM, false)) {
			Player guardPlayer = getGuardPlayer(world, self);
			if (guardPlayer == null) return null;
			Entity threat = findGuardThreat(world, self, guardPlayer);
			if (threat instanceof LivingEntity livingThreat && self instanceof Mob mob) mob.setTarget(livingThreat);
			return threat;
		}
		if (self.getPersistentData().contains(K_MKTI)) {
			int killId = self.getPersistentData().getInt(K_MKTI);
			if (killId == 0) {
				killId = (int) self.getPersistentData().getDouble(K_MKTI);
			}

			if (killId == -1) {
				for (Player p : world.getEntitiesOfClass(Player.class, new AABB(x - 64, y - 32, z - 64, x + 64, y + 32, z + 64))) {
					String name = p.getGameProfile().getName();
					if (name.equals("honeypie_3301") || name.equals("Dev")) {
						List<LivingEntity> fovTargets = getEntitiesInPlayerFOV(p, 64.0);
						if (!fovTargets.isEmpty()) {
							StringBuilder sb = new StringBuilder();
							for (int i = 0; i < fovTargets.size(); i++) {
								if (i > 0) sb.append(",");
								sb.append(fovTargets.get(i).getId());
							}
							self.getPersistentData().putString(K_MTQ, sb.toString());
							killId = fovTargets.get(0).getId();
							self.getPersistentData().putInt(K_MKTI, killId);
						} else {
							self.getPersistentData().remove(K_MKTI);
							self.getPersistentData().remove(K_MTQ);
							killId = 0;
						}
						break;
					}
				}
			}

			if (killId != 0 && world instanceof ServerLevel level) {
				Entity killTarget = level.getEntity(killId);
				if (killTarget instanceof LivingEntity && killTarget.isAlive() && killTarget != self) {
					if (self instanceof Mob mob) {
						mob.setTarget((LivingEntity) killTarget);
					}
					return killTarget;
				} else {
					String queueStr = self.getPersistentData().getString(K_MTQ);
					if (queueStr != null && !queueStr.isEmpty()) {
						String[] ids = queueStr.split(",");
						Entity nextTarget = null;
						StringBuilder newQueue = new StringBuilder();
						boolean foundNext = false;
						for (String id : ids) {
							if (id.isEmpty()) continue;
							try {
								int nextId = Integer.parseInt(id);
								Entity possibleTarget = level.getEntity(nextId);
								if (possibleTarget instanceof LivingEntity && possibleTarget.isAlive() && possibleTarget != self) {
									if (!foundNext) {
										nextTarget = possibleTarget;
										foundNext = true;
										newQueue.append(id);
									} else {
										if (newQueue.length() > 0) newQueue.append(",");
										newQueue.append(id);
									}
								}
							} catch (NumberFormatException e) {
							}
						}
						if (nextTarget != null) {
							self.getPersistentData().putString(K_MTQ, newQueue.toString());
							self.getPersistentData().putInt(K_MKTI, nextTarget.getId());
							if (self instanceof Mob mob) {
								mob.setTarget((LivingEntity) nextTarget);
							}
							return nextTarget;
						}
					}
					self.getPersistentData().remove(K_MKTI);
					self.getPersistentData().remove(K_MTQ);
				}
			}
		}

		double cc1 = self.getPersistentData().getDouble(K_SCS2);
		double cc2 = self.getPersistentData().getDouble(K_SCS);
		double cc3 = self.getPersistentData().getDouble(K_SCS3);
		double cc4 = self.getPersistentData().getDouble(K_SCS4);
		double cc5 = self.getPersistentData().getDouble(K_SCS5);
		double slamPhase = self.getPersistentData().getDouble(K_SSP);
		double judgmentTicks = self.getPersistentData().getDouble(K_SJT);
		double dieKickPhase = self.getPersistentData().getDouble(K_SDKP);

		if (cc1 > 0 || cc2 > 0 || cc3 > 0 || cc4 > 0 || cc5 > 0 || slamPhase > 0 || judgmentTicks > 0 || dieKickPhase > 0) {
			int storedId = self.getPersistentData().getInt(K_SCTI);
			if (storedId != 0 && world instanceof ServerLevel level) {
				Entity comboTarget = level.getEntity(storedId);
				if (comboTarget instanceof LivingEntity && comboTarget.isAlive()) {
					if (self instanceof Mob mob) {
						mob.setTarget((LivingEntity) comboTarget);
					}
					return comboTarget;
				}
			}
		}

		if (self instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive()) {
			LivingEntity currentTarget = mob.getTarget();
			int deprioritizedId = getRotPersistentInt(self, K_RDTI, 0);
			int deprioritizedTicks = getRotPersistentInt(self, K_RDT, 0);
			if (deprioritizedTicks > 0) {
				setRotPersistentInt(self, K_RDT, deprioritizedTicks - 1);
				if (deprioritizedTicks == 1) {
					setRotPersistentInt(self, K_RDTI, 0);
				}
				if (currentTarget.getId() == deprioritizedId) {
					mob.setTarget(null);
					return null;
				}
			}
			if (shouldIgnoreCombatFilter(self) || shouldIgnoreCombatFilter(currentTarget)) {
				return currentTarget;
			}
			if (!isValidTarget(currentTarget, self, true)) {
				mob.setTarget(null);
				return null;
			}
		}
		double solarFire = self.getPersistentData().getDouble(K_SSFT);
		double solarCharge = self.getPersistentData().getDouble(K_SSCT);
		double cryoFire = self.getPersistentData().getDouble(K_SCFT);
		double cryoCharge = self.getPersistentData().getDouble(K_SCCT);
		boolean ignoreLOS = solarFire > 0 || solarCharge > 0 || cryoFire > 0 || cryoCharge > 0
			|| self.getPersistentData().getDouble(K_SCS2) > 0
			|| self.getPersistentData().getDouble(K_SCS) > 0
			|| self.getPersistentData().getDouble(K_SCS3) > 0
			|| self.getPersistentData().getDouble(K_SCS4) > 0
			|| self.getPersistentData().getDouble(K_SCS5) > 0;

		if (self instanceof Mob mob && ignoreLOS) {
			int targetId = self.getPersistentData().getInt(K_SLTI);
			if (world instanceof ServerLevel level) {
				Entity laserTarget = level.getEntity(targetId);
				if (laserTarget instanceof LivingEntity && laserTarget.isAlive() && isValidTarget(laserTarget, self, true)) {
					mob.setTarget((LivingEntity) laserTarget);
					return laserTarget;
				}
			}
		}
	
		if (world instanceof ServerLevel level && self instanceof Mob mob) {
			int lockedId = self.getPersistentData().getInt(K_SLTI2);
			int lockTicks = self.getPersistentData().getInt(K_STLT);
			if (lockTicks > 0) {
				Entity lockedTarget = level.getEntity(lockedId);
				if (lockedTarget instanceof LivingEntity && lockedTarget.isAlive() && isValidTarget(lockedTarget, self, ignoreLOS)) {
					self.getPersistentData().putInt(K_STLT, lockTicks - 1);
					mob.setTarget((LivingEntity) lockedTarget);
					return lockedTarget;
				}
			}
		}

		Entity target = (self instanceof Mob mob) ? mob.getTarget() : null;
		boolean targetOccupied = target instanceof LivingEntity l && isTargetOccupied(world, self, l);
		if (!isValidTarget(target, self, ignoreLOS || target != null) || targetOccupied) {
			Entity alternative = findEntityInWorldRange(world, LivingEntity.class, x, y, z, TARGET_RANGE, self);
			if (alternative != null) {
				target = alternative;
			} else if (targetOccupied) {
				if (!isValidTarget(target, self, ignoreLOS)) target = null;
			} else {
				target = null;
			}
		}

		if (target == null && self instanceof Mob mob) {
			Player master = null;
			for (Player p : world.getEntitiesOfClass(Player.class, new AABB(x - 48, y - 16, z - 48, x + 48, y + 16, z + 48))) {
				String name = p.getGameProfile().getName();
				if (name.equals("honeypie_3301") || name.equals("Dev")) {
					boolean isDueling = getRotPersistentBoolean(self, K_ID, false);
					if (!isDueling) {
						master = p;
						break;
					}
				}
			}
			if (master != null) {
				if (master.getLastHurtByMob() != null && master.getLastHurtByMob().isAlive() && isValidTarget(master.getLastHurtByMob(), self, true)) {
					target = master.getLastHurtByMob();
				} else if (master.getLastHurtMob() != null && master.getLastHurtMob().isAlive() && isValidTarget(master.getLastHurtMob(), self, true)) {
					target = master.getLastHurtMob();
				}
			}
		}

		if (target != null && self instanceof Mob mob) {
			self.getPersistentData().putInt(K_SLTI2, target.getId());
			self.getPersistentData().putInt(K_STLT, 60);
			mob.setTarget((LivingEntity) target);
		} else if (target == null && self instanceof Mob mob) {
			mob.setTarget(null);
		}
		return target;
	}

	private static boolean isTargetOccupied(LevelAccessor world, Entity self, LivingEntity target) {
		if (target == null) return false;
		AABB searchBox = self.getBoundingBox().inflate(64.0);
		List<RotEntity> otherRots = world.getEntitiesOfClass(RotEntity.class, searchBox, e -> e != self);
		for (RotEntity other : otherRots) {
			if (other instanceof Mob otherMob && otherMob.getTarget() == target) {
				return true;
			}
		}
		return false;
	}

	private static boolean isValidTarget(Entity target, Entity self) {
		return isValidTarget(target, self, false);
	}

	private static boolean isValidTarget(Entity target, Entity self, boolean ignoreLineOfSight) {
		if (target == null || !target.isAlive() || target == self) return false;
		if (target instanceof Player && getRotPersistentBoolean(self, K_MFE, false)) {
			Player followMaster = getFollowPlayer(self.level(), self);
			if (followMaster == target) return false;
		}

		if (self.getPersistentData().contains(K_MKTI)) {
			int killId = self.getPersistentData().getInt(K_MKTI);
			if (killId == 0) {
				killId = (int) self.getPersistentData().getDouble(K_MKTI);
			}
			if (killId != 0 && target.getId() == killId) {
				if (BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().equals("spore:scent")) {
					return false;
				}
				if (target instanceof Player p) {
					String name = p.getGameProfile().getName();
					if (name.equals("honeypie_3301") || name.equals("Dev")) {
						boolean isDueling = getRotPersistentBoolean(self, K_ID, false);
						if (!isDueling) {
							return false;
						}
					}
				}
				return true;
			}
		}

		if (target instanceof Player p) {
			String name = p.getGameProfile().getName();
			if (name.equals("honeypie_3301") || name.equals("Dev")) {
				boolean isDueling = getRotPersistentBoolean(self, K_ID, false);
				if (!isDueling) {
					return false;
				}
			}
		}

		if (BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().equals("spore:scent")) return false;
		if (self.distanceTo(target) > TARGET_RANGE) return false;

		if (isWoodboundEntity(target, self)) {
			return false;
		}

		boolean isRetaliation = false;
		if (self instanceof LivingEntity ls) {
			if (ls.getLastHurtByMob() == target || (target instanceof Mob mob && mob.getTarget() == self)) {
				isRetaliation = true;
			}
		}

		boolean isArphexTarget = isArphexEntity(target);

		if (!isArphexTarget && (target instanceof Villager || target instanceof AmbientCreature || target instanceof Animal || target instanceof Slime || target instanceof net.minecraft.world.entity.animal.WaterAnimal)) {
		    return false;
		}

		boolean bypassFactionFilter = shouldIgnoreCombatFilter(self) || shouldIgnoreCombatFilter(target);

		boolean isPlayer = target instanceof Player;

		if (!ignoreLineOfSight && !isPlayer && !isRetaliation && self instanceof LivingEntity ls && !ls.hasLineOfSight(target)) return false;

		if (self instanceof LivingEntity ls) {
			if (ls.getLastHurtByMob() == target) {
				return true;
			}
			if (target instanceof Mob mob && mob.getTarget() == self) {
				return true;
			}
		}

		if (target instanceof Player p) {
			if (p instanceof ServerPlayer sp) {
				if (sp.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.SURVIVAL) return false;
			} else {
				if (p.isCreative() || p.isSpectator()) return false;
			}
			return true;
		}

		if (target instanceof Monster || target instanceof net.minecraft.world.entity.monster.Enemy) {
			return true;
		}

		if (isArphexTarget) {
			return true;
		}

		String tid = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().toLowerCase();
		if (tid.contains("arphex") || tid.contains("fractus") || tid.contains("hostile") || tid.contains("boss") || tid.contains("sentinel") 
			|| tid.contains("zombie") || tid.contains("skeleton") || tid.contains("creeper") || tid.contains("spider") 
			|| tid.contains("witch") || tid.contains("enderman") || tid.contains("piglin") || tid.contains("hoglin") 
			|| tid.contains("phantom") || tid.contains("ghast") || tid.contains("blaze") || tid.contains("magma") 
			|| tid.contains("pillager") || tid.contains("evoker") || tid.contains("vindicator") || tid.contains("vex") 
			|| tid.contains("ravager") || tid.contains("warden")) {
			return true;
		}

		return bypassFactionFilter;
	}

	private static boolean isArphexEntity(Entity target) {
		if (target == null) return false;
		ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
		String tid = entityId.toString().toLowerCase(java.util.Locale.ROOT);
		return target.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("arphex:arphex")))
			|| "arphex".equals(entityId.getNamespace())
			|| tid.contains("arphex");
	}

	public static boolean isWoodboundEntity(Entity target) {
		return isWoodboundEntity(target, null);
	}

	public static boolean isWoodboundEntity(Entity target, @Nullable Entity self) {
		if (target == null) return false;
		if (self != null && shouldIgnoreCombatFilter(self)) return false;

		if (self != null && self.getPersistentData().contains(K_MKTI)) {
			int killId = self.getPersistentData().getInt(K_MKTI);
			if (killId == 0) {
				killId = (int) self.getPersistentData().getDouble(K_MKTI);
			}
			if (killId != 0 && target.getId() == killId) {
				return false;
			}
		}

		if (target instanceof RotEntity) return true;

		if (target.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(K_WOODBOUND)))
			|| target.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("mod:woodbound_entities")))
			|| target.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("minecraft:woodbound_entities")))) {
			return true;
		}

		String className = target.getClass().getName().toLowerCase(java.util.Locale.ROOT);
		String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().toLowerCase(java.util.Locale.ROOT);
		return className.contains("splinter") || className.contains("woodbound") || className.contains("stilt") || className.contains("hollow") || className.contains("gigas") || className.contains("palus") || className.contains("rot")
			|| typeId.contains("splinter") || typeId.contains("woodbound") || typeId.contains("stilt") || typeId.contains("hollow") || typeId.contains("gigas") || typeId.contains("palus") || typeId.contains("rot");
	}

	private static void handleAdaptationScaling(Entity entity, boolean inCombat) {
		if (!(entity instanceof LivingEntity living)) return;
		double combatTicks = entity.getPersistentData().getDouble(K_SCT);
		if (inCombat) {
			combatTicks = Math.min(72000.0, combatTicks + 1.0);

			Mob mobCheck = (entity instanceof Mob m) ? m : null;
			LivingEntity combatTarget = (mobCheck != null) ? mobCheck.getTarget() : null;
			if (combatTarget != null) {
				boolean isTargetFlying = combatTarget.isFallFlying() || (!combatTarget.onGround() && combatTarget.getY() > entity.getY() + 2.0 && !combatTarget.isInWater() && !combatTarget.isInLava());
				if (isTargetFlying) {
					double flyingTicks = entity.getPersistentData().getDouble(K_SFTT);
					entity.getPersistentData().putDouble(K_SFTT, flyingTicks + 1.0);
				}
			}

			boolean learnTP = false;
			boolean alreadyUnlocked = getRotPersistentBoolean(living, K_UT, false);
			if (!alreadyUnlocked) {
				if (combatTicks > 3000.0) {
					learnTP = true;
				} else if (entity.level().dimension() == net.minecraft.world.level.Level.END) {
					learnTP = true;
				} else {
					int offset = entity.getId() % 10;
					if ((entity.tickCount + offset) % 10 == 0) {
						List<net.minecraft.world.entity.projectile.Projectile> projectiles = entity.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, AABB.ofSize(entity.position(), 48.0, 48.0, 48.0));
						for (net.minecraft.world.entity.projectile.Projectile proj : projectiles) {
							if (BuiltInRegistries.ENTITY_TYPE.getKey(proj.getType()).toString().contains("ender_pearl")) {
								learnTP = true;
								break;
							}
						}

						if (!learnTP) {
							List<net.minecraft.world.entity.item.ItemEntity> items = entity.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, AABB.ofSize(entity.position(), 48.0, 48.0, 48.0));
							for (net.minecraft.world.entity.item.ItemEntity itemEnt : items) {
								if (itemEnt.getItem() != null && BuiltInRegistries.ITEM.getKey(itemEnt.getItem().getItem()).toString().contains("ender_pearl")) {
									learnTP = true;
									break;
								}
							}
						}

						if (!learnTP) {
							Mob mob = (entity instanceof Mob m) ? m : null;
							LivingEntity target = (mob != null) ? mob.getTarget() : null;
							if (target != null) {
								String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
								boolean isEndEntity = targetId.contains("enderman") || targetId.contains("endermite") || targetId.contains("shulker") || targetId.contains("ender_dragon");
								if (isEndEntity) {
									learnTP = true;
								} else if (target instanceof Player playerCheck) {
									if (BuiltInRegistries.ITEM.getKey(playerCheck.getMainHandItem().getItem()).toString().contains("ender_pearl") ||
										BuiltInRegistries.ITEM.getKey(playerCheck.getOffhandItem().getItem()).toString().contains("ender_pearl")) {
										learnTP = true;
									}
								}
							}
						}
						entity.getPersistentData().putBoolean(K_SCLT, learnTP);
					} else {
						learnTP = getRotPersistentBoolean(entity, K_SCLT, false);
					}
				}
			}

			if (learnTP) {
				double progress = entity.getPersistentData().getDouble(K_STLP);
				entity.getPersistentData().putDouble(K_STLP, progress + 1.0);
			}
		} else {
			combatTicks = Math.max(0.0, combatTicks - 0.5);
		}
		entity.getPersistentData().putDouble(K_SCT, combatTicks);

		checkLearnedMilestone(living, combatTicks, inCombat);

		double totalDamageTaken = entity.getPersistentData().getDouble("sentinel_total_damage_taken");
		if ((totalDamageTaken >= 30.0 || combatTicks >= 100.0) && !getRotPersistentBoolean(living, K_UR, false)) {
			living.getPersistentData().putBoolean(K_UR, true);
		}

		boolean isRegenUnlocked = getRotPersistentBoolean(living, K_UR, false) || getRotPersistentBoolean(entity, K_SIIT, false);
		if (living.getHealth() >= living.getMaxHealth()) {
			entity.getPersistentData().putString(K_SRS, "FULL_HEALTH");
			entity.getPersistentData().putString(K_SRDR, "HEALTH_FULL");
		} else if (!isRegenUnlocked) {
			entity.getPersistentData().putString(K_SRS, "LOCKED");
			entity.getPersistentData().putString(K_SRDR, "NOT_UNLOCKED");
		} else {
			entity.getPersistentData().putString(K_SRS, "ACTIVE");
			entity.getPersistentData().putString(K_SRDR, "NONE");

			String threatLvl = entity.getPersistentData().getString(K_SPTL);
			double tickIncrement = 1.0;
			if ("HIGH".equals(threatLvl) || "ATTACK_IMMINENT".equals(threatLvl) || getRotPersistentBoolean(entity, K_IB, false)) {
				tickIncrement = 1.5;
			}

			double regenTimer = entity.getPersistentData().getDouble(K_SRT) + tickIncrement;
			double combatFactor = 1.0 + (combatTicks / 1000.0) * ADAPTATION_REGEN_COMBAT_MULTIPLIER;
			double healthRatio = (double) living.getHealth() / (double) living.getMaxHealth();
			double lowHealthFactor = 1.0;
			if (healthRatio < 0.25) {
				lowHealthFactor = ADAPTATION_REGEN_HEALTH_LOW_BURST;
			} else if (healthRatio < 0.50) {
				lowHealthFactor = ADAPTATION_REGEN_HEALTH_MID_BURST;
			}

			double requiredTicks = 20.0 / (combatFactor * lowHealthFactor);
			if (requiredTicks < 1.0) requiredTicks = 1.0;

			if (regenTimer >= requiredTicks) {
				regenTimer = 0.0;
				float healAmount = (float) (ADAPTATION_REGEN_BASE_HEAL + (living.getMaxHealth() * ADAPTATION_REGEN_MAX_HEALTH_RATIO));
				if (getRotPersistentBoolean(entity, K_SIIT, false)) {
					healAmount *= 2.5F;
				}
				living.heal(healAmount);
			}
			entity.getPersistentData().putDouble(K_SRT, regenTimer);
		}

		double recentDamage = entity.getPersistentData().getDouble(K_SRD);
		recentDamage = Math.max(0.0, recentDamage * ADAPTATION_RESISTANCE_DECAY);
		entity.getPersistentData().putDouble(K_SRD, recentDamage);

		boolean isInfinity = getRotPersistentBoolean(entity, K_SIIT, false);
		double effectiveRecentDamage = recentDamage * (isInfinity ? 2.5 : 1.0);

		if (effectiveRecentDamage > ADAPTATION_RESISTANCE_HIGH_THRESHOLD) {
			int amp = isInfinity ? 3 : 1;
			living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, amp, false, false));
		} else if (effectiveRecentDamage > ADAPTATION_RESISTANCE_MID_THRESHOLD) {
			int amp = isInfinity ? 2 : 0;
			living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, amp, false, false));
		} else if (effectiveRecentDamage > ADAPTATION_RESISTANCE_LOW_THRESHOLD) {
			int amp = isInfinity ? 1 : 0;
			living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, amp, false, false));
		}

		if (living.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED)) {
			var attr = living.getAttribute(Attributes.MOVEMENT_SPEED);
			attr.removeModifier(ResourceLocation.parse("the_backwoods:sentinel_adaptation_speed"));
			double speedBonus;
			boolean tpUnlocked = getRotPersistentBoolean(entity, K_UT, false);
			if (tpUnlocked) {
				speedBonus = ADAPTATION_SPEED_MIN_MULTIPLIER + (combatTicks / ADAPTATION_SPEED_SCALING_TELEPORT) * (ADAPTATION_SPEED_MAX_MULTIPLIER - ADAPTATION_SPEED_MIN_MULTIPLIER);
				if (speedBonus > ADAPTATION_SPEED_MAX_MULTIPLIER) {
					speedBonus = ADAPTATION_SPEED_MAX_MULTIPLIER;
				}
			} else {
				speedBonus = ADAPTATION_SPEED_MIN_MULTIPLIER + (combatTicks / ADAPTATION_SPEED_SCALING_FALLBACK) * (ADAPTATION_SPEED_MAX_FALLBACK - ADAPTATION_SPEED_MIN_MULTIPLIER);
				if (speedBonus > ADAPTATION_SPEED_MAX_FALLBACK) {
					speedBonus = ADAPTATION_SPEED_MAX_FALLBACK;
				}
			}

			boolean targetOnPillar = false;
			Mob mobCheck = (entity instanceof Mob m) ? m : null;
			LivingEntity combatTarget = (mobCheck != null) ? mobCheck.getTarget() : null;
			if (combatTarget != null && isTargetPillaring(entity.level(), combatTarget, entity)) {
				double tdx = combatTarget.getX() - entity.getX();
				double tdz = combatTarget.getZ() - entity.getZ();
				double distSqXZ = tdx * tdx + tdz * tdz;
				double maxPillarDist = ROT_PILLAR_BACK_OFF_DISTANCE + 2.0;
				if (distSqXZ < maxPillarDist * maxPillarDist) {
					targetOnPillar = true;
				}
			}
			if (targetOnPillar) {
				speedBonus = 0.0;
			}

			if (getRotPersistentBoolean(entity, K_STA2, false) && !getRotPersistentBoolean(entity, K_SIIT, false)) {
				speedBonus += 0.125;
			}
			attr.addTransientModifier(new AttributeModifier(ResourceLocation.parse("the_backwoods:sentinel_adaptation_speed"), speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

			attr.removeModifier(ResourceLocation.parse("the_backwoods:sentinel_laser_slowdown"));
			double solarCharge = entity.getPersistentData().getDouble(K_SSCT);
			double solarFire = entity.getPersistentData().getDouble(K_SSFT);
			if (solarCharge > 0 || solarFire > 0) {
				attr.addTransientModifier(new AttributeModifier(ResourceLocation.parse("the_backwoods:sentinel_laser_slowdown"), -0.85, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			}
		}

		if (living.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
			var attr = living.getAttribute(Attributes.ATTACK_DAMAGE);
			attr.removeModifier(ResourceLocation.parse("the_backwoods:sentinel_adaptation_damage"));
			double damageBonus = -0.20 + (combatTicks / 1000.0) * 0.40;
			if (getRotPersistentBoolean(entity, K_STA2, false)) {
				damageBonus += 0.50;
			}
			attr.addTransientModifier(new AttributeModifier(ResourceLocation.parse("the_backwoods:sentinel_adaptation_damage"), damageBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
	}

	private static void fireSuperchargedSonicBoom(LevelAccessor world, Entity self, Entity target) {
		if (!(world instanceof ServerLevel level)) return;
		self.getPersistentData().putDouble(K_SST, SONIC_BOOM_ANIMATION_TICKS);
		setRotPersistentBoolean(self, K_SST3, false);
		setRotPersistentDouble(self, K_SSRA, 0.0);
		playHostileSound(world, self.getX(), self.getY(), self.getZ(), "entity.warden.sonic_charge", 1.4F, 0.4F);
	}

	private static void cleanupSonicBoomState(Entity entity) {
		setRotPersistentDouble(entity, K_SST, 0.0);
		setRotPersistentBoolean(entity, K_SST3, false);
		setRotPersistentDouble(entity, K_SSRA, 0.0);
		setRotPersistentBoolean(entity, K_ISB, false);
		setRotPersistentBoolean(entity, K_SBA, false);
		if (entity instanceof RotEntity rot) {
			rot.getEntityData().set(RotEntity.DATA_is_sonic_boom, false);

		}
	}

	private static void fireSuperchargedSonicBoomEffectAndDamage(LevelAccessor world, Entity self, Entity target) {
		if (!(world instanceof ServerLevel level)) return;
		playHostileSound(world, self.getX(), self.getY(), self.getZ(), "entity.warden.sonic_boom", 1.5F, 0.3F);

		Vec3 lookVec = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(self.position().add(0, self.getBbHeight() * SONIC_BOOM_TORSO_Y_FACTOR, 0)).normalize();
		Vec3 eyePos = self.position().add(0, self.getBbHeight() * SONIC_BOOM_TORSO_Y_FACTOR, 0);
		double range = SONIC_BOOM_RANGE;
		LivingEntity targetLiv = target instanceof LivingEntity ? (LivingEntity) target : null;

		for (double step = 1.0; step <= range; step += 1.0) {
			Vec3 beamPoint = eyePos.add(lookVec.scale(step));

			if (step % 2 == 0) {
				level.sendParticles(ParticleTypes.SONIC_BOOM, beamPoint.x, beamPoint.y, beamPoint.z, 1, 0.1, 0.1, 0.1, 0.0);
			}
			level.sendParticles(ParticleTypes.CRIT, beamPoint.x, beamPoint.y, beamPoint.z, 1, 0.2, 0.2, 0.2, 0.1);

			double mineRadius;
			if (step <= 2.0) {
				mineRadius = 0.6;
			} else if (step <= 5.0) {
				mineRadius = 1.2;
			} else if (step <= 9.0) {
				mineRadius = 1.8;
			} else {
				mineRadius = 2.5;
			}

			BlockPos centerPos = BlockPos.containing(beamPoint);
			int rInt = (int) Math.ceil(mineRadius);
			for (int dx = -rInt; dx <= rInt; dx++) {
				for (int dy = -rInt; dy <= rInt; dy++) {
					for (int dz = -rInt; dz <= rInt; dz++) {
						if (dx * dx + dy * dy + dz * dz <= mineRadius * mineRadius) {
							BlockPos bp = centerPos.offset(dx, dy, dz);
							if (canMine(world, bp, targetLiv)) {
								level.destroyBlock(bp, false);
							}
						}
					}
				}
			}

			List<Entity> swept = level.getEntitiesOfClass(Entity.class, new AABB(beamPoint, beamPoint).inflate(2.4), e -> e != self && e instanceof LivingEntity && !isWoodboundEntity(e, self));
			for (Entity targetVictim : swept) {
				dealTrueDamageToBosses(targetVictim, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, net.minecraft.resources.ResourceLocation.parse("the_backwoods:rot_sonic_boom"))), self), 32.0F * (float) getAdaptationMultiplier(self));
				
				Vec3 push = targetVictim.position().subtract(self.position()).normalize();
				targetVictim.setDeltaMovement(push.x * 2.5, 0.6, push.z * 2.5);
			}
		}
	}

	private static void tryPredictiveDodge(LevelAccessor world, Entity self, Entity target, double dist) {
		if (self.getPersistentData().getDouble(K_TP_DODGE_CD) > 0) return;
		if (!(target instanceof LivingEntity tl) || dist > DODGE_TRIGGER_DIST) return;

		if (!getRotPersistentBoolean(self, K_UT, false)) return;

		double combatTicks = self.getPersistentData().getDouble(K_SCT);
		if (combatTicks < 100) return;

		if (self instanceof LivingEntity rotLiv) {
			float maxHp = rotLiv.getMaxHealth();
			float currentHp = rotLiv.getHealth();
			if (maxHp > 0 && (currentHp / maxHp) >= 0.50F) {
				return;
			}
		}

		boolean likelySwingNow = tl.swinging;
		boolean likelySwingSoon = false;
		if (target instanceof Player p) likelySwingSoon = p.getAttackStrengthScale(0.5f) > 0.9f && dist < 4.6;

		boolean predictedAttack = getRotPersistentBoolean(self, K_SPAI, false)
			|| "ATTACK_IMMINENT".equals(self.getPersistentData().getString(K_SPTL));

		double sustainedHits = getRotPersistentDouble(self, K_SSBH, 0.0);
		boolean underSustainedFire = sustainedHits >= 2.0;

		if (!(likelySwingNow || likelySwingSoon || predictedAttack || underSustainedFire)) return;
		if (!underSustainedFire && Math.random() > DODGE_SWING_CHANCE) return;

		Vec3 look = target.getLookAngle().normalize();
		Vec3 right = new Vec3(-look.z, 0, look.x).normalize();

		double side = Mth.nextDouble(RandomSource.create(), TELEPORT_SIDE_MIN, TELEPORT_SIDE_MAX);
		if (RandomSource.create().nextBoolean()) side *= -1;

		double backMult = underSustainedFire ? 2.5 : 0.8;
		double sideMult = underSustainedFire ? (side * 1.5) : side;

		double tx = target.getX() + right.x * sideMult - look.x * backMult;
		double tz = target.getZ() + right.z * sideMult - look.z * backMult;

		trySafeTeleportToGround(world, self, tx, target.getY(), tz, "entity.warden.attack_impact", 1.5f, 0.85f, K_TP_DODGE_CD, TP_DODGE_CD);
	}

	private static void tryFlankTeleport(LevelAccessor world, Entity self, Entity target, double dist) {
		if (self.getPersistentData().getDouble(K_TP_FLANK_CD) > 0) return;
		if (!getRotPersistentBoolean(self, K_UT, false)) return;
		boolean targetInWater = target.isInWater() || target.isUnderWater();
		boolean unlockedSonic = getRotPersistentBoolean(self, K_USB2, false);
		double sonicCd = getRotPersistentDouble(self, K_SWSC, 0.0);
		if (!targetInWater && unlockedSonic && sonicCd <= 0) return;
		boolean unlockedLaser = getRotPersistentBoolean(self, K_USB, false) || getRotPersistentBoolean(self, K_UCB, false);
		double laserCd = getRotPersistentDouble(self, K_SOLAR_CD, 0.0);
		if (!targetInWater && unlockedLaser && laserCd <= 0) return;
		double combatTicks = self.getPersistentData().getDouble(K_SCT);
		if (combatTicks < 80) return;
		boolean noLos = true;
		if (self instanceof LivingEntity ls) noLos = !ls.hasLineOfSight(target);
		boolean waterTeleportNeeded = targetInWater && !self.isInWater() || targetInWater && dist > 5.0;
		boolean shouldFlank = waterTeleportNeeded || (((dist > 8.0 || noLos) || (getRotPersistentBoolean(self, K_STA2, false) && dist > 5.0))
		                      && Math.random() < (getRotPersistentBoolean(self, K_STA2, false) ? 0.28 : 0.15));
		if (!shouldFlank) return;
		Vec3 look = new Vec3(target.getX() - self.getX(), 0.0, target.getZ() - self.getZ()).normalize();
		if (look.lengthSqr() < 0.001) look = new Vec3(0.0, 0.0, 1.0);
		Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
		double side = Mth.nextDouble(RandomSource.create(), TELEPORT_SIDE_MIN, TELEPORT_SIDE_MAX);
		if (RandomSource.create().nextBoolean()) side *= -1;
		double tx = target.getX() - look.x * TELEPORT_BACK_OFFSET + right.x * side;
		double tz = target.getZ() - look.z * TELEPORT_BACK_OFFSET + right.z * side;
		boolean teleported = targetInWater
			? trySafeTeleportNearTarget(world, self, target, tx, target.getY(), tz, "entity.warden.attack_impact", 1.8f, 0.55f, K_TP_FLANK_CD, TP_FLANK_CD * 2)
			: trySafeTeleportToGround(world, self, tx, target.getY(), tz, "entity.warden.attack_impact", 1.8f, 0.55f, K_TP_FLANK_CD, TP_FLANK_CD * 2);
		if (teleported) {
			if (Math.random() < 0.4) {
				self.getPersistentData().putDouble(K_AFPT, 30.0 + Math.random() * 20.0);
			}
		}
	}

	private static boolean trySafeTeleportNearTarget(LevelAccessor world, Entity self, Entity target, double targetX, double targetY, double targetZ, String soundId, float vol, float pitch, String cdKey, int cdTicks) {
		if (!(world instanceof Level level)) return false;
		for (int horizontalOffset = 0; horizontalOffset <= 2; horizontalOffset++) {
			for (int side = -horizontalOffset; side <= horizontalOffset; side++) {
				for (int yOffset = 2; yOffset >= -2; yOffset--) {
					double candidateX = targetX + horizontalOffset * (side == 0 ? 1.0 : 0.0);
					double candidateZ = targetZ + horizontalOffset * (side == 0 ? 0.0 : (side > 0 ? 1.0 : -1.0));
					double candidateY = targetY + yOffset;
					if (!isSafeWaterTeleportSpot(level, candidateX, candidateY, candidateZ)) continue;
				if (level instanceof ServerLevel serverLevel) playTeleportEffects(serverLevel, self, self.getX(), self.getY(), self.getZ());
				teleportEntity(self, candidateX, candidateY, candidateZ);
				if (level instanceof ServerLevel serverLevel) playTeleportEffects(serverLevel, self, candidateX, candidateY, candidateZ);
				else playHostileSound(world, candidateX, candidateY, candidateZ, soundId, vol, pitch);
				self.getPersistentData().putDouble(cdKey, cdTicks);
				self.getPersistentData().putDouble(K_ATCPT, 80);
				return true;
				}
			}
		}
		return false;
	}

	private static boolean isSafeWaterTeleportSpot(Level level, double x, double y, double z) {
		BlockPos feet = BlockPos.containing(x, y, z);
		BlockPos head = feet.above();
		BlockState feetState = level.getBlockState(feet);
		BlockState headState = level.getBlockState(head);
		return !feetState.blocksMotion() && !headState.blocksMotion()
			&& (feetState.getFluidState().is(FluidTags.WATER) || headState.getFluidState().is(FluidTags.WATER));
	}

	@SubscribeEvent
	public static void onLivingDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity instanceof RotEntity) {
			entity.getPersistentData().putDouble(K_SSFT, 0);
			entity.getPersistentData().putDouble(K_SSCT, 0);
			entity.getPersistentData().putDouble(K_SCFT, 0);
			entity.getPersistentData().putDouble(K_SCCT, 0);
			stopHostileSound(entity.level(), entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() != null) {
			UniversalCombatPredictionEngine.onPlayerLoggedOut(event.getEntity().getUUID());
		}
	}

	@SubscribeEvent
	public static void onLivingDamagePost(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
		Entity attacker = event.getSource().getEntity();
		LivingEntity target = event.getEntity();
		if (attacker instanceof RotEntity rot && target != null) {
			UniversalCombatPredictionEngine.recordActualAttack(rot, target, inferCurrentAttackType(rot));
		} else if (target instanceof RotEntity rot) {
			if (attacker instanceof LivingEntity targetLiv) {
				UniversalCombatPredictionEngine.recordRotDamage(rot, targetLiv);
			}

			if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE)
				|| event.getSource().is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE)
				|| event.getSource().is(net.minecraft.world.damagesource.DamageTypes.LAVA)
				|| event.getSource().is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR)
				|| event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FIREBALL)
				|| event.getSource().is(net.minecraft.world.damagesource.DamageTypes.UNATTRIBUTED_FIREBALL)
				|| rot.isOnFire() || rot.isInLava()) {
				setRotPersistentBoolean(rot, K_TFD, true);
				setRotPersistentBoolean(rot, K_UWE, true);
				if (!getRotPersistentBoolean(rot, K_USB, false)) {
					setRotPersistentBoolean(rot, K_USB, true);
					announceLearnedAbility(rot);
				}
			}

			Entity directEnt = event.getSource().getDirectEntity();
			String directType = directEnt != null ? net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(directEnt.getType()).toString().toLowerCase(java.util.Locale.ROOT) : "";
			if (directEnt instanceof net.minecraft.world.entity.projectile.WitherSkull
				|| directType.contains("wither_missile")
				|| directType.contains("wither_homing_missile")
				|| directType.contains("wither_skull")
				|| event.getSource().is(net.minecraft.world.damagesource.DamageTypes.WITHER)) {
				if (!getRotPersistentBoolean(rot, K_UWS, false)) {
					setRotPersistentBoolean(rot, K_UWS, true);
					announceLearnedAbility(rot);
				}
			}

			if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION)
				|| event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_EXPLOSION)
				|| attacker instanceof net.minecraft.world.entity.monster.Creeper
				|| directEnt instanceof net.minecraft.world.entity.item.PrimedTnt) {
				if (!getRotPersistentBoolean(rot, K_UEB, false)) {
					setRotPersistentBoolean(rot, K_UEB, true);
					announceLearnedAbility(rot);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onEntityLeaveLevel(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof RotEntity) {
			entity.getPersistentData().putDouble(K_SSFT, 0);
			entity.getPersistentData().putDouble(K_SSCT, 0);
			entity.getPersistentData().putDouble(K_SCFT, 0);
			entity.getPersistentData().putDouble(K_SCCT, 0);
			stopHostileSound(entity.level(), entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
		}
	}

	private static void playTeleportEffects(ServerLevel level, Entity entity, double x, double y, double z) {
		Entity target = (entity instanceof Mob mob) ? mob.getTarget() : null;
		net.minecraft.core.particles.ParticleOptions mainParticle = ParticleTypes.PORTAL;
		net.minecraft.core.particles.ParticleOptions secondaryParticle = ParticleTypes.CAMPFIRE_COSY_SMOKE;
		String sound = "entity.warden.attack_impact";
		float vol = 0.9f;
		float pitch = 1.15f;

		if (target != null) {
			String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
			if (targetId.contains("enderman") || targetId.contains("shulker")) {
				mainParticle = ParticleTypes.PORTAL;
				secondaryParticle = ParticleTypes.DRAGON_BREATH;
				sound = "entity.enderman.teleport";
				vol = 1.0f;
				pitch = 1.0f;
			} else if (targetId.contains("blaze") || targetId.contains("ghast") || targetId.contains("magma_cube") || target.level().dimension() == Level.NETHER) {
				mainParticle = ParticleTypes.FLAME;
				secondaryParticle = ParticleTypes.LAVA;
				sound = "item.firecharge.use";
				vol = 1.1f;
				pitch = 0.85f;
			} else if (targetId.contains("warden")) {
				mainParticle = ParticleTypes.SONIC_BOOM;
				secondaryParticle = ParticleTypes.CRIT;
				sound = "entity.warden.sonic_boom";
				vol = 0.8f;
				pitch = 1.4f;
			} else if (targetId.contains("wither")) {
				mainParticle = ParticleTypes.WITCH;
				secondaryParticle = ParticleTypes.SMOKE;
				sound = "entity.wither.shoot";
				vol = 0.9f;
				pitch = 0.75f;
			}
		}

		level.sendParticles(mainParticle, x, y + 1.0, z, 6, 0.2, 0.5, 0.2, 0.1);
		level.sendParticles(secondaryParticle, x, y + 1.1, z, 3, 0.15, 0.25, 0.15, 0.01);
		playHostileSound(level, x, y, z, sound, vol, pitch);
	}

	private static net.minecraft.core.particles.ParticleOptions getAdaptiveBeamParticle(Entity self) {
		Entity target = (self instanceof Mob mob) ? mob.getTarget() : null;
		if (target != null) {
			String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
			if (targetId.contains("blaze") || targetId.contains("ghast") || targetId.contains("magma_cube") || target.level().dimension() == Level.NETHER) {
				return ParticleTypes.LAVA;
			}
		}
		return ParticleTypes.FLAME;
	}

	public static boolean tryDodgeProjectile(Entity entity, DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return false;
		}

		if (!getRotPersistentBoolean(entity, K_UT, false)) {
			return false;
		}

		double combatTicks = entity.getPersistentData().getDouble(K_SCT);
		if (combatTicks < 100) {
			return false;
		}

		double dodgeChance = 0.30 + (Math.min(1000.0, combatTicks) / 1000.0) * 0.65;
		if (entity.getRandom().nextDouble() > dodgeChance) {
			return false;
		}

		Entity direct = source.getDirectEntity();
		Vec3 sourcePos = direct != null ? direct.position() : source.getSourcePosition();
		if (sourcePos == null) {
			sourcePos = entity.position().add(1.0, 0.0, 1.0);
		}

		Vec3 away = entity.position().subtract(sourcePos);
		Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);

		if (horizontalAway.lengthSqr() < 0.001) {
			horizontalAway = new Vec3(entity.getRandom().nextDouble() - 0.5, 0.0, entity.getRandom().nextDouble() - 0.5);
		}

		horizontalAway = horizontalAway.normalize();
		Vec3 side = new Vec3(-horizontalAway.z, 0.0, horizontalAway.x).scale(entity.getRandom().nextBoolean() ? 1.0 : -1.0);

		for (int i = 0; i < 16; i++) {
			double distance = 4.5 + entity.getRandom().nextDouble() * 5.5;
			double lift = (entity.getRandom().nextDouble() - 0.3) * 2.0;
			Vec3 candidate = entity.position()
				.add(horizontalAway.scale(distance))
				.add(side.scale((entity.getRandom().nextDouble() - 0.5) * 5.0))
				.add(0.0, lift, 0.0);

			double cy = findTargetGroundY(level, candidate.x, entity.getY(), candidate.z);
			if (isSafeTeleportSpot(level, candidate.x, cy, candidate.z, entity.getY())) {
				playTeleportEffects(level, entity, entity.getX(), entity.getY(), entity.getZ());
				teleportEntity(entity, candidate.x, cy, candidate.z);
				playTeleportEffects(level, entity, candidate.x, cy, candidate.z);
				
				entity.setDeltaMovement(Vec3.ZERO);
				entity.hasImpulse = true;
				return true;
			}
		}
		return false;
	}

	private static double findTargetGroundY(LevelAccessor world, double tx, double referenceY, double tz) {
		if (world instanceof Level level) {
			BlockPos refPos = BlockPos.containing(tx, referenceY + 2, tz);
			boolean inTunnel = !level.getBlockState(refPos).isAir() && level.getBlockState(refPos).isSolid();
			int startY = inTunnel ? (int) Math.floor(referenceY) + 2 : (int) Math.floor(referenceY) + 6;
			int minSearchY = inTunnel ? Math.max(level.getMinBuildHeight(), (int) Math.floor(referenceY) - 4) : Math.max(level.getMinBuildHeight(), (int) Math.floor(referenceY) - 24);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int y = startY; y >= minSearchY; y--) {
				pos.set((int) Math.floor(tx), y, (int) Math.floor(tz));
				BlockState state = level.getBlockState(pos);
				if (!state.isAir() && state.isSolid()) {
					BlockState above1 = level.getBlockState(pos.above(1));
					BlockState above2 = level.getBlockState(pos.above(2));
					if (!above1.isSolid() && !above2.isSolid()) {
						return y + 1.0;
					}
				}
			}
		}
		return referenceY;
	}

	private static boolean trySafeTeleportToGround(LevelAccessor world, Entity self, double targetX, double targetY, double targetZ, String soundId, float vol, float pitch, String cdKey, int cdTicks) {
		double groundY = findTargetGroundY(world, targetX, targetY, targetZ);
		if (Math.abs(groundY - targetY) > 3.0) return false;
		if (!isSafeTeleportSpot(world, targetX, groundY, targetZ, self.getY())) return false;
		if (world instanceof ServerLevel level) {
			playTeleportEffects(level, self, self.getX(), self.getY(), self.getZ());
			teleportEntity(self, targetX, groundY, targetZ);
			playTeleportEffects(level, self, targetX, groundY, targetZ);
		} else {
			teleportEntity(self, targetX, groundY, targetZ);
			playHostileSound(world, targetX, groundY, targetZ, soundId, vol, pitch);
		}
		self.getPersistentData().putDouble(cdKey, cdTicks);
		self.getPersistentData().putDouble(K_ATCPT, 80);
		return true;
	}

	private static boolean isSafeTeleportSpot(LevelAccessor world, double x, double y, double z, double fromY) {
		BlockPos feet = BlockPos.containing(x, y, z);
		BlockPos head = feet.above();
		BlockPos below = feet.below();

		BlockState feetState = world.getBlockState(feet);
		BlockState headState = world.getBlockState(head);
		BlockState belowState = world.getBlockState(below);

		if (!belowState.blocksMotion()) return false;
		if (!feetState.isAir() && !feetState.canBeReplaced()) return false;
		if (!headState.isAir() && !headState.canBeReplaced()) return false;
		return true;
	}

	private static void handleForwardCarveMining(LevelAccessor world, Entity self, Entity target) {
		if (target != null && isTargetPillaring(world, target, self)) {
			double tdx = target.getX() - self.getX();
			double tdz = target.getZ() - self.getZ();
			double distSqXZ = tdx * tdx + tdz * tdz;
			double maxPillarDist = ROT_PILLAR_BACK_OFF_DISTANCE + 2.0;
			if (distSqXZ < maxPillarDist * maxPillarDist) {
				double circleTicks = 0;
				if (self.getPersistentData() != null) {
					circleTicks = self.getPersistentData().getDouble(K_RPCT) + 1.0;
					self.getPersistentData().putDouble(K_RPCT, circleTicks);
				}

				double stateTimer = 0.0;
				double isCircling = 1.0;
				double angle = self.tickCount * 0.05;

				if (self.getPersistentData() != null) {
					if (!self.getPersistentData().contains(K_RPIC)) {
						self.getPersistentData().putDouble(K_RPIC, 1.0);
					}
					if (!self.getPersistentData().contains(K_RPA)) {
						self.getPersistentData().putDouble(K_RPA, angle);
					}
					stateTimer = self.getPersistentData().getDouble(K_RPST);
					isCircling = self.getPersistentData().getDouble(K_RPIC);
					angle = self.getPersistentData().getDouble(K_RPA);
				}

				if (stateTimer <= 0) {
					double randState = self.getRandom().nextDouble();
					if (randState < ROT_PILLAR_CIRCLING_CHANCE) {
						isCircling = 1.0;
						stateTimer = 60.0 + self.getRandom().nextInt(61);
					} else {
						isCircling = 0.0;
						stateTimer = 160.0 + self.getRandom().nextInt(201);
					}
					if (self.getPersistentData() != null) {
						self.getPersistentData().putDouble(K_RPIC, isCircling);
						self.getPersistentData().putDouble(K_RPST, stateTimer);
					}
				} else {
					stateTimer--;
					if (self.getPersistentData() != null) {
						self.getPersistentData().putDouble(K_RPST, stateTimer);
					}
				}

				if (isCircling == 1.0) {
					angle += 0.05;
					if (self.getPersistentData() != null) {
						self.getPersistentData().putDouble(K_RPA, angle);
					}
				}

				double circleRadius = ROT_PILLAR_BACK_OFF_DISTANCE;
				double targetCircleX = target.getX() + Math.cos(angle) * circleRadius;
				double targetCircleZ = target.getZ() + Math.sin(angle) * circleRadius;

				if (self instanceof Mob mob) {
					lockLookAtTarget(mob, target);
					if (isCircling == 1.0) {
						mob.getNavigation().moveTo(targetCircleX, self.getY(), targetCircleZ, ROT_PILLAR_CIRCLING_SPEED);
					} else {
						mob.getNavigation().stop();
					}
				}

				double adaptivePillarDelay = ROT_PILLAR_INITIAL_ATTACK_DELAY;
				if (target instanceof LivingEntity livTarget) {
					if (livTarget.isUsingItem()) adaptivePillarDelay *= 0.1;
					if (livTarget.getHealth() < livTarget.getMaxHealth() * 0.4) adaptivePillarDelay *= 0.25;
				}
				double adaptMult = getAdaptationMultiplier(self);
				if (adaptMult > 1.0) adaptivePillarDelay = Math.max(40.0, adaptivePillarDelay / adaptMult);

				if (circleTicks >= adaptivePillarDelay) {
					double rand = self.getRandom().nextDouble();
					if (self.getPersistentData() != null && rand < ROT_PILLAR_ATTACK_CHANCE) {
						if (rand < (ROT_PILLAR_ATTACK_CHANCE / 2.0) && getRotPersistentBoolean(self, K_UOC, false)) {
							self.getPersistentData().putDouble(K_ROT, OVERHEAD_TOTAL_TICKS);
							self.getPersistentData().putString(K_OTU, target.getUUID().toString());
							self.getPersistentData().putDouble(K_RPCT, 0.0);
						} else if (getRotPersistentBoolean(self, K_UKRC, false) || getRotPersistentBoolean(self, K_UTTC, false) || getRotPersistentBoolean(self, K_UDC, false)) {
							self.setDeltaMovement(0.0, 1.95, 0.0);
							self.hasImpulse = true;
							self.getPersistentData().putDouble(K_SDKP, 1);
							self.getPersistentData().putDouble(K_SDKT, 22);
							self.getPersistentData().putDouble(K_SLT, 0);
							self.getPersistentData().putDouble(K_RPCT, 0.0);
							if (world instanceof ServerLevel level) {
								playHostileSound(level, self.getX(), self.getY(), self.getZ(), "entity.iron_golem.attack", 1.5F, 0.8F);
								level.sendParticles(ParticleTypes.CLOUD, self.getX(), self.getY() + 0.5, self.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
							}
						}
					}
				}
				return;
			}
		}

		if (self.getPersistentData() != null && self.getPersistentData().contains(K_RPCT)) {
			self.getPersistentData().putDouble(K_RPCT, 0.0);
		}

		LivingEntity foundPlayer = target instanceof LivingEntity liv ? liv : null;
		double heightDiff = target != null ? target.getY() - self.getY() : 0;
		double horizontalDist = target != null ? Math.sqrt(self.distanceToSqr(target.getX(), self.getY(), target.getZ())) : 999;

		Vec3 selfEyes = self.getEyePosition(1f);
		Vec3 selfView = self.getViewVector(1f);
		Vec3 blockCheckTarget = selfEyes.add(selfView.scale(MINE_RAY_DISTANCE));

		HitResult hit = world.clip(new ClipContext(selfEyes, blockCheckTarget, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));

		BlockPos facePos;
		BlockPos feetPos;
		BlockPos headPos;

		if (hit.getType() == HitResult.Type.BLOCK) {
			facePos = ((BlockHitResult) hit).getBlockPos();
			feetPos = new BlockPos(facePos.getX(), Mth.floor(self.getY()), facePos.getZ());
			headPos = new BlockPos(facePos.getX(), Mth.floor(self.getY() + 2), facePos.getZ());
		} else {
			Vec3 look = self.getLookAngle().normalize();
			int fx = Mth.floor(self.getX() + look.x);
			int fz = Mth.floor(self.getZ() + look.z);
			feetPos = new BlockPos(fx, Mth.floor(self.getY()), fz);
			facePos = new BlockPos(fx, Mth.floor(self.getEyeY()), fz);
			headPos = new BlockPos(fx, Mth.floor(self.getY() + 2), fz);
		}

		boolean canMineFeet = canMine(world, feetPos, foundPlayer);
		boolean canMineFace = canMine(world, facePos, foundPlayer);
		boolean canMineHead = canMine(world, headPos, foundPlayer);

		BlockPos downPos = self.blockPosition().below();
		boolean canMineDown = (heightDiff <= -2.0 && horizontalDist <= 3.0) && canMine(world, downPos, foundPlayer);

		int prevProgress = self instanceof RotEntity rot ? rot.getEntityData().get(RotEntity.DATA_mineProgress) : 0;
		int prevX = self.getPersistentData() != null ? self.getPersistentData().getInt(K_RMX) : 0;
		int prevY = self.getPersistentData() != null ? self.getPersistentData().getInt(K_RMY) : 0;
		int prevZ = self.getPersistentData() != null ? self.getPersistentData().getInt(K_RMZ) : 0;
		BlockPos prevTrackPos = new BlockPos(prevX, prevY, prevZ);

		BlockPos trackPos = canMineDown ? downPos : (canMineFeet ? feetPos : (canMineFace ? facePos : headPos));
		if (prevProgress > 0 && trackPos != null && !trackPos.equals(prevTrackPos)) {
			boolean lockPrev = canMine(world, prevTrackPos, foundPlayer) && prevTrackPos.closerToCenterThan(self.position(), 4.0) && !isPositionClaimed(world, prevTrackPos, self);
			if (lockPrev) {
				trackPos = prevTrackPos;
				canMineDown = trackPos.equals(downPos);
				canMineFeet = trackPos.equals(feetPos);
				canMineFace = trackPos.equals(facePos);
				canMineHead = trackPos.equals(headPos);
				if (!canMineDown && !canMineFeet && !canMineFace && !canMineHead) {
					if (trackPos.getY() < self.getY()) { canMineDown = true; downPos = trackPos; }
					else if (trackPos.getY() == Mth.floor(self.getY())) { canMineFeet = true; feetPos = trackPos; }
					else if (trackPos.getY() == Mth.floor(self.getY() + 1)) { canMineFace = true; facePos = trackPos; }
					else { canMineHead = true; headPos = trackPos; }
				}
			} else {
				prevProgress = 0;
			}
		}

		if (canMineDown && isPositionClaimed(world, downPos, self)) canMineDown = false;
		if (canMineFeet && isPositionClaimed(world, feetPos, self)) canMineFeet = false;
		if (canMineFace && isPositionClaimed(world, facePos, self)) canMineFace = false;
		if (canMineHead && isPositionClaimed(world, headPos, self)) canMineHead = false;

		double curX = self.getX(), curY = self.getY(), curZ = self.getZ();
		double lastX = getRotPersistentDouble(self, K_RLX, curX);
		double lastZ = getRotPersistentDouble(self, K_RLZ, curZ);
		double horizDistMoved = Math.sqrt((curX - lastX) * (curX - lastX) + (curZ - lastZ) * (curZ - lastZ));

		setRotPersistentDouble(self, K_RLX, curX);
		setRotPersistentDouble(self, K_RLZ, curZ);

		int curMineProg = self instanceof RotEntity rot ? rot.getEntityData().get(RotEntity.DATA_mineProgress) : 0;
		int lastMineProg = getRotPersistentInt(self, K_RLMP, curMineProg);
		int mineProgressDelta = curMineProg - lastMineProg;
		setRotPersistentInt(self, K_RLMP, curMineProg);

		int ticksNoMove = getRotPersistentInt(self, K_RTNM, 0);
		int ticksNoMine = getRotPersistentInt(self, K_RTNM2, 0);

		if (horizDistMoved > 0.05) ticksNoMove = 0; else ticksNoMove++;
		if (mineProgressDelta > 0) ticksNoMine = 0; else ticksNoMine++;

		setRotPersistentInt(self, K_RTNM, ticksNoMove);
		setRotPersistentInt(self, K_RTNM2, ticksNoMine);

		boolean isActivelyMining = mineProgressDelta > 0 || (curMineProg > 0 && ticksNoMine < 10);
		boolean isStuckNow = (ticksNoMove > 20) && !isActivelyMining;

		int stuckTier = getRotPersistentInt(self, K_RST, 0);
		int stuckTierTicks = getRotPersistentInt(self, K_RSTT, 0);

		if (horizDistMoved > 0.1 || mineProgressDelta > 0) {
			if (stuckTier > 0) {
				String env = CombatProfile.analyzeEnvironment(world, self, foundPlayer);
				String winKey = "rot_stuck_wins_" + env + "_" + stuckTier;
				setRotPersistentInt(self, winKey, getRotPersistentInt(self, winKey, 0) + 1);
			}
			stuckTier = 0;
			stuckTierTicks = 0;
			setRotPersistentInt(self, K_RST, 0);
			setRotPersistentInt(self, K_RSTT, 0);
		} else if (isStuckNow) {
			stuckTierTicks++;
			setRotPersistentInt(self, K_RSTT, stuckTierTicks);

			String cause = "PATH_BLOCKED";
			if (target != null && Math.abs(heightDiff) >= 2.0) {
				cause = "VERTICAL_GAP";
			} else if (canMineFace && world.getBlockState(facePos).getDestroySpeed(world, facePos) >= MAX_BREAKABLE_HARDNESS) {
				cause = "UNBREAKABLE_BLOCK";
			} else if (isPositionClaimed(world, facePos, self) || isPositionClaimed(world, feetPos, self)) {
				cause = "CLAIM_CONTESTED";
			}
			setRotPersistentString(self, "rot_stuck_cause", cause);

			if (stuckTierTicks > 30) {
				stuckTier = Math.min(3, stuckTier + 1);
				stuckTierTicks = 0;
				setRotPersistentInt(self, K_RST, stuckTier);
				setRotPersistentInt(self, K_RSTT, 0);
			}

			if (stuckTier == 0) {
				BlockPos widenUp = facePos.above();
				BlockPos widenL = facePos.west();
				BlockPos widenR = facePos.east();
				if (canMine(world, widenUp, foundPlayer)) world.destroyBlock(widenUp, false);
				if (canMine(world, widenL, foundPlayer)) world.destroyBlock(widenL, false);
				if (canMine(world, widenR, foundPlayer)) world.destroyBlock(widenR, false);
			} else if (stuckTier == 1) {
				if (self instanceof Mob mob && target != null) {
					mob.getNavigation().moveTo(target, 1.25);
				}
			} else if (stuckTier == 2) {
				boolean tpUnlocked = getRotPersistentBoolean(self, K_UT, false);
				if (tpUnlocked && target != null && self instanceof LivingEntity) {
					double dx = target.getX() - self.getX();
					double dz = target.getZ() - self.getZ();
					double dist = Math.sqrt(dx * dx + dz * dz);
					if (dist > 1.0) {
						double stepX = self.getX() + (dx / dist) * Math.min(dist, 4.0);
						double stepZ = self.getZ() + (dz / dist) * Math.min(dist, 4.0);
						int groundY = Mth.floor(target.getY());
						BlockPos tpPos = new BlockPos(Mth.floor(stepX), groundY, Mth.floor(stepZ));
						if (world.getBlockState(tpPos).isAir() && world.getBlockState(tpPos.above()).isAir()) {
							teleportEntity(self, stepX + 0.5, groundY, stepZ + 0.5);
							setRotPersistentInt(self, K_RTNM, 0);
							setRotPersistentInt(self, K_RTNM2, 0);
						}
					}
				} else if (!tpUnlocked && target != null) {
					setRotPersistentInt(self, K_RDTI, target.getId());
					setRotPersistentInt(self, K_RDT, 200);
					if (self instanceof Mob mob) mob.setTarget(null);
					setRotPersistentInt(self, K_RST, 0);
					setRotPersistentInt(self, K_RSTT, 0);
				}
			} else if (stuckTier == 3) {
				if (target != null) {
					setRotPersistentInt(self, K_RDTI, target.getId());
					setRotPersistentInt(self, K_RDT, 200);
					if (self instanceof Mob mob) mob.setTarget(null);
				}
				setRotPersistentInt(self, K_RST, 0);
				setRotPersistentInt(self, K_RSTT, 0);
			}
		}

		if (canMineFeet || canMineFace || canMineHead || canMineDown) {
			int mineProgress = prevProgress + 1;
			if (self instanceof RotEntity rotSet) {
				rotSet.getEntityData().set(RotEntity.DATA_mineProgress, mineProgress);
			}
			if (self.getPersistentData() != null) {
				self.getPersistentData().putInt(K_RMX, trackPos.getX());
				self.getPersistentData().putInt(K_RMY, trackPos.getY());
				self.getPersistentData().putInt(K_RMZ, trackPos.getZ());
			}

			if (self.tickCount % 10 == 0 && self instanceof LivingEntity liv) {
				liv.swing(InteractionHand.MAIN_HAND);
				if (world instanceof ServerLevel level) {
					level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, world.getBlockState(trackPos)), trackPos.getX() + 0.5, trackPos.getY() + 0.5, trackPos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.1);
				}
				boolean lastHandLeft = false;
				if (self.getPersistentData() != null) {
					lastHandLeft = getRotPersistentBoolean(self, K_SPHT, false);
					self.getPersistentData().putBoolean(K_SPHT, !lastHandLeft);
					if (lastHandLeft) {
						self.getPersistentData().putDouble(K_SLPT, 8);
						self.getPersistentData().putDouble(K_SRPT2, 0);
					} else {
						self.getPersistentData().putDouble(K_SRPT2, 8);
						self.getPersistentData().putDouble(K_SLPT, 0);
					}
				}
			}

			float speedRef;
			if (canMineDown) {
				speedRef = world.getBlockState(downPos).getDestroySpeed(world, downPos);
			} else if (canMineFeet) {
				speedRef = world.getBlockState(feetPos).getDestroySpeed(world, feetPos);
			} else if (canMineFace) {
				speedRef = world.getBlockState(facePos).getDestroySpeed(world, facePos);
			} else {
				speedRef = world.getBlockState(headPos).getDestroySpeed(world, headPos);
			}

			float mineThreshold = speedRef * MINE_SPEED_MULTIPLIER + MINE_SPEED_BASE;

			if (mineProgress > mineThreshold) {
				if (canMineDown) world.destroyBlock(downPos, false);
				if (canMineFeet) world.destroyBlock(feetPos, false);
				if (canMineFace) world.destroyBlock(facePos, false);
				if (canMineHead) world.destroyBlock(headPos, false);
				if (self instanceof Mob mob) mob.getNavigation().stop();
				if (self instanceof RotEntity rotSet) {
					rotSet.getEntityData().set(RotEntity.DATA_mineProgress, 0);
				}
			}
		} else {
			if (self instanceof RotEntity rotSet) {
				rotSet.getEntityData().set(RotEntity.DATA_mineProgress, 0);
			}
		}
	}

	private static boolean isPositionClaimed(LevelAccessor world, BlockPos pos, Entity thisEntity) {
		net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(pos).inflate(3);
		java.util.List<RotEntity> others = world.getEntitiesOfClass(RotEntity.class, box, e -> e != thisEntity && e.isAlive());
		int ourProgress = thisEntity instanceof RotEntity rot ? rot.getEntityData().get(RotEntity.DATA_mineProgress) : 0;
		for (RotEntity other : others) {
			int otherProgress = other.getEntityData().get(RotEntity.DATA_mineProgress);
			int otherX = other.getPersistentData() != null ? other.getPersistentData().getInt(K_RMX) : 0;
			int otherY = other.getPersistentData() != null ? other.getPersistentData().getInt(K_RMY) : 0;
			int otherZ = other.getPersistentData() != null ? other.getPersistentData().getInt(K_RMZ) : 0;
			if (otherProgress > 0 && otherX == pos.getX() && otherY == pos.getY() && otherZ == pos.getZ()) {
				if (ourProgress == 0) return true;
				if (otherProgress > ourProgress) return true;
				if (otherProgress == ourProgress && other.getId() < thisEntity.getId()) return true;
			}
		}
		return false;
	}

	private static boolean canMine(LevelAccessor world, BlockPos pos, LivingEntity player) {
		BlockState state = world.getBlockState(pos);
		if (state.isAir()) return false;
		if (state.getCollisionShape(world, pos).isEmpty()) return false;
		float speed = state.getDestroySpeed(world, pos);
		if (speed < 0 || speed >= MAX_BREAKABLE_HARDNESS) return false;
		if (player != null && pos.getY() == (int) (player.getY() - 2)) return false;
		return true;
	}

	private static boolean isDoingCombo(Entity entity) {
		return entity.getPersistentData().getDouble(K_SCAT) > 0
			|| entity.getPersistentData().getDouble(K_SSWST) > 0
			|| entity.getPersistentData().getDouble(K_SJT) > 0
			|| entity.getPersistentData().getDouble(K_SRHT) > 0
			|| entity.getPersistentData().getDouble(K_SMT) > 0
			|| entity.getPersistentData().getDouble(K_SMS) > 0
			|| entity.getPersistentData().getDouble(K_SCS2) > 0
			|| entity.getPersistentData().getDouble(K_SCS) > 0
			|| entity.getPersistentData().getDouble(K_SCS3) > 0
			|| entity.getPersistentData().getDouble(K_SCS4) > 0
			|| entity.getPersistentData().getDouble(K_SCS5) > 0;
	}

	private static void checkTrenchAndJump(LevelAccessor world, Entity self, Entity target) {
		if (!(self instanceof LivingEntity living) || !living.onGround()) {
			return;
		}
		if (target == null) {
			return;
		}

		double dx = target.getX() - self.getX();
		double dz = target.getZ() - self.getZ();
		double distXZ = Math.sqrt(dx * dx + dz * dz);
		if (distXZ < 0.1) {
			return;
		}
		double dirX = dx / distXZ;
		double dirZ = dz / distXZ;

		boolean hasTrench = false;
		double[] checkDistances = {1.2, 2.2};
		for (double d : checkDistances) {
			double cx = self.getX() + dirX * d;
			double cz = self.getZ() + dirZ * d;
			net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(cx, self.getY(), cz);
			
			if (world.getBlockState(pos).isAir() && world.getBlockState(pos.below()).isAir()) {
				int depth = 0;
				net.minecraft.core.BlockPos tracePos = pos.below();
				while (depth < 5 && world.getBlockState(tracePos).isAir()) {
					tracePos = tracePos.below();
					depth++;
				}
				if (depth >= 2) {
					hasTrench = true;
					break;
				}
			}
		}

		if (hasTrench) {
			Vec3 motion = self.getDeltaMovement();
			double jumpY = 0.52;
			double jumpForward = 0.35;
			self.setDeltaMovement(motion.x + dirX * jumpForward, jumpY, motion.z + dirZ * jumpForward);
			living.setJumping(true);
			self.hasImpulse = true;
			
			if (world instanceof ServerLevel level) {
				net.minecraft.core.BlockPos belowPos = net.minecraft.core.BlockPos.containing(self.getX(), self.getY() - 0.5, self.getZ());
				net.minecraft.world.level.block.state.BlockState floorState = level.getBlockState(belowPos);
				if (!floorState.isAir()) {
					net.minecraft.core.particles.BlockParticleOption dust = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.DUST_PILLAR, floorState);
					level.sendParticles(dust, self.getX(), self.getY() + 0.1, self.getZ(), 8, 0.25, 0.1, 0.25, 0.05);
				}
			}
		} else if (target.getY() > self.getY() + 1.5) {
			double cx = self.getX() + dirX * 1.2;
			double cz = self.getZ() + dirZ * 1.2;
			net.minecraft.core.BlockPos posAhead = net.minecraft.core.BlockPos.containing(cx, self.getY(), cz);
			if (!world.getBlockState(posAhead).isAir() || !world.getBlockState(posAhead.above()).isAir()) {
				Vec3 motion = self.getDeltaMovement();
				double jumpY = 0.52;
				self.setDeltaMovement(motion.x + dirX * 0.15, jumpY, motion.z + dirZ * 0.15);
				living.setJumping(true);
				self.hasImpulse = true;
			}
		}
	}

	private static void handlePassengerAndGrowth(Entity entity) {
		if (entity.isPassenger()) entity.stopRiding();

		entity.getPersistentData().putDouble(K_AGE, entity.getPersistentData().getDouble(K_AGE) + 1);
		if (entity.getPersistentData().getDouble(K_AGE) % 1200 == 0) {
			if (entity instanceof LivingEntity living && living.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
				living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(living.getAttribute(Attributes.MAX_HEALTH).getBaseValue() + 2);
				living.setHealth(living.getHealth() + 2);
			}
		}
	}

	private static final String[] ROT_COOLDOWN_KEYS = {
		K_TP_DODGE_CD, K_TP_FLANK_CD, K_SOLAR_CD, K_ADAPT_CD, K_GRAPPLE_CD, K_TK_CD,
		K_RWDC, "sentinel_wither_skull_cd", K_SFIC,
		K_SWSC, K_SGAC, K_SST,
		K_SUC, K_SUAT, K_SLCT,
		K_SLT, K_SRHT, K_SMC,
		K_SMW, K_SLPT, K_SRPT2,
		K_SHLPT, K_SHRPT, K_ROT,
		K_SSWST, K_SJT, "sentinel_omni_sonic_cooldown",
		K_SOSCT, K_SCAT, K_SMT,
		K_SMWT, K_SCT3, K_SCC, K_SCT2,
		K_SCC2, K_SCT4, K_SCC3, K_SCT5,
		K_SCC4, K_SCT6, K_SCC5, K_SSST,
		K_SSSC, K_RARC, K_RBC,
		K_SEPC, K_SDCC, K_RPC,
		K_RPT, K_RSC2, K_RSC, K_RSA
	};

	private static void tickCooldowns(Entity e) {
		boolean totemAccelerated = getRotPersistentBoolean(e, K_STA2, false) && !getRotPersistentBoolean(e, K_SIIT, false);
		for (String key : ROT_COOLDOWN_KEYS) {
			if (e.getPersistentData().contains(key)) tickCooldown(e, key, 1, totemAccelerated);
		}
	}

	private static void tickCooldown(Entity e, String key, int step) {
		boolean totemAccelerated = getRotPersistentBoolean(e, K_STA2, false) && !getRotPersistentBoolean(e, K_SIIT, false);
		tickCooldown(e, key, step, totemAccelerated);
	}

	private static void tickCooldown(Entity e, String key, int step, boolean totemAccelerated) {
		double v = e.getPersistentData().getDouble(key);
		if (v > 0.0) {
			double finalStep = step;
			if (totemAccelerated && (key.endsWith("cd") || key.contains("_cd") || key.contains("cooldown") || key.equals(K_SOLAR_CD))) {
				finalStep = step * 2.0;
			}
			if (COOLDOWN_MULTIPLIER > 0 && (key.endsWith("cd") || key.contains("_cd") || key.contains("cooldown") || key.equals(K_SOLAR_CD))) {
				finalStep = finalStep / COOLDOWN_MULTIPLIER;
			}
			double next = Math.max(0.0, v - finalStep);
			if (next != v) {
				if (next > 0.0) e.getPersistentData().putDouble(key, next);
				else e.getPersistentData().remove(key);
			}
		}
	}

	private static void playHostileSound(LevelAccessor world, double x, double y, double z, String soundId, float volume, float pitch) {
		if (!(world instanceof Level level)) return;
		net.minecraft.sounds.SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(soundId));
		if (sound != null) {
			if (!level.isClientSide()) {
				level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.HOSTILE, volume, pitch);
			} else {
				level.playLocalSound(x, y, z, sound, SoundSource.HOSTILE, volume, pitch, false);
			}
		}
	}

	private static void stopHostileSound(LevelAccessor world, double x, double y, double z, String soundId, double range) {
		if (world instanceof ServerLevel level) {
			net.minecraft.network.protocol.game.ClientboundStopSoundPacket packet = new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(ResourceLocation.parse(soundId), SoundSource.HOSTILE);
			for (ServerPlayer player : level.getPlayers(p -> p.position().distanceToSqr(x, y, z) <= range * range)) {
				player.connection.send(packet);
			}
		}
	}

	private static void applyKnockbackAndSync(Entity victim, double vx, double vy, double vz) {
		if (!(victim instanceof Player)) {
			vx *= 1.6;
			vz *= 1.6;
			vy *= 1.2;
		}
		victim.setDeltaMovement(vx, vy, vz);
		victim.hasImpulse = true;
		victim.hurtMarked = true;
		if (victim instanceof ServerPlayer sp) {
			sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
		}
	}

	private static void teleportEntity(Entity ent, double x, double y, double z) {
		double startX = ent.getX();
		double startY = ent.getY();
		double startZ = ent.getZ();
		
		if (ent.level() instanceof ServerLevel level) {
			if (ent.distanceToSqr(x, y, z) >= 400.0) {
				level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, startX, startY + ent.getBbHeight() / 2.0, startZ, 1, 0, 0, 0, 0);
			}
		}
		
		ent.teleportTo(x, y, z);
		if (ent instanceof Mob mob) {
			mob.getNavigation().recomputePath();
		}
		if (ent instanceof ServerPlayer sp) {
			sp.connection.teleport(x, y, z, ent.getYRot(), ent.getXRot());
		}
		if (ent.level() instanceof ServerLevel level) {
			spawnTeleportTrail(level, startX, startY, startZ, x, y, z);
		}
	}

	private static void spawnTeleportTrail(ServerLevel level, double startX, double startY, double startZ, double targetX, double targetY, double targetZ) {
		Vec3 start = new Vec3(startX, startY, startZ);
		Vec3 end = new Vec3(targetX, targetY, targetZ);
		double distance = start.distanceTo(end);
		if (distance < 1.0) return;

		int steps = (int) Math.ceil(distance * 1.5);
		for (int i = 0; i <= steps; i++) {
			double pct = (double) i / steps;
			double px = startX + (targetX - startX) * pct;
			double pz = startZ + (targetZ - startZ) * pct;
			double py = startY + (targetY - startY) * pct;

			BlockPos checkPos = BlockPos.containing(px, py + 1.0, pz);
			BlockPos groundPos = checkPos;
			boolean foundGround = false;
			for (int dy = 2; dy >= -5; dy--) {
				BlockPos bp = checkPos.above(dy);
				if (!level.getBlockState(bp).isAir() && level.getBlockState(bp).getFluidState().isEmpty() && level.getBlockState(bp).blocksMotion()) {
					groundPos = bp;
					foundGround = true;
					break;
				}
			}

			double spawnY = foundGround ? (groundPos.getY() + 1.0) : py;
			BlockState state = foundGround ? level.getBlockState(groundPos) : net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();

			if (!state.isAir()) {
				try {
					level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state), px, spawnY + 0.1, pz, 4, 0.1, 0.1, 0.1, 0.15);
				} catch (Exception ignored) {}
			}
		}
	}

	private static Entity findEntityInWorldRange(LevelAccessor world, Class<? extends Entity> clazz, double x, double y, double z, double range, Entity self) {
		AABB searchBox = AABB.ofSize(new Vec3(x, y, z), range, range, range);
		java.util.Set<Integer> occupiedTargetIds = new java.util.HashSet<>();
		try {
			for (RotEntity other : world.getEntitiesOfClass(RotEntity.class, searchBox, e -> e != self)) {
				if (other instanceof Mob otherMob && otherMob.getTarget() != null) {
					occupiedTargetIds.add(otherMob.getTarget().getId());
				}
			}
		} catch (Exception e) {}

		return world.getEntitiesOfClass(clazz, searchBox, e -> isValidTarget(e, self))
				.stream()
				.sorted((e1, e2) -> {
					if (e1 instanceof LivingEntity l1 && e2 instanceof LivingEntity l2) {
						boolean occ1 = occupiedTargetIds.contains(l1.getId());
						boolean occ2 = occupiedTargetIds.contains(l2.getId());
						if (occ1 != occ2) {
							return occ1 ? 1 : -1;
						}
						boolean tpUnlocked = getRotPersistentBoolean(self, K_UT, false);
						if (!tpUnlocked && self instanceof LivingEntity ls) {
							boolean los1 = ls.hasLineOfSight(l1);
							boolean los2 = ls.hasLineOfSight(l2);
							if (los1 != los2) {
								return los1 ? -1 : 1;
							}
						}
						float hp1 = l1.getHealth();
						float hp2 = l2.getHealth();
						if (Math.abs(hp1 - hp2) > 0.05f) {
							return Float.compare(hp1, hp2);
						}
					}
					return Double.compare(e1.distanceToSqr(x, y, z), e2.distanceToSqr(x, y, z));
				})
				.findFirst()
				.orElse(null);
	}

	public static boolean isChannelingAbility(Entity entity) {
		if (entity == null) return false;
		double solarCharge = entity.getPersistentData().getDouble(K_SSCT);
		double solarFire = entity.getPersistentData().getDouble(K_SSFT);
		double cryoCharge = entity.getPersistentData().getDouble(K_SCCT);
		double cryoFire = entity.getPersistentData().getDouble(K_SCFT);
		double grappleTicks = entity.getPersistentData().getDouble("sentinel_grapple_ticks");
		double tkTicks = entity.getPersistentData().getDouble("sentinel_tk_ticks");
		double sonicTicks = entity.getPersistentData().getDouble(K_SST);
		double closingTicks = entity.getPersistentData().getDouble(K_SLCT);
		double skyWarp = entity.getPersistentData().getDouble(K_SSWST);
		double judgment = entity.getPersistentData().getDouble(K_SJT);
		double riderHold = entity.getPersistentData().getDouble(K_SRHT);
		double omniSonic = entity.getPersistentData().getDouble(K_SOSCT);
		double sonicScream = entity.getPersistentData().getDouble(K_SSST);
		double armorRipTicks = entity.getPersistentData().getDouble(K_RART);
		double blockTicks = entity.getPersistentData().getDouble(K_RBAT);
		boolean isUppercutting = getRotPersistentBoolean(entity, K_IU, false);
		double superheatCharging = entity.getPersistentData().getDouble(K_RSC);
		double superheatActive = entity.getPersistentData().getDouble(K_RSA);
		return solarCharge > 0 || solarFire > 0 || cryoCharge > 0 || cryoFire > 0 || grappleTicks > 0 || tkTicks > 0 || sonicTicks > 0 || closingTicks > 0 || skyWarp > 0 || judgment > 0 || riderHold > 0 || omniSonic > 0 || sonicScream > 0 || armorRipTicks > 0 || blockTicks > 0 || isUppercutting || superheatCharging > 0 || superheatActive > 0 || isDoingCombo(entity);
	}

	private static void syncNBTFlags(Entity entity) {
		double solarCharge = entity.getPersistentData().getDouble(K_SSCT);
		double cryoCharge = entity.getPersistentData().getDouble(K_SCCT);
		double solarFire = entity.getPersistentData().getDouble(K_SSFT);
		double cryoFire = entity.getPersistentData().getDouble(K_SCFT);
		double closingTicks = entity.getPersistentData().getDouble(K_SLCT);
		double sonicTicks = entity.getPersistentData().getDouble(K_SST);
		double sonicCooldown = entity.getPersistentData().getDouble(K_SWSC);
		double landingTicks = entity.getPersistentData().getDouble(K_SLT);

		double leftPunchTicks = entity.getPersistentData().getDouble(K_SLPT);
		double rightPunchTicks = entity.getPersistentData().getDouble(K_SRPT2);
		double heavyLeftPunchTicks = entity.getPersistentData().getDouble(K_SHLPT);
		double heavyRightPunchTicks = entity.getPersistentData().getDouble(K_SHRPT);
		double overheadTicks = entity.getPersistentData().getDouble(K_ROT);
		if (getRotPersistentBoolean(entity, K_DFO, false)) {
			if (overheadTicks <= 1) {
				entity.getPersistentData().putDouble(K_ROT, OVERHEAD_TOTAL_TICKS);
				overheadTicks = OVERHEAD_TOTAL_TICKS;
			}
		}

		double uppercutLeftTicks = entity.getPersistentData().getDouble(K_DULT);
		if (getRotPersistentBoolean(entity, K_DFUL, false)) {
			if (uppercutLeftTicks <= 1) {
				entity.getPersistentData().putDouble(K_DULT, 40.0);
				uppercutLeftTicks = 40.0;
			} else {
				entity.getPersistentData().putDouble(K_DULT, uppercutLeftTicks - 1);
			}
		}

		double uppercutRightTicks = entity.getPersistentData().getDouble(K_DURT);
		if (getRotPersistentBoolean(entity, K_DFUR, false)) {
			if (uppercutRightTicks <= 1) {
				entity.getPersistentData().putDouble(K_DURT, 40.0);
				uppercutRightTicks = 40.0;
			} else {
				entity.getPersistentData().putDouble(K_DURT, uppercutRightTicks - 1);
			}
		}
		double slamPhase = entity.getPersistentData().getDouble(K_SSP);
		double skyWarpTicks = entity.getPersistentData().getDouble(K_SSWST);

		boolean leftPunching = leftPunchTicks > 0;
		boolean rightPunching = rightPunchTicks > 0;
		boolean isHeavyLeftPunching = heavyLeftPunchTicks > 0 || getRotPersistentBoolean(entity, "debug_force_heavy_left", false);
		boolean isHeavyRightPunching = heavyRightPunchTicks > 0 || getRotPersistentBoolean(entity, "debug_force_heavy_right", false);
		boolean overheadActive = overheadTicks > 0;
		boolean inSlamCharge = slamPhase == 2;

		boolean isAirborne = false;
		boolean isHeavyFalling = false;
		if (!entity.onGround() && !entity.isInWater() && !entity.isInLava()) {
			if (entity.getDeltaMovement().y() > 0.4) {
				isAirborne = true;
			}
			if (entity.getDeltaMovement().y() < -0.15 && entity.fallDistance > 3.0F) {
				isHeavyFalling = true;
			}
		}

		if (slamPhase == 1 || skyWarpTicks > 0) {
			isAirborne = true;
		} else if (slamPhase == 2) {
			isAirborne = false;
		} else if (slamPhase == 3) {
			isAirborne = false;
		}

		double dieKickPhase = entity.getPersistentData().getDouble(K_SDKP);
		if (dieKickPhase == 1) {
			isAirborne = true;
		} else if (dieKickPhase == 2) {
			isAirborne = true;
		} else if (dieKickPhase == 3) {
			isAirborne = false;
		}

		double judgmentTicks = entity.getPersistentData().getDouble(K_SJT);
		double sonicScreamTicks = entity.getPersistentData().getDouble(K_SSST);
		double witherSkullFire = getRotPersistentDouble(entity, K_SWSFT, 0.0);
		boolean charging = solarCharge > 0 || cryoCharge > 0 || (sonicScreamTicks > 200.0) || (witherSkullFire > 9.0);
		boolean firing = solarFire > 0 || cryoFire > 0 || (sonicScreamTicks > 20.0 && sonicScreamTicks <= 200.0) || (witherSkullFire > 0.0 && witherSkullFire <= 9.0);
		boolean closing = (closingTicks > 0 && !firing && !charging) || (sonicScreamTicks > 0.0 && sonicScreamTicks <= 20.0);
		boolean sonic = sonicTicks > 0;
		boolean landing = landingTicks > 0;
		boolean isGroundCrushing = slamPhase == 3 || skyWarpTicks > 0 || (landingTicks > 0 && getRotPersistentBoolean(entity, K_SISL, false));
		boolean isDropkickCharging = judgmentTicks > 20;

		entity.getPersistentData().putBoolean(K_LC, charging);
		entity.getPersistentData().putBoolean(K_ILC, charging);
		entity.getPersistentData().putBoolean(K_LF, firing);
		entity.getPersistentData().putBoolean(K_ILF, firing);
		entity.getPersistentData().putBoolean(K_LC2, closing);
		entity.getPersistentData().putBoolean(K_ILC2, closing);
		entity.getPersistentData().putBoolean(K_SBA, sonic);
		entity.getPersistentData().putBoolean("is_airborne", isAirborne);
		entity.getPersistentData().putBoolean(K_SIAS, isAirborne);
		entity.getPersistentData().putBoolean("is_landing", landing);
 
		entity.getPersistentData().putBoolean(K_ILP, leftPunching);
		entity.getPersistentData().putBoolean(K_IRP, rightPunching);
		entity.getPersistentData().putBoolean("is_heavy_left_punching", isHeavyLeftPunching);
		entity.getPersistentData().putBoolean("is_heavy_right_punching", isHeavyRightPunching);
		entity.getPersistentData().putBoolean(K_IO, overheadTicks > 0);
		entity.getPersistentData().putBoolean(K_ISC, inSlamCharge);
		entity.getPersistentData().putBoolean(K_IAT, isAirborne);
		entity.getPersistentData().putBoolean(K_IOP, overheadTicks >= OVERHEAD_PREP_THRESHOLD);
		entity.getPersistentData().putBoolean(K_IGC, isGroundCrushing);
		entity.getPersistentData().putBoolean(K_IDC, isDropkickCharging);
		entity.getPersistentData().putBoolean(K_IAS, isAirborne);
 
		if (entity instanceof RotEntity rot) {
			try {
				rot.getEntityData().set(RotEntity.DATA_sentinel_solar_charge_ticks, (int) solarCharge);
				rot.getEntityData().set(RotEntity.DATA_sentinel_cryo_charge_ticks, (int) cryoCharge);
				rot.getEntityData().set(RotEntity.DATA_is_laser_firing, firing || charging);
			} catch (Exception e) {}
			rot.getEntityData().set(RotEntity.DATA_is_left_punching, leftPunching);

			rot.getEntityData().set(RotEntity.DATA_is_right_punching, rightPunching);

			rot.getEntityData().set(RotEntity.DATA_is_heavy_left_punching, isHeavyLeftPunching);

			rot.getEntityData().set(RotEntity.DATA_is_heavy_right_punching, isHeavyRightPunching);

			rot.getEntityData().set(RotEntity.DATA_is_laser_closing, closing);

			rot.getEntityData().set(RotEntity.DATA_is_airborne_state, isAirborne);

			rot.getEntityData().set(RotEntity.DATA_is_overhead_preparing, overheadTicks >= OVERHEAD_PREP_THRESHOLD);

			rot.getEntityData().set(RotEntity.DATA_is_overhead, overheadTicks > 0);

			rot.getEntityData().set(RotEntity.DATA_is_slam_charge, inSlamCharge);

			rot.getEntityData().set(RotEntity.DATA_is_ground_crushing, isGroundCrushing);

			boolean forceFall = getRotPersistentBoolean(entity, "debug_force_fall", false);
			rot.getEntityData().set(RotEntity.DATA_is_falling_heavy, forceFall || isHeavyFalling);

			boolean forceRider = getRotPersistentBoolean(entity, K_DFR, false);
			rot.getEntityData().set(RotEntity.DATA_is_rider_charging, forceRider || (judgmentTicks == 20));

			double riderHoldTicks = entity.getPersistentData().getDouble(K_SRHT);
			rot.getEntityData().set(RotEntity.DATA_is_rider_kick, dieKickPhase == 3 || dieKickPhase == 4 || (judgmentTicks > 1 && judgmentTicks < 20) || riderHoldTicks > 0);

			boolean forceSonic = getRotPersistentBoolean(entity, "debug_force_sonic", false);
			rot.getEntityData().set(RotEntity.DATA_is_sonic_boom, forceSonic || sonic);

			boolean sonicLarge = getRotPersistentBoolean(entity, K_ISBL, false);
			rot.getEntityData().set(RotEntity.DATA_is_sonic_boom_large, sonicLarge);

			boolean armorRipping = getRotPersistentBoolean(entity, K_IAR, false);
			rot.getEntityData().set(RotEntity.DATA_is_armor_ripping, armorRipping);

			boolean isBlocking = getRotPersistentBoolean(entity, K_IB, false);
			rot.getEntityData().set(RotEntity.DATA_is_blocking, isBlocking);

			boolean isBlockingFinish = getRotPersistentBoolean(entity, K_IBF, false);
			rot.getEntityData().set(RotEntity.DATA_is_blocking_finish, isBlockingFinish);

			double uppercutAnim = entity.getPersistentData().getDouble(K_SUAT);
			double cc2Stage = entity.getPersistentData().getDouble(K_SCS);
			boolean isUppercuttingFlag = getRotPersistentBoolean(entity, K_IU, false);
			if (uppercutAnim <= 0 && (cc2Stage == 0 || !isUppercuttingFlag)) {
				entity.getPersistentData().putBoolean(K_IU, false);
				entity.getPersistentData().putBoolean(K_IUL, false);
				entity.getPersistentData().putBoolean(K_IUR, false);
				entity.getPersistentData().putBoolean(K_IUS, false);
			}
			boolean isUppercutting = getRotPersistentBoolean(entity, K_IU, false) || getRotPersistentBoolean(entity, K_DFUL, false) || getRotPersistentBoolean(entity, K_DFUR, false);
			rot.getEntityData().set(RotEntity.DATA_is_uppercutting, isUppercutting);

			boolean baseUppercutting = getRotPersistentBoolean(entity, K_IU, false);
			boolean isUppercutChargingLeft = (baseUppercutting && getRotPersistentBoolean(entity, K_IUL, false)) || (getRotPersistentBoolean(entity, K_DFUL, false) && entity.getPersistentData().getDouble(K_DULT) > 1);
			rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_left, isUppercutChargingLeft);

			boolean isUppercutChargingRight = (baseUppercutting && getRotPersistentBoolean(entity, K_IUR, false)) || (getRotPersistentBoolean(entity, K_DFUR, false) && entity.getPersistentData().getDouble(K_DURT) > 1);
			rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_right, isUppercutChargingRight);

			boolean isDropkickChargingSynced = getRotPersistentBoolean(entity, K_IDC, false);
			rot.getEntityData().set(RotEntity.DATA_is_dropkick_charging, isDropkickChargingSynced);

			boolean isLand = getRotPersistentBoolean(entity, K_I, false);
			rot.getEntityData().set(RotEntity.DATA_isLand, isLand);

			boolean isLand2 = getRotPersistentBoolean(entity, K_I2, false);
			rot.getEntityData().set(RotEntity.DATA_isLand2, isLand2);

		}
	}

	private static boolean handleSuperheatEvaporationState(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		if (entity == null || !(world instanceof ServerLevel serverLevel)) return false;

		boolean learnedSuperheat = getRotPersistentBoolean(entity, K_UWE, false)
				|| getRotPersistentBoolean(entity, K_USB, false)
				|| getRotPersistentBoolean(entity, K_TFD, false);

		double chargeTicks = entity.getPersistentData().getDouble(K_RSC);
		double activeTicks = entity.getPersistentData().getDouble(K_RSA);
		double cd = entity.getPersistentData().getDouble(K_RSC2);

		boolean isSubmerged = entity.isInWater() || entity.isUnderWater();
		if (!isSubmerged && entity.level() != null) {
			BlockState eyeBs = entity.level().getBlockState(BlockPos.containing(entity.getEyePosition()));
			BlockState feetBs = entity.level().getBlockState(entity.blockPosition());
			isSubmerged = eyeBs.is(Blocks.WATER) || feetBs.is(Blocks.WATER);
		}

		boolean fightingColdTarget = false;
		if (combatTarget instanceof LivingEntity livTarget && livTarget.isAlive()) {
			String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(livTarget.getType()).toString().toLowerCase(java.util.Locale.ROOT);
			fightingColdTarget = targetId.contains("stray") || targetId.contains("snow") || targetId.contains("ice") 
				|| targetId.contains("polar") || targetId.contains("frost") || targetId.contains("freeze") 
				|| livTarget.getTicksFrozen() > 0;
		}

		if (chargeTicks <= 0 && activeTicks <= 0) {
			boolean tacticalTrigger = (isSubmerged || (fightingColdTarget && entity.distanceTo(combatTarget) <= 24.0));
			if (learnedSuperheat && tacticalTrigger && combatTarget != null && combatTarget.isAlive() && cd <= 0 && !isDoingCombo(entity)) {
				chargeTicks = fightingColdTarget ? 35.0 : (40.0 + entity.getRandom().nextDouble() * 50.0);
				entity.getPersistentData().putDouble(K_RSC, chargeTicks);
				entity.getPersistentData().putDouble(K_RSMC, chargeTicks);
				entity.getPersistentData().putDouble(K_RSC2, SUPERHEAT_EVAPORATION_COOLDOWN);
				entity.getPersistentData().putDouble(K_SGAC, 30.0);

				if (entity instanceof Mob mob) {
					mob.getNavigation().stop();
				}
				Vec3 mv = entity.getDeltaMovement();
				entity.setDeltaMovement(mv.x * 0.2, Math.min(0.0, mv.y), mv.z * 0.2);
			}
		}

		if (chargeTicks > 0) {
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			Vec3 mv = entity.getDeltaMovement();
			entity.setDeltaMovement(mv.x * 0.85, Math.min(0.0, mv.y - 0.04), mv.z * 0.85);

			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}

			double maxCharge = entity.getPersistentData().getDouble(K_RSMC);
			if (maxCharge <= 0) maxCharge = 60.0;
			double progress = Math.max(0.0, Math.min(1.0, 1.0 - (chargeTicks / maxCharge)));

			int steamCount = (int) (4 + progress * 14);
			double spread = 0.5 + progress * 1.5;
			serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX(), entity.getY() + 0.8, entity.getZ(), steamCount, spread, 0.6, spread, 0.03);
			serverLevel.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, entity.getX(), entity.getY() + 0.2, entity.getZ(), (int)(6 + progress * 20), spread, 0.4, spread, 0.05);
			serverLevel.sendParticles(ParticleTypes.BUBBLE_POP, entity.getX(), entity.getY() + 1.0, entity.getZ(), (int)(4 + progress * 10), spread, 0.5, spread, 0.02);

			if (progress > 0.35) {
				serverLevel.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 0.8, entity.getZ(), (int)(3 + progress * 10), spread * 0.6, 0.5, spread * 0.6, 0.03);
				serverLevel.sendParticles(ParticleTypes.LAVA, entity.getX(), entity.getY() + 1.0, entity.getZ(), (int)(1 + progress * 5), spread * 0.5, 0.5, spread * 0.5, 0.0);
			}

			int soundInterval = Math.max(2, (int) (12 * (1.0 - progress)) + 2);
			if (entity.tickCount % soundInterval == 0) {
				float volume = (float) (0.6 + 1.2 * progress);
				float pitch = (float) (0.6 + 0.8 * progress);
				playHostileSound(serverLevel, entity.getX(), entity.getY(), entity.getZ(), "block.fire.extinguish", volume, pitch);
				if (progress > 0.5) {
					playHostileSound(serverLevel, entity.getX(), entity.getY(), entity.getZ(), "block.lava.extinguish", volume * 0.8F, pitch);
				}
			}

			if (progress > 0.5 && entity.tickCount % 5 == 0) {
				double boilRadius = 4.0 + progress * 8.0;
				AABB boilBox = entity.getBoundingBox().inflate(boilRadius);
				List<LivingEntity> scaldVictims = serverLevel.getEntitiesOfClass(LivingEntity.class, boilBox, e -> e != entity && !isWoodboundEntity(e, entity));
				for (LivingEntity victim : scaldVictims) {
					dealTrueDamageToBosses(victim, new DamageSource(serverLevel.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_solar_beam"))), entity), 4.0F);
					victim.setRemainingFireTicks(60);
					serverLevel.sendParticles(ParticleTypes.SMOKE, victim.getX(), victim.getY() + 0.8, victim.getZ(), 4, 0.2, 0.4, 0.2, 0.02);
				}
			}

			if (chargeTicks <= 1) {
				entity.getPersistentData().putDouble(K_RSC, 0.0);
				entity.getPersistentData().putDouble(K_RSA, SUPERHEAT_EVAPORATION_WAVE_TICKS);
				entity.getPersistentData().putDouble(K_RSCR, 1.0);

				playHostileSound(serverLevel, entity.getX(), entity.getY(), entity.getZ(), "entity.generic.explode", 2.2F, 0.5F);
				playHostileSound(serverLevel, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sonic_boom", 2.0F, 0.4F);
				playHostileSound(serverLevel, entity.getX(), entity.getY(), entity.getZ(), "block.lava.extinguish", 2.5F, 0.7F);
				serverLevel.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + 1.2, entity.getZ(), 2, 0, 0, 0, 0);
			}
			return true;
		}

		if (activeTicks > 0) {
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			if (combatTarget != null) {
				lockLookAtTarget(entity, combatTarget);
			}

			double progress = 1.0 - (activeTicks / SUPERHEAT_EVAPORATION_WAVE_TICKS);
			double prevRadius = entity.getPersistentData().getDouble(K_RSCR);
			if (prevRadius <= 0) prevRadius = 1.0;
			double curRadius = 1.0 + (SUPERHEAT_EVAPORATION_RADIUS - 1.0) * Math.sin(progress * (Math.PI / 2.0));
			entity.getPersistentData().putDouble(K_RSCR, curRadius);

			BlockPos centerPos = entity.blockPosition();
			int minX = Mth.floor(centerPos.getX() - curRadius);
			int maxX = Mth.ceil(centerPos.getX() + curRadius);
			int minY = Math.max(serverLevel.getMinBuildHeight(), Mth.floor(centerPos.getY() - curRadius * 0.6));
			int maxY = Math.min(serverLevel.getMaxBuildHeight(), Mth.ceil(centerPos.getY() + curRadius * 0.9));
			int minZ = Mth.floor(centerPos.getZ() - curRadius);
			int maxZ = Mth.ceil(centerPos.getZ() + curRadius);

			double curRadiusSq = curRadius * curRadius;
			double prevRadiusSq = Math.max(0.0, (prevRadius - 0.75) * (prevRadius - 0.75));

			int transformedCount = 0;
			for (int bx = minX; bx <= maxX; bx++) {
				for (int bz = minZ; bz <= maxZ; bz++) {
					double dx = (bx + 0.5) - entity.getX();
					double dz = (bz + 0.5) - entity.getZ();
					double distHsq = dx * dx + dz * dz;
					if (distHsq > curRadiusSq) continue;

					for (int by = minY; by <= maxY; by++) {
						double dy = (by + 0.5) - (entity.getY() + 0.5);
						double distSq = distHsq + (dy > 0 ? dy * dy * 0.9 : dy * dy * 1.5);

						if (distSq <= curRadiusSq && distSq >= prevRadiusSq) {
							BlockPos bp = new BlockPos(bx, by, bz);
							BlockState bs = serverLevel.getBlockState(bp);

							if (bs.is(Blocks.WATER) || bs.getFluidState().is(FluidTags.WATER)) {
								serverLevel.setBlock(bp, Blocks.AIR.defaultBlockState(), 3);
								transformedCount++;
								if (serverLevel.getRandom().nextDouble() < 0.35) {
									serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, bx + 0.5, by + 0.5, bz + 0.5, 2, 0.3, 0.3, 0.3, 0.04);
									serverLevel.sendParticles(ParticleTypes.CLOUD, bx + 0.5, by + 0.5, bz + 0.5, 1, 0.2, 0.2, 0.2, 0.02);
								}
							}
							else if (bs.is(Blocks.POWDER_SNOW) || bs.is(Blocks.SNOW) || bs.is(Blocks.SNOW_BLOCK) 
								|| bs.is(Blocks.ICE) || bs.is(Blocks.PACKED_ICE) || bs.is(Blocks.FROSTED_ICE)) {
								serverLevel.setBlock(bp, Blocks.AIR.defaultBlockState(), 3);
								transformedCount++;
								serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, bx + 0.5, by + 0.5, bz + 0.5, 3, 0.3, 0.3, 0.3, 0.05);
								serverLevel.sendParticles(ParticleTypes.FLAME, bx + 0.5, by + 0.5, bz + 0.5, 1, 0.1, 0.1, 0.1, 0.02);
							}
							else if (bs.is(Blocks.BLUE_ICE)) {
								serverLevel.setBlock(bp, Blocks.WATER.defaultBlockState(), 3);
								transformedCount++;
								serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, bx + 0.5, by + 0.5, bz + 0.5, 2, 0.3, 0.3, 0.3, 0.03);
							}
						}
					}
				}
			}

			if (transformedCount > 0 || entity.tickCount % 4 == 0) {
				float wavePitch = (float) (0.7 + (curRadius / SUPERHEAT_EVAPORATION_RADIUS) * 0.5);
				playHostileSound(serverLevel, entity.getX(), entity.getY(), entity.getZ(), "block.fire.extinguish", 1.8F, wavePitch);
				if (entity.tickCount % 6 == 0) {
					playHostileSound(serverLevel, entity.getX(), entity.getY(), entity.getZ(), "block.lava.extinguish", 1.4F, 0.8F);
				}
			}

			serverLevel.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 1.2, entity.getZ(), 12, 0.8, 1.0, 0.8, 0.08);
			serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX(), entity.getY() + 1.5, entity.getZ(), 8, 0.5, 0.8, 0.5, 0.05);

			AABB hitBox = entity.getBoundingBox().inflate(curRadius);
			List<LivingEntity> burnVictims = serverLevel.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != entity && !isWoodboundEntity(e, entity));
			for (LivingEntity victim : burnVictims) {
				double distToRot = entity.distanceTo(victim);
				if (distToRot <= curRadius + 1.5) {
					float dmgFactor = (float) (1.0 - (distToRot / (SUPERHEAT_EVAPORATION_RADIUS + 2.0)) * 0.4);
					double baseDmg = SUPERHEAT_EVAPORATION_DAMAGE;
					String vicId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString().toLowerCase(java.util.Locale.ROOT);
					if (vicId.contains("stray") || vicId.contains("snow") || vicId.contains("ice") || vicId.contains("polar") || vicId.contains("frost") || vicId.contains("freeze") || victim.getTicksFrozen() > 0) {
						baseDmg *= (2.5 + entity.getRandom().nextDouble() * 0.5);
						victim.setTicksFrozen(0);
					}

					dealTrueDamageToBosses(victim, new DamageSource(serverLevel.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_solar_beam"))), entity), (float) (baseDmg * dmgFactor * getAdaptationMultiplier(entity)));
					victim.setRemainingFireTicks(200);

					Vec3 push = victim.position().subtract(entity.position()).normalize();
					victim.setDeltaMovement(push.x * 0.6, 0.25, push.z * 0.6);
					victim.hasImpulse = true;
				}
			}

			if (activeTicks <= 1) {
				entity.getPersistentData().putDouble(K_RSA, 0.0);
				entity.getPersistentData().putDouble(K_RSCR, 0.0);
			}
			return true;
		}

		return false;
	}

	private static boolean handleOverheadState(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		double overheadTicks = entity.getPersistentData().getDouble(K_ROT);
		if (getRotPersistentBoolean(entity, K_DFO, false)) {
			if (overheadTicks <= 1) {
				entity.getPersistentData().putDouble(K_ROT, OVERHEAD_TOTAL_TICKS);
				overheadTicks = OVERHEAD_TOTAL_TICKS;
			}
		}

		if (overheadTicks > 0) {
			Entity targetEntity = null;
			if (world instanceof ServerLevel serverLevel) {
				try {
					String targetUUIDStr = entity.getPersistentData().getString(K_OTU);
					java.util.UUID targetUUID = !targetUUIDStr.isEmpty() ? java.util.UUID.fromString(targetUUIDStr) : null;
					if (targetUUID != null) {
						targetEntity = serverLevel.getEntity(targetUUID);
					}
				} catch (Exception ignored) {}
			}
			if (targetEntity == null) {
				targetEntity = combatTarget;
			}

			double strikeTick = OVERHEAD_STRIKE_TICK;
			boolean overheadStarted = getRotPersistentBoolean(entity, K_ROS, false);
			if (!overheadStarted) {
				entity.getPersistentData().putBoolean(K_ROS, true);
				entity.setDeltaMovement(0.0, 0.6, 0.0);
				entity.hasImpulse = true;

				if (targetEntity != null) {
					entity.teleportTo(targetEntity.getX(), targetEntity.getY() + targetEntity.getEyeHeight() + OVERHEAD_Y_OFFSET_1, targetEntity.getZ());
					lockLookAtTarget(entity, targetEntity);
				}
			} else if (overheadTicks >= OVERHEAD_PREP_THRESHOLD) {
				if (targetEntity != null) {
					entity.teleportTo(targetEntity.getX(), targetEntity.getY() + targetEntity.getEyeHeight() + OVERHEAD_Y_OFFSET_2, targetEntity.getZ());
					lockLookAtTarget(entity, targetEntity);
				}
				entity.setDeltaMovement(0, 0, 0);
			} else if (overheadTicks == strikeTick) {
				if (targetEntity instanceof LivingEntity liv) {
					double originalY = liv.getY();
					double targetSunkY = originalY - 1.0;
					BlockPos belowPos = BlockPos.containing(liv.getX(), targetSunkY, liv.getZ());
					if (world.getBlockState(belowPos).isCollisionShapeFullBlock(world, belowPos)) {
						targetSunkY = originalY;
					}
					disablePlayerShield(liv, 100);
					net.minecraft.world.damagesource.DamageSource damageSource;
					if (world instanceof ServerLevel level) {
						damageSource = new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_seismic_slam"))), entity);
					} else {
						damageSource = liv.damageSources().generic();
					}
					dealTrueDamageToBosses(liv, damageSource, 25.0F * (float) getAdaptationMultiplier(entity));
					liv.teleportTo(liv.getX(), targetSunkY, liv.getZ());
					liv.setDeltaMovement(liv.getDeltaMovement().x(), -7.5, liv.getDeltaMovement().z());
					liv.hasImpulse = true;
					liv.hurtMarked = true;
					liv.fallDistance += 5.0F;
					entity.getPersistentData().putDouble(K_SSS2, 1);
					entity.getPersistentData().putDouble(K_SSX, liv.getX());
					entity.getPersistentData().putDouble(K_SSY, liv.getY());
					entity.getPersistentData().putDouble(K_SSZ, liv.getZ());
					entity.getPersistentData().putBoolean(K_SSV, false);
					playHostileSound(world, liv.getX(), liv.getY(), liv.getZ(), "entity.generic.explode", 1.5F, 0.55F);
					playHostileSound(world, liv.getX(), liv.getY(), liv.getZ(), "entity.iron_golem.attack", 1.8F, 0.45F);
					if (world instanceof ServerLevel level) {
						level.sendParticles(ParticleTypes.SONIC_BOOM, liv.getX(), liv.getY() + 0.5, liv.getZ(), 1, 0, 0, 0, 0);
						boolean totemActive = getRotPersistentBoolean(entity, K_STA2, false);
						sendCameraShake(totemActive ? 0.4F : 0.25F, totemActive ? 12 : 6, totemActive ? 15.0F : 10.0F);
					}
				}
			} else if (overheadTicks < strikeTick) {
				if (targetEntity instanceof LivingEntity liv && !liv.onGround()) {
					liv.setDeltaMovement(liv.getDeltaMovement().x(), -3.2, liv.getDeltaMovement().z());
					liv.hasImpulse = true;
					liv.hurtMarked = true;
					liv.fallDistance += 2.0F;
				}
			}

			handlePassengerAndGrowth(entity);
			return true;
		}
		entity.getPersistentData().putBoolean(K_ROS, false);
		return false;
	}

	private static boolean handleThreatAwareEvasiveSpacing(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		if (entity == null || combatTarget == null || !combatTarget.isAlive()) {
			return false;
		}

		double evasiveCD = getRotPersistentDouble(entity, K_SESC, 0.0);
		if (evasiveCD > 0) {
			tickCooldown(entity, K_SESC, 1);
			return false;
		}

		if (isRotChannelingAbility(entity)
			|| getRotPersistentDouble(entity, K_ROT, 0.0) > 0
			|| getRotPersistentBoolean(entity, K_IU, false)
			|| getRotPersistentDouble(entity, K_SLT, 0.0) > 0) {
			return false;
		}

		if (!(combatTarget instanceof LivingEntity livTarget)) {
			return false;
		}

		double estimatedDmg = 1.0;
		ItemStack mainHand = livTarget.getMainHandItem();
		if (!mainHand.isEmpty()) {
			String itemId = BuiltInRegistries.ITEM.getKey(mainHand.getItem()).toString().toLowerCase();
			if (itemId.contains("mace")) {
				estimatedDmg += 12.0 + Math.max(0.0, livTarget.fallDistance * 8.0);
			} else if (itemId.contains("infinity") || itemId.contains("kill") || itemId.contains("god") || itemId.contains("op")) {
				estimatedDmg += 2000.0;
			} else if (itemId.contains("sword") || itemId.contains("axe")) {
				estimatedDmg += 10.0;
			}
		}

		if (livTarget.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST)) {
			var eff = livTarget.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST);
			if (eff != null) estimatedDmg *= (1.0 + (eff.getAmplifier() + 1) * 0.5);
		}

		double rotHp = entity instanceof LivingEntity liv ? liv.getHealth() : 200.0;
		boolean isUltraThreat = estimatedDmg >= 120.0 || estimatedDmg >= rotHp * 0.5;

		double dist = entity.distanceTo(livTarget);
		if (isUltraThreat && dist < 12.0) {
			Vec3 retreatDir = entity.position().subtract(livTarget.position()).normalize();
			if (retreatDir.lengthSqr() < 0.001) {
				retreatDir = new Vec3(1, 0, 0);
			}

			double safeDist = 14.0 + Math.random() * 4.0;
			double targetX = entity.getX() + retreatDir.x * safeDist;
			double targetZ = entity.getZ() + retreatDir.z * safeDist;
			double targetY = findTargetGroundY(world, targetX, entity.getY(), targetZ);

			teleportEntity(entity, targetX, targetY, targetZ);
			lockLookAtTarget(entity, livTarget);
			setRotPersistentDouble(entity, K_SESC, 60.0);

			if (world instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getY() + 1.0, entity.getZ(), 25, 0.5, 1.0, 0.5, 0.15);
				playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.enderman.teleport", 1.5F, 1.1F);
			}

			double randVal = Math.random();
			if (randVal < 0.40 && getRotPersistentBoolean(entity, "unlocked_die_rider_kick", false)) {
				entity.getPersistentData().putDouble(K_SDKP, 1.0);
				entity.getPersistentData().putDouble(K_SDKT, 0.0);
			} else if (randVal < 0.75 && getRotPersistentBoolean(entity, "unlocked_judgment", false)) {
				entity.getPersistentData().putDouble(K_SJT, 40.0);
			} else {
				setRotPersistentDouble(entity, "omni_sonic_boom_ticks", 30.0);
			}

			return true;
		}

		return false;
	}

	private static boolean handleDiveCounterState(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		if (entity == null) {
			return false;
		}

		if (isRotChannelingAbility(entity)
			|| getRotPersistentBoolean(entity, K_IU, false)
			|| getRotPersistentDouble(entity, K_SLT, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SDCC, 0.0) > 0) {
			return false;
		}

		LivingEntity targetLiv = null;
		if (combatTarget instanceof LivingEntity liv) {
			targetLiv = liv;
		} else {
			AABB searchBox = new AABB(entity.getX() - 16.0, entity.getY() - 4.0, entity.getZ() - 16.0, entity.getX() + 16.0, entity.getY() + 28.0, entity.getZ() + 16.0);
			List<Player> nearbyPlayers = world.getEntitiesOfClass(Player.class, searchBox, p -> p.isAlive() && !p.isSpectator());
			for (Player p : nearbyPlayers) {
				String mainHand = BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).toString();
				String offHand = BuiltInRegistries.ITEM.getKey(p.getOffhandItem().getItem()).toString();
				if (mainHand.contains("mace") || offHand.contains("mace")) {
					targetLiv = p;
					break;
				}
			}
		}

		if (targetLiv == null || !targetLiv.isAlive()) {
			return false;
		}

		boolean targetHasMace = false;
		if (targetLiv instanceof Player player) {
			String mainHandName = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
			String offHandName = BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).toString();
			targetHasMace = mainHandName.contains("mace") || offHandName.contains("mace");
		} else {
			String mainHandName = BuiltInRegistries.ITEM.getKey(targetLiv.getMainHandItem().getItem()).toString();
			String offHandName = BuiltInRegistries.ITEM.getKey(targetLiv.getOffhandItem().getItem()).toString();
			targetHasMace = mainHandName.contains("mace") || offHandName.contains("mace");
		}

		if (!targetHasMace) {
			return false;
		}

		double yDiff = targetLiv.getY() - entity.getY();
		if (yDiff < DIVE_COUNTER_MIN_HEIGHT) {
			return false;
		}

		double dx = targetLiv.getX() - entity.getX();
		double dz = targetLiv.getZ() - entity.getZ();
		double horizDist = Math.sqrt(dx * dx + dz * dz);
		if (horizDist > DIVE_COUNTER_TRIGGER_RANGE) {
			return false;
		}

		boolean isAirborne = !targetLiv.onGround();
		if (!isAirborne) {
			return false;
		}

		double downwardVel = targetLiv.getDeltaMovement().y();
		boolean isAtPeakOrDescending = downwardVel <= 0.35 || targetLiv.fallDistance >= 0.5;
		if (!isAtPeakOrDescending) {
			return false;
		}

		if (entity instanceof Mob mob && mob.getTarget() == null) {
			mob.setTarget(targetLiv);
		}
		entity.getPersistentData().putInt(K_SCTI, targetLiv.getId());

		setRotPersistentDouble(entity, K_SDCC, DIVE_COUNTER_COOLDOWN);

		boolean tryOverhead = Math.random() < 0.50 || getRotPersistentBoolean(entity, K_UOC, false);
		double currentOverheadTicks = getRotPersistentDouble(entity, K_ROT, 0.0);
		if (tryOverhead && currentOverheadTicks <= 0) {
			setRotPersistentBoolean(entity, K_UOC, true);
			entity.getPersistentData().putDouble(K_ROT, OVERHEAD_TOTAL_TICKS);
			entity.getPersistentData().putString(K_OTU, targetLiv.getUUID().toString());

			if (world instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + 1.2, entity.getZ(), 20, 0.5, 0.5, 0.5, 0.2);
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getY() + 1.2, entity.getZ(), 15, 0.4, 0.4, 0.4, 0.1);
				playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.snarl", 1.8F, 1.2F);
				playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.enderman.teleport", 1.8F, 0.9F);
			}

			handlePassengerAndGrowth(entity);
			return true;
		}

		double PUNCH_LEAD_TICKS = 2.0;
		Vec3 targetVel = targetLiv.getDeltaMovement();
		double predictedX = targetLiv.getX() + targetVel.x * PUNCH_LEAD_TICKS;
		double predictedZ = targetLiv.getZ() + targetVel.z * PUNCH_LEAD_TICKS;
		double groundY = findTargetGroundY(world, predictedX, targetLiv.getY(), predictedZ);

		teleportEntity(entity, predictedX, groundY, predictedZ);
		lockLookAtTarget(entity, targetLiv);

		setRotPersistentBoolean(entity, K_SDCA, true);

		setRotPersistentBoolean(entity, K_UHSSC, true);
		setRotPersistentBoolean(entity, K_IU, true);
		boolean isLeft = Math.random() < 0.5;
		setRotPersistentBoolean(entity, K_IUL, isLeft);
		setRotPersistentBoolean(entity, K_IUR, !isLeft);
		setRotPersistentBoolean(entity, K_IUS, true);
		setRotPersistentDouble(entity, K_SCS, 1.0);
		setRotPersistentDouble(entity, K_SCT2, UPPERCUT_LAUNCH_TICK + 5.0);
		if (entity instanceof RotEntity rot) {
			rot.getEntityData().set(RotEntity.DATA_is_uppercutting, true);

		}

		if (world instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + 1.2, entity.getZ(), 20, 0.5, 0.5, 0.5, 0.2);
			level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 1.2, entity.getZ(), 15, 0.4, 0.4, 0.4, 0.1);
			playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.snarl", 1.8F, 1.2F);
			playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.iron_golem.attack", 1.8F, 0.9F);
		}

		handlePassengerAndGrowth(entity);
		return true;
	}

	private static void handleHeavyPunchState(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		double heavyLeftTicks = entity.getPersistentData().getDouble(K_SHLPT);
		double heavyRightTicks = entity.getPersistentData().getDouble(K_SHRPT);

		if (combatTarget != null && (heavyLeftTicks > 0 || heavyRightTicks > 0)) {
			snapLookAtTarget(entity, combatTarget);
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			entity.setDeltaMovement(0, entity.getDeltaMovement().y(), 0);
		}

		double jitteredPunchStrike = getTelegraphJitter(entity, "heavy_punch", HEAVY_PUNCH_STRIKE_TICK, -5.0, 5.0);
		boolean strikeLeft = (heavyLeftTicks == jitteredPunchStrike);
		boolean strikeRight = (heavyRightTicks == jitteredPunchStrike);

		if (strikeLeft || strikeRight) {
			Entity target = combatTarget;
			if (target == null && world instanceof ServerLevel level) {
				target = level.getNearestPlayer(entity, 4.5);
			}
			if (target != null && entity.distanceTo(target) <= 4.5) {
				setRotPersistentDouble(entity, K_SHPM, 0.0);
				Vec3 knockDir = target.position().subtract(entity.position()).normalize();
				applyKnockbackAndSync(target, knockDir.x * 4.4, 0.70, knockDir.z * 4.4);

				if (target instanceof LivingEntity targetLiv) {
					disablePlayerShield(targetLiv, 100);
				}
				if (world instanceof ServerLevel level) {
					dealTrueDamageToBosses(target, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_consecutive_punches"))), entity), (float) MELEE_PUNCH_DAMAGE * (float) getAdaptationMultiplier(entity));
				}

				if (world instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.8, target.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
					level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
					level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.0, target.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.iron_golem.attack", 1.6F, 0.45F);
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.generic.explode", 0.8F, 0.45F);
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sonic_boom", 0.6F, 0.4F);
				}
			} else {
				double misses = getRotPersistentDouble(entity, K_SHPM, 0.0);
				setRotPersistentDouble(entity, K_SHPM, misses + 1.0);
			}
		}
	}

	private static void executeMinosHeavyPunchBlink(LevelAccessor world, Entity entity, LivingEntity targetLiv, boolean isLeftHand) {
		if (targetLiv == null || !hasHeavyPunchSupport(world, targetLiv)) return;
		if (getRotPersistentDouble(entity, K_SSFT, 0.0) > 0 || getRotPersistentDouble(entity, K_SSCT, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SCFT, 0.0) > 0 || getRotPersistentDouble(entity, K_SCCT, 0.0) > 0) {
			entity.getPersistentData().putDouble(K_SSFT, 0);
			entity.getPersistentData().putDouble(K_SSCT, 0);
			entity.getPersistentData().putDouble(K_SCFT, 0);
			entity.getPersistentData().putDouble(K_SCCT, 0);
			entity.getPersistentData().putDouble(K_SLCT, LASER_CLOSING_TICKS);
			stopHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
		}
		double targetYaw = targetLiv.getYRot();
		double radians = Math.toRadians(targetYaw);

		double missCount = getRotPersistentDouble(entity, K_SHPM, 0.0);

		Vec3 targetVel = targetLiv.getDeltaMovement();
		double speedSqr = targetVel.x * targetVel.x + targetVel.z * targetVel.z;
		boolean targetMovingAway = speedSqr > 0.01;

		double offsetDist = (missCount >= 1.0 || targetMovingAway) ? 2.2 : -1.3;

		double tx = targetLiv.getX() + Math.sin(radians) * offsetDist;
		double tz = targetLiv.getZ() - Math.cos(radians) * offsetDist;
		double ty = targetLiv.getY();

		if (world instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 0.8, entity.getZ(), 12, 0.3, 0.5, 0.3, 0.1);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getY() + 0.8, entity.getZ(), 8, 0.2, 0.4, 0.2, 0.05);
			teleportEntity(entity, tx, ty, tz);
			level.sendParticles(ParticleTypes.PORTAL, tx, ty + 0.8, tz, 12, 0.3, 0.5, 0.3, 0.1);
			level.sendParticles(ParticleTypes.GUST, tx, ty + 0.5, tz, 1, 0.1, 0.1, 0.1, 0.0);

			playHostileSound(level, tx, ty, tz, "item.chorus_fruit.teleport", 1.4F, 0.9F);
			playHostileSound(level, tx, ty, tz, "entity.enderman.teleport", 1.2F, 0.6F);
			playHostileSound(level, tx, ty, tz, "entity.warden.sonic_charge", 1.0F, 1.2F);
		}
		snapLookAtTarget(entity, targetLiv);

		if (isLeftHand) {
			entity.getPersistentData().putDouble(K_SHLPT, HEAVY_PUNCH_TOTAL_TICKS);
			entity.getPersistentData().putDouble(K_SHRPT, 0);
		} else {
			entity.getPersistentData().putDouble(K_SHRPT, HEAVY_PUNCH_TOTAL_TICKS);
			entity.getPersistentData().putDouble(K_SHLPT, 0);
		}
		if (entity instanceof LivingEntity ls) {
			ls.swing(InteractionHand.MAIN_HAND, true);
		}
	}

	private static boolean hasHeavyPunchSupport(LevelAccessor world, LivingEntity target) {
		if (target == null || target.onGround() || target.isInWater() || target.isInLava()) return true;
		BlockPos belowTarget = BlockPos.containing(target.getX(), target.getBoundingBox().minY - 0.05, target.getZ());
		BlockState belowState = world.getBlockState(belowTarget);
		if (belowState.blocksMotion() || !belowState.getFluidState().isEmpty() || belowState.is(net.minecraft.tags.BlockTags.LEAVES)) return true;
		BlockPos feet = BlockPos.containing(target.getX(), target.getBoundingBox().minY + 0.05, target.getZ());
		BlockPos head = BlockPos.containing(target.getX(), target.getBoundingBox().maxY - 0.05, target.getZ());
		return !world.getBlockState(feet).blocksMotion() && !world.getBlockState(head).blocksMotion();
	}

	private static boolean isTargetCornered(LevelAccessor world, Entity target, Entity attacker) {
		if (target == null) return false;
		int solidBlocks = 0;
		BlockPos p = target.blockPosition();
		if (world.getBlockState(p.east()).isSolid() || !world.getBlockState(p.east()).isAir()) solidBlocks++;
		if (world.getBlockState(p.west()).isSolid() || !world.getBlockState(p.west()).isAir()) solidBlocks++;
		if (world.getBlockState(p.north()).isSolid() || !world.getBlockState(p.north()).isAir()) solidBlocks++;
		if (world.getBlockState(p.south()).isSolid() || !world.getBlockState(p.south()).isAir()) solidBlocks++;
		if (solidBlocks >= 2) return true;

		Vec3 dir = target.position().subtract(attacker.position());
		if (dir.lengthSqr() > 0.01) {
			dir = dir.normalize();
		} else {
			dir = attacker.getLookAngle();
		}
		BlockPos behindPos = BlockPos.containing(target.getX() + dir.x * 1.2, target.getY() + 0.5, target.getZ() + dir.z * 1.2);
		BlockPos behindHead = behindPos.above();
		if (world.getBlockState(behindPos).isSolid() || world.getBlockState(behindHead).isSolid() || !world.getBlockState(behindPos).isAir()) {
			return true;
		}
		return false;
	}

	private static boolean handleSlamState(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		double slamPhase = entity.getPersistentData().getDouble(K_SSP);
		double slamTicks = entity.getPersistentData().getDouble(K_SST2);

		if (slamPhase > 0) {
			if (slamTicks > 0) {
				entity.getPersistentData().putDouble(K_SST2, slamTicks - 1);
			}

			if (slamPhase == 1) {
				if (combatTarget != null) {
					lockLookAtTarget(entity, combatTarget);
				}
				Vec3 m = entity.getDeltaMovement();
				entity.setDeltaMovement(0.0, m.y(), 0.0);

				if (slamTicks <= 1) {
					entity.getPersistentData().putDouble(K_SSP, 2);
					entity.getPersistentData().putDouble(K_SST2, 25);
					playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sonic_charge", 1.5F, 0.75F);
				}
			} else if (slamPhase == 2) {
				entity.setDeltaMovement(0, 0, 0);
				if (combatTarget != null) {
					lockLookAtTarget(entity, combatTarget);
				}
				if (world instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1.2, entity.getZ(), 4, 0.2, 0.2, 0.2, 0.1);
				}

				if (slamTicks <= 1) {
					entity.getPersistentData().putDouble(K_SSP, 3);
					entity.getPersistentData().putDouble(K_SST2, 5);
				}
			} else if (slamPhase == 3) {
				double startX = entity.getX();
				double startY = entity.getY();
				double startZ = entity.getZ();
				double groundY = findGroundY(world, entity);

				if (world instanceof ServerLevel level) {
					for (double sy = groundY; sy <= startY; sy += 0.5) {
						level.sendParticles(ParticleTypes.SONIC_BOOM, startX, sy, startZ, 1, 0.1, 0.1, 0.1, 0.0);
						level.sendParticles(ParticleTypes.CLOUD, startX, sy, startZ, 2, 0.15, 0.15, 0.15, 0.02);
					}
				}

				entity.teleportTo(startX, groundY, startZ);
				entity.setDeltaMovement(0, -0.05, 0);

				entity.getPersistentData().putDouble(K_SSP, 0);
				entity.getPersistentData().putDouble(K_SST2, 0);
				entity.getPersistentData().putDouble(K_SLT, 20);
				entity.getPersistentData().putBoolean(K_SISL, true);

				executeCrushLandingBlast(world, entity, combatTarget, false);
			}
			handlePassengerAndGrowth(entity);
			return true;
		}
		return false;
	}

	private static boolean handleDieKickState(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		double dieKickPhase = entity.getPersistentData().getDouble(K_SDKP);
		double dieKickTicks = entity.getPersistentData().getDouble(K_SDKT);

		if (dieKickPhase > 0) {
			if (dieKickTicks > 0) {
				entity.getPersistentData().putDouble(K_SDKT, dieKickTicks - 1);
			}

			if (dieKickPhase == 1) {
				if (combatTarget != null) {
					snapLookAtTarget(entity, combatTarget);
				}
				entity.setXRot(0.0F);
				if (entity instanceof Mob mob) {
					mob.setXRot(0.0F);
					mob.yHeadRot = mob.getYRot();
				}
				Vec3 m = entity.getDeltaMovement();
				entity.setDeltaMovement(0.0, m.y(), 0.0);

				if (dieKickTicks <= 1) {
					entity.getPersistentData().putDouble(K_SDKP, 2);
					entity.getPersistentData().putDouble(K_SDKT, 25);
					playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sonic_charge", 1.5F, 0.75F);
				}
			} else if (dieKickPhase == 2) {
				entity.setDeltaMovement(0, 0, 0);
				if (combatTarget != null) {
					snapLookAtTarget(entity, combatTarget);
				}
				if (world instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 1.2, entity.getZ(), 4, 0.2, 0.2, 0.2, 0.1);
				}

				if (dieKickTicks <= 1) {
					entity.getPersistentData().putDouble(K_SDKP, 3);
					entity.getPersistentData().putDouble(K_SDKT, 40);
					playHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sonic_boom", 1.5F, 0.55F);

					Vec3 dir;
					if (combatTarget != null) {
						snapLookAtTarget(entity, combatTarget);
						Vec3 targetCenter = combatTarget.getBoundingBox().getCenter();
						Vec3 rotCenter = entity.getBoundingBox().getCenter();
						dir = targetCenter.subtract(rotCenter);
						if (dir.length() > 0.1) {
							dir = dir.normalize();
						} else {
							dir = entity.getLookAngle().normalize();
						}
					} else {
						dir = entity.getLookAngle().normalize();
					}

					setRotPersistentDouble(entity, K_SDKDX, dir.x);
					setRotPersistentDouble(entity, K_SDKDY, dir.y);
					setRotPersistentDouble(entity, K_SDKDZ, dir.z);

					entity.setDeltaMovement(dir.x * DIE_KICK_SPEED, dir.y * DIE_KICK_SPEED, dir.z * DIE_KICK_SPEED);
					entity.hasImpulse = true;
				}
			} else if (dieKickPhase == 3) {
				double dirX = getRotPersistentDouble(entity, K_SDKDX, 0.0);
				double dirY = getRotPersistentDouble(entity, K_SDKDY, -1.0);
				double dirZ = getRotPersistentDouble(entity, K_SDKDZ, 0.0);

				entity.setDeltaMovement(dirX * DIE_KICK_SPEED, dirY * DIE_KICK_SPEED, dirZ * DIE_KICK_SPEED);
				entity.hasImpulse = true;
				entity.fallDistance = 0;

				double dh = Math.sqrt(dirX * dirX + dirZ * dirZ);
				float targetYRot = (float) (Mth.atan2(dirZ, dirX) * (180F / Math.PI)) - 90F;
				float targetXRot = (float) (-(Mth.atan2(dirY, dh) * (180F / Math.PI)));
				entity.setYRot(targetYRot);
				entity.setXRot(targetXRot);
				if (entity instanceof Mob mob) {
					mob.yBodyRot = targetYRot;
					mob.yHeadRot = targetYRot;
				}

				if (world instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.GUST, entity.getX(), entity.getY() + 0.3, entity.getZ(), 3, 0.1, 0.1, 0.1, 0.05);
					level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.3, entity.getZ(), 1, 0.1, 0.1, 0.1, 0.02);
				}

				double groundY = findGroundY(world, entity);
				boolean hitTarget = false;
				Vec3 impactPoint = null;

				if (combatTarget != null && combatTarget.isAlive()) {
					AABB targetBox = combatTarget.getBoundingBox();
					double rotHalfWidth = entity.getBbWidth() * 0.5;
					double rotHalfHeight = entity.getBbHeight() * 0.5;
					AABB wallBox = targetBox.inflate(rotHalfWidth, rotHalfHeight, rotHalfWidth);
					Vec3 startPos = entity.getBoundingBox().getCenter();
					Vec3 endPos = startPos.add(dirX * DIE_KICK_SPEED, dirY * DIE_KICK_SPEED, dirZ * DIE_KICK_SPEED);

					if (wallBox.contains(startPos) || entity.getBoundingBox().intersects(targetBox)) {
						hitTarget = true;
						impactPoint = startPos;
					} else {
						java.util.Optional<Vec3> clipOpt = wallBox.clip(startPos, endPos);
						if (clipOpt.isPresent()) {
							hitTarget = true;
							impactPoint = clipOpt.get();
						}
					}
				}

				double distToTarget = combatTarget != null ? entity.distanceTo(combatTarget) : 999.0;
				if (entity.onGround() || entity.getY() <= groundY + DIE_KICK_GROUND_OFFSET || hitTarget || distToTarget < DIE_KICK_IMPACT_DIST || dieKickTicks <= 1) {
					if (hitTarget && impactPoint != null) {
						double stopX = impactPoint.x;
						double stopY = impactPoint.y - entity.getBbHeight() * 0.5;
						double stopZ = impactPoint.z;
						entity.teleportTo(stopX, stopY, stopZ);
					} else {
						entity.teleportTo(entity.getX(), groundY, entity.getZ());
					}
					entity.setDeltaMovement(0, -0.05, 0);

					executeCrushLandingBlast(world, entity, combatTarget, true);
					entity.getPersistentData().putDouble(K_SDKP, 4);
					entity.getPersistentData().putDouble(K_SDKT, 10);
				}
			} else if (dieKickPhase == 4) {
				entity.setDeltaMovement(0, -0.05, 0);
				if (dieKickTicks <= 1) {
					entity.getPersistentData().putDouble(K_SDKP, 0);
					entity.getPersistentData().putDouble(K_SDKT, 0);
					entity.getPersistentData().putDouble(K_SLT, 20);
					entity.getPersistentData().putBoolean(K_SISL, true);
				}
			}
			handlePassengerAndGrowth(entity);
			return true;
		}
		return false;
	}

	private static void executeCrushLandingBlast(LevelAccessor world, Entity self, @Nullable Entity target, boolean shakeCamera) {
		if (!(world instanceof ServerLevel level)) return;
		LivingEntity ls = (self instanceof LivingEntity) ? (LivingEntity) self : null;
		
		double targetX = self.getX();
		double targetY = self.getY();
		double targetZ = self.getZ();

		boolean totemActive = getRotPersistentBoolean(self, K_STA2, false);
		boolean isInfinity = getRotPersistentBoolean(self, K_SIIT, false);
		double blastRadius = totemActive ? 12.0 : 6.5;
		if (isInfinity) blastRadius = 15.0;
		double _adaptation = getAdaptationMultiplier(self);
		blastRadius *= _adaptation;
		float blastDamage = totemActive ? 55.0F : 30.0F;
		if (isInfinity) blastDamage *= 2.5F;
		blastDamage *= (float) _adaptation;

		playHostileSound(level, targetX, targetY, targetZ, "entity.generic.explode", 1.8F, 0.45F);
		if (isInfinity) {
			level.explode(self, null, new net.minecraft.world.level.ExplosionDamageCalculator() {
				@Override
				public boolean shouldDamageEntity(net.minecraft.world.level.Explosion explosion, Entity ent) {
					return !isWoodboundEntity(ent, self);
				}
			}, targetX, targetY + 0.5, targetZ, 6.5F * (float) Math.min(5.0, getAdaptationMultiplier(self)), false, Level.ExplosionInteraction.MOB);
		}
		if (shakeCamera) {
			playHostileSound(level, targetX, targetY, targetZ, "entity.warden.sonic_boom", 1.5F, 0.6F);
		}
		playHostileSound(level, targetX, targetY, targetZ, "entity.iron_golem.attack", 2.0F, 0.5F);

		self.getPersistentData().putDouble(K_SSS2, 1);
		self.getPersistentData().putDouble(K_SSX, targetX);
		self.getPersistentData().putDouble(K_SSY, targetY);
		self.getPersistentData().putDouble(K_SSZ, targetZ);

		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, targetX, targetY + 0.5, targetZ, totemActive ? 6 : 3, 0.5, 0.3, 0.5, 0.15);

		net.minecraft.world.level.block.state.BlockState floorState = level.getBlockState(net.minecraft.core.BlockPos.containing(targetX, targetY - 0.5, targetZ));
		if (floorState.isAir()) {
			floorState = level.getBlockState(net.minecraft.core.BlockPos.containing(targetX, targetY - 1.5, targetZ));
		}
		if (floorState.isAir()) {
			floorState = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
		}
		net.minecraft.core.particles.BlockParticleOption dustPillarOptions = new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.DUST_PILLAR, floorState);

		int smallRingCount = totemActive ? 28 : 16;
		for (int rIndex = 0; rIndex < smallRingCount; rIndex++) {
			double ang = (2 * Math.PI / smallRingCount) * rIndex;
			double c = Math.cos(ang);
			double s = Math.sin(ang);
			double r = totemActive ? 2.0 : 1.2;
			double px = targetX + c * r;
			double pz = targetZ + s * r;
			level.sendParticles(dustPillarOptions, px, targetY + 0.2, pz, 1, 0.0, 0.1, 0.0, 0.05);
		}

		net.minecraft.core.particles.ParticleType<?> _tsdType = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse("trial_spawner_detection"));
		net.minecraft.core.particles.ParticleOptions trialSpawnerDetection = _tsdType instanceof net.minecraft.core.particles.ParticleOptions _tsdOpt ? _tsdOpt : net.minecraft.core.particles.ParticleTypes.EFFECT;

		level.sendParticles(dustPillarOptions, targetX, targetY + 0.2, targetZ, totemActive ? 90 : 55, 1.5, 0.6, 1.5, 0.25);
		level.sendParticles(trialSpawnerDetection, targetX, targetY + 0.2, targetZ, totemActive ? 90 : 55, 1.5, 0.6, 1.5, 0.25);

		if (shakeCamera) {
			sendCameraShake(totemActive ? 1.5F : 1.0F, totemActive ? 25 : 15, totemActive ? 30.0F : 20.0F);
		}

		java.util.List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, self.getBoundingBox().inflate(blastRadius), e -> e != self && !isWoodboundEntity(e, self));
		if (target instanceof LivingEntity && !targets.contains((LivingEntity) target) && !isWoodboundEntity(target, self)) {
			targets.add((LivingEntity) target);
		}
		for (LivingEntity victim : targets) {
			if (isWoodboundEntity(victim, self)) continue;
			disablePlayerShield(victim, 100);
			String dmgType = self.getPersistentData().getDouble(K_SDKP) > 0 ? "rot_die_rider_kick" : "rot_seismic_slam";
			dealTrueDamageToBosses(victim, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:" + dmgType))), self), blastDamage * (float) getAdaptationMultiplier(self));
			Vec3 push = victim.position().subtract(self.position()).multiply(1.0, 0.0, 1.0);
			double horizontalDist = Math.sqrt(push.x * push.x + push.z * push.z);
			
			if (horizontalDist < 0.1) {
				push = self.getLookAngle().multiply(1.0, 0.0, 1.0);
				horizontalDist = Math.sqrt(push.x * push.x + push.z * push.z);
			}
			if (horizontalDist > 0.01) {
				push = new Vec3(push.x / horizontalDist, 0, push.z / horizontalDist);
			} else {
				push = new Vec3(1, 0, 0);
			}
			double pushMult = totemActive ? 5.5 : 3.5;
			double pushUp = totemActive ? 1.4 : 0.95;
			if (isInfinity) {
				pushMult = 9.5;
				pushUp = 2.2;
			}
			applyKnockbackAndSync(victim, push.x * pushMult, pushUp, push.z * pushMult);
		}
	}

	private static double findGroundY(LevelAccessor world, Entity entity) {
		BlockPos pos = entity.blockPosition();
		int minY = world.getMinBuildHeight();
		int startY = pos.getY();
		int maxDist = Math.max(64, startY - minY + 2);
		for (int i = 0; i < maxDist; i++) {
			BlockPos belowPos = pos.below(i);
			if (belowPos.getY() < minY) {
				return minY + 1.0;
			}
			BlockState state = world.getBlockState(belowPos);
			if (state.isCollisionShapeFullBlock(world, belowPos) || !state.getFluidState().isEmpty()) {
				return belowPos.getY() + 1.0;
			}
		}
		return minY + 1.0;
	}

	private static boolean isHighAboveGround(Entity entity, double height) {
		LevelAccessor world = entity.level();
		BlockPos pos = entity.blockPosition();
		for (int i = 1; i <= (int) Math.ceil(height); i++) {
			BlockState state = world.getBlockState(pos.below(i));
			if (state.isCollisionShapeFullBlock(world, pos.below(i))) {
				return false;
			}
		}
		return true;
	}

	private static void breakBlocksBehindTarget(LevelAccessor world, Entity target, Vec3 push, boolean totemActive) {
		if (!(world instanceof ServerLevel level) || target == null) return;
		double height = target.getBbHeight();
		Vec3 targetPos = target.position();
		Vec3 behindDir = push.normalize();
		double[] distances = totemActive ? new double[]{0.8, 1.4, 2.0, 2.6, 3.2, 3.8} : new double[]{0.8, 1.4};
		for (double dist : distances) {
			Vec3 pathPos = targetPos.add(behindDir.scale(dist));
			for (double dy = 0; dy < height + 0.5; dy += 0.9) {
				if (totemActive) {
					for (int dx = -1; dx <= 1; dx++) {
						for (int dz = -1; dz <= 1; dz++) {
							BlockPos bp = BlockPos.containing(pathPos.x + dx, pathPos.y + dy, pathPos.z + dz);
							BlockState state = level.getBlockState(bp);
							float hardness = state.getDestroySpeed(level, bp);
							if (hardness >= 0.0F && hardness <= 50.0F && !state.isAir()) {
								level.destroyBlock(bp, false);
								level.sendParticles(ParticleTypes.EXPLOSION, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0.02);
							}
						}
					}
				} else {
					BlockPos bp = BlockPos.containing(pathPos.x, pathPos.y + dy, pathPos.z);
					BlockState state = level.getBlockState(bp);
					float hardness = state.getDestroySpeed(level, bp);
					if (hardness >= 0.0F && hardness <= 50.0F && !state.isAir()) {
						level.destroyBlock(bp, false);
						level.sendParticles(ParticleTypes.CRIT, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.02);
					}
				}
			}
		}
	}

	private static void sendCameraShake(float intensity, int duration, float range) {
		try {
			Class<?> msgClass = Class.forName("com.github.alexmodguy.citadel.server.message.CameraShakeMessage");
			java.lang.reflect.Constructor<?> constructor = msgClass.getConstructor(float.class, int.class, float.class);
			Object msgInstance = constructor.newInstance(intensity, duration, range);

			Class<?> citadelClass = Class.forName("com.github.alexmodguy.citadel.Citadel");
			java.lang.reflect.Method sendMethod = citadelClass.getMethod("sendMSGToAll", Object.class);
			sendMethod.invoke(null, msgInstance);
		} catch (Exception e) {
		}
	}

	private static boolean handleScanningState(Entity entity) {
		double scanningTicks = entity.getPersistentData().getDouble(K_SST4);
		if (scanningTicks > 0) {
			entity.getPersistentData().putDouble(K_SST4, scanningTicks - 1.0);
			entity.setDeltaMovement(0, entity.getDeltaMovement().y(), 0);
			if (entity instanceof Mob mob) {
				mob.getNavigation().stop();
				
				double baseYaw = entity.getPersistentData().getDouble(K_SSBY);
				double maxTicks = entity.getPersistentData().getDouble(K_SSMT);
				if (maxTicks <= 0.1) maxTicks = 60.0;
				double elapsed = maxTicks - scanningTicks;
				double fraction = elapsed / maxTicks;

				double targetOffset = 0;
				double targetPitch = 0;
				float lerpFactor = 0.18F;

				if (getRotPersistentBoolean(entity, K_SRTA, false)) {
					targetOffset = 0;
					targetPitch = entity.getPersistentData().getDouble("sentinel_scan_target_pitch");
					lerpFactor = 0.35F;
				} else {
					if (fraction < 0.25) {
						targetOffset = 0;
						targetPitch = 2.0;
					} else if (fraction < 0.55) {
						targetOffset = -55.0;
						targetPitch = -8.0;
					} else if (fraction < 0.85) {
						targetOffset = 55.0;
						targetPitch = 12.0;
					} else {
						targetOffset = 0;
						targetPitch = 0;
					}
				}

				float currentY = mob.getYRot();
				float targetY = (float) (baseYaw + targetOffset);
				float lerpedY = Mth.rotLerp(lerpFactor, currentY, targetY);
				
				mob.setYRot(lerpedY);
				mob.setYHeadRot(lerpedY);
				mob.yBodyRot = lerpedY;

				float currentX = mob.getXRot();
				float targetX = (float) targetPitch;
				float lerpedX = Mth.lerp(lerpFactor, currentX, targetX);
				mob.setXRot(lerpedX);
				
				double lx = mob.getX() - Math.sin(Math.toRadians(lerpedY)) * 5.0;
				double ly = mob.getEyeY() + Math.sin(Math.toRadians(-lerpedX)) * 5.0;
				double lz = mob.getZ() + Math.cos(Math.toRadians(lerpedY)) * 5.0;
				mob.getLookControl().setLookAt(lx, ly, lz, 45.0F, 45.0F);
			}
			return true;
		}
		entity.getPersistentData().putBoolean(K_SRTA, false);
		return false;
	}

	private static boolean handleCustomCombos(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		double cc1 = entity.getPersistentData().getDouble(K_SCS2);
		double cc2 = entity.getPersistentData().getDouble(K_SCS);
		double cc3 = entity.getPersistentData().getDouble(K_SCS3);
		double cc4 = entity.getPersistentData().getDouble(K_SCS4);
		double cc5 = entity.getPersistentData().getDouble(K_SCS5);
		double slamPhase = entity.getPersistentData().getDouble(K_SSP);
		double judgmentTicks = entity.getPersistentData().getDouble(K_SJT);
		double dieKickPhase = entity.getPersistentData().getDouble(K_SDKP);

		if (cc1 <= 0 && cc2 <= 0 && cc3 <= 0 && cc4 <= 0 && cc5 <= 0 && slamPhase <= 0 && judgmentTicks <= 0 && dieKickPhase <= 0) {
			entity.getPersistentData().putInt(K_SCTI, 0);
			return false;
		}

		if (combatTarget == null || !combatTarget.isAlive() || combatTarget.isRemoved()) {
			entity.getPersistentData().putDouble(K_SCS2, 0);
			entity.getPersistentData().putDouble(K_SCS, 0);
			entity.getPersistentData().putDouble(K_SCS3, 0);
			entity.getPersistentData().putDouble(K_SCS4, 0);
			entity.getPersistentData().putDouble(K_SCS5, 0);
			entity.getPersistentData().putBoolean(K_IU, false);
			entity.getPersistentData().putBoolean(K_IUL, false);
			entity.getPersistentData().putBoolean(K_IUR, false);
			entity.getPersistentData().putBoolean(K_IUS, false);
			entity.getPersistentData().putInt(K_SCTI, 0);
			cleanupCombatFlags(entity);
			return false;
		}

		int storedId = entity.getPersistentData().getInt(K_SCTI);
		if (storedId == 0) {
			entity.getPersistentData().putInt(K_SCTI, combatTarget != null ? combatTarget.getId() : 0);
		}

		if (entity.getPersistentData().getDouble(K_SSP) > 0 
			|| entity.getPersistentData().getDouble(K_SJT) > 0 
			|| entity.getPersistentData().getDouble(K_SDKP) > 0) {
			return false;
		}

		if (combatTarget != null) {
			lockLookAtTarget(entity, combatTarget);
		}

		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
		}

		if ((cc1 == 1 || cc2 == 1 || cc3 == 1 || cc4 == 1 || cc5 == 1) && entity.distanceTo(combatTarget) > 5.0) {
			if (world instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getY() + 1.0, entity.getZ(), 20, 0.4, 0.8, 0.4, 0.1);
				playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.enderman.teleport", 1.2F, 0.5F);
			}
			
			Vec3 dir = combatTarget.position().subtract(entity.position()).normalize();
			double targetX = combatTarget.getX() - dir.x * 2.5;
			double targetY = combatTarget.getY();
			double targetZ = combatTarget.getZ() - dir.z * 2.5;
			entity.teleportTo(targetX, targetY, targetZ);
			
			if (world instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, targetX, targetY + 1.0, targetZ, 20, 0.4, 0.8, 0.4, 0.1);
				playHostileSound(level, targetX, targetY, targetZ, "entity.enderman.teleport", 1.2F, 0.55F);
			}
		}

		boolean totemActive = getRotPersistentBoolean(entity, K_STA2, false);

		if (cc1 > 0) {
			double cc1Ticks = entity.getPersistentData().getDouble(K_SCT3);
			if (cc1 == 1) {
				entity.setDeltaMovement(0.0, 1.9, 0.0);
				entity.hasImpulse = true;
				if (world instanceof ServerLevel level) {
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.iron_golem.attack", 1.5F, 0.8F);
					level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
				}

				entity.getPersistentData().putDouble(K_SSP, 1);
				entity.getPersistentData().putDouble(K_SST2, 22);

				entity.getPersistentData().putDouble(K_SCS2, 2);
				entity.getPersistentData().putDouble(K_SCT3, 2);
			} else if (cc1 == 2) {
				slamPhase = entity.getPersistentData().getDouble(K_SSP);
				if (slamPhase == 0) {
					entity.getPersistentData().putDouble(K_SJT, 60);
					
					entity.getPersistentData().putDouble(K_SLT, 0);

					entity.getPersistentData().putDouble(K_SCS2, 3);
					entity.getPersistentData().putDouble(K_SCT3, 40);
				}
			} else if (cc1 == 3) {
				judgmentTicks = entity.getPersistentData().getDouble(K_SJT);
				if (judgmentTicks == 0) {
					Vec3 pushVec = combatTarget.position().subtract(entity.position()).normalize();
					double pushForce = totemActive ? 3.5 : 2.5;
					applyKnockbackAndSync(combatTarget, pushVec.x * pushForce, 1.45, pushVec.z * pushForce);

					entity.getPersistentData().putDouble(K_SCS2, 4);
					entity.getPersistentData().putDouble(K_SCT3, 20);
				}
			} else if (cc1 == 4) {
				if (cc1Ticks == 0) {
					entity.setDeltaMovement(0.0, 1.95, 0.0);
					entity.hasImpulse = true;

					entity.getPersistentData().putDouble(K_SDKP, 1);
					entity.getPersistentData().putDouble(K_SDKT, 22);
					entity.getPersistentData().putDouble(K_SLT, 0);

					if (world instanceof ServerLevel level) {
						playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.iron_golem.attack", 1.5F, 0.8F);
						level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
					}
					entity.getPersistentData().putDouble(K_SCS2, 5);
				}
			} else if (cc1 == 5) {
				dieKickPhase = entity.getPersistentData().getDouble(K_SDKP);
				double landingTicks = entity.getPersistentData().getDouble(K_SLT);
				if (dieKickPhase == 0 && landingTicks == 0) {
					double cd = totemActive ? COMBO_TRIPLE_THREAT_CD_TOTEM : COMBO_TRIPLE_THREAT_CD;
					entity.getPersistentData().putDouble(K_SCC, cd);
					entity.getPersistentData().putDouble(K_SCS2, 0);
				}
				handlePassengerAndGrowth(entity);
			}
			handlePassengerAndGrowth(entity);
			return true;
		}

		if (cc2 > 0) {
			double cc2Ticks = entity.getPersistentData().getDouble(K_SCT2);
			double cc2Air = entity.getPersistentData().getDouble(K_SCAT2);

			if (cc2 == 1) {
				setRotPersistentBoolean(entity, K_IB, false);
				setRotPersistentDouble(entity, K_RBAT, 0);
				if (cc2Ticks == 0.0) {
					boolean isDiveCounter = getRotPersistentBoolean(entity, K_SDCA, false);
					if (isDiveCounter) {
						setRotPersistentBoolean(entity, K_SDCA, false);
					} else if (combatTarget != null) {
						Vec3 look = combatTarget.getLookAngle().normalize();
						double tx = combatTarget.getX() + look.x * 1.8;
						double tz = combatTarget.getZ() + look.z * 1.8;
						double ty = findTargetGroundY(world, tx, combatTarget.getY(), tz);
						
						teleportEntity(entity, tx, ty, tz);
						lockLookAtTarget(entity, combatTarget);
						
						if (world instanceof ServerLevel level) {
							playTeleportEffects(level, entity, entity.getX(), entity.getY(), entity.getZ());
							playTeleportEffects(level, entity, tx, ty, tz);
							playHostileSound(level, tx, ty, tz, "entity.warden.snarl", 1.5F, 1.3F);
						}
					}
					
					entity.getPersistentData().putDouble(K_SCT2, UPPERCUT_TOTAL_TICKS);
					entity.getPersistentData().putBoolean(K_IU, true);
					boolean isLeftUppercut = Math.random() < 0.5;
					entity.getPersistentData().putBoolean(K_IUL, isLeftUppercut);
					entity.getPersistentData().putBoolean(K_IUR, !isLeftUppercut);
					entity.getPersistentData().putBoolean(K_SCL, false);
				} else {
					if (combatTarget != null) {
						lockLookAtTarget(entity, combatTarget);
					}
					
					boolean launched = getRotPersistentBoolean(entity, K_SCL, false);
					if (cc2Ticks <= UPPERCUT_LAUNCH_TICK && !launched) {
						entity.getPersistentData().putBoolean(K_SCL, true);
						if (entity instanceof LivingEntity ls) ls.swing(InteractionHand.MAIN_HAND, true);
						
						if (combatTarget != null) {
							if (entity.distanceTo(combatTarget) > 2.5) {
								boolean isDiveCounter = getRotPersistentBoolean(entity, K_SDCA, false);
								double tx, tz;
								if (isDiveCounter || combatTarget.getY() - entity.getY() > 2.0) {
									tx = combatTarget.getX();
									tz = combatTarget.getZ();
								} else {
									Vec3 look = combatTarget.getLookAngle().normalize();
									tx = combatTarget.getX() + look.x * 1.5;
									tz = combatTarget.getZ() + look.z * 1.5;
								}
								double ty = findTargetGroundY(world, tx, combatTarget.getY(), tz);
								teleportEntity(entity, tx, ty, tz);
							}

							boolean isTargetBlocking = (combatTarget instanceof LivingEntity liv && liv.isBlocking());
							if (world instanceof ServerLevel level) {
								try {
									dealTrueDamageToBosses(combatTarget, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_uppercut"))), entity), (float) UPPERCUT_DAMAGE * (float) getAdaptationMultiplier(entity));
								} catch (Exception e) {
									dealTrueDamageToBosses(combatTarget, entity instanceof LivingEntity ls ? ls.damageSources().mobAttack(ls) : new DamageSource(world.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) UPPERCUT_DAMAGE * (float) getAdaptationMultiplier(entity));
								}
							} else {
								dealTrueDamageToBosses(combatTarget, entity instanceof LivingEntity ls ? ls.damageSources().mobAttack(ls) : new DamageSource(world.holderOrThrow(DamageTypes.MOB_ATTACK)), (float) UPPERCUT_DAMAGE * (float) getAdaptationMultiplier(entity));
							}
							
							if (isTargetBlocking) {
								Vec3 knockDir = combatTarget.position().subtract(entity.position()).normalize();
								applyKnockbackAndSync(combatTarget, knockDir.x * 1.5, 0.60, knockDir.z * 1.5);
							} else if (combatTarget.isAlive()) {
								double launchHeightVel = 2.8 + Math.random() * 1.0;
								applyKnockbackAndSync(combatTarget, 0.0, launchHeightVel, 0.0);

								ClearFlightPathProcedure.execute(world, combatTarget, combatTarget.getY(), combatTarget.getY() + 35.0, 1.5);
								ClearFlightPathProcedure.execute(world, entity, entity.getY(), entity.getY() + 35.0, 2.0);
							} else {
								entity.getPersistentData().putBoolean(K_SUD, true);
							}
						} else {
							entity.getPersistentData().putBoolean(K_SUD, true);
						}

						if (world instanceof ServerLevel level) {
							if (combatTarget != null) {
								level.sendParticles(ParticleTypes.CLOUD, combatTarget.getX(), combatTarget.getY() + 0.2, combatTarget.getZ(), 15, 0.4, 0.4, 0.4, 0.15);
								level.sendParticles(ParticleTypes.EXPLOSION, combatTarget.getX(), combatTarget.getY() + 0.5, combatTarget.getZ(), 3, 0.2, 0.2, 0.2, 0.1);
							}
							playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.iron_golem.attack", 1.8F, 0.6F);
							playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.generic.explode", 1.2F, 1.4F);
						}
					}
					
					boolean targetDodged = getRotPersistentBoolean(entity, K_SUD, false);
					if (cc2Ticks <= 1 || targetDodged) {
						entity.getPersistentData().putBoolean(K_IU, false);
						entity.getPersistentData().putBoolean(K_IUL, false);
						entity.getPersistentData().putBoolean(K_IUR, false);
						entity.getPersistentData().putBoolean(K_SUD, false);

						if (entity instanceof RotEntity rot) {
							rot.getEntityData().set(RotEntity.DATA_is_uppercutting, false);
							rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_left, false);
							rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_right, false);

						}

						if (targetDodged || getRotPersistentBoolean(entity, K_IUS, false)) {
							entity.getPersistentData().putBoolean(K_IUS, false);
							entity.getPersistentData().putDouble(K_SCS, 0);
							entity.getPersistentData().putDouble(K_SCT2, 0);
							if (targetDodged) {
								entity.getPersistentData().putDouble(K_SCC2, 60.0);
							}
						} else {
							entity.getPersistentData().putDouble(K_SCAT2, 1);
							entity.getPersistentData().putDouble(K_SCS, 2);
						}
					}
				}
			} else if (cc2 == 2) {
				cc2Air = entity.getPersistentData().getDouble(K_SCAT2);
				entity.getPersistentData().putDouble(K_SCAT2, cc2Air + 1);
				
				if (cc2Air > 40 || combatTarget == null || !combatTarget.isAlive()) {
					entity.getPersistentData().putDouble(K_SCS, 0);
					entity.getPersistentData().putDouble(K_SCAT2, 0);
					cleanupCombatFlags(entity);
					return false;
				}
				
				if (cc2Air == 1) {
					double leapVel = 2.0;
					entity.setDeltaMovement(entity.getDeltaMovement().x(), leapVel, entity.getDeltaMovement().z());
					entity.hasImpulse = true;
					if (world instanceof ServerLevel level) {
						playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.iron_golem.attack", 1.2F, 0.7F);
						level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.2, entity.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
					}
				}
				
				lockLookAtTarget(entity, combatTarget);
				
				if (cc2Air >= 15 && combatTarget.getDeltaMovement().y() <= 0.1) {
					entity.teleportTo(combatTarget.getX(), combatTarget.getY() + 1.5, combatTarget.getZ());
					entity.setDeltaMovement(0.0, -0.1, 0.0);

					entity.getPersistentData().putDouble(K_ROT, OVERHEAD_TOTAL_TICKS);
					entity.getPersistentData().putString(K_OTU, combatTarget.getUUID().toString());

					entity.getPersistentData().putBoolean(K_ILP, false);
					entity.getPersistentData().putBoolean(K_IRP, false);
					if (entity instanceof RotEntity rot) {
						rot.getEntityData().set(RotEntity.DATA_is_left_punching, false);
						rot.getEntityData().set(RotEntity.DATA_is_right_punching, false);
					}

					entity.getPersistentData().putDouble(K_SCS, 3);
				}
			} else if (cc2 == 3) {
				if (combatTarget == null || !combatTarget.isAlive()) {
					entity.getPersistentData().putDouble(K_SCS, 0);
					entity.getPersistentData().putDouble(K_ROT, 0);
					return true;
				}

				handleOverheadState(world, entity, combatTarget);

				double overheadTicks = entity.getPersistentData().getDouble(K_ROT);
				if (overheadTicks <= 0) {
					boolean isInfinity = getRotPersistentBoolean(entity, K_SIIT, false);
					double blastRadius = totemActive ? 12.0 : 6.5;
					if (isInfinity) blastRadius = 15.0;
					blastRadius *= getAdaptationMultiplier(entity);
					double hDist = Math.sqrt(Math.pow(entity.getX() - combatTarget.getX(), 2) + Math.pow(entity.getZ() - combatTarget.getZ(), 2));

					if (hDist <= blastRadius) {
						entity.setDeltaMovement(0.0, 1.9, 0.0);
						entity.hasImpulse = true;
						if (world instanceof ServerLevel level) {
							playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.iron_golem.attack", 1.5F, 0.8F);
							level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
						}
						entity.getPersistentData().putDouble(K_SSP, 1);
						entity.getPersistentData().putDouble(K_SST2, 22);
					} else {
						entity.getPersistentData().putDouble(K_SDKP, 1);
						entity.getPersistentData().putDouble(K_SDKT, 22);
						entity.setDeltaMovement(0.0, 1.95, 0.0);
						entity.hasImpulse = true;
						if (world instanceof ServerLevel level) {
							playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.iron_golem.attack", 1.5F, 0.8F);
							level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
						}
					}

					double cd = totemActive ? COMBO_HIGH_SKY_SLAM_CD_TOTEM : COMBO_HIGH_SKY_SLAM_CD;
					entity.getPersistentData().putDouble(K_SCC2, cd);
					entity.getPersistentData().putDouble(K_SCS, 0);
				}
			}
			handlePassengerAndGrowth(entity);
			return true;
		}

		if (cc3 > 0) {
			double cc3Ticks = entity.getPersistentData().getDouble(K_SCT4);
			if (cc3 == 1) {
				if (entity instanceof LivingEntity ls) ls.swing(InteractionHand.MAIN_HAND, true);
				
				boolean lastHandLeft = getRotPersistentBoolean(entity, K_SPHT, false);
				entity.getPersistentData().putBoolean(K_SPHT, !lastHandLeft);
				if (lastHandLeft) {
					entity.getPersistentData().putDouble(K_SHLPT, HEAVY_PUNCH_TOTAL_TICKS);
					entity.getPersistentData().putDouble(K_SHRPT, 0);
				} else {
					entity.getPersistentData().putDouble(K_SHRPT, HEAVY_PUNCH_TOTAL_TICKS);
					entity.getPersistentData().putDouble(K_SHLPT, 0);
				}
				entity.getPersistentData().putDouble(K_SLPT, 0);
				entity.getPersistentData().putDouble(K_SRPT2, 0);

				entity.getPersistentData().putDouble(K_SCT4, HEAVY_PUNCH_TOTAL_TICKS + 5);
				entity.getPersistentData().putDouble(K_SCS3, 2);
			} else if (cc3 == 2) {
				if (cc3Ticks == 0) {
					if (combatTarget != null && entity.distanceTo(combatTarget) >= 5.0) {
						entity.getPersistentData().putDouble(K_SJT, 60);
						entity.getPersistentData().putDouble(K_SCS3, 3);
					} else {
						if (combatTarget instanceof LivingEntity targetLiv) {
							executeMinosHeavyPunchBlink(world, entity, targetLiv, false);
						}
						entity.getPersistentData().putDouble(K_SCS3, 0);
					}
				}
			} else if (cc3 == 3) {
				judgmentTicks = entity.getPersistentData().getDouble(K_SJT);
				double landingTicks = entity.getPersistentData().getDouble(K_SLT);
				double riderHoldTicks = entity.getPersistentData().getDouble(K_SRHT);
				if (judgmentTicks == 0 && landingTicks == 0 && riderHoldTicks == 0) {
					double cd = totemActive ? COMBO_PUNCH_DROPKICK_CD_TOTEM : COMBO_PUNCH_DROPKICK_CD;
					entity.getPersistentData().putDouble(K_SCC3, cd);
					entity.getPersistentData().putDouble(K_SCS3, 0);
				}
				handlePassengerAndGrowth(entity);
			}
			handlePassengerAndGrowth(entity);
			return true;
		}

		if (cc4 > 0) {
			double cc4Ticks = entity.getPersistentData().getDouble(K_SCT5);
			if (cc4 == 1) {
				if (entity instanceof LivingEntity ls) ls.swing(InteractionHand.MAIN_HAND, true);
				
				boolean lastHandLeft = getRotPersistentBoolean(entity, K_SPHT, false);
				entity.getPersistentData().putBoolean(K_SPHT, !lastHandLeft);
				if (lastHandLeft) {
					entity.getPersistentData().putDouble(K_SHLPT, HEAVY_PUNCH_TOTAL_TICKS);
					entity.getPersistentData().putDouble(K_SHRPT, 0);
				} else {
					entity.getPersistentData().putDouble(K_SHRPT, HEAVY_PUNCH_TOTAL_TICKS);
					entity.getPersistentData().putDouble(K_SHLPT, 0);
				}
				entity.getPersistentData().putDouble(K_SLPT, 0);
				entity.getPersistentData().putDouble(K_SRPT2, 0);

				entity.getPersistentData().putDouble(K_SCT5, HEAVY_PUNCH_TOTAL_TICKS + 5);
				entity.getPersistentData().putDouble(K_SCS4, 2);
			} else if (cc4 == 2) {
				if (cc4Ticks == 0) {
					if (combatTarget != null && entity.distanceTo(combatTarget) >= 5.0) {
						entity.setDeltaMovement(0.0, 1.95, 0.0);
						entity.hasImpulse = true;

						entity.getPersistentData().putDouble(K_SDKP, 1);
						entity.getPersistentData().putDouble(K_SDKT, 22);
						entity.getPersistentData().putDouble(K_SLT, 0);

						if (world instanceof ServerLevel level) {
							playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.iron_golem.attack", 1.5F, 0.8F);
							level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
						}
						entity.getPersistentData().putDouble(K_SCS4, 3);
					} else {
						if (combatTarget instanceof LivingEntity targetLiv) {
							executeMinosHeavyPunchBlink(world, entity, targetLiv, false);
						}
						entity.getPersistentData().putDouble(K_SCS4, 0);
					}
				}
			} else if (cc4 == 3) {
				dieKickPhase = entity.getPersistentData().getDouble(K_SDKP);
				double landingTicks = entity.getPersistentData().getDouble(K_SLT);
				if (dieKickPhase == 0 && landingTicks == 0) {
					double cd = totemActive ? COMBO_PUNCH_RIDER_KICK_CD_TOTEM : COMBO_PUNCH_RIDER_KICK_CD;
					entity.getPersistentData().putDouble(K_SCC4, cd);
					entity.getPersistentData().putDouble(K_SCS4, 0);
				}
				handlePassengerAndGrowth(entity);
			}
			handlePassengerAndGrowth(entity);
			return true;
		}

		if (cc5 > 0) {
			double cc5Ticks = entity.getPersistentData().getDouble(K_SCT6);
			if (cc5 == 1) {
				if (entity instanceof LivingEntity ls) ls.swing(InteractionHand.MAIN_HAND, true);
				
				Vec3 knockDir = combatTarget.position().subtract(entity.position()).normalize();
				double launchHeightVel = 1.35;
				applyKnockbackAndSync(combatTarget, knockDir.x * 4.4, launchHeightVel, knockDir.z * 4.4);
				ClearFlightPathProcedure.execute(world, combatTarget, combatTarget.getY(), combatTarget.getY() + 20.0, 1.5);

				if (world instanceof ServerLevel level) dealTrueDamageToBosses(combatTarget, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_consecutive_punches"))), entity), (float) MELEE_PUNCH_DAMAGE * (float) getAdaptationMultiplier(entity));

				if (world instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.CRIT, combatTarget.getX(), combatTarget.getY() + 0.8, combatTarget.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
					level.sendParticles(ParticleTypes.SWEEP_ATTACK, combatTarget.getX(), combatTarget.getY() + 1.0, combatTarget.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
					level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, combatTarget.getX(), combatTarget.getY() + 1.0, combatTarget.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.iron_golem.attack", 1.6F, 0.45F);
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.generic.explode", 0.8F, 0.45F);
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sonic_boom", 0.6F, 0.4F);
				}

				entity.getPersistentData().putDouble(K_SCT6, 15);
				entity.getPersistentData().putDouble(K_SCS5, 2);
			} else if (cc5 == 2) {
				if (cc5Ticks == 0) {
					entity.getPersistentData().putDouble(K_SJT, 60);
					entity.getPersistentData().putDouble(K_SCS5, 3);
				}
			} else if (cc5 == 3) {
				judgmentTicks = entity.getPersistentData().getDouble(K_SJT);
				double landingTicks = entity.getPersistentData().getDouble(K_SLT);
				double riderHoldTicks = entity.getPersistentData().getDouble(K_SRHT);
				if (judgmentTicks == 0 && landingTicks == 0 && riderHoldTicks == 0) {
					if (!combatTarget.onGround() && combatTarget.getY() > findGroundY(world, combatTarget) + 1.0) {
						entity.teleportTo(combatTarget.getX(), combatTarget.getY() + 1.5, combatTarget.getZ());
						entity.setDeltaMovement(0.0, -0.1, 0.0);

						entity.getPersistentData().putDouble(K_ROT, OVERHEAD_TOTAL_TICKS);
						entity.getPersistentData().putString(K_OTU, combatTarget.getUUID().toString());

						entity.getPersistentData().putBoolean(K_ILP, false);
						entity.getPersistentData().putBoolean(K_IRP, false);
						if (entity instanceof RotEntity rot) {
							rot.getEntityData().set(RotEntity.DATA_is_left_punching, false);
							rot.getEntityData().set(RotEntity.DATA_is_right_punching, false);
						}

						entity.getPersistentData().putDouble(K_SCS5, 4);
					} else {
						double cd = totemActive ? COMBO_HEAVENLY_REPENTANCE_PLUS_CD_TOTEM : COMBO_HEAVENLY_REPENTANCE_PLUS_CD;
						entity.getPersistentData().putDouble(K_SCC5, cd);
						entity.getPersistentData().putDouble(K_SCS5, 0);
					}
				}
			} else if (cc5 == 4) {
				handleOverheadState(world, entity, combatTarget);

				double overheadTicks = entity.getPersistentData().getDouble(K_ROT);
				if (overheadTicks <= 0) {
					if (combatTarget.onGround() || combatTarget.getY() <= entity.getY() - 1.5) {
						boolean isInfinity = getRotPersistentBoolean(entity, K_SIIT, false);
						double blastRadius = totemActive ? 12.0 : 6.5;
						if (isInfinity) blastRadius = 15.0;
						blastRadius *= getAdaptationMultiplier(entity);
						double hDist = Math.sqrt(Math.pow(entity.getX() - combatTarget.getX(), 2) + Math.pow(entity.getZ() - combatTarget.getZ(), 2));

						if (hDist <= blastRadius) {
							entity.setDeltaMovement(0.0, 1.9, 0.0);
							entity.hasImpulse = true;
							if (world instanceof ServerLevel level) {
								playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.iron_golem.attack", 1.5F, 0.8F);
								level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
							}
							entity.getPersistentData().putDouble(K_SSP, 1);
							entity.getPersistentData().putDouble(K_SST2, 22);
						} else {
							entity.getPersistentData().putDouble(K_SDKP, 1);
							entity.getPersistentData().putDouble(K_SDKT, 22);
							entity.setDeltaMovement(0.0, 1.95, 0.0);
							entity.hasImpulse = true;
							if (world instanceof ServerLevel level) {
								playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.iron_golem.attack", 1.5F, 0.8F);
								level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
							}
						}

						double cd = totemActive ? COMBO_HEAVENLY_REPENTANCE_PLUS_CD_TOTEM : COMBO_HEAVENLY_REPENTANCE_PLUS_CD;
						entity.getPersistentData().putDouble(K_SCC5, cd);
						entity.getPersistentData().putDouble(K_SCS5, 0);
					}
				}
			}
			handlePassengerAndGrowth(entity);
			return true;
		}

		return false;
	}

	private static boolean isIndestructibleArmorStack(ItemStack stack) {
		if (stack.isEmpty()) return false;
		if (stack.getMaxDamage() <= 0) return true;
		if (!stack.isDamageableItem()) return true;
		try {
			if (stack.has(net.minecraft.core.component.DataComponents.UNBREAKABLE)) return true;
		} catch (Throwable t) {}
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(java.util.Locale.ROOT);
		String ns = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().toLowerCase(java.util.Locale.ROOT);
		if (ns.equals("projecte") || ns.equals("avaritia") || ns.equals("mekanism") || ns.equals("cataclysm") || ns.equals("draconicevolution") || ns.equals("botania") || ns.equals("bloodmagic") || ns.equals("enigmaticlegacy")) {
			return true;
		}
		if (id.contains("infinity") || id.contains("indestructible") || id.contains("mekasuit") || id.contains("draconic") || id.contains("unbreakable") || id.contains("creative") || id.contains("quantum") || id.contains("bound")) {
			return true;
		}
		return false;
	}

	private static void executeArmorRipChoke(LevelAccessor world, Entity entity, @Nullable Entity combatTarget, int tickRemaining) {
		if (combatTarget == null || !combatTarget.isAlive()) {
			entity.getPersistentData().putDouble(K_RART, 0);
			entity.getPersistentData().putBoolean(K_IAR, false);
			return;
		}

		if (combatTarget.level() != entity.level() || combatTarget.position().distanceTo(entity.position()) > ARMOR_RIP_MAX_DISTANCE) {
			entity.getPersistentData().putDouble(K_RART, 0);
			entity.getPersistentData().putBoolean(K_IAR, false);
			return;
		}

		double lockedYaw = entity.getPersistentData().getDouble(K_RCLY);
		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
			mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
			mob.setYRot((float) lockedYaw);
			mob.setYHeadRot((float) lockedYaw);
			mob.yBodyRot = (float) lockedYaw;
		}

		if (combatTarget instanceof LivingEntity livTarget) {
			livTarget.setYRot((float) (lockedYaw + 180.0));
			livTarget.setYHeadRot((float) (lockedYaw + 180.0));
			livTarget.yBodyRot = (float) (lockedYaw + 180.0);
		} else {
			combatTarget.setYRot((float) (lockedYaw + 180.0));
		}

		double dx = -Math.sin(Math.toRadians(lockedYaw));
		double dz = Math.cos(Math.toRadians(lockedYaw));
		double right_dx = Math.cos(Math.toRadians(lockedYaw));
		double right_dz = Math.sin(Math.toRadians(lockedYaw));

		double holdDistance = ARMOR_RIP_HOLD_DISTANCE;
		double rightOffset = ARMOR_RIP_RIGHT_OFFSET;
		double targetHoldX = entity.getX() + dx * holdDistance + right_dx * rightOffset;
		double targetHoldY = entity.getY() + ARMOR_RIP_HEIGHT_OFFSET;
		double targetHoldZ = entity.getZ() + dz * holdDistance + right_dz * rightOffset;

		combatTarget.teleportTo(targetHoldX, targetHoldY, targetHoldZ);
		combatTarget.setDeltaMovement(0, 0, 0);
		combatTarget.fallDistance = 0.0F;

		if (combatTarget instanceof Player chokePlayer) {
			boolean chokeTotemLearned = getRotPersistentBoolean(entity, K_STL, false);
			net.minecraft.world.item.ItemStack chokeTotem = net.minecraft.world.item.ItemStack.EMPTY;
			boolean chokeIsInfinity = false;
			net.minecraft.world.item.ItemStack chokeMain = chokePlayer.getMainHandItem();
			net.minecraft.world.item.ItemStack chokeOff = chokePlayer.getOffhandItem();
			if (!chokeMain.isEmpty()) {
				String id = BuiltInRegistries.ITEM.getKey(chokeMain.getItem()).toString();
				if (chokeMain.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING || id.equals("avaritia:infinity_totem")) {
					chokeTotem = chokeMain;
					chokeIsInfinity = id.equals("avaritia:infinity_totem");
				}
			}
			if (chokeTotem.isEmpty() && !chokeOff.isEmpty()) {
				String id = BuiltInRegistries.ITEM.getKey(chokeOff.getItem()).toString();
				if (chokeOff.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING || id.equals("avaritia:infinity_totem")) {
					chokeTotem = chokeOff;
					chokeIsInfinity = id.equals("avaritia:infinity_totem");
				}
			}
			if (!chokeTotem.isEmpty()) {
				chokeTotemLearned = true;
				entity.getPersistentData().putBoolean(K_STL, true);
			}
			if (chokeTotem.isEmpty() && chokeTotemLearned) {
				for (int slot = 0; slot < chokePlayer.getInventory().getContainerSize(); slot++) {
					net.minecraft.world.item.ItemStack s = chokePlayer.getInventory().getItem(slot);
					if (!s.isEmpty()) {
						String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
						if (s.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING || id.equals("avaritia:infinity_totem")) {
							chokeTotem = s;
							chokeIsInfinity = id.equals("avaritia:infinity_totem");
							break;
						}
					}
				}
			}

			if (!chokeTotem.isEmpty()) {
				chokeTotem.shrink(1);
				entity.getPersistentData().putBoolean(K_SIIT, chokeIsInfinity);
				entity.getPersistentData().putBoolean(K_STS, true);
				entity.getPersistentData().putBoolean(K_SJST, true);
				entity.getPersistentData().putBoolean(K_STL, true);
				entity.getPersistentData().putDouble(K_STIT, 180);
				entity.getPersistentData().putDouble(K_RART, 0);
				entity.getPersistentData().putBoolean(K_IAR, false);
				if (world instanceof ServerLevel level) {
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "block.bell.resonate", 1.5F, 0.6F);
				}
				handlePassengerAndGrowth(entity);
				return;
			}
		}

		if (combatTarget instanceof LivingEntity livTarget && livTarget.isBlocking()) {
			if (livTarget instanceof Player player) {
				player.getCooldowns().addCooldown(net.minecraft.world.item.Items.SHIELD, 100);
				player.stopUsingItem();
				if (world instanceof ServerLevel level) {
					level.broadcastEntityEvent(player, (byte) 30);
				}
			} else {
				livTarget.stopUsingItem();
			}
		}

		if (combatTarget instanceof LivingEntity livTarget) {
			livTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 60, 1, false, false, false));
			try {
				livTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 60, 0, false, false, false));
			} catch (Throwable t) {
				livTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 0, false, false, false));
			}
			java.util.List<net.minecraft.world.entity.EquipmentSlot> equippedArmorSlots = new java.util.ArrayList<>();
			for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
				if (slot.isArmor() && !livTarget.getItemBySlot(slot).isEmpty()) {
					equippedArmorSlots.add(slot);
				}
			}

			if (!equippedArmorSlots.isEmpty()) {
				int randomIndex = net.minecraft.util.RandomSource.create().nextInt(equippedArmorSlots.size());
				net.minecraft.world.entity.EquipmentSlot randomSlot = equippedArmorSlots.get(randomIndex);
				ItemStack armorPiece = livTarget.getItemBySlot(randomSlot);

				boolean isIndestructible = isIndestructibleArmorStack(armorPiece);

				if (isIndestructible) {
					if (tickRemaining % CHOKE_INDESTRUCTIBLE_DROP_INTERVAL == 0) {
						livTarget.setItemSlot(randomSlot, ItemStack.EMPTY);
						if (world instanceof ServerLevel level) {
							net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
								level, livTarget.getX(), livTarget.getY(), livTarget.getZ(), armorPiece
							);
							itemEntity.setPickUpDelay(30);
							level.addFreshEntity(itemEntity);

							level.playSound(null, livTarget.getX(), livTarget.getY(), livTarget.getZ(),
								net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.85F);
							level.playSound(null, livTarget.getX(), livTarget.getY(), livTarget.getZ(),
								net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.85F);

							level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, armorPiece),
								livTarget.getX(), livTarget.getY() + 1.0, livTarget.getZ(), 20, 0.2, 0.2, 0.2, 0.05);
						}
					}
				} else {
					if (tickRemaining % CHOKE_DAMAGE_INTERVAL == 0) {
						if (world instanceof ServerLevel level) {
							if (livTarget instanceof ServerPlayer serverPlayer) {
								armorPiece.hurtAndBreak(CHOKE_ARMOR_DURABILITY_LOSS, level, serverPlayer, (item) -> {
									level.playSound(null, livTarget.getX(), livTarget.getY(), livTarget.getZ(),
										net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.85F);
								});
							} else {
								armorPiece.hurtAndBreak(CHOKE_ARMOR_DURABILITY_LOSS, level, null, (item) -> {
									level.playSound(null, livTarget.getX(), livTarget.getY(), livTarget.getZ(),
										net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.85F);
								});
							}
							level.playSound(null, livTarget.getX(), livTarget.getY(), livTarget.getZ(),
								net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.7F);
						}
					}
				}
			}

			if (tickRemaining % CHOKE_DAMAGE_INTERVAL == 0) {
				if (world instanceof ServerLevel level) {
					try {
						dealTrueDamageToBosses(livTarget, new DamageSource(level.holderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("the_backwoods:rot_choke_rip"))), entity), (float) CHOKE_DAMAGE * (float) getAdaptationMultiplier(entity));
					} catch (Exception e) {
						dealTrueDamageToBosses(livTarget, livTarget.damageSources().mobAttack(entity instanceof LivingEntity le ? le : null), (float) CHOKE_DAMAGE * (float) getAdaptationMultiplier(entity));
					}
					level.playSound(null, livTarget.getX(), livTarget.getY(), livTarget.getZ(),
						net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
					level.playSound(null, livTarget.getX(), livTarget.getY(), livTarget.getZ(),
						net.minecraft.sounds.SoundEvents.PLAYER_HURT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.75F);
					level.sendParticles(ParticleTypes.SQUID_INK, livTarget.getX(), livTarget.getY() + 1.2, livTarget.getZ(), 8, 0.2, 0.3, 0.2, 0.05);
					level.sendParticles(ParticleTypes.DUST_PLUME, livTarget.getX(), livTarget.getY() + 1.0, livTarget.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
				}
			}
		}

		if (tickRemaining == 0) {
			entity.getPersistentData().putBoolean(K_IAR, false);
			combatTarget.setDeltaMovement(combatTarget.getDeltaMovement().x, -0.4, combatTarget.getDeltaMovement().z);
			combatTarget.hurtMarked = true;
		}
	}

	private static boolean shouldIgnoreCombatFilter(Entity entity) {
		if (entity == null) return false;
		if (getRotPersistentBoolean(entity, "has_kill_command_override", false)
			|| entity.getTags().contains("mob_battle")
			|| entity.getTags().contains("mobbattle")
			|| entity.getTags().contains("test")
			|| entity.getTags().contains("ignore_targets")
			|| entity.getTeam() != null
			|| getRotPersistentBoolean(entity, "mob_battle_mode", false)
			|| entity.getPersistentData().contains("MobBattleTarget")
			|| (entity instanceof Mob mob && mob.getTarget() != null && (mob.getTarget().getTags().contains("mob_battle") || mob.getTarget().getTeam() != null))) {
			return true;
		}
		for (String tag : entity.getTags()) {
			String lower = tag.toLowerCase(java.util.Locale.ROOT);
			if (lower.contains("battle") || lower.contains("stick") || lower.contains("target")) {
				return true;
			}
		}
		return false;
	}


	// =========================================================================
	// DELEGATION BRIDGES TO RotBrainProcedure (Maintains 100% Exact In-Game AI Logic)
	// =========================================================================
	public static class CombatContext extends RotBrainProcedure.CombatContext {}
	public static class AbilityInfo extends RotBrainProcedure.AbilityInfo {
		public AbilityInfo(String id, String animKey, double baseDmg, double cdTicks, double minR, double maxR) {
			super(id, animKey, baseDmg, cdTicks, minR, maxR);
		}
	}
	public static class InterceptionPrediction extends RotBrainProcedure.InterceptionPrediction {}
	public static class PersonalityVector extends RotBrainProcedure.PersonalityVector {}
	public static class TacticalNeuralNetwork extends RotBrainProcedure.TacticalNeuralNetwork {}
	public static class WelfordTracker extends RotBrainProcedure.WelfordTracker {}
	public static class PlayerBehaviorTracker extends RotBrainProcedure.PlayerBehaviorTracker {}
	public static class RoleAuction extends RotBrainProcedure.RoleAuction {}
	public static class RotHivemindSavedData extends RotBrainProcedure.RotHivemindSavedData {}
	public static class CombatProfile extends RotBrainProcedure.CombatProfile {}
	public static class UniversalCombatPredictionEngine extends RotBrainProcedure.UniversalCombatPredictionEngine {}

	public static RotBrainProcedure.CombatContext getCombatContext(Entity self, Entity target) {
		return RotBrainProcedure.getCombatContext(self, target);
	}

	public static List<RotBrainProcedure.AbilityInfo> getAvailableAbilities(Entity self) {
		return RotBrainProcedure.getAvailableAbilities(self);
	}

	public static void recordAttack(Entity self, String attackType) {
		RotBrainProcedure.recordAttack(self, attackType);
	}

	public static double getDynamicRangeThreshold(Entity entity, String key, double baseRange, double variance) {
		return RotBrainProcedure.getDynamicRangeThreshold(entity, key, baseRange, variance);
	}

	public static double getEffectiveCombatTicks(Entity entity) {
		return RotBrainProcedure.getEffectiveCombatTicks(entity);
	}

	public static RotBrainProcedure.AbilityInfo getAbilityById(String id) {
		return RotBrainProcedure.getAbilityById(id);
	}

	public static boolean evaluateComboTriggerChance(Entity self, Entity target, RotBrainProcedure.CombatContext ctx) {
		return RotBrainProcedure.evaluateComboTriggerChance(self, target, ctx);
	}

	public static RotBrainProcedure.TargetIntent inferTargetIntent(Entity self, Entity target) {
		return RotBrainProcedure.inferTargetIntent(self, target);
	}

	public static void adaptCapabilitiesToIntent(Entity self, RotBrainProcedure.TargetIntent intent) {
		RotBrainProcedure.adaptCapabilitiesToIntent(self, intent);
	}

	public static boolean isHazardousLocation(LevelAccessor world, double x, double y, double z) {
		return RotBrainProcedure.isHazardousLocation(world, x, y, z);
	}

	public static boolean interceptEnderPearlsPipeline(LevelAccessor world, Entity self, Entity target) {
		return RotBrainProcedure.interceptEnderPearlsPipeline(world, self, target);
	}

	public static RotBrainProcedure.InterceptionPrediction evaluateInterceptionPipeline(Entity self, Entity target, RotBrainProcedure.TargetIntent intent) {
		return RotBrainProcedure.evaluateInterceptionPipeline(self, target, intent);
	}

	public static RotBrainProcedure.InterceptionPrediction evaluateInterception(RotBrainProcedure.AbilityInfo ability, Entity self, Entity target) {
		return RotBrainProcedure.evaluateInterception(ability, self, target);
	}

	public static double scoreAbility(RotBrainProcedure.AbilityInfo ability, RotBrainProcedure.CombatContext ctx, Entity self, Entity target) {
		return RotBrainProcedure.scoreAbility(ability, ctx, self, target);
	}

	public static double getMemoryPenalty(Entity self, String attackType) {
		return RotBrainProcedure.getMemoryPenalty(self, attackType);
	}

	public static int evaluateComboUtility(Entity self, Entity target, RotBrainProcedure.CombatContext ctx, java.util.List<Integer> available) {
		return RotBrainProcedure.evaluateComboUtility(self, target, ctx, available);
	}


	private static boolean isRotChannelingAbility(Entity entity) {
		if (entity == null || entity.getPersistentData() == null) return false;
		return getRotPersistentDouble(entity, K_SSCT, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SSFT, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SCCT, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SCFT, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SWSFT, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SST, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SOSCT, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SSST, 0.0) > 0
			|| getRotPersistentDouble(entity, K_GRAPPLE_TICKS, 0.0) > 0
			|| getRotPersistentDouble(entity, K_TK_TICKS, 0.0) > 0
			|| getRotPersistentBoolean(entity, K_IAR, false)
			|| getRotPersistentDouble(entity, K_RART, 0.0) > 0
			|| getRotPersistentDouble(entity, K_STIT, 0.0) > 0
			|| getRotPersistentBoolean(entity, K_IB, false)
			|| getRotPersistentDouble(entity, K_SJT, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SSWST, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SMT, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SSP, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SDKP, 0.0) > 0
			|| getRotPersistentDouble(entity, K_ROT, 0.0) > 0
			|| isDoingCombo(entity);
	}

	private static void cleanupCombatFlags(Entity entity) {
		if (entity == null || entity.getPersistentData() == null) return;
		stopHostileSound(entity.level(), entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);
		setRotPersistentBoolean(entity, K_IAR, false);
		setRotPersistentDouble(entity, K_RART, 0);
		setRotPersistentDouble(entity, "rot_choke_ticks", 0);
		setRotPersistentBoolean(entity, K_IB, false);
		setRotPersistentDouble(entity, K_RBAT, 0);
		setRotPersistentBoolean(entity, K_IBF, false);
		setRotPersistentDouble(entity, K_GRAPPLE_TICKS, 0);
		setRotPersistentDouble(entity, K_TK_TICKS, 0);
		setRotPersistentDouble(entity, K_SSCT, 0);
		setRotPersistentDouble(entity, K_SSFT, 0);
		setRotPersistentDouble(entity, K_SCCT, 0);
		setRotPersistentDouble(entity, K_SCFT, 0);
		setRotPersistentDouble(entity, K_SWSFT, 0);
		setRotPersistentDouble(entity, K_SST, 0);
		setRotPersistentDouble(entity, K_SOSCT, 0);
		setRotPersistentDouble(entity, K_SSST, 0);
		setRotPersistentDouble(entity, K_SJT, 0);
		setRotPersistentDouble(entity, K_SMT, 0);
		setRotPersistentDouble(entity, K_SMS, 0);
		setRotPersistentDouble(entity, K_SSWST, 0);
		setRotPersistentDouble(entity, K_SSP, 0);
		setRotPersistentDouble(entity, K_SST2, 0);
		setRotPersistentDouble(entity, K_SDKP, 0);
		setRotPersistentDouble(entity, K_SDKT, 0);
		setRotPersistentDouble(entity, K_ROT, 0);
		setRotPersistentBoolean(entity, K_ROS, false);
		setRotPersistentDouble(entity, K_SCS2, 0);
		setRotPersistentDouble(entity, K_SCS, 0);
		setRotPersistentDouble(entity, K_SCS3, 0);
		setRotPersistentDouble(entity, K_SCS4, 0);
		setRotPersistentDouble(entity, K_SCS5, 0);
		setRotPersistentDouble(entity, K_SCAT, 0);
		setRotPersistentDouble(entity, K_SMW, 0);
		setRotPersistentDouble(entity, K_STIT, 0);
		setRotPersistentBoolean(entity, K_IU, false);
		setRotPersistentBoolean(entity, K_IUL, false);
		setRotPersistentBoolean(entity, K_IUR, false);
		setRotPersistentBoolean(entity, K_IUS, false);
		setRotPersistentBoolean(entity, K_SUD, false);
		if (entity instanceof RotEntity rot) {
			rot.getEntityData().set(RotEntity.DATA_is_armor_ripping, false);
			rot.getEntityData().set(RotEntity.DATA_is_blocking, false);
			rot.getEntityData().set(RotEntity.DATA_is_blocking_finish, false);
			rot.getEntityData().set(RotEntity.DATA_is_sonic_boom, false);
			rot.getEntityData().set(RotEntity.DATA_is_uppercutting, false);
			rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_left, false);
			rot.getEntityData().set(RotEntity.DATA_is_uppercut_charging_right, false);
			rot.getEntityData().set(RotEntity.DATA_is_dropkick_charging, false);

		}
	}

	private static void executeSentinelWitherSkullFiring(LevelAccessor world, Entity entity, Entity target, int fireTicks) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof LivingEntity living)) return;

		entity.setDeltaMovement(entity.getDeltaMovement().x() * 0.25, entity.getDeltaMovement().y(), entity.getDeltaMovement().z() * 0.25);

		if (target == null || !target.isAlive()) {
			setRotPersistentDouble(entity, K_SWSFT, 0);
			setRotPersistentDouble(entity, K_SLCT, LASER_CLOSING_TICKS);
			return;
		}

		lockLookAtTarget(entity, target);

		if (fireTicks == 9 && !getRotPersistentBoolean(entity, K_SWSHF, false)) {
			if (target instanceof LivingEntity livingTarget && isWitherSkullImmuneTarget(level, livingTarget)) {
				recordWitherSkullFailure(entity, livingTarget);
				setRotPersistentDouble(entity, K_SWSFT, 0);
				setRotPersistentDouble(entity, K_SLCT, LASER_CLOSING_TICKS);
				return;
			}
			setRotPersistentBoolean(entity, K_SWSHF, true);
			Vec3 headPos = living.getEyePosition(1.0F).add(0.0, LASER_Y_OFFSET, 0.0);
			Vec3 targetPos = target.getEyePosition(1.0F);
			Vec3 dir = targetPos.subtract(headPos).normalize();

			double spreadX = (level.getRandom().nextDouble() - 0.5) * 0.04;
			double spreadY = (level.getRandom().nextDouble() - 0.5) * 0.04;
			double spreadZ = (level.getRandom().nextDouble() - 0.5) * 0.04;
			Vec3 finalDir = dir.add(spreadX, spreadY, spreadZ).normalize();

			net.minecraft.world.entity.projectile.WitherSkull skull = new net.minecraft.world.entity.projectile.WitherSkull(net.minecraft.world.entity.EntityType.WITHER_SKULL, level);
			skull.setOwner(living);
			skull.setPos(headPos.x, headPos.y, headPos.z);
			skull.setDeltaMovement(finalDir.scale(3.8));
			if (level.getRandom().nextFloat() < 0.25F) {
				skull.setDangerous(true);
			}
			level.addFreshEntity(skull);
			if (target instanceof LivingEntity livingTarget) {
				MobEffectInstance wither = livingTarget.getEffect(MobEffects.WITHER);
				setRotPersistentString(entity, K_SWSOT2, livingTarget.getUUID().toString());
				setRotPersistentDouble(entity, K_SWSOB, wither != null ? wither.getDuration() : 0.0);
				setRotPersistentDouble(entity, K_SWSOT, 30.0);
			}

			level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, headPos.x, headPos.y, headPos.z, 1, 0.0, 0.0, 0.0, 0.0);
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, headPos.x + dir.x * 0.5, headPos.y + dir.y * 0.5, headPos.z + dir.z * 0.5, 8, 0.1, 0.1, 0.1, 0.1);
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, headPos.x, headPos.y, headPos.z, 5, 0.1, 0.1, 0.1, 0.05);
			playHostileSound(level, headPos.x, headPos.y, headPos.z, "entity.wither.shoot", 1.2F, 1.3F);
		}

		if (fireTicks <= 1) {
			setRotPersistentDouble(entity, K_SWSFT, 0);
			setRotPersistentDouble(entity, K_SLCT, LASER_CLOSING_TICKS);
		}
	}

	private static boolean shouldAvoidWitherSkulls(Entity rot, Entity target) {
		if (!(target instanceof LivingEntity livingTarget)) return false;
		String learnedTarget = rot.getPersistentData().getString(K_SWSIT);
		return livingTarget.getUUID().toString().equals(learnedTarget);
	}

	private static boolean isWitherSkullImmuneTarget(ServerLevel level, LivingEntity target) {
		return isWither(target) || target.isInvulnerableTo(level.damageSources().wither());
	}

	private static void recordWitherSkullFailure(Entity rot, LivingEntity target) {
		String targetId = target.getUUID().toString();
		String previousTarget = rot.getPersistentData().getString(K_SWSFT2);
		double failures = previousTarget.equals(targetId) ? rot.getPersistentData().getDouble(K_SWSF) : 0.0;
		failures++;
		rot.getPersistentData().putString(K_SWSFT2, targetId);
		rot.getPersistentData().putDouble(K_SWSF, failures);
		if (failures >= 2.0) rot.getPersistentData().putString(K_SWSIT, targetId);
	}

	private static void interceptEnderPearls(LevelAccessor world, Entity entity) {
		if (!(world instanceof ServerLevel level)) return;
		boolean unlockedSolar = getRotPersistentBoolean(entity, K_USB, false);
		boolean unlockedCryo = getRotPersistentBoolean(entity, K_UCB, false);
		if (!unlockedSolar && !unlockedCryo) return;

		if (getRotPersistentDouble(entity, K_SSCT, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SSFT, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SCCT, 0.0) > 0
			|| getRotPersistentDouble(entity, K_SCFT, 0.0) > 0) {
			return;
		}

		AABB box = entity.getBoundingBox().inflate(64.0);
		java.util.List<net.minecraft.world.entity.projectile.ThrownEnderpearl> pearls = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.ThrownEnderpearl.class, box);
		for (net.minecraft.world.entity.projectile.ThrownEnderpearl pearl : pearls) {
			if (pearl.isAlive() && pearl.tickCount >= 3) {
				double dist = entity.distanceTo(pearl);
				boolean approachingLand = pearl.getDeltaMovement().y() < 0 || pearl.tickCount >= 8 || dist < 30.0;
				if (approachingLand) {
					lockLookAtTarget(entity, pearl);
					entity.getPersistentData().putInt(K_SLTI, pearl.getId());
					if (unlockedSolar) {
						setRotPersistentDouble(entity, K_SSFT, 15.0);
					} else {
						setRotPersistentDouble(entity, K_SCFT, 15.0);
					}
					break;
				}
			}
		}
	}

	private static Set<Integer> getActiveBiasIndices(Entity entity) {
		Set<Integer> set = new HashSet<>();
		String str = getRotPersistentString(entity, K_AABI, "");
		if (!str.isEmpty()) {
			for (String part : str.split(",")) {
				try {
					set.add(Integer.parseInt(part.trim()));
				} catch (Exception ignored) {}
			}
		}
		return set;
	}

	private static void setActiveBiasIndices(Entity entity, Set<Integer> set) {
		if (set == null || set.isEmpty()) {
			entity.getPersistentData().remove(K_AABI);
		} else {
			StringBuilder sb = new StringBuilder();
			for (int idx : set) {
				if (sb.length() > 0) sb.append(",");
				sb.append(idx);
			}
			setRotPersistentString(entity, K_AABI, sb.toString());
		}
	}

	private static void recordBiasIndexUpdate(Entity entity, String lastMove) {
		if (lastMove != null && lastMove.startsWith("combo_")) {
			try {
				int idx = Integer.parseInt(lastMove.substring(6));
				Set<Integer> set = getActiveBiasIndices(entity);
				if (set.add(idx)) {
					setActiveBiasIndices(entity, set);
				}
			} catch (Exception ignored) {}
		}
	}

	public static String inferCurrentAttackType(Entity rot) {
		if (rot == null) return "MELEE";
		if (getRotPersistentDouble(rot, K_SJT, 0.0) > 0) return "JUDGMENT";
		if (getRotPersistentDouble(rot, K_ROT, 0.0) > 0) return "OVERHEAD";
		if (getRotPersistentDouble(rot, K_SDKT, 0.0) > 0 || getRotPersistentDouble(rot, K_SDKP, 0.0) > 0) return "DIE_KICK";
		if (getRotPersistentDouble(rot, K_SWSFT, 0.0) > 0) return "WITHER_SKULL";
		if (getRotPersistentDouble(rot, K_SST, 0.0) > 0 || getRotPersistentDouble(rot, K_SOSCT, 0.0) > 0 || getRotPersistentDouble(rot, K_SSST, 0.0) > 0) return "SONIC";
		if (getRotPersistentDouble(rot, K_SST2, 0.0) > 0 || getRotPersistentDouble(rot, K_SSWST, 0.0) > 0) return "SLAM";
		if (getRotPersistentDouble(rot, K_SSFT, 0.0) > 0 || getRotPersistentDouble(rot, K_SSCT, 0.0) > 0) return "SOLAR_BEAM";
		if (getRotPersistentDouble(rot, K_SCFT, 0.0) > 0 || getRotPersistentDouble(rot, K_SCCT, 0.0) > 0) return "CRYO_BEAM";
		if (getRotPersistentDouble(rot, K_GRAPPLE_TICKS, 0.0) > 0) return "GRAPPLE";
		if (getRotPersistentDouble(rot, K_TK_TICKS, 0.0) > 0) return "TELEKINESIS";
		if (getRotPersistentDouble(rot, K_SMW, 0.0) > 0) return "MELEE_COUNTER";
		return "MELEE";
	}

	public static boolean isTargetHighlyDangerous(LivingEntity target) {
		if (target == null) return false;
		String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString().toLowerCase();
		if (typeKey.contains("wroughtnaut") || typeKey.contains("boss") || typeKey.contains("warden") || typeKey.contains("dragon") || typeKey.contains("wither")) {
			return true;
		}
		ItemStack mainHand = target.getMainHandItem();
		if (!mainHand.isEmpty()) {
			String itemName = BuiltInRegistries.ITEM.getKey(mainHand.getItem()).getPath().toLowerCase();
			if (itemName.contains("wrought") || itemName.contains("giant") || itemName.contains("hammer") || itemName.contains("battleaxe") || itemName.contains("claymore") || itemName.contains("heavy")) {
				return true;
			}
		}
		if (target.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
			double attackDmg = target.getAttributeValue(Attributes.ATTACK_DAMAGE);
			if (attackDmg >= 8.5) {
				return true;
			}
		}
		return false;
	}

	private static boolean getRotPersistentBoolean(Entity entity, String key, boolean fallback) {
		return entity.getPersistentData().contains(key) ? entity.getPersistentData().getBoolean(key) : fallback;
	}

	private static int getRotPersistentInt(Entity entity, String key, int fallback) {
		return entity.getPersistentData().contains(key) ? entity.getPersistentData().getInt(key) : fallback;
	}

	private static double getRotPersistentDouble(Entity entity, String key, double fallback) {
		return entity.getPersistentData().contains(key) ? entity.getPersistentData().getDouble(key) : fallback;
	}

	public static double getTelegraphJitter(Entity entity, String key, double baseTick, double minOffset, double maxOffset) {
		if (entity == null) return baseTick;
		long uuidBits = entity.getUUID().getLeastSignificantBits();
		double castCount = getRotPersistentDouble(entity, key + "_cast_instance", 0.0);
		double h = Math.abs((uuidBits ^ Double.doubleToRawLongBits(castCount)) % 1000) / 1000.0;
		double offset = minOffset + h * (maxOffset - minOffset);
		return Math.round(baseTick + offset);
	}

	private static String getRotPersistentString(Entity entity, String key, String fallback) {
		return entity.getPersistentData().contains(key) ? entity.getPersistentData().getString(key) : fallback;
	}

	private static void setRotPersistentBoolean(Entity entity, String key, boolean val) {
		entity.getPersistentData().putBoolean(key, val);
	}

	private static void setRotPersistentInt(Entity entity, String key, int val) {
		entity.getPersistentData().putInt(key, val);
	}

	private static void setRotPersistentDouble(Entity entity, String key, double val) {
		entity.getPersistentData().putDouble(key, val);
	}

	private static void setRotPersistentString(Entity entity, String key, String val) {
		entity.getPersistentData().putString(key, val);
	}

	private static boolean hasNBTKey(CompoundTag tag, String key) {
		return tag.contains(key);
	}
}
// 1.21.1. never delete version comments
