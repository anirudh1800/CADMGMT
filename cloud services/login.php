<?php

$method = strtolower ( $_SERVER ['REQUEST_METHOD'] );
$body = file_get_contents ( 'php://input' );

$rawjson = '{
		"username" : "qwerr",
		"password" : "123"

	}';

$json = json_decode ( $body );

switch ($method) {
	
	case 'post' :
		authenticate ();
		break;
	
	default :
		http_response_code ( 405 );
}
function authenticate() {
	global $json;
	$mysqli = new mysqli ( "54.148.66.118", "master", "123456", "addb" );
	if ($mysqli->connect_errno) {
		printf ( "Connect failed: %s\n", $mysqli->connect_error );
		exit ();
	}
	
	$username = '"' . $mysqli->real_escape_string ( $json->username ) . '"';
	$password = '"' . $mysqli->real_escape_string ( $json->password ) . '"';
	
	$getUserRow = $mysqli->query ( "select * from user where username=$username and password=$password" );
	
	if (mysqli_num_rows ( $getUserRow ) > 0) {
		
		$row = $getUserRow->fetch_array ( MYSQLI_ASSOC );
		
		$userTable = $row ['username'];
		$groupname = $row['groupname'];
		$tokenTable = uniqid ();
		
		$userTable = '"' . $mysqli->real_escape_string ( $userTable ) . '"';
		$tokenTable = '"' . $mysqli->real_escape_string ( $tokenTable ) . '"';
	
		$insert_token = $mysqli->query ( "insert into token (username,token) values ($userTable,$tokenTable)" );
		
		if ($insert_token) {
			$arr = array (
					'username' => $userTable,
					'groupname' => $groupname,
					'token' => $tokenTable 
			);
			
			$json = json_encode ( $arr );
			http_response_code ( 200 );
			header ( 'Content-type: application/json' );
		} else {
			die ( 'Error : (' . $mysqli->errno . ') ' . $mysqli->error );
		}
	} else {
		http_response_code ( 401 );
	}
	
	$mysqli->close ();
	exit ( $json );
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