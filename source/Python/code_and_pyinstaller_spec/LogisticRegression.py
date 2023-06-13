from sklearn.linear_model import LinearRegression
import pandas as pd
import numpy as np
import math
import csv
import sys
import os

# get data file
path = "D:\\GitHub\\DV\\datasets\\DV_data.csv"# sys.argv[1]

if os.path.exists(path):
    # create dataframe from file
    df = pd.read_csv(path)

    # get data and labels
    quant = df['feature3'].values.reshape(-1, 1).ravel()
    data = df.drop(['feature3'], axis=1)

    # get LDA
    lr = LinearRegression()

    # train
    lr.fit(data, quant)
    score = lr.score(data, quant)
    print(score)

    """co = lr.coef_.copy()
    co = co / max(abs(co))
    lr.coef_ = co"""

    # get weights
    angles = lr.coef_.copy()

    # normalize
    angles = angles / max(abs(angles))

    # get degrees
    for i in range(len(angles)):
        angles[i] = np.arccos(angles[i])
        angles[i] = math.degrees(angles[i])

    # get predictions
    # pred = lr.predict(data)

    tmp = lr.coef_.copy()
    # pred = pred / max(abs(tmp))

    # store angles and predictions
    with open("D:\\GitHub\\DV\\datasets\\regression_info.csv", 'w') as f:
        # using csv.writer method from CSV package
        write = csv.writer(f)

        write.writerow(angles)
        write.writerow([lr.intercept_])
        # write.writerow(pred)
        write.writerow([max(abs(tmp))])
