import pandas as pd
import numpy as np


matches = pd.read_csv('data.csv')
matches = matches[matches.duration>1200] #99599
matches = matches[matches.avg_rank_tier>40]
radiant_won = matches[matches.radiant_win==True]
radiant_lost = matches[matches.radiant_win==False]
radiant_won = radiant_won.sample(radiant_lost.shape[0], random_state = 0)
matches =pd.concat([radiant_won, radiant_lost])
with pd.option_context('display.max_rows', None, 'display.max_columns', None):
    print(matches.describe())
    print(matches[matches.radiant_win==True].shape)
matches.to_csv('data_balanced.csv')
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

np.savetxt('input', picks_onehot)
np.savetxt('label', radiant_win_onehot)

