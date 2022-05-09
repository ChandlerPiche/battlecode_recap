package chanbot.RobotStrategies;

import battlecode.common.*;
import chanbot.Helpers;
import chanbot.RobotPlayer;
import chanbot.Comms.Comms;
import chanbot.Comms.MyArchonComms;
import chanbot.Comms.MyArchonObject;
import chanbot.ExtendedRobotController.ExtendedRobotController;
import chanbot.ExtendedRobotController.Pathfinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Run a single turn for an Archon.
 * This code is wrapped inside the infinite loop in run(), so it is called once
 * per turn.
 */
@SuppressWarnings("ComparatorCombinators")
// TODO: store currentRound globally for all bots
// TODO: store currentLocation globally for all bots
public strictfp class Archon {

  static int miners_i_built = 0;

  private static Boolean IsAreaSaturated(RobotController rc) {
    int lead_total = 0;
    int miner_total = 0;
    try {
      for (MapLocation lead : rc.senseNearbyLocationsWithLead()) {
        lead_total += rc.senseLead(lead);
      }
      for (RobotInfo ally : RobotPlayer.visible_ally_robots) {
        if (ally.type == RobotType.MINER) {
          miner_total++;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      return miner_total * 40 > lead_total;
    }
  }

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

  public static void Transform(RobotController rc) throws GameActionException {
    if (rc.canTransform()) {
      rc.transform();
    }
  }

  public static void runArchon(RobotController rc, Boolean make_builders) throws GameActionException {

    // FIRST ROUND ENEMY ARCHON LOCATION WRITING
    if (rc.getRoundNum() == 1) {
      int width = rc.getMapWidth();
      int height = rc.getMapHeight();
      MapLocation myLoc = rc.getLocation();
      Comms.addScoutingTarget(rc, new MapLocation(myLoc.x, height - myLoc.y - 1));
      Comms.addScoutingTarget(rc, new MapLocation(width - myLoc.x - 1, height - myLoc.y - 1));
      Comms.addScoutingTarget(rc, new MapLocation(width - myLoc.x - 1, myLoc.y));
    }

    // PRE SYNC
    // determine mode to be put in
    int mode = 0;
    if (RobotPlayer.visible_enemy_robots.length > 0) {
      mode = 1;
    }
    if (RobotPlayer.currentMode == RobotMode.PORTABLE) {
      mode = 2;
    }

    // SYNC AND GET ORDER
    RobotType order = MyArchonComms.MyArchonSyncAndBuild(rc, mode);

    MyArchonObject pocket = MyArchonObject.PocketArchon(rc, MyArchonComms.GetArchons(rc));
    Boolean can_see_pocket_archon = false; // bad name
    for (RobotInfo ally : RobotPlayer.ally_archons_within_5) {
      if (ally.ID == pocket.id) {
        can_see_pocket_archon = true;
        break;
      }
    }

    Boolean is_land_above_average = RobotPlayer.currentRubbleUnderfoot < localRubble(rc, 10);

    // MODE SPECIFIC LOGIC
    if (RobotPlayer.currentMode == RobotMode.PORTABLE) {
      // FIND THE NEAREST NON AWFUL TILE WITHIN ACTION RANGE OF POCKET ARCHON
      // MyArchonObject pocket = MyArchonObject.PocketArchon(rc,
      // MyArchonComms.GetArchons(rc));
      if (pocket.id == RobotPlayer.my_id || (can_see_pocket_archon)) {
        Transform(rc);
      } else {
        Pathfinder.smartMove(rc, pocket.location, 5, 1);
      }

    } else {
      // TODO: relocate to lower rubble tile

      // FURY
      if (Helpers.daysUntilAnomaly(rc, AnomalyType.FURY, 0)) {
        rc.setIndicatorString("Preparing for fury");
        if (rc.canTransform() && RobotPlayer.currentMode == RobotMode.TURRET) {
          rc.transform();
        }
      }

      if (!IsAreaSaturated(rc) && miners_i_built <= 3) {
        buildTowardsLowRubble(rc, RobotType.MINER);
        miners_i_built += 3;
      } else {
        // BUILD UNITS
        if (order != null) {
          rc.setIndicatorString(order.toString());
          if (order == RobotType.BUILDER) {
            if (RobotPlayer.visible_ally_builders.size() == 0) {
              buildTowardsLowRubble(rc, order);
            }
          } else {
            buildTowardsLowRubble(rc, order);
          }
        } else {
          // IF THEY ARE NOT CLOSE TO ANOTHER ARCHON AND THEY ARE NOT THE POCKET ARCHON,
          // MAKE THEM RELOCATE TOWARDS THE POCKET ARCHON
          // MyArchonObject pocket = MyArchonObject.PocketArchon(rc,
          // MyArchonComms.GetArchons(rc));

          if (!(RobotPlayer.my_id == pocket.id) && !can_see_pocket_archon) {
            Transform(rc);
          }
        }
      }

      // REPAIR
      // if (RobotPlayer.visible_enemy_robots.length > 0) {
      if (Helpers.findInjuredDroid(rc) != null) {
        RobotInfo repair_ally = lowestHpDamagedAllyDroid(rc);
        while (repair_ally != null
            && repair_ally.getHealth() < repair_ally.getType().getMaxHealth(repair_ally.getLevel())) {
          if (rc.isActionReady()) {
            repair(rc, repair_ally);
            repair_ally = lowestHpDamagedAllyDroid(rc);
          } else {
            break;
          }
        }
      }
      // } else {
      // if (Helpers.findInjuredDroid(rc) != null) {
      // RobotInfo repair_ally = highestHpDamagedAllyDroid(rc);
      // while (repair_ally != null && repair_ally.getHealth() <
      // repair_ally.getType().getMaxHealth(repair_ally.getLevel()) ) {
      // if (rc.isActionReady()) {
      // repair(rc, repair_ally);
      // repair_ally = highestHpDamagedAllyDroid(rc);
      // } else {
      // break;
      // }
      // }
      // }
      // }
    }
  }

  private static RobotInfo highestHpDamagedAllyDroid(RobotController rc) {
    try {
      RobotInfo ally = null; // self
      for (RobotInfo robot : rc.senseNearbyRobots(rc.getLocation(), rc.getType().actionRadiusSquared, rc.getTeam())) { // allies
        int rob_hp = robot.getHealth();
        int ally_hp = (ally != null ? ally.getHealth() : 10000);
        if (rob_hp > ally_hp && rob_hp < robot.getType().getMaxHealth(1) && !robot.getType().isBuilding()) {
          ally = robot;
        }
      }
      return ally;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static RobotInfo lowestHpDamagedAllyDroid(RobotController rc) {
    try {
      RobotInfo ally = null; // self
      for (RobotInfo robot : rc.senseNearbyRobots(rc.getLocation(), rc.getType().actionRadiusSquared, rc.getTeam())) { // allies
        int rob_hp = robot.getHealth();
        int ally_hp = (ally != null ? ally.getHealth() : 10000);
        if (rob_hp < ally_hp && rob_hp < robot.getType().getMaxHealth(1) && !robot.getType().isBuilding()) {
          ally = robot;
        }
      }
      return ally;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static void repair(RobotController rc, RobotInfo lowest_hp_ally) {
    try {
      if (rc.canRepair(lowest_hp_ally.getLocation())) {
        rc.repair(lowest_hp_ally.getLocation());
        rc.setIndicatorString("repairing");
      } else {
        Helpers.Log(rc, "cannot repair this ally" + lowest_hp_ally.getLocation().toString());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static void buildTowardsLowRubble(RobotController rc, RobotType type) throws GameActionException {
    // get possible building directions
    Direction[] dirs = Direction.allDirections();
    // sort by rubble level
    Arrays.sort(dirs, (a, b) -> getRubble(rc, a) - getRubble(rc, b));
    for (Direction d : dirs) {
      if (rc.canBuildRobot(type, d)) {
        rc.buildRobot(type, d);
      }
    }
  }

  static int getRubble(RobotController rc, Direction d) {
    try {
      MapLocation loc = rc.getLocation().add(d);
      return rc.senseRubble(loc);
    } catch (GameActionException e) {
      e.printStackTrace();
      return 0;
    }
  }
}
