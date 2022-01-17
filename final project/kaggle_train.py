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


features_onehot = np.loadtxt('kaggle_input', dtype=float)
radiant_win_onehot = np.loadtxt('kaggle_label', dtype=float)

batch_size = 1000
epochs = 100
lr = 0.15

train_data, validation_data, train_label, valid_label = train_test_split(features_onehot, radiant_win_onehot,
                                                                         test_size=0.2, random_state=0)
train_set = DotaDataset(train_data, train_label)
train_loader = DataLoader(train_set, batch_size=batch_size, shuffle=True)
val_set = DotaDataset(validation_data, valid_label)
val_loader = DataLoader(val_set, batch_size=batch_size, shuffle=True)

if torch.cuda.is_available():
    torch.set_default_tensor_type(torch.cuda.FloatTensor)

model = MultiLayerPerceptron(226)
model.cuda()
loss_fnc = torch.nn.MSELoss()
optimizer = torch.optim.SGD(model.parameters(), lr=lr)

training_accuracy = []
validation_accuracy = []

for epoch in range(0, epochs):
    for i, tbatch in enumerate(train_loader):
        feats, label = tbatch
        optimizer.zero_grad()
        prediction = model(feats.float())
        loss = loss_fnc(input=prediction.squeeze(), target=label.float())
        loss.backward()
        optimizer.step()
    validation_accuracy.append(evaluate(model, val_loader))
    training_accuracy.append(evaluate(model, train_loader))
    print(loss)
    print(epoch)
    print(validation_accuracy[epoch])
    print(training_accuracy[epoch])

final_val_accuracy = evaluate(model, val_loader)
print("final validation accuracy = {}".format(final_val_accuracy))
x = np.linspace(0, epochs, epochs)
plt.figure()
plt.plot(x, training_accuracy, label='Training')
plt.plot(x, validation_accuracy, label='Validation')
plt.xlabel('Number of epochs')
plt.ylabel('Accuracy')
plt.legend(loc=5)
plt.title("Training Accuracy vs. epoch")
plt.show()