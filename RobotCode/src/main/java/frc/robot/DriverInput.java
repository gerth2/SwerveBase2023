package frc.robot;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.filter.Debouncer.DebounceType;
import edu.wpi.first.wpilibj.XboxController;
import frc.Constants;
import frc.lib.Calibration.Calibration;
import frc.lib.Signal.Annotations.Signal;

public class DriverInput {
    
    XboxController driverController;

    SlewRateLimiter fwdRevSlewLimiter;
    SlewRateLimiter rotSlewLimiter;
    SlewRateLimiter sideToSideSlewLimiter;

    Calibration stickDeadband;
    Calibration fwdRevSlewRate;
    Calibration rotSlewRate;
    Calibration sideToSideSlewRate;
    Calibration translateCmdScalar;
    Calibration rotateCmdScalar;
    
    @Signal(units="cmd")
    double curFwdRevCmd;
    @Signal(units="cmd")
    double curRotCmd;
    @Signal(units="cmd")
    double curSideToSideCmd;

    @Signal(units="bool")
    boolean robotRelative;
   
    @Signal (units="cmd")
    double fwdRevSlewCmd;
    @Signal (units="cmd")
    double rotSlewCmd;
    @Signal (units="cmd")
    double sideToSideSlewCmd;

    @Signal(units="bool")
    boolean spinMoveCmd;
    @Signal(units="bool")
    boolean driveToCenterCmd;

    @Signal(units="bool")
    boolean resetOdometry;
    @Signal(units="bool")
    boolean isConnected;

    Debouncer resetOdoDbnc = new Debouncer(0.5, DebounceType.kRising);

    String getName(int idx){
        return "Driver Ctrl " + Integer.toString(idx) + " ";
    }

    public DriverInput(int controllerIdx){

        driverController = new XboxController(controllerIdx);

        stickDeadband = new Calibration(getName(controllerIdx) + "StickDeadBand", "", 0.1);

        fwdRevSlewRate = new Calibration(getName(controllerIdx) + "fwdRevSlewRate_", "", 1);
        rotSlewRate = new Calibration(getName(controllerIdx) + "rotSlewRate", "", 1);
        sideToSideSlewRate = new Calibration(getName(controllerIdx) + "sideToSideSlewRate", "", 1);
        translateCmdScalar = new Calibration(getName(controllerIdx) + "translateCmdScalar", "", 0.8);
        rotateCmdScalar = new Calibration(getName(controllerIdx) + "rotateCmdScalar", "", 0.8);

        fwdRevSlewLimiter = new SlewRateLimiter(fwdRevSlewRate.get());
        rotSlewLimiter = new SlewRateLimiter(rotSlewRate.get());
        sideToSideSlewLimiter = new SlewRateLimiter(sideToSideSlewRate.get());

    }

    public void update(){

        isConnected = driverController.isConnected();

        if(isConnected){

            
            curFwdRevCmd = -1.0 * driverController.getLeftY();
            curRotCmd = -1.0 * driverController.getRightX();
            curSideToSideCmd = -1.0 * driverController.getLeftX();

            curFwdRevCmd = MathUtil.applyDeadband( curFwdRevCmd,stickDeadband.get()) * translateCmdScalar.get(); 
            curRotCmd = MathUtil.applyDeadband( curRotCmd,stickDeadband.get())  * rotateCmdScalar.get();
            curSideToSideCmd = MathUtil.applyDeadband( curSideToSideCmd,stickDeadband.get())  * translateCmdScalar.get();

            if(driverController.getLeftStickButton()){
                curFwdRevCmd = curFwdRevCmd / 2.0;
                curSideToSideCmd = curSideToSideCmd / 2.0;
            } else if(driverController.getRightStickButton()) {
                curRotCmd = curRotCmd / 2.0;
            }
            
            fwdRevSlewCmd = fwdRevSlewLimiter.calculate(curFwdRevCmd);
            rotSlewCmd = rotSlewLimiter.calculate(curRotCmd);
            sideToSideSlewCmd = sideToSideSlewLimiter.calculate(curSideToSideCmd);
            
            robotRelative = driverController.getRightBumper();

            resetOdometry = resetOdoDbnc.calculate(driverController.getAButton());

            spinMoveCmd = driverController.getBButton();
            driveToCenterCmd = driverController.getXButton();
 
           

        } else {
            //Controller Unplugged Defaults
            curFwdRevCmd = 0.0;
            curRotCmd = 0.0; 
            curSideToSideCmd = 0.0; 
            robotRelative = false;
            resetOdometry = false;
        }

        
        if(fwdRevSlewRate.isChanged() ||
           rotSlewRate.isChanged() ||
           sideToSideSlewRate.isChanged()) {
                fwdRevSlewRate.acknowledgeValUpdate();
                rotSlewRate.acknowledgeValUpdate();
                sideToSideSlewRate.acknowledgeValUpdate();
                fwdRevSlewLimiter = new SlewRateLimiter(fwdRevSlewRate.get());
                rotSlewLimiter = new SlewRateLimiter(rotSlewRate.get());
                sideToSideSlewLimiter = new SlewRateLimiter(sideToSideSlewRate.get());
        }
               
           
        
    }

    /**
     * Gets the driver command for fwd/rev
     * 1.0 means "fast as possible forward"
     * 0.0 means stop
     * -1.0 means "fast as possible reverse"
     * @return 
     */
    public double getFwdRevCmd_mps(){
        return fwdRevSlewCmd * Constants.MAX_FWD_REV_SPEED_MPS;
    }

    /**
     * Gets the driver command for rotate
     * 1.0 means "fast as possible to the left"
     * 0.0 means stop
     * -1.0 means "fast as possible to the right"
     * @return 
     */
    public double getRotateCmd_rps(){
        return rotSlewCmd * Constants.MAX_ROTATE_SPEED_RAD_PER_SEC;
    }
    public double getSideToSideCmd_mps(){
        return sideToSideSlewCmd * Constants.MAX_FWD_REV_SPEED_MPS;
    }


    public boolean getRobotRelative(){
        return robotRelative;
    }


    public boolean getOdoResetCmd(){
        return resetOdometry;
    }

    public boolean getSpinMoveCmd(){
        return spinMoveCmd;
    }

    public boolean getDriveToCenterCmd(){
        return driveToCenterCmd;
    }

}