import torch.utils.data as data


class DotaDataset(data.Dataset):

    def __init__(self, X, y):

        ######

        # 4.1 YOUR CODE HERE
        self.X = X
        self.y = y
        ######

    def __len__(self):
        return len(self.X)

    def __getitem__(self, index):
        ######

        # 4.1 YOUR CODE HERE
        X = self.X[index]
        y = self.y[index]
        return X, y
        ######
