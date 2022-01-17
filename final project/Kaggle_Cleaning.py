import json
import requests
import time
import pandas as pd
import numpy as np


matches = pd.read_csv('Kaggle raw data.csv')
#with pd.option_context('display.max_rows', None, 'display.max_columns', None):
    #print(matches.describe())
    #print(matches.head())

matches_heroes = matches[['r1_hero', 'r2_hero', 'r3_hero','r4_hero','r5_hero','d1_hero','d2_hero','d3_hero','d4_hero','d5_hero']].to_numpy()
#print((matches_heroes.head()))
matches_radiant_gold = matches[['r1_gold', 'r2_gold', 'r3_gold', 'r4_gold', 'r5_gold']]
matches_dire_gold = matches[['d1_gold', 'd2_gold', 'd3_gold', 'd4_gold', 'd5_gold']]
matches_radiant_gold_sum = matches_radiant_gold.sum(axis=1).to_numpy()
matches_dire_gold_sum = matches_dire_gold.sum(axis=1).to_numpy()
matches_radiant_exp_sum = matches[['r1_xp', 'r2_xp', 'r3_xp', 'r4_xp', 'r5_xp']].sum(axis=1).to_numpy()
matches_dire_exp_sum = matches[['d1_xp', 'd2_xp', 'd3_xp', 'd4_xp', 'd5_xp']].sum(axis=1).to_numpy()

radiant_gold_adv = matches_radiant_gold_sum - matches_dire_gold_sum
radiant_gold_adv = radiant_gold_adv.reshape(1, radiant_gold_adv.shape[0])
radiant_exp_adv = (matches_radiant_exp_sum - matches_dire_exp_sum).reshape(1, matches_radiant_exp_sum.shape[0])
picks_onehot = np.zeros((matches_radiant_exp_sum.shape[0],224), dtype=int)
for i in range(0,matches_radiant_exp_sum.shape[0]):
    for j in range(0,5):
        picks_onehot[i][matches_heroes[i][j] - 1] = 1
        picks_onehot[i][matches_heroes[i][j+5] - 1 + 112] = 1

labels = matches['radiant_win'].to_numpy()
processed_data = np.concatenate([radiant_gold_adv.T, radiant_exp_adv.T, picks_onehot], axis=1)

np.savetxt('kaggle_input', processed_data)
np.savetxt('kaggle_label', labels)


