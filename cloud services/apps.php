<?php
/*
{
 "device_id" : "4803001540",
 "appname": "app_name1",
 "app_lock" : false
}
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
	case 'get':
		get_app_locks($_GET["deviceid"]);
		break;
	case 'put' :
		lock_app(); 
		break;
	default :
		http_response_code ( 405 );
}

function get_app_locks($deviceid){
	global $json;
	global $mysqli;	
	
	$deviceid = '"' . $mysqli->real_escape_string ( $deviceid ) . '"';
	$results = $mysqli->query ( "select appname, app_lock from apps where device_id = $deviceid" );
	$app_results = array();
	
	if ($results){
		while (($row = $results->fetch_array(MYSQLI_NUM)) == TRUE) 
				$app_results[$row[0]] = $row[1];
		
		$results->free();
		$json = json_encode ($app_results,JSON_FORCE_OBJECT);
		http_response_code ( 200 );
		header ( 'Content-type: application/json' );
	} else {
		die ( 'Error : (' . $mysqli->errno . ') ' . $mysqli->error );
	}
	
	$mysqli->close ();
	exit($json);
}

function lock_app(){
	global $json;
	global $mysqli;
	
	$deviceid = '"' . $mysqli->real_escape_string ( $json->device_id ) . '"';
	$appname = '"' . $mysqli->real_escape_string ( $json->appname ) . '"';
	$lock = '"' . $mysqli->real_escape_string ( $json->app_lock ) . '"';
	
	$update_row = $mysqli->query ( "update apps set app_lock = $lock where device_id =  $deviceid and appname = $appname" );
	
	if($update_row){
		http_response_code(200);
	}else{
		die ( 'Error : (' . $mysqli->errno . ') ' . $mysqli->error );
	}
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