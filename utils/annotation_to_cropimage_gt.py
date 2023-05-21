import json
import os
import cv2
import matplotlib.pyplot as plt
from tqdm import tqdm


data_root_path = './Traffic_Sign/'
save_root_path = './train/'

#test_annotations = json.load(open('./train_annotation.json'))
####################
test_annotations = json.load(open('./train_annotation.json', 'r', encoding='utf-8'))

################
gt_file = open(save_root_path+'gt_train.txt', 'w')

for file_name in tqdm(test_annotations):
    annotations = test_annotations[file_name]
    image = cv2.imread(data_root_path+file_name)
    for idx, annotation in enumerate(annotations):
        x,y,w,h = annotation['bbox']
        if x<= 0 or y<= 0 or w <= 0 or h <= 0:
            continue
        #if img is not None:
        text = annotation['text']
        crop_img = image[y:y+h,x:x+w]
        crop_file_name = file_name[:-4]+'_{:03}.jpg'.format(idx+1)
        
        ########################
        cv2.imwrite(save_root_path+'train/'+crop_file_name, crop_img)
        gt_file.write("{},{}\n".format(crop_file_name, text))
