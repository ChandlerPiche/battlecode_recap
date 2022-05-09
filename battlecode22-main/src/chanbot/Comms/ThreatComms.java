package chanbot.Comms;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import chanbot.Helpers;
import chanbot.RobotPlayer;

import java.util.ArrayList;

// THREATS

public class ThreatComms {
  /**
   * this adds a maplocation to threats
   */
  public static void WriteThreat(RobotController rc, MapLocation x) {
    try {
      if (!RobotPlayer.is_threats_full) {
        for (int i = 0; i < 10; i++) {
          if (rc.readSharedArray(20 + i) == 0) {
            rc.writeSharedArray(20 + i, LocToInt(x));
            break;
          }
        }
        RobotPlayer.is_threats_full = true;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * @param rc
   * @return null if no threat found, otherwise closest threat to the unit
   */
  public static MapLocation GetClosestThreat(RobotController rc) throws GameActionException {
    MapLocation result = null;
    int closest_distance = Integer.MAX_VALUE;
    for (int i = 0; i < 10; i++) {
      int q = rc.readSharedArray(20 + i);
      if (q != 0) {
        MapLocation y = IntToLoc(q);
        int actual_dist = y.distanceSquaredTo(RobotPlayer.currentLocation);
        if (actual_dist < closest_distance) {
          result = y;
          closest_distance = actual_dist;
        }
      } else {
        break;
      }
    }
    return result;
  }

  public static void ResetThreats(RobotController rc) {
    try {
      for (int i = 0; i < 10; i++) {
        if (rc.readSharedArray(20 + i) != 0) {
          rc.setIndicatorDot(IntToLoc(rc.readSharedArray(20 + i)), 255, 0, 0);
          rc.writeSharedArray(20 + i, 0);

        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // converts an int into a Map Location
  private static MapLocation IntToLoc(int locInt) {
    return new MapLocation(locInt / 100, locInt % 100);
  }

  // converts a MapLocation into an int
  private static int LocToInt(MapLocation loc) {
    return (loc.x * 100) + loc.y;
  }
}
