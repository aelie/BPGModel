package individual;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by aelie on 24/09/14.
 */
public class Variator {
    public static void main(String[] args) {
        String output_R = args[0];
        int maxTime = 50;
        int robustnessRuns = 30;
        int graphNumber = 25;
        try {
            PrintWriter pw_R = new PrintWriter(output_R, "UTF-8");
            pw_R.print("Parameters,Graph,T,Index,Robustness");
            for (int i = 0; i < 200; i++) {
                pw_R.print(",ESS" + i);
            }
            pw_R.println();
            PrintWriter pw_RD = new PrintWriter(output_R.split("\\.").length > 1 ? output_R.split("\\.")[0] + "D" + output_R.split("\\.")[1] : output_R + "D", "UTF-8");
            pw_RD.println("Parameters,I10%,F10%,D10%,I30%,F30%,D30%,Initial,Final,Difference");
            Simulator simulator;
            for (int applicationPoolSize = 100; applicationPoolSize < 1001; applicationPoolSize += 100) {
                for (int serverPoolSize = 50; serverPoolSize < 751 && serverPoolSize <= applicationPoolSize; serverPoolSize += 100) {
                    for (int servicePoolSize = 10; servicePoolSize < 311; servicePoolSize += 100) {
                        for (double mutationProbability = 0.2; mutationProbability < 0.7; mutationProbability += 0.1) {
                            for (int serverMaxConnexion = 10; serverMaxConnexion < 51; serverMaxConnexion += 10) {
                                String variables = applicationPoolSize + "_" +
                                        serverPoolSize + "_" +
                                        servicePoolSize + "_" +
                                        mutationProbability + "_" +
                                        serverMaxConnexion;
                                //I10, I30, I, F10, F30, F
                                List<SummaryStatistics> robustnesses = new ArrayList<>(Arrays.asList(new SummaryStatistics(), new SummaryStatistics(), new SummaryStatistics(),
                                        new SummaryStatistics(), new SummaryStatistics(), new SummaryStatistics()));
                                for (int graph = 0; graph < graphNumber; graph++) {
                                    System.out.println(variables + "/" + graph);
                                    simulator = Simulator.getInstance();
                                    simulator.setVariables(applicationPoolSize, serverPoolSize, servicePoolSize,
                                            mutationProbability, serverMaxConnexion,
                                            maxTime, robustnessRuns);
                                    simulator.warmup((int) System.currentTimeMillis(), true, true);
                                    simulator.start(false);
                                    Map<Integer, Map<Integer, List<Double>>> result = Simulator.getInstance().getRobustnessShuffleHistory();
                                    //initial
                                    for (Integer index : result.get(0).keySet()) {
                                        /*pw_R.print(variables + "," + graph + "," + 0 + "," + index);
                                        for (Double extinctionSequenceStep : result.get(0).get(index)) {
                                            pw_R.print("," + extinctionSequenceStep);
                                        }
                                        pw_R.println();*/
                                        List<Double> extinctionSequence = result.get(0).get(index);
                                        int index10 = (int)((extinctionSequence.size() - 1) * 1.0 / 10.0);
                                        int index30 = (int)((extinctionSequence.size() - 1) * 3.0 / 10.0);
                                        robustnesses.get(0).addValue(extinctionSequence.get(0));
                                        robustnesses.get(1).addValue(extinctionSequence.get(index10) / extinctionSequence.get(1));
                                        robustnesses.get(2).addValue(extinctionSequence.get(index30) / extinctionSequence.get(1));
                                    }
                                    //final
                                    for (Integer index : result.get(result.size() - 1).keySet()) {
                                        /*pw_R.print(variables + "," + graph + "," + maxTime + "," + index);
                                        for (Double extinctionSequenceStep : result.get(result.size() - 1).get(index)) {
                                            pw_R.print("," + extinctionSequenceStep);
                                        }
                                        pw_R.println();*/
                                        List<Double> extinctionSequence = result.get(result.size() - 1).get(index);
                                        int index10 = (int)((extinctionSequence.size() - 1) * 1.0 / 10.0);
                                        int index30 = (int)((extinctionSequence.size() - 1) * 3.0 / 10.0);
                                        robustnesses.get(3).addValue(extinctionSequence.get(0));
                                        robustnesses.get(4).addValue(extinctionSequence.get(index10) / extinctionSequence.get(1));
                                        robustnesses.get(5).addValue(extinctionSequence.get(index30) / extinctionSequence.get(1));
                                    }
                                }
                                pw_RD.println(variables + ","
                                        + robustnesses.get(1).getMean() + "," + robustnesses.get(4).getMean() + "," + (robustnesses.get(4).getMean() - robustnesses.get(1).getMean()) + ","
                                        + robustnesses.get(2).getMean() + "," + robustnesses.get(5).getMean() + "," + (robustnesses.get(5).getMean() - robustnesses.get(2).getMean()) + ","
                                        + robustnesses.get(0).getMean() + "," + robustnesses.get(3).getMean() + "," + (robustnesses.get(3).getMean() - robustnesses.get(0).getMean()));
                            }
                        }
                    }
                }
            }
            pw_RD.close();
            pw_R.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
