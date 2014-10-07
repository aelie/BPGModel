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
            pw_RD.println("Parameters,RobustnessDifference");
            Simulator simulator;
            for (int applicationPoolSize = 120; applicationPoolSize < 121; applicationPoolSize += 20) {
                for (int serverPoolSize = 40; serverPoolSize < 81; serverPoolSize += 10) {
                    for (int servicePoolSize = 40; servicePoolSize < 81; servicePoolSize += 10) {
                        for (double mutationProbability = 0.2; mutationProbability < 0.7; mutationProbability += 0.1) {
                            for (int serverMaxConnexion = 6; serverMaxConnexion < 13; serverMaxConnexion += 3) {
                                String variables = applicationPoolSize + "_" +
                                        serverPoolSize + "_" +
                                        servicePoolSize + "_" +
                                        mutationProbability + "_" +
                                        serverMaxConnexion;
                                List<SummaryStatistics> robustnesses = new ArrayList<>(Arrays.asList(new SummaryStatistics(), new SummaryStatistics()));
                                for (int graph = 0; graph < graphNumber; graph++) {
                                    System.out.println(variables + "/" + graph);
                                    simulator = Simulator.getInstance();
                                    simulator.setVariables(applicationPoolSize, serverPoolSize, servicePoolSize,
                                            mutationProbability, serverMaxConnexion,
                                            maxTime, robustnessRuns);
                                    simulator.warmup((int) System.currentTimeMillis(), true);
                                    simulator.start(false);
                                    List<Map<Integer, List<Double>>> result = Simulator.getInstance().getRobustnessResults();
                                    //initial
                                    for (Integer index : result.get(0).keySet()) {
                                        /*pw_R.print(variables + "," + graph + "," + 0 + "," + index);
                                        for (Double extinctionSequenceStep : result.get(0).get(index)) {
                                            pw_R.print("," + extinctionSequenceStep);
                                        }
                                        pw_R.println();*/
                                        robustnesses.get(0).addValue(result.get(0).get(index).get(0));
                                    }
                                    //final
                                    for (Integer index : result.get(result.size() - 1).keySet()) {
                                        /*pw_R.print(variables + "," + graph + "," + maxTime + "," + index);
                                        for (Double extinctionSequenceStep : result.get(result.size() - 1).get(index)) {
                                            pw_R.print("," + extinctionSequenceStep);
                                        }
                                        pw_R.println();*/
                                        robustnesses.get(result.size() - 1).addValue(result.get(result.size() - 1).get(index).get(0));
                                    }
                                }
                                pw_RD.println(variables + "," + (robustnesses.get(robustnesses.size() - 1).getMean() - robustnesses.get(0).getMean()));
                            }
                        }
                    }
                }
            }
            pw_RD.close();
            pw_R.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
