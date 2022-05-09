package chanbot.ExtendedRobotController;

import battlecode.common.*;
import chanbot.Helpers;
import chanbot.RobotPlayer;
import chanbot.Comms.Comms;

import java.util.ArrayList;

public class ExtendedRobotController {

  // this is a wrapper for move which reports enemy archon locations as well as
  // lead patches
  static public void scoutTowardPoint(RobotController rc, MapLocation move_target, int loops)
      throws GameActionException {

    // REPORT LEAD
    if (rc.getRoundNum() % 4 == 0) {
      int potentialLead = Helpers.lookForLead(rc);

      // TODO: figure out what number should be cutoff for lead reporting
      if (potentialLead > 40) {
        Comms.reportLeadDeposit(rc, rc.getLocation(), potentialLead);
      }
    }

    // REPORT ENEMY ARCHONS
    else if (rc.getRoundNum() % 4 == 1) {
      RobotInfo enemy = Helpers.nearbyRobotInfo(rc, RobotType.ARCHON, true);
      if (enemy != null) {
        Comms.reportEnemyArchonLoc(rc, enemy);
      }
    }

    RobotPlayer.ReportThreats(rc);

    Pathfinder.smartMove(rc, move_target, loops, 1);
  }

  static public void combatMove(RobotController rc, MapLocation x) {
    Pathfinder.smartMove(rc, x, 3, 1);
  }
}
