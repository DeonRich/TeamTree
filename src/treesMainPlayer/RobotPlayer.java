package treesMainPlayer;
import java.awt.*;
import java.lang.Math;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static boolean foundSoup = false;
    static boolean initialized = true;
    static boolean miningSoup = false;
    static boolean onWay = false;
    static MapLocation soupLocation;
    static MapLocation nextSoupLocation;
    static MapLocation currentLocation;
    static MapLocation HQ_location;
    static Integer[][] soupMap;
    static int nextSoupAmount = 0;
    static short builtMinersCount = 0;
    static int directionMoveTries = 0;
    static int perpDirectionMoveTimes = 0;
    static boolean movePerp = false;
    static Direction initialSearchDirection = Direction.NORTHEAST;
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
        if (initialized) {
            minerInitialize();
            initialized = false;
        } else {
            minerExecute();
        }
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

    static boolean tryMoveInGeneralDirection(Direction dr, int angle) throws GameActionException{
        if (rc.isReady() && angle > 0){
            if (rc.canMove(dr)){
                rc.move(dr);
                findNextSoupLocation();
                return true;
            } else if(rc.canMove(dr.rotateLeft()) && angle > 1){
                rc.move(dr.rotateLeft());
                findNextSoupLocation();
                return true;
            } else if(rc.canMove(dr.rotateRight()) && angle > 2){
                rc.move(dr.rotateRight());
                findNextSoupLocation();
                return true;
            } else if(rc.canMove(dr.rotateLeft().rotateLeft()) && angle > 3){
                rc.move(dr.rotateLeft().rotateLeft());
                findNextSoupLocation();
                return true;
            } else if(rc.canMove(dr.rotateRight().rotateRight()) && angle > 4){
                rc.move(dr.rotateRight().rotateRight());
                findNextSoupLocation();
                return true;
            } else if(rc.canMove(dr.opposite().rotateRight()) && angle > 5){
                rc.move(dr.opposite().rotateRight());
                findNextSoupLocation();
                return true;
            } else if(rc.canMove(dr.opposite().rotateLeft())  && angle > 6){
                rc.move(dr.opposite().rotateLeft());
                findNextSoupLocation();
                return true;
            }
        }
        return false;
    }

    static void moveTo(MapLocation ml) throws GameActionException{
        MapLocation currentLocation = rc.getLocation();
        Direction moveDirection = currentLocation.directionTo(ml);
        tryMove(moveDirection);
    }

    static void minerInitialize() throws GameActionException{
        setHqLocation();
        updateCurrentLocation();
        soupLocation = senseMaxSoupInRadius(rc.getCurrentSensorRadiusSquared());
        minerExecute();
    }

    static void minerExecute() throws GameActionException{
        updateCurrentLocation();
        System.out.println(rc.getSoupCarrying());
        if (soupLocation != null) {
            //add Sense soup while moving
            Direction soupDirection = currentLocation.directionTo(soupLocation);
            if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
                MapLocation refineryLocation = findNearestRefinery();
                moveToRefineSoup(refineryLocation);
            }
            if (rc.getTeamSoup() >= RobotType.REFINERY.cost && miningSoup){
                buildRefinery();
            }
            if (currentLocation.isAdjacentTo(soupLocation)){
                if (rc.canMineSoup(soupDirection)){
                    miningSoup = true;
                    rc.mineSoup(soupDirection);
                } else if(rc.canSenseLocation(soupLocation) && rc.senseSoup(soupLocation) == 0){
                    miningSoup = false;
                    updatePrimarySoupLocation();
                }
            } else {
                tryMoveInGeneralDirection(soupDirection, 7);
            }
        } else{
            //add Sense soup while moving keepTrack of next best soup to mine when moving around
            //but don't change soup location until finished mining that one soup
            System.out.println("Searching For Soup");
            searchForSoup();
            if (!miningSoup){
                soupLocation = senseSoupInOuterRadius();
            }
        }
    }
    static void updatePrimarySoupLocation() throws GameActionException{
        soupLocation = senseMaxSoupInRadius(1);
        if (soupLocation == null){
            soupLocation = nextSoupLocation;
            nextSoupLocation = null;
            nextSoupAmount = 0;
        }
    }
    static void updateCurrentLocation(){
        currentLocation = rc.getLocation();
    }
    static void buildRefinery() throws GameActionException{
        boolean refineryNearby = checkIfRefineryNearby();
        int distanceSquareToHQ = currentLocation.distanceSquaredTo(HQ_location);
        if (!refineryNearby){
            if(distanceSquareToHQ > RobotType.HQ.sensorRadiusSquared) {
                tryBuildRobotInGeneralDirection(RobotType.REFINERY, currentLocation.directionTo(HQ_location).opposite());
            } else {
                tryMoveInGeneralDirection(currentLocation.directionTo(HQ_location).opposite(), 7);
            }
        }
    }
    static void moveToRefineSoup(MapLocation ml) throws GameActionException {
        updateCurrentLocation();
        if (ml != null && !onWay){
            Direction refineryDir = currentLocation.directionTo(ml);
            if (!tryRefineSoupAt(refineryDir)){
                tryMoveInGeneralDirection(refineryDir, 7);
            }
        } else {
            int distanceToHQ = Math.abs(currentLocation.x - HQ_location.x) + Math.abs(currentLocation.y - HQ_location.y);
            float travelRate = 1 + rc.sensePollution(currentLocation)/2000;
            if ((int)(distanceToHQ * travelRate) < RobotType.REFINERY.cost - rc.getTeamSoup()) {
                onWay = true;
            }
            if (onWay){
                Direction hqDir = currentLocation.directionTo(HQ_location);
                if (tryRefineSoupAt(hqDir)){
                    onWay = false;
                } else {
                    tryMoveInGeneralDirection(hqDir, 7);
                }
            }
        }
    }
    static void findNextSoupLocation() throws GameActionException {
        MapLocation nextPotentialLoc = senseSoupInOuterRadius();
        if (nextSoupLocation != null && rc.canSenseLocation(nextSoupLocation)){
            nextSoupAmount = rc.senseSoup(nextSoupLocation);
        }
        if (nextPotentialLoc != null){
            int sensedSoup = rc.senseSoup(nextPotentialLoc);
            if(sensedSoup > nextSoupAmount){
                nextSoupAmount = sensedSoup;
                nextSoupLocation = nextPotentialLoc;
            }
        }
    }
    static boolean tryRefineSoupAt(Direction dir) throws GameActionException {
        if(rc.canDepositSoup(dir)){
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        }
        return false;
    }

    static boolean tryBuildRobotInGeneralDirection(RobotType rt, Direction dir) throws GameActionException {
        if (rc.canBuildRobot(rt,dir)){
            rc.buildRobot(rt,dir);
            return true;
        } else if (rc.canBuildRobot(rt,dir.rotateLeft())){
            rc.buildRobot(rt,dir.rotateLeft());
            return true;
        } else if (rc.canBuildRobot(rt,dir.rotateRight())){
            rc.buildRobot(rt,dir.rotateRight());
            return true;
        } else if (rc.canBuildRobot(rt,dir.rotateLeft().rotateLeft())){
            rc.buildRobot(rt,dir.rotateLeft().rotateLeft());
            return true;
        } else if (rc.canBuildRobot(rt,dir.rotateRight().rotateRight())){
            rc.buildRobot(rt,dir.rotateRight().rotateRight());
            return true;
        } else if (rc.canBuildRobot(rt,dir.opposite().rotateRight())){
            rc.buildRobot(rt,dir.opposite().rotateRight());
            return true;
        } else if (rc.canBuildRobot(rt,dir.opposite().rotateLeft())){
            rc.buildRobot(rt,dir.opposite().rotateLeft());
            return true;
        }
        return false;
    }
    static boolean checkIfRefineryNearby() throws GameActionException{
        RobotInfo[] allrobots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(),rc.getTeam());
        for (RobotInfo rob: allrobots){
            if (rob.getType().equals(RobotType.REFINERY)){
                return true;
            }
        }
        return false;
    }

    static MapLocation findNearestRefinery() throws GameActionException{
        updateCurrentLocation();
        RobotInfo[] allRobots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(),rc.getTeam());
        int nearestDist = 64*64*2;
        MapLocation nearest = null;
        for (RobotInfo rob: allRobots){
            if (rob.getType().equals(RobotType.REFINERY) &&
                    currentLocation.distanceSquaredTo(rob.getLocation()) < nearestDist){
                nearest = rob.getLocation();
                nearestDist = currentLocation.distanceSquaredTo(rob.getLocation());
            }
        }
        return nearest;
    }

    static MapLocation senseMaxSoupInRadius(int rSquared) throws GameActionException{
        updateCurrentLocation();
        rSquared = Math.min(rc.getCurrentSensorRadiusSquared(), rSquared);
        int radius = (int)Math.sqrt(rSquared+1);
        int maxSoup = 0;
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
    static void searchForSoup() throws GameActionException{
        if (!movePerp){
            System.out.println("Trying This Direction");
            if(!tryMoveInGeneralDirection(initialSearchDirection, 3)){
                directionMoveTries++;
            } else {
                directionMoveTries = 0;
            }
            if(directionMoveTries > 1) {
                System.out.println("Trying Different Direction");
                initialSearchDirection = initialSearchDirection.rotateRight().rotateRight();
                movePerp = true;
                directionMoveTries = 0;
            }
        } else {
            if(!tryMoveInGeneralDirection(initialSearchDirection, 3)){
                directionMoveTries++;
            } else {
                directionMoveTries=0;
                perpDirectionMoveTimes++;
            }
            if(perpDirectionMoveTimes >= 10 || directionMoveTries > 1) {
                initialSearchDirection = initialSearchDirection.rotateRight().rotateRight();
                movePerp = false;
                directionMoveTries = 0;
                perpDirectionMoveTimes = 0;
            }
        }
    }

    static MapLocation senseSoupInOuterRadius() throws GameActionException{
        updateCurrentLocation();
        int rSquared = rc.getCurrentSensorRadiusSquared();
        int radius = (int)Math.sqrt(rSquared);
        int maxSoup = 0;
        int fullRotationCount = 0;
        MapLocation bestSoupLocation = null;
        MapLocation newLocation = currentLocation.translate(-radius,0);
        MapLocation startLocation = newLocation;
        Direction firstDir = Direction.NORTH;
        Direction secondDir = Direction.EAST;
        while(fullRotationCount < 2){
            if (newLocation.equals(startLocation)){
                fullRotationCount++;
            }
            MapLocation triedLocation = newLocation.add(firstDir);
            if (rc.canSenseLocation(triedLocation)){
                newLocation = triedLocation;
                int currentSoup = rc.senseSoup(newLocation);
                if (currentSoup > maxSoup){
                    maxSoup = currentSoup;
                    bestSoupLocation = newLocation;
                }
            } else {
                triedLocation = newLocation.add(secondDir);
                if (rc.canSenseLocation(triedLocation)){
                    newLocation = triedLocation;
                    int currentSoup = rc.senseSoup(newLocation);
                    if (currentSoup > maxSoup){
                        maxSoup = currentSoup;
                        bestSoupLocation = newLocation;
                    }
                } else {
                    firstDir = firstDir.rotateRight().rotateRight();
                    secondDir = secondDir.rotateRight().rotateRight();
                }
            }
        }
        return bestSoupLocation;
    }

    static void setHqLocation(){
        for (RobotInfo rob : rc.senseNearbyRobots()){
            if (rob.getType().equals(RobotType.HQ)){
                HQ_location = rob.getLocation();
            }
        }
    }
}
