package treesMainPlayer;

import battlecode.common.*;

import java.util.Map;


public strictfp class DefeseLandscaper {
    static RobotController rc;
    static boolean initialized = true;
    static boolean moveAroundClockwise = true;
    static boolean isMovingOutsidePerimeter = true;
    static boolean isInsideLower = false;
    static boolean isInsideHigher = false;
    static boolean isPickingDirtNearby = false;
    static boolean isDepositingDirt = false;
    static boolean isBuildingWall = false;
    static boolean isUnloadingDirt = false;
    static boolean isSettingUp = true;
    static boolean isBuildingStairs = false;
    static boolean turnRight = true;
    static boolean movePerp = false;
    static MapLocation currentLocation;
    static MapLocation HQ_Location;
    static MapLocation initialLocation;
    static MapLocation stairsStartLocation;
    static MapLocation loopStartLocation;
    static MapLocation lastPossibleStairLocation;
    static MapLocation lowestEvelationLocation;
    static MapLocation highestElevationLocation;
    static int directionMoveTries = 0;
    static int directionMoveTimes = 0;
    static int rotationCount = 0;
    static int HqElevation;
    static int maxWallHeight;
    static int currentWallLevel =1;
    static int loopCount = 0;
    static final double efficiency = .2;
    static final int wallLength = 9;
    static final int wallSectionHeight = GameConstants.MAX_DIRT_DIFFERENCE;
    static Direction initialSearchDirection;


    static void runDefenseLandscaper(RobotController rc) throws GameActionException{
        if(initialized){
            DefeseLandscaper.rc = rc;
            initialize();
            initialized = false;
        }
        execute();
    }

    static void initialize() throws GameActionException{
        setHqLocation();
        updateCurrentLocation();
        initialSearchDirection = HQ_Location.directionTo(currentLocation);
        if (rc.canSenseLocation(HQ_Location)){
            HqElevation = rc.senseElevation(HQ_Location);
            maxWallHeight = (int)(roundsUntilFlooded(HqElevation) * efficiency / (4*(wallLength-1)));
        }
    }

    static void execute() throws GameActionException{
        updateCurrentLocation();
        System.out.println("I got " + rc.getDirtCarrying() + " Dirt");
        if(rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit){
            isPickingDirtNearby = false; isDepositingDirt = true;
            System.out.println("I'm full of dirt!");
        }

        if(isDepositingDirt) {
            if(isUnloadingDirt){
                if(rc.getDirtCarrying() < 5){
                    isUnloadingDirt = false;
                } else{
                    rc.depositDirt(HQ_Location.directionTo(currentLocation));
                }
            }
            if(isBuildingWall){
                System.out.println("Building wall");

                if(buildWallNearby()){
                    tryToMoveToWall();
                }
                if(rc.getDirtCarrying() == 0){
                    isDepositingDirt = false; isMovingOutsidePerimeter = true;
                } else{
                    if(isWallHigh()  && rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit){
                        isUnloadingDirt = true;
                    }
                    tryDigOutWalls();
                    moveAlongWall();
                }
            }
            if(isBuildingStairs){
                System.out.println("Building stairs");
                if(buildStairPair(stairsStartLocation,0)){
                    System.out.println("Stairs built!");
                    isBuildingStairs = false;
                    isBuildingWall = true;
                }
                if(rc.getDirtCarrying() == 0){
                    isDepositingDirt = false; isMovingOutsidePerimeter = true;
                } else{
                    moveAlongWall();
                }
            }
            if(isSettingUp){
                if(isInsideLower){
                    if(lowestEvelationLocation == null || buildStair(lowestEvelationLocation,0)){
                        lowestEvelationLocation = null;
                        findUnEvennessInside();
                        if(lowestEvelationLocation == null){
                            isInsideLower = false;
                        }
                    }
                } else {
                    System.out.println("trying to build setupWall");
                    if(buildWallNearby()){
                        tryToMoveToWall();
                        moveAlongWall();
                    }
                    if(trySetBuildLocations()){
                        isSettingUp = false;
                        isBuildingStairs = true;
                    }
                }
                if(rc.getDirtCarrying() == 0){
                    isDepositingDirt = false; isMovingOutsidePerimeter = true;
                }
            }
        }
        if(isMovingOutsidePerimeter) {
            findUnEvennessInside();
            if(lowestEvelationLocation != null){
                isInsideLower = true;
            }
            if(highestElevationLocation != null){
                System.out.println("moving to high inside location!");
                isInsideHigher = true;
                tryMoveTo(highestElevationLocation, 5);
                updateCurrentLocation();
                if(currentLocation.isAdjacentTo(highestElevationLocation)){
                    isPickingDirtNearby = true; isMovingOutsidePerimeter = false;
                }
            } else if(!isOutsideWallPerimeter(currentLocation)){
                System.out.println("moving outside Perimeter!");
                moveOutsidePerimeter();
            }else {
                isPickingDirtNearby = true; isMovingOutsidePerimeter = false;
            }
        }
        if(isPickingDirtNearby && !tryGetDirtFromNearby()){
            System.out.println("need to get more dirt!");
            if(isInsideHigher){
                highestElevationLocation = null;
                findUnEvennessInside();
                if(highestElevationLocation == null){
                    isInsideHigher = false;
                }
                if(!currentLocation.isAdjacentTo(highestElevationLocation)){
                    isMovingOutsidePerimeter = true; isPickingDirtNearby = false;
                }
            }
            if(!isInsideHigher
                    || (isInsideHigher
                    && rc.canSenseLocation(highestElevationLocation)
                    && isAdjacentToFlooding(highestElevationLocation))){
                moveAroundPerimeter();
            }
        }
    }

    static boolean buildWallNearby() throws GameActionException{
        updateCurrentLocation();
        if(isOnWall(currentLocation)){
            buildStair(currentLocation,1);
        }
        for(Direction dir: Direction.cardinalDirections()){
            if(isOnWall(currentLocation.add(dir))){
                if(!buildStair(currentLocation.add(dir),1)){
                    return false;
                }
            }
        }
        return true;
    }

    static boolean buildStairPair(MapLocation ml, int level) throws  GameActionException{
        for (Direction dir: Direction.cardinalDirections()){
            if(isOnWall(ml) && isInsideWallPerimeter(ml.add(dir))){
                if(buildStair(ml.add(dir), level)
                        && buildStair(ml.add(dir.opposite()), level)){
                    return true;
                }
            }
        }
        return false;
    }
    static boolean buildStair(MapLocation location, int level) throws GameActionException{
        updateCurrentLocation();
        Direction moveDirection = currentLocation.directionTo(location);
        if(rc.canSenseLocation(location) && rc.senseElevation(location) == level * wallSectionHeight + HqElevation){
            return true;
        }
        if(currentLocation.isAdjacentTo(location)){
            if(rc.canDepositDirt(moveDirection)){
                if(rc.senseElevation(location) < level * wallSectionHeight + HqElevation){
                    rc.depositDirt(moveDirection);
                } else{
                    return true;
                }
            }
        } else{
            tryMoveInGeneralDirection(moveDirection, 3);
        }
        return false;
    }

    static void buildWall() throws GameActionException{

//        if(!isInsidePartFinished){
//            System.out.println("Building inside stair!");
//            isInsidePartFinished = buildStair(insideLocation,1);
//        } else if(!isWallPartFinished){
//            System.out.println("Building wall!");
//            isWallPartFinished = buildStair(wallLocation,2);
//        } else if(!isOutsidePartFinished){
//            System.out.println("Building outside stair!");
//            isOutsidePartFinished = buildStair(outsideLocation,1);
//        }
    }

    static void moveAlongWall() throws GameActionException{
        if(moveAroundClockwise){
            if(couldBeStairLocation(currentLocation)){
                lastPossibleStairLocation = currentLocation;
            } else if(!isWallCorner(currentLocation)){
                stairsStartLocation = lastPossibleStairLocation;
            }
        } else{
            if(couldBeStairLocation(currentLocation)){
                stairsStartLocation = currentLocation;
            }
        }
        if(rc.isReady()) {
            if(currentLocation.equals(loopStartLocation)){
                loopCount++;
            }
            if(loopCount>1){
                stairsStartLocation = lastPossibleStairLocation;
            }
        }
        Direction moveDirection = currentLocation.directionTo(HQ_Location);
        if(!isCardinalDirection(moveDirection)){
            if(moveAroundClockwise){
                moveDirection = moveDirection.rotateLeft();
            } else {
                moveDirection = moveDirection.rotateRight();
            }
        }
        Direction moveDirection2;
        if(moveAroundClockwise){
            moveDirection2 = moveDirection.rotateLeft().rotateLeft();
        } else {
            moveDirection2 = moveDirection.rotateRight().rotateRight();
        }
        if(isOnWall(currentLocation.add(moveDirection)) && rc.canMove(moveDirection)){
            tryMoveInGeneralDirection(moveDirection,1);
        } else if(isOnWall(currentLocation.add(moveDirection2)) && rc.canMove(moveDirection2)){
            tryMoveInGeneralDirection(moveDirection2,1);
        } else if(rc.isReady()){
            System.out.println("Changing Rotation");
            moveAroundClockwise = !moveAroundClockwise;
        }
    }

    static boolean isWallHigh() throws GameActionException{
        updateCurrentLocation();
        for(Direction dir: Direction.cardinalDirections()){
            MapLocation newLocation = currentLocation.add(dir);
            if(isOnWall(newLocation)
                    && rc.senseElevation(newLocation) > HqElevation + wallSectionHeight){
                return true;
            }
        }
        return false;
    }
    static boolean tryDigOutWalls() throws GameActionException{
        updateCurrentLocation();
        for(Direction dir: Direction.cardinalDirections()){
            MapLocation newLocation = currentLocation.add(dir);
            if(isOnWall(newLocation)
                    && rc.senseElevation(newLocation) > HqElevation + wallSectionHeight
                    && rc.canDigDirt(dir)){
                rc.digDirt(dir);
                return true;
            }
        }
        return false;
    }
    static boolean trySetBuildLocations() throws GameActionException{
        updateCurrentLocation();
        if(stairsStartLocation != null){
            return true;
        }
        if(isOnWall(currentLocation) && !isWallCorner(currentLocation) && !couldBeStairLocation(currentLocation)){
            return true;
        }
        return false;
    }
    static boolean couldBeStairLocation(MapLocation ml) throws GameActionException{
        for(Direction dir: Direction.cardinalDirections()){
            if(isInsideWallPerimeter(ml.add(dir))
                    &&isOnWall(ml)){
                return true;
            }
        }
        return false;
    }

    static void setStairStartLocation() throws GameActionException{
        updateCurrentLocation();
        if (tryMoveToOutsidePerimeter()) {
            System.out.println("Moving around perimeter to build stairs");
            Direction moveDirection = currentLocation.directionTo(HQ_Location);
            if(!isCardinalDirection(moveDirection)){
                moveDirection = moveDirection.rotateLeft();
            }
            Direction moveDirection2 = moveDirection.rotateLeft().rotateLeft();
            if(isOutsideWallPerimeter(currentLocation.add(moveDirection)) && rc.canMove(moveDirection)){
                tryMoveInGeneralDirection(moveDirection,1);
                if(currentLocation.add(moveDirection).equals(initialLocation)){
                    stairsStartLocation = currentLocation.add(currentLocation.directionTo(HQ_Location));
                }
            } else if(isOutsideWallPerimeter(currentLocation.add(moveDirection2)) && rc.canMove(moveDirection2)){
                tryMoveInGeneralDirection(moveDirection2,1);
                if(currentLocation.add(moveDirection2).equals(initialLocation)){
                    stairsStartLocation = currentLocation.add(currentLocation.directionTo(HQ_Location));
                }
            } else if(rc.isReady()){
                stairsStartLocation = currentLocation.add(moveDirection);
            }
        }
    }

    static boolean tryMoveTo(MapLocation ml, int angle) throws GameActionException{
        updateCurrentLocation();
        if (currentLocation.equals(ml)){
            return true;
        }
        tryMoveInGeneralDirection(currentLocation.directionTo(ml), angle);
        return false;
    }
    static boolean tryMoveToOutsidePerimeter() throws GameActionException{
        updateCurrentLocation();
        Direction moveDirection = currentLocation.directionTo(HQ_Location);
        if(isOutsideWallPerimeter(currentLocation.add(moveDirection))){
            tryMoveInGeneralDirection(moveDirection, 3);
            return false;
        } else {
            if(initialLocation == null) {
                initialLocation = rc.getLocation();
            }
            return true;
        }
    }

    static boolean tryGetDirtFromNearby() throws GameActionException{
        updateCurrentLocation();
        for (Direction dir: Direction.allDirections()){
            MapLocation newLocation = currentLocation.add(dir);
            if(rc.canDigDirt(dir) && canDigInLocation(newLocation)){
                rc.digDirt(dir);
                return true;
            }
        }
        return false;
    }

    static boolean canDigInLocation(MapLocation ml) throws GameActionException{
        if(rc.senseElevation(ml) > HqElevation - wallSectionHeight
                && isOutsideWallPerimeter(ml)
                && !isAdjacentToFlooding(ml)
                && (rc.senseRobotAtLocation(ml) == null || !rc.senseRobotAtLocation(ml).getType().isBuilding())){
            return true;
        }
        if(rc.senseElevation(ml) > HqElevation
                && isInsideWallPerimeter(ml)
                && !isAdjacentToFlooding(ml)
                && (rc.senseRobotAtLocation(ml) == null || !rc.senseRobotAtLocation(ml).getType().isBuilding())){
            return true;
        }
        return false;
    }

    static void moveOutsidePerimeter() throws GameActionException{
        if (rotationCount == 2){
            turnRight = !turnRight;
            rotationCount = 0;
        }
        if (!movePerp){
            System.out.println("Trying This Direction");
            moveInDirection(128, 1);
        } else {
            moveInDirection(30, 1);
        }
    }

    static void moveAroundPerimeter() throws GameActionException{
        Direction moveDirection = currentLocation.directionTo(findNearestDiggableDirt());
        if(moveDirection != null){
            System.out.println("Move Direction: " + moveDirection);
            tryMoveInGeneralDirection(moveDirection,5);
        } else {
            System.out.println("Moving around  perimeter");
            moveOutsidePerimeter();
        }
    }

    static boolean tryToMoveToWall() throws GameActionException{
        updateCurrentLocation();
        if(isOnWall(currentLocation)){
            if(loopStartLocation == null){
                loopStartLocation = currentLocation;
            }
            return true;
        }
        if(currentLocation.x - HQ_Location.x > wallLength/2 && currentLocation.y - HQ_Location.y > wallLength/2){
            tryMoveInGeneralDirection(Direction.SOUTHWEST,1);
        } else if(currentLocation.x - HQ_Location.x > wallLength/2 && currentLocation.y - HQ_Location.y < -wallLength/2){
            tryMoveInGeneralDirection(Direction.NORTHWEST,1);
        } else if(currentLocation.x - HQ_Location.x < -wallLength/2 && currentLocation.y - HQ_Location.y > wallLength/2){
            tryMoveInGeneralDirection(Direction.SOUTHEAST,1);
        } else if(currentLocation.x - HQ_Location.x < -wallLength/2 && currentLocation.y - HQ_Location.y < -wallLength/2){
            tryMoveInGeneralDirection(Direction.NORTHEAST,1);
        } else if(currentLocation.x - HQ_Location.x > wallLength/2){
            tryMoveInGeneralDirection(Direction.WEST,1);
        } else if(currentLocation.x - HQ_Location.x < -wallLength/2){
            tryMoveInGeneralDirection(Direction.EAST,1);
        } else if(currentLocation.y - HQ_Location.y > wallLength/2){
            tryMoveInGeneralDirection(Direction.SOUTH,1);
        } else if(currentLocation.y - HQ_Location.y < -wallLength/2){
            tryMoveInGeneralDirection(Direction.NORTH,1);
        } else{
            tryMoveInGeneralDirection(HQ_Location.directionTo(currentLocation),3);
        }
        return false;
    }

    static MapLocation findNearestDiggableDirt() throws GameActionException{
        updateCurrentLocation();
        int rSquared = rc.getCurrentSensorRadiusSquared();
        int radius = (int)Math.sqrt(rSquared) -2;
        MapLocation bestLocation = null;
        int nearestDist = 64*64*2;
        MapLocation perimeterLocation = nearestLocationOnPerimeter();
        for( int dx = -radius; dx <= radius; dx++){
            for ( int dy = -radius; dy <= radius; dy++){
                MapLocation newLocation = currentLocation.translate(dx,dy);
                if (rc.canSenseLocation(newLocation)
                        && canDigInLocation(newLocation)){
                    if (newLocation.distanceSquaredTo(perimeterLocation) < nearestDist){
                        bestLocation = newLocation;
                        nearestDist = newLocation.distanceSquaredTo(perimeterLocation);
                    }
                }
            }
        }
        return bestLocation;
    }
    static void findUnEvennessInside() throws GameActionException{
        updateCurrentLocation();
        int rSquared = rc.getCurrentSensorRadiusSquared();
        int radius = (int)Math.sqrt(rSquared);
        int lowNearestDist;
        int highNearestDist;
        if (lowestEvelationLocation == null) {
            lowNearestDist = 64*64*2;
        }else {
            lowNearestDist = currentLocation.distanceSquaredTo(lowestEvelationLocation);
        }

        if (highestElevationLocation == null) {
            highNearestDist = 64*64*2;
        }else {
            highNearestDist = currentLocation.distanceSquaredTo(highestElevationLocation);
        }

        for( int dx = -radius; dx <= radius; dx++){
            for ( int dy = -radius; dy <= radius; dy++){
                MapLocation newLocation = currentLocation.translate(dx,dy);
                if (rc.canSenseLocation(newLocation)
                        && isInsideWallPerimeter(newLocation)){
                    if (rc.senseElevation(newLocation) < HqElevation
                            && newLocation.distanceSquaredTo(currentLocation) < lowNearestDist){
                        lowestEvelationLocation = newLocation;
                        lowNearestDist = newLocation.distanceSquaredTo(currentLocation);
                    }
                    if (rc.senseElevation(newLocation) > HqElevation
                            && newLocation.distanceSquaredTo(currentLocation) < highNearestDist){
                        highestElevationLocation = newLocation;
                        highNearestDist = newLocation.distanceSquaredTo(currentLocation);
                    }
                }
            }
        }
    }

    static MapLocation nearestLocationOnPerimeter(){
        updateCurrentLocation();
        MapLocation newLocation = currentLocation;
        while(isOutsideWallPerimeter(newLocation)){
            newLocation =newLocation.add(currentLocation.directionTo(HQ_Location));
        }
        return newLocation;
    }
    static boolean isOutsideWallPerimeter(MapLocation ml){
        if (Math.abs(ml.x - HQ_Location.x) > (wallLength/2 + 1) || Math.abs(ml.y - HQ_Location.y)> (wallLength/2 + 1)){
            return true;
        }
        return false;
    }

    static boolean isInsideWallPerimeter(MapLocation ml){
        if (Math.abs(ml.x - HQ_Location.x) < wallLength/2 && Math.abs(ml.y - HQ_Location.y) < wallLength/2){
            return true;
        }
        return false;
    }

    static boolean isOnWall(MapLocation ml){
        if ((Math.abs(ml.x - HQ_Location.x) == wallLength/2 && Math.abs(ml.y - HQ_Location.y) <= wallLength/2)
                ||(Math.abs(ml.y - HQ_Location.y) == wallLength/2 && Math.abs(ml.x - HQ_Location.x) <= wallLength/2)){
            return true;
        }
        return false;
    }
    static boolean isWallCorner(MapLocation ml){
        if ((Math.abs(ml.x - HQ_Location.x) == wallLength/2 && Math.abs(ml.y - HQ_Location.y) == wallLength/2)){
            return true;
        }
        return false;
    }

    static int waterLevelAtRound(int round){
        int ans = (int)Math.pow(Math.E,.0028*round - 1.38*Math.sin(.00157*round-1.73) + 1.38*Math.sin(-1.73)) - 1;
        return ans;
    }
    static int roundsUntilFlooded(int elevation) throws GameActionException{
        int start = 0;
        int end = 5000;
        if (elevation > 385000){
            return end;
        }
        while(end-start > 1){
            int midpoint =  (end+start)/2;
            if (waterLevelAtRound(midpoint) >= elevation){
                end = midpoint;
            } else{
                start = midpoint;
            }
        }
        return end;
    }

    static boolean tryMoveInGeneralDirection(Direction dr, int angle) throws GameActionException{
        if (rc.isReady() && angle > 0){
            if (rc.canMove(dr)){
                rc.move(dr);
                return true;
            } else if(rc.canMove(dr.rotateLeft()) && angle > 1){
                rc.move(dr.rotateLeft());
                return true;
            } else if(rc.canMove(dr.rotateRight()) && angle > 2){
                rc.move(dr.rotateRight());
                return true;
            } else if(rc.canMove(dr.rotateLeft().rotateLeft()) && angle > 3){
                rc.move(dr.rotateLeft().rotateLeft());
                return true;
            } else if(rc.canMove(dr.rotateRight().rotateRight()) && angle > 4){
                rc.move(dr.rotateRight().rotateRight());
                return true;
            } else if(rc.canMove(dr.opposite().rotateRight()) && angle > 5){
                rc.move(dr.opposite().rotateRight());
                return true;
            } else if(rc.canMove(dr.opposite().rotateLeft())  && angle > 6){
                rc.move(dr.opposite().rotateLeft());
                return true;
            }
        }
        return false;
    }
    /**
     * Try to move in the given direction for a certain distance, with set amount of times in can fail moving in the direction
     *
     * @param maxDistance the maximum distance to move in the initialSearchDirection
     * @param maxTries the maximum number of tries given to consecutively fail moving in the initialSearchDirection
     */
    static void moveInDirection(int maxDistance, int maxTries) throws GameActionException{
        System.out.println("Move Times: " + directionMoveTimes);
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

    static void updateCurrentLocation(){
        currentLocation = rc.getLocation();
    }

    static void setHqLocation(){
        for (RobotInfo rob : rc.senseNearbyRobots()){
            if (rob.getType().equals(RobotType.HQ)){
                HQ_Location = rob.getLocation();
            }
        }
    }

    static int distanceIn1D(MapLocation ml1, MapLocation ml2){
        return Math.abs(ml1.x-ml2.x) + Math.abs(ml1.y-ml2.y);
    }
    static boolean isAdjacentToFlooding(MapLocation ml) throws GameActionException{
        for (Direction dir: Direction.allDirections()){
            MapLocation newLocation = ml.add(dir);
            if(rc.canSenseLocation(newLocation) && rc.senseFlooding(newLocation)){
                return true;
            }
        }
        return false;
    }

    static boolean isCardinalDirection(Direction dr){
        for (Direction card: Direction.cardinalDirections()){
            if (card.equals(dr)){
                return true;
            }
        }
        return false;
    }


}
