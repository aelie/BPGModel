package individual;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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
            Simulator simulator;
            for (int applicationPoolSize = 10; applicationPoolSize < 111; applicationPoolSize += 20) {
                for (int serverPoolSize = 10; serverPoolSize < 51; serverPoolSize += 10) {
                    for (int servicePoolSize = 5; servicePoolSize < 55; servicePoolSize += 10) {
                        for (double mutationProbability = 0.2; mutationProbability < 1; mutationProbability += 0.1) {
                            for (int serverMaxConnexion = applicationPoolSize / serverPoolSize; serverMaxConnexion < 5 * applicationPoolSize / serverPoolSize; serverMaxConnexion += applicationPoolSize / serverPoolSize) {
                                for (double serviceListRatio = 0.1; serviceListRatio < 0.5; serviceListRatio += 0.1) {
                                    for (int graph = 0; graph < graphNumber; graph++) {
                                        String variables = applicationPoolSize + "_" +
                                                serverPoolSize + "_" +
                                                servicePoolSize + "_" +
                                                mutationProbability + "_" +
                                                serverMaxConnexion + "_" +
                                                serviceListRatio;
                                        System.out.println(variables + "/" + graph);
                                        simulator = Simulator.getInstance();
                                        simulator.setVariables(applicationPoolSize, serverPoolSize, servicePoolSize,
                                                mutationProbability, serverMaxConnexion, serviceListRatio,
                                                maxTime, robustnessRuns);
                                        simulator.warmup((int) System.currentTimeMillis(), true);
                                        simulator.start();
                                        //initial
                                        List<Map<Integer, List<Double>>> result = Simulator.getInstance().getRobustnessResults();
                                        for (Integer index : result.get(0).keySet()) {
                                            pw_R.print(variables + "," + graph + "," + 0 + "," + index);
                                            for (Double ess : result.get(0).get(index)) {
                                                pw_R.print("," + ess);
                                            }
                                            pw_R.println();
                                        }
                                        //end
                                        result = Simulator.getInstance().getRobustnessResults();
                                        for (Integer index : result.get(1).keySet()) {
                                            pw_R.print(variables + "," + graph + "," + maxTime + "," + index);
                                            for (Double ess : result.get(1).get(index)) {
                                                pw_R.print("," + ess);
                                            }
                                            pw_R.println();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            pw_R.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
