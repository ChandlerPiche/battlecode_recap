package chanbot.ExtendedRobotController;

import battlecode.common.*;
import chanbot.Helpers;
import chanbot.RobotPlayer;

import java.util.ArrayList;

/*
  movement either uses direct approach or bug2 circumnavigation. smart move is the core of the pathfinding logic.
*/

class NextMoveResult {
  String mode;
  Direction dir;
  int annoyance_tolerance; // set this to total cd required to get to target using approach
  Boolean is_clockwise;

  NextMoveResult(String mode, Direction dir, int annoyance_tolerance, Boolean is_clockwise) {
    this.mode = mode;
    this.dir = dir;
    this.annoyance_tolerance = annoyance_tolerance;
    this.is_clockwise = is_clockwise;
  }
}

class StepOutcome {

  MapLocation loc;
  int cd;

  StepOutcome(RobotController rc, MapLocation loc, int departed_tile_rubble) {
    this.loc = loc;
    cd = (10 + departed_tile_rubble) / 10 * rc.getType().movementCooldown;
  }
}

public class Pathfinder {

  static RobotController rc;
  static String actual_mode = "APPROACH";
  static Direction actual_last_move_dir;
  static MapLocation actual_last_goal;
  static MapLocation start;
  static Boolean actual_last_clock = true;
  static int annoyance = 0;
  static int annoyance_tolerance;
  static double annoyance_range;
  static int actual_rounds_in_around = 0;
  static int actual_moves_in_around = 0;
  static int problem_rubble = 0;
  static ArrayList<MapLocation> locations_this_goal = new ArrayList<>();
  static Boolean long_around = false;
  static Boolean long_around_clock = true;

  // 33
  static int getPathCd(RobotController rc, ArrayList<StepOutcome> steps, int loops, double multiplier,
      MapLocation scoring_tile, int rubble_avoidance) {
    int total_cd = 0;
    try {
      int last_tile_rubble = RobotPlayer.currentRubbleUnderfoot;
      MapLocation last_tile_location = RobotPlayer.currentLocation;

      for (int i = 0; i < steps.size(); i++) {
        if (steps.get(i) == null) {

        } else {
          total_cd += steps.get(i).cd;
          last_tile_location = steps.get(i).loc;
          Helpers.Log(rc, "loc:" + last_tile_location, 11978);
          last_tile_rubble = rc.senseRubble(last_tile_location);
        }
      }

      total_cd += (10 + rubble_avoidance * last_tile_rubble) / 10 * rc.getType().movementCooldown * multiplier
          * Math.sqrt(last_tile_location.distanceSquaredTo(scoring_tile));
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      return total_cd;
    }
  }

  static int scoreOutcome(RobotController rc, ArrayList<StepOutcome> steps, double beyond_consideration_multiplier,
      MapLocation target, int loops, MapLocation goal, int rubble_avoidance) {
    // MapLocation scoring_tile = target;
    // scoring_tile = scoring_tile.add(scoring_tile.directionTo(goal));
    // scoring_tile = scoring_tile.add(scoring_tile.directionTo(goal));
    // scoring_tile = scoring_tile.add(scoring_tile.directionTo(goal));
    rc.setIndicatorDot(target, 155, 155, 0);
    int total_score = 0;
    try {
      total_score += getPathCd(rc, steps, loops, beyond_consideration_multiplier, target, rubble_avoidance);
      return total_score;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      return total_score;
    }
  }

