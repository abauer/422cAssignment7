<?php
require "connect.php";
if ($m->connect_error) {
	die($m->connect_error);
}

function getUsername($id) {
	global $m;
	$s = $m->query("SELECT * FROM `proj7_users` WHERE `userId`='$id'") or die($m->error);
	if ($s->num_rows == 0) {
		$m->close();
		exit("-1");
	}
	$f = $s->fetch_array(MYSQLI_ASSOC);
	return $f['Username'];
}

function getId($name) {
	global $m;
	$s = $m->query("SELECT * FROM `proj7_users` WHERE `Username`='$name'") or die($m->error);
	if ($s->num_rows == 0) {
		$m->close();
		exit("-1");
	}
	$f = $s->fetch_array(MYSQLI_ASSOC);
	return $f['userId'];
}

function getGroupId($id1, $id2) {
	global $m;
	$lowerid = min($id1, $id2);
	$higherid = max($id1, $id2);
	$s = $m->query("SELECT * FROM `proj7_friends` WHERE `LowerId`='$lowerid' AND `HigherId`='$higherid'") or die($m->error);
	if ($s->num_rows == 0) {
		$m->close();
		exit("-1");
	}
	$f = $s->fetch_array(MYSQLI_ASSOC);
	return $f['groupId'];
}

