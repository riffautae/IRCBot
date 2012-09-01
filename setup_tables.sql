/*
 * This file is part of IRCBot.
 * Copyright (c) 2011 Ryan Morrison
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions, and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions, and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of the author of this software nor the name of
 *  contributors to this software may be used to endorse or promote products
 *  derived from this software without specific prior written consent.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */

/* Execute this SQL file against a database to prepare tables for use with IRCBot. */

DROP TABLE IF EXISTS Quotes;
DROP TABLE IF EXISTS Seen;
DROP TABLE IF EXISTS Intros;
DROP TABLE IF EXISTS GameStatus;

CREATE TABLE IF NOT EXISTS Quotes(
	ID		INT PRIMARY KEY AUTO_INCREMENT,
	Nick	VARCHAR(255) NOT NULL,
	Date	DATETIME NOT NULL,
	Channel	VARCHAR(32) NOT NULL,
	Quote	VARCHAR(255) NOT NULL,
	
	INDEX who (Nick, Channel)
);

CREATE TABLE IF NOT EXISTS Seen(
	ID		INT PRIMARY KEY AUTO_INCREMENT,
	Nick	VARCHAR(255) NOT NULL,
	Date	DATETIME NOT NULL,
	Channel	VARCHAR(32) NOT NULL,
	
	INDEX who (Nick, Channel)
);
CREATE TABLE IF NOT EXISTS Intros(
	ID			INT PRIMARY KEY AUTO_INCREMENT,
	Submitter	VARCHAR(255) NOT NULL,
	Date		DATETIME NOT NULL,
	Nick		VARCHAR(255) NOT NULL,
	Channel		VARCHAR(32) NOT NULL,
	Intro		VARCHAR(255) NOT NULL,
	
	INDEX (Submitter, Channel)
	INDEX (Nick, Channel)
);
CREATE TABLE IF NOT EXISTS GameStatus(
	ID 		INT PRIMARY KEY AUTO_INCREMENT,
	Nick	VARCHAR(255) NOT NULL,
	Date	DATETIME NOT NULL,
	Game	VARCHAR(255) NOT NULL
);
