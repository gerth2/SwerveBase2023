package frc.lib.Signal;

import edu.wpi.first.networktables.DoublePublisher;
import edu.wpi.first.networktables.DoubleTopic;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.PubSubOption;
import edu.wpi.first.networktables.TimestampedDouble;
import edu.wpi.first.networktables.Topic;

public class Signal {

    String name;
    String units;
    DoubleTopic nt4ValTopic;
    DoublePublisher nt4ValPublisher;

    /**
     * Class which describes one line on a plot
     * 
     * @param name_in  String of what to call the signal (human readable)
     * @param units_in units the signal is in.
     */
    public Signal(String name_in, String units_in) {
        name = name_in;
        units = units_in;

        NetworkTableInstance inst = NetworkTableInstance.getDefault();


        nt4ValTopic   = inst.getDoubleTopic(this.getNT4ValueTopicName());
        nt4ValTopic.setProperty("units", units);

        //The goal of a signal is to record the value of a variable every loop, for debugging down to loop-to-loop changes
        // Therefor we do want to send all vlaues over the network, and we do want to keep any duplicates.
        nt4ValPublisher = nt4ValTopic.publish(PubSubOption.sendAll(true), PubSubOption.keepDuplicates(true));
        SignalWrangler.getInstance().register(this);
    }

    /**
     * Adds a new sample to the signal queue. It is intended that the controls code
     * would call this once per loop to add a new datapoint to the real-time graph.
     * 
     * The boolean version converts true to 1.0 and false to 0.0.
     * 
     * @param time_in
     * @param value_in
     */
    public void addSample(double time_in_sec, boolean value_in) {
        this.addSample(time_in_sec, value_in ? 1.0 : 0.0);
    }

    /**
     * Adds a new sample to the signal queue. It is intended that the controls code
     * would call this once per loop to add a new datapoint to the real-time graph.
     * 
     * @param time_in
     * @param value_in
     */
    public void addSample(double time_in_sec, double value_in) {
        SignalWrangler.getInstance().logger.addSample(new DataSample(time_in_sec, value_in, this));
        nt4ValPublisher.set(value_in, Math.round(time_in_sec*1000000l));
    }

    /**
     * @return The User-friendly name of the signal
     */
    public String getName() {
        return name;
    }

    /**
     * @return The name of the units the signal is measured in.
     */
    public String getUnits() {
        return units;
    }

    public String getNT4ValueTopicName(){ return SignalUtils.nameToNT4ValueTopic(this.name); }


}
