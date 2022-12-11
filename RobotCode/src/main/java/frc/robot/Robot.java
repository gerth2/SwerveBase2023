// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;

import org.photonvision.PhotonCamera;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.TimedRobot;
import frc.lib.Calibration.CalWrangler;
import frc.lib.LoadMon.CasseroleRIOLoadMonitor;
import frc.lib.LoadMon.SegmentTimeTracker;
import frc.lib.Signal.SignalWrangler;
import frc.lib.Signal.Annotations.Signal;
import frc.lib.Webserver2.Webserver2;
import frc.robot.AutoDrive.AutoDrive;
import frc.robot.AutoDrive.AutoDrive.AutoDriveCmdState;
import frc.robot.Autonomous.Autonomous;
import frc.robot.Drivetrain.DrivetrainControl;
import frc.sim.RobotModel;

/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the name of this class or
 * the package after creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {

  public static double loopStartTime;

  ///////////////////////////////////////////////////////////////////
  // Instatntiate new classes after here 
  // ...

  // Website utilities
  Webserver2 webserver;
  Dashboard db;
  CalWrangler cw;

  // Things
  CasseroleRIOLoadMonitor loadMon;
  BatteryMonitor batMan;

  // Main Driver
  DriverInput di;

  //Drivetrain and drivetrain accessories
  DrivetrainControl dt;
  AutoDrive ad;

  // Autonomous Control Utilities
  Autonomous auto;
  PoseTelemetry pt;

  SegmentTimeTracker stt;

  @Signal(units = "sec")
  double mainLoopDuration;
  @Signal(units = "sec")
  double mainLoopPeriod;

  final double ANGULAR_P = 0.1;
  final double ANGULAR_D = 0.0;
  PIDController turnController = new PIDController(ANGULAR_P, 0, ANGULAR_D);
  // ... 
  // But before here
  ///////////////////////////////////////////////////////////////////


  ///////////////////////////////////////////////////////////////////
  // Do one-time initilization here
  ///////////////////////////////////////////////////////////////////
  @Override
  public void robotInit() {

    stt = new SegmentTimeTracker("Robot.java", 0.25);

    stt.start();

    // Disable default behavior of the live-window output manipulation logic
    // We've got our own and never use this anyway.
    LiveWindow.setEnabled(false);
    LiveWindow.disableAllTelemetry();
    stt.mark("LW Disable");

    NetworkTableInstance.getDefault().startServer();
    stt.mark("NT4");


    /* Init website utilties */
    webserver = new Webserver2();
    stt.mark("Webserver2");

    cw = CalWrangler.getInstance();
    stt.mark("Cal Wrangler");

    loadMon = new CasseroleRIOLoadMonitor();
    stt.mark("RIO Load Monitor");

    batMan = BatteryMonitor.getInstance();
    stt.mark("Battery Monitor");

    //bcd = new Ballcolordetector();
    stt.mark("Ball Color Detector");

    di = new DriverInput(0);
    stt.mark("Driver IO");

    dt = DrivetrainControl.getInstance();
    ad = new AutoDrive();
    stt.mark("Drivetrain Control");

    auto = Autonomous.getInstance();
    auto.loadSequencer();
    stt.mark("Autonomous");

    pt = PoseTelemetry.getInstance();
    stt.mark("Pose Telemetry");

    db = new Dashboard(webserver);
    stt.mark("Dashboard");

    if(Robot.isSimulation()){
      simulationSetup();
    }
    syncSimPoseToEstimate();
    stt.mark("Simulation");

    SignalWrangler.getInstance().registerSignals(this);
    stt.mark("Signal Registration");

    webserver.startServer();
    stt.mark("Webserver Startup");

    PhotonCamera.setVersionCheckEnabled(false);
    stt.mark("Photonvision Config");

    System.gc();
    stt.mark("Post Init GC");

    System.out.println("Init Stats:");
    stt.end();

  }


  ///////////////////////////////////////////////////////////////////
  // Autonomous-Specific
  ///////////////////////////////////////////////////////////////////
  @Override
  public void autonomousInit() {
    SignalWrangler.getInstance().logger.startLoggingAuto();
    //Reset sequencer
    auto.reset();
    auto.startSequencer();

    // Ensure simulation resets to correct pose at the start of autonomous
    syncSimPoseToEstimate();

  }

  @Override
  public void autonomousPeriodic() {
    stt.start();
    loopStartTime = Timer.getFPGATimestamp();

    //Step the sequencer forward
    auto.update();
    stt.mark("Auto Update");

  }

  
  ///////////////////////////////////////////////////////////////////
  // Teleop-Specific
  ///////////////////////////////////////////////////////////////////
  @Override
  public void teleopInit() {
   
    SignalWrangler.getInstance().logger.startLoggingTeleop();
  }

  @Override
  public void teleopPeriodic() {
    stt.start();
    loopStartTime = Timer.getFPGATimestamp();

    di.update();
    stt.mark("Driver Input");

    /////////////////////////////////////
    // Drivetrain Input Mapping

    if(di.getSpinMoveCmd()){
      ad.setCmd(AutoDriveCmdState.DO_A_BARREL_ROLL);
    } else if (di.getDriveToCenterCmd()){
      ad.setCmd(AutoDriveCmdState.DRIVE_TO_CENTER);
    } else {
      ad.setCmd(AutoDriveCmdState.MANUAL);
    }

    ad.setManualCommands(di.getFwdRevCmd_mps(), di.getSideToSideCmd_mps(), di.getRotateCmd_rps(), !di.getRobotRelative());

    ad.update();


    if(di.getOdoResetCmd()){
      //Reset pose estimate to angle 0, but at the same translation we're at
      Pose2d newPose = new Pose2d(dt.getCurEstPose().getTranslation(), new Rotation2d(0.0));
      dt.setKnownPose(newPose);
    }


    stt.mark("Human Input Mapping");

  }



  ///////////////////////////////////////////////////////////////////
  // Disabled-Specific
  ///////////////////////////////////////////////////////////////////
  @Override
  public void disabledInit() {
    SignalWrangler.getInstance().logger.stopLogging();
  }

  @Override
  public void disabledPeriodic() {
    stt.start();
    loopStartTime = Timer.getFPGATimestamp();

    
    dt.calUpdate(false);
    stt.mark("Cal Updates");

    auto.sampleDashboardSelector();
    stt.mark("Auto Mode Update");

  }



  
  ///////////////////////////////////////////////////////////////////
  // Common Periodic updates
  ///////////////////////////////////////////////////////////////////
  @Override
  public void robotPeriodic() {


    if(DriverStation.isTest() && !DriverStation.isDisabled()){
      dt.testUpdate();
    } else {
      dt.update();
    }
    stt.mark("Drivetrain");


    cw.update();
    stt.mark("Cal Wrangler");
    db.updateDriverView();
    stt.mark("Dashboard");
    telemetryUpdate();
    stt.mark("Telemetry");
    

    stt.end();


  }

  private void telemetryUpdate(){
    double time = loopStartTime;

    dt.updateTelemetry();

    pt.setDesiredPose(dt.getCurDesiredPose());
    pt.setEstimatedPose(dt.getCurEstPose());
    
    pt.update(time);

    mainLoopDuration = stt.loopDurationSec;
    mainLoopPeriod = stt.loopPeriodSec;

    SignalWrangler.getInstance().sampleAllSignals(time);
  }

  ///////////////////////////////////////////////////////////////////
  // Test-Mode-Specific
  ///////////////////////////////////////////////////////////////////

  @Override
  public void testInit(){
    // Tell the subsystems that care that we're entering test mode.
    dt.testInit();
  }

  @Override
  public void testPeriodic(){
    stt.start();
    loopStartTime = Timer.getFPGATimestamp();


    // Nothing special here, yet
  }

  ///////////////////////////////////////////////////////////////////
  // Simulation Support
  ///////////////////////////////////////////////////////////////////

  RobotModel plant;

  public void simulationSetup(){
    plant = new RobotModel();
    syncSimPoseToEstimate();
  }

  public void syncSimPoseToEstimate(){
    if(Robot.isSimulation()){
      plant.reset(dt.getCurEstPose());
    }
  }

  @Override
  public void simulationPeriodic(){
    plant.update(this.isDisabled());
    pt.setActualPose(plant.getCurActPose());
  }


}
