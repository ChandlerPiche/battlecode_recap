package chanbot.Comms;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import chanbot.Helpers;

import java.util.ArrayList;

public class MyArchonObject {
  // ALL ARCHON VARIABLES
  int status;
  boolean alive;
  public MapLocation location;
  public int id;
  MapLocation spawn;
  int position;

  MyArchonObject(int id, int status, MapLocation spawn, MapLocation location, Boolean alive) {
    this.status = status;
    this.spawn = spawn;
    this.location = location;
    this.id = id;
    this.alive = alive;
  }

  @Override
  public String toString() {
    return "MyArchonObject{" +
        "status=" + status +
        ", alive=" + alive +
        ", location=" + location +
        ", id=" + id +
        ", spawn=" + spawn +
        ", position=" + position +
        '}';
  }

  /**
   * this returns the approximate center of the ally archons' spawn
   */
  static private MapLocation Centroid(RobotController rc, ArrayList<MyArchonObject> archons) {
    int total_x = 0;
    int total_y = 0;
    int c = 0;
    for (MyArchonObject archon : archons) {
      total_x += archon.spawn.x;
      total_y += archon.spawn.y;
      c++;
    }
    MapLocation location = new MapLocation(total_x / c, total_y / c);
    rc.setIndicatorDot(location, 0, 0, 255);
    return location;
  }

  static private MapLocation MapCenter(RobotController rc) {
    MapLocation location = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
    rc.setIndicatorDot(location, 255, 0, 0);
    return location;
  }

  // gets the estimated safest point on the map
  static public MapLocation SafestPoint(RobotController rc, ArrayList<MyArchonObject> archons) {
    MapLocation center = MapCenter(rc);
    MapLocation centroid = Centroid(rc, archons);
    Direction safe_direction = center.directionTo(centroid);
    if (safe_direction == Direction.CENTER) { // PERFECTLY BALANCED
      return new MapLocation(0, 0);
    } else {
      int delta_x = safe_direction.getDeltaX();
      int delta_y = safe_direction.getDeltaY();
      MapLocation new_safest = centroid.translate(delta_x, delta_y);
      int c = 0;
      while (true) {
        if (new_safest.x <= 0 || new_safest.y <= 0 || new_safest.x >= (rc.getMapWidth() - 1)
            || new_safest.y >= (rc.getMapHeight() - 1)) { // this line seems buggy AF
          rc.setIndicatorDot(new_safest, 255, 0, 255);
          return new_safest;
        } else {
          new_safest = new_safest.add(safe_direction);
        }
      }
    }
  }

  static public Boolean InPocket(RobotController rc, MapLocation x) {
    MyArchonObject pocket = PocketArchon(rc, MyArchonComms.GetArchons(rc));
    return x.isWithinDistanceSquared(pocket.spawn, PocketRadiusSquared(rc));
  }

  static int PocketRadiusSquared(RobotController rc) {
    return (int) (rc.getMapWidth() * rc.getMapHeight() / 2 / 3.14);
  }

  /**
   *
   * @param rc
   * @param archons
   * @return the safest corner (not null)
   */
  static public MapLocation SafestCorner(RobotController rc, ArrayList<MyArchonObject> archons) {
    MapLocation safest_point = SafestPoint(rc, archons);
    if (safest_point.x == 0 || safest_point.x == rc.getMapWidth() - 1) {
      if (safest_point.y < rc.getMapHeight() / 2) {
        return new MapLocation(safest_point.x, 0);
      } else {
        return new MapLocation(safest_point.x, rc.getMapHeight() - 1);
      }

    } else if (safest_point.y == 0 || safest_point.y == rc.getMapHeight() - 1) {
      if (safest_point.x < rc.getMapWidth() / 2) {
        return new MapLocation(0, safest_point.y);
      } else {
        return new MapLocation(rc.getMapWidth() - 1, safest_point.y);
      }
    }
    return null;
  }

  static public MapLocation PocketCorner(RobotController rc, ArrayList<MyArchonObject> archons) {
    MapLocation pocket = PocketArchon(rc, archons).spawn;
    int pcy = 0;
    int pcx = 0;
    if (pocket.y > rc.getMapHeight() / 2) {
      pcy = rc.getMapHeight() - 1;
    }
    if (pocket.x > rc.getMapWidth() / 2) {
      pcx = rc.getMapWidth() - 1;
    }
    return new MapLocation(pcx, pcy);
  }

  static public MapLocation PocketCorner2(RobotController rc, ArrayList<MyArchonObject> archons) {
    MapLocation pocket = PocketArchon(rc, archons).spawn;
    MapLocation pocket_corner = PocketCorner(rc, archons);

    ArrayList<MapLocation> corners = new ArrayList<>();
    corners.add(new MapLocation(0, 0));
    corners.add(new MapLocation(rc.getMapWidth() - 1, 0));
    corners.add(new MapLocation(0, rc.getMapHeight() - 1));
    corners.add(new MapLocation(rc.getMapWidth() - 1, rc.getMapHeight() - 1));

    MapLocation closest_corner2 = null;
    int closts_corner_2_dist = Integer.MAX_VALUE;
    for (MapLocation corner : corners) {
      if (!corner.equals(pocket_corner)) {
        int dist = pocket.distanceSquaredTo(corner);
        if (dist < closts_corner_2_dist) {
          closts_corner_2_dist = dist;
          closest_corner2 = corner;
        }
      }
    }
    return closest_corner2;
  }

  static public MapLocation SafishCorner(RobotController rc, ArrayList<MyArchonObject> archons) {
    MapLocation safest_point = SafestPoint(rc, archons);
    if (safest_point.x == 0 || safest_point.x == rc.getMapWidth() - 1) {
      if (safest_point.y < rc.getMapHeight() / 2) {
        return new MapLocation(safest_point.x, rc.getMapHeight() - 1);
      } else {
        return new MapLocation(safest_point.x, 0);
      }

    } else if (safest_point.y == 0 || safest_point.y == rc.getMapHeight() - 1) {
      if (safest_point.x < rc.getMapWidth() / 2) {
        return new MapLocation(rc.getMapWidth() - 1, safest_point.y);
      } else {
        return new MapLocation(0, safest_point.y);
      }
    }
    return null;
  }

  /**
   * @return the archon WHO SPAWNED closest to the safest point (not nullable)
   */
  public static MyArchonObject PocketArchon(RobotController rc, ArrayList<MyArchonObject> archons) {
    MyArchonObject safest_archon = null;
    int closest_archon_range = Integer.MAX_VALUE;
    MapLocation safest_point = SafestPoint(rc, archons);
    for (MyArchonObject archon : archons) {
      int x = archon.spawn.distanceSquaredTo(safest_point);
      if (x < closest_archon_range) {
        closest_archon_range = x;
        safest_archon = archon;
      }
    }
    rc.setIndicatorDot(safest_archon.spawn, 255, 255, 255);
    // rc.setIndicatorDot(safest_archon.location, 0, 255, 0);
    return safest_archon;
    // the archon closest to the safest point
  }

}
