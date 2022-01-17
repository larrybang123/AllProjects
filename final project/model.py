import torch.nn as nn
import torch


class MultiLayerPerceptron(nn.Module):

    def __init__(self, input_size):

        super(MultiLayerPerceptron, self).__init__()

        self.input_size = input_size
        self.output_size = 1
        self.fc1 = nn.Linear(226, 50)
        self.relu = nn.ReLU()
        self.fc2 = nn.Linear(50,1)
        #self.fc3 = nn.Linear(25, 10)
        #self.fc4 = nn.Linear(10, 1)

    def forward(self, features):
        x = self.relu(self.fc1(features))
        x = self.relu(self.fc2(x))
        #x = self.relu(self.fc3(x))
        #x = self.relu(self.fc4(x))
        x = torch.sigmoid(x)
        return x



