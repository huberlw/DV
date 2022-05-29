from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
from sklearn.metrics import confusion_matrix, accuracy_score
import pandas as pd
import numpy as np
import math
import sys
import os

# get data file and dimensions
path = sys.argv[1]

# get printing
printing = bool(sys.argv[3])


if os.path.exists(path):
    print("HELLO")
    # create dataframe from file
    dataframe = pd.read_csv(path)

    # get data and labels
    labels = dataframe['class'].values.reshape(-1, 1)
    data = dataframe.drop(['class'], axis=1)

    # get LDA
    lda = LinearDiscriminantAnalysis(n_components=1)

    # train
    lda.fit(data, labels.ravel())

    # get weights
    angles = lda.coef_[0].copy()

    # normalize
    angles = angles / max(abs(angles))

    # get degrees
    for i in range(len(angles)):
        angles[i] = np.arccos(angles[i])
        angles[i] = math.degrees(angles[i])

    # inform DV program of angles
    if printing:
        for i in range(len(angles)):
            print(angles[i])

    # normalize weights
    weights = lda.coef_[0].copy()
    weights = weights / max(abs(weights))

    # apply weights to class means
    weightedMeans = lda.means_ * weights

    # get sum of feature values for each class
    weightedMeanSum1 = sum(weightedMeans[0])
    weightedMeanSum2 = sum(weightedMeans[1])

    # add weighted class sums then divide by 2 to get threshold
    if printing:
        print((weightedMeanSum1 + weightedMeanSum2) / 2)

    # get confusion matrix
    #print(pd.DataFrame(confusion_matrix(labels, lda.predict(data))))

