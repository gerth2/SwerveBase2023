package frc.robot.Drivetrain.Camera;

import java.util.ArrayList;
import java.util.List;

import frc.Constants;

import org.photonvision.PhotonCamera;
import org.photonvision.targeting.PhotonTrackedTarget;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.wpilibj.Timer;
import frc.lib.Signal.Annotations.Signal;

public class CasserolePhotonCam {

    PhotonCamera cam;

    @Signal
    boolean isConnected;

    List<CameraPoseObservation> observations;

    final Pose3d fieldPose = new Pose3d(); //Field-referenced orign

    final Transform3d robotToCam;


    public CasserolePhotonCam(String cameraName, Transform3d robotToCam){
        this.cam = new PhotonCamera(cameraName);
        this.robotToCam = robotToCam;
        this.observations = new ArrayList<CameraPoseObservation>();
    }

    public void update(){

        var res = cam.getLatestResult();
        double observationTime = Timer.getFPGATimestamp() - res.getLatencyMillis();

        List<PhotonTrackedTarget> tgtList = res.getTargets();

        observations = new ArrayList<CameraPoseObservation>();

        for(PhotonTrackedTarget t : tgtList){
            Transform3d camToTargetTrans = t.getBestCameraToTarget(); //TODO - better apriltag multiple pose arbitration strategy
            Transform3d targetTransform = Constants.VISION_NEAR_TGT_LOCATION; // TODO - needs to be looked up by AprilTag identifier
            Pose3d targetPose = fieldPose.transformBy(targetTransform);
            Pose3d camPose = targetPose.transformBy(camToTargetTrans.inverse());
            Pose2d visionEstPose = camPose.transformBy(robotToCam.inverse()).toPose2d();   
            observations.add(new CameraPoseObservation(observationTime, visionEstPose, 1.0)); //TODO - add trustworthiness scale by distance - further targets are less accurate  
        }
    }

    public List<CameraPoseObservation> getCurObservations(){
        return observations;
    }

    public int getCurTargetCount(){
        return observations.size();
    }

}
