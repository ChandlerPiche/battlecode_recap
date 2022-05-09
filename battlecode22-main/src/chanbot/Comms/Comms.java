package chanbot.Comms;

import battlecode.common.*;
import chanbot.RobotPlayer;

public strictfp class Comms {

  /**
   * 2^16
   * ARRAY USAGE
   * [0-12] : Scouting Locations List
   * [17, 18, 19]: labs built, labs at safest corner, last at 2nd safest corner
   * 100, 150, 200, etc.
   * [20-29]: Threats
   * [30-37]: Build Plan Logic
   * [40-49]: Lead Patch Locations List
   * [57-64]: Enemy Archon Locations
   */

  public static Boolean ShouldBuilderMakeLab(RobotController rc) {
    Boolean res = false;
    try {
      if (rc.readSharedArray(18) + rc.readSharedArray(19) <= 3
          && rc.readSharedArray(17) * 50 + 200 < RobotPlayer.round_number && rc.readSharedArray(17) <= 10) {
        res = true;
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      return res;
    }
  }

  public static void UpdateLabsBuiltTotal(RobotController rc) {
    try {
      rc.writeSharedArray(17, rc.readSharedArray(17) + 1);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void UpdateLabsAtBestCorner(RobotController rc, int x) {
    try {
      if (rc.readSharedArray(18) != x) {
        rc.writeSharedArray(18, x);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static int GetLabsAtBestCorner(RobotController rc) throws GameActionException {
    return rc.readSharedArray(18);
  }

  // public static void UpdateLabsAt2ndBestCorner(RobotController rc, int x) {
  // if (rc.readSharedArray(19) != x) {
  // rc.writeSharedArray(19, x);
  // }
  // }

  /**
   * [0]: number of locations in list
   * [1-12]: list of locations to scout
   */
  /*
   * Adds Maplocation loc to list of locations to scout
   */
  public static void addScoutingTarget(RobotController rc, MapLocation loc) throws GameActionException {
    int targetInt = LocToInt(loc);

    for (int i = 1; i < 13; i++) {
      int cur = rc.readSharedArray(i);
      if (cur == targetInt) { // don't add duplicate
        return;
      } else if (rc.readSharedArray(i) == 0) {
        rc.writeSharedArray(i, LocToInt(loc));
        rc.writeSharedArray(0, rc.readSharedArray(0) + 1);
        return;
      }
    }
    return;
  }

  /*
   * Gets location from list of locations to scout
   * (maybe should be nearest location?)
   * if nearest == true, return nearest target
   * otherwise select randomly
   */
  public static MapLocation getScoutingTarget(RobotController rc, boolean nearest) throws GameActionException {

    int numScoutingTargets = rc.readSharedArray(0);
    // if list of scouting targest not empty
    if (numScoutingTargets != 0) {
      if (nearest == false) {
        int randomIndex = RobotPlayer.rng.nextInt(rc.readSharedArray(0));
        int randomScoutingLocInt = rc.readSharedArray(randomIndex);

        if (randomScoutingLocInt != 0) {
          return IntToLoc(randomScoutingLocInt);
        }
      } else {
        MapLocation robotsLoc = rc.getLocation();
        MapLocation closestLoc = IntToLoc(rc.readSharedArray(1));
        int minDistance = robotsLoc.distanceSquaredTo(closestLoc);
        for (int i = 2; i <= numScoutingTargets; i++) {
          MapLocation curLoc = IntToLoc(rc.readSharedArray(i));
          int curDistance = robotsLoc.distanceSquaredTo(curLoc);

          if (curDistance < minDistance) {
            minDistance = curDistance;
            closestLoc = curLoc;
          }
        }
        return closestLoc;
      }
    }
    return new MapLocation(Math.floorDiv(rc.getMapWidth(), 2), Math.floorDiv(rc.getMapHeight(), 2));
  }

  /*
   * Removes loc from list of locations to scout
   * soldier do this
   */
  public static void removeScoutingTarget(RobotController rc, MapLocation loc) throws GameActionException {
    int targetInt = LocToInt(loc);
    int prevNumberOfTargets = rc.readSharedArray(0);
    if (prevNumberOfTargets == 0) {
      return;
    }
    for (int i = 1; i < 13; i++) {
      int cur = rc.readSharedArray(i);
      if (targetInt == cur) {
        // reduce length of target list
        rc.writeSharedArray(0, prevNumberOfTargets - 1);
        // swap last element with deleted one
        rc.writeSharedArray(i, rc.readSharedArray(prevNumberOfTargets));
        rc.writeSharedArray(prevNumberOfTargets, 0);
        return;
      }
    }
    return;
  }

  /**
   * [40-49] top 5 richest lead locations:
   * locations stored at (40, 42, 44, 46, 48)
   * their lead count at (41, 43, 45, 47, 49)
   */
  // TODO: figure out how far apart lead deposits should be for them two be
  // considered distinct
  private static final int minDistanceBtDeposits = 20;

  // stores leadDeposit location in list if its one of the top 5 best so far
  public static void reportLeadDeposit(RobotController rc, MapLocation leadDeposit, int leadAmount)
      throws GameActionException {

    int worstLeadCount = Integer.MAX_VALUE;
    int worstLeadIndex = 40;

    for (int i = 40; i < 49; i += 2) {
      int curPatch = rc.readSharedArray(i);
      int curLeadCount = rc.readSharedArray(i + 1);

      if (curPatch != 0) {
        // if patch is close by
        MapLocation curLoc = IntToLoc(curPatch);
        if (leadDeposit.isWithinDistanceSquared(curLoc, minDistanceBtDeposits)) {
          int locInt = LocToInt(leadDeposit);
          rc.writeSharedArray(i, locInt);
          rc.writeSharedArray(i + 1, leadAmount);
          return;
        }

        // if this is a distinct lead patch
        else {
          if (curLeadCount < worstLeadCount) {
            worstLeadCount = curLeadCount;
            worstLeadIndex = i;
          }
        }
      }

      // if there's an empty spot in the list, add lead spot
      else {
        int locInt = LocToInt(leadDeposit);
        rc.writeSharedArray(i, locInt);
        rc.writeSharedArray(i + 1, leadAmount);
        return;
      }
    }

    // replace the worst lead patch in list, if the new one is better
    if (leadAmount > worstLeadCount) {
      rc.writeSharedArray(worstLeadIndex, LocToInt(leadDeposit));
      rc.writeSharedArray(worstLeadIndex + 1, leadAmount);
    }

    return;
  }

  // updates the lead deposit total for this lead deposit
  public static void updateLeadDeposit(RobotController rc, MapLocation leadDeposit, int leadAmount)
      throws GameActionException {

    MapLocation robotLocation = rc.getLocation();

    for (int i = 40; i < 49; i += 2) {
      int curInt = rc.readSharedArray(i);
      MapLocation curLoc = IntToLoc(curInt);

      if (leadDeposit.isWithinDistanceSquared(curLoc, minDistanceBtDeposits)) {
        rc.writeSharedArray(i, LocToInt(robotLocation));
        rc.writeSharedArray(i + 1, leadAmount);
      }
    }
  }

  // returns the nearest lead deposit with >75 lead
  public static MapLocation getNearestLeadDeposit(RobotController rc) throws GameActionException {
    MapLocation nearestLead = null;
    int minDistance = Integer.MAX_VALUE;

    MapLocation robotLocation = rc.getLocation();

    for (int i = 40; i < 49; i += 2) {
      int curInt = rc.readSharedArray(i);
      if (curInt != 0) {
        MapLocation curLoc = IntToLoc(curInt);
        int curDistance = robotLocation.distanceSquaredTo(curLoc);
        if ((curDistance < minDistance) && (rc.readSharedArray(i + 1) >= 75)) {
          nearestLead = curLoc;
        }
      }
    }
    return nearestLead;
  }

  // returns total amount of lead seen
  public static int getBestLeadCount(RobotController rc) {
    int bestLeadCount = 0;

    try {
      for (int i = 41; i < 50; i += 2) {
        int curLead = rc.readSharedArray(i);
        if (curLead > bestLeadCount) {
          bestLeadCount = curLead;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      return bestLeadCount;
    }
  }

  // prints enemt archon locations
  public static void printer(RobotController rc) throws GameActionException {
    String s = "[";
    for (int i = 40; i <= 48; i += 2) {
      int n = rc.readSharedArray(i);
      MapLocation l = IntToLoc(n);
      s += l.x;
      s += ".";
      s += l.y;
      s += ": ";
      s += rc.readSharedArray(i + 1);
      s += ", ";
    }
    s += " ]";
    System.out.println(s);
  }

  /**
   * BUILD PLAN & MyArchon
   * [30-37] archons (id, spawn, location, status, alive)
   * [38] archon IDs listed here every round
   * [39] build order
   */

  /**
   * [56-63]: Enemy Archon Last Seen Locations
   * (57, 59, 61, 63): enemy archon IDs
   * (56, 58, 60, 62): enemy archon locations
   */

  public static void reportEnemyArchonLoc(RobotController rc, RobotInfo enemyArchon) throws GameActionException {
    int enemyID = enemyArchon.ID;

    for (int i = 56; i < 63; i += 2) {
      int curID = rc.readSharedArray(i);
      int curLocInt = rc.readSharedArray(i + 1);

      if (curID == enemyID) {
        rc.writeSharedArray(i + 1, LocToInt(enemyArchon.location));
        return;
      } else if ((curID == 0) && (curLocInt == 0)) {
        rc.writeSharedArray(i, enemyID);
        rc.writeSharedArray(i + 1, LocToInt(enemyArchon.location));
        return;
      }
    }
  }

  // TODO: attach this to soldier gunfire?
  public static void reportEnemyArchonDeath(RobotController rc, RobotInfo enemyArchon) throws GameActionException {
    int enemyID = enemyArchon.ID;

    for (int i = 56; i < 63; i += 2) {
      int curID = rc.readSharedArray(i);
      int curLocInt = rc.readSharedArray(i + 1);

      if (curID == enemyID) {
        rc.writeSharedArray(i + 1, 9999);
        return;
      }
    }
  }

  // returns the nearest enemy archon
  public static MapLocation getNearestEnemyArchon(RobotController rc) throws GameActionException {
    MapLocation nearestArchon = null;
    int minDistance = Integer.MAX_VALUE;

    MapLocation robotLocation = rc.getLocation();

    for (int i = 57; i < 63; i += 2) {
      int curInt = rc.readSharedArray(i);
      if ((curInt != 0) && (curInt != 9999)) {
        MapLocation curLoc = IntToLoc(curInt);
        int curDistance = robotLocation.distanceSquaredTo(curLoc);
        if (curDistance < minDistance) {
          nearestArchon = curLoc;
        }
      }
    }
    return nearestArchon;
  }

  // PRIVATE

  /**
   * Location List Helpers
   */

  // converts an int into a Map Location
  private static MapLocation IntToLoc(int locInt) {
    return new MapLocation(locInt / 100, locInt % 100);
  }

  // converts a MapLocation into an int
  private static int LocToInt(MapLocation loc) {
    return (loc.x * 100) + loc.y;
  }
}