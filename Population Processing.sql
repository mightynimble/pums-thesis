CREATE TABLE `person_household_expanded` (
  `id` INT NOT NULL AUTO_INCREMENT ,
  `SERIALNO` int(11) NOT NULL,
  `PUMA` int(11) DEFAULT NULL,
  `ST` int(11) DEFAULT NULL,
  `PWGTP` int(11) DEFAULT NULL,
  `AGEP` int(11) DEFAULT NULL,
  `SCH` int(11) DEFAULT NULL,
  `SEX` int(11) DEFAULT NULL,
  `ESR` int(11) DEFAULT NULL,
  `PINCP` int(11) DEFAULT NULL,
  `NP` int(11) DEFAULT NULL,
  `HHT` int(11) DEFAULT NULL,
  `HINCP` int(11) DEFAULT NULL,
  `HUPAOC` int(11) DEFAULT NULL,
  `HUPARC` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


update thesis.person_household
set HTINC=(case when HINCP is null then (select sumpinc from hh_inc where hh_inc.serialno = person_household.SERIALNO) else HINCP end);

update thesis.person_household
set htinc = -1 where serialno < 10;

select * from person_household where HINCP <= 1 limit 10;

update person_household_expanded set htinc = sumpinc where hincp is null;

update person_household_expanded set htinc = hincp where hincp is not null; 

update person_household_expanded set inc_lvl = 1 where htinc<30000;

update person_household_expanded set inc_lvl = 2 where htinc>=30000 and htinc < 75000;

update person_household_expanded set inc_lvl = 3 where htinc>=75000;

update person_household set emp_status = 3 where sch=2 or sch=3;

update person_household set emp_status = 1 where esr = 1 or esr = 2 or esr = 4 or esr = 5;

update person_household set emp_status = 2 where sch <> 2 and sch <> 3 and esr <> 1 and esr <> 2 and esr <> 4 and esr <> 5;

update person_household set hhtype = 1 where (hht = 1 or hht = 2 or hht = 3) and (hupaoc = 4 or huparc = 4);

update person_household set hhtype = 2 where (hht = 1 or hht = 2 or hht = 3) and (hupaoc = 1 or hupaoc = 2 or hupaoc = 3 or huparc = 1 or huparc = 2 or huparc = 3);

update person_household set hhtype = 3 where hht = 4 or hht = 6;

update person_household set hhtype = 4 where hht = 5 or hht = 7;

update person_household set htinc = pincp where hincp is null;

update person_household set inc_lvl = 1 where htinc < 30000 and inc_lvl is null;

update person_household set inc_lvl = 2 where htinc >= 30000 and htinc < 75000 and inc_lvl is null;

update person_household set inc_lvl = 3 where htinc >= 75000 and inc_lvl is null;

ALTER TABLE `thesis`.`person_lj_household` CHANGE COLUMN `DUMMY` `RAGE` INT(11) NULL DEFAULT NULL  ;

ALTER TABLE `thesis`.`person_household_expanded` CHANGE COLUMN `DUMMY` `RAGE` INT(11) NULL DEFAULT NULL  ;

update person_lj_household set rage = 1 where agep >= 19 and agep<=35;

update person_lj_household set rage = 2 where agep >= 36 and agep<=55;

update person_lj_household set rage = 3 where agep >= 55;

ALTER TABLE `thesis`.`person_household` ADD INDEX `st_indx` (`ST` ASC) ;

ALTER TABLE `thesis`.`person_household_expanded` ADD INDEX `st_indx` (`ST` ASC), ADD INDEX `puma_indx` (`PUMA` ASC) ;

load data infile 'E:\\Yijing_Dissertation\\yijinglu-thesis\\resources\\outputs\\expanded_output.csv' into table person_household_expanded
fields terminated by ',' lines terminated by '\n' 
(`SERIALNO`,`PWGTP`,`AGEP`,`PINCP`,`SCH`,`SEX`,`ESR`,`PUMA`,`ST`,`NP`,`HHT`,`HINCP`,`HUPAOC`,`HUPARC`,`SUMPINC`,`HTINC`,`RAGE`,`INC_LVL`,`EMP_STATUS`,`HHTYPE`,`MSAPMSA`,`R_BUSINESS`,`R_PERSON`,`R_PB`);

CREATE  TABLE `thesis`.`msa` (
  `id` INT NOT NULL ,
  `msa` INT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) ,
  INDEX `msa_indx` (`msa` ASC) );
  
  CREATE TABLE `thesis`.`id_puma` (
  `id` INT NOT NULL,
  `puma` INT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `puma_indx` (`puma` ASC));
  
  CREATE TABLE `thesis`.`id_rands` (
  `ID` INT NOT NULL,
  `R_BUSINESS` DOUBLE NULL,
  `R_PERSON` DOUBLE NULL,
  `R_PB` DOUBLE NULL,
  `ID_RANDScol` VARCHAR(45) NULL,
  PRIMARY KEY (`ID`),
  UNIQUE INDEX `ID_UNIQUE` (`ID` ASC));
  
CREATE TABLE state_pop 
select person_household_expanded.ST, count(*) as totpop
from person_househostate_popstate_popld_expanded
group by ST;
  
CREATE TABLE Non_Adult
select person_household_expanded.* 
from person_household_expanded
where person_household_expanded.AGEP<18;

Delete from person_household_expanded
where person_household_expanded.AGEP<18;

select person_household_expanded.MSAPMSA
from person_household_expanded
group by person_household_expanded.MSAPMSA;

select puma from person_household_expanded where st = 22 group by puma;

select st from person_household_expanded where puma = 77777 group by st;

select * from person_household_expanded where INC_LVL=0;

select * from person_household_expanded where id = 12752;id_expand1CREATE TABLE `id_expand1` (
  `id` int(11) NOT NULL,
  `Employment` int(11) DEFAULT NULL,
  `Unemployment` int(11) DEFAULT NULL,
  `Student` int(11) DEFAULT NULL,
  `Male` int(11) DEFAULT NULL,
  `hhchd` int(11) DEFAULT NULL,
  `hhochd` int(11) DEFAULT NULL,
  `single` int(11) DEFAULT NULL,
  `non_fam` int(11) DEFAULT NULL,
  `LowIncome` int(11) DEFAULT NULL,
  `MedIncome` int(11) DEFAULT NULL,
  `HighIncome` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `emp_indx` (`Employment`),
  KEY `unemp_indx` (`Unemployment`),
  KEY `male_indx` (`Male`),
  KEY `student_indx` (`Student`),
  KEY `hhchd_indx` (`hhchd`),
  KEY `hhochd_indx` (`hhochd`),
  KEY `single_indx` (`single`),
  KEY `non_fam_indx` (`non_fam`),
  KEY `low_indx` (`LowIncome`),
  KEY `med_indx` (`MedIncome`),
  KEY `high_indx` (`HighIncome`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


select 