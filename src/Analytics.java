import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Analytics
{
    // exception logger
    private final static Logger LOGGER = Logger.getLogger(Analytics.class.getName());

    // class names for each number
    static String classNames;
    static int totalPoints;

    // holds overlapping datapoints
    static ArrayList<double[]> upper;
    static ArrayList<double[]> lower;

    // holds angles and threshold for LDA
    static ArrayList<Double> LDAFunction = new ArrayList<>();

    // holds current classes
    static ArrayList<String> curClasses;

    // holds confusion matrices in order
    final static Map<Integer, JTextArea> CONFUSION_MATRIX = new HashMap<>();
    final static Map<Integer, JTextArea> REMOTE_CONFUSION_MATRIX = new HashMap<>();
    static String CROSS_VALIDATION = "Cross Validation Not Generated";


    /**
     * Generates all data, data without overlap,
     * overlap, worst case, user-validation, and SVM confusion matrices
     */
    public static class GenerateAnalytics extends SwingWorker<Boolean, Void>
    {
        @Override
        protected Boolean doInBackground()
        {
            // only do analytics if multiclass
            if (DV.classNumber > 1)
            {
                // get current classes being visualized
                classNames = DV.get_classes();
                getCurClasses();

                // remove old analytics
                CONFUSION_MATRIX.clear();
                DV.confusionMatrixPanel.removeAll();

                // display analytics in separate window
                if (DV.displayRemoteAnalytics)
                {
                    REMOTE_CONFUSION_MATRIX.clear();
                    DV.remoteConfusionMatrixPanel.removeAll();
                }

                // generate analytics
                if (execute_analytics())
                    display_analytics();
                else
                    return false;

            }

            return true;
        }
    }


    /**
     * Executes all analytics
     * @return success of analytics
     */
    private static boolean execute_analytics()
    {
        // add confusion matrices from previous splits
        AddOldConfusionMatrices oldCM = new AddOldConfusionMatrices();
        if (DV.prevAllDataChecked)
            oldCM.execute();

        // create all data confusion matrix
        GetAllDataConfusionMatrix allCM = new GetAllDataConfusionMatrix();
        if (DV.allDataChecked)
            allCM.execute();

        // create data without overlap confusion matrix
        GetDataWithoutOverlapConfusionMatrix withoutCM = new GetDataWithoutOverlapConfusionMatrix();
        if (DV.withoutOverlapChecked || DV.worstCaseChecked)
            withoutCM.execute();

        // create overlap confusion matrix
        GetOverlapConfusionMatrix overlapCM = new GetOverlapConfusionMatrix();
        if (DV.overlapChecked || DV.worstCaseChecked)
            overlapCM.execute();

        // create worst case confusion matrix
        GetWorstCaseConfusionMatrix worstCM = new GetWorstCaseConfusionMatrix();
        try
        {
            if (DV.worstCaseChecked && withoutCM.get() && overlapCM.get())
                worstCM.execute();
        }
        catch (ExecutionException | InterruptedException e)
        {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return false;
        }

        // create user validation confusion matrix
        GetUserValidationConfusionMatrix userCM = new GetUserValidationConfusionMatrix();
        if (DV.userValidationImported && DV.userValidationChecked)
            userCM.execute();

        // create svm confusion matrix
        GetSVMAnalytics svmA = new GetSVMAnalytics();
        if (DV.svmAnalyticsChecked && DV.supportVectors != null)
            svmA.execute();

        // wait for threads to finish
        try
        {
            if (DV.prevAllDataChecked) oldCM.get();
            if (DV.allDataChecked) allCM.get();
            if (DV.withoutOverlapChecked) withoutCM.get();
            if (DV.overlapChecked) overlapCM.get();
            if (DV.worstCaseChecked) worstCM.get();
            if (DV.userValidationImported && DV.userValidationChecked) userCM.get();
            if (DV.svmAnalyticsChecked && DV.supportVectors != null) svmA.get();
        }
        catch (ExecutionException | InterruptedException e)
        {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            return false;
        }

        return true;
    }


    /**
     * Displays analytics
     */
    private static void display_analytics()
    {
        // add confusion matrices in order
        for (int i = 0; i < DV.prevAllDataCM.size() + 6; i++)
        {
            if (CONFUSION_MATRIX.containsKey(i))
            {
                DV.confusionMatrixPanel.add(CONFUSION_MATRIX.get(i));
                if (DV.displayRemoteAnalytics)
                    DV.remoteConfusionMatrixPanel.add(REMOTE_CONFUSION_MATRIX.get(i));
            }
        }
    }


    /**
     * Generates k-Fold cross validation
     */
    public static void generateCrossValidation()
    {
        // only do analytics if multiclass
        if (DV.classNumber > 1)
        {
            // create k-fold cross validation
            GetKFoldCrossValidation kFold = new GetKFoldCrossValidation();
            if (DV.crossValidationNotGenerated)
            {
                CROSS_VALIDATION = "Cross Validation Not Generated";
                kFold.execute();

                // wait for threads to finish
                try
                {
                    DV.crossValidationNotGenerated = false;
                    kFold.get();
                }
                catch (ExecutionException | InterruptedException e)
                {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                    return;
                }
            }

            // add cross validation
            DV.crossValidationPanel.removeAll();
            JTextArea cross_validate = new JTextArea(CROSS_VALIDATION);
            cross_validate.setFont(cross_validate.getFont().deriveFont(Font.BOLD, 12f));
            cross_validate.setEditable(false);
            DV.crossValidationPanel.add(cross_validate);

            if (DV.displayRemoteAnalytics)
            {
                DV.remoteCrossValidationPanel.removeAll();
                JTextArea cross_validate2 = new JTextArea(CROSS_VALIDATION);
                cross_validate2.setFont(cross_validate2.getFont().deriveFont(Font.BOLD, 12f));
                cross_validate2.setEditable(false);
                DV.remoteCrossValidationPanel.add(cross_validate2);
            }
        }

        DV.crossValidationPanel.repaint();
        DV.crossValidationPanel.revalidate();

        if (DV.displayRemoteAnalytics)
        {
            DV.remoteCrossValidationPanel.repaint();
            DV.remoteCrossValidationPanel.revalidate();
        }
    }


    /**
     * Gets old confusion matrices from previous data splits
     */
    private static class AddOldConfusionMatrices extends SwingWorker<Boolean, Void>
    {
        @Override
        protected Boolean doInBackground()
        {
            // set all previous confusion matrices (for 3+ classes)
            for (int i = 0; i < DV.prevAllDataCM.size(); i++)
                addConfusionMatrix(DV.prevAllDataCM.get(i), i);

            return true;
        }
    }


    /**
     * Generates confusion matrix with all data
     * Confusion matrix uses its own linear discriminant function
     */
    private static class GetAllDataConfusionMatrix extends SwingWorker<Boolean, Void>
    {
        @Override
        protected Boolean doInBackground()
        {
            // get point distribution
            // if more than 50% of data is misclassified then switch threshold
            int[][] pntDist = getPointDistribution(DV.trainData, DV.threshold);
            if ((double) (pntDist[0][0] + pntDist[1][1]) / (pntDist[0][0] + pntDist[0][1] + pntDist[1][0] + pntDist[1][1]) <= 0.5)
            {
                DV.upperIsLower = !DV.upperIsLower;
                pntDist = getPointDistribution(DV.trainData, DV.threshold);
            }

            // stores number of correctly classified datapoints
            DV.allDataClassifications = new int[]{ 0, 0 };
            DV.allDataClassifications[0] = pntDist[0][0] + pntDist[1][1];
            DV.allDataClassifications[1] = pntDist[0][0] + pntDist[0][1] + pntDist[1][0] + pntDist[1][1];

            // create confusion matrix
            StringBuilder cm = confusionMatrixBuilder("All Data Analytics",
                    pntDist,
                    100.0 * DV.allDataClassifications[0] / DV.allDataClassifications[1],
                    100.0 * DV.allDataClassifications[1] / totalPoints);

            // add overall accuracy if applicable
            if (!DV.prevAllDataClassifications.isEmpty())
                appendOverallAccuracy(cm);

            // set current all data confusion matrix string
            DV.allDataCM = cm.toString();
            addConfusionMatrix(DV.allDataCM, DV.prevAllDataCM.size());

            return true;
        }
    }


    /**
     * Generates confusion matrix without overlap data
     * Confusion matrix uses its own linear discriminant function
     */
    private static class GetDataWithoutOverlapConfusionMatrix extends SwingWorker<Boolean, Void>
    {
        @Override
        protected Boolean doInBackground()
        {
            // get data without overlap threshold and angles for data without overlap OR worst case
            // store overlapping datapoints in upper and lower graphs
            ArrayList<double[]> upper = new ArrayList<>();
            ArrayList<double[]> lower = new ArrayList<>();
            getUpperAndLowerNonOverlapping(upper, lower);

            // total datapoints within subset of utilized data
            int totalPointsUsed = upper.size() + lower.size();
            double dataUsed = 100.0 * totalPointsUsed / totalPoints;

            // create file for python process
            String fileName = "source\\Python\\DWO_CM.csv";
            CSV.createCSV(new ArrayList<>(List.of(upper, lower)), fileName);

            // get confusion matrix with LDA
            ArrayList<String> cmValues = LDAForConfusionMatrices(true, fileName);

            if (DV.withoutOverlapChecked)
            {
                // create confusion matrix
                // set data without overlap confusion matrix
                StringBuilder cm = confusionMatrixBuilder("Data Without Overlap Analytics", cmValues, dataUsed);
                addConfusionMatrix(cm.toString(), DV.prevAllDataCM.size() + 1);
            }

            return true;
        }
    }


    /**
     * Generates confusion matrix with only overlap data
     * Confusion matrix uses its own linear discriminant function
     */
    private static class GetOverlapConfusionMatrix extends SwingWorker<Boolean, Void>
    {
        @Override
        protected Boolean doInBackground()
        {
            // store overlapping datapoints in upper and lower graphs
            upper = new ArrayList<>();
            lower = new ArrayList<>();
            getUpperAndLowerOverlapping(upper, lower);

            // get percentage of overlap points used
            int totalPointsUsed = upper.size() + lower.size();
            double dataUsed = 100.0 * totalPointsUsed / totalPoints;

            if (DV.overlapChecked)
            {
                // create file for python process
                String fileName = "source\\Python\\OL_CM.csv";
                CSV.createCSV(new ArrayList<>(List.of(upper, lower)), fileName);

                // get confusion matrix with LDA
                ArrayList<String> cmValues = LDAForConfusionMatrices(false, fileName);
                StringBuilder cm = confusionMatrixBuilder("Overlap Analytics", cmValues, dataUsed);

                // set overlap confusion matrix
                addConfusionMatrix(cm.toString(), DV.prevAllDataCM.size() + 2);
            }

            return true;
        }
    }


    /**
     * Generates confusion matrix with only overlap data
     * Confusion matrix uses linear discriminant function from getDataWithoutOverlapConfusionMatrix
     */
    private static class GetWorstCaseConfusionMatrix extends SwingWorker<Boolean, Void>
    {
        @Override
        protected Boolean doInBackground()
        {
            // create confusion matrix
            StringBuilder cm;

            // check if data without overlap linear discriminant function was created
            if (LDAFunction != null && !LDAFunction.isEmpty())
            {
                // total datapoints within subset of utilized data
                int totalPointsUsed = lower.size() + upper.size();

                // get data without overlap threshold and angles
                double worstCaseThreshold = LDAFunction.get(LDAFunction.size() - 1);
                double[] worstCaseAngles = new double[LDAFunction.size() - 1];
                for (int i = 0; i < LDAFunction.size() - 1; i++)
                    worstCaseAngles[i] = LDAFunction.get(i);

                // create DataObjects of overlapping points
                ArrayList<DataObject> noOverlap = new ArrayList<>(List.of(
                        new DataObject("upper", convertArrayListToDoubleArray(upper)),
                        new DataObject("lower", convertArrayListToDoubleArray(lower))
                ));

                // update points with worst case angles
                for (DataObject dataObject : noOverlap)
                    dataObject.updateCoordinatesGLC(worstCaseAngles);

                // get point distribution
                int[][] pntDist = getPointDistribution(noOverlap, worstCaseThreshold);

                // create confusion matrix
                cm = confusionMatrixBuilder(
                        "Worst Case Data Analytics",
                        pntDist,
                        100.0 * (pntDist[0][0] + pntDist[1][1]) / totalPointsUsed,
                        100.0 * totalPointsUsed / totalPoints);
            }
            else
                cm = emptyConfusionMatrixBuilder("Worst Case Data Analytics");

            // set worst case confusion matrix
            addConfusionMatrix(cm.toString(), DV.prevAllDataCM.size() + 3);

            return true;
        }
    }


    /**
     * Generates confusion matrix with user imported validation data
     */
    private static class GetUserValidationConfusionMatrix extends SwingWorker<Boolean, Void>
    {
        @Override
        protected Boolean doInBackground()
        {
            // update points with angles
            for (int i = 0; i < DV.testData.size(); i++)
                DV.testData.get(i).updateCoordinatesGLC(DV.angles);

            // get point distribution
            int[][] pntDist = getPointDistribution(DV.testData, DV.threshold);
            int correctPoints = pntDist[0][0] + pntDist[1][1];
            int totalPointsUsed = pntDist[0][0] + pntDist[0][1] + pntDist[1][0] + pntDist[1][1];

            int totalPoints = 0;
            for (int i = 0; i < DV.testData.size(); i++)
                totalPoints += DV.testData.get(i).data.length;

            // create confusion matrix
            StringBuilder cm = confusionMatrixBuilder(
                    "User Validation Data Analytics",
                    pntDist,
                    100.0 * correctPoints / totalPointsUsed,
                    100.0 * totalPointsUsed / totalPoints);

            // set user validation confusion matrix
            addConfusionMatrix(cm.toString(), DV.prevAllDataCM.size() + 4);

            return true;
        }
    }


    /**
     * Generates analytics for SVM support vectors
     */
    private static class GetSVMAnalytics extends SwingWorker<Boolean, Void>
    {
        @Override
        protected Boolean doInBackground()
        {
            // update support vectors with angles
            DV.supportVectors.updateCoordinatesGLC(DV.angles);

            // support vectors within overlap
            int overlapSVM = getOverlapSVM();

            // create confusion matrix
            // append number of support vectors
            // append percentage of support vectors within the overlap
            String cm = "SVM Support Vector Analytics\n" + "Number of Support Vectors: " + DV.supportVectors.data.length +
                    String.format("\nPercent of SVM in Overlap: %.2f%%", 100.0 * overlapSVM / DV.supportVectors.data.length);

            // set user validation confusion matrix
            addConfusionMatrix(cm, DV.prevAllDataCM.size() + 5);

            return true;
        }

        private static int getOverlapSVM()
        {
            int overlapSVM = 0;

            // check all classes
            for (int i = 0; i < DV.supportVectors.data.length; i++)
            {
                double endpoint = DV.supportVectors.coordinates[i][DV.supportVectors.coordinates[0].length-1][0];

                // if endpoint is within overlap then store point
                if ((DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                    overlapSVM++;
            }
            return overlapSVM;
        }
    }


    /**
     * Runs k-fold cross validation on dataset
     */
    public static class GetKFoldCrossValidation extends SwingWorker<Boolean, Void>
    {
        @Override
        protected Boolean doInBackground()
        {
            // store datapoints in upper and lower graphs
            ArrayList<double[]> upper = new ArrayList<>();
            ArrayList<double[]> lower = new ArrayList<>();
            getUpperAndLower(upper, lower);

            // create file for python process
            String fileName = "source\\Python\\k_fold.csv";
            CSV.createCSV(new ArrayList<>(List.of(upper, lower)), fileName);

            // create k-fold (python) process
            ProcessBuilder cv = new ProcessBuilder("cmd", "/c",
                    "source\\Python\\kFoldCrossValidation\\kFoldCrossValidation.exe",
                    fileName,
                    String.valueOf(DV.kFolds));

            try
            {
                // run python (LDA) process
                Process process = cv.start();

                // read python outputs
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String output;

                // confusion matrix classifications
                ArrayList<String> cvTable = new ArrayList<>();
                while ((output = reader.readLine()) != null)
                    cvTable.add(output);

                if (!cvTable.isEmpty())
                {
                    // create confusion matrix
                    StringBuilder table = new StringBuilder("k-Fold Cross Validation");

                    for (int i = 0; i < cvTable.size(); i++)
                    {
                        // append model rows
                        table.append("\n").append(cvTable.get(i));

                        // add line break between models and st. dev. + avg.
                        if (i == cvTable.size() - 3)
                            table.append("\n").append("-".repeat(cvTable.get(i).length()));
                    }

                    // delete created file
                    File fileToDelete = new File(fileName);
                    Files.deleteIfExists(fileToDelete.toPath());

                    // set cross validation table
                    CROSS_VALIDATION = table.toString();
                }
            }
            catch (IOException e)
            {
                JOptionPane.showMessageDialog(DV.mainFrame, "Error: could not run k-Fold Cross Validation", "Error", JOptionPane.ERROR_MESSAGE);
            }

            return true;
        }
    }


    /**
     * Gets functions for confusion matrices
     * @param storeFunction whether to store angles and threshold
     * @param fileName name of csv file storing data
     */
    private static ArrayList<String> LDAForConfusionMatrices(boolean storeFunction, String fileName)
    {
        // get python boolean
        String pyBool;
        if (storeFunction)
            pyBool = "T";
        else
            pyBool = "";

        // create LDA (python) process
        ProcessBuilder lda = new ProcessBuilder("cmd", "/c",
                "source\\Python\\ConfusionMatrixGenerator\\ConfusionMatrixGenerator.exe",
                fileName,
                pyBool);

        try
        {
            // run python (LDA) process
            Process process = lda.start();

            // read python outputs
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output;

            // confusion matrix classifications
            ArrayList<String> classifications = new ArrayList<>();

            // data without overlap confusion matrix angles and threshold
            if (storeFunction)
                LDAFunction = new ArrayList<>();

            int cnt = 0;

            while ((output = reader.readLine()) != null)
            {
                // get angles and threshold
                if (storeFunction && cnt <= DV.trainData.get(0).coordinates[0].length)
                {
                    LDAFunction.add(Double.parseDouble(output));
                    cnt++;
                }
                else
                {
                    // get rows of confusion matrix
                    classifications.add(output);
                }
            }

            // delete created file
            File fileToDelete = new File(fileName);
            Files.deleteIfExists(fileToDelete.toPath());

            // return confusion matrix
            return classifications;
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(DV.mainFrame, "Error: could not create confusion matrix", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }


    /**
     * Gets actual and predicted value distribution from the DV Linear Discriminant Function
     * @param data data used in classification
     * @param threshold class discrimination threshold
     * @return distribution of actual and predicted values
     */
    public static int[][] getPointDistribution(ArrayList<DataObject> data, double threshold)
    {
        // create threads for each data object
        int numThreads = Math.min(Runtime.getRuntime().availableProcessors(), data.size());
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<int[][]>> futures = new ArrayList<>();

        // get point distribution
        int[][] pntDist = new int[2][2];
        for (int i = 0; i < data.size(); i++)
        {
            if (i == DV.upperClass || DV.lowerClasses.get(i))
            {
                // execute task
                int finalI = i;
                Callable<int[][]> task = () -> pointDistributionHelper(data.get(finalI), threshold, finalI);
                Future<int[][]> future = executor.submit(task);
                futures.add(future);
            }
        }

        // get results from threads
        for (Future<int[][]> future : futures)
        {
            try
            {
                int[][] localPntDist = future.get();
                for (int i = 0; i < 2; i++)
                {
                    for (int j = 0; j < 2; j++)
                        pntDist[i][j] += localPntDist[i][j];
                }
            }
            catch (InterruptedException | ExecutionException e)
            {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }

        executor.shutdown();

        return pntDist;
    }


    /**
     * Helper function for getPointDistribution
     * @param data data used in classification
     * @param threshold class discrimination threshold
     * @param i class index
     * @return distribution of actual and predicted values
     */
    private static int[][] pointDistributionHelper(DataObject data, double threshold, int i)
    {
        // get point distribution
        int[][] pntDist = new int[2][2];
        for (int j = 0; j < data.coordinates.length; j++)
        {
            // check if endpoint is within the subset of used data
            double endpoint = data.coordinates[j][data.coordinates[j].length-1][0];
            if ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive)
            {
                // get classification
                if (i == DV.upperClass && DV.upperIsLower)
                {
                    // check if endpoint is correctly classified
                    if (endpoint > threshold)
                        pntDist[0][1]++;
                    else
                        pntDist[0][0]++;
                }
                else if (i == DV.upperClass)
                {
                    // check if endpoint is correctly classified
                    if (endpoint < threshold)
                        pntDist[0][1]++;
                    else
                        pntDist[0][0]++;
                }
                else if(DV.lowerClasses.get(i) && DV.upperIsLower)
                {
                    // check if endpoint is correctly classified
                    if (endpoint < threshold)
                        pntDist[1][0]++;
                    else
                        pntDist[1][1]++;
                }
                else
                {
                    // check if endpoint is correctly classified
                    if (endpoint > threshold)
                        pntDist[1][0]++;
                    else
                        pntDist[1][1]++;
                }
            }
        }

        return pntDist;
    }


    /**
     * Gets all non-overlapping data on the upper and lower GLC-L graphs.
     * @param upper stores non-overlapping data on the upper graph
     * @param lower stores non-overlapping data on the lower graph
     */
    private static void getUpperAndLowerNonOverlapping(ArrayList<double[]> upper, ArrayList<double[]> lower)
    {
        // check all classes
        for (int i = 0; i < DV.trainData.size(); i++)
        {
            if (i == DV.upperClass || DV.lowerClasses.get(i))
            {
                for (int j = 0; j < DV.trainData.get(i).coordinates.length; j++)
                {
                    // if endpoint is outside of overlap then store point
                    double endpoint = DV.trainData.get(i).coordinates[j][DV.trainData.get(i).coordinates[j].length-1][0];
                    if ((DV.overlapArea[0] > endpoint || endpoint > DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                    {
                        if (i == DV.upperClass)
                            upper.add(DV.trainData.get(i).data[j]);
                        else
                            lower.add(DV.trainData.get(i).data[j]);
                    }
                }
            }
        }
    }


    /**
     * Gets all overlapping data on the upper and lower GLC-L graphs.
     * @param upper stores overlapping data on the upper graph
     * @param lower stores overlapping data on the lower graph
     */
    private static void getUpperAndLowerOverlapping(ArrayList<double[]> upper, ArrayList<double[]> lower)
    {
        // check all classes
        for (int i = 0; i < DV.trainData.size(); i++)
        {
            if (i == DV.upperClass || DV.lowerClasses.get(i))
            {
                for (int j = 0; j < DV.trainData.get(i).coordinates.length; j++)
                {
                    // if endpoint is within overlap then store point
                    double endpoint = DV.trainData.get(i).coordinates[j][DV.trainData.get(i).coordinates[j].length-1][0];
                    if ((DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                    {
                        if (i == DV.upperClass)
                            upper.add(DV.trainData.get(i).data[j]);
                        else
                            lower.add(DV.trainData.get(i).data[j]);
                    }
                }
            }
        }
    }


    /**
     * Gets all data on the upper and lower GLC-L graphs.
     * @param upper stores data on the upper graph
     * @param lower stores data on the lower graph
     */
    private static void getUpperAndLower(ArrayList<double[]> upper, ArrayList<double[]> lower)
    {
        // check all classes
        for (int i = 0; i < DV.trainData.size(); i++)
        {
            if (i == DV.upperClass || DV.lowerClasses.get(i))
            {
                for (int j = 0; j < DV.trainData.get(i).coordinates.length; j++)
                {
                    // if endpoint is within domain
                    double endpoint = DV.trainData.get(i).coordinates[j][DV.trainData.get(i).coordinates[j].length-1][0];
                    if ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive)
                    {
                        if (i == DV.upperClass)
                            upper.add(DV.trainData.get(i).data[j]);
                        else
                            lower.add(DV.trainData.get(i).data[j]);
                    }
                }
            }
        }
    }


    /**
     * Creates confusion matrix
     * @param name name of confusion matrix
     * @param pntDist distribution of actual and predicted values
     * @param accuracy accuracy of classification
     * @param dataUsed amount of data used
     * @return confusion matrix
     */
    private static StringBuilder confusionMatrixBuilder(String name, int[][] pntDist, double accuracy, double dataUsed)
    {
        // create confusion matrix
        StringBuilder cm = new StringBuilder(name);
        cm.append("\nReal\tPredictions\nClass\t");

        // append predicted classes
        for (int i = 0; i < 2; i++)
            cm.append(curClasses.get(i)).append("\t");

        for (int i = 0; i < 2; i++)
        {
            // append class label
            cm.append("\n").append(curClasses.get(i)).append("\t");

            // append classifications
            cm.append(pntDist[i][0]).append("\t").append(pntDist[i][1]).append("\t");
        }

        // append accuracy
        cm.append(String.format("\nAccuracy: %.2f%%", accuracy));

        // append percentage of total points used
        cm.append(String.format("\nData Used: %.2f%%", dataUsed));

        return cm;
    }


    /**
     * Creates confusion matrix
     * @param name name of confusion matrix
     * @param dataUsed amount of data used
     * @return confusion matrix
     */
    private static StringBuilder confusionMatrixBuilder(String name, ArrayList<String> cmValues, double dataUsed)
    {
        // create confusion matrix
        if (cmValues != null && !cmValues.isEmpty())
        {
            StringBuilder cm = new StringBuilder(name);
            cm.append("\nReal\tPredictions\nClass\t");

            // append predicted classes
            for (int i = 0; i < 2; i++)
                cm.append(curClasses.get(i)).append("\t");

            for (int i = 0, index = -1; i < 2; i++)
            {
                // append class label
                cm.append("\n").append(curClasses.get(i)).append("\t");

                // append classifications
                cm.append(cmValues.get(++index)).append("\t").append(cmValues.get(++index)).append("\t");
            }

            // append accuracy
            cm.append("\n").append(cmValues.get(cmValues.size() - 1));

            // append percentage of total points used
            cm.append(String.format("\nData Used: %.2f%%", dataUsed));

            return cm;
        }
        else
        {
            return emptyConfusionMatrixBuilder(name);
        }
    }


    /**
     * Creates empty confusion matrix
     * @param name name of confusion matrix
     * @return empty confusion matrix
     */
    private static StringBuilder emptyConfusionMatrixBuilder(String name)
    {
        // create confusion matrix
        StringBuilder cm = new StringBuilder(name);
        cm.append("\nReal\tPredictions\nClass\t");

        // append predicted classes
        for (int i = 0; i < 2; i++)
            cm.append(curClasses.get(i)).append("\t");

        for (int i = 0; i < 2; i++)
        {
            // append class label
            cm.append("\n").append(curClasses.get(i)).append("\t");

            // append classifications
            cm.append(0).append("\t").append(0).append("\t");
        }

        // append accuracy
        cm.append("\nAccuracy: NaN%");

        // append percentage of total points used
        cm.append("\nData Used: 0%");

        return cm;
    }


    /**
     * Gets current classes being visualized
     */
    private static void getCurClasses()
    {
        totalPoints = DV.trainData.get(DV.upperClass).data.length;

        // add upper class
        curClasses = new ArrayList<>();
        curClasses.add(String.format("%d", DV.upperClass));

        // add lower classes
        StringBuilder lowerClasses = new StringBuilder();
        for (int i = 0; i < DV.classNumber; i++)
        {
            if (DV.lowerClasses.get(i))
            {
                if (!lowerClasses.isEmpty())
                    lowerClasses.append(",");

                lowerClasses.append(i);
                totalPoints += DV.trainData.get(i).data.length;
            }
        }

        curClasses.add(lowerClasses.toString());
    }


    /**
     * Adds confusion matrix to analytics window
     * @param cm confusion matrix
     * @param index index of confusion matrix
     */
    private static void addConfusionMatrix(String cm, int index)
    {
        // set all data confusion matrix
        JTextArea confusionMatrix = new JTextArea(cm);
        confusionMatrix.setToolTipText(classNames);
        confusionMatrix.setFont(confusionMatrix.getFont().deriveFont(Font.BOLD, 12f));
        confusionMatrix.setEditable(false);
        synchronized (CONFUSION_MATRIX)
        {
            CONFUSION_MATRIX.put(index, confusionMatrix);
        }

        // add to separate analytics window
        if (DV.displayRemoteAnalytics)
        {
            // set data without overlap confusion matrix
            JTextArea confusionMatrix2 = new JTextArea(cm);
            confusionMatrix2.setToolTipText(classNames);
            confusionMatrix2.setFont(confusionMatrix2.getFont().deriveFont(Font.BOLD, 12f));
            confusionMatrix2.setEditable(false);
            synchronized (REMOTE_CONFUSION_MATRIX)
            {
                REMOTE_CONFUSION_MATRIX.put(index, confusionMatrix2);
            }
        }
    }


    /**
     * Appends overall accuracy to confusion matrix
     * @param cm confusion matrix
     */
    private static void appendOverallAccuracy(StringBuilder cm)
    {
        int correct = DV.allDataClassifications[0];
        int used = DV.allDataClassifications[1];

        for (int i = 0; i < DV.prevAllDataClassifications.size(); i++)
        {
            correct += DV.prevAllDataClassifications.get(i)[0];
            used += DV.prevAllDataClassifications.get(i)[1];
        }

        cm.append(String.format("\nOverall Accuracy: %.2f%%", 100.0 * correct  / used));
    }


    /**
     * Converts an arraylist of double[] to a double[][]
     * @param arrayList list to convert
     * @return double[][]
     */
    public static double[][] convertArrayListToDoubleArray(ArrayList<double[]> arrayList)
    {
        double[][] array = new double[arrayList.size()][];
        for (int i = 0; i < arrayList.size(); i++)
            array[i] = arrayList.get(i);

        return array;
    }
}
