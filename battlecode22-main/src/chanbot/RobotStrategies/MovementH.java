package chanbot.RobotStrategies;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import chanbot.Helpers;
import chanbot.RobotPlayer;
import chanbot.Comms.MyArchonComms;
import chanbot.ExtendedRobotController.ExtendedRobotController;

public class MovementH {

  // should only be called on robots you can see
  static private float getRobotStationaryDPS(RobotController rc, RobotInfo robot, Boolean is_potential)
      throws GameActionException {
    if (is_potential) {
      float num = robot.type.getDamage(robot.level) * 10 / robot.type.actionCooldown;
      float denom = 1;
      if (num > 0) {
        return num / denom;
      }
    } else {
      float num = robot.type.getDamage(robot.level) * 10 / robot.type.actionCooldown;
      float denom = 1 + (rc.senseRubble(robot.location) / 10);
      if (num > 0) {
        return num / denom;
      }
    }
    return 0;
  }

  static private float myDPS(RobotController rc, Boolean is_potential) throws GameActionException {
    if (is_potential) {
      return (float) RobotPlayer.my_type.getDamage(RobotPlayer.my_level) * 10 / (1 * rc.getActionCooldownTurns());
    } else {
      return (float) RobotPlayer.my_type.getDamage(RobotPlayer.my_level) * 10
          / ((1 + (rc.senseRubble(rc.getLocation()) / 10)) * rc.getActionCooldownTurns());
    }
  }

  // loop over all enemies
  static private float getVisibleEnemyDPS(RobotController rc, Boolean is_potential) throws GameActionException {
    int dps_tally = 0;
    Boolean doISeeEnemyArchon = false; // if so, add x to our teams DPS
    for (RobotInfo robot : RobotPlayer.visible_enemy_robots) {
      if (robot.type == RobotType.ARCHON) {
        doISeeEnemyArchon = true;
      }
      dps_tally += getRobotStationaryDPS(rc, robot, is_potential);
    }
    return dps_tally + (doISeeEnemyArchon ? 9 : 0);
  }

  // include self
  static private float getVisibleAllyDPS(RobotController rc, Boolean is_potential) throws GameActionException {
    float dps_tally = myDPS(rc, is_potential);

    Boolean doISeeAllyArchon = false; // if so, add x to our teams DPS
    for (RobotInfo robot : RobotPlayer.visible_ally_robots) {
      if (robot.type == RobotType.ARCHON) {
        doISeeAllyArchon = true;
      }
      if (robot.health >= robot.type.getMaxHealth(1) / 2) {
        dps_tally += getRobotStationaryDPS(rc, robot, is_potential);
      }
    }
    return dps_tally > 0 ? dps_tally + (doISeeAllyArchon ? 21 : 0) : dps_tally;
  }

  // CALL THIS BEFORE SCOUTING MOVEMENT IFF visible_enemies > 0
  static public void tradingStance(RobotController rc) throws GameActionException { // if we think fighting here might
                                                                                    // be DPS positive, then do it
                                                                                    // (UNLESS ARCHON)

    float enemy_dps = getVisibleEnemyDPS(rc, false);
    float enemy_potential_dps = getVisibleEnemyDPS(rc, true);
    float friendly_dps = getVisibleAllyDPS(rc, false);
    float friendly_potential_dps = getVisibleAllyDPS(rc, true);
    rc.setIndicatorString(
        "US(" + friendly_dps + "," + friendly_potential_dps + ")(" + enemy_dps + "," + enemy_potential_dps + ")");
    if (friendly_dps > enemy_dps + 2 && friendly_potential_dps + 1 > enemy_potential_dps) {
      pursuit(rc);
    } else {
      retreat(rc);
    }
  }

  // move towards weakest enemy
  static private void pursuit(RobotController rc) throws GameActionException {
    ExtendedRobotController.combatMove(rc, Helpers.Nearest(rc, RobotPlayer.visible_enemy_robots));
  }

  // move towards nearest friendly archon
  static private void retreat(RobotController rc) throws GameActionException {
    ExtendedRobotController.combatMove(rc, MyArchonComms.GetNearestAlliedArchon(rc, rc.getLocation()));
  }
}
