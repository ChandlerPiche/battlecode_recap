package chanbot.RobotStrategies;

import battlecode.common.*;
import chanbot.Helpers;
import chanbot.RobotPlayer;
import chanbot.Comms.Comms;
import chanbot.Comms.MyArchonComms;
import chanbot.Comms.ThreatComms;
import chanbot.ExtendedRobotController.ExtendedRobotController;
import chanbot.ExtendedRobotController.Pathfinder;

import java.util.ArrayList;

public strictfp class SoldierStrategy {

  // SOLDIER VARS
  static MapLocation targetLocation = null;
  static int parentID = -1;
  static MapLocation parentArchonLoc;
  boolean enemyArchonSighted = false;

  private static void soldierExploration(RobotController rc) throws GameActionException {
    if (targetLocation == null) {
      MapLocation knownEnemyLoc = Comms.getNearestEnemyArchon(rc); // TODO: use this?
      targetLocation = Comms.getScoutingTarget(rc, false);

    } else { // target location set

      ExtendedRobotController.scoutTowardPoint(rc, targetLocation, 3);

      // if target is within sight, check for enemy archon
      if (rc.canSenseLocation(targetLocation)) {
        rc.setIndicatorString("REACHED ENEMY ZONE" + targetLocation.x + " " + targetLocation.y);
        RobotInfo enemy = Helpers.nearbyRobotInfo(rc, RobotType.ARCHON, true);

        if (enemy != null) {
          // TODO: add better attach logic ?
        } else {
          // System.out.println("No ENEMY FOUND AT " + targetLocation.x + " " +
          // targetLocation.y);
          Comms.removeScoutingTarget(rc, targetLocation);
          targetLocation = Comms.getScoutingTarget(rc, true);
        }
      }
    }
    if (targetLocation != null) {
      Pathfinder.smartMove(rc, targetLocation, 3, 1);
    }
  }

  public static void runSoldier(RobotController rc) throws GameActionException {
    // assign parent archon info if its missing
    if (parentID == -1) {
      RobotInfo parent = Helpers.nearbyRobotInfo(rc, RobotType.ARCHON, false);
      if (parent != null) {
        parentID = parent.getID();
        parentArchonLoc = new MapLocation(parent.location.x, parent.location.y);
      }
    }

    // PRE MOVEMENT ACTIONS
    ShootingHelpers.shoot(rc);

    // MOVEMENT
    if (RobotPlayer.visible_enemy_robots.length != 0) { // COMBAT STANCE
      MovementH.tradingStance(rc);
    } else { // EXPLORATION STANCE
      MapLocation closest_threat = ThreatComms.GetClosestThreat(rc);
      if (ThreatComms.GetClosestThreat(rc) != null) {
        ExtendedRobotController.scoutTowardPoint(rc, closest_threat, 3);
      } else {
        soldierExploration(rc);
      }
    }

    // POST MOVEMENT ACTIONS
    ShootingHelpers.shoot(rc);
  }
}

class ShootingHelpers {

  // PRIVATE HELPERS
  private static int getShootingPriority(RobotInfo robot) {
    RobotType type = robot.type;
    int level = robot.level;
    if (type == RobotType.WATCHTOWER && level != 1) {
      return 7;
    } else if (type == RobotType.SAGE) {
      return 6;
    } else if (type == RobotType.SOLDIER) {
      return 5;
    } else if (type == RobotType.LABORATORY) {
      return 4;
    } else if (type == RobotType.WATCHTOWER) {
      return 3;
    } else if (type == RobotType.BUILDER) {
      return 2;
    } else if (type == RobotType.MINER) {
      return 1;
    } else if (type == RobotType.ARCHON) {
      return 0;
    }
    return 0;
  }

  private static ArrayList<RobotInfo> getBestTargets() {
    // get a list of the types of robots we want to shoot
    ArrayList<RobotInfo> best_type_options = new ArrayList<>();
    int best_type_prio = 0;
    for (RobotInfo enemy : RobotPlayer.actionable_enemy_robots) {
      int enemy_prio = getShootingPriority(enemy);
      if (enemy_prio < best_type_prio) {
      } else if (enemy_prio == best_type_prio) {
        best_type_options.add(enemy);
      } else { // >=
        best_type_prio = enemy_prio;
        best_type_options.clear();
        best_type_options.add(enemy);
      }
    }
    return best_type_options;
  }

  private static RobotInfo selectTargetByHealth(ArrayList<RobotInfo> narrowed_targets) {
    RobotInfo target = null;
    Boolean execute = false;
    int lowest_hp = Integer.MAX_VALUE;
    int largest_executable_hp = Integer.MIN_VALUE;
    for (RobotInfo enemy : narrowed_targets) {
      // execute healthiest visible target OR shoot lowest HP target
      if (!execute && enemy.health <= RobotPlayer.my_type.getDamage(RobotPlayer.my_level)) {
        execute = true;
      }

      if (execute == true) { // execute healthiest visible target
        if (enemy.health > largest_executable_hp && enemy.health <= RobotPlayer.my_damage) {
          target = enemy;
        }
      } else { // shoot lowest health
        if (enemy.health < lowest_hp) {
          target = enemy;
          lowest_hp = enemy.health;
        }
      }
    }
    return target;
  }

  // PUBLIC
  public static void shoot(RobotController rc) throws GameActionException {

    ArrayList<RobotInfo> narrowed_targets = ShootingHelpers.getBestTargets();
    if (narrowed_targets.size() > 0) {
      Helpers.Log(rc, "B", 10049, 631);
      RobotInfo target = ShootingHelpers.selectTargetByHealth(narrowed_targets);
      Helpers.Log(rc, "C" + target.location, 10049, 631);
      if (rc.isActionReady()) {
        if (rc.canAttack(target.location)) { // THIS ISNT REDUNDANT. DEV BUG?
          rc.attack(target.location);
        }
      }
    }
  }
}
