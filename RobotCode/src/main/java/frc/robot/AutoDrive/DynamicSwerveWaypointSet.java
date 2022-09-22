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

    /**
     * Starts a waypoint set with nothing configured. Requires the user manually specify each component later on.
     */
    public DynamicSwerveWaypointSet(){};

    /**
     * Init's a set of waypoints that goes from one transaltion ( just x/y coordinates) to another, adjusting the rotations to match 
     * a minimal-movement vector betweeen the start/end points
     * @param startPosition
     * @param endPosition
     */
    public DynamicSwerveWaypointSet(Translation2d startPosition, Translation2d endPosition){

    }

    /**
     * Init's a complete waypoint set, where the user manually specifies (up front) all of the starting and ending
     * Poses and rotations.
     * @param start
     * @param startRot
     * @param interiorWaypoints
     * @param end
     * @param endRot
     */
    public DynamicSwerveWaypointSet(Pose2d start, Rotation2d startRot, List<Translation2d> interiorWaypoints,  Pose2d end, Rotation2d endRot){
        this.start = start;
        this.startRot = startRot;
        this.interiorWaypoints = interiorWaypoints;
        this.end = end;
        this.endRot = endRot;
    }

    
}
