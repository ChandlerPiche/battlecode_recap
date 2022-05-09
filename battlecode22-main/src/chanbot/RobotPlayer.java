package chanbot;

import battlecode.common.*;
import chanbot.Comms.MyArchonObject;
import chanbot.Comms.ThreatComms;
import chanbot.ExtendedRobotController.ExtendedRobotController;
import chanbot.RobotStrategies.*;

import java.util.ArrayList;
import java.util.Random;

// THESE ARE ACCESSIBLE EVERYWHERE
public strictfp class RobotPlayer {

  // STRATEGY CONFIG
  static int suicideTurn = 10000;
  static Boolean dev = false;
  static public final Random rng = new Random(6147);
  static boolean make_builders = false;

  // STATIC VARIABLES
  static public Direction[] directions = Direction.cardinalDirections();
  public static String strategy = "SOLDIER RUSH"; // "TECH"
  public static int action_radius_squared;
  public static Team my_team;
  public static Team my_opponent;
  public static int my_id;
  public static MapLocation spawn_loc;
  static public Random my_random;
  public static RobotType my_type;

  private static void updateStaticUsefulVariables(RobotController rc) {
    my_team = rc.getTeam();
    my_opponent = my_team.opponent();
    my_type = rc.getType();
    action_radius_squared = my_type.actionRadiusSquared;
    my_id = rc.getID();
    spawn_loc = rc.getLocation();
    my_random = new Random(rc.getID());
    if (dev && my_team == Team.B) {
      strategy = "SOLDIER RUSH";
    }
  }

  // DYNAMIC VARIABLES (ROUND & HP BASED)
  static public int roundsAlive = 0;
  static public int round_number; // arbitrary
  static MapLocation first_seen_ally_archon = null;
  static Boolean return_for_repair = false;
  public static int my_level = 0;
  public static int my_damage;

  private static void updateOtherDynamics(RobotController rc) {
    if (round_number == suicideTurn) {
      rc.resign();
    }

    // try to record an ally archon location in memory if possible
    if (first_seen_ally_archon == null) {
      RobotInfo[] nearby_robots = rc.senseNearbyRobots();
      for (RobotInfo rob : nearby_robots) {
        if (rob.type == RobotType.ARCHON && rob.getTeam() == rc.getTeam()) {
          Helpers.Log(rc, "spotted! " + rob.getLocation().toString() + Clock.getBytecodesLeft(), 13958, 89);
          first_seen_ally_archon = rob.getLocation();
        }
      }
    }

    // UPDATE EVERY TURN
    if (my_level < rc.getLevel()) {
      my_level = rc.getLevel();
      my_damage = my_type.getDamage(my_level);
    }

    round_number = rc.getRoundNum();
    currentMode = rc.getMode();
    roundsAlive++;
  }

  // DYNAMIC VARIABLES (LOCATION BASED)
  static public MapLocation currentLocation;
  static public int currentRubbleUnderfoot;
  static public RobotMode currentMode;
  public static RobotInfo[] actionable_enemy_robots;
  public static RobotInfo[] actionable_ally_robots;
  public static ArrayList<RobotInfo> ally_archons_within_5;
  public static ArrayList<RobotInfo> actionable_ally_miners;
  public static RobotInfo[] visible_enemy_robots;
  public static RobotInfo[] visible_ally_robots;
  public static ArrayList<RobotInfo> visible_ally_miners;
  public static ArrayList<RobotInfo> visible_ally_labs;
  public static ArrayList<RobotInfo> visible_ally_builders;
  public static ArrayList<RobotInfo> visible_wounded_ally_droids;
  public static Boolean is_threats_full = false;

  public static void updateLocationDynamics(RobotController rc) throws GameActionException {
    // SET LOCATION BEFORE
    currentLocation = rc.getLocation();
    actionable_enemy_robots = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, RobotPlayer.my_opponent);
    actionable_ally_robots = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, RobotPlayer.my_team);
    actionable_ally_miners = new ArrayList<>();
    for (RobotInfo rob : actionable_ally_robots) {
      if (rob.type == RobotType.MINER) {
        actionable_ally_miners.add(rob);
      }
    }
    visible_enemy_robots = rc.senseNearbyRobots(RobotPlayer.my_type.visionRadiusSquared, RobotPlayer.my_opponent);
    visible_ally_robots = rc.senseNearbyRobots(RobotPlayer.my_type.visionRadiusSquared, RobotPlayer.my_team);
    visible_wounded_ally_droids = new ArrayList<>();
    visible_ally_miners = new ArrayList<>();
    visible_ally_labs = new ArrayList<>();
    visible_ally_builders = new ArrayList<>();
    ally_archons_within_5 = new ArrayList<>();
    for (RobotInfo rob : visible_ally_robots) {
      if (rob.type == RobotType.MINER) {
        visible_ally_miners.add(rob);
      } else if (rob.type == RobotType.LABORATORY) {
        visible_ally_labs.add(rob);
      } else if (rob.type == RobotType.BUILDER) {
        visible_ally_builders.add(rob);
      } else if (rob.type == RobotType.ARCHON) {
        if (rob.location.isWithinDistanceSquared(RobotPlayer.currentLocation, 5)) {
          ally_archons_within_5.add(rob);
        }
      }
      if (rob.health < rob.type.getMaxHealth(1)) {
        visible_wounded_ally_droids.add(rob);
      }
    }
    currentRubbleUnderfoot = rc.senseRubble(currentLocation);
  }

  // SHARED ROBOT ACTIONS
  private static void updateDynamicUsefulVariables(RobotController rc) {
    try {
      updateOtherDynamics(rc);
      updateLocationDynamics(rc);
    } catch (Exception e) {
      System.out.println("VAR UPDATE EXCEPTION");
      e.printStackTrace();
    }
  }

  private static void getRepaired(RobotController rc) throws GameActionException {
    // RETURN FOR REPAIR CHUNK
    if (((double) rc.getHealth() / (double) rc.getType().getMaxHealth(rc.getLevel())) < .5
        && !rc.getType().isBuilding()) {
      return_for_repair = true;
    }
    if (return_for_repair == true && rc.isMovementReady() && first_seen_ally_archon != null) {
      try {
        ExtendedRobotController.scoutTowardPoint(rc, first_seen_ally_archon, 2);
      } catch (Exception e) {
        System.out.println("CONCEALED ERROR: traffic?");
      }
      if (rc.getHealth() > rc.getType().getMaxHealth(rc.getLevel()) - 2) {
        return_for_repair = false;
      }
      if ((double) rc.getHealth() / (double) rc.getType().getMaxHealth(rc.getLevel()) > .5
          && RobotPlayer.visible_wounded_ally_droids.size() > 3) {
        return_for_repair = false;
      }
    }

    // SET LOCATIONS AFTER
    currentLocation = rc.getLocation();
    actionable_enemy_robots = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, RobotPlayer.my_opponent);
    actionable_ally_robots = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, RobotPlayer.my_team);
    currentRubbleUnderfoot = rc.senseRubble(currentLocation);
  }

  public static void ReportThreats(RobotController rc) {
    for (RobotInfo enemy : RobotPlayer.visible_enemy_robots) {
      if (MyArchonObject.InPocket(rc, enemy.location)) {
        ThreatComms.WriteThreat(rc, enemy.location);
      } else {
        rc.setIndicatorDot(enemy.location, 100, 255, 0);
      }
    }
  }

  // THIS IS CALLED BY GAME ENGINE
  public static void run(RobotController rc) throws GameActionException {
    // on robot creation
    try {
      updateStaticUsefulVariables(rc);
    } catch (Exception e) {
      System.out.println("ERROR: static vars not set");
      e.printStackTrace();
    }

    // each loop is 1 robot turn
    while (true) {
      try {
        is_threats_full = false;
        updateDynamicUsefulVariables(rc);
        ReportThreats(rc);
        getRepaired(rc);

        // ROBOT TYPE SPECIFIC ACTIONS
        switch (my_type) {
          case ARCHON:
            Archon.runArchon(rc, make_builders);
            break;
          case MINER:
            MinerStrategy.runMiner(rc);
            break;
          case SOLDIER:
            SoldierStrategy.runSoldier(rc);
            break;
          case LABORATORY:
            Laboratory.runLaboratory(rc);
            break;
          case WATCHTOWER:
            WatchtowerStrategy.runWatchtower(rc);
          case BUILDER:
            Builder.runBuilder(rc);
            break;
          case SAGE:
            Sage.runSage(rc);
            break;
        }
      } catch (GameActionException e) {
        System.out.println(rc.getType() + " GA EXCEPTION");
        e.printStackTrace();
      } catch (Exception e) {
        System.out.println(rc.getType() + " EXCEPTION");
        e.printStackTrace();
      } finally {
        Clock.yield();
      }
    }
  }
}
