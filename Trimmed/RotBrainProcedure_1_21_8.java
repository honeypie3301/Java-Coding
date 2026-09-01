package net.mcreator.thebackwoods.procedures;

// 1.21.8 - Rot AI Brain, Neural Network, Prediction & Adaptation Subsystems
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
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import com.mojang.serialization.Codec;

public class RotBrainProcedure {

	// =========================================================================
	// NBT KEY CONSTANTS
	// =========================================================================
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
	public static final String K_RART = "rot_armor_rip_ticks";
	public static final String K_RBAT = "rot_block_active_ticks";
	public static final String K_STIT = "sentinel_totem_inspect_ticks";
	public static final String K_SSRA = "sentinel_sonic_reposition_attempts";
	public static final String K_SJDX = "sentinel_judgment_dir_x";
	public static final String K_SJDY = "sentinel_judgment_dir_y";
	public static final String K_SJDZ = "sentinel_judgment_dir_z";
	public static final String K_SEPC = "sentinel_eat_punish_cooldown";
	public static final String K_SJT = "sentinel_judgment_ticks";
	public static final String K_SAOS = "sentinel_attack_outcome_score";
	public static final String K_SWSFT = "sentinel_wither_skull_fire_ticks";
	public static final String K_SLAX = "sentinel_laser_aim_x";
	public static final String K_SLAY = "sentinel_laser_aim_y";
	public static final String K_SLAZ = "sentinel_laser_aim_z";
	public static final String K_SLTI = "sentinel_laser_target_id";
	public static final String K_SUC = "sentinel_uppercut_cd";
	public static final String K_SDKP = "sentinel_die_kick_phase";
	public static final String K_SLT = "sentinel_landing_ticks";
	public static final String K_SSFT = "sentinel_solar_fire_ticks";
	public static final String K_SCFT = "sentinel_cryo_fire_ticks";
	public static final String K_SWSC = "sentinel_warden_sonic_cooldown";
	public static final String K_RLX = "rot_last_x";
	public static final String K_RLZ = "rot_last_z";
	public static final String K_SESC = "sentinel_evasive_spacing_cd";
	public static final String K_ROT = "rot_overhead_ticks";
	public static final String K_SDCC = "sentinel_dive_counter_cd";
	public static final String K_SHPM = "sentinel_heavy_punch_misses";
	public static final String K_SSCT = "sentinel_solar_charge_ticks";
	public static final String K_SCCT = "sentinel_cryo_charge_ticks";
	public static final String K_SDKDX = "sentinel_die_kick_dir_x";
	public static final String K_SDKDY = "sentinel_die_kick_dir_y";
	public static final String K_SDKDZ = "sentinel_die_kick_dir_z";
	public static final String K_SSBH = "sentinel_sustained_bullet_hits";
	public static final String K_SCT = "sentinel_combat_ticks";
	public static final String K_STST = "sentinel_totem_steal_timer";
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
	public static final String K_RSC = "rot_superheat_cd";
	public static final String K_SPTS = "sentinel_predicted_threat_score";
	public static final String K_RBC = "rot_block_cooldown";
	public static final String K_SMC = "sentinel_melee_cooldown";
	public static final String K_SMW = "sentinel_melee_windup";
	public static final String K_SST = "sentinel_sonic_ticks";
	public static final String K_SOSCT = "sentinel_omni_sonic_charge_ticks";
	public static final String K_SSST = "sentinel_sonic_scream_ticks";
	public static final String K_SSWST = "sentinel_sky_warp_slam_ticks";
	public static final String K_SMT = "sentinel_minos_ticks";
	public static final String K_SSP = "sentinel_slam_phase";
	public static final String K_SDKT = "sentinel_die_kick_ticks";
	public static final String K_SST2 = "sentinel_slam_ticks";
	public static final String K_SCS = "sentinel_cc2_stage";
	public static final String K_SCT2 = "sentinel_cc2_ticks";
	public static final String K_SLCT = "sentinel_laser_closing_ticks";
	public static final String K_RPT = "rot_phase_ticks";
	public static final String K_SMS = "sentinel_minos_stage";
	public static final String K_SCS2 = "sentinel_cc1_stage";
	public static final String K_SCS3 = "sentinel_cc3_stage";
	public static final String K_SCS4 = "sentinel_cc4_stage";
	public static final String K_SCS5 = "sentinel_cc5_stage";
	public static final String K_SCAT = "sentinel_combo_active_ticks";
	public static final String K_DFO = "debug_force_overhead";
	public static final String K_DFR = "debug_force_rider";
	public static final String K_IAR = "is_armor_ripping";
	public static final String K_IB = "is_blocking";
	public static final String K_UEB = "unlocked_explosion_boom";
	public static final String K_SST3 = "sentinel_sonic_triggered";
	public static final String K_MGM = "master_guard_mode";
	public static final String K_MFE = "master_follow_enabled";
	public static final String K_UT = "unlocked_teleportation";
	public static final String K_IFH = "is_falling_heavy";
	public static final String K_UHSSC = "unlocked_high_sky_slam_combo";
	public static final String K_USB = "unlocked_sonic_boom";
	public static final String K_USB2 = "unlocked_solar_beam";
	public static final String K_UCB = "unlocked_cryo_beam";
	public static final String K_UWS = "unlocked_wither_skulls";
	public static final String K_STA2 = "sentinel_totem_active";
	public static final String K_SIIT = "sentinel_is_infinity_totem";
	public static final String K_UWE = "unlocked_water_evaporation";
	public static final String K_TFD = "taken_fire_damage";
	public static final String K_IU = "is_uppercutting";
	public static final String K_UOC = "unlocked_overhead_combo";
	public static final String K_SDCA = "sentinel_dive_counter_active";
	public static final String K_STL = "sentinel_totem_learned";
	public static final String K_STS = "sentinel_totem_stolen";
	public static final String K_SJST = "sentinel_just_stole_totem";
	public static final String K_RPS = "rot_phase_shifting";
	public static final String K_SWSHF = "sentinel_wither_skull_has_fired";
	public static final String K_ISB = "is_sonic_boom";
	public static final String K_SBA = "sonic_boom_active";
	public static final String K_IUL = "is_uppercutting_left";
	public static final String K_IUR = "is_uppercutting_right";
	public static final String K_IUS = "is_uppercut_standalone";
	public static final String K_SPAI = "sentinel_predicted_attack_imminent";
	public static final String K_SWD = "sentinel_windup_detected";
	public static final String K_IBF = "is_blocking_finish";
	public static final String K_ROS = "rot_overhead_started";
	public static final String K_SUD = "sentinel_uppercut_dodged";
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
	public static final String K_RSC2 = "rot_superheat_charging";
	public static final String K_RSA = "rot_superheat_active";
	public static final String K_RBFT = "rot_block_finish_ticks";
	public static final String K_RDTC = "rot_dps_tick_counter";
	public static final String K_RDTS = "rot_dmg_this_sec";
	public static final String K_RDS0 = "rot_dmg_sec_0";
	public static final String K_RDS1 = "rot_dmg_sec_1";
	public static final String K_RDS2 = "rot_dmg_sec_2";
	public static final String K_RDS3 = "rot_dmg_sec_3";
	public static final String K_RLT = "rot_land_timer";
	public static final String K_SSS = "sentinel_shockwave_stage";
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
	public static final String K_CHL = "client_had_laser";
	public static final String K_RDSA = "rot_death_sequence_active";
	public static final String K_RDHS2 = "rot_death_hole_spawned";
	public static final String K_AGM = "adapted_gravitational_mass";
	public static final String K_RFGA = "rot_forced_gravity_active";
	public static final String K_SSI = "sentinel_spawn_initialized";
	public static final String K_SSS2 = "sentinel_should_scan";
	public static final String K_DFB = "debug_force_block";
	public static final String K_SISLA = "sentinel_immune_slam_landing_active";
	public static final String K_SSV = "sentinel_shockwave_vertical";
	public static final String K_SCICB = "sentinel_cached_in_cold_biome";
	public static final String K_SSPT = "sentinel_said_prepare_thyself";
	public static final String K_SSWSI = "sentinel_sky_warp_slam_impact";
	public static final String K_SRHO = "sentinel_rider_hold_onground";
	public static final String K_ID = "is_dueling";
	public static final String K_AS = "analyzed_species_";
	public static final String K_AFR = "adapted_forcefield_repulsion";
	public static final String K_UG = "unlocked_grapple";
	public static final String K_SWI = "sentinel_waiting_intercept";
	public static final String K_SPHT = "sentinel_punch_hand_toggle";
	public static final String K_UDC = "unlocked_dropkick_combo";
	public static final String K_UTTC = "unlocked_triple_threat_combo";
	public static final String K_UKRC = "unlocked_knockback_rider_combo";
	public static final String K_SCLT = "sentinel_cached_learn_tp";
	public static final String K_UR = "unlocked_regen";
	public static final String K_DFUL = "debug_force_uppercut_left";
	public static final String K_DFUR = "debug_force_uppercut_right";
	public static final String K_SISL = "sentinel_is_slam_landing";
	public static final String K_ISBL = "is_sonic_boom_large";
	public static final String K_IDC = "is_dropkick_charging";
	public static final String K_I = "isLand";
	public static final String K_I2 = "isLand2";
	public static final String K_SRTA = "sentinel_reacting_to_attacker";
	public static final String K_SCL = "sentinel_cc2_launched";
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


	public static void execute() {
	}

	public static class CombatContext {
		public boolean isAirborne;
		public boolean isHealing;
		public boolean isBlocking;
		public boolean isCornered;
		public boolean isMovingFast;
		public boolean isEatingHealingItem;
		public int eatingTicksRemaining;
		public double dist;
		public double dY;

		public boolean isJumpCritIncoming;
		public double incomingProjectileDistance;
		public int nearbyTargetCount;
		public boolean targetNearLedgeOrHazard;
		public boolean isEnclosedSpace;
		public double expectedIncomingDamage;
	}

	public static CombatContext getCombatContext(Entity self, Entity target) {
		CombatContext ctx = new CombatContext();
		if (self == null || target == null) return ctx;
		ctx.dist = self.distanceTo(target);
		ctx.dY = target.getY() - self.getY();
		ctx.isAirborne = !target.onGround() && ctx.dY > 2.0;

		if (target instanceof LivingEntity liv) {
			ctx.isBlocking = liv.isBlocking();
			if (liv.isUsingItem()) {
				ItemStack useItem = liv.getUseItem();
				if (useItem.is(net.minecraft.world.item.Items.GOLDEN_APPLE)
						|| useItem.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE)
						|| useItem.is(net.minecraft.world.item.Items.MILK_BUCKET)
						|| useItem.getItem() instanceof net.minecraft.world.item.PotionItem) {
					ctx.isEatingHealingItem = true;
					ctx.eatingTicksRemaining = liv.getUseItemRemainingTicks();
				}
			}
			ctx.isHealing = ctx.isEatingHealingItem || liv.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION) || liv.getHealth() < liv.getMaxHealth() * 0.4 || (liv.isUsingItem() && liv.getHealth() < liv.getMaxHealth());

