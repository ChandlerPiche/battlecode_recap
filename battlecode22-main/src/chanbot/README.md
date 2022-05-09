RobotPlayer is run by the game engine, once for each living robot.

RobotStrategies contains the logic that is unique to each type of robot. MovementH is used for soldiers to determine which fights are winning or losing.

ExtendedRobotController contains the pathfinding logic. ERC.java contains wrappers for different types of movement. Pathfinder contains the core pathfinding logic. Step and Path and objects used by Pathfinder and stored to keep a record of the path each robot has taken to get to a given point.

Comms contains logic for communication between robots. Communication is limited to an array of 64 integers. Different values correspond to different uses.