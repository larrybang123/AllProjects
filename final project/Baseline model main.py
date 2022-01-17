import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
import torch
from torch.autograd import Variable
from torch.utils.data import DataLoader
import argparse
from dataset import DotaDataset
from Baselinemodel import LogisticRegressionModel
import matplotlib.pyplot as plt



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

torch.manual_seed(10)
train_data, validation_data, train_label, valid_label = train_test_split(picks_onehot, radiant_win_onehot,
                                                                         test_size=0.2, random_state=0)
train_set = DotaDataset(train_data, train_label)
train_loader = DataLoader(train_set, batch_size=3000, shuffle=True)
val_set = DotaDataset(validation_data, valid_label)
val_loader = DataLoader(val_set, batch_size=3000, shuffle=False)



model = LogisticRegressionModel(258)
loss_fnc = torch.nn.CrossEntropyLoss()
learning_rate = 0.5
optimizer = torch.optim.SGD(model.parameters(), lr=learning_rate)

epochs=100
training_loss_epoch_array=[]
training_accuracy = []
valid_loss_epoch_array = []
valid_accuracy = []
numepoch=[]
num=0


def evaluate(model, loader):
    total_corr = 0
    total = 0
    for x, vbatch in enumerate(loader):
        data, labels = vbatch
        prediction = model(data.float())
        _,prediction_results = torch.max(prediction, 1)
        total_corr += (prediction_results == labels.long()).sum()
        total += labels.size(0)

    return total_corr.item()/total

for epoch in range(0, epochs):
        loss_epoch = 0
        total_corr = 0
        total = 0

        for i, batch in enumerate(train_loader, 0):
            data, labels = batch
            optimizer.zero_grad()
            prediction = model(data.float())
            #print(prediction)
            loss = loss_fnc(prediction, labels.long())
            loss.backward()
            optimizer.step()
        trainresult = evaluate(model, train_loader)
        validresult = evaluate(model,val_loader)
        #loss_record.append(loss_epoch)
        training_accuracy .append(trainresult)
        valid_accuracy.append(validresult)
        num+=1
        print(num)
        numepoch.append(num)

print(training_accuracy)
print(valid_accuracy)
print(training_accuracy[-1],valid_accuracy[-1])
plt.plot(numepoch, training_accuracy,label='train')
plt.plot(numepoch, valid_accuracy, label='valid')
plt.xlabel('number of epoch')
plt.ylabel('accuracy')
plt.title('Accuracy vs epoch')
plt.legend(loc=5)
plt.show()


plt.plot(numepoch, training_accuracy,label='train')
plt.plot(numepoch, valid_accuracy, label='valid')
plt.xlabel('number of epoch')
plt.ylabel('accuracy')
plt.title('Accuracy vs epoch')
plt.legend(loc=5)
plt.show()
