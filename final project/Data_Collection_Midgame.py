import json
import requests
import time
import pandas as pd
import numpy as np

matches = pd.read_csv('data_balanced.csv')
print(matches.describe())
match_ids = matches["match_id"].to_numpy()

API_base = "https://api.opendota.com/api/matches/"
API_key = "?api_key=8e3d20cf-e09f-4aec-8126-43ca9e34ba0d"

matches = pd.DataFrame(index = np.arange(0, matches.shape[0]), columns=["radiant_gold_adv", "radiant_xp_adv"])

for i in range(0, 100):# matches.shape[0]):
    url = API_base + str(match_ids[i]) #+ API_key
    print(url)
    #r = requests.get(url).json()
    r = requests.get(url).json()
    print("hi")
    print(r["radiant_gold_adv"])
    if r["radiant_gold_adv"] is not None:
        matches.loc[i] = [r["radiant_gold_adv"][10], r["radiant_xp_adv"][10]]
    time.sleep(1)

print(matches.describe())