package frc.robot.Autonomous;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.networktables.IntegerPublisher;
import edu.wpi.first.networktables.IntegerSubscriber;
import edu.wpi.first.networktables.IntegerTopic;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.RobotController;
import frc.lib.AutoSequencer.AutoSequencer;
import frc.lib.Autonomous.AutoMode;
import frc.lib.Autonomous.AutoModeList;
import frc.lib.Util.CrashTracker;
import frc.robot.Autonomous.Modes.DoNothing;
import frc.robot.Autonomous.Modes.DriveFwd;
import frc.robot.Autonomous.Modes.Wait;
import frc.robot.Autonomous.Modes.Steak;
import frc.robot.Autonomous.Modes.TwoBallAuto;
import frc.robot.Drivetrain.DrivetrainControl;


/*
 *******************************************************************************************
 * Copyright (C) 2022 FRC Team 1736 Robot Casserole - www.robotcasserole.org
 *******************************************************************************************
 *
 * This software is released under the MIT Licence - see the license.txt
 *  file in the root of this repo.
 *
 * Non-legally-binding statement from Team 1736:
 *  Thank you for taking the time to read through our software! We hope you
 *    find it educational and informative! 
 *  Please feel free to snag our software for your own use in whatever project
 *    you have going on right now! We'd love to be able to help out! Shoot us 
 *    any questions you may have, all our contact info should be on our website
 *    (listed above).
 *  If you happen to end up using our software to make money, that is wonderful!
 *    Robot Casserole is always looking for more sponsors, so we'd be very appreciative
 *    if you would consider donating to our club to help further STEM education.
 */


public class Autonomous {

    IntegerTopic curDelayModeTopic;
    IntegerTopic curMainModeTopic;
    IntegerPublisher curDelayModePublisher;
    IntegerPublisher curMainModePublisher;

    IntegerTopic desDelayModeTopic;
    IntegerTopic desMainModeTopic;
    IntegerSubscriber desDelayModeSubscriber;
    IntegerSubscriber desMainModeSubscriber;

    long desDelayMode = 0;
    long desMainMode = 0;

    public AutoModeList mainModeList = new AutoModeList("main");
    public AutoModeList delayModeList = new AutoModeList("delay");

    AutoMode curDelayMode = null;
    AutoMode curMainMode = null;

    AutoMode prevDelayMode = null;
    AutoMode prevMainMode = null;

    
    /* Singleton infratructure*/
    private static Autonomous inst = null;
    public static synchronized Autonomous getInstance() {
        if (inst == null)
            inst = new Autonomous();
        return inst;
    }

    AutoSequencer seq;


    private Autonomous(){
        seq = new AutoSequencer("Autonomous");

        delayModeList.add(new Wait(0.0));
        delayModeList.add(new Wait(3.0));
        delayModeList.add(new Wait(6.0));
        delayModeList.add(new Wait(9.0));

        mainModeList.add(new Steak());
        mainModeList.add(new TwoBallAuto());
        mainModeList.add(new DriveFwd());
        mainModeList.add(new DoNothing());
        

        // Create and subscribe to NT4 topics
        NetworkTableInstance inst = NetworkTableInstance.getDefault();

        // Delay mode current/desired NT entries
        curDelayMode = delayModeList.getDefault();
        desDelayModeTopic = inst.getIntegerTopic(delayModeList.getDesModeTopicName());
        desDelayModeSubscriber = desDelayModeTopic.subscribe(curDelayMode.idx);
        curDelayModeTopic = inst.getIntegerTopic(delayModeList.getCurModeTopicName());
        curDelayModePublisher = curDelayModeTopic.publish();
        curDelayModePublisher.setDefault(curDelayMode.idx);

        // Main mode current/desired NT entries
        curMainMode  = mainModeList.getDefault();
        desMainModeTopic  = inst.getIntegerTopic(mainModeList.getDesModeTopicName());
        desMainModeSubscriber  = desMainModeTopic.subscribe(curMainMode.idx);
        curMainModeTopic  = inst.getIntegerTopic(mainModeList.getCurModeTopicName());
        curMainModePublisher = curMainModeTopic.publish();
        curMainModePublisher.setDefault(curMainMode.idx);

    }

    /* This should be called periodically in Disabled, and once in auto init */
    public void sampleDashboardSelector(){
        desDelayMode = desDelayModeSubscriber.get();
        desMainMode  = desMainModeSubscriber.get();
        curDelayMode = delayModeList.get((int)desDelayMode);
        curMainMode = mainModeList.get((int)desMainMode);	
        if(curDelayMode != prevDelayMode || curMainMode != prevMainMode){
            loadSequencer();
            prevDelayMode = curDelayMode;
            prevMainMode = curMainMode;
        }

        if(RobotController.getUserButton()) {
            DrivetrainControl.getInstance().setKnownPose(getStartPose());
        }
    }


    public void startSequencer(){
        sampleDashboardSelector(); //ensure it gets called once more
        DrivetrainControl.getInstance().setKnownPose(curMainMode.getInitialPose());
        if(curMainMode != null){
            seq.start();
        }
    }

    public void loadSequencer(){
        
        CrashTracker.logGenericMessage("Initing new auto routine " + curDelayMode.humanReadableName + "s delay, " + curMainMode.humanReadableName);

        seq.stop();
        seq.clearAllEvents();

        curDelayMode.addStepsToSequencer(seq);
        curMainMode.addStepsToSequencer(seq);

        curDelayModePublisher.set(desDelayMode);
        curMainModePublisher.set(desMainMode);
    
        DrivetrainControl.getInstance().setKnownPose(getStartPose());
        
    }


    /* This should be called periodically, always */
    public void update(){

        seq.update();
    }

    /* Should be called when returning to disabled to stop and reset everything */
    public void reset(){
        seq.stop();
        loadSequencer();
    }

    public boolean isActive(){
        return (seq.isRunning() && curMainMode != null);
    }

    public Pose2d getStartPose(){
        return curMainMode.getInitialPose();
    }


}