package chanbot.RobotStrategies;

import battlecode.common.*;
import chanbot.Helpers;
import chanbot.RobotPlayer;
import chanbot.Comms.Comms;
import chanbot.Comms.MyArchonComms;
import chanbot.Comms.MyArchonObject;
import chanbot.Comms.ThreatComms;
import chanbot.ExtendedRobotController.ExtendedRobotController;
import chanbot.ExtendedRobotController.Pathfinder;

public class Sage {
  // IF CAN ATTACK, ATTACK
  // IF SAGE SEES AN ENEMY AND ATTACK IS UP, MOVE FORWARD AND ATTACK
  // IF SAGE SEES AN ENEMY AND ATTACK IS DOWN, KITE THEM
  // KITING
  // if they're closer to vision range than attack range, approach (70 - 100%)
  // if they're at a good range, don't move
  // if they're getting close to attack range, back up
  // otherwise,
  // IF SAGE SEES NO ENEMY, LOOK FOR ENEMY

  static MapLocation target_enemy_archon_location = null;

  private static void sageTrade(RobotController rc) throws GameActionException {
    MapLocation nearest_enemy = Helpers.Nearest(rc, RobotPlayer.visible_enemy_robots);
    if (rc.isActionReady()) {
      ExtendedRobotController.combatMove(rc, nearest_enemy); // APPROACH TO SHOOT
    } else {
      int dist_square_from_enemy = nearest_enemy.distanceSquaredTo(RobotPlayer.currentLocation);
      if (dist_square_from_enemy <= 28) {
        ExtendedRobotController.scoutTowardPoint(rc,
            MyArchonObject.PocketArchon(rc, MyArchonComms.GetArchons(rc)).location, 4);
      } else if (dist_square_from_enemy >= 31) {
        ExtendedRobotController.scoutTowardPoint(rc, nearest_enemy, 4); // HES ALMOST OUT OF RANGE
      } else {
        // stand still, ur in position
      }
    }
  }

  private static void sageAttack(RobotController rc) throws GameActionException {
    // OTHERWISE: ATTACK ENEMY ARCHONS
    if (target_enemy_archon_location == null) {
      MapLocation knownEnemyLoc = Comms.getNearestEnemyArchon(rc); // TODO: use this?
      target_enemy_archon_location = Comms.getScoutingTarget(rc, false);

    } else { // target location set

      ExtendedRobotController.scoutTowardPoint(rc, target_enemy_archon_location, 3);

      // if target is within sight, check for enemy archon
      if (rc.canSenseLocation(target_enemy_archon_location)) {
        RobotInfo enemy = Helpers.nearbyRobotInfo(rc, RobotType.ARCHON, true);

        if (enemy == null) {
          Comms.removeScoutingTarget(rc, target_enemy_archon_location);
          target_enemy_archon_location = Comms.getScoutingTarget(rc, true);
        }
      }
    }
    if ((target_enemy_archon_location != null)
        && (rc.canMove(RobotPlayer.currentLocation.directionTo(target_enemy_archon_location)))) {
      ExtendedRobotController.scoutTowardPoint(rc, target_enemy_archon_location, 3);
    }
  }

  // private static void sageDefend(RobotController rc) throws GameActionException
  // {
  //
  // }

  private static void sageMovement(RobotController rc) throws GameActionException {
    if (RobotPlayer.visible_enemy_robots.length > 0) {
      sageTrade(rc);
    } else {
      MapLocation closest_threat = ThreatComms.GetClosestThreat(rc);
      if (ThreatComms.GetClosestThreat(rc) != null) {
        ExtendedRobotController.scoutTowardPoint(rc, closest_threat, 3);
      } else {
        sageAttack(rc);
      }
      // TODO: DEFEND POCKET

    }
  }

  public static void runSage(RobotController rc) throws GameActionException {

    // PRE MOVEMENT ACTIONS
    ShootingHelpers.shoot(rc);

    // MOVEMENT
    sageMovement(rc);

    // POST MOVEMENT ACTIONS
    ShootingHelpers.shoot(rc);
  }
}
