package frc.robot.Drivetrain.Camera;

import java.util.ArrayList;
import java.util.List;

import frc.Constants;

import org.photonvision.PhotonCamera;
import org.photonvision.targeting.PhotonTrackedTarget;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.wpilibj.Timer;
import frc.lib.Signal.Annotations.Signal;

public class CasserolePhotonCam {

    PhotonCamera cam;

    @Signal
    boolean isConnected;

    List<CameraPoseObservation> observations;

    final Pose2d fieldPose = new Pose2d(); //Field-referenced orign

    final Transform2d robotToCam;


    public CasserolePhotonCam(String cameraName, Transform2d robotToCam){
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
            Transform2d camToTargetTrans = t.getCameraToTarget();
            Transform2d targetTransform = Constants.VISION_FAR_TGT_LOCATION; // TODO - needs to be looked up by AprilTag identifier
            Pose2d targetPose = fieldPose.transformBy(targetTransform);
            Pose2d camPose = targetPose.transformBy(camToTargetTrans.inverse());
            Pose2d visionEstPose = camPose.transformBy(robotToCam.inverse());   
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
