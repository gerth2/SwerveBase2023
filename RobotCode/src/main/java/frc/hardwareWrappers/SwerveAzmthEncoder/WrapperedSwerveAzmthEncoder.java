package frc.hardwareWrappers.SwerveAzmthEncoder;

import edu.wpi.first.math.geometry.Rotation2d;
import frc.UnitUtils;
import frc.hardwareWrappers.SwerveAzmthEncoder.CANCoder.RealCANCoder;
import frc.hardwareWrappers.SwerveAzmthEncoder.SRXEncoder.RealSRXEncoder;
import frc.hardwareWrappers.SwerveAzmthEncoder.Sim.SimSwerveAzmthEncoder;
import frc.hardwareWrappers.SwerveAzmthEncoder.ThriftyEncoder.RealThriftyEncoder;
import frc.lib.Calibration.Calibration;
import frc.lib.Signal.Annotations.Signal;
import frc.robot.Robot;

public class WrapperedSwerveAzmthEncoder  {

    AbstractSwerveAzmthEncoder enc;

    public enum SwerveAzmthEncType {
        SRXEncoder,
        CANCoder,
        Thrifty
    }

    @Signal(units="rad")
    double curAngleRad;

    Calibration mountingOffsetCal;


    public WrapperedSwerveAzmthEncoder(SwerveAzmthEncType type, String prefix, int id, double dfltMountingOffset_rad){
        if(Robot.isReal()){
            switch(type){
                case SRXEncoder:
                    //ID = digital input
                    enc = new RealSRXEncoder(id);
                    break;
                case CANCoder:
                    //ID = CAN ID
                    enc = new RealCANCoder(id);
                    break;
                case Thrifty:
                    //ID = Analog Input
                    enc = new RealThriftyEncoder(id);
                    break;
            }
        } else {
            enc = new SimSwerveAzmthEncoder(id);
        }
        mountingOffsetCal = new Calibration(prefix + "MountingOffset", "rad", dfltMountingOffset_rad);
    }

    public void update(){
        curAngleRad = UnitUtils.wrapAngleRad( enc.getRawAngle_rad() - mountingOffsetCal.get());
    }

    public double getAngle_rad(){
        return curAngleRad;
    }

    public Rotation2d getRotation2d() {
        return new Rotation2d(this.getAngle_rad());
    }
    
}
