package chanbot.RobotStrategies;

import battlecode.common.*;
import chanbot.Helpers;
import chanbot.RobotPlayer;
import chanbot.Comms.Comms;
import chanbot.Comms.MyArchonComms;
import chanbot.Comms.MyArchonObject;
import chanbot.ExtendedRobotController.ExtendedRobotController;

public strictfp class Laboratory {

  int maxPrice = 2;
  static Boolean has_ever_moved = false;
  static Boolean has_visited_safe_point = false;
  static Boolean has_visited_safe_point2 = false;
  static Boolean approach_2 = false;

  /**
   * returns the rubble in an area around the lab
   */
  // could be mem heavy
  private static int localRubble(RobotController rc, int dist_square) throws GameActionException {
    int c = 0;
    int rubble_total = 0;
    MapLocation[] tiles = rc.getAllLocationsWithinRadiusSquared(RobotPlayer.currentLocation, dist_square);
    for (MapLocation tile : tiles) {
      if (rc.canSenseLocation(tile)) {
        c++;
        rubble_total += rc.senseRubble(tile);
      }
    }
    return rubble_total / c;
  }

  public static void runLaboratory(RobotController rc) throws GameActionException {

    // if (has_ever_moved == false) {
    // if (rc.canTransform()) {
    // rc.transform();
    // has_ever_moved = true;
    // }
    // }

    int units_in_sight = RobotPlayer.visible_ally_robots.length + RobotPlayer.visible_enemy_robots.length;
    MapLocation safest_corner = MyArchonObject.PocketCorner(rc, MyArchonComms.GetArchons(rc));
    MapLocation second_safest_corner = MyArchonObject.PocketCorner2(rc, MyArchonComms.GetArchons(rc));

    // TRANSMUTE OR RELOCATE
    if (rc.getMode() == RobotMode.TURRET) {
      Comms.UpdateLabsAtBestCorner(rc, RobotPlayer.visible_ally_labs.size());
      if (units_in_sight < 6) {
        if (rc.getTeamLeadAmount(RobotPlayer.my_team) >= rc.getTransmutationRate()) {
          rc.transmute();
        }
      } else {
        if (RobotPlayer.currentLocation.distanceSquaredTo(safest_corner) <= 5) {
          if (rc.getTeamLeadAmount(RobotPlayer.my_team) >= rc.getTransmutationRate()) {
            rc.transmute();
          }
        } else if (rc.canTransform()) {
          rc.transform();
        }
      }

    } else { // PORTABLE

      // SETTLE ANYTIME YOU LESS < 3 and no drois
      if (units_in_sight < 3) {
        // if (rc.senseRubble(RobotPlayer.currentLocation) < localRubble(rc, 10)) {
        if (rc.canTransform()) {
          rc.transform();
        }
        // }
      }

      // if youre within 5 of safe corner, settle
      if (RobotPlayer.currentLocation.distanceSquaredTo(safest_corner) <= 5
          || RobotPlayer.currentLocation.distanceSquaredTo(second_safest_corner) <= 5) {
        // Comms.UpdateLabsAtBestCorner(rc, RobotPlayer.visible_ally_labs.size());
        // if (rc.senseRubble(RobotPlayer.currentLocation) < localRubble(rc, 10)) {
        if (rc.canTransform()) {
          rc.transform();
        }
        // }
      }

      int visible_turret_ally_labs = 0;
      for (RobotInfo lab : RobotPlayer.visible_ally_labs) {
        if (lab.mode == RobotMode.TURRET) {
          visible_turret_ally_labs++;
        }
      }

      // if more than 3 are settled
      if (visible_turret_ally_labs >= 3 && rc.canSenseLocation(safest_corner)) {
        approach_2 = true;
      }
      ;

      if (approach_2) {
        ExtendedRobotController.scoutTowardPoint(rc, second_safest_corner, 3);
      } else {
        ExtendedRobotController.scoutTowardPoint(rc, safest_corner, 3);
      }
    }
  }
}
