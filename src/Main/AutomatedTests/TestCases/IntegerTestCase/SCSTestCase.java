package Main.AutomatedTests.TestCases.IntegerTestCase;

import Main.Algorithms.Other.Random.IRange;
import Main.Algorithms.Other.Random.DRange;

/**
 * Created by IMN on 1/7/2017.
 */
public class SCSTestCase {
    public int customerQty;
    public int vehicleQty;
    public double fixCost;
    public DRange capacityRange;
    public IRange processTimeRange;
    public DRange dueDateRange;
    public DRange deadLineRange;
    public IRange penaltyRange;
    public IRange edgeWeightRange;
    public IRange maxGainRange;

    public SCSTestCase(int customerQty, int vehicleQty, double fixCost,
                       DRange capacityRange, IRange processTimeRange, DRange dueDateRange, DRange deadLineRange,
                       IRange penaltyRange, IRange edgeWeightRange, IRange maxGainRange) {

        this.customerQty = customerQty;
        this.vehicleQty = vehicleQty;
        this.fixCost = fixCost;
        this.capacityRange = capacityRange;
        this.processTimeRange = processTimeRange;
        this.dueDateRange = dueDateRange;
        this.deadLineRange = deadLineRange;
        this.penaltyRange = penaltyRange;
        this.edgeWeightRange = edgeWeightRange;
        this.maxGainRange = maxGainRange;
    }

    public static String getTableHeader(){
        return "customerQty,vehicleQty,fixCost,capacityRange,processTimeRange,dueDateRange,penaltyRange,edgeWeightRange";
    }

    public String getCSVRow() {
        return "" + customerQty + "," + vehicleQty + "," + fixCost + "," + capacityRange + ","
                + processTimeRange + "," + dueDateRange + "," + penaltyRange + "," + edgeWeightRange;
    }
}
