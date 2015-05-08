<?php
///// Devices dot PHP File 
/*Request Data Format
$rawjson = '{
		"device_id" : "4803001540",
		"groupname" : "123",
		"time_stamp" : ";laskdjf;sf",
		"memory_available" : 268.56,
		"latitude" : 232,
		"longitude" : 300,
		"signal_strength" : 32,
		"apps" :["app_name1", "app_name2", "app_name3"],
	}';
*/

$headers = apache_request_headers();
$token_value = 0;

foreach ($headers as $header => $value) {
    if(strcmp($header,"auth-token") == 0)
		$token_value = $value;
}

$mysqli = new mysqli ( "54.148.66.118", "master", "123456", "addb" );
$token_value = '"'.$mysqli->real_escape_string($token_value).'"';
$validation = $mysqli->query("select username from token where token = $token_value");

if($validation->num_rows == 0){
	http_response_code(403);
	$mysqli->close();
	exit();
}

$method = strtolower ( $_SERVER ['REQUEST_METHOD'] );
$body = file_get_contents ( 'php://input' );

if ($mysqli->connect_errno) {
	printf ( "Connect failed: %s\n", $mysqli->connect_error );
	exit ();
}

$json = json_decode ( $body);

switch ($method) {
	case 'get' :
		get_devices ($_GET["deviceid"], $_GET["groupname"]); 
		break;
	case 'post' :
		add_device (); 
		break;
	case 'put' :
		update_device (); 
		break;
	case 'delete' :
		remove_device ();
		break;
	default :
		http_response_code ( 405 );
}

function get_devices($deviceid, $groupname) {
	global $json;
	global $mysqli;	
	
	$deviceid = '"' . $mysqli->real_escape_string ( $deviceid ) . '"';
	$groupname = '"' . $mysqli->real_escape_string ( $groupname ) . '"';
	$results = $mysqli->query ( "select device_id from device_group where groupname = $groupname" );
	$deviceids = array ();
	$devices = array ();
	
	if ($results){
		while (($row = $results->fetch_array(MYSQLI_NUM)) == TRUE) {
			array_push ( $deviceids, $row[0] );
		}
		
		$results->free();
		
		for($i = 0; $i < count ( $deviceids ); $i++) {
			$deviceidss = '"' . $mysqli->real_escape_string ( $deviceids[$i] ) . '"';
			$results = $mysqli->query ( "select * from device where device_id = $deviceidss" );
			$row = $results->fetch_array(MYSQLI_ASSOC);	
			$apps_results = $mysqli->query("select appname from apps where device_id = $deviceidss");
			
			$appnames = array();
			
			for($j = 0 ;$j < $apps_results->num_rows; $j++){
				$apps_row = $apps_results->fetch_array(MYSQLI_NUM);	
				array_push($appnames,$apps_row[0]);
			}
			
			array_push($row, $appnames);
			array_push($devices,$row);
			
			mysqli_free_result($results);
			mysqli_free_result($apps_results);
		}
		
		$json = json_encode ( $devices );
		http_response_code ( 200 );
		header ( 'Content-type: application/json' );
	} else {
		die ( 'Error : (' . $mysqli->errno . ') ' . $mysqli->error );
	}
	
	$mysqli->close ();
	exit($json);
}

function add_device() {
	global $json;
	global $mysqli;
	
	$deviceid = '"' . $mysqli->real_escape_string ( $json->device_id ) . '"';
	$groupname = '"' . $mysqli->real_escape_string ( $json->groupname ) . '"';
	$timestamp = '"' . $mysqli->real_escape_string ( $json->time_stamp ) . '"';
	$memoryavailable = '"' . $mysqli->real_escape_string ( $json->memory_available) . '"';
	$latitude = '"' . $mysqli->real_escape_string ( $json->latitude ) . '"';
	$longitude = '"' . $mysqli->real_escape_string ( $json->longitude ) . '"';
	$signalstrength = '"' . $mysqli->real_escape_string ( $json->signal_strength ) . '"';
	$apps = $json->apps;
	
	$check = $mysqli->query("select device_id from device_group where device_id = $deviceid");
	
	if($check){
		if($check->num_rows > 0 ){
			$mysqli->close();
			http_response_code(500);
			echo 'Device already in group';
			exit();
		}
	}
	
	$insert_row1 = $mysqli->query ( "insert into device (device_id, time_stamp, memory_available, latitude, longitude, signal_strength) values ($deviceid,$timestamp,$memoryavailable,$latitude,$longitude,$signalstrength)" );
	
	echo $mysqli->error;
	
	$insert_row2 = $mysqli->query ( "insert into device_group (groupname, device_id) values ($groupname, $deviceid)" );
		
	if ($insert_row1 && $insert_row2) {
		http_response_code ( 200 );
	} else {
		echo 'Device cannot be added';
		echo $mysqli->errno;
		$mysqli->close ();
		exit();
	}
	
	for($i = 0 ;$i < count($apps) ; $i++){
		$app_name = '"' . $mysqli->real_escape_string ( $apps[$i] ) . '"';
		$insert_row3 = $mysqli->query ( "insert into apps (appname, device_id) values ($app_name, $deviceid)" );
		
		if($insert_row3)
			http_response_code(200);
		else{
			echo 'Apps for this device cannot be added ';
			$mysqli->close ();
			exit();
		}
	}
	
	$mysqli->close ();
	exit ();
}

