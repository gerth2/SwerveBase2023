package frc.robot.Drivetrain;


import java.util.ArrayList;
import java.util.List;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.util.Units;

import frc.Constants;
import frc.hardwareWrappers.Gyro.WrapperedGyro;
import frc.hardwareWrappers.Gyro.WrapperedGyro.GyroType;
import frc.lib.Signal.Annotations.Signal;
import frc.robot.Drivetrain.Camera.PhotonCamWrapper;

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

    WrapperedGyro gyro;

    //SwerveDrivePoseEstimator<N7, N7, N5> m_poseEstimator;

    SwerveDrivePoseEstimator m_poseEstimator;

    List<PhotonCamWrapper> cams = new ArrayList<PhotonCamWrapper>();

    //Trustworthiness of the internal model of how motors should be moving
    // Measured in expected standard deviation (meters of position and degrees of rotation)
    Matrix<N3, N1> stateStdDevs = VecBuilder.fill(0.1, 0.1, 0.1);

    //Trustworthiness of the vision system
    // Measured in expected standard deviation (meters of position and degrees of rotation)
    Matrix<N3, N1>  visionMeasurementStdDevs = VecBuilder.fill(0.9, 0.9, 0.9);



    @Signal(units = "ft/sec")
    double curSpeed = 0;

    private DrivetrainPoseEstimator(){

        cams.add(new PhotonCamWrapper("FRONT_CAM", Constants.robotToFrontCameraTrans)); 
        cams.add(new PhotonCamWrapper("REAR_CAM", Constants.robotToRearCameraTrans)); 
        //TODO add more cameras here

        gyro = new WrapperedGyro(GyroType.ADXRS453);

        //Temp default - will poopulate with real valeus in the resetPosition method
        SwerveModulePosition[] initialStates = {new SwerveModulePosition(),new SwerveModulePosition(),new SwerveModulePosition(),new SwerveModulePosition()};

        m_poseEstimator = new SwerveDrivePoseEstimator(
            Constants.m_kinematics,
            new Rotation2d(),
            initialStates,
            new Pose2d(),
            stateStdDevs,
            visionMeasurementStdDevs
            );

    }

    /**
     * Snap update the estimator to a known pose.
     * @param in known pose
     */
    public void setKnownPose(Pose2d in){
        DrivetrainControl.getInstance().resetWheelEncoders();
        gyro.reset(in.getRotation().getRadians());
        var states = DrivetrainControl.getInstance().getModuleActualPositions();
        
        m_poseEstimator.resetPosition(in.getRotation(), states, in);
        curEstPose = in;
    }

    public Pose2d getEstPose(){ return curEstPose; }

    public void update(){

        // Handle gyro-related update tasks
        gyro.update();

        //Based on gyro and measured module speeds and positions, estimate where our robot should have moved to.
        SwerveModulePosition[] positions = DrivetrainControl.getInstance().getModuleActualPositions();
        Pose2d prevEstPose = curEstPose;
        curEstPose = m_poseEstimator.update(getGyroHeading(), positions);

        //Calculate a "speedometer" velocity in ft/sec
        Transform2d deltaPose = new Transform2d(prevEstPose, curEstPose);
        curSpeed = Units.metersToFeet(deltaPose.getTranslation().getNorm()) / Constants.Ts;

        for(var cam : cams){
            cam.update();
            for(var obs : cam.getCurObservations()){
                m_poseEstimator.addVisionMeasurement(obs.estFieldPose, obs.time, visionMeasurementStdDevs.times(1.0/obs.trustworthiness));
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