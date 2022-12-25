package frc.hardwareWrappers.Gyro.NavX;

import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.SPI.Port;
import frc.hardwareWrappers.Gyro.AbstractGyro;

public class RealNavx extends AbstractGyro {

    AHRS ahrs;

    public RealNavx(){
        ahrs = new AHRS(Port.kMXP);
        this.calibrate();
    }

    @Override
    public void reset() {
        ahrs.reset();
    }

    @Override
    public void calibrate() {
        System.out.println("======================================================");
        System.out.println("== GYRO: CALIBRATION IN PROCESS, DO NOT MOVE ROBOT...");
        ahrs.calibrate();
        System.out.println("== ... Complete!");
        System.out.println("======================================================");
    }

    @Override
    public double getRate() {
        return Units.degreesToRadians(ahrs.getRate());
    }

    @Override
    public double getRawAngle() {
        return Units.degreesToRadians(ahrs.getAngle());
    }

    @Override
    public boolean isConnected() {
        return ahrs.isConnected();
    }
    
}
