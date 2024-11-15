from sklearn.tree import DecisionTreeClassifier
from sklearn.svm import SVC
from sklearn.ensemble import RandomForestClassifier
from sklearn.neighbors import KNeighborsClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
from sklearn.naive_bayes import GaussianNB
from sklearn.neural_network import MLPClassifier
from sklearn.linear_model import SGDClassifier
from sklearn.metrics import accuracy_score
import pandas as pd
import statistics
import warnings
import sys
import os


# clear warnings
def warn(*args, **kwargs):
    pass


warnings.warn = warn


# train and evaluate classifiers
def train_and_evaluate_classifier(classifier, train_data, train_labels, test_data, test_labels):
    # Train the classifier
    classifier.fit(train_data, train_labels.ravel())

    # Predict on the test data
    predictions = classifier.predict(test_data)

    # Calculate the accuracy
    accuracy = accuracy_score(test_labels, predictions)

    # Output the accuracy result
    return accuracy


# computes average and standard deviation of list of numbers
def compute_statistics(numbers):
    average = statistics.mean(numbers)
    std_deviation = statistics.stdev(numbers)
    return average, std_deviation


# file
train_path = sys.argv[1]
test_path = sys.argv[2]

# get models
# allow customization of models later
models = ["DT", "SGD", "NB", "SVM", "KNN", "LR", "LDA", "MLP", "RF"]
output = []

if os.path.exists(train_path) and os.path.exists(test_path):
    # create dataframe from file
    train_dataframe = pd.read_csv(train_path)
    test_dataframe = pd.read_csv(test_path)

    # get data and labels
    train_labels = train_dataframe["class"].values.reshape(-1, 1)
    train_data = train_dataframe.drop(["class"], axis=1)

    test_labels = test_dataframe["class"].values.reshape(-1, 1)
    test_data = test_dataframe.drop(["class"], axis=1)

    # run classifiers
    output.append(train_and_evaluate_classifier(DecisionTreeClassifier(), train_data, train_labels, test_data, test_labels))
    output.append(train_and_evaluate_classifier(SGDClassifier(), train_data, train_labels, test_data, test_labels))
    output.append(train_and_evaluate_classifier(GaussianNB(), train_data, train_labels, test_data, test_labels))
    output.append(train_and_evaluate_classifier(SVC(), train_data, train_labels, test_data, test_labels))
    output.append(train_and_evaluate_classifier(KNeighborsClassifier(), train_data, train_labels, test_data, test_labels))
    output.append(train_and_evaluate_classifier(LogisticRegression(), train_data, train_labels, test_data, test_labels))
    output.append(train_and_evaluate_classifier(LinearDiscriminantAnalysis(), train_data, train_labels, test_data, test_labels))
    output.append(train_and_evaluate_classifier(MLPClassifier(), train_data, train_labels, test_data, test_labels))
    output.append(train_and_evaluate_classifier(RandomForestClassifier(), train_data, train_labels, test_data, test_labels))

    # compute statistics
    avg, sd = compute_statistics(output)

    # print results
    for i in range(len(models)):
        print(f"{output[i] * 100:.2f}%")#print(f"{models[i]}: {output[i] * 100:.2f}%")

    #print(f"{avg * 100:.2f}%")#print(f"Average: {avg * 100:.2f}%")
    #print(f"{sd * 100:.2f}")#print(f"Standard Deviation: {sd * 100:.2f}")
