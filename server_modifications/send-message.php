<?php
// +------------------------------------------------------------------------+
// | @author Deen Doughouz (DoughouzForest)
// | @author_url 1: http://www.wowonder.com
// | @author_url 2: http://codecanyon.net/user/doughouzforest
// | @author_email: wowondersocial@gmail.com
// +------------------------------------------------------------------------+
// | WoWonder - The Ultimate Social Networking Platform
// | Copyright (c) 2018 WoWonder. All rights reserved.
// +------------------------------------------------------------------------+

// HYBRID VERSION: Support both WorldMates (GCM) and Official WoWonder (ECB)

// Підключаємо модуль шифрування AES-256-GCM
if (file_exists(__DIR__ . '/../crypto_helper.php')) {
    require_once(__DIR__ . '/../crypto_helper.php');
}

$response_data = array(
    'api_status' => 400
);

$required_fields = array(
    'user_id',
    'message_hash_id'
);

if (empty($_POST['product_id'])) {
    if (empty($_POST['text']) && $_POST['text'] != 0 && empty($_POST['lat']) && empty($_POST['lng'])) {
    	if (empty($_FILES['file']['name']) && empty($_POST['image_url']) && empty($_POST['gif'])) {
    	    $error_code    = 3;
    	    $error_message = 'file (STREAM FILE) AND text (POST) AND image_url AND gif (POST) are missing, at least one is required';
    	}
    }
}

foreach ($required_fields as $key => $value) {
    if (empty($_POST[$value]) && empty($error_code)) {
        $error_code    = 4;
        $error_message = $value . ' (POST) is missing';
    }
}

// HYBRID: Определяем тип клиента в начале
$use_gcm = !empty($_POST['use_gcm']) && $_POST['use_gcm'] == 'true';