function update_device() {
	global $json;
	global $mysqli;
	
	$deviceid = '"' . $mysqli->real_escape_string ( $json->device_id ) . '"';
	$timestamp = '"' . $mysqli->real_escape_string ( $json->time_stamp ) . '"';
	$memoryavailable = '"' . $json->memory_available . '"';
	$latitude = '"' . $json->latitude . '"';
	$longitude = '"' . $json->longitude . '"';
	$signalstrength = '"' . $json->signal_strength . '"';
	$apps = $json->apps;
	
	$update_row = $mysqli->query ( "update device set time_stamp = $timestamp , memory_available = $memoryavailable , latitude = $latitude , longitude = $longitude , signal_strength = $signalstrength where device_id =  $deviceid " );
	
	if ($update_row) {
		http_response_code ( 200 );
	} else {
		echo 'Failed to update the device info';
		$mysqli->close();
		exit();
	}

	$apps_in_table	= $mysqli->query("select appname from apps where device_id = $deviceid");
	$apps_table = array();
	
	for($i=0;$i<$apps_in_table->num_rows; $i++){
		$row = $apps_in_table->fetch_array( MYSQLI_ASSOC );
		array_push($apps_table,$row['appname']);
	}
	
	for($i=0;$i< count($apps);$i++){
		if(!in_array($apps[$i],$apps_table)){
			$app= '"' . $mysqli->real_escape_string ( $apps[$i] ) . '"';
			$mysqli->query("insert into apps (appname, device_id) values ($app, $deviceid)");
			if($mysqli->errno != 0){
				echo 'Error in update';
				$mysqli->close();
				exit();
			}
		}
	}
	
	for($i=0;$i< count($apps_table);$i++){
		if(!in_array($apps_table[$i], $apps)){
			$app= '"' . $mysqli->real_escape_string ( $apps_table[$i] ) . '"';
			$mysqli->query("delete from apps where device_id = $deviceid and appname = $app");
			if($mysqli->errno != 0){
				echo 'Error in update';
				$mysqli->close();
				exit();
			}
		}
	}
	
	$mysqli->close ();
	exit ();
}

function remove_device() {
	global $json;
	global $mysqli;
		
	$deviceid = '"' . $mysqli->real_escape_string ( $json->device_id ) . '"';
	$groupname = '"' . $mysqli->real_escape_string ( $json->groupname ) . '"';
	
	$delete_row1 = $mysqli->query ( "delete from device where device_id = $deviceid" );
	$delete_row2 = $mysqli->query ( "delete from device_group where device_id = $deviceid" );
	$delete_row3 = $mysqli->query (" delete from apps where device_id = $deviceid");
	
	if ($delete_row1 && $delete_row2 && $delete_row3) {
		http_response_code ( 200 );
	} else {
		die ( 'Error : (' . $mysqli->errno . ') ' . $mysqli->error );
	}
	
	$mysqli->close ();
	exit ();
}

if (! function_exists ( 'http_response_code' )) {
	function http_response_code($code = NULL) {
		if ($code !== NULL) {
			
			switch ($code) {
				case 100 :
					$text = 'Continue';
					break;
				case 101 :
					$text = 'Switching Protocols';
					break;
				case 200 :
					$text = 'OK';
					break;
				case 201 :
					$text = 'Created';
					break;
				case 202 :
					$text = 'Accepted';
					break;
				case 203 :
					$text = 'Non-Authoritative Information';
					break;
				case 204 :
					$text = 'No Content';
					break;
				case 205 :
					$text = 'Reset Content';
					break;
				case 206 :
					$text = 'Partial Content';
					break;
				case 300 :
					$text = 'Multiple Choices';
					break;
				case 301 :
					$text = 'Moved Permanently';
					break;
				case 302 :
					$text = 'Moved Temporarily';
					break;
				case 303 :
					$text = 'See Other';
					break;
				case 304 :
					$text = 'Not Modified';
					break;
				case 305 :
					$text = 'Use Proxy';
					break;
				case 400 :
					$text = 'Bad Request';
					break;
				case 401 :
					$text = 'Unauthorized';
					break;
				case 402 :
					$text = 'Payment Required';
					break;
				case 403 :
					$text = 'Forbidden';
					break;
				case 404 :
					$text = 'Not Found';
					break;
				case 405 :
					$text = 'Method Not Allowed';
					break;
				case 406 :
					$text = 'Not Acceptable';
					break;
				case 407 :
					$text = 'Proxy Authentication Required';
					break;
				case 408 :
					$text = 'Request Time-out';
					break;
				case 409 :
					$text = 'Conflict';
					break;
				case 410 :
					$text = 'Gone';
					break;
				case 411 :
					$text = 'Length Required';
					break;
				case 412 :
					$text = 'Precondition Failed';
					break;
				case 413 :
					$text = 'Request Entity Too Large';
					break;
				case 414 :
					$text = 'Request-URI Too Large';
					break;
				case 415 :
					$text = 'Unsupported Media Type';
					break;
				case 500 :
					$text = 'Internal Server Error';
					break;
				case 501 :
					$text = 'Not Implemented';
					break;
				case 502 :
					$text = 'Bad Gateway';
					break;
				case 503 :
					$text = 'Service Unavailable';
					break;
				case 504 :
					$text = 'Gateway Time-out';
					break;
				case 505 :
					$text = 'HTTP Version not supported';
					break;
				default :
					exit ( 'Unknown http status code "' . htmlentities ( $code ) . '"' );
					break;
			}
			
			$protocol = (isset ( $_SERVER ['SERVER_PROTOCOL'] ) ? $_SERVER ['SERVER_PROTOCOL'] : 'HTTP/1.0');
			
			header ( $protocol . ' ' . $code . ' ' . $text );
			
			$GLOBALS ['http_response_code'] = $code;
		} else {
			
			$code = (isset ( $GLOBALS ['http_response_code'] ) ? $GLOBALS ['http_response_code'] : 200);
		}
		
		return $code;
	}
}
?>