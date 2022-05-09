package chanbot.RobotStrategies;

import battlecode.common.*;

public strictfp class WatchtowerStrategy {

  public static void runWatchtower(RobotController rc) throws GameActionException {

    // Try to attack someone
    int radius = rc.getType().actionRadiusSquared;
    Team opponent = rc.getTeam().opponent();
    RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
    if (enemies.length > 0) {
      MapLocation toAttack = enemies[0].location;
      if (rc.canAttack(toAttack)) {
        rc.attack(toAttack);
      }
    }
  }

}
