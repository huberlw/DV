from sklearn.tree import DecisionTreeClassifier
import pandas as pd
import sys
import os


# get data file
path = sys.argv[1]

if os.path.exists(path):
    # create dataframe from file
    df = pd.read_csv(path)

    #  get data and labels
    labels = df['Class'].values.reshape(-1, 1).ravel()
    data = df.drop(['Class'], axis=1)

    # get SVM
    dt = DecisionTreeClassifier(min_samples_leaf=round(len(labels) * 0.02))

    # train
    dt.fit(data, labels)
    
    # get feature importance
    importance = dt.feature_importances_

    # inform about importance
    for x in importance:
        print(x)
