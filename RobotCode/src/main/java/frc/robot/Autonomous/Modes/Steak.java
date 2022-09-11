package frc.robot.Autonomous.Modes;
import edu.wpi.first.math.geometry.Pose2d;
import frc.Constants;
import frc.lib.AutoSequencer.AutoSequencer;
import frc.lib.Autonomous.AutoMode;
import frc.robot.Autonomous.Events.AutoEventJSONTrajectory;

public class Steak extends AutoMode {

    AutoEventJSONTrajectory driveEvent1 = null;
    AutoEventJSONTrajectory driveEvent2 = null;

    @Override
    public void addStepsToSequencer(AutoSequencer seq) {
        driveEvent1 = new AutoEventJSONTrajectory("many_Pickup1", 0.7);
        seq.addEvent(driveEvent1);
        driveEvent2 = new AutoEventJSONTrajectory("many_Pickup2", 0.65);
        seq.addEvent(driveEvent2);
       
    }

    @Override
    public Pose2d getInitialPose(){
        return driveEvent1.getInitialPose();
        
    }
    
}

