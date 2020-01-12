package treesMainPlayer;
import java.lang.Math;

import battlecode.common.*;
public strictfp class Miner {
    static RobotController rc;
    static boolean initialized = true;
    static boolean miningSoup = false;
    static boolean onWay = false;
    static MapLocation soupLocation;
    static MapLocation nextSoupLocation;
    static MapLocation currentLocation;
    static MapLocation HQ_location;
    static MapLocation nearestRefineryLocation;
    static int nextSoupAmount = 0;
    static int directionMoveTries = 0;
    static int directionMoveTimes = 0;
    static int rotationCount = 0;
    static boolean turnRight = true;
    static boolean movePerp = false;
    static Direction initialSearchDirection = Direction.NORTHEAST;

    static void runMiner(RobotController rc) throws GameActionException {
        Miner.rc = rc;
        if (initialized) {
            minerInitialize();
            initialized = false;
        } else {
            minerExecute();
        }
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
                System.out.println("Going to refine soup");
                MapLocation refineryLocation = findNearestRefinery();
                moveToRefineSoup(refineryLocation);
            }
            if (rc.getTeamSoup() >= RobotType.REFINERY.cost && miningSoup && shouldBuildRefinery()){
                System.out.println("Going to build refinery");
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

    static void moveToRefineSoup(MapLocation ml) throws GameActionException {
        updateCurrentLocation();
        if (ml != null && !onWay){
            Direction refineryDir = currentLocation.directionTo(ml);
            if (!tryRefineSoupAt(refineryDir)){
                tryMoveInGeneralDirection(refineryDir, 7);
            }
        } else if(nearestRefineryLocation != null && !onWay){
            Direction refineryDir = currentLocation.directionTo(nearestRefineryLocation);
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

    static boolean shouldBuildRefinery(){
        if (nearestRefineryLocation != null && currentLocation.distanceSquaredTo(nearestRefineryLocation) < 64) {
            return false;
        }
        return true;
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

    static boolean checkIfRefineryNearby() throws GameActionException{
        RobotInfo[] allrobots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(),rc.getTeam());
        for (RobotInfo rob: allrobots){
            if (rob.getType().equals(RobotType.REFINERY)){
                return true;
            }
        }
        return false;
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

    static void searchForSoup() throws GameActionException{
        if (rotationCount == 2){
            turnRight = !turnRight;
            rotationCount = 0;
        }
        if (!movePerp){
            System.out.println("Trying This Direction");
            moveInDirection(128, 1);
        } else {
            moveInDirection(10, 1);
        }
    }

    static boolean tryBuildRobotInGeneralDirection(RobotType rt, Direction dir) throws GameActionException {
        if (rc.canBuildRobot(rt,dir)){
            rc.buildRobot(rt,dir);
            nearestRefineryLocation = currentLocation.add(dir);
            return true;
        } else if (rc.canBuildRobot(rt,dir.rotateLeft())){
            rc.buildRobot(rt,dir.rotateLeft());
            nearestRefineryLocation = currentLocation.add(dir.rotateLeft());
            return true;
        } else if (rc.canBuildRobot(rt,dir.rotateRight())){
            rc.buildRobot(rt,dir.rotateRight());
            nearestRefineryLocation = currentLocation.add(dir.rotateRight());
            return true;
        } else if (rc.canBuildRobot(rt,dir.rotateLeft().rotateLeft())){
            rc.buildRobot(rt,dir.rotateLeft().rotateLeft());
            nearestRefineryLocation = currentLocation.add(dir.rotateLeft().rotateLeft());
            return true;
        } else if (rc.canBuildRobot(rt,dir.rotateRight().rotateRight())){
            rc.buildRobot(rt,dir.rotateRight().rotateRight());
            nearestRefineryLocation = currentLocation.add(dir.rotateRight().rotateRight());
            return true;
        } else if (rc.canBuildRobot(rt,dir.opposite().rotateRight())){
            rc.buildRobot(rt,dir.opposite().rotateRight());
            nearestRefineryLocation = currentLocation.add(dir.opposite().rotateRight());
            return true;
        } else if (rc.canBuildRobot(rt,dir.opposite().rotateLeft())){
            rc.buildRobot(rt,dir.opposite().rotateLeft());
            nearestRefineryLocation = currentLocation.add(dir.opposite().rotateLeft());
            return true;
        }
        return false;
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

    static void moveInDirection(int maxDistance, int maxTries) throws GameActionException{
        if(!tryMoveInGeneralDirection(initialSearchDirection, 3)){
            directionMoveTries++;
        } else {
            directionMoveTries = 0;
            directionMoveTimes++;
        }
        if(directionMoveTimes >= maxDistance || directionMoveTries > maxTries) {
            System.out.println("Trying Different Direction");
            if (turnRight){
                initialSearchDirection = initialSearchDirection.rotateRight().rotateRight();
            } else {
                initialSearchDirection = initialSearchDirection.rotateLeft().rotateLeft();
            }
            rotationCount++;
            movePerp = true;
            directionMoveTries = 0;
            directionMoveTimes = 0;
        }
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

    static void updateCurrentLocation(){
        currentLocation = rc.getLocation();
    }
}
