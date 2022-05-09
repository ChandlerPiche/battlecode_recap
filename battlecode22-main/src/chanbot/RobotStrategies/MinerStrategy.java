package chanbot.RobotStrategies;

import battlecode.common.*;
import chanbot.Helpers;
import chanbot.RobotPlayer;
import chanbot.Comms.Comms;
import chanbot.Comms.MyArchonComms;
import chanbot.ExtendedRobotController.ExtendedRobotController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

enum MinerModes {
  MINING_PATCH, GOING_TO_PATCH, EXPLORING
}

// TODO: try to stand on low rubble while mining
public strictfp class MinerStrategy {

  static MapLocation targeted_ore = null;

  /**
   * Mines the richest adjacent patch
   */
  static private boolean SwingingPickaxe(RobotController rc) throws GameActionException {
    // Try to mine on squares around us.
    MapLocation me = RobotPlayer.currentLocation;
    boolean isMining = false;

    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
        if (rc.canSenseLocation(mineLocation)) {
          while (rc.isActionReady()) {
            if (rc.canMineLead(mineLocation) && rc.senseLead(mineLocation) > 1) {
              rc.mineLead(mineLocation);
            } else if (rc.canMineGold(mineLocation)) {
              rc.mineGold(mineLocation);
            } else {
              break;
            }
          }
        }
      }
    }
    return isMining;
  }

  // returns the location of a random unmined nearby ore patch (null if none
  // exist)
  static private MapLocation OreScan(RobotController rc, Boolean ignore_occupied) throws GameActionException {
    ArrayList<MapLocation> nearbyLead = new ArrayList<>(Arrays.asList(rc.senseNearbyLocationsWithLead(1000, 2)));
    ArrayList<MapLocation> nearbyGold = new ArrayList<>(Arrays.asList(rc.senseNearbyLocationsWithGold()));
    if (nearbyGold.size() > 0) {
      return nearbyGold.get(0);
    } else if (nearbyLead.size() > 0) {
      if (ignore_occupied) {
        ArrayList<MapLocation> unoccupiedLead = new ArrayList<>();
        for (MapLocation lead : nearbyLead) {
          if (AmIClosestMinerTo(rc, lead)) {
            unoccupiedLead.add(lead);
          }
        }
        if (unoccupiedLead.size() > 0) {
          return unoccupiedLead.get(RobotPlayer.rng.nextInt(unoccupiedLead.size()));
        } else {
          return null;
        }
      } else {
        return nearbyLead.get(RobotPlayer.rng.nextInt(nearbyLead.size()));
      }
    }
    return null;
  }

  private static boolean AmIClosestMinerTo(RobotController rc, MapLocation lead) {
    int my_distance = rc.getLocation().distanceSquaredTo(lead);
    for (RobotInfo ally_miner : RobotPlayer.visible_ally_miners) { // THIS SHOULD BE VISIBLE
      if (ally_miner.getLocation().distanceSquaredTo(lead) <= my_distance) {
        return false;
      }
    }
    return true;
  }

  // RANDOM EXPLORATION LOGIC
  static MapLocation randomPoint = null;

  // ORE LOCATING LOGIC
  static MapLocation targetOre = null;

  static MinerModes CurrentMode = MinerModes.EXPLORING;
  static MapLocation CurrentTarget;

  /**
   * Run a single turn for a Miner.
   * This code is wrapped inside the infinite loop in run(), so it is called once
   * per turn.
   */
  public static void runMiner(RobotController rc) throws GameActionException {

    // =========================== INITIALIZATION =============================
    Random rng = new Random(rc.getID() + rc.getRoundNum());
    int currentRound = rc.getRoundNum();
    MapLocation minerLocation = rc.getLocation();
    if (randomPoint == null) {
      randomPoint = new MapLocation(
          rng.nextInt(rc.getMapWidth()),
          rng.nextInt(rc.getMapHeight()));
    }

    // =============================== MINER TURN ===============================
    if (CurrentTarget == null) {
      CurrentMode = MinerModes.EXPLORING;
      CurrentTarget = randomPoint;
    }

    Boolean in_danger = false;
    for (RobotInfo robot : RobotPlayer.visible_enemy_robots) {
      if (robot.type == RobotType.SOLDIER || robot.type == RobotType.SAGE) {
        in_danger = true;
      }
    }

    if (in_danger) {
      Helpers.Log(rc, "danger");

      ExtendedRobotController.scoutTowardPoint(rc,
          MyArchonComms.GetNearestAlliedArchon(rc, RobotPlayer.currentLocation), 3);
      rc.setIndicatorLine(RobotPlayer.currentLocation,
          MyArchonComms.GetNearestAlliedArchon(rc, RobotPlayer.currentLocation), 0, 0, 0);
    }

    rc.setIndicatorString(CurrentMode.toString());

    switch (CurrentMode) {
      case GOING_TO_PATCH: // GOING_TO_PATCH -> EXPLORING
        // TODO: check if the lead on the patch is still > 0
    }

    switch (CurrentMode) {
      case EXPLORING: // EXPLORING -> GOING_TO_PATCH (only if im the closest to a visible ore)
        MapLocation nearbyOre = OreScan(rc, true);
        if (nearbyOre != null) {
          rc.setIndicatorString("Nearby ore sighted");
          CurrentTarget = nearbyOre;
          CurrentMode = MinerModes.GOING_TO_PATCH;
        }
        break;
      case GOING_TO_PATCH: // GOING_TO_PATCH -> MINING_PATCH
        if (minerLocation.isWithinDistanceSquared(CurrentTarget, rc.getType().actionRadiusSquared)) {
          CurrentMode = MinerModes.MINING_PATCH;
        }
      default:
        break;
    }

    switch (CurrentMode) {
      case EXPLORING: // CONTINUE, CHOOSE NEW RANDOMPOINT IF REQUIRED
        if (minerLocation.isWithinDistanceSquared(randomPoint, 20)) {
          randomPoint = new MapLocation(
              rng.nextInt(rc.getMapWidth()),
              rng.nextInt(rc.getMapHeight()));
          CurrentTarget = randomPoint;
        } else {
          ExtendedRobotController.scoutTowardPoint(rc, CurrentTarget, 3);
        }
        break;

      case MINING_PATCH: // MINING ACTION + MINING_PATCH -> EXPLORING
        rc.setIndicatorString("Mining patch");
        while (rc.isActionReady()) {

          // UPDATE SHARED LEAD LIST
          if (currentRound % 4 == 0) {
            int nearbyLeadAmount = Helpers.lookForLead(rc);
            Comms.updateLeadDeposit(rc, rc.getLocation(), nearbyLeadAmount);
          }

          if (!SwingingPickaxe(rc)) { // NOT IN MINING RANGE OF ANY LEAD

            // UPDATE SHARED LEAD LIST
            int nearbyLeadAmount = Helpers.lookForLead(rc);
            Comms.updateLeadDeposit(rc, CurrentTarget, nearbyLeadAmount);

            // TRY TO CONTINUE MINING
            CurrentTarget = OreScan(rc, true);
            if (CurrentTarget == null) {
              CurrentTarget = randomPoint;
              CurrentMode = MinerModes.EXPLORING;
            } else {
              ExtendedRobotController.scoutTowardPoint(rc, CurrentTarget, 3);
            }
            break;
          }
        }

      case GOING_TO_PATCH:
        ExtendedRobotController.scoutTowardPoint(rc, CurrentTarget, 3);
    }

    rc.setIndicatorLine(RobotPlayer.currentLocation, CurrentTarget, 120, 120, 120);
  }
}