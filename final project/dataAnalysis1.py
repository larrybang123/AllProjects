import argparse
from time import time

import numpy as np
import pandas as pd
from sklearn.preprocessing import LabelEncoder, OneHotEncoder
from sklearn.model_selection import train_test_split

from sklearn.utils import shuffle

import torch
from torch.autograd import Variable
from torch.utils.data import DataLoader

from model import MultiLayerPerceptron
from dataset import DotaDataset


import matplotlib
import matplotlib.pyplot as plt
from matplotlib.pyplot import imshow

from scipy.signal import savgol_filter

picks_onehot=np.loadtxt('input',dtype=float)
radiant_win_onehot=np.loadtxt('label',dtype=float)

#Note since our file'input' is too large to push onto the Github, same procedure below can produce the
#input one hot vector 'picks_onehot' and corresponding label 'radiant_win_onehot'
'''
matches = pd.read_csv('data_balanced.csv')
data = matches[['dire_team', 'radiant_team','radiant_win']]
#data['radiant_team'] = data['radiant_team'] + 129
dire_picks= data['dire_team'].to_numpy()
radiant_picks = data['radiant_team'].to_numpy()
radiant_win = data['radiant_win'].to_numpy()
picks_onehot = np.zeros((dire_picks.size,258), dtype=int)
radiant_win_onehot = np.zeros(dire_picks.size, dtype=int)
for i in range(0,dire_picks.size):
    dire_picks[i] = np.asarray(dire_picks[i].split(",")).astype(int)
    radiant_picks[i] = np.asarray(radiant_picks[i].split(",")).astype(int)
    if radiant_win[i]:
        radiant_win_onehot[i] = 1
    else:
        radiant_win_onehot[i] = 0
    for j in range(0,5):
        picks_onehot[i][dire_picks[i][j] - 1] = 1
        picks_onehot[i][radiant_picks[i][j] - 1 + 129] = 1
'''

winrateforall =[]
#This is the overall win rate for each hero
#winrate overall
for j in range(129):
    wincount = 0
    pickcount=0
    for i in range(len(picks_onehot)):
        #if it is on dire team
        if picks_onehot[i][j]==1:
            pickcount+=1
            #dire team win
            if radiant_win_onehot[i]==0:
             wincount+=1
        # if it is on radiant team
        if picks_onehot[i][j+129]==1:
            pickcount+=1
            #radiant team win
            if radiant_win_onehot[i]==1:
             wincount+=1
    if pickcount==0:
        winrate='NL'
    else:
        winrate=round(float(wincount/pickcount),4)
    winrateforall.append(winrate)
print(winrateforall)

#put the data into Excel so can produce some statistic features
'''
import xlsxwriter

workbook = xlsxwriter.Workbook('dataAnalysis winrateforall.xlsx')
worksheet = workbook.add_worksheet()

row = 1
column = 1

# iterating through content list
for item in winrateforall:
    # write operation perform
    worksheet.write(row, column, item)

    # incrementing the value of row by one
    # with each iteratons.
    column += 1

workbook.close()
'''
#each hero's winrate against other heros

winvsrate_for_all=[]
for j in range(129):
    winvsrate_for_onehero=[]

    for k in range(129):
        wincount = 0
        vscount = 0
        for i in range(len(picks_onehot)):
            #if picked hero is on dire team
            if picks_onehot[i][j]==1:
                #so opponents are on radiant team
                if picks_onehot[i][k+129]==1:
                    vscount +=1
                    if radiant_win_onehot[i] == 0:
                        wincount += 1
            # if picked hero is on radiant team
            if picks_onehot[i][j+129]==1:
                #so opponents are on dire team
                if picks_onehot[i][k]==1:
                    vscount +=1
                    if radiant_win_onehot[i] == 0:
                        wincount += 1
        if vscount ==0:
            winrate = 'NL'
        else:
            winrate=round(float(wincount/vscount),4)
        winvsrate_for_onehero.append(winrate)
    winvsrate_for_all.append(winvsrate_for_onehero)
# a dimension 129*129 matrix element winvsrate_for_all and winvsrate_for_all[i] represents the winrate for the ith hero against
#all other 128 heros and winvsrate_for_all[i][j] is the win rate when ith hero facing jth hero. note winvsrate_for_all[i][i]
#means the hero is facing himself which is impossible so the value will be 'NL'

'''
workbook = xlsxwriter.Workbook('data Analysis.xlsx')
worksheet = workbook.add_worksheet()
row = 5

for i in range(len(winvsrate_for_all)):
    col = 1
    for j in range(len(winvsrate_for_all[0])):

        worksheet.write(row, col, winvsrate_for_all[i][j])
        col+=1
    row += 1

workbook.close()
'''




