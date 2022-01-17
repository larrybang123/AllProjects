#用于给客户demo的code


try:
    import cv2
    import numpy as np
    from keras.models import load_model
    import cvlib as cv
    from skimage.color import rgb2gray

    print('start demo')
    # face_cascade = cv2.CascadeClassifier("haarcascade_frontalface_default.xml")
    model = load_model("4model4.h5")
    #model = load_model("models/new_trained_model4.h5")
    face_model = load_model('face_ornot4.h5')
    camera = cv2.VideoCapture(0)

    # camera.set(3,640)
    # camera.set(4,480)
    num=1
    while True:
        ret, frame = camera.read()

        # roi=frame[100:500,100:500]
        outputframe=frame
        #frame=cv2.blur(frame,(2,2))
        #gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        # Detect faces using cascade face detection



        output = np.asarray(frame, dtype=np.uint8)
        # cv2.rectangle(frame,(100,100),(500,500),(0,255,0),0)

        input = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        input = cv2.resize(input, (160, 160))
        #input = cv2.resize(input, (320, 320))
        inputarray = []
        arr = np.array(input)
        inputarray = np.append(inputarray, arr)
        inputarray = np.array(inputarray, dtype='float32')
        inputarray = inputarray.reshape((1, 160, 160, 3))
        #inputarray = inputarray.reshape((1, 320, 320, 3))
        inputarray /= 255

        inputarray = rgb2gray(inputarray)
        inputarray = inputarray.reshape(1, 160, 160, 1)
        #inputarray = inputarray.reshape(1, 320, 320, 1)

        pred_array = model.predict(inputarray[:1])
        pred_array = pred_array[0].reshape(160, 160)
        #pred_array = pred_array[0].reshape(320, 320)
        pred_array = 1 - pred_array

        # print(pred_array.shape)
        image_8bit = np.uint8(pred_array * 255)

        #print(k)
        #image_8bit=image_8bit.resize(160,160)
        face_input=image_8bit.reshape(1,160,160,1)

        pred =  face_model.predict(face_input)
        #print(np.argmax(pred[0]))


        ret, thresh = cv2.threshold(image_8bit, 100, 200, 0)
        thresh = cv2.resize(thresh, (output.shape[1], output.shape[0]))

        items = cv2.findContours(thresh, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
        contours = items[0] if len(items) == 2 else items[1]

        # contours, hierarchy = cv2.findContours(thresh, cv2.RETR_TREE, cv2.CHAIN_APPROX_NONE)
        # contours, hierarchy = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)
        # cnts = imutils.grab_contours(items)
        #cv2.drawContours(outputframe, contours, -1, (0, 255, 0), 3)

        #contourarray = np.array(contours)
        '''
        contourarray=[]
    
        for i,cnt in enumerate(contours):
            if cv2.contourArea(cnt)>1000:
                cv2.drawContours(frame, [cnt], -1, (0, 255, 0), 3)
                contourarray.append(cnt)
        contourarray=np.array(contourarray)
        '''
        #'''
        contourarray=[]
        precontourarray=[]
        if np.argmax(pred[0])==1:
        #if 1 == 1:
            area=[]
            for i,cnt in enumerate(contours):
                if cv2.contourArea(cnt) >=1000:
                    area.append(cv2.contourArea(cnt))
                    precontourarray.append(cnt)
            sortarea=sorted(area)
            n=len(sortarea)
            threshold=n//3
            #threshold=n-threshold
            #print(sortarea)
            #print(sortarea[-1])

        #'''
            if area!=[]:
                #if n>=2:
                    for cnt in precontourarray:
                        if  cv2.contourArea(cnt) >= sortarea[-threshold] :
                            #cv2.drawContours(outputframe, [cnt], -1, (0, 255, 0), 3)
                            contourarray.append(cnt)
                #if n==1:
                 #   for cnt in precontourarray:
                        #if  cv2.contourArea(cnt) >= sortarea[-1] :
                            #cv2.drawContours(outputframe, [cnt], -1, (0, 255, 0), 3)
                  #          contourarray.append(cnt)

        #'''
        #print(len(precontourarray))
        #print('------------------')
        #print(len(contourarray))
        '''
        if np.argmax(pred[0]) == 0:
                area = []
                for i, cnt in enumerate(contours):
                    if cv2.contourArea(cnt) >=5000:
                        area.append(cv2.contourArea(cnt))
                sortarea = sorted(area)
                n = len(sortarea)
                threshold = n // 2
                # threshold=n-threshold

                if area != []:

                        for i, cnt in enumerate(contours):
                            if cv2.contourArea(cnt) >= sortarea[threshold]:
                                # cv2.drawContours(outputframe, [cnt], -1, (0, 255, 0), 3)
                                contourarray.append(cnt)
        contourarray = np.array(contourarray)
        '''
            #'''
        face, confidence = cv.detect_face(frame)

        #cv2.drawContours(frame, contours, -1, (0, 255, 0), 3)
        # loop through detected faces
        for idx, f in enumerate(face):
            # get corner points of face rectangle
            (startX, startY) = f[0], f[1]
            (endX, endY) = f[2], f[3]
            width=endX-startX
            height=endY-startY
            # draw rectangle over face
            cv2.rectangle(outputframe, (startX, startY), (endX, endY), (0, 255, 0), 2)
            '''
            for array in contourarray:
                # print(array)
                for array2 in array:
                    if startX-width/10 < array2[0][0] < endX+width/10 and startY-height/10 < array2[0][1] < endY+height/10:
                        cv2.putText(outputframe, text='hand touch face', org=(startX+20, startY-20), fontFace=1, fontScale=1.5,
                                    color=(255, 255, 255))
                        cv2.rectangle(outputframe, (startX, startY), (endX, endY), (0, 0, 255), 2)
                        break
            '''
        #'''
            for c in contourarray:

                M = cv2.moments(c)
                cX = int(M["m10"] / M["m00"])
                cY = int(M["m01"] / M["m00"])

                if startX + ((endX - startX) / 5) < cX < endX - ((endX - startX) / 5) and startY + (
                        (endY - startY) / 5) < cY < endY - ((endY - startY) / 5):
                #if startX   < cX < endX  and startY  < cY < endY:
                    break
                else:
                    #cv2.circle(frame, (cX, cY), 7, (255, 255, 255), -1)

                    for array2 in c:
                        if startX < array2[0][0] < endX and startY < array2[0][1] < endY:
                            cv2.putText(outputframe, text='hand touch face', org=(startX+10, startY+10), fontFace=1, fontScale=1.5,
                                        color=(255, 255, 255))
                            cv2.rectangle(outputframe, (startX, startY), (endX, endY), (0, 0, 255), 2)
                            break
        #'''
        #'''

         #   '''
        # '''

        # c=max(contours,key=cv2.contourArea)
        # '''
        # cv2.drawContours(output, c, -1, (0, 255, 0), 3)
        k = cv2.waitKey(10)
        if k == 27:
            break

        if k==32:
            cv2.imwrite('output2/'+str(num)+'.jpg',image_8bit)
            num+=1
            print(num)
        # cv2.imshow('test1', thresh)

        cv2.imshow('test1', outputframe)

    # mg=cv2.imread('x_test/4_P_hgr1_id01_1.jpg')
    # output=np.asarray(img, dtype=np.uint8)
    # img = cv2.resize(img,(320, 320))
    # img=cv2.cvtColor(img,cv2.COLOR_BGR2RGB)

    # x_train1=[]
    # arr = np.array(img)
    # x_train1 = np.append(x_train1, arr)
    # x_train1 = np.array(x_train1, dtype='float32')
    # x_train1 = x_train1.reshape((1, 320, 320, 3))
    # x_train1 /= 255
    ##predict
    # pred_array = model.predict(x_train1[:1])
    # pred_array=pred_array[0].reshape(320,320)
    # imgray=pred_array

    ##display output
    # cv2.imshow('IMAGE GRAY',imgray)
    # cv2.waitKey(0)
    # cv2.destroyAllWindows()
    ##draw contours
    # image_8bit = np.uint8(imgray * 255)
    # ret, thresh = cv2.threshold(image_8bit ,100,255,0)
    # thresh=cv2.resize(thresh,(output.shape[1],output.shape[0]))
    # contours,hierarchy= cv2.findContours(thresh,cv2.RETR_TREE,cv2.CHAIN_APPROX_NONE)

    ###display original pic with contours
    # cv2.drawContours(output,contours,-1,(0,255,0),3)
    # cv2.imshow('IMAGE GRAY',output)
    # cv2.waitKey(0)
    cv2.destroyAllWindows()
except Exception as error:
    print('Looks like there is something wrong. Please check your camera, check if install requirement packages, check if download needed models')
    print('for packages: pip install scikit-image/ pip install keras/ pip install opencv-python/ pip install cvlib')
    print('The Error is: '+ str (error))

