package chanbot.Comms;

import battlecode.common.*;
import chanbot.Helpers;
import chanbot.RobotPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class MyArchonComms {

  // ARCHON 1 VARIABLES
  static private int totalSoldiersInBuildplan = 0;
  static private int totalMinersInBuildplan = 0;

  private static void WriteBuildPlan(RobotController rc, String strategy) {
    try {
      int archons_alive = rc.getArchonCount();
      int stored_archons_alive = StoredArchonsAlive(rc);
      if (archons_alive == stored_archons_alive) { // WRITE BUILD PLAN
        int gold_stash = rc.getTeamGoldAmount(RobotPlayer.my_team);
        int lead_stash = rc.getTeamLeadAmount(RobotPlayer.my_team);
        int def_position = 69;

        String mode = "CHILL";
        int available_buildings = archons_alive;

        ArrayList<MyArchonObject> my_arcs = GetArchons(rc);
        for (MyArchonObject arc : my_arcs) {
          if (arc.status == 2) {
            available_buildings -= 1;
          } else if (arc.status == 1) {
            Helpers.Log(rc, "ALLY IS IN DEFEND MODE");
            mode = "DEFEND";
            def_position = arc.position;
          }
        }

        int available_snapshot = available_buildings;

        if (mode == "CHILL") {
          int build_soldiers = 0;
          int build_miners = 0;
          int build_sages = 0;
          int build_builders = 0;
          // TODO: build miners if there is much reported lead
          // ELSE: (will do 80 20 for now)
          while (lead_stash >= 75 && available_buildings > 0) {
            int NG = new Random(RobotPlayer.round_number).nextInt(10);

            // SHARED EARLY GAME BUILD
            if (totalMinersInBuildplan < (4 - rc.getArchonCount())) {
              lead_stash -= RobotType.MINER.buildCostLead;
              totalMinersInBuildplan++;
              build_miners++;
            } else if (totalSoldiersInBuildplan < 6) {
              if (NG == 8 || NG == 9) {
                lead_stash -= RobotType.MINER.buildCostLead;
                totalMinersInBuildplan++;
                build_miners++;
              } else {
                lead_stash -= RobotType.SOLDIER.buildCostLead;
                totalSoldiersInBuildplan++;
                build_soldiers++;
              }

              // LATE GAME BUILDS

            } else if (strategy == "SOLDIER RUSH") {
              if (NG == 8 || NG == 9) {
                lead_stash -= RobotType.MINER.buildCostLead;
                totalMinersInBuildplan++;
                build_miners++;
              } else {
                lead_stash -= RobotType.SOLDIER.buildCostLead;
                totalSoldiersInBuildplan++;
                build_soldiers++;
              }
            } else if (strategy == "TECH") {
              if (RobotPlayer.round_number >= 100) {
                // leave a decent stack to builders...
                if (gold_stash >= 20) {
                  build_sages++;
                  gold_stash -= 20;
                }
                if (lead_stash >= (180 + RobotType.BUILDER.buildCostLead + RobotType.SOLDIER.buildCostLead)) {
                  if (totalMinersInBuildplan >= 100) {
                    if (NG == 9) {
                      lead_stash -= RobotType.MINER.buildCostLead;
                      totalMinersInBuildplan++;
                      build_miners++;
                    } else {
                      lead_stash -= RobotType.SOLDIER.buildCostLead;
                      totalSoldiersInBuildplan++;
                      build_soldiers++;
                    }
                  } else {
                    if (NG == 7 | NG == 8 || NG == 9) {
                      lead_stash -= RobotType.MINER.buildCostLead;
                      totalMinersInBuildplan++;
                      build_miners++;
                    } else {
                      lead_stash -= RobotType.SOLDIER.buildCostLead;
                      totalSoldiersInBuildplan++;
                      build_soldiers++;
                    }
                  }

                }
                if (lead_stash >= (180 + RobotType.BUILDER.buildCostLead)) {
                  build_builders += 1;
                  lead_stash -= RobotType.BUILDER.buildCostLead;
                }
              } else {
                if (NG == 8 || NG == 9) {
                  lead_stash -= RobotType.MINER.buildCostLead;
                  totalMinersInBuildplan++;
                  build_miners++;
                } else {
                  lead_stash -= RobotType.SOLDIER.buildCostLead;
                  totalSoldiersInBuildplan++;
                  build_soldiers++;
                }
              }

              // TODO: add tech specific logic here
            } else if (strategy == "ONLY DEFENCE") {

            }
            available_buildings--;
          }

          int build_plan_int = 0;

          Collections.shuffle(my_arcs);
          MyArchonObject pocket = MyArchonObject.PocketArchon(rc, my_arcs);

          for (MyArchonObject arc : my_arcs) {
            if (arc.alive && arc.status != 2) { // not travelling

              // IF IT IS THE POCKET ARCHON, it SHOULD BUILD THE BUILDER
              if (build_sages > 0) {
                build_plan_int += 1 * Math.pow(10, arc.position);
              } else if (build_builders > 0 && arc.id == pocket.id) {
                Helpers.Log(rc, "miner angle");
                build_plan_int += 5 * Math.pow(10, arc.position);
              } else if (build_soldiers > 0) {
                build_plan_int += 2 * Math.pow(10, arc.position);
                build_soldiers--;
              } else if (build_miners > 0) {
                build_plan_int += 3 * Math.pow(10, arc.position);
                // Helpers.Log(rc, "MINER @ POSITION " + arc.position);

              } else { // nothing to build
                build_plan_int += 9 * Math.pow(10, arc.position);
              }
            }
          }

          // Helpers.Log(rc, "CHILL plan:" + build_plan_int);

          rc.writeSharedArray(39, (int) (build_plan_int));

          // try to get an observatory built
        } else if (mode == "DEFEND") {
          Helpers.Log(rc, "build in DEFEND mode");
          int best_affordable_type = 0;
          if (gold_stash >= RobotType.SAGE.buildCostGold) {
            best_affordable_type = 1;
          } else if (lead_stash >= RobotType.SOLDIER.buildCostLead) {
            best_affordable_type = 2;
            totalSoldiersInBuildplan++;
          }
          rc.writeSharedArray(39, (int) (best_affordable_type * Math.pow(10, def_position)));
        }
      } else {
        rc.writeSharedArray(39, 0);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * PUBLIC
   * BUILD PLAN
   * [30-37] archons (id, spawn, location, status, alive)
   * [38] archon IDs listed here every round (last wipes)
   * [39] build order
   * id is always stored with an offest of 1
   *
   * STATUS [0 - 3]
   * 0: chilling (troops can be built here)
   * 1: defending (troops should be built here)
   * 2: relocating (cannot build troops)
   *
   * 1 == SAGE
   * 2 == SOLDIER
   * 3 == MINER
   */
  // this function should be run at the start of every archon turn, returns the
  // RobotType that should be built or null
  static public RobotType MyArchonSyncAndBuild(RobotController rc, int status) {
    if (AmIFirstArchon(rc)) {
      WriteBuildPlan(rc, RobotPlayer.strategy);
      ThreatComms.ResetThreats(rc);
      // Helpers.Log(rc, String.valueOf("best lead: " + Comms.getBestLeadCount(rc)));
    }
    SyncArchon(rc, status); // round 1: write to [30-37], update status and location, write to [38]
    RobotType x = ReadBuildPlan(rc);
    if (AmILastArchon(rc)) { // set all dead archons to dead. reset 38
      MarkDeadArchons(rc);
      WipeCheckIn(rc);
    }
    return x;
  }

  /**
   * PRIVATE
   */
  // this can and will return null in the case that nothing should be built
  private static RobotType ReadBuildPlan(RobotController rc) {
    RobotType rt = null;
    try {

      int position = GetArchonPosition(rc);
      int build_plan_int = rc.readSharedArray(39);
      int chosen_type = 0;
      if (position == 0) {
        chosen_type = build_plan_int % 10;
      } else if (position == 1) {
        chosen_type = build_plan_int / 10 % 10;
      } else if (position == 2) {
        chosen_type = build_plan_int / 100 % 10;
      } else {
        chosen_type = build_plan_int / 1000;
      }

      // OVERRIDE DECISION to MINER if first round
      if (RobotPlayer.round_number == 1) {
        chosen_type = 3;
      }

      rt = IntToRobotType(chosen_type);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      return rt;
    }
  }

  // TODO: SHOULD RETURN ONLY LIVE

  /**
   *
   * @param rc
   * @return all the alive MyArchonObject stored in memory
   */
  public static ArrayList<MyArchonObject> GetArchons(RobotController rc) {
    ArrayList<MyArchonObject> my_arcs = new ArrayList<>();
    int pos = 0;
    try {
      for (int i = 30; i <= 36; i += 2) {
        int my_archon_int_1 = rc.readSharedArray(i);
        int my_archon_int_2 = rc.readSharedArray(i + 1);
        if (my_archon_int_1 != 0 && my_archon_int_2 != 0) {
          MyArchonObject x = IntToMyArchon(my_archon_int_1, my_archon_int_2);
          if (x.alive) {
            x.position = pos;
            my_arcs.add(x);
            pos++;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      return my_arcs;
    }
  }

  // ALL ARCHON RESPONSIBILITIES
  // TODO: bug in here
  private static void SyncArchon(RobotController rc, int status) {

    try {
      // if archon has not been written yet, do so
      if (RobotPlayer.round_number == 1) {
        Boolean has_written = false;
        for (int i = 30; i <= 36; i += 2) {
          int my_archon_int_1 = rc.readSharedArray(i);
          int my_archon_int_2 = rc.readSharedArray(i + 1);
          if (my_archon_int_1 == 0 && my_archon_int_2 == 0 & !has_written) {
            MyArchonObject mine = new MyArchonObject(RobotPlayer.my_id, status, RobotPlayer.spawn_loc,
                RobotPlayer.currentLocation, true);
            rc.writeSharedArray(i, MyArchonToInt1(mine));
            rc.writeSharedArray(i + 1, MyArchonToInt2(mine));
            has_written = true;
          }
        }
      }

      CheckIn(rc, RobotPlayer.my_id); // CheckIn must go before GetByID

      MyArchonObject mine = GetById(rc, RobotPlayer.my_id);
      if (mine.status != status || mine.location != RobotPlayer.currentLocation) {
        UpdateById(rc, RobotPlayer.my_id, status, RobotPlayer.currentLocation);
      }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {

    }
  }

  private static void UpdateById(RobotController rc, int id, int status, MapLocation currentLocation) {
    try {
      for (int i = 30; i <= 36; i += 2) {
        int my_archon_int_1 = rc.readSharedArray(i);
        int my_archon_int_2 = rc.readSharedArray(i + 1);
        if (my_archon_int_1 != 0 && my_archon_int_2 != 0) {
          MyArchonObject stored = IntToMyArchon(my_archon_int_1, my_archon_int_2);
          if (stored.id == id) {
            stored.status = status;
            stored.location = currentLocation;
            rc.writeSharedArray(i, MyArchonToInt1(stored));
            rc.writeSharedArray(i + 1, MyArchonToInt2(stored));
            break;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @param rc
   * @param id
   * @return SHOULDN'T BE NULL (TESTED)
   */
  private static MyArchonObject GetById(RobotController rc, int id) {
    MyArchonObject res = null;
    try {
      for (int i = 30; i <= 36; i += 2) {
        int my_archon_int_1 = rc.readSharedArray(i);
        int my_archon_int_2 = rc.readSharedArray(i + 1);
        if (my_archon_int_1 != 0 && my_archon_int_2 != 0) {
          MyArchonObject arc = IntToMyArchon(my_archon_int_1, my_archon_int_2);
          if (arc.id == id) {
            res = arc;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      return res;
    }
  }

  private static int StoredArchonsAlive(RobotController rc) {
    int c = 0;
    for (MyArchonObject arc : GetArchons(rc)) {
      if (arc.alive == true) {
        c++;
      }
    }
    return c;
  }

  private static void CheckIn(RobotController rc, int id) { // TODO: bug: this isnt maintaining existing value
    try {
      int check_in_int = rc.readSharedArray(38);
      int new_check_in_int = check_in_int;
      if (check_in_int > 16 * 16) {
        new_check_in_int += (id + 1) * 16 * 16 * 16;
      } else if (check_in_int > 16) {
        new_check_in_int += (id + 1) * 16 * 16;
      } else if (check_in_int > 0) {
        new_check_in_int += (id + 1) * 16;
      } else {
        new_check_in_int += (id + 1);
      }

      // Helpers.Log(rc, "last check in: " + new_check_in_int, 9);
      rc.writeSharedArray(38, new_check_in_int);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 38: Every Round ID CheckIn
   */
  // this assumes last guy wiped it properly

  /**
   * TESTED
   * 
   * @param rc
   * @return
   */
  private static Boolean AmIFirstArchon(RobotController rc) {
    Boolean res = false;
    try {
      res = rc.readSharedArray(38) == 0;
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      return res;
    }
  }

  private static Boolean AmILastArchon(RobotController rc) {
    // Helpers.Log(rc, "counted_ids" + ReadCheckInIDs(rc).size(), 8);
    if (ReadCheckInIDs(rc).size() == rc.getArchonCount()) {
      return true;
    }
    return false;
  }

  private static void MarkDeadArchons(RobotController rc) {
    try {
      if (StoredArchonsAlive(rc) != rc.getArchonCount()) {
        ArrayList<Integer> checked_in_archons = ReadCheckInIDs(rc);
        for (int i = 30; i <= 36; i += 2) {
          int my_archon_int_1 = rc.readSharedArray(i);
          int my_archon_int_2 = rc.readSharedArray(i + 1);
          if (my_archon_int_1 != 0 && my_archon_int_2 != 0) {
            MyArchonObject x = IntToMyArchon(my_archon_int_1, my_archon_int_2);
            if (checked_in_archons.contains(x.id)) {
              x.alive = false;
              rc.writeSharedArray(i, MyArchonToInt1(x));
              rc.writeSharedArray(i + 1, MyArchonToInt2(x));
              break;
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void WipeCheckIn(RobotController rc) {
    try {
      rc.writeSharedArray(38, 0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // run this only after sync and before wipe (should investigate what it does)
  private static int GetArchonPosition(RobotController rc) {
    return ReadCheckInIDs(rc).size() - 1;
  }

  private static ArrayList<Integer> ReadCheckInIDs(RobotController rc) {
    ArrayList<Integer> arc_ids = new ArrayList<>();
    try {
      int encoded_archon_ids = rc.readSharedArray(38);
      // Helpers.Log(rc, "check_in_int" + encoded_archon_ids, 8);
      if (encoded_archon_ids % 16 != 0) {
        arc_ids.add(encoded_archon_ids % 16 - 1);
      }
      if (encoded_archon_ids / 16 % 16 != 0) {
        arc_ids.add(encoded_archon_ids / 16 % 16 - 1);
      }
      if (encoded_archon_ids / 16 / 16 % 16 != 0) {
        arc_ids.add(encoded_archon_ids / 16 / 16 % 16 - 1);
      }
      if (encoded_archon_ids / 16 / 16 / 16 % 16 != 0) {
        arc_ids.add(encoded_archon_ids / 16 / 16 / 16 % 16 - 1);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      return arc_ids;
    }
  }

  private static RobotType SoldierMiner8020(Random r) {
    int x = r.nextInt(10);
    if (x == 9 || x == 8) {
      return RobotType.MINER;
    } else {
      return RobotType.SOLDIER;
    }
  }

  /**
   * 39: Build Plan
   */

  // GENERIC ARCHON HELPERS
  private static int MyArchonToInt1(MyArchonObject arc) {
    int offset_id = arc.id + 1;
    int spawn_x = arc.spawn.x;
    int spawn_y = arc.spawn.y;
    return spawn_x * (64 * 16) + (16) * spawn_y + (1) * offset_id;
  }

  private static int MyArchonToInt2(MyArchonObject arc) {
    int status = arc.status;
    int location_x = arc.location.x;
    int location_y = arc.location.y;
    int alive = arc.alive ? 1 : 0;
    return location_x * (64 * 8) + (8) * location_y + (2) * status + alive;
  }

  private static MyArchonObject IntToMyArchon(int int1, int int2) {
    int id = int1 % 16 - 1;
    int status = (int2 / 2) % 4;
    Boolean alive = int2 % 2 == 1;
    MapLocation spawn = new MapLocation(int1 / (64 * 16), int1 / 16 % 64);
    MapLocation location = new MapLocation(int2 / (64 * 4 * 2), (int2 / 8) % 64);
    System.out.println(location.toString());
    return new MyArchonObject(id, status, spawn, location, alive);
  }

  private static RobotType IntToRobotType(int chosen_type) {
    if (chosen_type == 0) {
      return null;
    } else if (chosen_type == 1) {
      return RobotType.SAGE;
    } else if (chosen_type == 2) {
      return RobotType.SOLDIER;
    } else if (chosen_type == 3) {
      return RobotType.MINER;
    } else if (chosen_type == 4) {
      return RobotType.LABORATORY;
    } else if (chosen_type == 5) {
      return RobotType.BUILDER;
    }

    // 9 is set but null
    return null;
  }

  /**
   * @param rc
   * @param location the location to check
   * @return maplocation of nearest allied archon (not null)
   */
  public static MapLocation GetNearestAlliedArchon(RobotController rc, MapLocation location) {
    ArrayList<MyArchonObject> my_archons = GetArchons(rc);
    MapLocation nearest_ally_archon = null;
    int nearest_ally_distance_squared = Integer.MAX_VALUE;
    for (MyArchonObject arc : my_archons) {
      int x = arc.location.distanceSquaredTo(location);
      if (x < nearest_ally_distance_squared) {
        nearest_ally_archon = arc.location;
        nearest_ally_distance_squared = x;
      }
    }
    return nearest_ally_archon;
  }
}
