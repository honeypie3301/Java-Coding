export type Difficulty = 'EASY' | 'NORMAL' | 'HARD';

export type TriggerType = 'staring' | 'proximity' | 'attacked';

export interface AIState {
  watchTimer: number;
  ticksToEnrage: number;
  enraged: boolean;
  triggerType: TriggerType;
  stalkTimer: number;
  stalkGoal: number;
  cycleCount: number;
  mineProgress: number;
  mineThreshold: number;
  activeSplinters: number;
  meleeDodgeCooldown: number;
  projDodgeCooldown: number;
  telegraphTicks: number;
  currentSpeed: number;
  distanceToPlayer: number;
  playerFacingAngle: number; // degrees relative to Stilt
  isPlayerWatched: boolean;
  isPlayerInProximity: boolean;
  difficulty: Difficulty;
  stiltHp: number;
  maxStiltHp: number;
}

export interface SimLogEvent {
  id: string;
  timestamp: string;
  type: 'info' | 'warn' | 'alert' | 'danger' | 'success';
  message: string;
}

export interface WikiFeature {
  id: string;
  category: string;
  title: string;
  summary: string;
  details: string[];
  oldBehavior: string;
  newBehavior: string;
  tags: string[];
}
