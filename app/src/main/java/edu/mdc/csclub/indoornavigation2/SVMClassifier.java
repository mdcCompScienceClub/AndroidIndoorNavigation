package edu.mdc.csclub.indoornavigation2;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.StringTokenizer;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;

/**
 * Created by transflorida on 6/12/17.
 */

public class SVMClassifier {
    private static final String TAG = SVMClassifier.class.getSimpleName();

    private static final String DIR = "/data/data/edu.mdc.csclub.indoornavigation2/files/";
    private static final String MODEL_FILENAME = "model.dat";
    private static final String SCALING_PARAMS_FILENAME = "scalingParameters.dat";
    private static final String svm_type_table[] =
            {
                    "c_svc", "nu_svc", "one_class", "epsilon_svr", "nu_svr",
            };

    private static final String kernel_type_table[] =
            {
                    "linear", "polynomial", "rbf", "sigmoid", "precomputed"
            };

    private svm_model model;
    private int max_index;

    private double lower = -1.0;
    private double upper = 1.0;
    private double y_lower;
    private double y_upper;
    private boolean y_scaling = false;

    private double y_max = -Double.MAX_VALUE;
    private double y_min = Double.MAX_VALUE;

    private double[] feature_max;
    private double[] feature_min;

    private Context mContext;


    public SVMClassifier(Context context) {
        mContext = context;

        model = loadSVMModel();
        Log.i(TAG, "******MODEL PARAMETERS LOADED = " + model.param);

        loadScalingParameters();
    }

