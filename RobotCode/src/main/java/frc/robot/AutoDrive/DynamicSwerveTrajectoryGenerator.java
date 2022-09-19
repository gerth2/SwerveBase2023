package frc.robot.AutoDrive;

import java.util.List;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrajectoryConfig;
import edu.wpi.first.math.trajectory.TrajectoryGenerator;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.Timer;
import frc.Constants;
import frc.robot.Drivetrain.SwerveTrajectoryCmd;


public class DynamicSwerveTrajectoryGenerator {

    private final TrajectoryConfig config = new TrajectoryConfig(Units.feetToMeters(12), Units.feetToMeters(12));
    boolean trajRunning = false;
    boolean trajGenFinished = false;
    double curTrajectoryTime_s = 0;
    double trajLen_s = 0;
    double trajStart_s = 0;
    Trajectory curTraj = null;
    List<Translation2d> curInteriorWaypoints = null;

    Rotation2d trajDeltaRot;

    DynamicSwerveWaypointSet waypoints;

    Thread backgroundGenRunner;

    Runnable backgroundGen = new Runnable() {

        public void run() {
            curTraj = TrajectoryGenerator.generateTrajectory(
                waypoints.start,
                waypoints.interiorWaypoints,
                waypoints.end,
                config);

            trajDeltaRot = waypoints.endRot.minus(waypoints.startRot);

            trajLen_s = curTraj.getTotalTimeSeconds();
            trajGenFinished = true;
        }
    };
    
    public void startGeneration(DynamicSwerveWaypointSet waypoints){
        this.waypoints = waypoints;

        backgroundGenRunner = new Thread(backgroundGen);
        backgroundGenRunner.start();
    }

    public boolean isReady(){
        return trajGenFinished;
    }

    public void startTrajectory(){
        trajStart_s = Timer.getFPGATimestamp();
        curTrajectoryTime_s = 0;
        trajRunning = true;
    }

    public SwerveTrajectoryCmd getCurCmd(){
        curTrajectoryTime_s = Timer.getFPGATimestamp() - trajStart_s;

        //TODO these should probably be grouped into a swerve trajectory class
        Trajectory.State curState = curTraj.sample(curTrajectoryTime_s);
        Rotation2d curHeading = waypoints.startRot.plus(trajDeltaRot.times(curTrajectoryTime_s/trajLen_s)); 
        Rotation2d nextHeading = waypoints.startRot.plus(trajDeltaRot.times((curTrajectoryTime_s+Constants.Ts)/trajLen_s)); 
        Rotation2d curHeadingVel = nextHeading.minus(curHeading).times(1.0 / (Constants.Ts));

        return new SwerveTrajectoryCmd(curState, curHeading, curHeadingVel);

    }

    public boolean isFinished(){
        return trajGenFinished &&  trajRunning && (curTrajectoryTime_s >= trajLen_s);
    }
}
