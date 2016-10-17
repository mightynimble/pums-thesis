update person_household_expanded set inc_lvl = 2 where htinc>=30000 and htinc < 75000;

update person_household_expanded set inc_lvl = 3 where htinc>=75000;

LOAD DATA LOCAL INFILE 'E:/Yijing_Dissertation/2040/person_synthetic_data_06.csv' 
INTO TABLE futureyear.syn_person_ca
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"' 
LINES TERMINATED BY '\n';

CREATE TABLE `sample_per_hh` (
  `SERIALNO` int(11) NOT NULL,
  `SPORDER` int(11) NOT NULL,
  `PUMA` int(11) DEFAULT NULL,
  `ST` int(11) DEFAULT NULL,
  `AGEP` int(11) DEFAULT NULL,
  `SCH` int(11) DEFAULT NULL,
  `SEX` int(11) DEFAULT NULL,
  `ESR` int(11) DEFAULT NULL,
  `PINCP` int(11) DEFAULT NULL,
  `NP` int(11) DEFAULT NULL,
  `HHT` int(11) DEFAULT NULL,
  `HINCP` int(11) DEFAULT NULL,
  `HUPAOC` int(11) DEFAULT NULL,
  `HUPARC` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

LOAD DATA LOCAL INFILE 'E:/Yijing_Dissertation/2040/2010_per_hh_sample.csv' 
INTO TABLE futureyear.sample_per_hh
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

select * from sample_per_hh
where pincp <0 limit 100;
TRUNCATE table futureyear.sample_per_hh;
TRUNCATE table futureyear.syn_per_lj_hh;

Create table syn_per_lj_hh_ca
select syn_person_ca.hhserialno, syn_person_ca.pnum, syn_person_ca.pweight, syn_person_ca.state, sample_per_hh.*
from syn_person_ca left join sample_per_hh
on syn_person_ca.hhserialno=sample_per_hh.serialno AND syn_person_ca.pnum=sample_per_hh.sporder;


TRUNCATE table futureyear.hh_inc;
create table hh_inc
select sample_per_hh.SERIALNO, sum(sample_per_hh.pincp) as sumpinc
from sample_per_hh
group by sample_per_hh.SERIALNO;

TRUNCATE table futureyear.person_household_2040;

create table person_household_2040_ca
select syn_per_lj_hh_ca.*, hh_inc.sumpinc
from syn_per_lj_hh_ca left join hh_inc 
on syn_per_lj_hh_ca.SERIALNO=hh_inc.SERIALNO;

select count(*) from person_household_2040;
select count(*) from syn_per_lj_hh;
select count(*) from syn_person;

ALTER TABLE `futureyear`.`person_household_2040_ca`
ADD COLUMN `HTINC` INT NULL AFTER `SUMPINC`,
ADD COLUMN `RAGE` INT NULL  AFTER `HTINC`,
ADD COLUMN `INC_LVL` INT NULL  AFTER `RAGE`,
ADD COLUMN `EMP_STATUS` INT NULL  AFTER `INC_LVL` ,
ADD COLUMN `HHTYPE` INT NULL  AFTER `EMP_STATUS`,
ADD COLUMN `MSAPMSA` INT NULL  AFTER `HHTYPE`; 

update `futureyear`.`person_household_2040_ca`
set HTINC=(case when HINCP is null then sumpinc else HINCP end);


update `futureyear`.`person_household_2040_ca` set inc_lvl = case
when htinc<30000 then 1
when htinc>=30000 and htinc < 75000 then 2
when htinc>=75000 then 3
end;


update `futureyear`.`person_household_2040_ca`  set emp_status = case
when sch=2 or sch=3 then 3
when esr = 1 or esr = 2 or esr = 4 or esr = 5 then 1
when sch <> 2 and sch <> 3 and esr <> 1 and esr <> 2 and esr <> 4 and esr <> 5 then 2
end;

update `futureyear`.`person_household_2040_ca` set hhtype = case 
when (hht = 1 or hht = 2 or hht = 3) and (hupaoc = 4 or huparc = 4) then 1
when (hht = 1 or hht = 2 or hht = 3) and (hupaoc = 1 or hupaoc = 2 or hupaoc = 3 or huparc = 1 or huparc = 2 or huparc = 3) then 2
when hht = 4 or hht = 6 or hht =0 then 3
when hht = 5 or hht = 7 then 4
end;

ALTER TABLE  `futureyear`.`person_household_2040_ca` ADD  `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

 
CREATE TABLE `futureyear`.`person_household_expanded2040_ca` (
  `SERIALNO` int(11) NOT NULL,
  `PWGTP` int(11) DEFAULT NULL,
  `AGEP` int(11) DEFAULT NULL,
  `PINCP` int(11) DEFAULT NULL,
  `SCH` int(11) DEFAULT NULL,
  `SEX` int(11) DEFAULT NULL,
  `ESR` int(11) DEFAULT NULL,
  `PUMA` int(11) DEFAULT NULL,
  `ST` int(11) DEFAULT NULL,
  `NP` int(11) DEFAULT NULL,
  `HHT` int(11) DEFAULT NULL,
  `HINCP` int(11) DEFAULT NULL,
  `HUPAOC` int(11) DEFAULT NULL,
  `HUPARC` int(11) DEFAULT NULL,
  `SUMPINC` int(11) DEFAULT NULL,
  `HTINC` int(11) DEFAULT NULL,
  `RAGE` int(11) DEFAULT NULL,
  `INC_LVL` int(11) DEFAULT NULL,
  `EMP_STATUS` int(11) DEFAULT NULL,
  `HHTYPE` int(11) DEFAULT NULL,
  `MSAPMSA` int(11) DEFAULT NULL,
  `R_BUSINESS` int(11) DEFAULT NULL,
  `R_PERSON` int(11) DEFAULT NULL,
  `R_PB` int(11) DEFAULT NULL
) ENGINE=InnoDB AUTO_INCREMENT=309349690 DEFAULT CHARSET=utf8;

LOAD DATA LOCAL INFILE 'E:/Yijing_Dissertation/2040/population_2040_ca.csv' 
INTO TABLE futureyear.person_household_expanded2040_ca
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"' 
LINES TERMINATED BY '\n';

ALTER TABLE  `futureyear`.`person_household_expanded2040` ADD  `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

select id from person_household_expanded2040 order by id desc limit 1;

CREATE TABLE state_pop_2040 
select person_household_expanded2040.ST, count(*) as totpop
from person_household_expanded2040
group by ST;