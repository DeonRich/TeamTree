package treesMainPlayer;
import java.lang.Math;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static boolean foundSoup = false;
    static boolean initialized = true;
    static MapLocation soupLocation;
    static Integer[][] soupMap;
    static short builtMinersCount = 0;
    static Direction[] cardinal = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST,};
    static Direction[] adjDirections = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST,
                                        Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.SOUTHWEST};

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        soupMap = new Integer[rc.getMapHeight()][rc.getMapWidth()];

        while (true) {
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    case REFINERY:           runRefinery();          break;
                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    case NET_GUN:            runNetGun();            break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
        if (builtMinersCount < 2) {
            for (Direction dir: adjDirections){
                if (tryBuildRobot(RobotType.MINER, dir)) {
                    builtMinersCount++;
                }
            }
        }
    }

    static void runMiner() throws GameActionException {
        minerInitialize();
    }

    static void runRefinery() throws GameActionException {

    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {

    }

    static void runFulfillmentCenter() throws GameActionException {

    }

    static void runLandscaper() throws GameActionException {

    }

    static void runDeliveryDrone() throws GameActionException {

    }

    static void runNetGun() throws GameActionException {

    }

    static boolean tryBuildRobot(RobotType rt, Direction dr) throws GameActionException{
        if (rc.canBuildRobot(rt, dr) && rc.isReady()){
            rc.buildRobot(rt, dr);
            return true;
        } else return false;
    }

    static boolean tryMove(Direction dr) throws GameActionException{
        if (rc.canMove( dr) && rc.isReady()){
            rc.move(dr);
            return true;
        } else return false;
    }

    static boolean tryMoveInGeneralDirection(Direction dr) throws GameActionException{
        if (rc.isReady()){
            if (rc.canMove(dr)){
                rc.move(dr);
            } else if(rc.canMove(dr.rotateLeft())){
                rc.move(dr.rotateLeft());
            } else if(rc.canMove(dr.rotateRight())){
                rc.move(dr.rotateRight());
            }
            return true;
        } else return false;
    }

    static void moveTo(MapLocation ml) throws GameActionException{
        MapLocation currentLocation = rc.getLocation();
        Direction moveDirection = currentLocation.directionTo(ml);
        tryMove(moveDirection);
    }

    static void minerInitialize() throws GameActionException{
        soupLocation = senseMaxSoupInRadius();
        minerExecute();
    }

    static void minerExecute() throws GameActionException{
        if (soupLocation != null) {
            //add Sense soup while moving
            MapLocation currentLocation = rc.getLocation();
            Direction soupDirection = currentLocation.directionTo(soupLocation);
            if (currentLocation.isAdjacentTo(soupLocation)){
                if (rc.isReady()){
                    rc.mineSoup(soupDirection);
                }
            } else {
                tryMoveInGeneralDirection(soupDirection);
            }
        } else{
            //add Sense soup while moving
            tryMove(Direction.NORTHEAST);
        }
    }

    static boolean checkIfRefineryNearby() throws GameActionException{
        RobotInfo[] allrobots = rc.senseNearbyRobots();
        for (RobotInfo rob: allrobots){
        }
        return false;
    }
/*
    static MapLocation findSoupInDirection(Direction dir) throws GameActionException{
        ArrayList<Object> pair = senseMaxSoupInRadius();
        if ((int)pair.get(0) > 0){
            foundSoup = true;
            return (MapLocation)pair.get(1);
        } else {
        tryMove(dir);
        return null;
        }
    }
    static ArrayList<Object> senseMaxSoupInRadius() throws GameActionException {
        ArrayList<MapLocation> locations = getLocationsInRadius();
        ArrayList<Object> output = new ArrayList<Object>(2);
        Integer maxSoup = 0;
        MapLocation bestLocation = rc.getLocation();
        for (MapLocation loc: locations){
            int soup = rc.senseSoup(loc);
            soupMap[loc.x][loc.y] = soup;
            if (soup >= maxSoup){
                maxSoup = soup;
                bestLocation = loc;
            }
        }
        output.add(maxSoup);
        output.add(bestLocation);
        return output;
    }*/

    static MapLocation senseMaxSoupInRadius() throws GameActionException{
        int rSquared = rc.getType().sensorRadiusSquared;
        int radius = (int)Math.sqrt(rSquared+1);
        int maxSoup = 0;
        MapLocation currentLocation = rc.getLocation();
        MapLocation bestLocation = null;
        for( int dx = -radius; dx <= radius; dx++){
           for ( int dy = -radius; dy <= radius; dy++){
                MapLocation newLocation = currentLocation.translate(dx,dy);
               if (rc.canSenseLocation(newLocation)){
                   int currentSoup = rc.senseSoup(newLocation);
                    if (currentSoup > maxSoup){
                        bestLocation = newLocation;
                        maxSoup = currentSoup;
                    }
               }
           }
        }
        return bestLocation;
    }
    static void getOuterRadius() throws GameActionException{
        int rSquared = rc.getType().sensorRadiusSquared;
        int radius = (int)Math.sqrt(rSquared+1);
        MapLocation currentLocation = rc.getLocation();
        MapLocation newLocation = currentLocation.translate(-radius,0);
        MapLocation endLocation = currentLocation.translate(-radius+1,-1);
        Direction firstDir = Direction.NORTH;
        Direction secondDir = Direction.EAST;
        while(!newLocation.equals(endLocation)){
            MapLocation triedLocation = newLocation.add(firstDir);
            if (currentLocation.distanceSquaredTo(triedLocation) < rSquared){
                newLocation = triedLocation.add(Direction.CENTER);
            } else {
                triedLocation = newLocation.add(secondDir);
                if (currentLocation.distanceSquaredTo(triedLocation) < rSquared){
                    newLocation = triedLocation.add(Direction.CENTER);
                } else {
                    firstDir = firstDir.rotateRight().rotateRight();
                    secondDir = secondDir.rotateRight().rotateRight();
                }
            }
        }
    }
}
