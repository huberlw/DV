from sklearn.svm import SVC
import pandas as pd
import csv
import sys
import os


# get data file
path = sys.argv[1]

if os.path.exists(path):
    # create dataframe from file
    df = pd.read_csv(path)

    #  get data and labels
    labels = df['class'].values.reshape(-1, 1).ravel()
    data = df.drop(['class'], axis=1)

    # get SVM
    svm = SVC()

    # train
    svm.fit(data, labels)
    
    # get support vectors
    sv = svm.support_vectors_

    # create header
    header = []
    for i in range(len(sv[0])):
        header.append(f"feature{i}")

    # output csv file
    output = open(os.path.dirname(os.path.realpath(__file__)) + "..\\..\\svm_data.csv", "w")
    writer = csv.writer(output, lineterminator='\n')
    writer.writerow(header)
    writer.writerows(sv)
