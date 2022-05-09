package chanbot.ExtendedRobotController;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import chanbot.Helpers;

import java.util.ArrayList;

public class Path {
  ArrayList<Step> steps = new ArrayList<Step>();

  public String toString() {
    String res = "";
    for (Step s : steps) {
      res += s.toString();
    }
    return res;
  }

  int CalculateTotalCd() {
    int total = 0;
    for (Step s : steps) {
      total += s.cd;
    }
    return total;
  }

  // this CAN return null
  public Path(ArrayList<Step> steps) {
    this.steps = steps;
  }

  public Path addStep(Step s, RobotController rc) {
    Helpers.Log(rc, "pre add" + Clock.getBytecodesLeft(), 10196);
    steps.add(s); // TODO: 55 bytecode
    return new Path(steps);
  }

  public int getStepCount() {
    return steps.size();
  }

  Step getLastStep() {
    return steps.get(steps.size() - 1);
  }

  public String getLastMode() {
    return getLastStep().getMode();
  }

  public Direction getLastDirection() {
    return getLastStep().getDirection();
  }

  public MapLocation getLastLocation(RobotController rc) {
    MapLocation x = getLastStep().getLocation();
    // Helpers.Log(rc, "Path.getLastLocation: " + x.toString());
    return x;
  }

  public Boolean getLastIsClockwise() {
    return getLastStep().getIsClockwise();
  }
}
