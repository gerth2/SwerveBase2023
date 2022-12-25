package frc.robot.AutoDrive;

import java.util.List;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.trajectory.TrapezoidProfile.Constraints;
import edu.wpi.first.math.trajectory.TrapezoidProfile.State;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.Timer;
import frc.Constants;
import frc.robot.Drivetrain.SwerveTrajectoryCmd;


public class DynamicSwerveTrajectoryGenerator {

    boolean trajRunning = false;
    boolean trajGenFinished = false;
    double curTrajectoryTime_s = 0;
    double trajLen_s = 0;
    double trajStart_s = 0;
    List<Translation2d> curInteriorWaypoints = null;


    final double TRAJ_SPEED_MPS = Units.feetToMeters(10); //hardcode max speed

    TrapezoidProfile timeProfile = null;


    Rotation2d trajDeltaRot;

    DynamicSwerveWaypointSet waypoints;

    Thread backgroundGenRunner;

    Runnable backgroundGen = new Runnable() {

        public void run() {

            //Calcualte starting rotation that gets us cleanly from start to end
            var initVelVector = waypoints.end.minus(waypoints.start);
            var trajStartRot = new Rotation2d(initVelVector.getX(), initVelVector.getY());
            var trajEndRot = new Rotation2d(initVelVector.getX(), initVelVector.getY());


            Pose2d start = new Pose2d(waypoints.start.getTranslation(), trajStartRot);
            Pose2d end = new Pose2d(waypoints.end.getTranslation(), trajEndRot);
            Transform2d trajDelta = new Transform2d(start, end);

            trajDeltaRot = waypoints.endRot.minus(waypoints.startRot);

            trajLen_s = trajDelta.getTranslation().getNorm() / TRAJ_SPEED_MPS;

            //Time profile needs to advance from 0 up to trajlen_sec
            // Kinda hacky
            timeProfile = new TrapezoidProfile(new Constraints(1.0, 1.0), new State(trajLen_s, 0));


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

    private double getTrajFrac(double curTimeSec){
        //Hacky part 1. 
        // Abuse the a trapezoidal profile to convert a time into a fraction that goes from 0 to 1
        // at a reasonable rate.
        var tmp = timeProfile.calculate(curTrajectoryTime_s);
        double modTrajTime = tmp.position;

        var trajFrac = modTrajTime / trajLen_s;
        trajFrac = Math.max(trajFrac, 0);
        trajFrac = Math.min(trajFrac, 1);

        return trajFrac;
    }

    public SwerveTrajectoryCmd getCurCmd(){
        curTrajectoryTime_s = Timer.getFPGATimestamp() - trajStart_s;

        //Hacky part 2
        // do physics of position/velocity/accel with euler method and
        // looking forward to the next three fractions
        var trajFrac_0 = getTrajFrac(curTrajectoryTime_s);
        var trajFrac_1 = getTrajFrac(curTrajectoryTime_s + Constants.Ts);
        var trajFrac_2 = getTrajFrac(curTrajectoryTime_s + 2*Constants.Ts);

        // Hacky part 2.5
        // Poses just interpolate
        // WPILib trajectory seemed to have some wonky corner cases
        // soooo we do the simple method.
        var pose_0 = waypoints.start.interpolate(waypoints.end, trajFrac_0);
        var pose_1 = waypoints.start.interpolate(waypoints.end, trajFrac_1);
        var pose_2 = waypoints.start.interpolate(waypoints.end, trajFrac_2);

        var vel_0 = pose_1.minus(pose_0).times(1.0 / (Constants.Ts)).getTranslation().getNorm();
        var vel_1 = pose_2.minus(pose_1).times(1.0 / (Constants.Ts)).getTranslation().getNorm();

        var accel_0 = (vel_1 - vel_0)/(Constants.Ts);
        
        // Hacky part 3
        // Pretend all the previous math produces a trajectory state
        // which it really doesn't but uhhhh well yea.
        Trajectory.State curState = new Trajectory.State(curTrajectoryTime_s, vel_0, accel_0, pose_0, 0.0);

        // Hacky part 4
        // Headings just interpolate like poses
        Rotation2d curHeading = waypoints.startRot.plus(trajDeltaRot.times(trajFrac_0)); 
        Rotation2d nextHeading = waypoints.startRot.plus(trajDeltaRot.times(trajFrac_1)); 
        Rotation2d curHeadingVel = nextHeading.minus(curHeading).times(1.0 / (Constants.Ts));

        return new SwerveTrajectoryCmd(curState, curHeading, curHeadingVel);

    }

}
