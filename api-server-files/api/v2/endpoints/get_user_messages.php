<?php

// Підключаємо модуль шифрування AES-256-GCM
if (file_exists(__DIR__ . '/../crypto_helper.php')) {
    require_once(__DIR__ . '/../crypto_helper.php');
}

// Підключаємо гибридный middleware если существует
if (file_exists(__DIR__ . '/../hybrid_middleware.php')) {
    require_once(__DIR__ . '/../hybrid_middleware.php');
}

$use_gcm = !empty($_POST['use_gcm']) && $_POST['use_gcm'] == 'true';
$is_worldmates = $use_gcm;

if (!empty($_POST['recipient_id']) && is_numeric($_POST['recipient_id']) && $_POST['recipient_id'] > 0) {
	$json_success_data   = array();
	$user_id         = $wo['user']['id'];
	$user_login_data = $wo['user'];
	if (!empty($user_login_data)) {
		$recipient_id    = $_POST['recipient_id'];
        $user_login_data2 = Wo_UserData($recipient_id);
        if (!empty($user_login_data2)) {

            deleteDisappearingMessages($recipient_id,$user_id);

        	$limit             = 20;
            $after_message_id  = 0;
            $before_message_id = 0;
            $message_id = 0;
            $full_history = false;
            $count_only = false;

            // 🔥 CLOUD BACKUP: Параметр для загрузки всей истории
            if (!empty($_POST['full_history']) && $_POST['full_history'] == 'true') {
                $full_history = true;
                $limit = 10000; // Большой лимит для полной истории
            }

            // 📊 CLOUD BACKUP: Параметр для получения только количества сообщений
            if (!empty($_POST['count_only']) && $_POST['count_only'] == 'true') {
                $count_only = true;
            }

            if (!empty($_POST['limit']) && is_numeric($_POST['limit']) && $_POST['limit'] > 0 && !$full_history) {
                $limit = $_POST['limit'];
            }
            if (!empty($_POST['after_message_id'])) {
                $after_message_id = $_POST['after_message_id'];
            }
            if (!empty($_POST['before_message_id'])) {
                $before_message_id = $_POST['before_message_id'];
            }
            if (!empty($_POST['message_id'])) {
                $message_id = $_POST['message_id'];
            }

            // 📊 Если запрошен только подсчет - возвращаем количество
            if ($count_only) {
                global $sqlConnect;
                $count_query = "SELECT COUNT(*) as total FROM " . T_MESSAGES .
                    " WHERE ((`from_id` = {$recipient_id} AND `to_id` = {$user_id} AND `deleted_two` = '0') " .
                    " OR (`from_id` = {$user_id} AND `to_id` = {$recipient_id} AND `deleted_one` = '0')) " .
                    " AND `page_id` = '0'";
                $count_result = mysqli_query($sqlConnect, $count_query);
                $count_data = mysqli_fetch_assoc($count_result);

                $response_data = array(
                    'api_status' => 200,
                    'total_messages' => intval($count_data['total'])
                );
                // Выход из скрипта после подсчета
                echo json_encode($response_data);
                exit;
            }

            $message_info = array(
                'user_id' => $user_id,
                'recipient_id' => $recipient_id,
                'before_message_id' => $before_message_id,
                'after_message_id' => $after_message_id,
                'message_id' => $message_id
            );

            $message_info = Wo_GetMessagesAPPN($message_info,$limit);
            
                // Подключаем browser_compatibility если он существует
            if (file_exists(__DIR__ . '/../browser_compatibility.php')) {
                require_once(__DIR__ . '/../browser_compatibility.php');
                
                if (class_exists('BrowserCompatibility')) {
                    // Определяем, это браузер или приложение
                    if (BrowserCompatibility::isBrowserRequest()) {
                        error_log("Detected browser request - converting GCM to ECB");
                        BrowserCompatibility::processMessagesForBrowser($message_info);
                    } else {
                        error_log("Detected WorldMates app request - keeping GCM format");
                        // Для WorldMates используем hybrid_middleware
                        if (file_exists(__DIR__ . '/../hybrid_middleware.php') && 
                            class_exists('HybridMiddleware')) {
                            HybridMiddleware::processMessages($message_info, true);
                        }
                    }
                }
            } 
            
                        if (file_exists(__DIR__ . '/../hybrid_middleware.php')) {
                require_once(__DIR__ . '/../hybrid_middleware.php');
                if (class_exists('HybridMiddleware')) {
                    HybridMiddleware::processMessages($message_info);
                }
            }
            $not_include_status = false;
            $not_include_array = array();
            if (!empty($_POST['not_include'])) {
                $not_include_array = @explode(',', $_POST['not_include']);
                $not_include_status = true;
            }
            $timezone = new DateTimeZone($user_login_data['timezone']);

            // Определяем: это браузер или приложение
            $is_browser = empty($_POST['app_version']) && empty($_GET['app_version']);

            foreach ($message_info as $message) {
                // ГІБРИДНА ЛОГІКА: Вибираємо правильну версію шифрування
                if ($is_worldmates) {
                    // WorldMates Messenger: Повертаємо GCM (text, iv, tag, cipher_version)
                    // Сообщения уже зашифрованы в БД, просто используем как есть
                } else {
                    // WoWonder (браузер/оф.приложение)
                    if (!empty($message['text_ecb'])) {
                        if ($is_browser) {
                            // БРАУЗЕР: Дешифруємо ECB і повертаємо plain text
                            $decrypted = CryptoHelper::decryptECB($message['text_ecb'], $message['time']);
                            if ($decrypted !== false) {
                                $message['text'] = $decrypted;
                            } else {
                                $message['text'] = $message['text_ecb'];
                            }
                        } else {
                            // ПРИЛОЖЕНИЕ: Повертаємо зашифрований ECB
                            $message['text'] = $message['text_ecb'];
                        }
                    }
                    // Видаляємо GCM поля для WoWonder клієнтів
                    unset($message['iv']);
                    unset($message['tag']);
                    unset($message['cipher_version']);

                    // Обрабатываем reply тоже
                    if (!empty($message['reply']) && !empty($message['reply']['text_ecb'])) {
                        if ($is_browser) {
                            $decrypted = CryptoHelper::decryptECB($message['reply']['text_ecb'], $message['reply']['time']);
                            if ($decrypted !== false) {
                                $message['reply']['text'] = $decrypted;
                            } else {
                                $message['reply']['text'] = $message['reply']['text_ecb'];
                            }
                        } else {
                            $message['reply']['text'] = $message['reply']['text_ecb'];
                        }
                        unset($message['reply']['iv']);
                        unset($message['reply']['tag']);
                        unset($message['reply']['cipher_version']);
                    }
                }

                if ($not_include_status == true) {
                    foreach ($not_include_array as $value) {
                        if (!empty($value)) {
                            $value = Wo_Secure($value);
                            unset($message[$value]);
                        }
                    }
                }
                if (empty($message['stickers'])) {
                    $message['stickers'] = '';
                }
                $message['time_text'] = Wo_Time_Elapsed_String($message['time']);
                $message_po  = 'left';
                if ($message['from_id'] == $user_id) {
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
                    $time_today  = time() - 86400;
                    if ($message['time'] < $time_today) {
                        $message['time_text'] = date('m.d.y', $message['time']);
                    } else {
                        $time = new DateTime('now', $timezone);
                        $time->setTimestamp($message['time']);
                        $message['time_text'] = $time->format('H:i');
                    }
                }

                if (!empty($message['reply'])) {
                    // Reply сообщения тоже уже зашифрованы в БД, не шифруем повторно
                    if (empty($message['reply']['stickers'])) {
                        $message['reply']['stickers'] = '';
                    }
                    $message['reply']['time_text'] = Wo_Time_Elapsed_String($message['reply']['time']);
                    $message_po  = 'left';
                    if ($message['reply']['from_id'] == $user_id) {
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
                if (!empty($message['story'])) {
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

                // Сообщения УЖЕ зашифрованы в БД - просто возвращаем как есть
                // Поля iv, tag, cipher_version уже есть в $message из БД
                // НЕ шифруем повторно!

                array_push($json_success_data, $message);
            }
            $send_messages_to_phones = Wo_MessagesPushNotifier();
            $typing = 0;
			$check_typing = Wo_IsTyping($recipient_id);
			if ($check_typing) {
			    $typing = 1;
			}
            $is_recording = $db->where('follower_id',$wo['user']['id'])->where('following_id',$recipient_id)->where('is_typing',2)->getValue(T_FOLLOWERS,"COUNT(*)");

            deleteOneTimeMessages();


            $response_data = array('api_status' => 200,
            	                   'messages' => $json_success_data,
            	                   'typing' => $typing,
                                   'is_recording' => $is_recording);

        }
        else{
        	$error_code    = 5;
		    $error_message = 'recipient user not found';
        }
	}
	else{
		$error_code    = 4;
	    $error_message = 'user not found';
	}
}
else{
	$error_code    = 3;
    $error_message = 'recipient_id can not be empty';
}