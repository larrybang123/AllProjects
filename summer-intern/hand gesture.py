#这是一个用于辨别手势的code


###load the model
#model = load_model("my_model_withflip.h5")
#model = load_model("my_model_noAug.h5")
try:

    import os
    import keras
    import cv2
    import matplotlib.style as style
    import numpy as np
    import tables
    from PIL import Image
    from keras import optimizers
    from keras.applications import VGG16
    from keras.callbacks import Callback
    from keras.layers import Dense, Dropout, Flatten
    from keras.layers import Input
    from keras.models import Model
    from keras.preprocessing import image as image_utils
    from keras.preprocessing.image import ImageDataGenerator
    from keras.utils import to_categorical
    from sklearn.metrics import classification_report, confusion_matrix
    from keras.models import load_model


    model = load_model("my_model.h5")
    gesture_names = {0: 'palm',
                     1: 'l',
                     2: 'fist',
                     3: 'fist_moved',
                     4: 'thumb',
                     5: 'index',
                     6: 'ok',
                     7: 'palm_moved',
                     8: 'c',
                     9: 'down'}
    ##predict function return prediction gesture and the probability
    def predict_image(image):
        image = np.array(image, dtype='float32')
        image /= 255
        pred_array = model.predict(image)
        #print(pred_array)

        # model.predict() returns an array of probabilities -
        # np.argmax grabs the index of the highest probability.
        result = gesture_names[np.argmax(pred_array)]
        score = float("%0.2f" % (max(pred_array[0]) * 100))
        return result,score
    #initiate camera
    print("start")
    camera = cv2.VideoCapture(0)

    while camera.isOpened():
        #ret returns True if camera is running, frame grabs each frame of the video feed
        ret, frame = camera.read()

        gray=cv2.cvtColor(frame,cv2.COLOR_BGR2GRAY)
        blur=cv2.GaussianBlur(gray,(41,41),0)
        ret2,gray=cv2.threshold(blur,100,255,cv2.THRESH_BINARY)
        roi=gray[100:400,100:400]
        cv2.rectangle(frame,(100,100),(400,400),(0,255,0),0)
        cv2.putText(frame, text="please press space to detect gesture and ESC to close",org=(50,500),fontFace=1,fontScale=1.5,color=(0,255,0))

        k = cv2.waitKey(10)
        if k ==27:
            break
        if k==32:
            roi=cv2.resize(roi,(224,224))
            roi=roi.reshape(1,224,224,1)
            prediction,score=predict_image(roi)
            print("prediction is [" + prediction+"] with probability of %"+ str(score))
            cv2.putText(frame, text= prediction+"   "+  str(score)+ "%",org=(50,50),fontFace=1,fontScale=1.5,color=(0,255,0))
        cv2.imshow("Hand gesture recoginition", frame)
except Exception as error:
    print('Looks like there is something wrong. Please check your camera, your library (keras,cv2) and rerun the program')
    print('The Error is: '+ str (error))
