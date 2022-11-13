package frc.robot.Drivetrain;


import java.util.ArrayList;
import java.util.List;

import edu.wpi.first.math.Nat;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.Vector;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.util.Units;

import frc.Constants;
import frc.lib.Signal.Annotations.Signal;
import frc.robot.Drivetrain.Camera.CasserolePhotonCam;
import frc.wrappers.ADXRS453.CasseroleADXRS453;

public class DrivetrainPoseEstimator {

    /* Singleton infrastructure */
    private static DrivetrainPoseEstimator instance;
    public static DrivetrainPoseEstimator getInstance() {
        if (instance == null) {
            instance = new DrivetrainPoseEstimator();
        }
        return instance;
    }

    Pose2d curEstPose = new Pose2d(Constants.DFLT_START_POSE.getTranslation(), Constants.DFLT_START_POSE.getRotation());

    CasseroleADXRS453 gyro;

    //SwerveDrivePoseEstimator m_poseEstimator;

    List<CasserolePhotonCam> cams = new ArrayList<CasserolePhotonCam>();

    //Trustworthiness of the internal model of how motors should be moving
    // Measured in expected standard deviation (meters of position and degrees of rotation)
    Vector<N3> stateStdDevs  = VecBuilder.fill(0.05, 0.05, Units.degreesToRadians(5));

    //Trustworthiness of gyro in radians of standard deviation.
    Vector<N1> localMeasurementStdDevs  = VecBuilder.fill(Units.degreesToRadians(0.1));

    //Trustworthiness of the vision system
    // Measured in expected standard deviation (meters of position and degrees of rotation)
    Vector<N3> visionMeasurementStdDevs = VecBuilder.fill(0.01, 0.01, Units.degreesToRadians(0.1));

    @Signal(units = "ft/sec")
    double curSpeed = 0;

    private DrivetrainPoseEstimator(){

        cams.add(new CasserolePhotonCam("FRONT_CAM", Constants.robotToFrontCameraTrans)); 
        cams.add(new CasserolePhotonCam("REAR_CAM", Constants.robotToRearCameraTrans)); 
        //TODO add more cameras here

        gyro = new CasseroleADXRS453();

        SwerveModuleState[] initialStates = DrivetrainControl.getInstance().getModuleActualStates();


        //m_poseEstimator = new SwerveDrivePoseEstimator( Nat.N7(), Nat.N5(), Nat.N7(),
        //                                               getGyroHeading(), 
        //                                               Constants.DFLT_START_POSE, 
        //                                               initialStates,
        //                                               Constants.m_kinematics, 
        //                                               stateStdDevs, 
        //                                               localMeasurementStdDevs, 
        //                                               visionMeasurementStdDevs, 
        //                                               Constants.Ts);

    }

    /**
     * Snap update the estimator to a known pose.
     * @param in known pose
     */
    public void setKnownPose(Pose2d in){
        DrivetrainControl.getInstance().resetWheelEncoders();
        gyro.reset(in.getRotation().getRadians());
        //m_poseEstimator.resetPosition(in, in.getRotation());
        curEstPose = in;
    }

    public Pose2d getEstPose(){ return curEstPose; }

    public void update(){

        // Handle gyro-related update tasks
        gyro.update();

        //Based on gyro and measured module speeds and positions, estimate where our robot should have moved to.
        SwerveModuleState[] states = DrivetrainControl.getInstance().getModuleActualStates();
        Pose2d prevEstPose = curEstPose;
        //curEstPose = m_poseEstimator.update(getGyroHeading(), states[0], states[1], states[2], states[3]);

        //Calculate a "speedometer" velocity in ft/sec
        Transform2d deltaPose = new Transform2d(prevEstPose, curEstPose);
        curSpeed = Units.metersToFeet(deltaPose.getTranslation().getNorm()) / Constants.Ts;

        for(var cam : cams){
            cam.update();
            for(var obs : cam.getCurObservations()){
                //m_poseEstimator.addVisionMeasurement(obs.estFieldPose, obs.time, visionMeasurementStdDevs.times(1.0/obs.trustworthiness));
            }
        }

    }

    public Rotation2d getGyroHeading(){
        return gyro.getRotation2d();
    }

    public double getSpeedFtpSec(){
        return curSpeed;
    }

    public boolean getVisionTargetsVisible(){
        for(var cam:cams){
            if(cam.getCurTargetCount() > 0){
                return true;
            }
        }
        return false;
    }


}