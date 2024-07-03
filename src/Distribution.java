import java.util.List;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class Distribution {

    private String name;
    private List<Float> data;

    public Distribution(String name, List<Float> data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public float getMean() {
        float sum = 0;
        for (float value : data) {
            sum += value;
        }
        return sum / data.size();
    }

    public float getVariance() {
        float mean = this.getMean();
        float temp = 0;
        for (float value : data) {
            temp += (value - mean) * (value - mean);
        }
        return temp / data.size();
    }

    public int getN() {
        return data.size();
    }

    public void setData(List<Float> data) {
        this.data = data;
    }

    public List<Float> getData() {
        return data;
    }

    public void addData(float value) {
        data.add(value);
    }

    //t test p value
    public float p_value(Distribution other) {
        
        DescriptiveStatistics stats1 = new DescriptiveStatistics();
        for (float value : data) {
            stats1.addValue(value);
        }

        DescriptiveStatistics stats2 = new DescriptiveStatistics();
        for (float value : other.getData()) {
            stats2.addValue(value);
        }
        
        TTest tTest = new TTest();
        float p = (float) tTest.tTest(stats1, stats2);

        return p;
    }
}
