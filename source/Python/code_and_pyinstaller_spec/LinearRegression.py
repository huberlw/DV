from sklearn.linear_model import LinearRegression
import pandas as pd
import numpy as np
import math
import sys
import os

from sklearn.metrics import mean_squared_error

# get data file
path = sys.argv[1]

if os.path.exists(path):
    # create dataframe from file
    df = pd.read_csv(path)

    # get data and labels
    quant = df['output'].values.reshape(-1, 1)
    data = df.drop(['output'], axis=1)

    # get LDA
    lr = LinearRegression()

    # train
    lr.fit(data, quant)

    # get weights
    angles = lr.coef_[0].copy()

    # normalize
    largest = max(abs(angles))
    angles = angles / largest

    # get degrees
    for i in range(len(angles)):
        angles[i] = np.arccos(angles[i])
        angles[i] = math.degrees(angles[i])

    # inform DV program of angles
    for i in range(len(angles)):
        print(angles[i])

    # inform DV program of intercept
    norm_intercept = lr.intercept_[0] / largest
    print(norm_intercept)
    print(largest)

    # get predictions# get predictions
    pred = lr.predict(data)

    # inform DV program of RMSE and average value
    print(np.sqrt(mean_squared_error(quant, pred)))
    print((sum(quant) / len(pred))[0])

    # inform DV program of predictions
    for i in range(len(pred)):
        print(pred[i][0] / largest - norm_intercept)
