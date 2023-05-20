from keras import backend as K
from keras.layers import Conv2D, MaxPooling2D
from keras.layers import Input, Dense, Activation
from keras.layers import Reshape, Lambda, BatchNormalization
from tensorflow.python.keras.layers.merge import add, concatenate
from keras.models import Model
from tensorflow.python.keras.layers.recurrent import LSTM
from parameter import *




def ctc_lambda_func(args):    
    y_pred, labels, input_length, label_length = args
    y_pred = y_pred[:, 2:, :]
    return K.ctc_batch_cost(labels, y_pred, input_length, label_length)

def CRNN(training):
    inputs = Input(name='the_input', shape=input_shape, dtype='float32') 

    # CNN VGG
    #                                커널의 이동량 한칸씩, 두칸씩 // 패딩, 이미지를 원본 이미지의 크기와같게 0으로 채움 빈부
    #       피쳐맵,  커널사이즈(3x3) 스트라이드 1 패딩 1
    x = Conv2D(64, (3, 3), padding='same', name='conv1', kernel_initializer='he_normal')(inputs)  
    x = BatchNormalization()(x)
    x = Activation('relu')(x)
    x = MaxPooling2D(pool_size=(2, 2), name='max1')(x)
    #    MaxPooling2D 는 스트라이드를 설정하지 않으면 풀사이즈와 같게 설정됌

    x = Conv2D(128, (3, 3), padding='same', name='conv2', kernel_initializer='he_normal')(x)  
    x = BatchNormalization()(x)
    x = Activation('relu')(x)
    x = MaxPooling2D(pool_size=(2, 2), name='max2')(x)  

    x = Conv2D(256, (3, 3), padding='same', name='conv3', kernel_initializer='he_normal')(x) 
    x = BatchNormalization()(x)
    x = Activation('relu')(x)
    x = Conv2D(256, (3, 3), padding='same', name='conv4', kernel_initializer='he_normal')(x)  
    x = BatchNormalization()(x)
    x = Activation('relu')(x)
    x = MaxPooling2D(pool_size=(1, 2), name='max3')(x)
    
    x = Conv2D(512, (3, 3), padding='same', name='conv5', kernel_initializer='he_normal')(x)  
    x = BatchNormalization()(x)
    x = Activation('relu')(x)
    x = Conv2D(512, (3, 3), padding='same', name='conv6')(x) 
    x = BatchNormalization()(x)
    x = Activation('relu')(x)
    x = MaxPooling2D(pool_size=(1, 2), name='max4')(x) 

    x = Conv2D(512, (2, 2), padding='same', kernel_initializer='he_normal', name='con7')(x) 
    x = BatchNormalization()(x)
    x = Activation('relu')(x)

    # Convert
    x = Reshape((32,2048))(x) 
    x = Dense(64, activation='relu', kernel_initializer='he_normal', name='dense1')(x)  

    # RNN 
    lstm_1 = LSTM(256, return_sequences=True, kernel_initializer='he_normal', name='lstm1')(x)  
    lstm_1b = LSTM(256, return_sequences=True, go_backwards=True, kernel_initializer='he_normal', name='lstm1_b')(x)
    reversed_lstm_1b = Lambda(lambda inputTensor: K.reverse(inputTensor, axes=1)) (lstm_1b)

    lstm1_merged = add([lstm_1, reversed_lstm_1b])  
    lstm1_merged = BatchNormalization()(lstm1_merged)
    
    lstm_2 = LSTM(256, return_sequences=True, kernel_initializer='he_normal', name='lstm2')(lstm1_merged)
    lstm_2b = LSTM(256, return_sequences=True, go_backwards=True, kernel_initializer='he_normal', name='lstm2_b')(lstm1_merged)
    reversed_lstm_2b= Lambda(lambda inputTensor: K.reverse(inputTensor, axes=1)) (lstm_2b)

    lstm2_merged = concatenate([lstm_2, reversed_lstm_2b]) 
    lstm2_merged = BatchNormalization()(lstm2_merged)

    x = Dense(num_classes, kernel_initializer='he_normal',name='dense2')(lstm2_merged) 
    y_pred = Activation('softmax', name='softmax')(x)


    # CTC 로스 인자 정의
    labels = Input(name='the_labels', shape=[max_text_len], dtype='float32') 
    input_length = Input(name='input_length', shape=[1], dtype='int64')    
    label_length = Input(name='label_length', shape=[1], dtype='int64')    

    # CTC 
    loss_out = Lambda(ctc_lambda_func, output_shape=(1,), name='ctc')([y_pred, labels, input_length, label_length])
    
    if training:
        return Model(inputs=[inputs, labels, input_length, label_length], outputs=loss_out)
    else:
        return Model(inputs=[inputs], outputs=y_pred)
    
