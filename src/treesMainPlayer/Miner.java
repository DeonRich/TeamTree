package treesMainPlayer;
import java.lang.Math;

import battlecode.common.*;
public strictfp class Miner {
    static RobotController rc;
    static boolean initialized = true;
    static boolean miningSoup = false;
    static boolean onWayToHq = false;
    static MapLocation soupLocation;
    static MapLocation nextSoupLocation;
    static MapLocation currentLocation;
    static MapLocation HQ_location;
    static MapLocation nearestRefineryLocation;
    static int nextSoupAmount = 0;
    static int directionMoveTries = 0;
    static int directionMoveTimes = 0;
    static int rotationCount = 0;
    static int maxDistanceToRefinery = 64;
    static boolean turnRight = true;
    static boolean movePerp = false;
    static Direction initialSearchDirection = Direction.NORTHEAST;
    
    /**
     * Runs the miner code
     *
     * @param rc The RobotController that is the miner
     * @throws GameActionException
     */
    static void runMiner(RobotController rc) throws GameActionException {
        Miner.rc = rc;
        if (initialized) {
            minerInitialize();
            initialized = false;
        } else {
            minerExecute();
        }
    }

    /**
     * Miner code that runs once, only when it is created
     *
     * @throws GameActionException
     */    
    static void minerInitialize() throws GameActionException{
        setHqLocation();
        updateCurrentLocation();
        soupLocation = senseMaxSoupInRadius(rc.getCurrentSensorRadiusSquared());
        minerExecute();
    }

    /**
     * Miner code that runs continuously. Miner will search for soup, move towards it,
     * build refinery, and deposit soup.
     *
     * @throws GameActionException
     */
    static void minerExecute() throws GameActionException{
        updateCurrentLocation();
        if (soupLocation != null) {
            //add Sense soup while moving
            Direction soupDirection = currentLocation.directionTo(soupLocation);
            if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
                System.out.println("Going to refine my maxed soup");
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
    
    /**
     * Changes the primary soup location to be the max soup found adjacent to the miner,
     * if none found, make it the nextSoupLocation.
     *
     * @throws GameActionException
     */
    static void updatePrimarySoupLocation() throws GameActionException{
        soupLocation = senseMaxSoupInRadius(1);
        if (soupLocation == null){
            soupLocation = nextSoupLocation;
            nextSoupLocation = null;
            nextSoupAmount = 0;
        }
    }

    /**
     * looks for nearest refinery in search radius.
     * 
     * @return nearest refinery if found, otherwise null
     */
    static MapLocation findNearestRefinery(){
        updateCurrentLocation();
        RobotInfo[] allRobots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(),rc.getTeam());
        int nearestDist = 64*64*2;
        MapLocation nearest = nearestRefineryLocation;
        for (RobotInfo rob: allRobots){
            if (rob.getType().equals(RobotType.REFINERY) &&
                    currentLocation.distanceSquaredTo(rob.getLocation()) < nearestDist){
                nearest = rob.getLocation();
                nearestDist = currentLocation.distanceSquaredTo(rob.getLocation());
            }
        }
        return nearest;
    }

    /**
     * Moves to refinery or HQ to refine soup, then refines soup.
     *
     * @param ml MapLocation of the nearest refinery
     */
    static void moveToRefineSoup(MapLocation ml) throws GameActionException {
        updateCurrentLocation();
        if (ml != null && !onWayToHq && currentLocation.distanceSquaredTo(ml) < maxDistanceToRefinery){
            Direction refineryDir = currentLocation.directionTo(ml);
            if (!tryRefineSoupAt(refineryDir)){
                tryMoveInGeneralDirection(refineryDir, 7);
            }
        } else if(currentLocation.distanceSquaredTo(HQ_location) < maxDistanceToRefinery){
            int distanceToHQ = Math.abs(currentLocation.x - HQ_location.x) + Math.abs(currentLocation.y - HQ_location.y);
            float travelRate = 1 + rc.sensePollution(currentLocation)/2000;
            if ((int)(distanceToHQ * travelRate) < RobotType.REFINERY.cost - rc.getTeamSoup()) {
                onWayToHq = true;
            }
            if (onWayToHq){
                Direction hqDir = currentLocation.directionTo(HQ_location);
                if (tryRefineSoupAt(hqDir)){
                    onWayToHq = false;
                } else {
                    tryMoveInGeneralDirection(hqDir, 7);
                }
            }
        }
    }

    /**
     * Should miner build refinery based on nearest refinery location
     *
     */
    static boolean shouldBuildRefinery(){
        if (nearestRefineryLocation != null &&
                currentLocation.distanceSquaredTo(nearestRefineryLocation) < maxDistanceToRefinery) {
            return false;
        }
        return true;
    }

    /**
     * If too close to HQ move away, otherwise build refinery in opposite direction of HQ if possible.
     *
     */
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

    /**
     * Quick search to see if refineries are in nearby search radius
     *
     */
    static boolean checkIfRefineryNearby() throws GameActionException{
        RobotInfo[] allrobots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(),rc.getTeam());
        for (RobotInfo rob: allrobots){
            if (rob.getType().equals(RobotType.REFINERY)){
                return true;
            }
        }
        return false;
    }

    /**
     * looks for soup in outer sensor radius and changes the next soup location to be the on with most soup
     *
     */
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

    /**
     * Search for soup by moving in the initialSearchDirection and sensing soup in outer sensor radius.
     *
     */
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

    /**
     * Try to build a given robot type in the given direction, trying all direction except the one opposite to given
     * direction
     *
     * @param rt the RobotType to build
     * @param dir the direction to first try building in
     * @return true if built successfully else false
     */
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

    /**
     * Sense soup in the entire current radius of the miner
     *
     * @return the location of the most soup, otherwise null
     */
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

    /**
     * Try to move in the given direction for a certain distance, with set amount of times in can fail moving in the direction
     *
     * @param maxDistance the maximum distance to move in the initialSearchDirection
     * @param maxTries the maximum number of tries given to consecutively fail moving in the initialSearchDirection
     */
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

    /**
     * Try to move in the given direction, and if successful sense soup in the outer sensor radius of new location
     * and update the next soup location to look for
     *
     * @param dr the direction to first move in
     * @param angle the number of directions to try in an alternating fashion around the given direction
     * @return true if moved successfully else false
     */
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

    /**
     * Looking only in the outer sensor radius, sense soup.
     *
     * @return the location of most soup, otherwise null
     */
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
