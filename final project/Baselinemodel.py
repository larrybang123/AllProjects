import torch.nn as nn
import torch
import torch.nn.functional as F
class LogisticRegressionModel(nn.Module):
    def __init__(self, input_dim):
        super(LogisticRegressionModel, self).__init__()

        self.linear = nn.Linear(input_dim, 2)

    def forward(self, x):
        x=x.view(-1,258)

        x = self.linear(x)

        x=nn.functional.softmax(x)

        return x