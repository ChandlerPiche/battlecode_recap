package chanbot.RobotStrategies;

import battlecode.common.*;
import chanbot.Helpers;
import chanbot.RobotPlayer;
import chanbot.Comms.Comms;
import chanbot.ExtendedRobotController.ExtendedRobotController;
import chanbot.ExtendedRobotController.Pathfinder;

public strictfp class Builder {

  private static void Repair(RobotController rc) throws GameActionException {
    RobotInfo chosen = null;
    for (RobotInfo robot : RobotPlayer.actionable_ally_robots) {
      if (robot.health < robot.type.getMaxHealth(robot.level)) {
        if (robot.mode == RobotMode.PROTOTYPE) {
          if (rc.canRepair(robot.location)) {
            rc.repair(robot.location);
            return;
          }
        }
        if (robot.mode != RobotMode.DROID) {
          chosen = robot;
        }
      }
    }

    if (chosen != null) {
      if (rc.canRepair(chosen.location)) {
        rc.repair(chosen.location);
        return;
      }
    }
  }

  private static void Build(RobotController rc, RobotType type) throws GameActionException {
    int lowest_rubble = 100;
    Direction choice = Direction.NORTH;
    for (Direction x : Direction.allDirections()) {
      MapLocation this_dir_loc = RobotPlayer.currentLocation.add(x);
      if (x != Direction.CENTER && rc.canSenseLocation(this_dir_loc) && !rc.isLocationOccupied(this_dir_loc)) {
        int this_dir_rubble = rc.senseRubble(this_dir_loc);
        if (this_dir_rubble < lowest_rubble) {
          lowest_rubble = this_dir_rubble;
          choice = x;
        }
      }
    }
    if (rc.canBuildRobot(type, choice)) {
      rc.buildRobot(type, choice);
    }
  }

  private static void MutateArchon(RobotController rc) throws GameActionException {
    for (Direction x : Direction.allDirections()) {
      MapLocation this_dir_loc = RobotPlayer.currentLocation.add(x);
      if (x != Direction.CENTER) {
        if (rc.canMutate(this_dir_loc) && RobotPlayer.visible_ally_labs.size() == 0) {
          rc.mutate(this_dir_loc);
        }
      }
    }
  }

  /**
   * BUILDER PRIORITIES
   * 1. ACTION: Repair an adjacent ally if possible
   *
   */
  public static void runBuilder(RobotController rc) throws GameActionException {

    // DIRECT APPROACHES PROTOTYPE LABS
    RobotInfo nearby_lab = Helpers.nearbyRobotInfo(rc, RobotType.LABORATORY, false);
    if (nearby_lab != null && nearby_lab.mode == RobotMode.PROTOTYPE) {
      ExtendedRobotController.scoutTowardPoint(rc, nearby_lab.location, 3);
    }

    // DIRECT APPROACHES ALLY ARCHONS
    RobotInfo nearby_archon = Helpers.nearbyRobotInfo(rc, RobotType.ARCHON, false);
    if (nearby_archon != null) {
      ExtendedRobotController.scoutTowardPoint(rc, nearby_archon.location, 3);
    }

    // UPGRADE ARCHON IF POSSIBLE
    MutateArchon(rc);

    // build a lab if a lab cannot be seen
    if (RobotPlayer.visible_ally_labs.size() == 0
        && rc.getTeamLeadAmount(RobotPlayer.my_team) >= RobotType.LABORATORY.buildCostLead
        && Comms.ShouldBuilderMakeLab(rc)) {
      Build(rc, RobotType.LABORATORY);
      Comms.UpdateLabsBuiltTotal(rc);
    }

    Repair(rc);
  }
}
