package frc.robot.AutoDrive;

import java.util.List;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;

public class DynamicSwerveWaypointSet {

    public Pose2d start;
    public Rotation2d startRot;
    public List<Translation2d> interiorWaypoints;
    public Pose2d end;
    public Rotation2d endRot;

    public DynamicSwerveWaypointSet(){};

    public DynamicSwerveWaypointSet(Pose2d start, Rotation2d startRot,   List<Translation2d> interiorWaypoints,  Pose2d end, Rotation2d endRot){
        this.start = start;
        this.startRot = startRot;
        this.interiorWaypoints = interiorWaypoints;
        this.end = end;
        this.endRot = endRot;
    }

    
}