if (empty($error_code)) {
    $recipient_id   = Wo_Secure($_POST['user_id']);
    $recipient_data = Wo_UserData($recipient_id);
    if (empty($recipient_data)) {
        $error_code    = 6;
        $error_message = 'Recipient user not found';
    } else {
        if (empty($_POST['product_id'])) {
            $mediaFilename = '';
            $mediaName     = '';
            if (!empty($_FILES['file']['name'])) {
                $fileInfo      = array(
                    'file' => $_FILES["file"]["tmp_name"],
                    'name' => $_FILES['file']['name'],
                    'size' => $_FILES["file"]["size"],
                    'type' => $_FILES["file"]["type"],
                    'types' => 'jpg,png,jpeg,gif,mp4,m4v,webm,flv,mov,mpeg,mp3,wav'
                );
                $media         = Wo_ShareFile($fileInfo);
                $mediaFilename = $media['filename'];
                $mediaName     = $media['name'];
            }
            else if (!empty($_POST['image_url'])) {
                $url     = $_POST['image_url'];
                $query   = parse_url($url);
                if (!empty($query['scheme']) && !empty($query['host'])) {
                    $fileInfo      = array(
                        'file' => $url,
                        'name' => $url,
                        'size' => 0,
                        'type' => 'jpg',
                        'types' => 'jpg,png,jpeg,gif'
                    );
                    $media         = Wo_ShareFile($fileInfo);
                    $mediaFilename = $media['filename'];
                    $mediaName     = $media['name'];
                }
            }
            $gif = '';
            if (!empty($_POST['gif'])) {
                $gif = Wo_Secure($_POST['gif']);
            }
            $lng = '';
            $lat = '';
            if (!empty($_POST['lat']) && !empty($_POST['lng'])) {
                $lng = Wo_Secure($_POST['lng']);
                $lat = Wo_Secure($_POST['lat']);
            }

            $message_data = array(
                'from_id' => Wo_Secure($wo['user']['user_id']),
                'to_id' => Wo_Secure($recipient_id),
                'media' => Wo_Secure($mediaFilename),
                'mediaFileName' => Wo_Secure($mediaName),
                'time' => time(),
                'type_two' => (!empty($_POST['contact'])) ? 'contact' : '',
                'text' => '',
                'stickers' => $gif,
                'lng' => $lng,
                'lat' => $lat,
            );

    		if (!empty($_POST['text']) || (isset($_POST['text']) && $_POST['text'] === '0') ) {
    		 	$plaintext = Wo_Secure($_POST['text']);

    		 	// HYBRID: Шифруем ПЕРЕД сохранением в БД
    		 	if ($use_gcm && class_exists('CryptoHelper')) {
    		 	    // WorldMates: AES-256-GCM
    		 	    $encrypted = CryptoHelper::encryptGCM($plaintext, $message_data['time']);
    		 	    if ($encrypted !== false) {
    		 	        $message_data['text'] = $encrypted['text'];
    		 	        $message_data['gcm_iv'] = $encrypted['iv'];  // Временно храним
    		 	        $message_data['gcm_tag'] = $encrypted['tag'];
    		 	        $message_data['gcm_version'] = $encrypted['cipher_version'];
    		 	        error_log("send-message.php: GCM encrypted, iv=" . substr($encrypted['iv'], 0, 10));
    		 	    } else {
    		 	        error_log("send-message.php: GCM encryption FAILED");
    		 	        $message_data['text'] = $plaintext;
    		 	    }
    		 	} else {
    		 	    // Official WoWonder: AES-128-ECB
    		 	    $encrypted_text = openssl_encrypt($plaintext, "AES-128-ECB", $message_data['time']);
    		 	    if ($encrypted_text !== false) {
    		 	        $message_data['text'] = $encrypted_text;
    		 	    } else {
    		 	        $message_data['text'] = $plaintext;
    		 	    }
    		 	}
    		}
            else{
                if (empty($lng) && empty($lat) && empty($_FILES['file']['name']) && empty($_POST['image_url']) && empty($_POST['gif'])) {
                    $error_code    = 5;
                    $error_message = 'Please check your details.';
                }
            }
            if (!empty($_POST['message_type'])) {
                $message_data['type_two'] = Wo_Secure($_POST['message_type']);
            }

            // Сохраняем GCM поля отдельно (Wo_RegisterMessage их не поддерживает)
            $gcm_iv = isset($message_data['gcm_iv']) ? $message_data['gcm_iv'] : null;
            $gcm_tag = isset($message_data['gcm_tag']) ? $message_data['gcm_tag'] : null;
            $gcm_version = isset($message_data['gcm_version']) ? $message_data['gcm_version'] : 1;

            // Убираем временные поля перед Wo_RegisterMessage
            unset($message_data['gcm_iv'], $message_data['gcm_tag'], $message_data['gcm_version']);

            if (empty($error_message)) {
                $last_id      = Wo_RegisterMessage($message_data);

                // КРИТИЧНО: Сохраняем GCM поля в БД после Wo_RegisterMessage
                if (!empty($last_id) && $gcm_iv !== null) {
                    $update_result = $db->where('id', $last_id)->update(T_MESSAGES, array(
                        'iv' => $gcm_iv,
                        'tag' => $gcm_tag,
                        'cipher_version' => $gcm_version
                    ));
                    error_log("send-message.php: UPDATE GCM fields - result=" . ($update_result ? 'OK' : 'FAIL'));
                }
            }
        }
        else{
            $last_id = Wo_RegisterMessage(array(
                            'from_id' => Wo_Secure($wo['user']['user_id']),
                            'to_id' => $recipient_id,
                            'time' => time(),
                            'stickers' => '',
                            'product_id' => Wo_Secure($_POST['product_id'])
                        ));
        }
        if (!empty($last_id)) {
            if (!empty($_POST['reply_id']) && is_numeric($_POST['reply_id']) && $_POST['reply_id'] > 0) {
                $reply_id = Wo_Secure($_POST['reply_id']);
                $db->where('id',$last_id)->update(T_MESSAGES,array('reply_id' => $reply_id));
            }
            if (!empty($_POST['story_id']) && is_numeric($_POST['story_id']) && $_POST['story_id'] > 0) {
                $story_id = Wo_Secure($_POST['story_id']);
                $db->where('id',$last_id)->update(T_MESSAGES,array('story_id' => $story_id));
            }


            error_log("send-message.php: Message saved with ID=$last_id");

            $send_message_data = Wo_SendMessageNotifier($last_id);
            error_log("send-message.php: Notifier sent");

            if ($send_message_data == true) {
                $message = Wo_MessageData($last_id);
                error_log("send-message.php: Got message data");

                foreach ($non_allowed as $key => $value) {
                   unset($message['messageUser'][$value]);
                }
                if (!empty($message['reply_id']) && $message['reply_id'] > 0) {
                    $message['reply'] = Wo_MessageData($message['reply_id']);
                    foreach ($non_allowed as $key => $value) {
                       unset($message['reply']['messageUser'][$value]);
                    }
                }
                if (!empty($message['story_id']) && $message['story_id'] > 0) {
                    $message['story'] = Wo_GetStory($message['story_id']);
                    foreach ($non_allowed as $key => $value) {
                       unset($message['story']['user_data'][$value]);
                    }
                    if (!empty($message['story']['thumb']['filename'])) {
                        $message['story']['thumbnail'] = $message['story']['thumb']['filename'];
                        unset($message['story']['thumb']);
                    } else {
                        $message['story']['thumbnail'] = $message['story']['user_data']['avatar'];
                    }
                    $message['story']['time_text'] = Wo_Time_Elapsed_String($message['story']['posted']);
                    $message['story']['view_count'] = $db->where('story_id',$message['story']['id'])->where('user_id',$message['story']['user_id'],'!=')->getValue(T_STORY_SEEN,'COUNT(*)');
                }
                if (empty($message['stickers'])) {
                    $message['stickers'] = '';
                }
                $message['time_text'] = Wo_Time_Elapsed_String($message['time']);
                $message_po  = 'left';
                if ($message['from_id'] == $wo['user']['id']) {
                    $message_po  = 'right';
                }

                $message['position']  = $message_po;
                $message['type']      = Wo_GetFilePosition($message['media']);
                if (!empty($message['stickers']) && strpos($message['stickers'], '.gif') !== false) {
                    $message['type'] = 'gif';
                }
                if ($message['type_two'] == 'contact') {
                    $message['type']   = 'contact';
                }
                $message['type']     = $message_po . '_' . $message['type'];
                $message['product']     = null;
                if (!empty($message['product_id'])) {
                    $message['type']     = $message_po . '_product';
                    $message['product'] = Wo_GetProduct($message['product_id']);
                }
                $message['file_size'] = 0;
                if (!empty($message['media'])) {
                    $message['file_size'] = '0MB';
                    if (file_exists($message['file_size'])) {
                        $message['file_size'] = Wo_SizeFormat(filesize($message['media']));
                    }
                    $message['media']     = Wo_GetMedia($message['media']);
                }
                if (!empty($message['time'])) {
                    $timezone = new DateTimeZone($wo['user']['timezone']);
                    $time_today  = time() - 86400;
                    if ($message['time'] < $time_today) {
                        $message['time_text'] = date('m.d.y', $message['time']);
                    } else {
                        $time = new DateTime('now', $timezone);
                        $time->setTimestamp($message['time']);
                        $message['time_text'] = $time->format('H:i');
                    }
                }
                $message['message_hash_id'] = $_POST['message_hash_id'];

                // HYBRID: Добавляем GCM поля для WorldMates
                if ($use_gcm) {
                    try {
                        // Пробуем получить поля из БД
                        global $db;
                        if (isset($db) && !empty($last_id)) {
                            $gcm_row = $db->where('id', $last_id)->getOne(T_MESSAGES, 'iv, tag, cipher_version');
                            if ($gcm_row) {
                                if (!empty($gcm_row['iv'])) $message['iv'] = $gcm_row['iv'];
                                if (!empty($gcm_row['tag'])) $message['tag'] = $gcm_row['tag'];
                                if (isset($gcm_row['cipher_version'])) $message['cipher_version'] = $gcm_row['cipher_version'];
                                error_log("send-message.php: Added GCM fields from DB");
                            }
                        }
                    } catch (Exception $e) {
                        error_log("send-message.php: GCM fields error - " . $e->getMessage());
                        // Продолжаем без GCM полей
                    }
                }

                error_log("send-message.php: Before response build");

                unset($message['or_text']);
                if (!empty($message['reply'])) {
                    foreach ($non_allowed as $key => $value) {
                       unset($message['reply']['messageUser'][$value]);
                    }

                    $message['reply']['text'] = Wo_Markup($message['reply']['or_text']);
                    if (empty($message['reply']['stickers'])) {
                        $message['reply']['stickers'] = '';
                    }
                    $message['reply']['time_text'] = Wo_Time_Elapsed_String($message['reply']['time']);
                    $message_po  = 'left';
                    if ($message['reply']['from_id'] == $wo['user']['id']) {
                        $message_po  = 'right';
                    }

                    $message['reply']['position']  = $message_po;
                    $message['reply']['type']      = Wo_GetFilePosition($message['reply']['media']);
                    if (!empty($message['reply']['stickers']) && strpos($message['reply']['stickers'], '.gif') !== false) {
                        $message['reply']['type'] = 'gif';
                    }
                    if ($message['reply']['type_two'] == 'contact') {
                        $message['reply']['type']   = 'contact';
                    }
                    $message['reply']['type']     = $message_po . '_' . $message['reply']['type'];
                    $message['reply']['product']     = null;
                    if (!empty($message['reply']['product_id'])) {
                        $message['reply']['type']     = $message_po . '_product';
                        $message['reply']['product'] = Wo_GetProduct($message['reply']['product_id']);
                    }
                    $message['reply']['file_size'] = 0;
                    if (!empty($message['reply']['media'])) {
                        $message['reply']['file_size'] = '0MB';
                        if (file_exists($message['reply']['file_size'])) {
                            $message['reply']['file_size'] = Wo_SizeFormat(filesize($message['reply']['media']));
                        }
                        $message['reply']['media']     = Wo_GetMedia($message['reply']['media']);
                    }
                    if (!empty($message['reply']['time'])) {
                        $time_today  = time() - 86400;
                        if ($message['reply']['time'] < $time_today) {
                            $message['reply']['time_text'] = date('m.d.y', $message['reply']['time']);
                        } else {
                            $time = new DateTime('now', $timezone);
                            $time->setTimestamp($message['reply']['time']);
                            $message['reply']['time_text'] = $time->format('H:i');
                        }
                    }
                }
                $response_data = array(
                    'api_status' => 200,
                    'message_data' => $message,
                    'typing' => Wo_IsTyping($recipient_id) ? 1 : 0
                );
            }
        }
    }
}
