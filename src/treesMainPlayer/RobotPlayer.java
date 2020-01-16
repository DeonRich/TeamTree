package treesMainPlayer;
import java.lang.Math;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static boolean initialized = true;
    static Integer[][] soupMap;
    static short builtMinersCount = 0;
    static short builtDefLandscapers = 0;
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
        Miner.runMiner(rc);
        //tryBuildRobot(RobotType.DESIGN_SCHOOL,Direction.NORTHWEST);

    }

    static void runRefinery() throws GameActionException {

    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
        if (builtDefLandscapers <1) {
            if(tryBuildRobot(RobotType.LANDSCAPER, Direction.SOUTHWEST)){
                builtDefLandscapers++;
            }
        }
    }

    static void runFulfillmentCenter() throws GameActionException {

    }

    static void runLandscaper() throws GameActionException {
        DefeseLandscaper.runDefenseLandscaper(rc);
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

}
