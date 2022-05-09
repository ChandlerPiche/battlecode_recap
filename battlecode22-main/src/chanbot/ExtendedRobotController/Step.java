package chanbot.ExtendedRobotController;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class Step {
  Direction dir;
  String mode;
  int cd;
  Boolean clock;
  MapLocation loc;
  // TODO: Boolean imagined;

  public Step(Direction dir, String mode, int cd, Boolean clock, MapLocation loc) {
    // TODO: could calculate cd down here
    this.dir = dir;
    this.mode = mode;
    this.cd = cd;
    this.clock = clock;
    this.loc = loc;
  }

  public String toString() {
    return (this.clock ? "L" : "R") + "|" + this.mode + "|" + this.cd + "|" + this.dir.toString() + "|"
        + this.loc.toString();
  }

  public Direction getDirection() {
    return this.dir;
  }

  public MapLocation getLocation() {
    return this.loc;
  }

  public String getMode() {
    return this.mode;
  }

  public Boolean getIsClockwise() {
    return this.clock;
  }

  // // getRubble should return the rubble of the tile if possible, otherwise
  // last_seen_rubble
  // public MapLocation getRubble() {
  // return this.rubble;
  // }
}
