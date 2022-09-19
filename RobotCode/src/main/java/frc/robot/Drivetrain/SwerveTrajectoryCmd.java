package frc.robot.Drivetrain;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.trajectory.Trajectory;

public class SwerveTrajectoryCmd {

    public Trajectory.State desTrajState; 
    public Rotation2d desAngle; 
    public Rotation2d desAngVel;

    public SwerveTrajectoryCmd(Trajectory.State desTrajState, Rotation2d desAngle, Rotation2d desAngVel){
        this.desTrajState = desTrajState;
        this.desAngle = desAngle;
        this.desAngVel = desAngVel;
    }
    
}
