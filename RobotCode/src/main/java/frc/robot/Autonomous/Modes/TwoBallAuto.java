package frc.robot.Autonomous.Modes;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import frc.Constants;
import frc.lib.AutoSequencer.AutoSequencer;
import frc.lib.Autonomous.AutoMode;
import frc.robot.Autonomous.Events.AutoEventDriveTime;

public class TwoBallAuto extends AutoMode {

    AutoEventDriveTime driveFwd = null;
    AutoEventDriveTime driveRev = null;

    @Override
    public void addStepsToSequencer(AutoSequencer seq) {
        driveFwd = new AutoEventDriveTime(Constants.TAXI_DRIVE_TIME_S, Constants.TAXI_DRIVE_SPEED_MPS, this.getInitialPose().getRotation().getRadians());
        seq.addEvent(driveFwd); 
        driveRev = new AutoEventDriveTime(Constants.TAXI_DRIVE_TIME_S, -1.0 * Constants.TAXI_DRIVE_SPEED_MPS, this.getInitialPose().getRotation().getRadians());
        seq.addEvent(driveRev);     }

    @Override
    public Pose2d getInitialPose(){
        return new Pose2d(7.261, 4.741, Rotation2d.fromDegrees(155));
    }
    
}