			boolean isFalling = target.getDeltaMovement().y < -0.05 && !target.onGround();
			boolean isSwingingOrAttacking = liv.swinging || (liv instanceof Player p && p.getAttackStrengthScale(0.5f) > 0.6f);
			ctx.isJumpCritIncoming = isFalling && isSwingingOrAttacking && ctx.dist <= 4.0;
		}

		ctx.isMovingFast = target.getDeltaMovement().horizontalDistanceSqr() > 0.04;
		
		int solidBlocks = 0;
		net.minecraft.core.BlockPos p = target.blockPosition();
		net.minecraft.world.level.Level lvl = target.level();
		if (lvl.getBlockState(p.east()).isSolid()) solidBlocks++;
		if (lvl.getBlockState(p.west()).isSolid()) solidBlocks++;
		if (lvl.getBlockState(p.north()).isSolid()) solidBlocks++;
		if (lvl.getBlockState(p.south()).isSolid()) solidBlocks++;
		ctx.isCornered = solidBlocks >= 2;

		ctx.incomingProjectileDistance = -1.0;
		if (self != null && self.level() instanceof net.minecraft.world.level.Level level) {
			AABB pBox = self.getBoundingBox().inflate(16.0);
			List<net.minecraft.world.entity.projectile.Projectile> projs = level.getEntitiesOfClass(
				net.minecraft.world.entity.projectile.Projectile.class, pBox,
				proj -> proj.getOwner() != self && proj.getDeltaMovement().lengthSqr() > 0.04
			);
			Vec3 selfPos = self.position().add(0, self.getBbHeight() * 0.5, 0);
			double minProjDist = 999.0;
			for (net.minecraft.world.entity.projectile.Projectile proj : projs) {
				Vec3 pPos = proj.position();
				Vec3 pVel = proj.getDeltaMovement();
				Vec3 toSelf = selfPos.subtract(pPos);
				if (pVel.dot(toSelf) > 0) {
					double t = toSelf.dot(pVel) / pVel.lengthSqr();
					if (t > 0 && t <= 20.0) {
						Vec3 closest = pPos.add(pVel.scale(t));
						if (closest.distanceTo(selfPos) < 2.5) {
							double d = pPos.distanceTo(selfPos);
							if (d < minProjDist) minProjDist = d;
						}
					}
				}
			}
			if (minProjDist < 990.0) {
				ctx.incomingProjectileDistance = minProjDist;
			}
		}

		double expectedDamage = 0.0;
		if (target instanceof LivingEntity liv) {
			double attackDamage = liv.getAttribute(Attributes.ATTACK_DAMAGE) != null ? liv.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0.0;
			if (attackDamage <= 0.0) attackDamage = 4.0;
			double readiness = target instanceof Player ? ((Player) target).getAttackStrengthScale(0.5f) : (liv.swinging ? 1.0 : 0.35);
			if (ctx.dist <= 4.5 && (liv.swinging || readiness > 0.8 || target instanceof Mob)) {
				expectedDamage += attackDamage * readiness;
			}
			if (ctx.isJumpCritIncoming) expectedDamage *= 1.5;
		}
		if (ctx.incomingProjectileDistance > 0.0) {
			double projectilePressure = 4.0 + getRotPersistentDouble(self, K_SSBH, 0.0) * 1.5;
			expectedDamage += projectilePressure * Math.max(0.25, 1.0 - ctx.incomingProjectileDistance / 20.0);
		}
		ctx.expectedIncomingDamage = expectedDamage;

		if (self != null && self.level() instanceof net.minecraft.world.level.Level level) {
			AABB crowdBox = self.getBoundingBox().inflate(8.0);
			List<LivingEntity> crowd = level.getEntitiesOfClass(
				LivingEntity.class, crowdBox,
				e -> e != self && e.isAlive() && (e instanceof Player || e instanceof Mob)
			);
			ctx.nearbyTargetCount = Math.max(1, crowd.size());
		} else {
			ctx.nearbyTargetCount = 1;
		}

		if (target != null && target.level() instanceof net.minecraft.world.level.Level level) {
			BlockPos tPos = target.blockPosition();
			boolean hazard = false;
			for (BlockPos offset : BlockPos.betweenClosed(tPos.offset(-2, -1, -2), tPos.offset(2, 0, 2))) {
				net.minecraft.world.level.block.state.BlockState bs = level.getBlockState(offset);
				if (bs.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) || bs.is(net.minecraft.world.level.block.Blocks.VOID_AIR)) {
					hazard = true;
					break;
				}
			}
			if (!hazard) {
				for (int x = -2; x <= 2; x++) {
					for (int z = -2; z <= 2; z++) {
						if (Math.abs(x) + Math.abs(z) == 0) continue;
						BlockPos checkPos = tPos.offset(x, 0, z);
						int drop = 0;
						while (drop < 5 && level.isEmptyBlock(checkPos.below(drop))) {
							drop++;
						}
						if (drop >= 4) {
							hazard = true;
							break;
						}
					}
					if (hazard) break;
				}
			}
			ctx.targetNearLedgeOrHazard = hazard;
		}

		if (self != null && self.level() instanceof net.minecraft.world.level.Level level) {
			BlockPos selfPos = self.blockPosition();
			int solidCount = 0;
			for (BlockPos offset : BlockPos.betweenClosed(selfPos.offset(-2, 0, -2), selfPos.offset(2, 3, 2))) {
				if (level.getBlockState(offset).isSolid()) {
					solidCount++;
				}
			}
			ctx.isEnclosedSpace = solidCount >= 12;
		}
		
		return ctx;
	}

	public static class AbilityInfo {
		public final String id;
		public final String unlockFlag;
		public final String cooldownKey;
		public final double range;
		public final double damage;
		public final Set<String> tags;

		public double horizontalReach;
		public double verticalReach;
		public double forwardMovement;
		public double upwardMovement;
		public double downwardMovement;
		public String movementType = "GROUND";
		public double maximumTravelDistance;
		public double travelSpeed = 1.0;
		public boolean canCrossWater = false;
		public boolean canCrossGaps = false;
		public boolean canTraverseAir = false;
		public boolean requiresGround = false;
		public boolean requiresLineOfSight = true;

		public double trackingStrength = 1.0;
		public double predictionStrength = 1.0;
		public double homingStrength = 0.0;
		public boolean lockOnDuringMove = false;
		public boolean usableAgainstAir = true;
		public boolean usableAgainstGround = true;
		public boolean usableAgainstWater = true;
		public boolean usableAgainstBoats = true;
		public boolean usableAgainstMountedTargets = true;

		public double antiAirRating = 0.0;
		public double gapCloserRating = 0.0;
		public double crowdControlRating = 0.0;
		public double burstRating = 0.0;
		public double sustainedRating = 0.0;
		public double escapePunishRating = 0.0;
		public double comboStarterRating = 0.0;
		public double comboExtenderRating = 0.0;
		public double finisherRating = 0.0;
		public double shieldBreakRating = 0.0;
		public double interruptionRating = 0.0;
		public double zoningRating = 0.0;

		public double startupTicks = 10.0;
		public double activeTicks = 10.0;
		public double recoveryTicks = 10.0;
		public double cooldownWeight = 1.0;
		public double commitment = 1.0;
		public double missPunishment = 1.0;
		public double interruptResistance = 1.0;

		public double knockbackPower = 1.0;
		public boolean launchesTarget = false;
		public boolean launchesSelf = false;
		public boolean causesAirborneState = false;
		public boolean slamsTarget = false;
		public boolean createsAOE = false;
		public double preferredMinimumRange = 0.0;
		public double preferredMaximumRange = 16.0;

		public double closesDistance = 0.0;
		public double gainsAltitude = 0.0;
		public double descendsQuickly = 0.0;
		public double interceptsMovingTargets = 0.0;
		public double punishesRetreat = 0.0;
		public double antiEscape = 0.0;
		public double antiFlight = 0.0;
		public double antiShield = 0.0;
		public double antiGroup = 0.0;
		public double forcesMovement = 0.0;
		public double keepsPressure = 0.0;
		public double opensCombo = 0.0;
		public double extendsCombo = 0.0;
		public double endsCombo = 0.0;

		public Set<String> affordances = new HashSet<>();

		public AbilityInfo(String id, String unlockFlag, String cooldownKey, double range, double damage, String... tags) {
			this.id = id;
			this.unlockFlag = unlockFlag;
			this.cooldownKey = cooldownKey;
			this.range = range;
			this.damage = damage;
			this.tags = new HashSet<>(java.util.Arrays.asList(tags));
			this.horizontalReach = range;
			this.verticalReach = range * 0.5;
			this.maximumTravelDistance = range;
			this.preferredMaximumRange = range;
		}

		public boolean hasTag(String tag) {
			return tags.contains(tag);
		}

		public boolean hasAffordance(String affordance) {
			return affordances.contains(affordance);
		}

		public AbilityInfo mobility(double hReach, double vReach, double fwdMove, double upMove, double downMove, String type, double speed, boolean crossWater, boolean crossGaps, boolean traverseAir) {
			this.horizontalReach = hReach;
			this.verticalReach = vReach;
			this.forwardMovement = fwdMove;
			this.upwardMovement = upMove;
			this.downwardMovement = downMove;
			this.movementType = type;
			this.travelSpeed = speed;
			this.canCrossWater = crossWater;
			this.canCrossGaps = crossGaps;
			this.canTraverseAir = traverseAir;
			this.maximumTravelDistance = Math.max(hReach, vReach);
			return this;
		}

		public AbilityInfo targeting(double track, double predict, double homing, boolean lockOn, boolean air, boolean ground, boolean water, boolean boats, boolean mounted) {
			this.trackingStrength = track;
			this.predictionStrength = predict;
			this.homingStrength = homing;
			this.lockOnDuringMove = lockOn;
			this.usableAgainstAir = air;
			this.usableAgainstGround = ground;
			this.usableAgainstWater = water;
			this.usableAgainstBoats = boats;
			this.usableAgainstMountedTargets = mounted;
			return this;
		}

		public AbilityInfo ratings(double antiAir, double gapCloser, double cc, double burst, double sustained, double escapePunish, double comboStart, double comboExt, double finisher, double shieldBreak, double interrupt, double zoning) {
			this.antiAirRating = antiAir;
			this.gapCloserRating = gapCloser;
			this.crowdControlRating = cc;
			this.burstRating = burst;
			this.sustainedRating = sustained;
			this.escapePunishRating = escapePunish;
			this.comboStarterRating = comboStart;
			this.comboExtenderRating = comboExt;
			this.finisherRating = finisher;
			this.shieldBreakRating = shieldBreak;
			this.interruptionRating = interrupt;
			this.zoningRating = zoning;
			return this;
		}

		public AbilityInfo risk(double startup, double active, double recovery, double cdWeight, double commitment, double missPunish, double intResist) {
			this.startupTicks = startup;
			this.activeTicks = active;
			this.recoveryTicks = recovery;
			this.cooldownWeight = cdWeight;
			this.commitment = commitment;
			this.missPunishment = missPunish;
			this.interruptResistance = intResist;
			return this;
		}

		public AbilityInfo control(double kbPower, boolean launchTarget, boolean launchSelf, boolean airborneState, boolean slamTarget, boolean aoe, double minR, double maxR) {
			this.knockbackPower = kbPower;
			this.launchesTarget = launchTarget;
			this.launchesSelf = launchSelf;
			this.causesAirborneState = airborneState;
			this.slamsTarget = slamTarget;
			this.createsAOE = aoe;
			this.preferredMinimumRange = minR;
			this.preferredMaximumRange = maxR;
			return this;
		}

		public AbilityInfo tactical(double closesDist, double gainsAlt, double descendsQuick, double interceptMoving, double punishRetreat, double antiEsc, double antiFlt, double antiShld, double antiGrp, double forcesMove, double pressure, double openC, double extC, double endC) {
			this.closesDistance = closesDist;
			this.gainsAltitude = gainsAlt;
			this.descendsQuickly = descendsQuick;
			this.interceptsMovingTargets = interceptMoving;
			this.punishesRetreat = punishRetreat;
			this.antiEscape = antiEsc;
			this.antiFlight = antiFlt;
			this.antiShield = antiShld;
			this.antiGroup = antiGrp;
			this.forcesMovement = forcesMove;
			this.keepsPressure = pressure;
			this.opensCombo = openC;
			this.extendsCombo = extC;
			this.endsCombo = endC;
			return this;
		}

		public AbilityInfo affordances(String... affs) {
			for (String a : affs) {
				this.affordances.add(a);
			}
			return this;
		}
	}

	public static final List<AbilityInfo> ABILITY_REGISTRY = new ArrayList<>();
	static {
		ABILITY_REGISTRY.add(new AbilityInfo("sonic_boom", K_USB, K_SWSC, 24.0, SONIC_BOOM_DMG, "ranged", "burst", "anti-air", "control")
			.mobility(24.0, 12.0, 24.0, 0.0, 0.0, "PROJECTILE", 3.0, true, true, true)
			.targeting(2.0, 2.0, 0.5, true, true, true, true, true, true)
			.ratings(2.8, 0.0, 2.0, 2.5, 0.5, 2.0, 0.0, 0.5, 1.5, 0.5, 2.5, 2.5)
			.tactical(0.0, 0.0, 0.0, 2.2, 2.0, 2.0, 2.5, 0.0, 0.0, 1.5, 1.0, 0.0, 0.0, 0.0)
			.affordances("ranged_stagger", "anti_flight_deny", "knockback"));

		ABILITY_REGISTRY.add(new AbilityInfo("omni_sonic_boom", K_USB, "sentinel_omni_sonic_cooldown", 6.0, SONIC_BOOM_DMG, "aoe", "burst", "control")
			.mobility(6.0, 6.0, 0.0, 0.0, 0.0, "AOE", 1.0, true, true, true)
			.ratings(1.5, 0.0, 3.0, 2.8, 0.0, 0.0, 0.0, 0.0, 1.5, 1.0, 3.0, 3.0)
			.control(2.5, true, false, true, false, true, 0.0, 6.0)
			.tactical(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 2.5, 1.5, 0.0, 0.0, 0.0)
			.affordances("shockwave", "area_denial", "multi_target_stagger"));

		ABILITY_REGISTRY.add(new AbilityInfo("solar_beam", K_USB2, K_SOLAR_CD, 32.0, SOLAR_BEAM_DMG_BASE, "ranged", "sustained", "burst")
			.mobility(32.0, 16.0, 32.0, 0.0, 0.0, "PROJECTILE", 4.0, true, true, true)
			.ratings(1.8, 0.0, 1.0, 2.8, 2.5, 2.2, 0.0, 0.0, 2.0, 1.5, 1.5, 2.8)
			.tactical(0.0, 0.0, 0.0, 2.0, 2.5, 2.2, 1.8, 1.0, 0.0, 1.0, 2.5, 0.0, 0.0, 0.0)
			.affordances("sustained_damage", "zoning_fire", "heat_buildup"));

		ABILITY_REGISTRY.add(new AbilityInfo("cryo_beam", K_UCB, K_SOLAR_CD, 32.0, CRYO_BEAM_DMG_BASE, "ranged", "sustained", "control")
			.mobility(32.0, 16.0, 32.0, 0.0, 0.0, "PROJECTILE", 4.0, true, true, true)
			.ratings(1.8, 0.0, 2.8, 1.8, 2.8, 2.2, 0.0, 0.0, 1.5, 1.0, 2.0, 3.0)
			.tactical(0.0, 0.0, 0.0, 2.0, 2.5, 2.5, 1.8, 1.0, 0.0, 1.0, 2.5, 0.0, 0.0, 0.0)
			.affordances("slow_debuff", "freeze_lock", "zoning_ice"));

		ABILITY_REGISTRY.add(new AbilityInfo("wither_skulls", K_UWS, "sentinel_wither_skull_cd", 32.0, 12.0, "ranged", "burst")
			.mobility(32.0, 16.0, 32.0, 0.0, 0.0, "PROJECTILE", 2.0, true, true, true)
			.ratings(1.5, 0.0, 1.5, 2.2, 1.0, 1.8, 0.0, 0.0, 1.0, 0.5, 1.0, 2.0)
			.affordances("wither_debuff", "ranged_harass"));

		ABILITY_REGISTRY.add(new AbilityInfo("telekinesis", "unlocked_telekinesis", K_TK_CD, 16.0, COMBO_TK_SLAM_DMG, "control", "ranged", "gap-closer")
			.mobility(16.0, 10.0, 16.0, 5.0, 5.0, "TELEPORT", 3.0, true, true, true)
			.targeting(2.5, 2.5, 2.0, true, true, true, true, true, true)
			.ratings(2.0, 2.8, 3.0, 1.8, 0.0, 2.8, 2.0, 1.5, 1.0, 2.0, 3.0, 2.0)
			.tactical(2.8, 1.5, 1.5, 2.5, 2.8, 2.8, 2.0, 2.0, 1.0, 3.0, 2.0, 2.0, 1.5, 0.0)
			.affordances("pull_target", "disrupt_boat", "ground_slam", "juggle_opportunity"));

		ABILITY_REGISTRY.add(new AbilityInfo("grapple", K_UG, K_GRAPPLE_CD, 12.0, MUTANT_DNA_GRAPPLE_DMG, "drain", "control", "gap-closer")
			.mobility(12.0, 8.0, 12.0, 0.0, 0.0, "DASH", 2.5, true, true, true)
			.ratings(1.0, 2.8, 2.5, 1.5, 1.5, 2.5, 2.2, 1.5, 1.0, 1.5, 2.5, 1.0)
			.tactical(2.8, 0.0, 0.0, 2.2, 2.5, 2.5, 1.0, 1.5, 0.0, 2.5, 2.0, 2.2, 1.5, 0.0)
			.affordances("pull_self_to_target", "choke_grab", "life_drain"));

		ABILITY_REGISTRY.add(new AbilityInfo("overhead_combo", K_UOC, "sentinel_overhead_cooldown", 4.5, COMBO_OVERHEAD_SLAM_DMG, "burst", "anti-shield", "control")
			.mobility(4.5, 8.0, 2.0, 0.0, 8.0, "LEAP", 2.0, false, true, true)
			.ratings(1.5, 1.0, 2.5, 2.8, 0.0, 1.5, 1.0, 2.5, 2.8, 3.0, 2.5, 1.0)
			.control(2.0, false, false, false, true, true, 0.0, 4.5)
			.tactical(1.0, 0.0, 3.0, 1.5, 1.5, 1.5, 1.5, 3.0, 1.0, 2.0, 2.0, 1.0, 2.5, 2.8)
			.affordances("ground_slam", "shockwave", "target_grounded", "area_denial", "target_stunned"));

		ABILITY_REGISTRY.add(new AbilityInfo("dropkick_combo", K_UDC, "sentinel_dropkick_cooldown", 6.0, COMBO_JUDGMENT_KICK_DMG, "gap-closer", "burst", "control")
			.mobility(200.0, 20.0, 200.0, 5.0, 5.0, "DASH", 4.5, true, true, true)
			.targeting(3.0, 3.0, 2.5, true, true, true, true, true, true)
			.ratings(2.0, 3.0, 2.5, 3.0, 0.0, 3.0, 2.0, 2.0, 2.5, 2.0, 2.5, 1.0)
			.control(3.0, true, false, false, false, false, 4.0, 200.0)
			.tactical(3.0, 0.5, 0.5, 3.0, 3.0, 3.0, 2.0, 1.5, 0.0, 2.5, 2.5, 2.0, 2.0, 2.2)
			.affordances("large_displacement", "knockback", "spacing", "pursuit_opportunity"));

		ABILITY_REGISTRY.add(new AbilityInfo("minos_combo", "unlocked_minos_combo", "sentinel_minos_cooldown", 5.0, COMBO_SEISMIC_SLAM_DMG, "burst", "sustained")
			.mobility(5.0, 3.0, 5.0, 0.0, 0.0, "DASH", 2.0, false, true, false)
			.ratings(0.5, 1.8, 2.0, 2.8, 2.0, 1.5, 2.0, 2.5, 2.0, 1.5, 2.0, 1.0)
			.control(2.0, false, false, false, true, true, 0.0, 5.0)
			.affordances("seismic_shockwave", "combo_chain", "ground_pressure"));

		ABILITY_REGISTRY.add(new AbilityInfo("die_rider_kick", K_UKRC, K_SCC4, 8.0, COMBO_DIE_RIDER_KICK_DMG, "gap-closer", "burst", "anti-air")
			.mobility(30.0, 15.0, 30.0, 8.0, 0.0, "LEAP", 3.5, true, true, true)
			.targeting(2.8, 2.8, 1.8, true, true, true, true, true, true)
			.ratings(2.8, 3.0, 2.0, 3.0, 0.0, 2.8, 1.5, 2.0, 2.8, 1.5, 2.5, 1.0)
			.tactical(3.0, 2.2, 0.0, 2.8, 2.8, 2.8, 2.8, 1.0, 0.0, 2.0, 2.5, 1.5, 2.0, 2.8)
			.affordances("aerial_pursuit", "knockback", "juggle_opportunity", "supersonic_kick"));

		ABILITY_REGISTRY.add(new AbilityInfo("triple_threat_combo", K_UTTC, K_SCC, 4.5, MELEE_PUNCH_DAMAGE * 3.0, "sustained", "burst")
			.mobility(4.5, 2.5, 3.0, 0.0, 0.0, "GROUND", 1.0, false, false, false)
			.ratings(0.5, 1.0, 1.5, 2.5, 2.8, 1.0, 2.8, 2.8, 2.0, 1.8, 2.0, 0.5)
			.tactical(1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.5, 1.5, 0.0, 1.5, 2.8, 2.8, 2.8, 1.5)
			.affordances("triple_strike", "melee_pressure", "combo_starter"));

		ABILITY_REGISTRY.add(new AbilityInfo("high_sky_slam_combo", K_UHSSC, K_SCC2, 6.0, UPPERCUT_DAMAGE + COMBO_SEISMIC_SLAM_DMG, "anti-air", "aoe", "control")
			.mobility(6.0, 12.0, 4.0, 12.0, 12.0, "LEAP", 3.0, true, true, true)
			.targeting(2.5, 2.5, 1.0, true, true, true, true, true, true)
			.ratings(3.0, 2.2, 2.8, 2.5, 1.0, 2.2, 2.0, 2.5, 2.2, 1.5, 2.8, 1.5)
			.control(2.5, true, true, true, true, true, 0.0, 6.0)
			.tactical(2.0, 3.0, 2.5, 2.8, 2.0, 2.2, 3.0, 1.0, 2.0, 2.5, 2.0, 2.0, 2.5, 2.2)
			.affordances("self_airborne", "target_airborne", "juggle_opportunity", "aerial_followup_window"));

		ABILITY_REGISTRY.add(new AbilityInfo("knockback_dropkick_combo", "unlocked_knockback_dropkick_combo", K_SCC3, 6.0, COMBO_JUDGMENT_KICK_DMG, "gap-closer", "control")
			.mobility(200.0, 20.0, 200.0, 5.0, 5.0, "DASH", 4.5, true, true, true)
			.targeting(3.0, 3.0, 2.5, true, true, true, true, true, true)
			.ratings(2.0, 3.0, 2.5, 2.8, 0.0, 3.0, 1.5, 2.0, 2.5, 2.0, 2.5, 1.0)
			.control(3.0, true, false, false, false, false, 4.0, 200.0)
			.tactical(3.0, 0.5, 0.5, 3.0, 3.0, 3.0, 2.0, 1.5, 0.0, 2.5, 2.5, 1.5, 2.0, 2.2)
			.affordances("large_displacement", "knockback", "spacing", "pursuit_opportunity"));

		ABILITY_REGISTRY.add(new AbilityInfo("heavenly_repentance_plus", "unlocked_heavenly_repentance_plus", K_SCC5, 5.0, COMBO_DIE_RIDER_KICK_DMG, "burst", "aoe", "control")
			.mobility(8.0, 8.0, 8.0, 5.0, 5.0, "LEAP", 3.0, true, true, true)
			.ratings(2.2, 2.5, 3.0, 3.0, 1.0, 2.5, 2.0, 2.5, 2.8, 2.0, 2.8, 2.0)
			.control(2.8, true, false, true, true, true, 0.0, 8.0)
			.affordances("divine_explosion", "massive_knockback", "shockwave", "finisher_slam"));

		ABILITY_REGISTRY.add(new AbilityInfo("armor_rip", "unlocked_armor_rip", K_RARC, ARMOR_RIP_TRIGGER_DISTANCE, CHOKE_DAMAGE, "anti-shield", "control", "drain")
			.mobility(3.5, 2.0, 3.5, 0.0, 0.0, "GROUND", 1.0, false, false, false)
			.ratings(0.0, 1.0, 3.0, 2.0, 2.5, 1.5, 1.0, 2.0, 1.5, 3.0, 3.0, 0.5)
			.tactical(1.0, 0.0, 0.0, 1.0, 1.5, 2.0, 0.0, 3.0, 0.0, 2.5, 2.5, 1.0, 2.0, 1.5)
			.affordances("armor_destruction", "choke_grab", "protection_strip"));

		ABILITY_REGISTRY.add(new AbilityInfo("block", "unlocked_block", K_RBC, 5.5, 0.0, "control")
			.ratings(0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
			.affordances("damage_reduction", "parry_window"));
	}

	public static List<AbilityInfo> getAvailableAbilities(Entity self) {
		List<AbilityInfo> available = new ArrayList<>();
		if (self == null) return available;
		for (AbilityInfo ability : ABILITY_REGISTRY) {
			boolean unlocked = false;
			if ("unlocked_armor_rip".equals(ability.unlockFlag)) {
				unlocked = ENABLE_ARMOR_RIP && getRotPersistentBoolean(self, "unlocked_armor_rip", true);
			} else if ("unlocked_block".equals(ability.unlockFlag)) {
				unlocked = ENABLE_BLOCKING && getRotPersistentBoolean(self, "unlocked_block", true);
			} else if (ability.unlockFlag != null) {
				unlocked = getRotPersistentBoolean(self, ability.unlockFlag, false);
			} else {
				unlocked = true;
			}

			if (!unlocked) continue;

			if (ability.cooldownKey != null) {
				double cd = getRotPersistentDouble(self, ability.cooldownKey, 0.0);
				if (cd > 0.0) continue;
			}
			available.add(ability);
		}
		return available;
	}

	public static void recordAttack(Entity self, String attackType) {
		String last1 = getRotPersistentString(self, K_SM1, "");
		String last2 = getRotPersistentString(self, K_SM2, "");
		self.getPersistentData().putString(K_SM3, last2);
		self.getPersistentData().putString(K_SM2, last1);
		self.getPersistentData().putString(K_SM1, attackType);

		String history = getRotPersistentString(self, K_RAH, "");
		List<String> list = new ArrayList<>();
		if (!history.isEmpty()) {
			for (String s : history.split(",")) {
				if (!s.trim().isEmpty()) list.add(s.trim());
			}
		}
		list.add(attackType);
		while (list.size() > 10) {
			list.remove(0);
		}
		self.getPersistentData().putString(K_RAH, String.join(",", list));
	}

	public static double getTelegraphJitter(Entity entity, String key, double baseTick, double minOffset, double maxOffset) {
		if (entity == null) return baseTick;
		long uuidBits = entity.getUUID().getLeastSignificantBits();
		double castCount = getRotPersistentDouble(entity, key + "_cast_instance", 0.0);
		double h = Math.abs((uuidBits ^ Double.doubleToRawLongBits(castCount)) % 1000) / 1000.0;
		double offset = minOffset + h * (maxOffset - minOffset);
		return Math.round(baseTick + offset);
	}

	public static double getDynamicRangeThreshold(Entity entity, String key, double baseRange, double variance) {
		if (entity == null) return baseRange;
		long uuidBits = entity.getUUID().getLeastSignificantBits();
		double h = Math.abs((uuidBits ^ key.hashCode()) % 1000) / 1000.0;
		return baseRange + (h - 0.5) * 2.0 * variance;
	}

	public static double getEffectiveCombatTicks(Entity entity) {
		if (entity == null) return 0.0;
		double rawTicks = getRotPersistentDouble(entity, K_SCT, 0.0);
		double playerDps = getRotPersistentDouble(entity, "sentinel_player_dps_window", 0.0);
		double misses = getRotPersistentDouble(entity, K_SHPM, 0.0);
		double blockSuccess = getRotPersistentDouble(entity, "sentinel_target_block_rate", 0.2);
		double dpsFactor = Math.min(2.5, 1.0 + (playerDps / 10.0) * 0.5);
		double skillFactor = Math.min(2.0, 1.0 + (misses * 0.08) + (blockSuccess * 0.5));
		return rawTicks * dpsFactor * skillFactor;
	}

	public static AbilityInfo getAbilityById(String id) {
		if (id == null) return null;
		for (AbilityInfo info : ABILITY_REGISTRY) {
			if (id.equals(info.id)) return info;
		}
		return null;
	}

	public static boolean evaluateComboTriggerChance(Entity self, Entity target, CombatContext ctx) {
		if (self == null || target == null) return false;
		boolean totemActive = getRotPersistentBoolean(self, K_STA2, false);
		double baseChance = totemActive ? 0.85 : 0.28;
		double multiplier = 1.0;
		double dist = self.distanceTo(target);
		if (dist <= 4.0) multiplier *= 1.4;
		if (ctx != null) {
			if (ctx.isAirborne) multiplier *= 1.3;
			if (ctx.isBlocking) multiplier *= 1.5;
			if (ctx.isCornered) multiplier *= 1.6;
			if (ctx.isHealing) multiplier *= 1.4;
		}
		double threat = getRotPersistentDouble(self, "sentinel_threat_score", 0.0);
		if (threat > 50.0) multiplier *= 1.3;
		double finalChance = Math.min(0.95, baseChance * multiplier);
		return Math.random() < finalChance;
	}

	public enum TargetIntent {
		ENGAGING,
		ESCAPING,
		AERIAL_ADVANTAGE,
		REPOSITIONING,
		HEALING,
		RANGED_ATTACK,
		BAITING,
		CREATING_DISTANCE
	}

	public static TargetIntent inferTargetIntent(Entity self, Entity target) {
		if (self == null || target == null) return TargetIntent.ENGAGING;

		Vec3 targetVel = target.getDeltaMovement();
		double speedSq = targetVel.horizontalDistanceSqr();
		double dist = self.distanceTo(target);

		if (target.isPassenger() && target.getVehicle() != null) {
			String vType = target.getVehicle().getType().toString().toLowerCase();
			if (vType.contains("boat") || vType.contains("horse") || vType.contains("minecart")) {
				return TargetIntent.ESCAPING;
			}
		}

		if (target instanceof LivingEntity liv) {
			if (liv.isFallFlying() || (!liv.onGround() && target.getY() > self.getY() + 2.5 && !liv.isInWater())) {
				return TargetIntent.AERIAL_ADVANTAGE;
			}
		}

		if (target.level() instanceof ServerLevel sLvl) {
			AABB searchBox = target.getBoundingBox().inflate(48.0);
			java.util.List<net.minecraft.world.entity.projectile.ThrownEnderpearl> pearls = sLvl.getEntitiesOfClass(net.minecraft.world.entity.projectile.ThrownEnderpearl.class, searchBox);
			for (net.minecraft.world.entity.projectile.ThrownEnderpearl p : pearls) {
				if (p.isAlive() && (p.getOwner() == null || p.getOwner().equals(target))) {
					return TargetIntent.REPOSITIONING;
				}
			}
		}

		if (target instanceof LivingEntity liv) {
			if (liv.isUsingItem()) {
				ItemStack useItem = liv.getUseItem();
				if (useItem.is(net.minecraft.world.item.Items.GOLDEN_APPLE)
					|| useItem.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE)
					|| useItem.is(net.minecraft.world.item.Items.MILK_BUCKET)
					|| useItem.getItem() instanceof net.minecraft.world.item.PotionItem) {
					return TargetIntent.HEALING;
				}
			}

			ItemStack mainHand = liv.getMainHandItem();
			if (mainHand.getItem() instanceof net.minecraft.world.item.BowItem 
				|| mainHand.getItem() instanceof net.minecraft.world.item.CrossbowItem
				|| mainHand.getItem() instanceof net.minecraft.world.item.TridentItem) {
				if (dist > 5.0) return TargetIntent.RANGED_ATTACK;
			}

			if (liv.isBlocking()) {
				return TargetIntent.BAITING;
			}

			boolean lowHealth = liv.getHealth() < liv.getMaxHealth() * 0.4f;
			Vec3 awayVector = target.position().subtract(self.position()).normalize();
			double dotAway = targetVel.normalize().dot(awayVector);
			if (liv.isSprinting() && dotAway > 0.5 && dist > 4.0) {
				return TargetIntent.ESCAPING;
			}
			if (lowHealth && speedSq > 0.02 && dotAway > 0.3) {
				return TargetIntent.ESCAPING;
			}
			if (dist > 8.0 && liv.isSprinting() && dotAway > 0.4) {
				return TargetIntent.CREATING_DISTANCE;
			}
		}

		return TargetIntent.ENGAGING;
	}

	public static void adaptCapabilitiesToIntent(Entity self, TargetIntent intent) {
		if (self == null || intent == null) return;
		net.minecraft.nbt.CompoundTag data = self.getPersistentData();
		if (intent == TargetIntent.ESCAPING) {
			data.putBoolean(K_UDC, true);
			data.putBoolean("unlocked_knockback_dropkick_combo", true);
			data.putBoolean("unlocked_telekinesis", true);
			data.putBoolean(K_UT, true);
		} else if (intent == TargetIntent.AERIAL_ADVANTAGE) {
			data.putBoolean(K_UHSSC, true);
			data.putBoolean(K_UKRC, true);
			data.putBoolean("unlocked_sonic_scream", true);
			data.putBoolean(K_UT, true);
		} else if (intent == TargetIntent.REPOSITIONING) {
			data.putBoolean(K_UT, true);
			data.putBoolean(K_UKRC, true);
			data.putBoolean(K_UDC, true);
		}
	}

	private static void cancelActiveCombosAndAbilities(Entity entity) {
		if (entity == null) return;
		net.minecraft.nbt.CompoundTag data = entity.getPersistentData();
		data.putDouble(K_SMT, 0);
		data.putDouble(K_SMS, 0);
		data.putDouble(K_SMWT, 0);
		data.putDouble(K_ROT, 0);
		data.putDouble(K_SST2, 0);
		data.putDouble(K_SSP, 0);
		data.putDouble(K_SDKT, 0);
		data.putDouble(K_SDKP, 0);
		data.putDouble(K_SCT3, 0);
		data.putDouble(K_SCS2, 0);
		data.putDouble(K_SCT2, 0);
		data.putDouble(K_SCS, 0);
		data.putDouble(K_SCT4, 0);
		data.putDouble(K_SCS3, 0);
		data.putDouble(K_SCT5, 0);
		data.putDouble(K_SCS4, 0);
		data.putDouble(K_SCT6, 0);
		data.putDouble(K_SCS5, 0);
		data.putDouble(K_SSFT, 0);
		data.putDouble(K_SSCT, 0);
		data.putDouble(K_SCFT, 0);
		data.putDouble(K_SCCT, 0);
		data.putDouble("sentinel_grapple_ticks", 0);
		data.putDouble("sentinel_tk_ticks", 0);
		data.putDouble(K_SST, 0);
		data.putDouble(K_SLCT, 0);
		data.putDouble(K_SSWST, 0);
		data.putDouble(K_SJT, 0);
		data.putDouble(K_SRHT, 0);
		data.putDouble(K_SOSCT, 0);
		data.putDouble(K_SSST, 0);
		data.putDouble(K_RART, 0);
		data.putBoolean(K_IAR, false);
		data.putBoolean(K_IU, false);
		data.putBoolean(K_SSPT, false);
	}

	private static boolean handleTotemStealing(LevelAccessor world, Entity entity, @Nullable Entity combatTarget) {
		Player p = null;
		if (combatTarget instanceof Player playerTarget) {
			p = playerTarget;
		} else if (entity instanceof Mob mob && mob.getTarget() instanceof Player playerTarget) {
			p = playerTarget;
		}
		if (entity == null || p == null) {
			return false;
		}

		boolean stolenTotemActive = getRotPersistentBoolean(entity, K_STA2, false);
		boolean hasStolenTotem = getRotPersistentBoolean(entity, K_STS, false);

		if (stolenTotemActive || hasStolenTotem) {
			return false;
		}

		double inspectTicks = getRotPersistentDouble(entity, K_STIT, 0.0);
		if (inspectTicks > 0) {
			return false;
		}

		double stealTimer = getRotPersistentDouble(entity, K_STST, 0.0);
		double combatTicks = getRotPersistentDouble(entity, K_SCT, 0.0);
		net.minecraft.world.item.ItemStack totemStack = net.minecraft.world.item.ItemStack.EMPTY;
		boolean isInfinityTotem = false;
		boolean isHeldTotem = false;
		int currentTotemSlot = -99;

		net.minecraft.world.item.ItemStack mainHand = p.getMainHandItem();
		net.minecraft.world.item.ItemStack offHand = p.getOffhandItem();

		if (!mainHand.isEmpty()) {
			String itemId = BuiltInRegistries.ITEM.getKey(mainHand.getItem()).toString();
			if (itemId.equals("avaritia:infinity_totem")) {
				totemStack = mainHand;
				isInfinityTotem = true;
				isHeldTotem = true;
				currentTotemSlot = -1;
			} else if (mainHand.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING) {
				totemStack = mainHand;
				isHeldTotem = true;
				currentTotemSlot = -1;
			}
		}

		if (totemStack.isEmpty() && !offHand.isEmpty()) {
			String itemId = BuiltInRegistries.ITEM.getKey(offHand.getItem()).toString();
			if (itemId.equals("avaritia:infinity_totem")) {
				totemStack = offHand;
				isInfinityTotem = true;
				isHeldTotem = true;
				currentTotemSlot = -2;
			} else if (offHand.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING) {
				totemStack = offHand;
				isHeldTotem = true;
				currentTotemSlot = -2;
			}
		}

		boolean totemLearned = getRotPersistentBoolean(entity, K_STL, false);

		if (isHeldTotem && !totemLearned) {
			double observeProgress = getRotPersistentDouble(entity, K_STOP, 0.0);
			observeProgress += 1.0;
			entity.getPersistentData().putDouble(K_STOP, observeProgress);
			if (observeProgress >= 60.0) {
				totemLearned = true;
				entity.getPersistentData().putBoolean(K_STL, true);
				if (world instanceof ServerLevel level) {
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sniff", 1.2F, 0.6F);
					RotDialoguesProcedure.sendTotemObserved(p);
				}
			}
		}

		if (totemStack.isEmpty() && totemLearned) {
			for (int slot = 0; slot < p.getInventory().getContainerSize(); slot++) {
				net.minecraft.world.item.ItemStack s = p.getInventory().getItem(slot);
				if (!s.isEmpty()) {
					String itemId = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
					if (itemId.equals("avaritia:infinity_totem")) {
						totemStack = s;
						isInfinityTotem = true;
						currentTotemSlot = slot;
						break;
					}
				}
			}
			if (totemStack.isEmpty()) {
				for (int slot = 0; slot < p.getInventory().getContainerSize(); slot++) {
					net.minecraft.world.item.ItemStack s = p.getInventory().getItem(slot);
					if (!s.isEmpty() && s.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING) {
						totemStack = s;
						currentTotemSlot = slot;
						break;
					}
				}
			}
		}

		double awareness = getRotPersistentDouble(entity, K_STA, 0.0);
		int lastTotemCount = getRotPersistentInt(entity, K_SLTC, -1);
		int currentTotemCount = 0;

		for (int slot = 0; slot < p.getInventory().getContainerSize(); slot++) {
			net.minecraft.world.item.ItemStack s = p.getInventory().getItem(slot);
			if (!s.isEmpty() && (s.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING || BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals("avaritia:infinity_totem"))) {
				currentTotemCount += s.getCount();
			}
		}

		boolean justStoleThisTick = getRotPersistentBoolean(entity, K_SJST, false);
		if (justStoleThisTick) {
			entity.getPersistentData().putBoolean(K_SJST, false);
		} else if (lastTotemCount >= 0 && currentTotemCount < lastTotemCount) {
			totemLearned = true;
			entity.getPersistentData().putBoolean(K_STL, true);
			entity.getPersistentData().putDouble(K_STA, 160.0);
			int popsWitnessed = getRotPersistentInt(entity, K_STPW, 0) + (lastTotemCount - currentTotemCount);
			entity.getPersistentData().putInt(K_STPW, popsWitnessed);
			if (world instanceof ServerLevel level) {
				playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "entity.warden.sniff", 1.5F, 0.5F);
				RotDialoguesProcedure.sendTotemPopped(p);
			}
		}

		entity.getPersistentData().putInt(K_SLTC, currentTotemCount);

		if (!totemStack.isEmpty() && isHeldTotem) {
			awareness += 1.0;
			entity.getPersistentData().putDouble(K_STA, awareness);
		}

		if (totemStack.isEmpty()) {
			entity.getPersistentData().putDouble(K_STST, 0.0);
			entity.getPersistentData().putInt(K_SLTS, -99);
			return false;
		}

		if (isChannelingAbility(entity) || getRotPersistentBoolean(entity, K_IAR, false)) {
			return false;
		}

		if (totemLearned) {
			int lastSlot = entity.getPersistentData().contains(K_SLTS) ? entity.getPersistentData().getInt(K_SLTS).orElse(-99) : -99;
			boolean slotSwapped = (lastSlot != -99 && lastSlot != currentTotemSlot);
			entity.getPersistentData().putInt(K_SLTS, currentTotemSlot);

			int popsWitnessed = getRotPersistentInt(entity, K_STPW, 0);
			double minTime = isHeldTotem ? 10.0 : (popsWitnessed > 0 ? 25.0 : 40.0);
			double maxTime = isHeldTotem ? 20.0 : (popsWitnessed > 0 ? 45.0 : 65.0);
			double reqCombatTicks = 0.0;
			double maxDist = 6.0;

			double targetStealTime = getRotPersistentDouble(entity, K_STTST, 0.0);
			if (targetStealTime <= 0 || targetStealTime > maxTime) {
				double range = maxTime - minTime;
				if (range > 0) {
					targetStealTime = minTime + RandomSource.create().nextInt((int) range + 1);
				} else {
					targetStealTime = minTime;
				}
				entity.getPersistentData().putDouble(K_STTST, targetStealTime);
			}

			if (entity.distanceTo(p) <= maxDist && combatTicks >= reqCombatTicks) {
				double increment = isHeldTotem ? 3.0 : 1.5;
				if (totemLearned) {
					increment += 1.0;
				}
				if (slotSwapped) {
					increment += 1.5;
				}
				float hpRatio = p.getHealth() / p.getMaxHealth();
				if (hpRatio < 0.25f) {
					increment += 2.0;
				} else if (hpRatio < 0.5f) {
					increment += 1.0;
				}

				stealTimer += increment;
				entity.getPersistentData().putDouble(K_STST, stealTimer);
				if (stealTimer >= targetStealTime) {
					cancelActiveCombosAndAbilities(entity);
					stopHostileSound(world, entity.getX(), entity.getY(), entity.getZ(), "the_backwoods:fractus_laser", 256.0);

					totemStack.shrink(1);
					entity.getPersistentData().putBoolean(K_SIIT, isInfinityTotem);
					if (entity instanceof LivingEntity living) {
						net.minecraft.world.item.Item infinityTotemItem = isInfinityTotem ? BuiltInRegistries.ITEM.get(ResourceLocation.parse("avaritia:infinity_totem")).map(ref -> ref.value()).orElse(null) : null;
						net.minecraft.world.item.ItemStack stackToHold = (infinityTotemItem != null) ? new net.minecraft.world.item.ItemStack(infinityTotemItem) : new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
						living.setItemInHand(InteractionHand.MAIN_HAND, stackToHold);
					}
					entity.getPersistentData().putBoolean(K_STS, true);
					entity.getPersistentData().putBoolean(K_SJST, true);
					entity.getPersistentData().putBoolean(K_STL, true);
					entity.getPersistentData().putDouble(K_STIT, 180);
					entity.getPersistentData().putDouble(K_SSCT, 0);
					entity.getPersistentData().putDouble(K_SSFT, 0);
					entity.getPersistentData().putDouble(K_SCCT, 0);
					entity.getPersistentData().putDouble(K_SCFT, 0);
					if (world instanceof ServerLevel level) {
						playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "block.bell.resonate", 1.5F, 0.6F);
						if (!isInfinityTotem) {
							RotDialoguesProcedure.sendTotemStolen(p);
						}
					}
					handlePassengerAndGrowth(entity);
					return true;
				}
			} else {
				if (stealTimer > 0) {
					entity.getPersistentData().putDouble(K_STST, Math.max(0.0, stealTimer - 0.5));
				}
			}
		}

		return false;
	}

	private static boolean checkAndSeekDroppedTotems(LevelAccessor world, Entity entity) {
		if (world == null || entity == null) return false;
		if (getRotPersistentBoolean(entity, K_STA2, false) || getRotPersistentBoolean(entity, K_STS, false)) return false;
		if (getRotPersistentDouble(entity, K_STIT, 0.0) > 0) return false;
		if (isChannelingAbility(entity) || getRotPersistentBoolean(entity, K_IAR, false)) return false;

		List<net.minecraft.world.entity.item.ItemEntity> items = world.getEntitiesOfClass(
			net.minecraft.world.entity.item.ItemEntity.class,
			AABB.ofSize(entity.position(), 96.0, 96.0, 96.0)
		);

		net.minecraft.world.entity.item.ItemEntity bestItem = null;
		boolean bestIsInfinity = false;
		double bestDistSq = Double.MAX_VALUE;

		for (net.minecraft.world.entity.item.ItemEntity itemEnt : items) {
			if (itemEnt == null || !itemEnt.isAlive() || itemEnt.getItem().isEmpty()) continue;
			net.minecraft.world.item.ItemStack stack = itemEnt.getItem();
			String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			boolean isInfinity = itemId.equals("avaritia:infinity_totem");
			boolean isVanillaTotem = stack.getItem() == net.minecraft.world.item.Items.TOTEM_OF_UNDYING;

			if (!isInfinity && !isVanillaTotem) continue;

			if (isHazardousLocation(world, itemEnt.getX(), itemEnt.getY(), itemEnt.getZ())) {
				continue;
			}

			double distSq = entity.distanceToSqr(itemEnt);

			if (bestItem == null) {
				bestItem = itemEnt;
				bestIsInfinity = isInfinity;
				bestDistSq = distSq;
			} else {
				if (isInfinity && !bestIsInfinity) {
					bestItem = itemEnt;
					bestIsInfinity = true;
					bestDistSq = distSq;
				} else if (isInfinity == bestIsInfinity) {
					if (RandomSource.create().nextBoolean() || distSq < bestDistSq) {
						bestItem = itemEnt;
						bestIsInfinity = isInfinity;
						bestDistSq = distSq;
					}
				}
			}
		}

		if (bestItem != null) {
			entity.getPersistentData().putBoolean(K_STL, true);
			double dist = entity.distanceTo(bestItem);

			if (dist <= 2.8) {
				bestItem.discard();
				entity.getPersistentData().putBoolean(K_SIIT, bestIsInfinity);
				entity.getPersistentData().putBoolean(K_STS, true);
				entity.getPersistentData().putDouble(K_STIT, 180);
				entity.getPersistentData().putDouble(K_SSCT, 0);
				entity.getPersistentData().putDouble(K_SSFT, 0);
				entity.getPersistentData().putDouble(K_SCCT, 0);
				entity.getPersistentData().putDouble(K_SCFT, 0);
				if (world instanceof ServerLevel level) {
					playHostileSound(level, entity.getX(), entity.getY(), entity.getZ(), "block.bell.resonate", 1.5F, 0.6F);
					level.sendParticles(ParticleTypes.ENCHANTED_HIT, bestItem.getX(), bestItem.getY(), bestItem.getZ(), 35, 0.4, 0.4, 0.4, 0.2);
				}
				handlePassengerAndGrowth(entity);
				return true;
			} else {
				if (entity instanceof Mob mob) {
					mob.getNavigation().moveTo(bestItem.getX(), bestItem.getY(), bestItem.getZ(), 1.45);
				}
				snapLookAtTarget(entity, bestItem);
				if (dist <= 10.0) {
					Vec3 pull = entity.position().subtract(bestItem.position()).normalize().scale(0.18);
					bestItem.setDeltaMovement(bestItem.getDeltaMovement().add(pull));
					bestItem.hasImpulse = true;
				}
				return true;
			}
		}
		return false;
	}

	public static boolean isHazardousLocation(LevelAccessor world, double x, double y, double z) {
		if (world == null) return true;
		if (y <= world.getMinY() + 1.0) return true;

		BlockPos posFeet = BlockPos.containing(x, y, z);
		BlockPos posHead = posFeet.above();
		BlockPos posBelow = posFeet.below();

		net.minecraft.world.level.block.state.BlockState feetState = world.getBlockState(posFeet);
		net.minecraft.world.level.block.state.BlockState headState = world.getBlockState(posHead);
		net.minecraft.world.level.block.state.BlockState belowState = world.getBlockState(posBelow);

		if (feetState.is(net.minecraft.world.level.block.Blocks.LAVA) || belowState.is(net.minecraft.world.level.block.Blocks.LAVA)) return true;
		if (feetState.is(net.minecraft.world.level.block.Blocks.WATER) || feetState.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)) return true;
		if (belowState.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) || feetState.is(net.minecraft.world.level.block.Blocks.FIRE) || feetState.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) return true;
		if (feetState.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH) || feetState.is(net.minecraft.world.level.block.Blocks.WITHER_ROSE) || feetState.is(net.minecraft.world.level.block.Blocks.COBWEB)) return true;

		if (feetState.is(net.minecraft.world.level.block.Blocks.TNT) || belowState.is(net.minecraft.world.level.block.Blocks.TNT)) return true;
		if (feetState.is(net.minecraft.world.level.block.Blocks.TRIPWIRE) || feetState.getBlock() instanceof net.minecraft.world.level.block.BasePressurePlateBlock) return true;

		if (feetState.isSolid() && headState.isSolid()) return true;

		if (!belowState.isSolid() && !belowState.isCollisionShapeFullBlock(world, posBelow)) {
			int airCount = 0;
			for (int dy = 1; dy <= 5; dy++) {
				BlockPos checkPos = posFeet.below(dy);
				if (!world.getBlockState(checkPos).isSolid()) {
					airCount++;
				} else {
					break;
				}
			}
			if (airCount >= 4) return true;
		}

		return false;
	}

	public static boolean interceptEnderPearlsPipeline(LevelAccessor world, Entity self, Entity target) {
		if (!(world instanceof ServerLevel level) || self == null || target == null) return false;

		AABB box = target.getBoundingBox().inflate(48.0);
		java.util.List<net.minecraft.world.entity.projectile.ThrownEnderpearl> pearls = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.ThrownEnderpearl.class, box);

		net.minecraft.world.entity.projectile.ThrownEnderpearl targetPearl = null;
		for (net.minecraft.world.entity.projectile.ThrownEnderpearl p : pearls) {
			if (p.isAlive() && (p.getOwner() == null || p.getOwner().equals(target))) {
				targetPearl = p;
				break;
			}
		}

		if (targetPearl == null) return false;

		cancelActiveCombosAndAbilities(self);

		Vec3 pearlPos = targetPearl.position();
		Vec3 pearlVel = targetPearl.getDeltaMovement();

		Vec3 predictedLanding = pearlPos.add(pearlVel.scale(15.0));
		BlockHitResult hit = level.clip(new ClipContext(pearlPos, pearlPos.add(pearlVel.scale(30.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, targetPearl));
		if (hit.getType() != HitResult.Type.MISS) {
			predictedLanding = hit.getLocation();
		}

		int pearlSeed = targetPearl.getId();
		double offsetAngle = (pearlSeed % 360) * (Math.PI / 180.0);
		double offsetRadius = 1.6 + (Math.abs(pearlSeed * 37) % 15) * 0.1;
		double ambushX = predictedLanding.x + Math.cos(offsetAngle) * offsetRadius;
		double ambushZ = predictedLanding.z + Math.sin(offsetAngle) * offsetRadius;
		double groundY = findTargetGroundY(level, ambushX, predictedLanding.y, ambushZ);

		boolean isTrap = isHazardousLocation(level, ambushX, groundY, ambushZ);

		if (isTrap) {
			if (self instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			self.setDeltaMovement(self.getDeltaMovement().x() * 0.1, self.getDeltaMovement().y(), self.getDeltaMovement().z() * 0.1);

			boolean unlockedSolar = getRotPersistentBoolean(self, K_USB2, false);
			boolean unlockedCryo = getRotPersistentBoolean(self, K_UCB, false);
			if ((unlockedSolar || unlockedCryo) && getRotPersistentDouble(self, K_SGAC, 0.0) <= 0) {
				lockLookAtTarget(self, targetPearl);
				if (unlockedSolar) {
					setRotPersistentDouble(self, K_SSFT, 15.0);
				} else {
					setRotPersistentDouble(self, K_SCFT, 15.0);
				}
				setRotPersistentDouble(self, K_SGAC, 20.0);
			} else {
				lockLookAtTarget(self, target);
			}
			return true;
		}

		double distToAmbush = self.position().distanceTo(new Vec3(ambushX, groundY, ambushZ));

		if (self instanceof Mob mob) {
			mob.getNavigation().stop();
		}
		self.setDeltaMovement(self.getDeltaMovement().x() * 0.1, self.getDeltaMovement().y(), self.getDeltaMovement().z() * 0.1);

		lockLookAtTarget(self, targetPearl);
		self.getPersistentData().putBoolean(K_SWI, true);

		self.getPersistentData().putBoolean(K_UT, true);
		self.getPersistentData().putBoolean(K_UKRC, true);
		self.getPersistentData().putBoolean(K_UDC, true);

		double pearlFlightDist = pearlPos.distanceTo(predictedLanding);
		boolean aboutToLand = pearlFlightDist < 3.0 || targetPearl.tickCount > 12;

		if (aboutToLand && distToAmbush > 2.0) {
			double globalCd = getRotPersistentDouble(self, K_SGAC, 0.0);
			if (globalCd <= 0) {
				teleportEntity(self, ambushX, groundY, ambushZ);
				self.setOnGround(true);
				self.setDeltaMovement(0.0, 0.0, 0.0);
				lockLookAtTarget(self, target);

				self.getPersistentData().putDouble(K_SCS4, 1);
				self.getPersistentData().putDouble(K_SCT5, 20);
				self.getPersistentData().putDouble(K_SCAT, 120);
				self.getPersistentData().putDouble(K_SGAC, 30.0);
			}
		} else if (self instanceof Mob mob && distToAmbush > 2.0) {
			mob.getNavigation().moveTo(ambushX, groundY, ambushZ, ROT_RUN_SPEED);
		}

		return true;
	}

	public static InterceptionPrediction evaluateInterceptionPipeline(Entity self, Entity target, TargetIntent intent) {
		if (self == null || target == null) {
			return new InterceptionPrediction(target != null ? target.position() : Vec3.ZERO, 0.0, 0.5, false, 0.0, null);
		}

		Vec3 selfPos = self.position();
		Vec3 targetPos = target.position();
		Vec3 targetVel = target.getDeltaMovement();

		double dist = selfPos.distanceTo(targetPos);

		double leadSec = 0.5 + Math.min(1.5, dist / 10.0);
		Vec3 predictedTargetPos = targetPos.add(targetVel.scale(leadSec * 20.0));

		double confidence = 0.85;
		if (intent == TargetIntent.AERIAL_ADVANTAGE) {
			confidence = 0.90;
		} else if (intent == TargetIntent.ESCAPING) {
			confidence = 0.88;
		} else if (intent == TargetIntent.REPOSITIONING) {
			confidence = 0.95;
		}

		if (self.level() instanceof ServerLevel level) {
			BlockHitResult hit = level.clip(new ClipContext(targetPos, predictedTargetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
			if (hit.getType() != HitResult.Type.MISS) {
				predictedTargetPos = hit.getLocation();
				confidence *= 0.6;
			}
		}

		boolean recommendWait = false;
		double waitTicks = 0.0;
		Vec3 repositionPos = null;

		if (intent == TargetIntent.AERIAL_ADVANTAGE) {
			repositionPos = new Vec3(predictedTargetPos.x, selfPos.y, predictedTargetPos.z);
			double distToReposition = selfPos.distanceTo(repositionPos);
			if (distToReposition > 2.0 && dist > 4.0) {
				recommendWait = true;
				waitTicks = Math.min(30.0, distToReposition * 4.0);
			}
		} else if (intent == TargetIntent.ESCAPING) {
			repositionPos = predictedTargetPos;
			double distToReposition = selfPos.distanceTo(repositionPos);
			if (distToReposition > 4.0 && dist > 6.0) {
				recommendWait = true;
				waitTicks = 15.0;
			}
		}

		return new InterceptionPrediction(predictedTargetPos, leadSec * 20.0, confidence, recommendWait, waitTicks, repositionPos);
	}

	public static class InterceptionPrediction {
		public final Vec3 predictedPos;
		public final double leadTicks;
		public final double interceptProbability;
		public final boolean recommendWait;
		public final double waitTicks;
		public final Vec3 repositionTargetPos;

		public InterceptionPrediction(Vec3 predictedPos, double leadTicks, double interceptProbability, boolean recommendWait, double waitTicks, Vec3 repositionTargetPos) {
			this.predictedPos = predictedPos;
			this.leadTicks = leadTicks;
			this.interceptProbability = interceptProbability;
			this.recommendWait = recommendWait;
			this.waitTicks = waitTicks;
			this.repositionTargetPos = repositionTargetPos;
		}
	}

	public static InterceptionPrediction evaluateInterception(AbilityInfo ability, Entity self, Entity target) {
		if (ability == null || self == null || target == null) {
			return new InterceptionPrediction(target != null ? target.position() : Vec3.ZERO, 0.0, 0.0, false, 0.0, null);
		}

		Vec3 selfPos = self.position();
		Vec3 targetPos = target.position();
		Vec3 targetVel = target.getDeltaMovement();

		boolean isMounted = target.isPassenger();
		boolean inBoat = isMounted && target.getVehicle() != null && target.getVehicle().getType().toString().toLowerCase().contains("boat");
		boolean isFlying = (target instanceof LivingEntity liv) && (liv.isFallFlying() || (!liv.onGround() && targetPos.y > selfPos.y + 2.0));
		boolean inWater = target.isInWater();

		double startupSec = ability.startupTicks / 20.0;
		double travelTime = 0.0;
		double speed = Math.max(0.1, ability.travelSpeed * 1.5);
		double dist = selfPos.distanceTo(targetPos);

		if ("DASH".equals(ability.movementType) || "LEAP".equals(ability.movementType) || "TELEPORT".equals(ability.movementType)) {
			travelTime = Math.min(dist / speed, 2.0);
		} else if ("PROJECTILE".equals(ability.movementType)) {
			travelTime = dist / 2.0;
		} else {
			travelTime = dist / Math.max(0.3, self instanceof LivingEntity liv ? liv.getAttributeValue(Attributes.MOVEMENT_SPEED) * 5.0 : 1.0);
		}

		double totalLeadSec = startupSec + travelTime;
		Vec3 effectiveVel = targetVel;
		if (inBoat) {
			effectiveVel = new Vec3(targetVel.x * 1.25, targetVel.y, targetVel.z * 1.25);
		} else if (isFlying) {
			effectiveVel = new Vec3(targetVel.x, targetVel.y * 0.85, targetVel.z);
		}

		Vec3 predictedPos = targetPos.add(effectiveVel.scale(totalLeadSec * 20.0));
		double maxReach = Math.max(ability.horizontalReach, ability.range);

		double immediateProb = 1.0;
		if (dist > maxReach) {
			immediateProb = 0.0;
		} else {
			immediateProb = Math.max(0.1, 1.0 - (dist / maxReach));
		}

		if (inWater || inBoat) {
			if (ability.usableAgainstBoats || ability.canCrossWater) {
				immediateProb = Math.min(1.0, immediateProb * 1.8);
			} else {
				immediateProb *= 0.3;
			}
		}

		if (isFlying) {
			if (ability.usableAgainstAir || ability.antiAirRating > 1.0) {
				immediateProb = Math.min(1.0, immediateProb * 1.6);
			} else {
				immediateProb *= 0.2;
			}
		}

		Vec3 predPos10 = targetPos.add(effectiveVel.scale(10.0));
		Vec3 predPos20 = targetPos.add(effectiveVel.scale(20.0));
		double dist10 = selfPos.distanceTo(predPos10);
		double dist20 = selfPos.distanceTo(predPos20);

		double prob10 = (dist10 <= maxReach) ? (1.0 - (dist10 / maxReach)) : 0.0;
		double prob20 = (dist20 <= maxReach) ? (1.0 - (dist20 / maxReach)) : 0.0;

		boolean recommendWait = false;
		double waitTicks = 0.0;
		if (prob10 > immediateProb + 0.35) {
			recommendWait = true;
			waitTicks = 10.0;
		} else if (prob20 > immediateProb + 0.50) {
			recommendWait = true;
			waitTicks = 20.0;
		}

		Vec3 repositionPos = null;
		if (isFlying && ability.gainsAltitude > 1.0) {
			repositionPos = new Vec3(predictedPos.x, selfPos.y, predictedPos.z);
		}

		return new InterceptionPrediction(predictedPos, totalLeadSec * 20.0, Math.min(1.0, Math.max(0.0, immediateProb)), recommendWait, waitTicks, repositionPos);
	}

	public static double scoreAbility(AbilityInfo ability, CombatContext ctx, Entity self, Entity target) {
		if (ability == null || self == null || target == null) return 0.0;

		double baseScore = 10.0;
		double multiplier = 1.0;
		double dist = self.distanceTo(target);

		if (target instanceof LivingEntity targetLiv) {
			double targetHp = targetLiv.getHealth();
			double rotDmg = MELEE_PUNCH_DAMAGE * getAdaptationMultiplier(self);
			if (!(target instanceof Player) && (targetHp <= rotDmg || targetHp <= 20.0)) {
				if (ability.hasTag("aoe") || ability.createsAOE) {
					multiplier *= 2.5;
				} else if (ability.hasTag("burst") || ability.id.contains("combo") || "overhead_combo".equals(ability.id) || "high_sky_slam_combo".equals(ability.id)) {
					multiplier *= 0.1;
				}
			}
		}

		TargetIntent intent = inferTargetIntent(self, target);
		if (intent == TargetIntent.AERIAL_ADVANTAGE && (ability.antiAirRating > 0.0 || ability.usableAgainstAir || ability.gainsAltitude > 0.0)) {
			multiplier *= 4.0;
		} else if (intent == TargetIntent.ESCAPING && (ability.usableAgainstBoats || ability.gapCloserRating > 0.0 || ability.escapePunishRating > 0.0)) {
			multiplier *= 4.0;
		} else if (intent == TargetIntent.REPOSITIONING && "TELEPORT".equals(ability.movementType)) {
			multiplier *= 4.0;
		}

		InterceptionPrediction prediction = evaluateInterception(ability, self, target);
		multiplier *= (0.2 + 1.8 * prediction.interceptProbability);

		if (prediction.recommendWait) {
			multiplier *= 0.3;
		}

		double maxReach = Math.max(ability.horizontalReach, ability.range);
		if (dist > maxReach) {
			multiplier *= 0.1;
		} else if (dist < 3.0 && ability.hasTag("ranged")) {
			multiplier *= 0.3;
		} else if (dist > 10.0 && ability.hasTag("ranged")) {
			multiplier *= (1.0 + ability.zoningRating * 0.4);
		} else if (dist > 5.0 && ability.gapCloserRating > 0.0) {
			multiplier *= (1.0 + ability.gapCloserRating * 0.5);
		}

		if (ability.gapCloserRating > 0.0 && dist >= 6.0) {
			multiplier *= (1.0 + ability.gapCloserRating * 0.4);
		}
		if (ability.antiAirRating > 0.0 && (ctx != null && ctx.isAirborne)) {
			multiplier *= (1.0 + ability.antiAirRating * 0.5);
		}
		if (ability.shieldBreakRating > 0.0 && (ctx != null && ctx.isBlocking)) {
			multiplier *= (1.0 + ability.shieldBreakRating * 0.5);
		}
		if (ability.escapePunishRating > 0.0 && target.getDeltaMovement().lengthSqr() > 0.1) {
			multiplier *= (1.0 + ability.escapePunishRating * 0.4);
		}

		if (ctx != null) {
			if (ctx.expectedIncomingDamage > 0.0) {
				boolean defensiveAbility = ability.hasTag("defense") || ability.hasTag("anti-projectile") || "block".equals(ability.id) || "teleport".equals(ability.id) || ability.interruptionRating > 1.5;
				if (defensiveAbility) multiplier *= 1.0 + Math.min(1.5, ctx.expectedIncomingDamage / 20.0);
				if (ctx.expectedIncomingDamage > 0.5 * Math.max(1.0f, self instanceof LivingEntity liv ? liv.getMaxHealth() : 20.0f) && ability.commitment > 2.5) multiplier *= 0.45;
			}
			if (ctx.isAirborne && (ability.hasTag("anti-air") || ability.antiAirRating > 1.0)) multiplier *= 2.0;
			if (ctx.isBlocking && (ability.hasTag("anti-shield") || ability.shieldBreakRating > 1.0)) multiplier *= 2.0;
			if (ctx.isCornered && (ability.hasTag("aoe") || ability.hasTag("control") || ability.crowdControlRating > 1.0)) multiplier *= 1.8;
			if (ctx.isHealing && (ability.hasTag("burst") || ability.burstRating > 1.0)) multiplier *= 2.0;

			if (ctx.isJumpCritIncoming && (ability.hasTag("anti-air") || ability.shieldBreakRating > 0.0 || ability.id.contains("punch") || "block".equals(ability.id) || "uppercut".equals(ability.id))) {
				multiplier *= 2.8;
			}

			if (ctx.incomingProjectileDistance > 0.0 && ctx.incomingProjectileDistance <= 10.0 && (ability.hasTag("anti-projectile") || ability.hasTag("defense") || "block".equals(ability.id) || "teleport".equals(ability.id) || ability.gapCloserRating > 1.5)) {
				multiplier *= 3.0;
			}

			if (ctx.nearbyTargetCount >= 2 && (ability.hasTag("aoe") || ability.createsAOE || "omni_sonic_boom".equals(ability.id) || "sonic_scream".equals(ability.id) || ability.id.contains("slam"))) {
				multiplier *= (1.0 + (ctx.nearbyTargetCount - 1) * 0.75);
			}

			if (ctx.targetNearLedgeOrHazard && (ability.hasTag("knockback") || ability.id.contains("kick") || ability.id.contains("punch") || "sonic_boom".equals(ability.id) || "overhead_combo".equals(ability.id))) {
				multiplier *= 3.5;
			}

			if (ctx.isEnclosedSpace) {
				if (ability.gainsAltitude > 1.0 || "high_sky_slam_combo".equals(ability.id) || "sky_warp_slam".equals(ability.id)) {
					multiplier *= 0.15;
				} else if (ability.hasTag("corridor") || ability.hasTag("grab") || "grapple".equals(ability.id) || "armor_rip".equals(ability.id) || ability.id.contains("beam") || "telekinesis".equals(ability.id)) {
					multiplier *= 2.2;
				}
			}
		}

		if ("omni_sonic_boom".equals(ability.id)) {
			AABB box = self.getBoundingBox().inflate(6.0);
			List<LivingEntity> nearby = self.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != self && e.isAlive());
			if (nearby.size() >= 3) {
				multiplier *= (1.0 + nearby.size() * 0.5);
			} else {
				multiplier *= 0.1;
			}
		} else if (ability.id.contains("beam") || "solar_beam".equals(ability.id) || "cryo_beam".equals(ability.id)) {
			double heat = getRotPersistentDouble(self, K_SLH, 0.0);
			if (heat > 50.0) multiplier *= 0.1;
			else if (heat > 0.0) multiplier *= (1.0 - heat / 100.0);
		}

		String currentStrategy = getRotPersistentString(self, "sentinel_current_strategy", "BALANCED");
		if ("ANTI_AIR".equals(currentStrategy)) {
			if (ability.hasTag("anti-air") || ability.antiAirRating > 1.5) multiplier *= 2.2;
		} else if ("BURST_AND_DISENGAGE".equals(currentStrategy)) {
			if (ability.hasTag("burst") || ability.gapCloserRating > 1.5) multiplier *= 1.8;
			if (ability.hasTag("sustained") || ability.sustainedRating > 2.0) multiplier *= 0.4;
		} else if ("MELEE_DOMINANCE".equals(currentStrategy)) {
			if (maxReach <= 6.0 || ability.gapCloserRating > 1.0) multiplier *= 1.8;
			if (ability.hasTag("ranged")) multiplier *= 0.5;
		} else if ("GROUNDED_STABILITY".equals(currentStrategy)) {
			if (ability.hasTag("control") || ability.gapCloserRating > 1.0) multiplier *= 1.6;
		} else if ("HEAVY_BURST_SPACING".equals(currentStrategy)) {
			if (ability.hasTag("burst") || ability.hasTag("ranged")) multiplier *= 1.8;
		} else if ("AOE_CLEAR".equals(currentStrategy)) {
			if (ability.hasTag("aoe") || ability.createsAOE) multiplier *= 2.2;
		} else if ("FLANK_AND_PUNISH".equals(currentStrategy)) {
			if (ability.hasTag("anti-shield") || ability.shieldBreakRating > 1.5 || ability.hasTag("control")) multiplier *= 1.8;
		} else if ("CONTINUOUS_PRESSURE".equals(currentStrategy)) {
			if (ability.hasTag("sustained") || ability.sustainedRating > 1.5 || ability.hasTag("burst")) multiplier *= 1.6;
		} else if ("EVASIVE_FLANK".equals(currentStrategy)) {
			if (ability.gapCloserRating > 1.5 || ability.hasTag("ranged")) multiplier *= 1.5;
		} else if ("BAIT_AND_PUNISH".equals(currentStrategy)) {
			if (ability.hasTag("control") || ability.hasTag("burst")) multiplier *= 1.6;
		}

		String currentPlan = getRotPersistentString(self, K_STP, "BALANCED_PRESSURE");
		if ("MAINTAIN_DISTANCE".equals(currentPlan)) {
			if (ability.hasTag("ranged") || ability.zoningRating > 1.5) multiplier *= 2.0;
			if (ability.gapCloserRating > 1.0 || maxReach <= 5.0) multiplier *= 0.3;
		} else if ("AGGRESSIVE_CHARGE".equals(currentPlan)) {
			if (ability.gapCloserRating > 1.0 || ability.hasTag("burst")) multiplier *= 2.0;
		} else if ("RETREAT_AND_HEAL".equals(currentPlan)) {
			if (ability.hasTag("ranged") || ability.hasTag("control")) multiplier *= 1.5;
			if (maxReach <= 5.0) multiplier *= 0.2;
		}

		multiplier *= getMemoryPenalty(self, ability.id);

		String last1 = getRotPersistentString(self, K_SM1, "");
		String last2 = getRotPersistentString(self, K_SM2, "");
		if (!last1.isEmpty() && !last2.isEmpty()) {
			String trigram = last2 + "->" + last1 + "->" + ability.id;
			String history = getRotPersistentString(self, K_RAH, "");
			if (history.contains(trigram)) {
				multiplier *= 0.35;
			}
		}

		return baseScore * multiplier * (0.8 + Math.random() * 0.4);
	}

	public static double getMemoryPenalty(Entity self, String attackType) {
		double penalty = 1.0;
		if (self.getPersistentData().getString(K_SM1).orElse("").equals(attackType)) penalty *= 0.15;
		if (self.getPersistentData().getString(K_SM2).orElse("").equals(attackType)) penalty *= 0.4;
		if (self.getPersistentData().getString(K_SM3).orElse("").equals(attackType)) penalty *= 0.7;
		return penalty;
	}

	public static int evaluateComboUtility(Entity self, Entity target, CombatContext ctx, java.util.List<Integer> available) {
		int bestCombo = 0;
		double bestScore = -1.0;
		double playerAir = target.getPersistentData().getDouble(K_ATAT).orElse(0.0);
		double playerShield = target.getPersistentData().getDouble(K_ATST).orElse(0.0);
		TargetIntent intent = inferTargetIntent(self, target);

		double targetHp = (target instanceof LivingEntity tLiv) ? tLiv.getHealth() : 999.0;
		double rotDmg = MELEE_PUNCH_DAMAGE * getAdaptationMultiplier(self);
		boolean lowHpTarget = !(target instanceof Player) && (targetHp <= rotDmg || targetHp <= 20.0);

		for (int c : available) {
			if (lowHpTarget && (c == 5 || c == 8 || c == 12 || c == 101 || c == 102 || c == 103 || c == 104)) {
				continue;
			}
			double score = 10.0;
			String comboName = "combo_" + c;
			score *= getMemoryPenalty(self, comboName);
			score *= self.getPersistentData().getDouble(K_AB + comboName).orElse(1.0);
			double tpComboPenalty = self.getPersistentData().getDouble(K_ATCPT).orElse(0.0);
			if (tpComboPenalty > 0 && (c == 4 || c == 7 || c == 8 || c == 9 || c == 10 || c == 11)) {
				score *= 0.1;
			}

			if (intent == TargetIntent.AERIAL_ADVANTAGE) {
				if (c == 102 || c == 104 || c == 1) score *= 5.0;
			} else if (intent == TargetIntent.ESCAPING) {
				if (c == 13 || c == 103 || c == 104 || c == 3) score *= 5.0;
			} else if (intent == TargetIntent.REPOSITIONING) {
				if (c == 4 || c == 7 || c == 8 || c == 9 || c == 10 || c == 104) score *= 5.0;
			}

			if (c == 1) {
				if (ctx.isAirborne) score *= 0.2;
				if (ctx.isCornered) score *= 1.5;
			} else if (c == 2) {
				if (ctx.dist > 4.0) score *= 2.0;
				if (ctx.isMovingFast) score *= 1.5;
			} else if (c == 3) {
				if (ctx.isAirborne || playerAir > 20) score *= 2.5;
			} else if (c == 4) {
				if (ctx.isBlocking || playerShield > 20) score *= 2.5;
				if (ctx.isMovingFast) score *= 1.5;
			} else if (c == 5) {
				if (!ctx.isAirborne) score *= 1.8;
				if (ctx.isCornered) score *= 1.5;
			} else if (c == 6) {
				if (ctx.isHealing) score *= 2.0;
			} else if (c == 8) {
				if (ctx.isBlocking || playerShield > 20) score *= 2.5;
			} else if (c == 10 || c == 13) {
				if (ctx.dist > 6.0) score *= 2.0;
			} else if (c == 12) {
				if (!ctx.isAirborne) score *= 1.5;
			} else if (c == 14) {
				score *= 3.0;
				if (ctx.isMovingFast) score *= 1.5;
			}
			if (c == 102) {
				score *= 2.5;
				if (playerShield > 20) score *= 1.5;
			}
			if (c == 103 || c == 104) {
				if (playerAir > 20) score *= 2.0;
			}
			score *= (0.8 + Math.random() * 0.4);
			if (score > bestScore) {
				bestScore = score;
				bestCombo = c;
			}
		}
		if (bestCombo != 0) {
			recordAttack(self, "combo_" + bestCombo);
		}
		return bestCombo;
	}

	public enum FightStyle {
		AGGRESSIVE,
		DEFENSIVE,
		HIT_AND_RUN,
		PROJECTILE_FOCUSED,
		BEAM_FOCUSED,
		COMBO_FOCUSED,
		AOE_FOCUSED,
		MOBILITY_FOCUSED,
		COUNTER_ATTACKER,
		TANK,
		SUMMONER,
		SUPPORT,
		HYBRID
	}

	public enum ThreatLevel {
		NONE(0),
		LOW(1),
		MEDIUM(2),
		HIGH(3),
		ATTACK_IMMINENT(4);

		private final int level;
		ThreatLevel(int level) { this.level = level; }
		public int getLevel() { return level; }
		public boolean isHighOrImminent() { return this == HIGH || this == ATTACK_IMMINENT; }
	}

	public static class PersonalityVector {
		public double aggression;
		public double patience;
		public double riskTolerance;
		public double spite;

		public PersonalityVector() {
			java.util.Random rnd = new java.util.Random();
			this.aggression = 0.2 + rnd.nextDouble() * 0.6;
			this.patience = 0.2 + rnd.nextDouble() * 0.6;
			this.riskTolerance = 0.2 + rnd.nextDouble() * 0.6;
			this.spite = 0.2 + rnd.nextDouble() * 0.6;
		}

		public PersonalityVector(double a, double p, double r, double s) {
			this.aggression = Mth.clamp(a, 0.0, 1.0);
			this.patience = Mth.clamp(p, 0.0, 1.0);
			this.riskTolerance = Mth.clamp(r, 0.0, 1.0);
			this.spite = Mth.clamp(s, 0.0, 1.0);
		}

		public void drift(double dA, double dP, double dR, double dS) {
			this.aggression = Mth.clamp(this.aggression + dA * PERSONALITY_DRIFT_RATE, 0.0, 1.0);
			this.patience = Mth.clamp(this.patience + dP * PERSONALITY_DRIFT_RATE, 0.0, 1.0);
			this.riskTolerance = Mth.clamp(this.riskTolerance + dR * PERSONALITY_DRIFT_RATE, 0.0, 1.0);
			this.spite = Mth.clamp(this.spite + dS * PERSONALITY_DRIFT_RATE, 0.0, 1.0);
		}

		public void saveToNbt(CompoundTag tag) {
			if (tag == null) return;
			tag.putDouble("rot_personality_aggression", aggression);
			tag.putDouble("rot_personality_patience", patience);
			tag.putDouble("rot_personality_risk_tolerance", riskTolerance);
			tag.putDouble("rot_personality_spite", spite);
			tag.putBoolean("rot_personality_initialized", true);
		}

		public static PersonalityVector loadFromNbt(CompoundTag tag) {
			if (tag == null || !hasNBTKey(tag, "rot_personality_initialized")) {
				return new PersonalityVector();
			}
			double a = tag.getDouble("rot_personality_aggression").orElse(0.5);
			double p = tag.getDouble("rot_personality_patience").orElse(0.5);
			double r = tag.getDouble("rot_personality_risk_tolerance").orElse(0.5);
			double s = tag.getDouble("rot_personality_spite").orElse(0.5);
			return new PersonalityVector(a, p, r, s);
		}
	}

	public static class TacticalNeuralNetwork {
		public static final int INPUT_SIZE = 53;
		public static final int HIDDEN_SIZE = 20;
		public static final int OUTPUT_SIZE = 15;
		public static final int TOTAL_WEIGHTS = (INPUT_SIZE * HIDDEN_SIZE) + HIDDEN_SIZE + (HIDDEN_SIZE * OUTPUT_SIZE) + OUTPUT_SIZE;

		public double[] weights;

		public TacticalNeuralNetwork() {
			this.weights = new double[TOTAL_WEIGHTS];
			initDefaultWeights();
		}

		public TacticalNeuralNetwork(double[] w) {
			if (w != null && w.length == TOTAL_WEIGHTS) {
				this.weights = w;
			} else {
				this.weights = new double[TOTAL_WEIGHTS];
				initDefaultWeights();
			}
		}

		private void initDefaultWeights() {
			java.util.Random rnd = new java.util.Random(1337);
			for (int i = 0; i < weights.length; i++) {
				weights[i] = (rnd.nextDouble() - 0.5) * 0.2;
			}
		}

		public double[] forward(double[] inputs, double[] hiddenOut) {
			int idx = 0;
			for (int h = 0; h < HIDDEN_SIZE; h++) {
				double sum = weights[idx++];
				for (int i = 0; i < INPUT_SIZE; i++) {
					double val = (i < inputs.length) ? inputs[i] : 0.0;
					sum += val * weights[idx++];
				}
				hiddenOut[h] = Math.tanh(sum);
			}

			double[] outputs = new double[OUTPUT_SIZE];
			for (int o = 0; o < OUTPUT_SIZE; o++) {
				double sum = weights[idx++];
				for (int h = 0; h < HIDDEN_SIZE; h++) {
					sum += hiddenOut[h] * weights[idx++];
				}
				outputs[o] = sum;
			}
			return outputs;
		}

		public void trainDelta(double[] inputs, int chosenPlanOrdinal, double netEfficiency) {
			double[] hiddenOut = new double[HIDDEN_SIZE];
			double[] outputs = forward(inputs, hiddenOut);

			double feedback = Math.max(-2.0, Math.min(2.0, netEfficiency / 5.0));
			double targetOutput = outputs[chosenPlanOrdinal] + feedback;
			double outputError = targetOutput - outputs[chosenPlanOrdinal];

			int idx = (INPUT_SIZE * HIDDEN_SIZE) + HIDDEN_SIZE;
			double[] hiddenErrors = new double[HIDDEN_SIZE];

			for (int o = 0; o < OUTPUT_SIZE; o++) {
				double delta = (o == chosenPlanOrdinal) ? outputError : 0.0;
				weights[idx] = Mth.clamp(weights[idx] + NN_LEARNING_RATE * delta, -5.0, 5.0);
				idx++;
				for (int h = 0; h < HIDDEN_SIZE; h++) {
					hiddenErrors[h] += delta * weights[idx];
					weights[idx] = Mth.clamp(weights[idx] + NN_LEARNING_RATE * delta * hiddenOut[h], -5.0, 5.0);
					idx++;
				}
			}

			idx = 0;
			for (int h = 0; h < HIDDEN_SIZE; h++) {
				double dtanh = 1.0 - (hiddenOut[h] * hiddenOut[h]);
				double hiddenDelta = hiddenErrors[h] * dtanh;
				weights[idx] = Mth.clamp(weights[idx] + NN_LEARNING_RATE * hiddenDelta, -5.0, 5.0);
				idx++;
				for (int i = 0; i < INPUT_SIZE; i++) {
					double val = (i < inputs.length) ? inputs[i] : 0.0;
					weights[idx] = Mth.clamp(weights[idx] + NN_LEARNING_RATE * hiddenDelta * val, -5.0, 5.0);
					idx++;
				}
			}
		}

		public long[] toLongArray() {
			long[] arr = new long[weights.length];
			for (int i = 0; i < weights.length; i++) {
				arr[i] = Double.doubleToRawLongBits(weights[i]);
			}
			return arr;
		}

		public static TacticalNeuralNetwork fromLongArray(long[] arr) {
			if (arr == null || arr.length != TOTAL_WEIGHTS) return new TacticalNeuralNetwork();
			double[] w = new double[TOTAL_WEIGHTS];
			for (int i = 0; i < TOTAL_WEIGHTS; i++) {
				w[i] = Double.longBitsToDouble(arr[i]);
			}
			return new TacticalNeuralNetwork(w);
		}
	}

	public static class WelfordTracker {
		public double count = 0.0;
		public double mean = 0.0;
		public double M2 = 0.0;

		public void update(double val) {
			count += 1.0;
			double delta = val - mean;
			mean += delta / count;
			double delta2 = val - mean;
			M2 += delta * delta2;
		}

		public double getVariance() {
			if (count < 2.0) return 0.0;
			return M2 / (count - 1.0);
		}

		public double getStdDev() {
			return Math.sqrt(getVariance());
		}

		public double calculateZScore(double val) {
			if (count < ANOMALY_MIN_SAMPLES) return 0.0;
			double std = getStdDev();
			if (std < 1e-4) std = 1e-4;
			return Math.abs(val - mean) / std;
		}
	}

	public static class PlayerBehaviorTracker {
		private static final Map<UUID, PlayerBehaviorTracker> TRACKERS = new HashMap<>();

		public WelfordTracker attackIntervalTracker = new WelfordTracker();
		public WelfordTracker distanceTracker = new WelfordTracker();
		public long lastAttackTick = 0;

		public static PlayerBehaviorTracker get(UUID playerUuid) {
			return TRACKERS.computeIfAbsent(playerUuid, k -> new PlayerBehaviorTracker());
		}

		public static void remove(UUID playerUuid) {
			TRACKERS.remove(playerUuid);
		}

		public boolean observe(long currentTick, double distance, boolean isAttack) {
			boolean isSurprise = false;
			if (distance > 0) {
				double zDist = distanceTracker.calculateZScore(distance);
				if (zDist >= SURPRISE_Z_SCORE_THRESHOLD) {
					isSurprise = true;
				}
				distanceTracker.update(distance);
			}

			if (isAttack) {
				if (lastAttackTick > 0 && currentTick > lastAttackTick) {
					double interval = (double) (currentTick - lastAttackTick);
					double zInt = attackIntervalTracker.calculateZScore(interval);
					if (zInt >= SURPRISE_Z_SCORE_THRESHOLD) {
						isSurprise = true;
					}
					attackIntervalTracker.update(interval);
				}
				lastAttackTick = currentTick;
			}
			return isSurprise;
		}
	}

	public static class RoleAuction {
		public enum Role { TANK, FLANKER, CASTER }

		public static class RoleBid {
			public UUID rotUuid;
			public double bidUtility;
			public long expireTick;

			public RoleBid(UUID rotUuid, double utility, long expireTick) {
				this.rotUuid = rotUuid;
				this.bidUtility = utility;
				this.expireTick = expireTick;
			}
		}

		private static final Map<String, RoleBid> BIDS = new HashMap<>();

		public static void pruneStaleBids(long currentTick) {
			BIDS.entrySet().removeIf(entry -> entry.getValue() == null || currentTick > entry.getValue().expireTick + 100);
		}

		public static Role calculateRotRole(Entity rot, LivingEntity target, long currentTick) {
			if (rot == null || target == null || !target.isAlive()) return Role.TANK;

			UUID targetUuid = target.getUUID();
			UUID rotUuid = rot.getUUID();
			double dist = rot.distanceTo(target);

			float health = rot instanceof LivingEntity liv ? liv.getHealth() : 20.0f;
			float maxHealth = rot instanceof LivingEntity liv ? liv.getMaxHealth() : 20.0f;
			double hpRatio = maxHealth > 0 ? health / maxHealth : 1.0;

			CompoundTag nbt = rot.getPersistentData();
			boolean teleportUnlocked = getRotPersistentBoolean(rot, K_UT, false);
			boolean beamUnlocked = getRotPersistentBoolean(rot, K_USB2, false);
			boolean sonicUnlocked = getRotPersistentBoolean(rot, K_USB, false);

			double tankUtil = hpRatio * 40.0 + Math.max(0.0, (24.0 - dist) * 1.5);
			double flankerUtil = (teleportUnlocked ? 25.0 : 10.0) + (dist >= 3.0 && dist <= 12.0 ? 30.0 : 10.0);
			double casterUtil = ((beamUnlocked || sonicUnlocked) ? 35.0 : 5.0) + (dist >= 8.0 ? 30.0 : 10.0) + (1.0 - hpRatio) * 20.0;

			Role bestRole = Role.TANK;
			double maxWonUtil = -1.0;

			Role[] roles = Role.values();
			double[] utils = new double[]{tankUtil, flankerUtil, casterUtil};

			for (int i = 0; i < roles.length; i++) {
				Role role = roles[i];
				double u = utils[i];
				String key = targetUuid.toString() + "_" + role.name();
				RoleBid current = BIDS.get(key);

				if (current == null || currentTick > current.expireTick || rotUuid.equals(current.rotUuid) || u > current.bidUtility + 2.0) {
					BIDS.put(key, new RoleBid(rotUuid, u, currentTick + ROLE_AUCTION_DURATION_TICKS));
					if (u > maxWonUtil) {
						maxWonUtil = u;
						bestRole = role;
					}
				} else if (rotUuid.equals(current.rotUuid)) {
					if (u > maxWonUtil) {
						maxWonUtil = u;
						bestRole = role;
					}
				}
			}
			return bestRole;
		}
	}

	public static class RotHivemindSavedData extends SavedData {
		public static final String DATA_NAME = "rot_hivemind_data";
		public static final Codec<RotHivemindSavedData> CODEC = CompoundTag.CODEC.xmap(
			tag -> load(tag, null),
			data -> data.save(new CompoundTag(), null)
		);
		public static final SavedDataType<RotHivemindSavedData> TYPE = new SavedDataType<>(
			DATA_NAME,
			RotHivemindSavedData::new,
			CODEC,
			null
		);
		public final Map<UUID, CompoundTag> playerMemories = new HashMap<>();

		public RotHivemindSavedData() {}

		public static RotHivemindSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
			RotHivemindSavedData data = new RotHivemindSavedData();
			if (tag != null && tag.contains("PlayerMemories")) {
				CompoundTag mems = tag.getCompound("PlayerMemories").orElse(new CompoundTag());
				for (String key : mems.keySet()) {
					try {
						UUID uuid = UUID.fromString(key);
						data.playerMemories.put(uuid, mems.getCompound(key).orElse(new CompoundTag()));
					} catch (Exception ignored) {}
				}
			}
			return data;
		}

		public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
			CompoundTag mems = new CompoundTag();
			for (Map.Entry<UUID, CompoundTag> entry : playerMemories.entrySet()) {
				mems.put(entry.getKey().toString(), entry.getValue());
			}
			tag.put("PlayerMemories", mems);
			return tag;
		}

		public CompoundTag getMemory(UUID playerUuid) {
			return playerMemories.computeIfAbsent(playerUuid, k -> new CompoundTag());
		}

		public void updateMemory(UUID playerUuid, CompoundTag data) {
			playerMemories.put(playerUuid, data);
			setDirty();
		}

		public static RotHivemindSavedData get(LevelAccessor world) {
			if (world instanceof ServerLevel serverLevel) {
				ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
				if (overworld != null) {
					return overworld.getDataStorage().computeIfAbsent(TYPE);
				}
			}
			return null;
		}
	}

	public static class CombatProfile {
		public String entityTypeId = "";
		public int totalObservedAttacks = 0;
		public long lastAttackTick = 0;
		public double averageMeleeInterval = 40.0;
		public double averageProjectileInterval = 60.0;
		public double minInterval = 999.0;
		public double maxInterval = 0.0;
		public double intervalVariance = 15.0;

		public double averageChargeDuration = 25.0;
		public double averageRecoveryDuration = 15.0;
		public double preferredAttackRange = 3.5;
		public double maxAttackRange = 16.0;
		public double aoeRadiusEstimate = 4.0;
		public double preferredEngagementDistance = 3.5;
		public double preferredMovementSpeed = 0.15;

		public int meleeUsageCount = 0;
		public int projectileUsageCount = 0;
		public int beamUsageCount = 0;
		public int aoeUsageCount = 0;
		public int dashCount = 0;
		public int teleportCount = 0;
		public int verticalAttackCount = 0;
		public int airAttackCount = 0;

		public double averageComboLength = 1.0;
		public int comboCount = 0;
		public String lastAttackType = "";
		public Map<String, Integer> sequenceTransitions = new HashMap<>();

		public int attacksMissed = 0;
		public int attacksBlocked = 0;
		public int hitsDealt = 0;

		public double confidence = 0.0;

		public FightStyle fightStyle = FightStyle.HYBRID;
		public int currentPhase = 1;
		public double lastHealthRatio = 1.0;
		public double phaseAggressionBaseline = 40.0;

		public double recentInterval = 40.0;
		public double longTermInterval = 40.0;
		public List<String> recentAttackHistory = new ArrayList<>();

		public Map<String, Double> signalAlpha = new HashMap<>();
		public Map<String, Double> signalBeta = new HashMap<>();
		public Map<String, Double> defenseAlpha = new HashMap<>();
		public Map<String, Double> defenseBeta = new HashMap<>();
		public Map<String, Double> patternAlpha = new HashMap<>();
		public Map<String, Double> patternBeta = new HashMap<>();

		public TacticalNeuralNetwork neuralNet = new TacticalNeuralNetwork();
		public PersonalityVector personality = new PersonalityVector();
		public boolean hasLoadedHivemindWeights = false;

		public Map<String, Double> patternConfidence = new HashMap<>();

		public Map<String, Double> defenseSuccessRates = new HashMap<>();

		public Map<String, Double> signalWeights = new HashMap<>();
		public Map<String, Integer> signalTruePositives = new HashMap<>();
		public Map<String, Integer> signalFalsePositives = new HashMap<>();

		public Map<String, Integer> defenseSuccesses = new HashMap<>();
		public Map<String, Integer> defenseAttempts = new HashMap<>();

		public enum EnemyTrait {
			LIFESTEAL, PASSIVE_REGEN, BURST_REGEN, PROJECTILE_REFLECTION, SHIELDING,
			DAMAGE_REFLECTION, THORNS, ARMOR_GROWTH, DAMAGE_REDUCTION, MAGIC_RESISTANCE,
			FIRE_RESISTANCE, FREEZE_RESISTANCE, KNOCKBACK_RESISTANCE, FLIGHT, HOVERING,
			TELEPORTATION, GRAVITY_MANIPULATION, LEVITATION, PULL_EFFECTS, PUSH_EFFECTS,
			BLINDNESS, SLOWNESS, BLEEDING, POISON, WITHER, DECAY, SUMMONING, ILLUSIONS,
			CLONING, REVIVAL, SECOND_PHASE, MULTIPLE_PHASES, TRANSFORMATION, SELF_BUFFING,
			ALLY_BUFFING, HEALING_ALLIES, PROJECTILE_SPAM, BEAM_SPECIALIST, AOE_SPECIALIST,
			COUNTER_ATTACKER, COMBO_SPECIALIST, ENVIRONMENTAL_MANIPULATION
		}

		public Map<EnemyTrait, Double> traitConfidence = new HashMap<>();
		public Map<Integer, Set<EnemyTrait>> phaseTraits = new HashMap<>();
		public List<String> successfulCounters = new ArrayList<>();
		public List<String> failedCounters = new ArrayList<>();
		public String currentStrategy = "BALANCED";
		public String experimentalCounter = "NONE";
		public double experimentalCounterEfficiency = 0.0;
		public long experimentalTrialStartTick = 0;
		public float rotHealthAtTrialStart = 0.0f;
		public float targetHealthAtTrialStart = 0.0f;
		public double lastObservedTargetHealth = -1.0;
		public long lastTargetDamageTick = 0;
		public long lastRotDamageTick = 0;

		public void increaseTraitConfidence(EnemyTrait trait, double delta) {
			double current = traitConfidence.getOrDefault(trait, 0.0);
			double updated = Math.min(1.0, current + delta);
			traitConfidence.put(trait, updated);
			if (updated >= 0.50) {
				phaseTraits.computeIfAbsent(currentPhase, k -> new HashSet<>()).add(trait);
			}
			evolveStrategy();
		}

		public void decreaseTraitConfidence(EnemyTrait trait, double delta) {
			double current = traitConfidence.getOrDefault(trait, 0.0);
			traitConfidence.put(trait, Math.max(0.0, current - delta));
			evolveStrategy();
		}

		public Set<EnemyTrait> getKnownTraits() {
			Set<EnemyTrait> known = new HashSet<>();
			for (Map.Entry<EnemyTrait, Double> entry : traitConfidence.entrySet()) {
				if (entry.getValue() >= 0.50) {
					known.add(entry.getKey());
				}
			}
			return known;
		}

		public String getKnownTraitsString() {
			Set<EnemyTrait> known = getKnownTraits();
			if (known.isEmpty()) return "UNKNOWN";
			List<String> list = new ArrayList<>();
			for (EnemyTrait t : known) list.add(t.name());
			return String.join(",", list);
		}

		public String getTraitConfidenceString() {
			if (traitConfidence.isEmpty()) return "NONE";
			List<String> parts = new ArrayList<>();
			for (Map.Entry<EnemyTrait, Double> entry : traitConfidence.entrySet()) {
				if (entry.getValue() > 0.05) {
					parts.add(entry.getKey().name() + ":" + String.format("%.2f", entry.getValue()));
				}
			}
			return parts.isEmpty() ? "NONE" : String.join(",", parts);
		}

		public void evolveStrategy() {
			Set<EnemyTrait> known = getKnownTraits();
			if (known.contains(EnemyTrait.LIFESTEAL)) {
				currentStrategy = "BURST_AND_DISENGAGE";
				setExperimentalCounter("SHORT_MELEE_TRADES");
			} else if (known.contains(EnemyTrait.PROJECTILE_REFLECTION)) {
				currentStrategy = "MELEE_DOMINANCE";
				setExperimentalCounter("SUPPRESS_PROJECTILES");
			} else if (known.contains(EnemyTrait.GRAVITY_MANIPULATION) || known.contains(EnemyTrait.LEVITATION) || known.contains(EnemyTrait.PULL_EFFECTS) || known.contains(EnemyTrait.PUSH_EFFECTS)) {
				currentStrategy = "GROUNDED_STABILITY";
				setExperimentalCounter("TELEPORT_REPOSITION");
			} else if (known.contains(EnemyTrait.FLIGHT) || known.contains(EnemyTrait.HOVERING)) {
				currentStrategy = "ANTI_AIR";
				setExperimentalCounter("GROUND_PULL_BEAM");
			} else if (known.contains(EnemyTrait.THORNS) || known.contains(EnemyTrait.DAMAGE_REFLECTION)) {
				currentStrategy = "HEAVY_BURST_SPACING";
				setExperimentalCounter("RANGE_SUPPRESSION");
			} else if (known.contains(EnemyTrait.SUMMONING) || known.contains(EnemyTrait.ILLUSIONS) || known.contains(EnemyTrait.CLONING)) {
				currentStrategy = "AOE_CLEAR";
				setExperimentalCounter("AOE_FOCUS");
			} else if (known.contains(EnemyTrait.SHIELDING) || known.contains(EnemyTrait.ARMOR_GROWTH) || known.contains(EnemyTrait.DAMAGE_REDUCTION)) {
				currentStrategy = "FLANK_AND_PUNISH";
				setExperimentalCounter("FLANK_GUARD_BREAK");
			} else if (known.contains(EnemyTrait.PASSIVE_REGEN) || known.contains(EnemyTrait.BURST_REGEN)) {
				currentStrategy = "CONTINUOUS_PRESSURE";
				setExperimentalCounter("BURST_BEFORE_REGEN");
			} else if (known.contains(EnemyTrait.BEAM_SPECIALIST) || known.contains(EnemyTrait.PROJECTILE_SPAM)) {
				currentStrategy = "EVASIVE_FLANK";
				setExperimentalCounter("LATERAL_DODGE");
			} else if (known.contains(EnemyTrait.COUNTER_ATTACKER) || known.contains(EnemyTrait.COMBO_SPECIALIST)) {
				currentStrategy = "BAIT_AND_PUNISH";
				setExperimentalCounter("INTERRUPT_TIMING");
			} else {
				currentStrategy = "BALANCED";
				setExperimentalCounter("NONE");
			}
		}

		public void setExperimentalCounter(String counter) {
			if (counter.equals(experimentalCounter)) return;
			if (failedCounters.contains(counter)) return;
			this.experimentalCounter = counter;
		}

		public void updateExperimentalTrial(Entity rot, LivingEntity target, long currentTick) {
			if (rot == null || target == null || experimentalCounter.equals("NONE")) return;
			if (experimentalTrialStartTick == 0) {
				experimentalTrialStartTick = currentTick;
				rotHealthAtTrialStart = rot instanceof LivingEntity liv ? liv.getHealth() : 0.0f;
				targetHealthAtTrialStart = target.getHealth();
				return;
			}

			if (currentTick - experimentalTrialStartTick >= 60) {
				float rotDamageTaken = (rot instanceof LivingEntity liv ? rotHealthAtTrialStart - liv.getHealth() : 0.0f);
				float targetDamageDealt = targetHealthAtTrialStart - target.getHealth();
				double netEfficiency = (double) targetDamageDealt - (double) rotDamageTaken;

				if (netEfficiency > 0.0) {
					if (!successfulCounters.contains(experimentalCounter)) {
						successfulCounters.add(experimentalCounter);
					}
					failedCounters.remove(experimentalCounter);
				} else if (netEfficiency < -2.0) {
					if (!failedCounters.contains(experimentalCounter)) {
						failedCounters.add(experimentalCounter);
					}
					successfulCounters.remove(experimentalCounter);
					experimentalCounter = "NONE";
				}
				experimentalTrialStartTick = currentTick;
				rotHealthAtTrialStart = rot instanceof LivingEntity liv ? liv.getHealth() : 0.0f;
				targetHealthAtTrialStart = target.getHealth();
			}
		}

		public double getMechanicProfileCompletion() {
			int knownCount = getKnownTraits().size();
			double traitProgress = Math.min(1.0, (double) knownCount / 5.0) * 50.0;
			double confidenceProgress = Math.min(1.0, confidence) * 30.0;
			double counterProgress = Math.min(1.0, (double) (successfulCounters.size() + failedCounters.size()) / 3.0) * 20.0;
			return Math.min(100.0, traitProgress + confidenceProgress + counterProgress);
		}

		public void observeMechanics(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, long currentTick) {
			if (target == null || !target.isAlive()) return;

			double currentTargetHealth = target.getHealth();

			if (lastObservedTargetHealth > 0) {
				double healthDiff = currentTargetHealth - lastObservedTargetHealth;
				setRotPersistentDouble(rot, K_STRR, Math.max(0.0, healthDiff));
				if (healthDiff > 0.4 && (currentTick - lastTargetDamageTick) > 20) {
					if (healthDiff > 3.0) {
						increaseTraitConfidence(EnemyTrait.BURST_REGEN, 0.25);
					} else {
						increaseTraitConfidence(EnemyTrait.PASSIVE_REGEN, 0.15);
					}
				}
			}

			if ((currentTick - lastRotDamageTick) <= 10 && currentTargetHealth > lastObservedTargetHealth + 0.3) {
				increaseTraitConfidence(EnemyTrait.LIFESTEAL, 0.30);
			}

			if (target.isBlocking() || target.isUsingItem()) {
				increaseTraitConfidence(EnemyTrait.SHIELDING, 0.15);
			}

			if (!target.onGround()) {
				Vec3 vel = target.getDeltaMovement();
				if (Math.abs(vel.y) < 0.08 && target.position().y > rot.position().y + 2.5) {
					increaseTraitConfidence(EnemyTrait.HOVERING, 0.15);
				} else if (vel.y > 0.05 && target.position().y > rot.position().y + 3.0) {
					increaseTraitConfidence(EnemyTrait.FLIGHT, 0.15);
				}
			}

			if (obs.lastPos != null) {
				double distMoved = target.position().distanceTo(obs.lastPos);
				if (distMoved > 6.0 && obs.lastSpeed < 1.0) {
					setRotPersistentDouble(rot, K_STES, 1.0);
					increaseTraitConfidence(EnemyTrait.TELEPORTATION, 0.35);
				}
			}
			setRotPersistentDouble(rot, K_STES, Math.max(0.0, getRotPersistentDouble(rot, K_STES, 0.0) - 0.02));

			if (rot instanceof LivingEntity rotLiv) {
				if (rotLiv.hasEffect(MobEffects.LEVITATION)) {
					increaseTraitConfidence(EnemyTrait.LEVITATION, 0.35);
					increaseTraitConfidence(EnemyTrait.GRAVITY_MANIPULATION, 0.25);
				}
				if (rotLiv.hasEffect(MobEffects.BLINDNESS)) increaseTraitConfidence(EnemyTrait.BLINDNESS, 0.30);
				if (rotLiv.hasEffect(MobEffects.SLOWNESS)) increaseTraitConfidence(EnemyTrait.SLOWNESS, 0.30);
				if (rotLiv.hasEffect(MobEffects.POISON)) increaseTraitConfidence(EnemyTrait.POISON, 0.30);
				if (rotLiv.hasEffect(MobEffects.WITHER)) increaseTraitConfidence(EnemyTrait.WITHER, 0.30);
			}

			if ((currentTick - lastTargetDamageTick) <= 5) {
				double targetSpeed = target.getDeltaMovement().horizontalDistance();
				if (targetSpeed < 0.03 && rot.distanceTo(target) < 3.0) {
					increaseTraitConfidence(EnemyTrait.KNOCKBACK_RESISTANCE, 0.15);
				}
			}

			if (beamUsageCount >= 3) increaseTraitConfidence(EnemyTrait.BEAM_SPECIALIST, 0.25);
			if (projectileUsageCount >= 5) increaseTraitConfidence(EnemyTrait.PROJECTILE_SPAM, 0.25);
			if (aoeUsageCount >= 4) increaseTraitConfidence(EnemyTrait.AOE_SPECIALIST, 0.25);
			if (attacksBlocked >= 3) increaseTraitConfidence(EnemyTrait.COUNTER_ATTACKER, 0.25);
			if (averageComboLength >= 2.0) increaseTraitConfidence(EnemyTrait.COMBO_SPECIALIST, 0.25);

			lastObservedTargetHealth = currentTargetHealth;
			updateExperimentalTrial(rot, target, currentTick);
		}

		public enum TacticalPlan {
			AGGRESSIVE_PRESSURE, BURST_DAMAGE, HIT_AND_RUN, MAINTAIN_DISTANCE, COUNTER_FOCUS,
			ATTRITION, DEFENSIVE_RECOVERY, INTERRUPT_SPECIALIST, PROJECTILE_SUPPRESSION,
			GROUND_CONTROL, MOBILITY_WARFARE, SURVIVAL, EXPERIMENTAL, HYBRID, PHASE_DISPLACEMENT
		}

		public TacticalPlan currentPlan = TacticalPlan.AGGRESSIVE_PRESSURE;
		public Map<TacticalPlan, Double> planConfidence = new HashMap<>();
		public Map<TacticalPlan, Double> planSuccessRate = new HashMap<>();
		public Map<TacticalPlan, Integer> planUsageCount = new HashMap<>();
		public double momentum = 0.0;
		public String environmentState = "OPEN_SPACE";
		public String planReason = "BALANCED_ENGAGEMENT";
		public boolean isExperimentingTactics = false;
		public TacticalPlan experimentPlan = null;
		public long lastPlannerUpdateTick = 0;
		public long planStartTick = 0;
		public float rotHealthAtPlanStart = 0.0f;
		public float targetHealthAtPlanStart = 0.0f;

		public double[] constructNeuralInputs(Entity rot, LivingEntity target) {
			return constructNeuralInputs(rot, target, 0.0);
		}

		public double[] constructNeuralInputs(Entity rot, LivingEntity target, double threatScore) {
			CombatContext ctx = getCombatContext(rot, target);
			double[] inputs = new double[TacticalNeuralNetwork.INPUT_SIZE];
			if (rot == null || target == null) return inputs;

			float rotHealth = rot instanceof LivingEntity rotLiv ? rotLiv.getHealth() : 20.0f;
			float rotMaxHealth = rot instanceof LivingEntity rotLiv ? rotLiv.getMaxHealth() : 20.0f;
			float rotHealthPct = rotMaxHealth > 0 ? rotHealth / rotMaxHealth : 1.0f;

			inputs[0] = Math.min(1.0, rot.distanceTo(target) / 32.0);
			inputs[1] = rotHealthPct;
			inputs[2] = Math.max(-1.0, Math.min(1.0, momentum / 100.0));
			inputs[3] = Math.min(1.0, threatScore / 100.0);
			inputs[4] = getEnvOrdinal(environmentState) / 4.0;
			inputs[5] = fightStyle != null ? fightStyle.ordinal() / 10.0 : 0.0;
			inputs[6] = Math.max(0.0, Math.min(1.0, getRotPersistentDouble(rot, "sentinel_recent_hit_rate", 0.5)));
			inputs[7] = Math.max(0.0, Math.min(1.0, getRotPersistentDouble(rot, "sentinel_target_dodge_rate", 0.2)));
			inputs[8] = Math.max(0.0, Math.min(1.0, getRotPersistentDouble(rot, "sentinel_signal_confidence", 50.0) / 100.0));
			inputs[9] = (ctx != null && ctx.isJumpCritIncoming) ? 1.0 : 0.0;
			inputs[10] = (ctx != null && ctx.incomingProjectileDistance > 0.0) ? Math.max(0.0, 1.0 - ctx.incomingProjectileDistance / 16.0) : 0.0;
			inputs[11] = (ctx != null) ? Math.min(1.0, ctx.nearbyTargetCount / 5.0) : 0.2;
			inputs[12] = (ctx != null && ctx.targetNearLedgeOrHazard) ? 1.0 : 0.0;
			inputs[13] = (ctx != null && ctx.isEnclosedSpace) ? 1.0 : 0.0;
			inputs[14] = Math.min(1.0, target.getArmorValue() / 20.0);
			inputs[15] = Math.max(0.0, 1.0 - (target.getHealth() / target.getMaxHealth()));
			inputs[16] = getRotPersistentDouble(rot, K_RPC, 0.0) <= 0.0 ? 1.0 : 0.0;
			inputs[17] = Math.min(1.0, getRotPersistentDouble(rot, K_RPM, 0.0));

			double sustainedHits = getRotPersistentDouble(rot, K_SSBH, 0.0);
			inputs[18] = Math.max(0.0, Math.min(1.0, sustainedHits / 8.0));

			double crosshairLock = 0.0;
			Vec3 lookVec = target.getLookAngle().normalize();
			Vec3 dirToRot = rot.position().subtract(target.position()).normalize();
			double dot = lookVec.dot(dirToRot);
			if (dot > 0) crosshairLock = Math.max(0.0, Math.min(1.0, dot));
			inputs[19] = crosshairLock;

			boolean hasLos = rot instanceof LivingEntity rLiv && rLiv.hasLineOfSight(target);
			inputs[20] = hasLos ? 1.0 : 0.0;

			double reloadStall = 0.0;
			double lastRpmTick = getRotPersistentDouble(rot, "sentinel_last_high_rpm_tick", 0.0);
			long currentTickVal = rot.level() instanceof Level lvl ? lvl.getGameTime() : rot.tickCount;
			if (lastRpmTick > 0 && (currentTickVal - lastRpmTick) < 50 && !target.isUsingItem() && !target.swinging) {
				reloadStall = Math.max(0.0, 1.0 - ((currentTickVal - lastRpmTick) / 50.0));
			}
			inputs[21] = reloadStall;

			Vec3 targetVelocity = target.getDeltaMovement();
			Vec3 targetToRot = rot.position().subtract(target.position()).normalize();
			Vec3 horizontalToRot = new Vec3(targetToRot.x, 0.0, targetToRot.z).normalize();
			Vec3 horizontalVelocity = new Vec3(targetVelocity.x, 0.0, targetVelocity.z);
			inputs[22] = Math.max(-1.0, Math.min(1.0, horizontalVelocity.dot(horizontalToRot) / 0.35));
			inputs[23] = Math.max(-1.0, Math.min(1.0, (horizontalVelocity.x * horizontalToRot.z - horizontalVelocity.z * horizontalToRot.x) / 0.35));
			inputs[24] = Math.min(1.0, getRotPersistentDouble(rot, K_ATST, 0.0) / 40.0);
			inputs[25] = Math.min(1.0, getRotPersistentDouble(rot, K_ATSAT, 0.0) / 60.0);
			inputs[26] = target.isUsingItem() ? Math.min(1.0, target.getTicksUsingItem() / 40.0) : 0.0;
			inputs[27] = Math.min(1.0, getRotPersistentDouble(rot, K_SGAC, 0.0) / 100.0);
			inputs[28] = Math.min(1.0, getAvailableAbilities(rot).size() / 8.0);
			inputs[29] = Math.min(1.0, getRotPersistentDouble(rot, K_SHPM, 0.0) / 4.0);
			inputs[30] = Math.max(-1.0, Math.min(1.0, getRotPersistentDouble(rot, K_ADT, 0.0)));
			inputs[31] = Math.max(-1.0, Math.min(1.0, target.getDeltaMovement().y / 0.35));
			inputs[32] = Math.min(1.0, getRotPersistentDouble(rot, K_SRD, 0.0) / 40.0);
			inputs[33] = (ctx != null && ctx.isHealing) ? 1.0 : 0.0;
			inputs[34] = isRotChannelingAbility(rot) ? 1.0 : 0.0;
			double alliedRotCount = 0.0;
			if (rot.level() instanceof Level level) {
				alliedRotCount = level.getEntitiesOfClass(RotEntity.class, rot.getBoundingBox().inflate(16.0), ally -> ally != rot && ally.isAlive() && ally instanceof Mob allyMob && allyMob.getTarget() == target).size();
			}
			inputs[35] = Math.min(1.0, alliedRotCount / 4.0);
			String damageCategory = getRotPersistentString(rot, "sentinel_last_damage_category", "NONE");
			inputs[36] = "PROJECTILE".equals(damageCategory) ? 1.0 : "MAGIC".equals(damageCategory) ? 0.8 : "EXPLOSION".equals(damageCategory) ? 0.7 : "MELEE".equals(damageCategory) ? 0.5 : 0.0;
			inputs[37] = Math.max(-1.0, Math.min(1.0, getRotPersistentDouble(rot, K_SAOS, 0.0)));
			long lastSuccessTick = (long) getRotPersistentDouble(rot, K_SLTDT, 0.0);
			long nowTick = rot.level() instanceof Level level ? level.getGameTime() : rot.tickCount;
			inputs[38] = lastSuccessTick > 0 ? Math.max(0.0, 1.0 - (nowTick - lastSuccessTick) / 200.0) : 0.0;
			inputs[39] = Math.min(1.0, getRotPersistentDouble(rot, K_STRR, 0.0) / 4.0);
			inputs[40] = Math.min(1.0, getRotPersistentDouble(rot, K_STES, 0.0));
			inputs[41] = Math.min(1.0, alliedRotCount / 4.0) * ("TANK".equals(getRotPersistentString(rot, K_SAR, "TANK")) ? 0.5 : 1.0);
			inputs[42] = Math.max(0.0, Math.min(1.0, getRotPersistentDouble(rot, K_SDSR, 0.5)));
			float maxHealthForRisk = rot instanceof LivingEntity rotLiving ? rotLiving.getMaxHealth() : 20.0f;
			inputs[43] = Math.max(0.0, Math.min(1.0, ctx.expectedIncomingDamage / Math.max(1.0f, maxHealthForRisk)));

			inputs[44] = (target.isInWater() || target.isUnderWater()) ? 1.0 : 0.0;

			inputs[45] = target.isPassenger() ? 1.0 : 0.0;

			double targetAttackCadence = getRotPersistentDouble(rot, "ai_target_attack_cadence", 0.0);
			inputs[46] = Math.max(0.0, Math.min(1.0, targetAttackCadence / 20.0));

			inputs[47] = Math.min(1.0, target.getActiveEffects().size() / 5.0);

			boolean learnedSuperheat = getRotPersistentBoolean(rot, K_UWE, false)
					|| getRotPersistentBoolean(rot, K_USB2, false)
					|| getRotPersistentBoolean(rot, K_TFD, false);
			inputs[48] = (learnedSuperheat && getRotPersistentDouble(rot, K_RSC, 0.0) <= 0.0) ? 1.0 : 0.0;

			Vec3 tgtLook = target.getLookAngle().normalize();
			Vec3 tgtToRotVec = rot.position().subtract(target.position()).normalize();
			inputs[49] = Math.max(-1.0, Math.min(1.0, tgtLook.dot(tgtToRotVec)));

			inputs[50] = isTargetHighlyDangerous(target) ? 1.0 : 0.0;

			inputs[51] = Math.min(1.0, getRotPersistentDouble(rot, "adaptation_level", 0.0) / 10.0);

			boolean hasTotem = target.getOffhandItem().is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING) || target.getMainHandItem().is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
			inputs[52] = hasTotem ? 1.0 : 0.0;

			return inputs;
		}

		public void updateTacticalPlan(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, long currentTick, double threatScore) {
			if (rot == null || target == null || !target.isAlive()) return;
			if (currentTick - lastPlannerUpdateTick < 15) return;
			lastPlannerUpdateTick = currentTick;

			environmentState = analyzeEnvironment(world, rot, target);
			CombatContext ctx = getCombatContext(rot, target);

			float rotHealth = rot instanceof LivingEntity rotLiv ? rotLiv.getHealth() : 20.0f;
			float rotMaxHealth = rot instanceof LivingEntity rotLiv ? rotLiv.getMaxHealth() : 20.0f;
			float targetHealth = target.getHealth();

			if (planStartTick > 0 && currentTick - planStartTick >= 60) {
				float rotDmgTaken = rotHealthAtPlanStart - rotHealth;
				float targetDmgDealt = targetHealthAtPlanStart - targetHealth;
				double deltaMomentum = (targetDmgDealt * 3.0) - (rotDmgTaken * 4.0);
				momentum = Math.max(-100.0, Math.min(100.0, momentum + deltaMomentum));

				double planEfficiency = targetDmgDealt - rotDmgTaken;
				double oldRate = planSuccessRate.getOrDefault(currentPlan, 0.50);
				double newRate = Math.max(0.05, Math.min(1.0, oldRate + (planEfficiency > 0 ? 0.08 : -0.08)));
				planSuccessRate.put(currentPlan, newRate);

				double[] nnInputs = constructNeuralInputs(rot, target, threatScore);
				neuralNet.trainDelta(nnInputs, currentPlan.ordinal(), planEfficiency);

				PersonalityVector pVec = PersonalityVector.loadFromNbt(rot.getPersistentData());
				if (planEfficiency > 0.0) {
					pVec.drift(+0.05, -0.02, +0.03, -0.02);
				} else {
					pVec.drift(-0.02, +0.05, -0.03, +0.04);
				}
				pVec.saveToNbt(rot.getPersistentData());

				if (target instanceof Player player) {
					RotHivemindSavedData hivemind = RotHivemindSavedData.get(world);
					if (hivemind != null) {
						CompoundTag mem = hivemind.getMemory(player.getUUID());
						mem.putLongArray("RotNNWeights", neuralNet.toLongArray());
						hivemind.updateMemory(player.getUUID(), mem);
					}
				}

				if (isExperimentingTactics && experimentPlan != null) {
					double expRate = planSuccessRate.getOrDefault(experimentPlan, 0.50);
					planSuccessRate.put(experimentPlan, Math.max(0.05, Math.min(1.0, expRate + (planEfficiency > 0 ? 0.12 : -0.12))));
					isExperimentingTactics = false;
					experimentPlan = null;
				}

				planStartTick = currentTick;
				rotHealthAtPlanStart = rotHealth;
				targetHealthAtPlanStart = targetHealth;
			} else if (planStartTick == 0) {
				planStartTick = currentTick;
				rotHealthAtPlanStart = rotHealth;
				targetHealthAtPlanStart = targetHealth;
			}

			Set<EnemyTrait> known = getKnownTraits();
			Map<TacticalPlan, Double> weights = new HashMap<>();
			for (TacticalPlan p : TacticalPlan.values()) {
				weights.put(p, planSuccessRate.getOrDefault(p, 0.50) * 10.0);
			}

			double[] nnInputs = constructNeuralInputs(rot, target, threatScore);
			double[] hiddenOut = new double[TacticalNeuralNetwork.HIDDEN_SIZE];
			double[] nnScores = neuralNet.forward(nnInputs, hiddenOut);
			for (TacticalPlan p : TacticalPlan.values()) {
				weights.put(p, weights.get(p) + nnScores[p.ordinal()] * CONTEXT_SCORE_WEIGHT_NN);
			}

			PersonalityVector pVec = PersonalityVector.loadFromNbt(rot.getPersistentData());
			weights.put(TacticalPlan.AGGRESSIVE_PRESSURE, weights.get(TacticalPlan.AGGRESSIVE_PRESSURE) + pVec.aggression * 15.0);
			weights.put(TacticalPlan.BURST_DAMAGE, weights.get(TacticalPlan.BURST_DAMAGE) + pVec.aggression * 10.0);
			weights.put(TacticalPlan.MAINTAIN_DISTANCE, weights.get(TacticalPlan.MAINTAIN_DISTANCE) + pVec.patience * 15.0);
			weights.put(TacticalPlan.COUNTER_FOCUS, weights.get(TacticalPlan.COUNTER_FOCUS) + pVec.patience * 10.0 + pVec.spite * 10.0);
			weights.put(TacticalPlan.DEFENSIVE_RECOVERY, weights.get(TacticalPlan.DEFENSIVE_RECOVERY) + pVec.patience * 12.0);
			weights.put(TacticalPlan.INTERRUPT_SPECIALIST, weights.get(TacticalPlan.INTERRUPT_SPECIALIST) + pVec.spite * 15.0);
			weights.put(TacticalPlan.PROJECTILE_SUPPRESSION, weights.get(TacticalPlan.PROJECTILE_SUPPRESSION) + pVec.spite * 10.0);
			weights.put(TacticalPlan.EXPERIMENTAL, weights.get(TacticalPlan.EXPERIMENTAL) + pVec.riskTolerance * 20.0);
			weights.put(TacticalPlan.MOBILITY_WARFARE, weights.get(TacticalPlan.MOBILITY_WARFARE) + pVec.riskTolerance * 12.0);

			String roleStr = rot.getPersistentData().getString(K_SAR).orElse("TANK");
			if ("TANK".equalsIgnoreCase(roleStr)) {
				weights.put(TacticalPlan.AGGRESSIVE_PRESSURE, weights.get(TacticalPlan.AGGRESSIVE_PRESSURE) + 12.0);
				weights.put(TacticalPlan.GROUND_CONTROL, weights.get(TacticalPlan.GROUND_CONTROL) + 10.0);
			} else if ("FLANKER".equalsIgnoreCase(roleStr)) {
				weights.put(TacticalPlan.HIT_AND_RUN, weights.get(TacticalPlan.HIT_AND_RUN) + 12.0);
				weights.put(TacticalPlan.MOBILITY_WARFARE, weights.get(TacticalPlan.MOBILITY_WARFARE) + 10.0);
			} else if ("CASTER".equalsIgnoreCase(roleStr)) {
				weights.put(TacticalPlan.PROJECTILE_SUPPRESSION, weights.get(TacticalPlan.PROJECTILE_SUPPRESSION) + 12.0);
				weights.put(TacticalPlan.MAINTAIN_DISTANCE, weights.get(TacticalPlan.MAINTAIN_DISTANCE) + 10.0);
			}

			if (known.contains(EnemyTrait.LIFESTEAL)) {
				weights.put(TacticalPlan.BURST_DAMAGE, weights.get(TacticalPlan.BURST_DAMAGE) + 15.0);
				weights.put(TacticalPlan.HIT_AND_RUN, weights.get(TacticalPlan.HIT_AND_RUN) + 10.0);
			}
			if (known.contains(EnemyTrait.PROJECTILE_REFLECTION)) {
				weights.put(TacticalPlan.INTERRUPT_SPECIALIST, weights.get(TacticalPlan.INTERRUPT_SPECIALIST) + 12.0);
				weights.put(TacticalPlan.GROUND_CONTROL, weights.get(TacticalPlan.GROUND_CONTROL) + 10.0);
			}
			if (known.contains(EnemyTrait.GRAVITY_MANIPULATION) || known.contains(EnemyTrait.LEVITATION) || known.contains(EnemyTrait.PULL_EFFECTS) || known.contains(EnemyTrait.PUSH_EFFECTS)) {
				weights.put(TacticalPlan.GROUND_CONTROL, weights.get(TacticalPlan.GROUND_CONTROL) + 20.0);
				weights.put(TacticalPlan.MOBILITY_WARFARE, weights.get(TacticalPlan.MOBILITY_WARFARE) + 15.0);
			}
			if (known.contains(EnemyTrait.FLIGHT) || known.contains(EnemyTrait.HOVERING)) {
				weights.put(TacticalPlan.MOBILITY_WARFARE, weights.get(TacticalPlan.MOBILITY_WARFARE) + 15.0);
				weights.put(TacticalPlan.INTERRUPT_SPECIALIST, weights.get(TacticalPlan.INTERRUPT_SPECIALIST) + 10.0);
			}
			if (known.contains(EnemyTrait.BEAM_SPECIALIST) || known.contains(EnemyTrait.PROJECTILE_SPAM)) {
				weights.put(TacticalPlan.PROJECTILE_SUPPRESSION, weights.get(TacticalPlan.PROJECTILE_SUPPRESSION) + 20.0);
				weights.put(TacticalPlan.HIT_AND_RUN, weights.get(TacticalPlan.HIT_AND_RUN) + 10.0);
			}
			if (known.contains(EnemyTrait.SHIELDING) || known.contains(EnemyTrait.COUNTER_ATTACKER)) {
				weights.put(TacticalPlan.COUNTER_FOCUS, weights.get(TacticalPlan.COUNTER_FOCUS) + 15.0);
			}

			float rotHealthPct = rotMaxHealth > 0 ? rotHealth / rotMaxHealth : 1.0f;
			if (rotHealthPct < 0.30) {
				weights.put(TacticalPlan.DEFENSIVE_RECOVERY, weights.get(TacticalPlan.DEFENSIVE_RECOVERY) + 30.0);
				weights.put(TacticalPlan.SURVIVAL, weights.get(TacticalPlan.SURVIVAL) + 25.0);
				weights.put(TacticalPlan.MAINTAIN_DISTANCE, weights.get(TacticalPlan.MAINTAIN_DISTANCE) + 20.0);
				weights.put(TacticalPlan.PHASE_DISPLACEMENT, weights.getOrDefault(TacticalPlan.PHASE_DISPLACEMENT, 0.0) + 30.0);
			}

			double phaseCd = getRotPersistentDouble(rot, K_RPC, 0.0);
			if (phaseCd <= 0.0) {
				weights.put(TacticalPlan.PHASE_DISPLACEMENT, weights.getOrDefault(TacticalPlan.PHASE_DISPLACEMENT, 0.0) + 18.0);
			}
			if (ctx != null && (ctx.isJumpCritIncoming || ctx.incomingProjectileDistance > 0.0)) {
				weights.put(TacticalPlan.PHASE_DISPLACEMENT, weights.getOrDefault(TacticalPlan.PHASE_DISPLACEMENT, 0.0) + 30.0);
			}
			if (ctx != null && ctx.nearbyTargetCount >= 2) {
				weights.put(TacticalPlan.PHASE_DISPLACEMENT, weights.getOrDefault(TacticalPlan.PHASE_DISPLACEMENT, 0.0) + 35.0);
			}

			if (momentum > 25.0) {
				weights.put(TacticalPlan.AGGRESSIVE_PRESSURE, weights.get(TacticalPlan.AGGRESSIVE_PRESSURE) + 15.0);
				weights.put(TacticalPlan.BURST_DAMAGE, weights.get(TacticalPlan.BURST_DAMAGE) + 10.0);
			} else if (momentum < -25.0) {
				weights.put(TacticalPlan.DEFENSIVE_RECOVERY, weights.get(TacticalPlan.DEFENSIVE_RECOVERY) + 15.0);
				weights.put(TacticalPlan.HIT_AND_RUN, weights.get(TacticalPlan.HIT_AND_RUN) + 15.0);
			}

			if (environmentState.equals("CONFINED")) {
				weights.put(TacticalPlan.COUNTER_FOCUS, weights.get(TacticalPlan.COUNTER_FOCUS) + 10.0);
				weights.put(TacticalPlan.BURST_DAMAGE, weights.get(TacticalPlan.BURST_DAMAGE) + 10.0);
			} else if (environmentState.equals("HAZARDOUS_LAVA") || environmentState.equals("WATER_BOUND")) {
				weights.put(TacticalPlan.GROUND_CONTROL, weights.get(TacticalPlan.GROUND_CONTROL) + 15.0);
				weights.put(TacticalPlan.MOBILITY_WARFARE, weights.get(TacticalPlan.MOBILITY_WARFARE) + 10.0);
			}

			if (known.size() >= 3) {
				weights.put(TacticalPlan.HYBRID, weights.get(TacticalPlan.HYBRID) + 25.0);
			}

			if (!ENABLE_PHASE_SHIFT) {
				weights.put(TacticalPlan.PHASE_DISPLACEMENT, 0.0);
			}

			double totalWeight = 0.0;
			TacticalPlan bestPlan = TacticalPlan.AGGRESSIVE_PRESSURE;
			double maxWeight = -1.0;

			for (Map.Entry<TacticalPlan, Double> entry : weights.entrySet()) {
				totalWeight += entry.getValue();
				if (entry.getValue() > maxWeight) {
					maxWeight = entry.getValue();
					bestPlan = entry.getKey();
				}
			}

			for (TacticalPlan p : TacticalPlan.values()) {
				planConfidence.put(p, totalWeight > 0 ? weights.get(p) / totalWeight : 0.10);
			}

			double chosenConfidence = planConfidence.getOrDefault(bestPlan, 0.50);

			if (chosenConfidence < 0.25 || (currentTick % 100 == 0 && Math.random() < 0.20)) {
				isExperimentingTactics = true;
				List<TacticalPlan> validPlans = new ArrayList<>();
				for (TacticalPlan p : TacticalPlan.values()) {
					if (p == TacticalPlan.PHASE_DISPLACEMENT && !ENABLE_PHASE_SHIFT) continue;
					validPlans.add(p);
				}
				experimentPlan = validPlans.get((int) (Math.random() * validPlans.size()));
				currentPlan = experimentPlan;
				planReason = "EXPERIMENT_MODE: Testing " + experimentPlan.name();
			} else {
				currentPlan = bestPlan;
				planReason = "MULTI_TRAIT_SYNERGY (" + known.size() + " traits, Momentum " + String.format("%.1f", momentum) + ")";
			}

			planUsageCount.put(currentPlan, planUsageCount.getOrDefault(currentPlan, 0) + 1);

			if (ENABLE_PHASE_SHIFT && phaseCd <= 0.0 && !getRotPersistentBoolean(rot, K_RPS, false)) {
				if (currentPlan == TacticalPlan.PHASE_DISPLACEMENT || rotHealthPct < 0.35 || (ctx != null && (ctx.isJumpCritIncoming || ctx.incomingProjectileDistance > 0.0 || ctx.nearbyTargetCount >= 2))) {
					double phaseDur = (rotHealthPct < 0.35 || (ctx != null && ctx.nearbyTargetCount >= 2)) ? 600.0 + Math.random() * 600.0 : 300.0 + Math.random() * 300.0;
					setRotPersistentDouble(rot, K_RPT, phaseDur);
					rot.getPersistentData().putBoolean(K_RPS, true);
					if (world instanceof ServerLevel serverLevel) {
						serverLevel.sendParticles(ParticleTypes.PORTAL, rot.getX(), rot.getY() + 1.0, rot.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
						playHostileSound(serverLevel, rot.getX(), rot.getY(), rot.getZ(), "entity.evoker.cast_spell", 1.2F, 0.6F);
					}
				}
			}

			if (currentTick % 200 == 0) {
				for (TacticalPlan p : TacticalPlan.values()) {
					double rate = planSuccessRate.getOrDefault(p, 0.50);
					planSuccessRate.put(p, rate * 0.95 + 0.025);
				}
			}
		}

		public static String analyzeEnvironment(LevelAccessor world, Entity rot, LivingEntity target) {
			if (rot == null) return "OPEN_SPACE";
			BlockPos pos = BlockPos.containing(rot.getX(), rot.getY(), rot.getZ());
			int solidSides = 0;
			if (world.getBlockState(pos.east()).isSolid()) solidSides++;
			if (world.getBlockState(pos.west()).isSolid()) solidSides++;
			if (world.getBlockState(pos.north()).isSolid()) solidSides++;
			if (world.getBlockState(pos.south()).isSolid()) solidSides++;

			if (solidSides >= 2) return "CONFINED";
			if (world.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.LAVA)) return "HAZARDOUS_LAVA";
			if (world.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.WATER)) return "WATER_BOUND";
			if (target != null && target.getY() - rot.getY() > 3.0) return "ELEVATED";

			return "OPEN_SPACE";
		}

		private double getEnvOrdinal(String state) {
			if ("CONFINED".equalsIgnoreCase(state)) return 1.0;
			if ("HAZARDOUS_LAVA".equalsIgnoreCase(state)) return 2.0;
			if ("WATER_BOUND".equalsIgnoreCase(state)) return 3.0;
			if ("ELEVATED".equalsIgnoreCase(state)) return 4.0;
			return 0.0;
		}

		public double getTacticalProfileCompletion() {
			double base = getMechanicProfileCompletion();
			double planUsageProgress = Math.min(1.0, (double) planUsageCount.size() / 5.0) * 20.0;
			return Math.min(100.0, base * 0.8 + planUsageProgress);
		}

		public double getPosteriorMean(double alpha, double beta) {
			double total = alpha + beta;
			if (total <= 0.0) return 0.5;
			return alpha / total;
		}

		public double getPosteriorVariance(double alpha, double beta) {
			double total = alpha + beta;
			if (total <= 0.0) return 0.25;
			return (alpha * beta) / (total * total * (total + 1.0));
		}

		public CombatProfile() {
			signalAlpha.put("sudden_stop", 2.0); signalBeta.put("sudden_stop", 2.0);
			signalAlpha.put("accel_toward", 2.0); signalBeta.put("accel_toward", 2.0);
			signalAlpha.put("facing_lock", 2.0); signalBeta.put("facing_lock", 2.0);
			signalAlpha.put("windup_pattern", 2.0); signalBeta.put("windup_pattern", 2.0);
			signalAlpha.put("interval_due", 2.0); signalBeta.put("interval_due", 2.0);
			signalAlpha.put("projectile_incoming", 2.0); signalBeta.put("projectile_incoming", 2.0);
			signalAlpha.put("adapter_trigger", 2.0); signalBeta.put("adapter_trigger", 2.0);
			signalAlpha.put("rapid_fire", 2.0); signalBeta.put("rapid_fire", 2.0);
			signalAlpha.put("ballistic_tracking", 2.0); signalBeta.put("ballistic_tracking", 2.0);
			signalAlpha.put("reload_window", 2.0); signalBeta.put("reload_window", 2.0);

			defenseAlpha.put("TELEPORT", 4.0); defenseBeta.put("TELEPORT", 1.33);
			defenseAlpha.put("BLOCK", 3.25); defenseBeta.put("BLOCK", 1.75);
			defenseAlpha.put("COUNTER", 3.0); defenseBeta.put("COUNTER", 2.0);
			defenseAlpha.put("STRAFE", 2.5); defenseBeta.put("STRAFE", 2.5);

			signalWeights.put("sudden_stop", 1.0);
			signalWeights.put("accel_toward", 1.0);
			signalWeights.put("facing_lock", 1.0);
			signalWeights.put("windup_pattern", 1.0);
			signalWeights.put("interval_due", 1.0);
			signalWeights.put("projectile_incoming", 1.0);
			signalWeights.put("adapter_trigger", 1.0);
			signalWeights.put("rapid_fire", 1.0);
			signalWeights.put("ballistic_tracking", 1.0);
			signalWeights.put("reload_window", 1.0);

			defenseSuccessRates.put("TELEPORT", 0.75);
			defenseSuccessRates.put("BLOCK", 0.65);
			defenseSuccessRates.put("COUNTER", 0.60);
			defenseSuccessRates.put("STRAFE", 0.50);
		}

		public double getSignalWeight(String signal) {
			double a = signalAlpha.getOrDefault(signal, 2.0);
			double b = signalBeta.getOrDefault(signal, 2.0);
			double mean = getPosteriorMean(a, b);
			double var = getPosteriorVariance(a, b);
			double effective = mean * 2.0;
			if (var > BETA_VARIANCE_GATE_THRESHOLD) {
				double factor = Math.min(1.0, (var - BETA_VARIANCE_GATE_THRESHOLD) / 0.15);
				effective = (1.0 - factor) * effective + factor * 1.0;
			}
			return Math.max(0.10, Math.min(3.0, effective));
		}

		public void rewardSignal(String signal) {
			double a = signalAlpha.getOrDefault(signal, 2.0) + 1.0;
			signalAlpha.put(signal, a);
			signalTruePositives.put(signal, signalTruePositives.getOrDefault(signal, 0) + 1);
			signalWeights.put(signal, getSignalWeight(signal));
		}

		public void penalizeSignal(String signal) {
			double b = signalBeta.getOrDefault(signal, 2.0) + 1.0;
			signalBeta.put(signal, b);
			signalFalsePositives.put(signal, signalFalsePositives.getOrDefault(signal, 0) + 1);
			signalWeights.put(signal, getSignalWeight(signal));
		}

		public void recordDefenseResult(String action, boolean success) {
			if (action == null || action.isEmpty()) return;
			int attempts = defenseAttempts.getOrDefault(action, 0) + 1;
			defenseAttempts.put(action, attempts);
			if (success) {
				defenseAlpha.put(action, defenseAlpha.getOrDefault(action, 2.0) + 1.0);
				defenseSuccesses.put(action, defenseSuccesses.getOrDefault(action, 0) + 1);
			} else {
				defenseBeta.put(action, defenseBeta.getOrDefault(action, 2.0) + 1.0);
			}
			defenseSuccessRates.put(action, getDefenseSuccessRate(action));
		}

		public double getDefenseSuccessRate(String action) {
			double a = defenseAlpha.getOrDefault(action, 2.0);
			double b = defenseBeta.getOrDefault(action, 2.0);
			double mean = getPosteriorMean(a, b);
			double var = getPosteriorVariance(a, b);
			if (var > BETA_VARIANCE_GATE_THRESHOLD) {
				double factor = Math.min(1.0, (var - BETA_VARIANCE_GATE_THRESHOLD) / 0.15);
				mean = (1.0 - factor) * mean + factor * 0.50;
			}
			return Math.max(0.05, Math.min(0.95, mean));
		}

		public String getTopRecommendedDefense() {
			String[] actions = new String[]{"TELEPORT", "BLOCK", "COUNTER", "STRAFE"};
			String best = "TELEPORT";
			double bestRate = -1.0;
			for (String a : actions) {
				double rate = getDefenseSuccessRate(a);
				if (rate > bestRate) {
					bestRate = rate;
					best = a;
				}
			}
			return best;
		}

		public void classifyFightStyle(LivingEntity target) {
			int total = meleeUsageCount + projectileUsageCount + beamUsageCount + aoeUsageCount + dashCount + teleportCount;
			if (total < 3) {
				fightStyle = FightStyle.HYBRID;
				return;
			}
			double beamRatio = (double) beamUsageCount / total;
			double projRatio = (double) (projectileUsageCount + beamUsageCount) / total;
			double aoeRatio = (double) aoeUsageCount / total;
			double mobilityRatio = (double) (dashCount + teleportCount) / total;
			double blockRatio = (double) attacksBlocked / Math.max(1, total);

			if (target != null && target.getMaxHealth() >= 300.0) {
				fightStyle = FightStyle.TANK;
				return;
			}
			if (beamRatio >= 0.30) {
				fightStyle = FightStyle.BEAM_FOCUSED;
			} else if (projRatio >= 0.45) {
				fightStyle = FightStyle.PROJECTILE_FOCUSED;
			} else if (aoeRatio >= 0.35) {
				fightStyle = FightStyle.AOE_FOCUSED;
			} else if (mobilityRatio >= 0.40) {
				fightStyle = FightStyle.HIT_AND_RUN;
			} else if (averageMeleeInterval <= 25.0 && meleeUsageCount >= total * 0.5) {
				fightStyle = FightStyle.AGGRESSIVE;
			} else if (averageMeleeInterval >= 50.0 && blockRatio >= 0.25) {
				fightStyle = FightStyle.DEFENSIVE;
			} else if (averageComboLength >= 2.0) {
				fightStyle = FightStyle.COMBO_FOCUSED;
			} else if (getDefenseSuccessRate("COUNTER") >= 0.70) {
				fightStyle = FightStyle.COUNTER_ATTACKER;
			} else {
				fightStyle = FightStyle.HYBRID;
			}
		}

		public void checkPhaseShift(LivingEntity target, long currentTick) {
			if (target == null) return;
			double currentHealthRatio = (double) target.getHealth() / (double) target.getMaxHealth();
			boolean phaseChanged = false;

			if ((lastHealthRatio >= 0.75 && currentHealthRatio < 0.75) ||
				(lastHealthRatio >= 0.50 && currentHealthRatio < 0.50) ||
				(lastHealthRatio >= 0.25 && currentHealthRatio < 0.25)) {
				phaseChanged = true;
			}
			if (phaseAggressionBaseline > 0 && averageMeleeInterval < phaseAggressionBaseline * 0.65) {
				phaseChanged = true;
			}

			if (phaseChanged) {
				currentPhase++;
				lastHealthRatio = currentHealthRatio;
				phaseAggressionBaseline = averageMeleeInterval;
				recentAttackHistory.clear();
			}
		}

		public void recordAttack(long currentTick, double distance, Vec3 velocity, double windupTicks, String attackType) {
			if (lastAttackTick > 0 && currentTick > lastAttackTick) {
				double interval = (double) (currentTick - lastAttackTick);
				if (interval < minInterval) minInterval = interval;
				if (interval > maxInterval) maxInterval = interval;

				if ("PROJECTILE".equalsIgnoreCase(attackType) || "BEAM".equalsIgnoreCase(attackType)) {
					averageProjectileInterval = 0.7 * averageProjectileInterval + 0.3 * interval;
				} else {
					averageMeleeInterval = 0.7 * averageMeleeInterval + 0.3 * interval;
				}
				recentInterval = 0.7 * recentInterval + 0.3 * interval;
				longTermInterval = 0.9 * longTermInterval + 0.1 * interval;

				double diff = Math.abs(interval - averageMeleeInterval);
				intervalVariance = 0.8 * intervalVariance + 0.2 * diff;
			}

			if (lastAttackType != null && !lastAttackType.isEmpty()) {
				String transition = lastAttackType + "->" + attackType;
				sequenceTransitions.put(transition, sequenceTransitions.getOrDefault(transition, 0) + 1);

				double prevConf = patternConfidence.getOrDefault(transition, 0.40);
				double newConf = Math.min(0.99, prevConf + 0.12);
				patternConfidence.put(transition, newConf);

				for (String key : new ArrayList<>(patternConfidence.keySet())) {
					if (key.startsWith(lastAttackType + "->") && !key.equals(transition)) {
						patternConfidence.put(key, Math.max(0.05, patternConfidence.get(key) - 0.08));
					}
				}
			}
			lastAttackType = attackType;

			recentAttackHistory.add(attackType);
			if (recentAttackHistory.size() > 10) {
				recentAttackHistory.remove(0);
			}

			lastAttackTick = currentTick;
			totalObservedAttacks++;

			if (windupTicks > 0) {
				averageChargeDuration = 0.7 * averageChargeDuration + 0.3 * windupTicks;
			}
			if (distance > 0) {
				preferredAttackRange = 0.8 * preferredAttackRange + 0.2 * distance;
				preferredEngagementDistance = preferredAttackRange;
				maxAttackRange = Math.max(maxAttackRange, distance);
			}

			if ("PROJECTILE".equalsIgnoreCase(attackType)) projectileUsageCount++;
			else if ("BEAM".equalsIgnoreCase(attackType)) beamUsageCount++;
			else if ("VERTICAL".equalsIgnoreCase(attackType)) verticalAttackCount++;
			else if ("AOE".equalsIgnoreCase(attackType)) aoeUsageCount++;
			else if ("DASH".equalsIgnoreCase(attackType)) dashCount++;
			else if ("TELEPORT".equalsIgnoreCase(attackType)) teleportCount++;
			else meleeUsageCount++;

			confidence = Math.min(1.0, totalObservedAttacks / 8.0);
		}

		public String predictFollowUp(String currentAttack) {
			if (currentAttack == null || currentAttack.isEmpty()) return "UNKNOWN";
			String bestNext = "UNKNOWN";
			double maxConf = -1.0;
			for (Map.Entry<String, Double> entry : patternConfidence.entrySet()) {
				if (entry.getKey().startsWith(currentAttack + "->")) {
					if (entry.getValue() > maxConf) {
						maxConf = entry.getValue();
						bestNext = entry.getKey().substring((currentAttack + "->").length());
					}
				}
			}
			if ("UNKNOWN".equals(bestNext)) {
				int maxCount = 0;
				for (Map.Entry<String, Integer> entry : sequenceTransitions.entrySet()) {
					if (entry.getKey().startsWith(currentAttack + "->")) {
						if (entry.getValue() > maxCount) {
							maxCount = entry.getValue();
							bestNext = entry.getKey().substring((currentAttack + "->").length());
						}
					}
				}
			}
			return bestNext;
		}

		public double getPatternConfidence(String currentAttack, String predictedNext) {
			if (currentAttack == null || predictedNext == null) return 0.0;
			return patternConfidence.getOrDefault(currentAttack + "->" + predictedNext, confidence * 0.75);
		}

		public double getRiskScore(String predictedAttack, ThreatLevel level, Entity rot, LivingEntity target) {
			double baseRisk = level.getLevel() * 20.0;
			if ("BEAM".equalsIgnoreCase(predictedAttack) || "AOE".equalsIgnoreCase(predictedAttack)) {
				baseRisk += 25.0;
			}
			if (target != null && target.getMaxHealth() >= 200.0) {
				baseRisk += 15.0;
			}
			if (rot instanceof LivingEntity rotLiv) {
				double healthRatio = (double) rotLiv.getHealth() / (double) rotLiv.getMaxHealth();
				if (healthRatio < 0.40) baseRisk *= 1.4;
			}
			return Math.min(100.0, baseRisk);
		}

		public double getCompletionPercentage() {
			double factor = 0.0;
			if (totalObservedAttacks >= 8) factor += 40.0;
			else factor += (totalObservedAttacks / 8.0) * 40.0;

			if (!sequenceTransitions.isEmpty()) factor += 20.0;
			if (!defenseAttempts.isEmpty()) factor += 20.0;
			if (!signalTruePositives.isEmpty()) factor += 20.0;

			return Math.min(100.0, factor);
		}

		public void saveToNbt(CompoundTag mem) {
			if (mem == null) return;
			mem.putLongArray("RotNNWeights", neuralNet.toLongArray());

			CompoundTag traitTag = new CompoundTag();
			for (Map.Entry<EnemyTrait, Double> entry : traitConfidence.entrySet()) {
				traitTag.putDouble(entry.getKey().name(), entry.getValue());
			}
			mem.put("TraitConfidence", traitTag);

			CompoundTag defAlphaTag = new CompoundTag();
			for (Map.Entry<String, Double> entry : defenseAlpha.entrySet()) defAlphaTag.putDouble(entry.getKey(), entry.getValue());
			mem.put("DefenseAlpha", defAlphaTag);

			CompoundTag defBetaTag = new CompoundTag();
			for (Map.Entry<String, Double> entry : defenseBeta.entrySet()) defBetaTag.putDouble(entry.getKey(), entry.getValue());
			mem.put("DefenseBeta", defBetaTag);

			CompoundTag defRatesTag = new CompoundTag();
			for (Map.Entry<String, Double> entry : defenseSuccessRates.entrySet()) defRatesTag.putDouble(entry.getKey(), entry.getValue());
			mem.put("DefenseSuccessRates", defRatesTag);

			CompoundTag pTag = new CompoundTag();
			personality.saveToNbt(pTag);
			mem.put("PersonalityVector", pTag);

			CompoundTag planTag = new CompoundTag();
			for (Map.Entry<TacticalPlan, Double> entry : planSuccessRate.entrySet()) planTag.putDouble(entry.getKey().name(), entry.getValue());
			mem.put("PlanSuccessRate", planTag);

			mem.putString("SuccessfulCounters", String.join(",", successfulCounters));
			mem.putString("FailedCounters", String.join(",", failedCounters));
		}

		public void loadFromNbt(CompoundTag mem) {
			if (mem == null) return;
			if (mem.contains("RotNNWeights")) {
				long[] savedW = mem.getLongArray("RotNNWeights").orElse(null);
				if (savedW != null && savedW.length == TacticalNeuralNetwork.TOTAL_WEIGHTS) {
					neuralNet = TacticalNeuralNetwork.fromLongArray(savedW);
				}
			}

			if (mem.contains("TraitConfidence")) {
				CompoundTag tag = mem.getCompound("TraitConfidence").orElse(new CompoundTag());
				for (String k : tag.keySet()) {
					try {
						EnemyTrait trait = EnemyTrait.valueOf(k);
						traitConfidence.put(trait, tag.getDouble(k).orElse(0.0));
					} catch (Exception ignored) {}
				}
			}

			if (mem.contains("DefenseAlpha")) {
				CompoundTag tag = mem.getCompound("DefenseAlpha").orElse(new CompoundTag());
				for (String k : tag.keySet()) defenseAlpha.put(k, tag.getDouble(k).orElse(0.0));
			}

			if (mem.contains("DefenseBeta")) {
				CompoundTag tag = mem.getCompound("DefenseBeta").orElse(new CompoundTag());
				for (String k : tag.keySet()) defenseBeta.put(k, tag.getDouble(k).orElse(0.0));
			}

			if (mem.contains("DefenseSuccessRates")) {
				CompoundTag tag = mem.getCompound("DefenseSuccessRates").orElse(new CompoundTag());
				for (String k : tag.keySet()) defenseSuccessRates.put(k, tag.getDouble(k).orElse(0.0));
			}

			if (mem.contains("PersonalityVector")) {
				personality = PersonalityVector.loadFromNbt(mem.getCompound("PersonalityVector").orElse(new CompoundTag()));
			}

			if (mem.contains("PlanSuccessRate")) {
				CompoundTag tag = mem.getCompound("PlanSuccessRate").orElse(new CompoundTag());
				for (String k : tag.keySet()) {
					try {
						TacticalPlan plan = TacticalPlan.valueOf(k);
						planSuccessRate.put(plan, tag.getDouble(k).orElse(0.0));
					} catch (Exception ignored) {}
				}
			}

			if (mem.contains("SuccessfulCounters")) {
				String str = mem.getString("SuccessfulCounters").orElse("");
				successfulCounters.clear();
				if (!str.isEmpty()) {
					for (String s : str.split(",")) if (!s.trim().isEmpty()) successfulCounters.add(s.trim());
				}
			}

			if (mem.contains("FailedCounters")) {
				String str = mem.getString("FailedCounters").orElse("");
				failedCounters.clear();
				if (!str.isEmpty()) {
					for (String s : str.split(",")) if (!s.trim().isEmpty()) failedCounters.add(s.trim());
				}
			}
		}
	}

	public static class PendingPrediction {
		public UUID targetUuid;
		public long predictionTick;
		public long expireTick;
		public String predictedAttackType = "MELEE";
		public ThreatLevel threatLevel = ThreatLevel.NONE;
		public double threatScore = 0.0;
		public List<String> activeSignals = new ArrayList<>();
		public String rotDefensiveAction = "";
		public boolean evaluated = false;
	}

	public static class EntityObservation {
		public Vec3 lastPos = Vec3.ZERO;
		public Vec3 lastDeltaMovement = Vec3.ZERO;
		public double lastSpeed = 0.0;
		public float lastYaw = 0.0f;
		public float lastPitch = 0.0f;
		public Vec3 lastLookVector = Vec3.ZERO;
		public int ticksFacingRot = 0;
		public int ticksStillAfterMoving = 0;
		public int lastItemUseTicks = 0;
		public long lastObservationTick = 0;
	}

	public static abstract class AttackPredictorAdapter {
		public abstract double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile);
	}

	public static class VanillaPredictorAdapter extends AttackPredictorAdapter {
		@Override
		public double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile) {
			double score = 0.0;
			if (target.isUsingItem()) {
				int useTicks = target.getTicksUsingItem();
				score += Math.min(35.0, 10.0 + useTicks * 2.0);
			}
			if (target.swinging) {
				score += 25.0;
			}
			if (target instanceof Player player) {
				if (player.getAttackStrengthScale(0.5f) > 0.85f && rot.distanceTo(target) <= 4.5) {
					score += 20.0;
				}
			}
			return score;
		}
	}

	public static class TACZPredictorAdapter extends AttackPredictorAdapter {
		@Override
		public double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile) {
			double score = 0.0;
			try {
				ItemStack main = target.getMainHandItem();
				ItemStack off = target.getOffhandItem();
				String mainKey = BuiltInRegistries.ITEM.getKey(main.getItem()).toString();
				String offKey = BuiltInRegistries.ITEM.getKey(off.getItem()).toString();

				boolean hasGun = mainKey.contains("tacz") || offKey.contains("tacz") || mainKey.contains("modern_kinetic_gun");
				if (hasGun) {
					score += 20.0;
					Vec3 look = target.getLookAngle().normalize();
					Vec3 toRot = rot.position().subtract(target.position()).normalize();
					double dot = look.dot(toRot);
					if (dot > 0.94) {
						score += 35.0;
					} else if (dot > 0.85) {
						score += 15.0;
					}

					double sustainedHits = getRotPersistentDouble(rot, K_SSBH, 0.0);
					if (sustainedHits > 0) {
						score += Math.min(45.0, sustainedHits * 12.0);
					}

					if (target.isUsingItem()) {
						score += 20.0;
					}
				}
			} catch (Exception ignored) {}
			return score;
		}
	}

	public static class CataclysmPredictorAdapter extends AttackPredictorAdapter {
		@Override
		public double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile) {
			double score = 0.0;
			try {
				String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
				if (typeKey.contains("cataclysm")) {
					score += 25.0;
					if (target.swinging) score += 30.0;
					if (target.fallDistance > 1.5) score += 25.0;
					if (target.distanceTo(rot) <= 6.0) score += 20.0;
				}

				ItemStack main = target.getMainHandItem();
				String mainKey = BuiltInRegistries.ITEM.getKey(main.getItem()).toString();
				if (mainKey.contains("cataclysm")) {
					score += 20.0;
					if (mainKey.contains("infernal_forge") || mainKey.contains("incinerator") || mainKey.contains("tidal_claws")) {
						if (target.swinging || target.isUsingItem()) score += 35.0;
					} else if (mainKey.contains("laser_gatling") || mainKey.contains("wither_assault")) {
						if (target.isUsingItem()) score += 40.0;
					}
				}
			} catch (Exception ignored) {}
			return score;
		}
	}

	public static boolean isWroughtnautStuck(LivingEntity target) {
		if (target == null) return false;
		String className = target.getClass().getName();
		if (className.contains("Wroughtnaut") || className.contains("EntityWroughtnaut")) {
			try {
				Object currentAnim = target.getClass().getMethod("getAnimation").invoke(target);
				if (currentAnim != null) {
					int currentTick = (Integer) target.getClass().getMethod("getAnimationTick").invoke(target);
					for (java.lang.reflect.Field field : target.getClass().getDeclaredFields()) {
						if (field.getType().getSimpleName().equals("Animation")) {
							field.setAccessible(true);
							Object animVal = field.get(null);
							if (animVal == currentAnim) {
								String fieldName = field.getName().toUpperCase();
								if (fieldName.contains("VERTICAL") || fieldName.contains("SLAM") || fieldName.contains("STUCK")) {
									if (currentTick >= 20) {
										return true;
									}
								}
							}
						}
					}
				}
			} catch (Exception ignored) {
				if (target.getDeltaMovement().horizontalDistanceSqr() < 0.001 && target.swingTime == 0 && target.fallDistance == 0.0) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isWroughtnautAttacking(LivingEntity target) {
		if (target == null) return false;
		String className = target.getClass().getName();
		if (className.contains("Wroughtnaut") || className.contains("EntityWroughtnaut")) {
			try {
				Object currentAnim = target.getClass().getMethod("getAnimation").invoke(target);
				if (currentAnim != null) {
					for (java.lang.reflect.Field field : target.getClass().getDeclaredFields()) {
						if (field.getType().getSimpleName().equals("Animation")) {
							field.setAccessible(true);
							Object animVal = field.get(null);
							if (animVal == currentAnim) {
								String fieldName = field.getName().toUpperCase();
								if (fieldName.contains("ATTACK") || fieldName.contains("SLAM") || fieldName.contains("STOMP") || fieldName.contains("SWING")) {
									return true;
								}
							}
						}
					}
				}
			} catch (Exception ignored) {}
			if (target.swingTime > 0) {
				return true;
			}
		}
		return false;
	}

	public static class MowziesPredictorAdapter extends AttackPredictorAdapter {
		@Override
		public double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile) {
			double score = 0.0;
			try {
				String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
				boolean isWrought = typeKey.contains("wroughtnaut") || typeKey.contains("ferrous_wroughtnaut");

				if (typeKey.contains("mowziesmobs")) {
					score += 25.0;
					if (target.swinging) score += 30.0;
					if (target.fallDistance > 1.2) score += 35.0;
					if (target.distanceTo(rot) <= 5.0) score += 15.0;

					if (isWrought) {
						if (!isWroughtnautStuck(target)) {
							score += 55.0;
						} else {
							score -= 45.0;
						}
					}
				}

				ItemStack main = target.getMainHandItem();
				String itemKey = BuiltInRegistries.ITEM.getKey(main.getItem()).toString();
				if (itemKey.contains("mowziesmobs")) {
					score += 15.0;
					if (itemKey.contains("wrought_axe")) {
						if (target.fallDistance > 1.0 || target.getDeltaMovement().y < -0.2) {
							score += 45.0;
						} else if (target.swinging) {
							score += 25.0;
						}
					} else if (itemKey.contains("ice_crystal") || itemKey.contains("blowgun") || itemKey.contains("sol_visage")) {
						if (target.isUsingItem()) {
							score += 35.0;
						}
					} else if (itemKey.contains("spear") && target.swinging) {
						score += 25.0;
					}
				}
			} catch (Exception ignored) {}
			return score;
		}
	}

	public static class AlexsCavesPredictorAdapter extends AttackPredictorAdapter {
		@Override
		public double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile) {
			double score = 0.0;
			try {
				String typeKey = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
				if (typeKey.contains("alexscaves")) {
					score += 25.0;
					if (target.swinging || target.isUsingItem()) score += 30.0;
				}

				ItemStack main = target.getMainHandItem();
				String itemKey = BuiltInRegistries.ITEM.getKey(main.getItem()).toString();
				if (itemKey.contains("alexscaves")) {
					score += 15.0;
					if (itemKey.contains("raygun") || itemKey.contains("tremor_zapper") || itemKey.contains("dreadbow") || itemKey.contains("darkness_incinerator")) {
						if (target.isUsingItem()) {
							score += 40.0;
						}
					} else if (itemKey.contains("extinction_spear") || itemKey.contains("galena_gauntlet")) {
						if (target.swinging || target.isUsingItem()) {
							score += 30.0;
						}
					}
				}
			} catch (Exception ignored) {}
			return score;
		}
	}

	public static class EpicFightPredictorAdapter extends AttackPredictorAdapter {
		@Override
		public double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile) {
			double score = 0.0;
			try {
				ItemStack main = target.getMainHandItem();
				String itemKey = BuiltInRegistries.ITEM.getKey(main.getItem()).toString();
				if (itemKey.contains("epicfight") || target.swinging) {
					score += 20.0;
					if (target.swinging && rot.distanceTo(target) <= 4.5) {
						score += 30.0;
					}
				}
			} catch (Exception ignored) {}
			return score;
		}
	}

	public static class IronSpellsPredictorAdapter extends AttackPredictorAdapter {
		@Override
		public double evaluateThreat(LevelAccessor world, Entity rot, LivingEntity target, EntityObservation obs, CombatProfile profile) {
			double score = 0.0;
			try {
				ItemStack main = target.getMainHandItem();
				ItemStack off = target.getOffhandItem();
				String mainKey = BuiltInRegistries.ITEM.getKey(main.getItem()).toString();
				String offKey = BuiltInRegistries.ITEM.getKey(off.getItem()).toString();

				boolean isHoldingSpellItem = mainKey.contains("irons_spellbooks") || offKey.contains("irons_spellbooks")
					|| mainKey.contains("spell_book") || mainKey.contains("scroll") || offKey.contains("spell_book");

				if (isHoldingSpellItem) {
					score += 20.0;
					if (target.isUsingItem()) {
						int castTicks = target.getTicksUsingItem();
						score += Math.min(45.0, 20.0 + castTicks * 2.5);
					}
				}

				for (net.minecraft.world.effect.MobEffectInstance effect : target.getActiveEffects()) {
					String effectKey = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString();
					if (effectKey.contains("irons_spellbooks")) {
						score += 15.0;
						break;
					}
				}
			} catch (Exception ignored) {}
			return score;
		}
	}

	public static class UniversalCombatPredictionEngine {
		private static final Map<UUID, EntityObservation> OBSERVATIONS = new HashMap<>();
		private static final Map<String, CombatProfile> PROFILES = new HashMap<>();
		private static final Map<UUID, PendingPrediction> PENDING_PREDICTIONS = new HashMap<>();
		private static final List<AttackPredictorAdapter> ADAPTERS = List.of(
			new VanillaPredictorAdapter(),
			new TACZPredictorAdapter(),
			new CataclysmPredictorAdapter(),
			new MowziesPredictorAdapter(),
			new AlexsCavesPredictorAdapter(),
			new EpicFightPredictorAdapter(),
			new IronSpellsPredictorAdapter()
		);

		public static void tickPrediction(LevelAccessor world, Entity rot, LivingEntity target) {
			if (rot == null || target == null || !target.isAlive()) return;

			long currentTick = world instanceof Level lvl ? lvl.getGameTime() : rot.tickCount;
			if (currentTick % 6000 == 0) {
				pruneStaleProfiles(currentTick);
				RoleAuction.pruneStaleBids(currentTick);
			}

			UUID targetUuid = target.getUUID();
			EntityObservation obs = OBSERVATIONS.computeIfAbsent(targetUuid, k -> new EntityObservation());

			String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
			String profileKey = (target instanceof Player player) ? ("player:" + player.getUUID()) : typeId;
			CombatProfile profile = PROFILES.computeIfAbsent(profileKey, k -> {
				CombatProfile p = new CombatProfile();
				p.entityTypeId = typeId;
				return p;
			});

			RoleAuction.Role wonRole = RoleAuction.calculateRotRole(rot, target, currentTick);
			setRotPersistentString(rot, K_SAR, wonRole.name());

			if (target instanceof Player player) {
				boolean isAttacking = target.swinging || target.isUsingItem();
				boolean isSurprise = PlayerBehaviorTracker.get(player.getUUID()).observe(currentTick, rot.distanceTo(target), isAttacking);
				if (isSurprise) {
					setRotPersistentDouble(rot, "sentinel_surprise_alert_ticks", 60.0);
				}
			}

			if (target instanceof Player player && !profile.hasLoadedHivemindWeights) {
				profile.hasLoadedHivemindWeights = true;
				RotHivemindSavedData hivemind = RotHivemindSavedData.get(world);
				if (hivemind != null) {
					CompoundTag mem = hivemind.getMemory(player.getUUID());
					profile.loadFromNbt(mem);
				}
			}

			profile.classifyFightStyle(target);
			profile.checkPhaseShift(target, currentTick);

			PendingPrediction pending = PENDING_PREDICTIONS.get(targetUuid);
			if (pending != null && !pending.evaluated && currentTick > pending.expireTick) {
				if (pending.threatLevel.isHighOrImminent() || pending.threatScore >= 45.0) {
					for (String signal : pending.activeSignals) {
						profile.penalizeSignal(signal);
					}
					if (!pending.rotDefensiveAction.isEmpty()) {
						profile.recordDefenseResult(pending.rotDefensiveAction, false);
					}
				}
				pending.evaluated = true;
			}

			double score = 0.0;
			List<String> activeSignals = new ArrayList<>();
			double dist = rot.distanceTo(target);
			Vec3 pos = target.position();
			Vec3 rotPos = rot.position();
			Vec3 dirToRot = rotPos.subtract(pos).normalize();

			Vec3 currentMovement = target.getDeltaMovement();
			double currentSpeed = currentMovement.horizontalDistance();
			boolean suddenStop = (obs.lastSpeed > 0.12 && currentSpeed < 0.035);
			if (suddenStop) {
				score += 10.0 * profile.getSignalWeight("sudden_stop");
				activeSignals.add("sudden_stop");
			}

			Vec3 acceleration = currentMovement.subtract(obs.lastDeltaMovement);
			double accelTowardRot = acceleration.dot(dirToRot);
			if (accelTowardRot > 0.08) {
				score += 20.0 * profile.getSignalWeight("accel_toward");
				activeSignals.add("accel_toward");
			}

			Vec3 lookVec = target.getLookAngle().normalize();
			double dot = lookVec.dot(dirToRot);
			double angleDeg = Math.toDegrees(Math.acos(Mth.clamp(dot, -1.0, 1.0)));

			if (angleDeg < 15.0) {
				score += 8.0 * profile.getSignalWeight("facing_lock");
				activeSignals.add("facing_lock");
				obs.ticksFacingRot++;
				if (obs.ticksFacingRot >= 3) {
					score += 12.0 * profile.getSignalWeight("facing_lock");
				}
			} else {
				obs.ticksFacingRot = 0;
			}

			boolean isWindupPattern = suddenStop && angleDeg < 20.0 && dist <= profile.preferredAttackRange + 2.0;
			if (isWindupPattern) {
				score += 30.0 * profile.getSignalWeight("windup_pattern");
				activeSignals.add("windup_pattern");
			}

			if (dist <= 3.2 && (obs.lastSpeed > 0.15 || target.isSprinting())) {
				score += 25.0 * profile.getSignalWeight("accel_toward");
				if (!activeSignals.contains("accel_toward")) activeSignals.add("accel_toward");
			}

			if (profile.confidence > 0.3 && profile.lastAttackTick > 0) {
				double ticksSince = (double) (currentTick - profile.lastAttackTick);
				if (Math.abs(ticksSince - profile.averageMeleeInterval) <= 6.0 || Math.abs(ticksSince - profile.averageProjectileInterval) <= 6.0) {
					score += 50.0 * profile.getSignalWeight("interval_due");
					activeSignals.add("interval_due");
				}
			}

			boolean projectileIncoming = checkIncomingProjectiles(world, rot, target);
			if (projectileIncoming) {
				score += 45.0 * profile.getSignalWeight("projectile_incoming");
				activeSignals.add("projectile_incoming");
			}

			if (world instanceof Level lvl) {
				List<LivingEntity> nearbyHostiles = lvl.getEntitiesOfClass(LivingEntity.class, rot.getBoundingBox().inflate(16.0), e -> e != rot && e != target && e.isAlive() && (e instanceof Player || (e instanceof Mob m && m.getTarget() == rot)));
				if (!nearbyHostiles.isEmpty()) {
					score += Math.min(25.0, nearbyHostiles.size() * 10.0);
					activeSignals.add("multiple_threats");
				}
			}

			for (AttackPredictorAdapter adapter : ADAPTERS) {
				double adapterScore = adapter.evaluateThreat(world, rot, target, obs, profile);
				if (adapterScore > 0) {
					score += adapterScore * profile.getSignalWeight("adapter_trigger");
					activeSignals.add("adapter_trigger");
				}
			}

			double prevScore = getRotPersistentDouble(rot, K_SPTS, 0.0);
			double finalScore = 0.7 * prevScore + 0.3 * score;

			ThreatLevel level = ThreatLevel.NONE;
			if (finalScore >= 90.0) level = ThreatLevel.ATTACK_IMMINENT;
			else if (finalScore >= 60.0) level = ThreatLevel.HIGH;
			else if (finalScore >= 35.0) level = ThreatLevel.MEDIUM;
			else if (finalScore >= 15.0) level = ThreatLevel.LOW;

			String followup = profile.predictFollowUp(profile.lastAttackType);
			double patternConf = profile.getPatternConfidence(profile.lastAttackType, followup);

			PendingPrediction newPending = new PendingPrediction();
			newPending.targetUuid = targetUuid;
			newPending.predictionTick = currentTick;
			newPending.expireTick = currentTick + 25;
			newPending.threatLevel = level;
			newPending.threatScore = finalScore;
			newPending.activeSignals = activeSignals;
			newPending.predictedAttackType = followup;
			PENDING_PREDICTIONS.put(targetUuid, newPending);

			profile.observeMechanics(world, rot, target, obs, currentTick);

			profile.updateTacticalPlan(world, rot, target, obs, currentTick, finalScore);

			setRotPersistentString(rot, K_SPTL, level.name());
			setRotPersistentDouble(rot, K_SPTS, finalScore);
			setRotPersistentBoolean(rot, K_SPAI, level == ThreatLevel.ATTACK_IMMINENT);
			setRotPersistentBoolean(rot, K_SWD, isWindupPattern);
			setRotPersistentDouble(rot, "sentinel_prediction_confidence", profile.confidence);
			setRotPersistentString(rot, "sentinel_predicted_followup", followup);
			setRotPersistentDouble(rot, "sentinel_pattern_confidence", patternConf);
			setRotPersistentString(rot, "sentinel_fight_style", profile.fightStyle.name());
			setRotPersistentDouble(rot, "sentinel_combat_phase", profile.currentPhase);
			setRotPersistentString(rot, "sentinel_top_recommended_defense", profile.getTopRecommendedDefense());
			setRotPersistentDouble(rot, "sentinel_profile_completion", profile.getCompletionPercentage());

			setRotPersistentString(rot, "sentinel_strategy", profile.currentStrategy);
			setRotPersistentString(rot, "sentinel_known_traits", profile.getKnownTraitsString());
			setRotPersistentString(rot, "sentinel_trait_confidence", profile.getTraitConfidenceString());
			setRotPersistentString(rot, "sentinel_successful_counters", String.join(",", profile.successfulCounters));
			setRotPersistentString(rot, "sentinel_failed_counters", String.join(",", profile.failedCounters));
			setRotPersistentString(rot, "sentinel_experimental_counter", profile.experimentalCounter);
			setRotPersistentDouble(rot, "sentinel_mechanic_profile_completion", profile.getMechanicProfileCompletion());

			setRotPersistentString(rot, K_STP, profile.currentPlan.name());
			setRotPersistentDouble(rot, "sentinel_plan_confidence", profile.planConfidence.getOrDefault(profile.currentPlan, 0.50));
			setRotPersistentDouble(rot, "sentinel_plan_success_rate", profile.planSuccessRate.getOrDefault(profile.currentPlan, 0.50));
			setRotPersistentDouble(rot, "sentinel_momentum", profile.momentum);
			setRotPersistentString(rot, "sentinel_environment_state", profile.environmentState);
			setRotPersistentString(rot, "sentinel_plan_reason", profile.planReason);
			setRotPersistentDouble(rot, "sentinel_tactical_completion", profile.getTacticalProfileCompletion());

			if (getRotPersistentBoolean(rot, "sentinel_debug_mode", false) && currentTick % 40 == 0) {
				if (!world.isClientSide() && rot instanceof LivingEntity) {
					System.out.printf("[Sentinel Engine P5] Target: %s | Plan: %s (%.0f%%, Succ: %.0f%%) | Reason: %s | Momentum: %.1f | Env: %s | Exp: %b | Progress: %.0f%%%n",
						typeId, profile.currentPlan.name(), profile.planConfidence.getOrDefault(profile.currentPlan, 0.50) * 100.0,
						profile.planSuccessRate.getOrDefault(profile.currentPlan, 0.50) * 100.0,
						profile.planReason, profile.momentum, profile.environmentState, profile.isExperimentingTactics,
						profile.getTacticalProfileCompletion());
				}
			}

			CombatContext ctx = getCombatContext(rot, target);
			boolean jumpCritDanger = ctx != null && ctx.isJumpCritIncoming;
			boolean projDanger = ctx != null && ctx.incomingProjectileDistance > 0.0 && ctx.incomingProjectileDistance <= 12.0;
			double maxRiskHealth = rot instanceof LivingEntity rotLiving ? rotLiving.getMaxHealth() : 20.0;
			boolean isUrgentDanger = isWindupPattern || projectileIncoming || jumpCritDanger || projDanger || ctx.expectedIncomingDamage >= maxRiskHealth * 0.50;

			if (level.isHighOrImminent() || isUrgentDanger) {
				double riskScore = profile.getRiskScore(followup, level, rot, target);
				if (profile.confidence >= 0.25 || level == ThreatLevel.ATTACK_IMMINENT || riskScore >= 50.0 || isUrgentDanger) {
					triggerReactiveDefenses(world, rot, target, dist, level, isUrgentDanger, profile, newPending);
				}
			}

			obs.lastPos = pos;
			obs.lastDeltaMovement = currentMovement;
			obs.lastSpeed = currentSpeed;
			obs.lastYaw = target.getYRot();
			obs.lastPitch = target.getXRot();
			obs.lastLookVector = lookVec;
			obs.lastObservationTick = currentTick;

			if (target instanceof Player player && currentTick % 20 == 0) {
				RotHivemindSavedData hivemind = RotHivemindSavedData.get(world);
				if (hivemind != null) {
					CompoundTag mem = hivemind.getMemory(player.getUUID());
					profile.saveToNbt(mem);
					hivemind.updateMemory(player.getUUID(), mem);
				}
			}
		}

		private static boolean checkIncomingProjectiles(LevelAccessor world, Entity rot, LivingEntity target) {
			if (!(world instanceof Level level)) return false;
			AABB searchBox = rot.getBoundingBox().inflate(24.0);
			List<net.minecraft.world.entity.projectile.Projectile> projectiles = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, searchBox, p -> p.getOwner() == target || (p.getOwner() != rot && p.getDeltaMovement().lengthSqr() > 0.1));

			Vec3 rotPos = rot.position().add(0, rot.getBbHeight() * 0.5, 0);
			for (net.minecraft.world.entity.projectile.Projectile p : projectiles) {
				Vec3 pPos = p.position();
				Vec3 pVel = p.getDeltaMovement();
				if (pVel.lengthSqr() < 0.01) continue;

				Vec3 pToRot = rotPos.subtract(pPos);
				if (pVel.dot(pToRot) <= 0) continue;

				double t = pToRot.dot(pVel) / pVel.lengthSqr();
				if (t > 0 && t <= 15.0) {
					Vec3 closestPoint = pPos.add(pVel.scale(t));
					if (closestPoint.distanceTo(rotPos) < 2.5) {
						return true;
					}
				}
			}
			return false;
		}

		private static void triggerReactiveDefenses(LevelAccessor world, Entity rot, LivingEntity target, double dist, ThreatLevel level, boolean immediateDanger, CombatProfile profile, PendingPrediction pending) {
			String recommendedDefense = profile.getTopRecommendedDefense();

			double phaseCd = getRotPersistentDouble(rot, K_RPC, 0.0);
			if (ENABLE_PHASE_SHIFT && phaseCd <= 0.0 && !getRotPersistentBoolean(rot, K_RPS, false)) {
				CombatContext reactiveCtx = getCombatContext(rot, target);
				float rotHealth = rot instanceof LivingEntity rotLiv ? rotLiv.getHealth() : 20.0f;
				float rotMaxHealth = rot instanceof LivingEntity rotLiv ? rotLiv.getMaxHealth() : 20.0f;
				float rotHpPct = rotMaxHealth > 0 ? rotHealth / rotMaxHealth : 1.0f;
				if (immediateDanger || rotHpPct < 0.35 || (reactiveCtx != null && (reactiveCtx.isJumpCritIncoming || reactiveCtx.incomingProjectileDistance > 0.0 || reactiveCtx.nearbyTargetCount >= 2))) {
					double phaseDur = (rotHpPct < 0.35 || (reactiveCtx != null && reactiveCtx.nearbyTargetCount >= 2)) ? 600.0 + Math.random() * 600.0 : 300.0 + Math.random() * 300.0;
					setRotPersistentDouble(rot, K_RPT, phaseDur);
					rot.getPersistentData().putBoolean(K_RPS, true);
					if (world instanceof ServerLevel serverLevel) {
						serverLevel.sendParticles(ParticleTypes.PORTAL, rot.getX(), rot.getY() + 1.0, rot.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
						playHostileSound(serverLevel, rot.getX(), rot.getY(), rot.getZ(), "entity.evoker.cast_spell", 1.2F, 0.6F);
					}
				}
			}

			if (ENABLE_BLOCKING && !isRotChannelingAbility(rot) && dist <= 6.0 && !getRotPersistentBoolean(rot, K_IB, false) && getRotPersistentDouble(rot, K_RBC, 0.0) <= 0) {
				if ("BLOCK".equals(recommendedDefense) || immediateDanger || level == ThreatLevel.ATTACK_IMMINENT) {
					if (rot.getRandom().nextDouble() < 0.85) {
						setRotPersistentDouble(rot, K_RBAT, BLOCK_MIN_TICKS + rot.getRandom().nextInt((int)(BLOCK_MAX_TICKS - BLOCK_MIN_TICKS + 1)));
						setRotPersistentBoolean(rot, K_IB, true);
						pending.rotDefensiveAction = "BLOCK";
						return;
					}
				}
			}

			if (immediateDanger && dist <= 8.0 && getRotPersistentDouble(rot, K_TP_DODGE_CD, 0.0) <= 0 && getRotPersistentBoolean(rot, K_UT, false)) {
				if ("TELEPORT".equals(recommendedDefense) || rot.getRandom().nextDouble() < 0.85) {
					tryPredictiveDodge(world, rot, target, dist);
					pending.rotDefensiveAction = "TELEPORT";
					return;
				}
			}

			if (level == ThreatLevel.ATTACK_IMMINENT && dist <= 2.8 && getRotPersistentDouble(rot, K_SMC, 0.0) <= 0 && getRotPersistentDouble(rot, K_SMW, 0.0) <= 0) {
				if ("COUNTER".equals(recommendedDefense) || rot.getRandom().nextDouble() < 0.50) {
					setRotPersistentDouble(rot, K_SMW, 4);
					pending.rotDefensiveAction = "COUNTER";
				}
			}
		}

		public static void recordActualAttack(Entity rot, Entity target, String attackType) {
			if (target instanceof LivingEntity targetLiv) {
				UUID targetUuid = targetLiv.getUUID();
				String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(targetLiv.getType()).toString();
				String profileKey = (targetLiv instanceof Player player) ? ("player:" + player.getUUID()) : typeId;
				CombatProfile profile = PROFILES.get(profileKey);
				if (profile != null) {
					long currentTick = rot.level().getGameTime();
					profile.lastTargetDamageTick = currentTick;
					rot.getPersistentData().putDouble(K_SLTDT, currentTick);
					double outcome = getRotPersistentDouble(rot, K_SAOS, 0.0);
					rot.getPersistentData().putDouble(K_SAOS, Math.min(1.0, outcome + 0.25));
					profile.recordAttack(currentTick, rot.distanceTo(target), target.getDeltaMovement(), 0, attackType);

					PendingPrediction pending = PENDING_PREDICTIONS.get(targetUuid);
					if (pending != null && !pending.evaluated) {
						if (pending.threatLevel.isHighOrImminent() || pending.threatScore >= 40.0) {
							for (String signal : pending.activeSignals) {
								profile.rewardSignal(signal);
							}
							if (!pending.rotDefensiveAction.isEmpty()) {
								profile.recordDefenseResult(pending.rotDefensiveAction, true);
								rot.getPersistentData().putDouble(K_SDSR, Math.min(1.0, getRotPersistentDouble(rot, K_SDSR, 0.5) + 0.1));
							}
						}
						pending.evaluated = true;
					}
				}
			}
		}

		public static void recordRotDamage(Entity rot, Entity attacker) {
			if (attacker instanceof LivingEntity targetLiv) {
				UUID targetUuid = targetLiv.getUUID();
				String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(targetLiv.getType()).toString();
				String profileKey = (targetLiv instanceof Player player) ? ("player:" + player.getUUID()) : typeId;
				CombatProfile profile = PROFILES.get(profileKey);
				if (profile != null) {
					profile.lastRotDamageTick = rot.level().getGameTime();
					profile.momentum = Math.max(-100.0, profile.momentum - 15.0);
					if (profile.currentPlan != null) {
						double[] inputs = profile.constructNeuralInputs(rot, targetLiv);
						profile.neuralNet.trainDelta(inputs, profile.currentPlan.ordinal(), -5.0);
					}
					PendingPrediction pending = PENDING_PREDICTIONS.get(targetUuid);
					if (pending != null) {
						if (!pending.evaluated && pending.threatScore < 40.0) {
							for (String signal : pending.activeSignals) {
								profile.rewardSignal(signal);
							}
						}
						if (!pending.rotDefensiveAction.isEmpty()) {
							profile.recordDefenseResult(pending.rotDefensiveAction, false);
							rot.getPersistentData().putDouble(K_SDSR, Math.max(0.0, getRotPersistentDouble(rot, K_SDSR, 0.5) - 0.1));
						}
					}
				}
			}
		}

		public static void recordRotKill(Entity rot, Entity victim) {
			if (rot == null) return;
			if (victim instanceof LivingEntity targetLiv) {
				UUID targetUuid = targetLiv.getUUID();
				String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(targetLiv.getType()).toString();
				String profileKey = (targetLiv instanceof Player player) ? ("player:" + player.getUUID()) : typeId;
				CombatProfile profile = PROFILES.get(profileKey);
				if (profile != null) {
					profile.momentum = Math.min(100.0, profile.momentum + 25.0);
					if (profile.currentPlan != null) {
						double[] inputs = profile.constructNeuralInputs(rot, targetLiv);
						profile.neuralNet.trainDelta(inputs, profile.currentPlan.ordinal(), 10.0);
					}
				}
				PENDING_PREDICTIONS.remove(targetUuid);
			}
		}

		public static void recordRotLandingImpact(Entity rot, double fallDistance) {
			if (rot == null || !(rot.level() instanceof net.minecraft.world.level.Level level)) return;
			if (fallDistance >= 4.0) {
				Vec3 pos = rot.position();
				AABB shockBox = rot.getBoundingBox().inflate(6.0);
				List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, shockBox, e -> e != rot && e.isAlive() && (e instanceof Player || e instanceof Mob));
				for (LivingEntity enemy : enemies) {
					Vec3 push = enemy.position().subtract(pos).normalize().scale(0.4).add(0, 0.25, 0);
					enemy.setDeltaMovement(enemy.getDeltaMovement().add(push));
					enemy.hasImpulse = true;
					enemy.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 40, 0));
				}
				if (level instanceof ServerLevel serverLevel && !enemies.isEmpty()) {
					serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.2, pos.z, 12, 1.5, 0.2, 1.5, 0.1);
				}
			}
		}

		public static void onPlayerLoggedOut(UUID playerUuid) {
			OBSERVATIONS.remove(playerUuid);
			PENDING_PREDICTIONS.remove(playerUuid);
			PlayerBehaviorTracker.remove(playerUuid);
		}

		public static void pruneStaleProfiles(long currentTick) {
			PROFILES.entrySet().removeIf(entry -> {
				String key = entry.getKey();
				if (key != null && key.startsWith("player:")) {
					CombatProfile profile = entry.getValue();
					return profile != null && profile.lastAttackTick > 0 && (currentTick - profile.lastAttackTick > 72000);
				}
				return false;
			});
		}

		public static void clearPrediction(Entity rot) {
			setRotPersistentString(rot, K_SPTL, "NONE");
			setRotPersistentDouble(rot, K_SPTS, 0.0);
			setRotPersistentBoolean(rot, K_SPAI, false);
			setRotPersistentBoolean(rot, K_SWD, false);
		}
	}

	public static boolean getRotPersistentBoolean(Entity entity, String key, boolean fallback) {
		return entity.getPersistentData().getBoolean(key).orElse(fallback);
	}

	public static int getRotPersistentInt(Entity entity, String key, int fallback) {
		return entity.getPersistentData().getInt(key).orElse(fallback);
	}

	public static double getRotPersistentDouble(Entity entity, String key, double fallback) {
		return entity.getPersistentData().getDouble(key).orElse(fallback);
	}

	public static String getRotPersistentString(Entity entity, String key, String fallback) {
		return entity.getPersistentData().getString(key).orElse(fallback);
	}

	public static void setRotPersistentBoolean(Entity entity, String key, boolean val) {
		entity.getPersistentData().putBoolean(key, val);
	}

	public static void setRotPersistentInt(Entity entity, String key, int val) {
		entity.getPersistentData().putInt(key, val);
	}

	public static void setRotPersistentDouble(Entity entity, String key, double val) {
		entity.getPersistentData().putDouble(key, val);
	}

	public static void setRotPersistentString(Entity entity, String key, String val) {
		entity.getPersistentData().putString(key, val);
	}

	public static boolean hasNBTKey(CompoundTag tag, String key) {
		return tag.contains(key);
	}

}
// 1.21.8. never delete version comments
