package chanbot;

import battlecode.common.*;

public class Helpers {

  public static AnomalyScheduleEntry[] anomaly_schedule = null;

  /**
   * checks if a certain robot type is nearby (selects randomly)
   */
  public static RobotInfo nearbyRobotInfo(RobotController rc, RobotType target, boolean enemy)
      throws GameActionException {
    int radius = rc.getType().visionRadiusSquared;
    Team targetTeam = rc.getTeam();
    if (enemy) {
      targetTeam = rc.getTeam().opponent();
    }
    RobotInfo[] enemies = rc.senseNearbyRobots(radius, targetTeam);
    for (RobotInfo r : enemies) {
      if (r.getType() == target && (r.team == targetTeam)) {
        return r;
      }
    }
    return null;
  }

  // returns random wounded ally droid in action range, or NULL
  public static RobotInfo findInjuredDroid(RobotController rc) {
    RobotInfo[] robots = RobotPlayer.visible_ally_robots;
    for (RobotInfo r : robots) {
      RobotType r_type = r.type;
      if (!r_type.isBuilding() && r.getHealth() < r_type.getMaxHealth(r.getLevel())
          && RobotPlayer.currentLocation.isWithinDistanceSquared(r.location, RobotPlayer.action_radius_squared)) {
        return r;
      }
    }
    return null;
  }

  /**
   * Checks surrounding tiles for lead
   * returns: total amount of lead the robot can see
   */
  public static int lookForLead(RobotController rc) throws GameActionException {
    MapLocation[] leadLocations = rc.senseNearbyLocationsWithLead(500, 2);
    int totalLead = 0;
    for (MapLocation loc : leadLocations) {
      totalLead += rc.senseLead(loc);
    }
    return totalLead;
  }

  public static MapLocation getRandomPoint(RobotController rc) throws GameActionException {
    RobotPlayer.rng.setSeed(rc.getID()); // rng needs different number otherwise all miners have same dir

    return new MapLocation(RobotPlayer.rng.nextInt(rc.getMapWidth()), RobotPlayer.rng.nextInt(rc.getMapHeight()));
  }

  /**
   * Helpers.Log(msg, optional: id, optional: turn)
   */
  // 15 BYTECODE
  static public void Log(RobotController rc, String msg) {
    System.out.println(msg);
  }

  static public void Log(RobotController rc, String msg, int id) {
    if (rc.getID() == id) {
      System.out.println(msg);
    }
  }

  static public void Log(RobotController rc, String msg, int id, int turn) {
    if (rc.getID() == id) {
      if (rc.getRoundNum() == turn) {
        System.out.println(msg);
      }
    }
  }

  public static RobotType decodeRobotType(int encodedRobotType) {
    switch (encodedRobotType) {
      case 4:
        return RobotType.MINER;
      case 5:
        return RobotType.BUILDER;
      case 6:
        return RobotType.SOLDIER;
      default:
        return null;
    }
  }

  // returns whether or not is this many days to the given anomaly type
  public static Boolean daysUntilAnomaly(RobotController rc, AnomalyType type, int i) {
    if (anomaly_schedule == null) {
      anomaly_schedule = rc.getAnomalySchedule();
    }

    for (AnomalyScheduleEntry entry : anomaly_schedule) {
      if (entry.anomalyType == type && entry.roundNumber == RobotPlayer.round_number + i) {
        return true;
      } else if (entry.roundNumber > RobotPlayer.round_number + i) {
        return false;
      }
    }
    return false;
  }

  /**
   *
   * @param robots should have (length > 0)
   * @return location of the nearest robot (shouldn't be null)
   */
  public static MapLocation Nearest(RobotController rc, RobotInfo[] robots) {
    int distance = Integer.MAX_VALUE;
    MapLocation y = null;
    for (RobotInfo robot : robots) {
      int x = robot.location.distanceSquaredTo(rc.getLocation());
      if (x < distance) {
        distance = x;
        y = robot.location;
      }
    }
    return y;
  }
}
