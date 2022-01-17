import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
import torch
from torch.autograd import Variable
from torch.utils.data import DataLoader
import matplotlib.pyplot as plt

from model import MultiLayerPerceptron
from dataset import DotaDataset


features_onehot = np.loadtxt('kaggle_input', dtype=float)
radiant_win_onehot = np.loadtxt('kaggle_label', dtype=float)
print(features_onehot[1],radiant_win_onehot[1])
wingolddiff=[]
winexpdiff=[]
losegolddiff=[]
loseexpdiff=[]
for i in range(1000):
    if radiant_win_onehot[i] == 1:
        wingolddiff.append(features_onehot[i][0])
        winexpdiff.append(features_onehot[i][1])
    if radiant_win_onehot[i] == 0:
        losegolddiff.append(features_onehot[i][0])
        loseexpdiff.append(features_onehot[i][1])
print(winexpdiff)
print(wingolddiff)
print(loseexpdiff)
print(losegolddiff)

import xlsxwriter

workbook = xlsxwriter.Workbook('dataAnalysis expdiff.xlsx')
worksheet = workbook.add_worksheet()

row = 1
column = 1

# iterating through content list
for item in winexpdiff:
    # write operation perform
    worksheet.write(row, column, item)

    # incrementing the value of row by one
    # with each iteratons.
    column += 1

row = 5
column = 1

# iterating through content list
for item in loseexpdiff:
    # write operation perform
    worksheet.write(row, column, item)

    # incrementing the value of row by one
    # with each iteratons.
    column += 1

workbook.close()