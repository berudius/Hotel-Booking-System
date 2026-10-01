CREATE DATABASE IF NOT EXISTS persistent_login_db;
CREATE DATABASE IF NOT EXISTS java_hotel_user_service_db;
CREATE DATABASE IF NOT EXISTS java_hotel_booking_service_db;
CREATE DATABASE IF NOT EXISTS java_hotel_booking_resource_service_db;
			
CREATE TABLE IF NOT EXISTS persistent_login_db.persistent_logins (
    username VARCHAR(64) NOT NULL,
    series VARCHAR(64) PRIMARY KEY,
    token VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL
);


-- insert into java_hotel_user_service_db.user (account_state,hash_password,login,phone_number,registered,role) values (0,"$2a$10$2gU2.77qIJDMdl8BRTAaEuh2NE9XuWDDOxdH2pWVoddR.wfzpg1H2","admin","380972157788","2026-05-16 18:12:59.641804","ADMIN")
	