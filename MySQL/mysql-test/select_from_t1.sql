
create table Course
(
    c_id varchar(20),
    c_name varchar(20) not null default '',
    t_id varchar(20) not null,
    primary key(c_id)
)engine=innodb,charset=utf8;
show create table Course;

CREATE TABLE `Teacher`(
	`t_id` VARCHAR(20),
	`t_name` VARCHAR(20) NOT NULL DEFAULT '',
	PRIMARY KEY(`t_id`)
)engine=innodb,charset=utf8;
show create table Teacher;

CREATE TABLE `Score`(
	`s_id` VARCHAR(20),
	`c_id`  VARCHAR(20),
	`s_score` INT(3),
	PRIMARY KEY(`s_id`,`c_id`)
)engine=innodb,charset=utf8;
show create table Score;

insert into Student values('01' , '赵雷' , '1990-01-01' , '男');
insert into Student values('02' , '钱电' , '1990-12-21' , '男');
insert into Student values('03' , '孙风' , '1990-05-20' , '男');
insert into Student values('04' , '李云' , '1990-08-06' , '男');
insert into Student values('05' , '周梅' , '1991-12-01' , '女');
insert into Student values('06' , '吴兰' , '1992-03-01' , '女');
insert into Student values('07' , '郑竹' , '1989-07-01' , '女');
insert into Student values('08' , '王菊' , '1990-01-20' , '女');

drop table student;

create table Student
(
    s_id varchar(20),
    s_name varchar(20) not null default '',
    s_birth varchar(20) not null default '',
    s_sex varchar(20) not null default '',
    primary key(s_id)
)engine=innodb,charset=utf8;
show create table Student;

insert into Student values('01' , '赵雷' , '1990-01-01' , '男');
insert into Student values('02' , '钱电' , '1990-12-21' , '男');
insert into Student values('03' , '孙风' , '1990-05-20' , '男');
insert into Student values('04' , '李云' , '1990-08-06' , '男');
insert into Student values('05' , '周梅' , '1991-12-01' , '女');
insert into Student values('06' , '吴兰' , '1992-03-01' , '女');
insert into Student values('07' , '郑竹' , '1989-07-01' , '女');
insert into Student values('08' , '王菊' , '1990-01-20' , '女');
select * from Student;
commit;
show tables;
select * from student;
select * from Course;
select * from Score;


select st.*,sc.* from Student st , Score sc on sc.s_id = sc.s_id where select s_id from  q1111   `