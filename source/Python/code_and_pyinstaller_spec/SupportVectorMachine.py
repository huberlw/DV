from sklearn.svm import SVC
import pandas as pd
import csv
import sys
import os
from sklearn.metrics import confusion_matrix


# get data file
path = "C:\\Users\\Administrator\\GitHub\\DV\\datasets\\train_integerized_copy.csv"#sys.argv[1]

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
    
    y_pred = svm.predict(data)
    cfm = confusion_matrix(labels, y_pred)
    print(cfm)
    
    sv = svm.support_vectors_
    print(len(sv[0]))
    
    # get support vectors
"""    sv = svm.support_vectors_

    # create header
    header = []
    for i in range(len(sv[0])):
        header.append(f"feature{i}")

    # output csv file
    output = open(os.path.dirname(os.path.realpath(__file__)) + "..\\..\\svm_data.csv", "w")
    writer = csv.writer(output, lineterminator='\n')
    writer.writerow(header)
    writer.writerows(sv)"""
