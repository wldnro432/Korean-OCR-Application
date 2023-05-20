import base64
import io

# data_preprocessing
import itertools

# eval
import cv2
import flask
import numpy as np
import tensorflow as tf
from PIL import Image
from absl import flags
import sys
from flask import jsonify
from flask import request

# east
import lanms
from model2 import EAST_model
import locality_aware_nms as nms_locality

# crnn 
from Model import CRNN
from data_processor import restore_rectangle
from parameter import letters




flags.DEFINE_string('model_path', default='./east_model/', help='trained model saved path')

FLAGS = flags.FLAGS
FLAGS(sys.argv)

app = flask.Flask(__name__)
app.config['JSON_AS_ASCII'] = False

@app.route('/', methods=['GET', 'POST'])
def handle_request():
    return "hello"

@app.route('/east', methods=['GET', 'POST'])
def east():
    global pred_texts

    if request.method == 'POST':
        req = request.json
        one_data = req['image']
        imgdata = base64.b64decode(one_data)
        image = Image.open(io.BytesIO(imgdata))
        image_resized = image.resize((int(image.width / 2), int(image.height / 2)))
        numpy_image = np.array(image_resized)
        server_input_img = cv2.cvtColor(numpy_image, cv2.COLOR_RGB2BGR)

    def resize_image(im, max_side_len=2400):
        '''
        resize image to a size multiple of 32 which is required by the network
        :param im: the resized image
        :param max_side_len: limit of max image size to avoid out of memory in gpu
        :return: the resized image and the resize ratio
        '''
        h, w, _ = im.shape
        resize_w = w
        resize_h = h

        # limit the max side
        if max(resize_h, resize_w) > max_side_len:
            ratio = float(max_side_len) / resize_h if resize_h > resize_w else float(max_side_len) / resize_w
        else:
            ratio = 1.
        resize_h = int(resize_h * ratio)
        resize_w = int(resize_w * ratio)

        resize_h = resize_h if resize_h % 32 == 0 else (resize_h // 32) * 32
        resize_w = resize_w if resize_w % 32 == 0 else (resize_w // 32) * 32
        im = cv2.resize(im, (int(resize_w), int(resize_h)))

        ratio_h = resize_h / float(h)
        ratio_w = resize_w / float(w)

        return im, (ratio_h, ratio_w)

    def detect(score_map, geo_map, score_map_thresh=0.8, box_thresh=0.1, nms_thres=0.2):
        '''
        restore text boxes from score map and geo map
        :param score_map:
        :param geo_map:
        :param score_map_thresh: threshhold for score map
        :param box_thresh: threshhold for boxes
        :param nms_thres: threshold for nms
        :return:
        '''
        if len(score_map.shape) == 4:
            score_map = score_map[0, :, :, 0]
            geo_map = geo_map[0, :, :, ]
        # filter the score map
        xy_text = np.argwhere(score_map > score_map_thresh)
        # sort the text boxes via the y axis
        xy_text = xy_text[np.argsort(xy_text[:, 0])]
        #restore
        text_box_restored = restore_rectangle(xy_text[:, ::-1] * 4, geo_map[xy_text[:, 0], xy_text[:, 1], :])  # N*4*2
        print('{} text boxes before nms'.format(text_box_restored.shape[0]))
        boxes = np.zeros((text_box_restored.shape[0], 9), dtype=np.float32)
        boxes[:, :8] = text_box_restored.reshape((-1, 8))
        boxes[:, 8] = score_map[xy_text[:, 0], xy_text[:, 1]]
        #nms
        boxes = nms_locality.nms_locality(boxes.astype(np.float64), nms_thres)

        if boxes.shape[0] == 0:
            return None

        # here we filter some low score boxes by the average score map, this is different from the orginal paper
        for i, box in enumerate(boxes):
            mask = np.zeros_like(score_map, dtype=np.uint8)
            cv2.fillPoly(mask, box[:8].reshape((-1, 4, 2)).astype(np.int32) // 4, 1)
            boxes[i, 8] = cv2.mean(score_map, mask)[0]
        boxes = boxes[boxes[:, 8] > box_thresh]

        return boxes

    def sort_poly(p):
        min_axis = np.argmin(np.sum(p, axis=1))
        p = p[[min_axis, (min_axis + 1) % 4, (min_axis + 2) % 4, (min_axis + 3) % 4]]
        if abs(p[0, 0] - p[1, 0]) > abs(p[0, 1] - p[1, 1]):
            return p
        else:
            return p[[0, 3, 2, 1]]

    def decode_label(out):
        # out : (1, 32, 42)
        out_best = list(np.argmax(out[0, 2:], axis=1))  # get max index -> len = 32
        out_best = [k for k, g in itertools.groupby(out_best)]  # remove overlap value
        outstr = ''
        for i in out_best:
            if i < len(letters):
                outstr += letters[i]
        return outstr

    # EAST START
    model_east = EAST_model()

    ckpt = tf.train.Checkpoint(step=tf.Variable(0), model=model_east)
    latest_ckpt = tf.train.latest_checkpoint(FLAGS.model_path)

    if latest_ckpt:
        ckpt.restore(latest_ckpt)
        print('global_step : {}, checkpoint is restored!'.format(int(ckpt.step)))

    # 안드로이드에서 받아온 사용자 이미지
    img = server_input_img[:, :, ::-1]
    img_resized, (ratio_h, ratio_w) = resize_image(img)
    img_resized = (img_resized / 127.5) - 1

    # feed image into model
    score_map, geo_map = model_east.predict(img_resized[np.newaxis, :, :, :])
    boxes = detect(score_map=score_map, geo_map=geo_map)

    if boxes is not None:
        boxes = boxes[:, :8].reshape((-1, 4, 2))
        boxes[:, :, 0] /= ratio_w
        boxes[:, :, 1] /= ratio_h

    crop_to_np = []
    if boxes is not None:
        for box in boxes:
            box = sort_poly(box.astype(np.int32))
            if np.linalg.norm(box[0] - box[1]) < 5 or np.linalg.norm(box[3] - box[0]) < 5:
                continue
            c_img = img[box[0, 1]:box[2, 1], box[0, 0]:box[1, 0]]
            img_array = np.array(c_img)
            crop_to_np.append(img_array) # crop_to_np = east 탐지 후 크롭된 이미지 모아놓은 배열

            cv2.polylines(img[:, :, ::-1], [box.astype(np.int32).reshape((-1, 1, 2))], True,
                          color=(255, 255, 0), thickness=1)

        result_image = img[:, :, ::-1] # east 완료 결과 이미지

    np_to_crop_image = np.array(crop_to_np)  # np.arary to image # np_to_crop_image == crop_to_np 배열을 이미지로 읽을 수 있게 변환

    # CRNN
    model_crnn = CRNN(training=False)
    model_crnn.load_weights('./weight/CRNN_01_42.603.ckpt')

    final_list = ""
    for test_img in np_to_crop_image:
        img_numpy = np.array(test_img, 'uint8')
        img = cv2.cvtColor(img_numpy, cv2.COLOR_BGR2GRAY)

        img_pred = img.astype(np.float32)
        img_pred = cv2.resize(img_pred, (128, 64))  # 리사이징
        img_pred = (img_pred / 255.0) * 2.0 - 1.0  # 정규화 0 ~ 1
        img_pred = img_pred.T  # 전치
        img_pred = np.expand_dims(img_pred, axis=-1)
        img_pred = np.expand_dims(img_pred, axis=0)

        net_out_value = model_crnn.predict(img_pred)
        pred_texts = decode_label(net_out_value)
        final_list = final_list + pred_texts + "   "
        print('Predicted: %s' % (pred_texts))

    send_image = base64.b64encode(cv2.imencode('.jpg', result_image)[1]).decode()
    img_dict = {'img':send_image,
                'text':final_list}
    send_image_json = jsonify(img_dict)

    return send_image_json

if __name__ == '__main__':
    app.run(host='0.0.0.0')