if (count($_GET) > 0) {
	foreach($_GET as $k=>$v) {
		if (!isset($$k)) {
			$$k = $m->escape_string($v);
		}
	}
	switch($_GET['type']) {
		case "login":
			$s = $m->query("SELECT * FROM `proj7_users` WHERE `Username`='$username' AND `Password`='$password'") or die($m->error);
			if ($s->num_rows == 0) {
				echo "-1";
				break;
			}
			$f = $s->fetch_array(MYSQLI_ASSOC);
			echo $f['userId'];
			break;
		case "register":
			$s = $m->query("SELECT * FROM `proj7_users` WHERE `Username`='$username'") or die($m->error);
			if ($s->num_rows > 0) {
				echo "-1";
				break;
			}
			$m->query("INSERT INTO `proj7_users` (`Username`, `Password`) VALUES ('$username', '$password')") or die($m->error);
			echo $m->insert_id;
			break;
		case "update_password":
			$m->query("UPDATE `proj7_users` SET `Password`='$newpassword' WHERE `userId`='$clientid' AND `Password`='$oldpassword'") or die($m->error);
			if ($m->affected_rows == 0) {
				echo "-1";
				break;
			}
			echo "1";
			break;
		case "send_group_message":
			//TODO: check if in group
			$myname = getUsername($clientid);
			$message = "[" . strtoupper($myname) . "]: " . $message;
			$m->query("INSERT INTO `proj7_messages` (`groupId`,`userId`,`Message`) VALUES ('$groupid','$clientid','$message')") or die($m->error);
			if ($m->affected_rows == 0) {
				echo "-1";
				break;
			}
			echo "1";
			break;
		case "send_message":
			//TODO: can't friend self
			$myname = getUsername($clientid);
			$message = "[" . strtoupper($myname) . "]: " . $message;
			//get friendship, if exists
			$friendid = getId($recipient);
			$groupid = getGroupId($friendid, $clientid);
			$m->query("INSERT INTO `proj7_messages` (`groupId`,`userId`,`Message`) VALUES ('$groupid','$clientid','$message')") or die($m->error);
			if ($m->affected_rows == 0) {
				echo "-1";
				break;
			}
			echo "1";
			break;
		case "get_friends":
			$s = $m->query("SELECT * FROM `proj7_friends` WHERE `LowerId`='$clientid' OR `HigherId`='$clientid' WHERE `Confirmed`=1") or die($m->error);
			while ($f = $s->fetch_array(MYSQLI_ASSOC)) {
				echo (($f['LowerId'] == $clientid) ? $f['HigherId'] : $f['LowerId']) . "\n";
			}
			break;
		case "make_chat":
			$members = explode(",", $groupmembers);
			$m->query("INSERT INTO `proj7_groups` (`Name`) VALUES ('$groupname')") or die($m->error);
			$groupid = $m->insert_id;
			// TODO check if >1 member
			$members[] = $clientid; // add self too
			foreach ($members as $member) {
				$memberid = getId($member);
				$m->query("INSERT INTO `proj7_membership` (`userId`,`groupId`) VALUES ('$memberid','$groupid')") or die($m->error);
			}
			echo $groupid;
			break;
		case "add_friend":
			$friendid = getId($username);
			$lowerid = min($friendid, $clientid);
			$higherid = max($friendid, $clientid);
			$s = $m->query("SELECT * FROM `proj7_friends` WHERE `LowerId`='$lowerid' AND `HigherId`='$higherid' AND `Confirmed`=0 AND `Requester`='$friendid'") or die($m->error);
			// check to see if there's already a pending request from the other side
			if ($s->num_rows == 1) {
				$f = $s->fetch_array(MYSQLI_ASSOC);
				$friendshipid = $f['friendshipId'];
				$groupid = $f['groupId'];
				// accept request
				$m->query("UPDATE `proj7_friends` SET `Confirmed`=1 WHERE `friendshipId`='$friendshipid'") or die($m->error);
				// send message to it
				$message = strtoupper($clientid) . " accepted " . strtoupper($friendid) . "'s friend request.";
				$m->query("INSERT INTO `proj7_messages` (`groupId`,`userId`,`Message`) VALUES ('$groupid','$clientid','$message')") or die($m->error);
				echo "1";
				break;
			}
			// check for any other existing friendship, i.e. a confirmed friendship or a pending request from this side
			$s = $m->query("SELECT * FROM `proj7_friends` WHERE `LowerId`='$lowerid' AND `HigherId`='$higherid'") or die($m->error);
			if ($s->num_rows != 0) {
				echo "-1";
				break;
			}
			// create new group
			$m->query("INSERT INTO `proj7_groups` (`OneOnOne`) VALUES ('1')") or die($m->error);
			$groupid = $m->insert_id;
			// add friends to it
			$m->query("INSERT INTO `proj7_membership` (`userId`,`groupId`) VALUES ('$clientid','$groupid')") or die($m->error);
			$m->query("INSERT INTO `proj7_membership` (`userId`,`groupId`) VALUES ('$friendid','$groupid')") or die($m->error);
			// send message to it
			$message = strtoupper($clientid) . " sent " . strtoupper($friendid) . " a friend request.";
			$m->query("INSERT INTO `proj7_messages` (`groupId`,`userId`,`Message`) VALUES ('$groupid','$clientid','$message')") or die($m->error);
			// create request
			$m->query("INSERT INTO `proj7_friends` (`LowerId`,`HigherId`,`Requester`,`groupId`) VALUES ('$lowerid','$higherid','$clientid','$groupid')") or die($m->error);
			echo "1";
			break;
		case "remove_friend":
			$friendid = getId($username);
			$groupid = getGroupId($clientid, $friendid);
			// delete group
			$m->query("DELETE FROM `proj7_groups` WHERE `groupId`='$groupid'") or die($m->error);
			$m->query("DELETE FROM `proj7_membership` WHERE `groupId`='$groupid'") or die($m->error);
			$m->query("DELETE FROM `proj7_messages` WHERE `groupId`='$groupid'") or die($m->error);
			// delete friendship
			$m->query("DELETE FROM `proj7_friends` WHERE `groupId`='$groupid'") or die($m->error); // uniquely identifies anyway
			echo "1";
			break;
		case "get_group_message_history":
			// TODO check membership
			$s = $m->query("SELECT * FROM `proj7_messages` WHERE `groupId`='$groupid'") or die($m->error);
			while ($f = $s->fetch_array(MYSQLI_ASSOC)) {
				echo $f['Message'] . "\n";
			}
			break;
		case "get_message_history":
			$friendid = getId($username);
			$groupid = getGroupId($clientid, $friendid);
			$s = $m->query("SELECT * FROM `proj7_messages` WHERE `groupId`='$groupid'") or die($m->error);
			while ($f = $s->fetch_array(MYSQLI_ASSOC)) {
				echo $f['Message'] . "\n";
			}
			break;
		case "leave_group":
			$m->query("DELETE FROM `proj7_membership` WHERE `userId`='$clientid' AND `groupId`='$groupid'");
			if ($m->affected_rows != 1) {
				echo "-1";
				break;
			}
			echo"1";
			break;
		case "get_groups":
			$s = $m->query("SELECT * FROM `proj7_membership` l INNER JOIN `proj7_groups` r ON l.groupId=r.groupId WHERE l.userId='$clientid'") or die($m->error);
			while ($f = $s->fetch_array(MYSQLI_ASSOC)) {
				echo $f['groupId'] . "\n";
				echo $f['Name'] . "\n";
			}
			break;
		default:
			echo "-1";
	}
} else {
	echo "-1";
}

$m->close();
?>