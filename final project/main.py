import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
import torch
from torch.autograd import Variable
from torch.utils.data import DataLoader
import matplotlib.pyplot as plt

from model import MultiLayerPerceptron
from dataset import DotaDataset


def evaluate(model, val_loader):
    total_corr = 0
    for x, vbatch in enumerate(val_loader):
        feats, label = vbatch
        prediction = model(feats.float())
        corr = (prediction > 0.5).squeeze().int() == label
        total_corr += int(corr.sum())

    return float(total_corr)/len(val_loader.dataset)


picks_onehot = np.loadtxt('input', dtype=int)
radiant_win_onehot = np.loadtxt('label', dtype=int)
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

batch_size = 1000
epochs = 1000
lr = 0.15

train_data, validation_data, train_label, valid_label = train_test_split(picks_onehot, radiant_win_onehot,
                                                                         test_size=0.2, random_state=0)
train_set = DotaDataset(train_data, train_label)
train_loader = DataLoader(train_set, batch_size=batch_size, shuffle=True)
val_set = DotaDataset(validation_data, valid_label)
val_loader = DataLoader(val_set, batch_size=batch_size, shuffle=False)

if torch.cuda.is_available():
    torch.set_default_tensor_type(torch.cuda.FloatTensor)

model = MultiLayerPerceptron(258)
model.cuda()
loss_fnc = torch.nn.MSELoss()
optimizer = torch.optim.SGD(model.parameters(), lr=lr)

training_accuracy = []
validation_accuracy = []
loss_record = []

for epoch in range(0, epochs):
    loss_epoch = 0
    x = 0
    for i, tbatch in enumerate(train_loader):
        feats, label = tbatch
        optimizer.zero_grad()
        prediction = model(feats.float())
        loss = loss_fnc(input=prediction.squeeze(), target=label.float())
        loss.backward()
        loss_epoch += loss.item()
        optimizer.step()
        x = x + 1
    validation_accuracy.append(evaluate(model, val_loader))
    training_accuracy.append(evaluate(model, train_loader))

    print(epoch)
    print(loss_epoch/x)
    loss_record.append(loss_epoch)
    print(validation_accuracy[epoch])
    print(training_accuracy[epoch])

final_val_accuracy = evaluate(model, val_loader)
print("final validation accuracy = {}".format(final_val_accuracy))
x = np.linspace(0, epochs, epochs)
plt.figure()
plt.plot(x, training_accuracy, label='Training')
plt.plot(x, validation_accuracy, label='Validation')
plt.xlabel('Number of Epochs')
plt.ylabel('Accuracy')
plt.legend(loc=5)
plt.title("Accuracy vs. Epochs")
plt.show()
plt.figure()
plt.plot(x, loss_record)
plt.xlabel('Number of Epochs')
plt.ylabel('Loss')
plt.title("Training Loss vs. Epochs")
plt.show()