  /**
   *
   * @param rc
   * @param goal             the tile to try to move to
   * @param loops            (how many moves into the future to look)
   * @param rubble_avoidance (higher numbers are slower but avoid rubble better)
   */
  static public void smartMove(RobotController rc, MapLocation goal, int loops, int rubble_avoidance) {
    try {
      Boolean is_movement_ready = rc.isMovementReady();

      // RESET SOME FIELDS ON NEW GOAL
      if (!goal.equals(actual_last_goal) && is_movement_ready) {
        actual_mode = "APPROACH";
        // actual_last_move_dir = null;
        actual_last_goal = goal;
        start = RobotPlayer.currentLocation;
        locations_this_goal = new ArrayList<>();
      }

      locations_this_goal.add(RobotPlayer.currentLocation);

      if (actual_mode == "APPROACH") {
        annoyance = 0;
        actual_rounds_in_around = 0;
      } else if (actual_mode == "AROUND") {
        annoyance += 10;
        actual_rounds_in_around++;
      }

      if (is_movement_ready) {
        // GET NEXT MOVE
        NextMoveResult next_move;
        next_move = decideNextMove(rc, goal, loops, rubble_avoidance);

        // enter long around mode
        if (long_around != true && locations_this_goal.contains(RobotPlayer.currentLocation.add(next_move.dir))) {
          long_around = true;
          annoyance_range = (int) (Math.sqrt(RobotPlayer.currentLocation.distanceSquaredTo(goal)));
          long_around_clock = next_move.is_clockwise;
          // ENTER SOME SOME PERSISTENT CIRCUMNAVIGATE MODE UNTIL

        }

        // long around mode
        if (long_around == true) {
          if (annoyance > annoyance_tolerance) {
            if (Math.sqrt(RobotPlayer.currentLocation.distanceSquaredTo(goal)) < annoyance_range) {
              long_around = false;
              // rc.setIndicatorString("mode reset");
            } else {
              next_move = new NextMoveResult("APPROACH", approachNextMove(rc, RobotPlayer.currentLocation, goal), 69,
                  true);
              if (next_move == null || next_move.dir == Direction.CENTER || next_move.dir == null || Math
                  .sqrt(RobotPlayer.currentLocation.add(next_move.dir).distanceSquaredTo(goal)) < annoyance_range) {
                long_around = false;
              }
            }
          } else {
            next_move = new NextMoveResult("AROUND", aroundNextMove(rc, RobotPlayer.currentLocation, goal,
                long_around_clock, problem_rubble - 1, 69, actual_last_move_dir, goal, annoyance > 0), 69,
                long_around_clock);
            actual_last_move_dir = next_move.dir;
          }
        }

        if (next_move == null || next_move.dir == Direction.CENTER || next_move.dir == null) {
        } else {

          // movement
          if (rc.canMove(next_move.dir)) {
            if (rubble_avoidance > 1) {
              rubble_avoiding_movement_3(next_move.dir);
            } else {
              rc.move(next_move.dir);
            }
            RobotPlayer.updateLocationDynamics(rc);
          }
          // goal
          // rc.setIndicatorLine(RobotPlayer.currentLocation, goal, 0, 255, 0);
        }

        // PRE MOVE
        actual_last_clock = next_move.is_clockwise;

        // ENTERING AROUND
        if (actual_mode == "APPROACH" && next_move.mode == "AROUND") {

          // annoyance_range = (int)
          // Math.round(goal.distanceSquaredTo(RobotPlayer.currentLocation)) - 1;
          annoyance_tolerance = next_move.annoyance_tolerance;
          actual_moves_in_around++;
        }

        if (actual_mode == "AROUND" && next_move.mode == "APPROACH") {
          problem_rubble = 0;
        }

        // UPDATE STATIC VARIABLES
        actual_mode = next_move.mode;
        actual_last_move_dir = next_move.dir;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Performs movement in the direction, or one of the adjacent ones if the rubble
   * there is lower.
   * 
   * @param dir (must be one you can move in)
   */
  private static void rubble_avoiding_movement_3(Direction dir) throws GameActionException {
    MapLocation forward = RobotPlayer.currentLocation.add(dir);
    MapLocation left = RobotPlayer.currentLocation.add(dir.rotateLeft());
    MapLocation right = RobotPlayer.currentLocation.add(dir.rotateRight());

    int left_rubble = Integer.MAX_VALUE;
    int forward_rubble = rc.senseRubble(forward);
    int right_rubble = Integer.MAX_VALUE;

    if (rc.canSenseLocation(left)) {
      left_rubble = rc.senseRubble(left);
    }
    if (rc.canSenseLocation(right)) {
      right_rubble = rc.senseRubble(right);
    }

    if (rc.isMovementReady()) {
      if (left_rubble < forward_rubble && left_rubble < right_rubble) {
        rc.move(dir.rotateLeft());
      } else if (right_rubble < left_rubble && right_rubble < forward_rubble) {
        rc.move(dir.rotateLeft());
      } else {
        rc.move(dir);
      }
    }
  }

  static NextMoveResult decideNextMove(RobotController rc, MapLocation goal, int loops, int rubble_avoidance) {
    NextMoveResult result = null;
    try {
      MapLocation current_location = RobotPlayer.currentLocation;
      MapLocation target = goal;
      if (!rc.canSenseLocation(goal)) {
        target = current_location
            .add(target.directionTo(target.add(target.directionTo(target.add(target.directionTo(goal)))))); // bytecode
                                                                                                            // shortcut
        MapLocation next = target.add(target.directionTo(goal));
        while (rc.canSenseLocation(next)) {
          target = next;
          next = target.add(target.directionTo(goal));
        }
      }
      Direction ANM = approachNextMove(rc, current_location, target);
      if (ANM == null) {
        result = new NextMoveResult("APPROACH", Direction.CENTER, 69, true);
      } else {
        MapLocation approach_tile = RobotPlayer.currentLocation.add(ANM);
        int approach_rubble = rc.senseRubble(approach_tile);
        if (approach_rubble > problem_rubble) {
          problem_rubble = approach_rubble;
        }

        if (current_location.equals(goal)) {
          result = new NextMoveResult("APPROACH", Direction.CENTER, 69, true);
        } else {
          if (approach_rubble > RobotPlayer.currentRubbleUnderfoot) { // DECISION REQUIRED
            ArrayList<StepOutcome> HARD_LEFT = new ArrayList<>();
            ArrayList<StepOutcome> SOFT_LEFT = aroundGetOutcome(true, approach_rubble - 1, rc, current_location, target,
                loops, actual_moves_in_around, goal);
            ArrayList<StepOutcome> FORWARD = approachGetOutcome(rc, current_location, target, loops);
            ArrayList<StepOutcome> SOFT_RIGHT = aroundGetOutcome(false, approach_rubble - 1, rc, current_location,
                target, loops, actual_moves_in_around, goal);
            ArrayList<StepOutcome> HARD_RIGHT = new ArrayList<>();
            ;

            int HARD_LEFT_SCORE = 10000;
            int SOFT_LEFT_SCORE;
            int FORWARD_SCORE;
            int SOFT_RIGHT_SCORE;
            int HARD_RIGHT_SCORE = 10000;
            int bytecode_remaining = Clock.getBytecodesLeft();

            if (SOFT_LEFT.size() == loops
                && rc.senseRubble(SOFT_LEFT.get(loops - 1).loc) > RobotPlayer.currentRubbleUnderfoot) { // CONSIDER HARD
                                                                                                        // LEFT
              HARD_LEFT = bytecode_remaining > 2500
                  ? aroundGetOutcome(true, RobotPlayer.currentRubbleUnderfoot, rc, current_location, target, loops,
                      actual_moves_in_around, goal)
                  : aroundGetOutcome(true, RobotPlayer.currentRubbleUnderfoot, rc, current_location, target, loops - 1,
                      actual_moves_in_around, goal);
              HARD_LEFT_SCORE = scoreOutcome(rc, HARD_LEFT, 1, target, loops, goal, rubble_avoidance);
            }
            if (SOFT_RIGHT.size() == loops
                && rc.senseRubble(SOFT_RIGHT.get(loops - 1).loc) > RobotPlayer.currentRubbleUnderfoot) { // CONSIDER
                                                                                                         // HARD RIGHT
              HARD_RIGHT = bytecode_remaining > 2500
                  ? aroundGetOutcome(false, RobotPlayer.currentRubbleUnderfoot, rc, current_location, target, loops,
                      actual_moves_in_around, goal)
                  : aroundGetOutcome(false, RobotPlayer.currentRubbleUnderfoot, rc, current_location, target, loops - 1,
                      actual_moves_in_around, goal);
              HARD_RIGHT_SCORE = scoreOutcome(rc, HARD_RIGHT, 1, target, loops, goal, rubble_avoidance);
            }

            // Different final tile weight
            SOFT_LEFT_SCORE = scoreOutcome(rc, SOFT_LEFT, 1, target, loops, goal, rubble_avoidance);
            FORWARD_SCORE = scoreOutcome(rc, FORWARD, 1, target, loops, goal, rubble_avoidance);
            SOFT_RIGHT_SCORE = scoreOutcome(rc, SOFT_RIGHT, 1, target, loops, goal, rubble_avoidance);

            // IF THE RUBBLE ON THE LOCATION IN THE FIRST DIRECTION IN SOFT LEFT IS LESS
            // THAN OR EQUAL TO UNDERFOOT then SKIP HARD LEFT

            Helpers.Log(rc, "FULLDEC:" + HARD_LEFT_SCORE + "|" + SOFT_LEFT_SCORE + "|" + FORWARD_SCORE + "|"
                + SOFT_RIGHT_SCORE + "|" + HARD_RIGHT_SCORE, 11978);

            if (HARD_LEFT_SCORE <= SOFT_LEFT_SCORE && HARD_LEFT_SCORE <= HARD_RIGHT_SCORE
                && HARD_LEFT_SCORE <= SOFT_RIGHT_SCORE && HARD_LEFT_SCORE <= FORWARD_SCORE && HARD_LEFT.size() > 0) {
              result = new NextMoveResult("AROUND", rc.getLocation().directionTo(HARD_LEFT.get(0).loc), FORWARD_SCORE,
                  true);
            } else if (SOFT_LEFT_SCORE <= FORWARD_SCORE && SOFT_LEFT_SCORE <= SOFT_RIGHT_SCORE
                && SOFT_LEFT_SCORE <= HARD_RIGHT_SCORE && SOFT_LEFT.size() > 0) {
              result = new NextMoveResult("AROUND", rc.getLocation().directionTo(SOFT_LEFT.get(0).loc), FORWARD_SCORE,
                  true);
            } else if (FORWARD_SCORE <= SOFT_RIGHT_SCORE && FORWARD_SCORE <= HARD_RIGHT_SCORE) {
              result = new NextMoveResult("APPROACH", rc.getLocation().directionTo(FORWARD.get(0).loc), FORWARD_SCORE,
                  true);
            } else if (SOFT_RIGHT_SCORE <= HARD_RIGHT_SCORE && SOFT_RIGHT.size() > 0) {
              result = new NextMoveResult("AROUND", rc.getLocation().directionTo(SOFT_RIGHT.get(0).loc), FORWARD_SCORE,
                  false);
            } else if (HARD_RIGHT.size() > 0) {
              result = new NextMoveResult("AROUND", rc.getLocation().directionTo(HARD_RIGHT.get(0).loc), FORWARD_SCORE,
                  false);
            } else {
              result = new NextMoveResult("APPROACH", rc.getLocation().directionTo(FORWARD.get(0).loc), FORWARD_SCORE,
                  true);
            }
          } else { // NO DECISION REQUIRED
            result = new NextMoveResult("APPROACH", approachNextMove(rc, current_location, target), 69, true);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {

      return result;
    }
  }

  // can return any of 9 directions
  private static Direction approachNextMove(RobotController rc, MapLocation location, MapLocation target) {
    if (location.equals(target)) {
      return Direction.CENTER;
    } else {
      Direction result = null;

      Direction most_direct = location.directionTo(target);
      if (rc.canMove(most_direct)) {
        result = most_direct;
      } else {
        if (rc.canMove(most_direct.rotateLeft())) {
          result = most_direct.rotateLeft();
        } else if (rc.canMove(most_direct.rotateRight())) {
          result = most_direct.rotateRight();
        } else if (rc.canMove(most_direct.rotateLeft().rotateLeft())) {
          result = most_direct.rotateLeft().rotateLeft();
        } else if (rc.canMove(most_direct.rotateRight().rotateRight())) {
          result = most_direct.rotateRight().rotateRight();
        } else if (rc.canMove(most_direct.rotateRight().rotateRight().rotateRight())) {
          result = most_direct.rotateRight().rotateRight().rotateRight();
        } else if (rc.canMove(most_direct.rotateLeft().rotateLeft().rotateLeft())) {
          result = most_direct.rotateLeft().rotateLeft().rotateLeft();

        }
      }
      return result;
    }
  }

  private static ArrayList<StepOutcome> approachGetOutcome(RobotController rc, MapLocation location, MapLocation target,
      int loops) {
    ArrayList<StepOutcome> SOarray = new ArrayList<>();
    try {
      MapLocation sim_loc = location; // this should be current?
      for (int i = 0; i < loops; i++) {
        Direction next_move = approachNextMove(rc, sim_loc, target);
        if (next_move == null) {
          break;
        }
        MapLocation next_loc = sim_loc.add(next_move);
        if (sim_loc.equals(target) || !rc.canSenseLocation(next_loc)) { // STOP IF SIM_LOC IS THE TARGET OR NEXT_LOC IS
                                                                        // NOT SENSABLE
          return SOarray;
        } else {
          SOarray.add(new StepOutcome(rc, next_loc, rc.senseRubble(sim_loc)));
          sim_loc = next_loc;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      return SOarray;
    }

  }

  public Pathfinder(RobotController rc) {
    this.rc = rc;
  }

  // [90, 315] -> [96, 264]
  private static Direction aroundNextMove(RobotController rc, MapLocation location, MapLocation target,
      Boolean is_clockwise, int max_rubble, int moves_in_around, Direction last_move_dir, MapLocation goal,
      Boolean use_last_move) {
    Direction choice = null;
    try {

      if (use_last_move && last_move_dir != Direction.CENTER && last_move_dir != null) {
        choice = is_clockwise ? last_move_dir.rotateRight().rotateRight() : last_move_dir.rotateLeft().rotateLeft();
      } else {
        choice = RobotPlayer.currentLocation.directionTo(goal);
      }
      MapLocation choice_loc = null;

      // TODO: break occurs here
      // straighten until valid tile found
      int max_spins = 4;
      int spin_counter = 0;
      while (true) {
        choice_loc = location.add(choice);
        if (rc.canSenseLocation(location.add(choice))) {
          if (rc.senseRubble(choice_loc) <= max_rubble && !rc.isLocationOccupied(choice_loc)) {
            break;
          } else {
            choice = is_clockwise ? choice.rotateLeft() : choice.rotateRight();
          }

        } else {
          if (rc.onTheMap(choice_loc)) {
            System.out.println("pathing bug 4 chan?");
            choice = null;
            break;
          } else {
            choice = is_clockwise ? choice.rotateLeft() : choice.rotateRight();
          }
        }
        spin_counter++;
        if (spin_counter >= max_spins) {
          choice = approachNextMove(rc, location, target);
          break;
        }
      }
    } catch (Exception e) {

      e.printStackTrace();
    } finally {
      return choice;
    }
  }

  private static ArrayList<StepOutcome> aroundGetOutcome(Boolean clockwise, int max_rubble, RobotController rc,
      MapLocation location, MapLocation target, int loops, int moves_in_around, MapLocation goal) {
    ArrayList<StepOutcome> SOarray = new ArrayList<StepOutcome>();
    try {
      if (Clock.getBytecodesLeft() < 1000) {
        System.out.println("WARN: pathing bytecode");
      }
      MapLocation sim_loc = location;
      Direction last_move_dir = actual_last_move_dir;

      for (int i = 0; i < loops; i++) {

        Direction next = aroundNextMove(rc, sim_loc, target, clockwise, max_rubble, moves_in_around + i, last_move_dir,
            goal, i != 0);

        if (next == null || sim_loc.equals(target)) { // STOP IF SIM_LOC IS THE TARGET OR NEXT_LOC IS NOT SENSABLE
          // aroundNextMove(rc, )
          break;
        } else {
          MapLocation next_loc = sim_loc.add(next);
          if (next_loc == null || !rc.canSenseLocation(next_loc)) {
            break;
          }

          StepOutcome so = new StepOutcome(rc, next_loc, rc.senseRubble(sim_loc));
          SOarray.add(so);
          last_move_dir = sim_loc.directionTo(so.loc);

          sim_loc = next_loc;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      return SOarray;
    }
  }
}
