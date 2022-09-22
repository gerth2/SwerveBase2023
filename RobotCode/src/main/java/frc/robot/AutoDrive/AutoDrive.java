package frc.robot.AutoDrive;

import java.util.ArrayList;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.util.Units;
import frc.robot.Drivetrain.DrivetrainControl;

public class AutoDrive {

    //Team 1619-inspired driver assist module
    // Normally, the operator provides manual motion inputs
    // However, when commanded, this class will generate a trajectory on the fly
    // and run the trajectory as long as the operator commands it

    // Command State = what the operator wants the system to do.
    // Add more actions here if desired.
	public enum AutoDriveCmdState{
		MANUAL(0), // operator has full control over drivetrain
		DRIVE_TO_CENTER(1), // Driver wants robot to drive to the center of the field
		DO_A_BARREL_ROLL(2); // Driver wants to do a defense avoidance spin-to-the-right move

		public final int value;

		private AutoDriveCmdState(int value) {
			this.value = value;
		}

		public int toInt() {
			return this.value;
		}
	}

    // State of the auto-drive algorithm's state machine
    public enum AutoDriveState{ 
		MANUAL(0), // Passing driver commands through to the drivetrain
		GENERATING_TRAJECTORY(1), //Driver commands still active, but a trajectory is being generated in the background
		RUNNING_TRAJECTORY(2), //Trajectory is available, and is currently being run.
        DONE(3); //Trajectory has finished. Waiting for next command.

		public final int value;

		private AutoDriveState(int value) {
			this.value = value;
		}

		public int toInt() {
			return this.value;
		}
	}

    double manualFwdRevCmd = 0;
    double manualStrafeCmd = 0;
    double manualRotateCmd = 0;
    boolean manualFieldRelativeCmd = false;

    AutoDriveCmdState curCmd = AutoDriveCmdState.MANUAL;
    AutoDriveCmdState prevCmd = AutoDriveCmdState.MANUAL;

    AutoDriveState curState = AutoDriveState.MANUAL;
    AutoDriveState prevState = AutoDriveState.MANUAL;

    DynamicSwerveTrajectoryGenerator curTraj = null;



    public AutoDrive(){

    }

    public void setManualCommands(double fwdRevCmd, double strafeCmd, double rotateCmd, boolean fieldRelativeCmd){
        manualFwdRevCmd = fwdRevCmd;
        manualStrafeCmd = strafeCmd;
        manualRotateCmd = rotateCmd;
        manualFieldRelativeCmd = fieldRelativeCmd;
    }

    public void setCmd(AutoDriveCmdState cmd){
        curCmd = cmd;
    }

    public void update(){
        DrivetrainControl dt = DrivetrainControl.getInstance();

        // Update state machine
        if(curCmd == AutoDriveCmdState.MANUAL){
            //Manual always takes prescedence
            curState = AutoDriveState.MANUAL;
        } else if ( curCmd != prevCmd ){
            //Command has changed, initiate a trajectory calculation

            System.out.println("[AutoDrive] New Auto Drive Trajectory generation starting...");
            
            DynamicSwerveWaypointSet waypoints = new DynamicSwerveWaypointSet();

            //TODO - we assume the start of the trajectory is where we're at now, not where
            // we're actually at once the trajectory starts. todo... maybe mark the
            // current time and advance the time of the trajectory by the calculation time?
            waypoints.start = dt.getCurEstPose();
            waypoints.startRot = dt.getCurEstPose().getRotation();
            waypoints.interiorWaypoints = new ArrayList<Translation2d>();

            //TODO rotation2d for start/end needs to be along the vector from start to end

            // Pick waypoint ends based on the command
            if(curCmd == AutoDriveCmdState.DRIVE_TO_CENTER){
                waypoints.end = new Pose2d( new Translation2d(Units.feetToMeters(54/2), Units.feetToMeters(27/2)), waypoints.startRot);
                waypoints.endRot = waypoints.startRot;
            } else if(curCmd == AutoDriveCmdState.DO_A_BARREL_ROLL){
                waypoints.end = waypoints.start.transformBy(new Transform2d( new Translation2d(3.0, 3.0), new Rotation2d()));
                waypoints.endRot = waypoints.startRot.plus(Rotation2d.fromDegrees(180.0));

            } else {
                //should never get here, so just pick a sane default of no trajectory.
                waypoints.end = waypoints.start;
                waypoints.endRot = waypoints.startRot;
            }

            // Start the dynamic generation
            curTraj = new DynamicSwerveTrajectoryGenerator();
            curTraj.startGeneration(waypoints);

            
            curState = AutoDriveState.GENERATING_TRAJECTORY;
        } else if (curState == AutoDriveState.GENERATING_TRAJECTORY){
            if(curTraj.isReady()){
                System.out.println("[AutoDrive] Starting trajectory...");
                curTraj.startTrajectory();
                curState = AutoDriveState.RUNNING_TRAJECTORY;
            }
        } else if(curState == AutoDriveState.RUNNING_TRAJECTORY){
            if(curTraj.isFinished()){
                System.out.println("[AutoDrive] Trajectory Complete.");
                curState = AutoDriveState.DONE;

            }
        }


        // Send outputs to the drivetrain
        if(curState == AutoDriveState.MANUAL || curState == AutoDriveState.GENERATING_TRAJECTORY){
            if(manualFieldRelativeCmd){
                dt.setCmdFieldRelative(manualFwdRevCmd, manualStrafeCmd, manualRotateCmd);
            } else {
                dt.setCmdRobotRelative(manualFwdRevCmd, manualStrafeCmd, manualRotateCmd);
            }
        } else if(curState == AutoDriveState.RUNNING_TRAJECTORY) {
            dt.setCmdTrajectory(curTraj.getCurCmd());

        } else {
            dt.setCmdRobotRelative(0, 0, 0);
        }


        // Update previous 
        prevState = curState;
        prevCmd = curCmd;
    }
    
}
