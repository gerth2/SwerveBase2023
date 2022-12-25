package frc.hardwareWrappers.Gyro;

import edu.wpi.first.math.geometry.Rotation2d;
import frc.hardwareWrappers.Gyro.ADXRS453.RealADXRS453;
import frc.lib.Signal.Annotations.Signal;
import frc.robot.Robot;

public class WrapperedGyro  {

    AbstractGyro gyro;
    double offset_rad = 0;

    public enum GyroType {
        ADXRS453,
        NAVX
    }

    @Signal(units = "rad")
    private double curAngle_rad;

    public WrapperedGyro(GyroType type){
        if(Robot.isReal()){
            if(type == GyroType.ADXRS453){
                gyro = new RealADXRS453();
            } else if (type == GyroType.NAVX){

            }
        } else {
            gyro = new SimGyro();
        }
    }

    public void update(){
        // Gyros are inverted in reference frame (positive clockwise)
        // and we maintain our own offset in code when rezeroing.
        curAngle_rad = gyro.getRawAngle() * -1.0 + offset_rad;
    }

    public void reset(double curAngle_rad) {
        offset_rad = curAngle_rad;
        gyro.reset();
    }

    public void calibrate() {
        gyro.calibrate();
    }

    public double getRate_radpersec() {
        return gyro.getRate();
    }

    public double getAngle_rad() {
        return curAngle_rad;
    }

    public boolean isConnected() {
        return gyro.isConnected();
    }

    public Rotation2d getRotation2d() {
        return new Rotation2d(this.getAngle_rad());
    }
    
}
