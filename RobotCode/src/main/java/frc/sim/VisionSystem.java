package frc.sim;

import org.photonvision.SimVisionSystem;
import org.photonvision.SimVisionTarget;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.util.Units;
import frc.Constants;

public class VisionSystem {

    ////////////////////////////////////////////////////////////////
    // Simulated Vision System.

    //Configure these to match the targets we'll see on the field.
    Pose2d fieldOrigin = new Pose2d();
    Pose2d farTargetPose = fieldOrigin.transformBy(Constants.VISION_FAR_TGT_LOCATION);
    //TODO add more targets here

    //200mm AprilTags
    double targetHeightAboveGround = 1.0; // meters //TODO - is this right?
    double targetWidth = 0.2;             // meters
    double targetHeight = 0.2;            // meters

    final double MAX_CAM_DISTANCE_M = Units.feetToMeters(20);
    final double CAM_DIAG_FOV_DEG = 79.0;


    // Configure these to match your PhotonVision Camera,
    // pipeline, and LED setup.
    double camDiagFOV = 170.0; // degrees - assume wide-angle camera
    double camPitch = -5.0; // degrees
    double camHeightOffGround = 0.25; // meters
    double maxLEDRange = 20; // meters
    int camResolutionWidth = 640; // pixels
    int camResolutionHeight = 480; // pixels
    double minTargetArea = 10; // square pixels

    SimVisionSystem simVisionFront =
    new SimVisionSystem(
            "FRONT_CAM",
            camDiagFOV,
            camPitch,
            Constants.robotToFrontCameraTrans.inverse(),
            camHeightOffGround,
            maxLEDRange,
            camResolutionWidth,
            camResolutionHeight,
            minTargetArea);

    
    SimVisionSystem simVisionRear =
    new SimVisionSystem(
            "REAR_CAM",
            camDiagFOV,
            camPitch,
            Constants.robotToRearCameraTrans.inverse(),
            camHeightOffGround,
            maxLEDRange,
            camResolutionWidth,
            camResolutionHeight,
            minTargetArea);


    public VisionSystem(){
        simVisionFront.addSimVisionTarget(new SimVisionTarget(farTargetPose, targetHeight/2, targetWidth, targetHeight));
        simVisionRear.addSimVisionTarget(new SimVisionTarget(farTargetPose, targetHeight/2, targetWidth, targetHeight));
    }

    public void update(Pose2d curPose){
        simVisionFront.processFrame(curPose);
        simVisionRear.processFrame(curPose);
    }
    
}
