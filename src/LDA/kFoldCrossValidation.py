from sklearn.tree import DecisionTreeClassifier
from sklearn.svm import SVC
from sklearn.ensemble import RandomForestClassifier
from sklearn.neighbors import KNeighborsClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
from sklearn.naive_bayes import GaussianNB
from sklearn.neural_network import MLPClassifier
from sklearn.linear_model import SGDClassifier
from sklearn.model_selection import cross_validate
import pandas as pd
import warnings
import sys
import os


# clear warnings
def warn(*args, **kwargs):
    pass


warnings.warn = warn


# create header for output table
def create_output_columns(num_folds):
    columns = ["Model"]
    for i in range(num_folds):
        columns.append("Fold " + str(i+1))

    columns.append("AVG")
    print(columns)
    sys.stdout.flush()


# add model split to output table
def create_output_row(cross_val, model):
    # get accuracies
    splits = list(map(lambda x: "%.2f%%" % x, cross_val["test_score"] * 100))
    avg = "{:.2f}%".format(cross_val["test_score"].mean() * 100)

    # add model row to output string
    row = [model]

    # add accuracy of each split to each fold column
    for split in splits:
        row.append(split)

    # add average
    row.append(avg)

    print(row)
    sys.stdout.flush()


# file
path = sys.argv[1]

# number of folds
folds = int(sys.argv[2])

# get models
# allow customization of models later
models = ["DT", "SGD", "NB", "SVM", "KNN", "LR", "LDA", "MLP", "RF"]

if os.path.exists(path):
    # create dataframe from file
    dataframe = pd.read_csv(path)

    # get data and labels
    labels = dataframe["class"].values.reshape(-1, 1)
    data = dataframe.drop(["class"], axis=1)

    # create output table
    create_output_columns(folds)

    # run decision tree
    cv = cross_validate(DecisionTreeClassifier(), data, labels, cv=folds)
    create_output_row(cv, "DT")

    # run SGD
    cv = cross_validate(SGDClassifier(), data, labels, cv=folds)
    create_output_row(cv, "SGD")

    # run naive bayes
    cv = cross_validate(GaussianNB(), data, labels, cv=folds)
    create_output_row(cv, "NB")

    # run support vector machine
    cv = cross_validate(SVC(), data, labels, cv=folds)
    create_output_row(cv, "SVM")

    # run k nearest neighbors
    cv = cross_validate(KNeighborsClassifier(), data, labels, cv=folds)
    create_output_row(cv, "KNN")

    # run logistic regression
    cv = cross_validate(LogisticRegression(), data, labels, cv=folds)
    create_output_row(cv, "LR")

    # run linear discriminant analysis
    cv = cross_validate(LinearDiscriminantAnalysis(), data, labels, cv=folds)
    create_output_row(cv, "LDA")

    # run multi layer perceptron
    cv = cross_validate(MLPClassifier(), data, labels, cv=folds)
    create_output_row(cv, "MLP")

    # run random forest
    cv = cross_validate(RandomForestClassifier(), data, labels, cv=folds)
    create_output_row(cv, "RF")