    private svm_model loadSVMModel() {

        svm_model model = null;
        try {
            InputStream is = mContext.getAssets().open(MODEL_FILENAME);
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader fp = new BufferedReader(isr);

            model = new svm_model();
            svm_parameter param = new svm_parameter();
            model.param = param;
            model.rho = null;
            model.probA = null;
            model.probB = null;
            model.label = null;
            model.nSV = null;

            while (true) {
                String cmd = fp.readLine();
                String arg = cmd.substring(cmd.indexOf(' ') + 1);

                if (cmd.startsWith("svm_type")) {
                    int i;
                    for (i = 0; i < svm_type_table.length; i++) {
                        if (arg.indexOf(svm_type_table[i]) != -1) {
                            param.svm_type = i;
                            break;
                        }
                    }
                    if (i == svm_type_table.length) {
                        System.err.print("unknown svm type.\n");
                        return null;
                    }
                } else if (cmd.startsWith("kernel_type")) {
                    int i;
                    for (i = 0; i < kernel_type_table.length; i++) {
                        if (arg.indexOf(kernel_type_table[i]) != -1) {
                            param.kernel_type = i;
                            break;
                        }
                    }
                    if (i == kernel_type_table.length) {
                        System.err.print("unknown kernel function.\n");
                        return null;
                    }
                } else if (cmd.startsWith("degree"))
                    param.degree = atoi(arg);
                else if (cmd.startsWith("gamma"))
                    param.gamma = atof(arg);
                else if (cmd.startsWith("coef0"))
                    param.coef0 = atof(arg);
                else if (cmd.startsWith("nr_class"))
                    model.nr_class = atoi(arg);
                else if (cmd.startsWith("total_sv"))
                    model.l = atoi(arg);
                else if (cmd.startsWith("rho")) {
                    int n = model.nr_class * (model.nr_class - 1) / 2;
                    model.rho = new double[n];
                    StringTokenizer st = new StringTokenizer(arg);
                    for (int i = 0; i < n; i++)
                        model.rho[i] = atof(st.nextToken());
                } else if (cmd.startsWith("label")) {
                    int n = model.nr_class;
                    model.label = new int[n];
                    StringTokenizer st = new StringTokenizer(arg);
                    for (int i = 0; i < n; i++)
                        model.label[i] = atoi(st.nextToken());
                } else if (cmd.startsWith("probA")) {
                    int n = model.nr_class * (model.nr_class - 1) / 2;
                    model.probA = new double[n];
                    StringTokenizer st = new StringTokenizer(arg);
                    for (int i = 0; i < n; i++)
                        model.probA[i] = atof(st.nextToken());
                } else if (cmd.startsWith("probB")) {
                    int n = model.nr_class * (model.nr_class - 1) / 2;
                    model.probB = new double[n];
                    StringTokenizer st = new StringTokenizer(arg);
                    for (int i = 0; i < n; i++)
                        model.probB[i] = atof(st.nextToken());
                } else if (cmd.startsWith("nr_sv")) {
                    int n = model.nr_class;
                    model.nSV = new int[n];
                    StringTokenizer st = new StringTokenizer(arg);
                    for (int i = 0; i < n; i++)
                        model.nSV[i] = atoi(st.nextToken());
                } else if (cmd.startsWith("SV")) {
                    break;
                } else {
                    System.err.print("unknown text in model file: [" + cmd + "]\n");
                    return null;
                }
            }

            // read sv_coef and SV

            int m = model.nr_class - 1;
            int l = model.l;
            model.sv_coef = new double[m][l];
            model.SV = new svm_node[l][];

            for (int i = 0; i < l; i++) {
                String line = fp.readLine();
                StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");

                for (int k = 0; k < m; k++)
                    model.sv_coef[k][i] = atof(st.nextToken());
                int n = st.countTokens() / 2;
                model.SV[i] = new svm_node[n];
                for (int j = 0; j < n; j++) {
                    model.SV[i][j] = new svm_node();
                    model.SV[i][j].index = atoi(st.nextToken());
                    model.SV[i][j].value = atof(st.nextToken());
                }
            }

            fp.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return model;

    }


    public Cell predict(Measurement measurement) {
        Log.i(TAG, "******MEASURED: ");

        double[] measured = {measurement.getBeacon11RSSI(), measurement.getBeacon12RSSI(), measurement.getBeacon21RSSI(), measurement.getBeacon22RSSI(), measurement.getBeacon31RSSI(), measurement.getBeacon32RSSI()};
        for (int index = 0; index < measured.length; index++) {
            System.out.print(measured[index] + " ");
            Log.i(TAG, "******" + measured[index] + " ");
        }

        Log.i(TAG, "******MEASURED AND SCALED: ");
        for (int index = 0; index < measured.length; index++) {
                /* skip single-valued attribute */
            if (Math.abs(feature_max[index] - feature_min[index]) < 1e-2)
                return null;

            if (Math.abs(measured[index] - feature_min[index]) < 1e-2)
                measured[index] = lower;
            else if (Math.abs(measured[index] - feature_max[index]) < 1e-2)
                measured[index] = upper;
            else {
                measured[index] = lower + (upper - lower) * (measured[index] - feature_min[index])
                        / (feature_max[index] - feature_min[index]);
            }

            Log.i(TAG, "******" + measured[index] + " ");
        }
        double predicted = evaluate(measured);
        Log.i(TAG, "******Predicted" + predicted);
        Cell cell = new Cell((int) predicted / 30, (int) predicted % 30, -1);
        return cell;

    }

    private void loadScalingParameters() {
        try {

            InputStream is = mContext.getAssets().open(SCALING_PARAMS_FILENAME);
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader fp_restore = new BufferedReader(isr);

            int idx, c;
            if ((c = fp_restore.read()) == 'y') {
                fp_restore.readLine();
                fp_restore.readLine();
                fp_restore.readLine();
            }
            fp_restore.readLine();
            fp_restore.readLine();

            String restore_line = null;
            while ((restore_line = fp_restore.readLine()) != null) {
                StringTokenizer st2 = new StringTokenizer(restore_line);
                idx = Integer.parseInt(st2.nextToken());
                max_index = Math.max(max_index, idx);
            }
            fp_restore = rewind(fp_restore, SCALING_PARAMS_FILENAME);


            try {
                feature_max = new double[(max_index + 1)];
                feature_min = new double[(max_index + 1)];
            } catch (OutOfMemoryError e) {
                System.err.println("can't allocate enough memory");
                System.exit(1);
            }

            for (int i = 0; i <= max_index; i++) {
                feature_max[i] = -Double.MAX_VALUE;
                feature_min[i] = Double.MAX_VALUE;
            }


            // fp_restore rewinded in finding max_index
            double fmin, fmax;

            fp_restore.mark(2);                // for reset
            if ((c = fp_restore.read()) == 'y') {
                fp_restore.readLine();        // pass the '\n' after 'y'
                StringTokenizer st = new StringTokenizer(fp_restore.readLine());
                y_lower = Double.parseDouble(st.nextToken());
                y_upper = Double.parseDouble(st.nextToken());
                st = new StringTokenizer(fp_restore.readLine());
                y_min = Double.parseDouble(st.nextToken());
                y_max = Double.parseDouble(st.nextToken());
                y_scaling = true;
            } else
                fp_restore.reset();

            if (fp_restore.read() == 'x') {
                fp_restore.readLine();        // pass the '\n' after 'x'
                StringTokenizer st = new StringTokenizer(fp_restore.readLine());
                lower = Double.parseDouble(st.nextToken());
                upper = Double.parseDouble(st.nextToken());

                Log.i(TAG, "******SCALING PARAMETERS LOADED: x =" + lower + "-" + upper);
                restore_line = null;
                while ((restore_line = fp_restore.readLine()) != null) {
                    StringTokenizer st2 = new StringTokenizer(restore_line);
                    idx = Integer.parseInt(st2.nextToken());
                    fmin = Double.parseDouble(st2.nextToken());
                    fmax = Double.parseDouble(st2.nextToken());
                    if (idx <= max_index) {
                        feature_min[idx] = fmin;
                        feature_max[idx] = fmax;

                        Log.i(TAG, "******SCALING PARAMETERS LOADED: " + idx + ": " + feature_min[idx] + "-" + feature_max[idx]);

                    }
                }
            }
            fp_restore.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private double evaluate(double[] features) {
        svm_node[] nodes = new svm_node[features.length];
        for (int i = 0; i < features.length; i++) {
            svm_node node = new svm_node();
            node.index = i + 1;
            node.value = features[i];
            nodes[i] = node;
        }

        int totalClasses = 1500;
        int[] labels = new int[totalClasses];
        svm.svm_get_labels(model, labels);

        double[] prob_estimates = new double[totalClasses];
        return svm.svm_predict_probability(model, nodes, prob_estimates);
    }

    private BufferedReader rewind(BufferedReader fp, String filename) throws IOException {
        fp.close();
        InputStream is = mContext.getAssets().open(SCALING_PARAMS_FILENAME);
        InputStreamReader isr = new InputStreamReader(is);
        return new BufferedReader(isr);
    }

    private static double atof(String s) {
        return Double.parseDouble(s);
    }

    private static int atoi(String s) {
        return Integer.parseInt(s);
    }

}
