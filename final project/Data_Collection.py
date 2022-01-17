import json
import requests
import time
import pandas as pd

def initiate():
    matches_json = json.loads(r.text)
    matches = pd.io.json.json_normalize(matches_json)

    curr_id = matches.loc[99].match_id
    matches = matches[matches.avg_mmr > 4500]
    matches = matches[matches.lobby_type == 7]
    matches = matches[matches.game_mode == 22]


API_base = "https://api.opendota.com/api"
url_base = API_base + "/publicMatches?api_key=8e3d20cf-e09f-4aec-8126-43ca9e34ba0d"

matches = pd.read_csv('data.csv')
print(matches.columns)
# Had error referencing last item, thus changing this by hand every time
curr_id = 5047275118

# Configuration when the code is reset
#matches = matches.drop(columns=['Unnamed: 0', 'Unnamed: 0.1'])
#matches.to_csv('data.csv')


while matches.shape[0]<200000:
    url = url_base + "&less_than_match_id=" + str(curr_id)
    r = requests.get(url)
    matches_json = json.loads(r.text)
    curr_matches = pd.io.json.json_normalize(matches_json)

    curr_id = curr_matches.loc[99].match_id
    curr_matches = curr_matches[curr_matches.avg_mmr > 4500]
    curr_matches = curr_matches[curr_matches.lobby_type == 7]
    curr_matches = curr_matches[curr_matches.game_mode == 22]

    matches = pd.concat([matches, curr_matches], sort=False)
    matches.to_csv('data.csv')
    if matches.shape[0] % 10 == 0:
        print(matches.shape[0])
    time.sleep(0.2)

