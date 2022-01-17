
import tensorflow as tf
import keras
from keras.models import Sequential,Model
from keras.layers import Dense,convolutional,Input,Conv2D,BatchNormalization,Activation,UpSampling2D,Dropout
from keras.layers.merge import concatenate,Add
from keras import models, layers
import numpy as np
############dataset
from PIL import Image,ImageOps
import os
import sys
import h5py
import tables
from keras.layers.advanced_activations import LeakyReLU
from matplotlib import pyplot as plt

#'''
#此代码用于做数据（各种图片）的preprocessing

## input training/testing dataset
## here is the code to generate file x_train1_160.h5
## other training dataset are all the same but change the path name/file name
xtrain1_path = "x_train1/"
xtrain1_dirs = os.listdir( xtrain1_path )
x_train1 = []
i=1
j=1
for file in xtrain1_dirs:
    # only need half of the resource images
    if i % 2 == 0:
        print(i)
        img = Image.open('x_train1/'+file)
        ##convert to 160*160 resolution
        img = img.resize((160, 160))
        arr = np.array(img)
        x_train1=np.append(x_train1,arr)
        x_train1 = np.array(x_train1, dtype='float32')
        ##RGB 3 channels
        x_train1 = x_train1.reshape((j, 160, 160, 3))
        j += 1
    i += 1
hf = h5py.File('x_train1_160.h5', 'w')
hf.create_dataset('x_train1', data=x_train1)
hf.close()
##ground truth
## here is the code to generate file y_train1_160.h5
## other training ground truth are all the same but change the path name/file name


ytrain1_path = "y_train1/"
ytrain1_dirs = os.listdir( ytrain1_path )
y_train1 = []
i=1
j=1
for file in ytrain1_dirs:
    # only need half of the resource images
    if i % 2 == 0:
        print(i)
        img = Image.open('y_train1/'+file)
        ##convert to 160*160 resolution
        img = img.resize((160, 160))
        arr = np.array(img)
        y_train1=np.append(y_train1,arr)
        y_train1 = np.array(y_train1, dtype='float32')
        ##mask only have one channel
        y_train1 = y_train1.reshape((j, 160, 160, 1))
        j += 1
    i += 1
hf = h5py.File('y_train1_160.h5', 'w')
hf.create_dataset('y_train1', data=y_train1)
hf.close()
#'''
