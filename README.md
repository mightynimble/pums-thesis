# National Travel Demand Simulation

## Pre-requirements
 - Netbeans with JAVA SDK
 - MySQL (consider move MySQL table data onto a different drive other than C if the free space on the system drive is less than 50GB.)
 - (Optional) MySQL Workbench

## Build
 - Open Netbeans and open ```yijinglu-thesis``` in Netbeans
 - Right click ```Thesis``` in ```Project``` panel (Ctrl + 1 to open it)
 - Click ```Clean and Build```
 - Wait for ```BUILD SUCCESSFUL (total time x seconds)``` in the output panel (it will take a few seconds)

## Configure
 There is a ```config.properties``` file in ```.\resources``` directory. Different items are required for different executables.
Note that the directories in paths in Windows should be separated with double '\'. For example: D:\\Data\\yijinglu-thesis\\resources\\inputs\\foo.bar


## Run
 - Assuming all csv data files have been imported into MySQL
 - Open ```Windows PowerShell```
 - Navigate to ```yijinglu-thesis``` directory
 - Run java file: ```pums2010ExpandByPWGTPIntoCsv.java``` in PowerShell to expand ```person_lj_household``` table.
 ```sh
  javaw -Xmx13500m -classpath ".\dist\Thesis.jar" umd.lu.thesis.pums2010.pums2010ExpandByPWGTPIntoCsv
 ```
 - Then execute this SQL in MySQL Workbench:
 ```sql
 ALTER TABLE `thesis`.`person_household_expanded` 
ADD INDEX `st_indx` (`ST` ASC) 
, ADD INDEX `puma_indx` (`PUMA` ASC) ;
```
 - Import expanded_output.csv into MySQL person_household_expanded
```sql
load data infile 'G:\\2010PUMS\\expanded_output.csv' into table person_household_expanded
fields terminated by ',' lines terminated by '\n' 
(`SERIALNO`,`PWGTP`,`AGEP`,`PINCP`,`SCH`,`SEX`,`ESR`,`PUMA`,`ST`,`NP`,`HHT`,`HINCP`,`HUPAOC`,`HUPARC`,`SUMPINC`,`HTINC`,`RAGE`,`INC_LVL`,`EMP_STATUS`,`HHTYPE`,`MSAPMSA`,`R_BUSINESS`,`R_PERSON`,`R_PB`);
```
 - Create a table ```MSA``` so that MSA and MSAPMSA can be joined by ID:
```sql
CREATE  TABLE `thesis`.`msa` (
  `id` INT NOT NULL ,
  `msa` INT NULL ,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) ,
  INDEX `msa_indx` (`msa` ASC) );```
 - Create table ID_PUMA with index on PUMA
 ```sql
CREATE TABLE `thesis`.`id_puma` (
  `id` INT NOT NULL,
  `puma` INT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `puma_indx` (`puma` ASC));
  ```
 - Run java file: ```ProcessExpandedTableMSAPMSA.java``` in PowerShell to set ```MSAPMSA``` column. (the operation will take a day or two.)
 ```sh
  javaw -Xmx13500m -classpath ".\dist\Thesis.jar" umd.lu.thesis.pums2010.ProcessExpandedTableMSAPMSA
 ```
 - Create new table then batch insert using JAVA
 ```sql
CREATE TABLE `thesis`.`id_rands` (
  `ID` INT NOT NULL,
  `R_BUSINESS` DOUBLE NULL,
  `R_PERSON` DOUBLE NULL,
  `R_PB` DOUBLE NULL,
  `ID_RANDScol` VARCHAR(45) NULL,
  PRIMARY KEY (`ID`),
  UNIQUE INDEX `ID_UNIQUE` (`ID` ASC));
```
 - Run java file: ```ProcessExpandedTableRandValues.java``` in PowerShell to set random values columns. (the operation will take a day or two.)
 ```sh
  javaw -Xmx13500m -classpath ".\dist\Thesis.jar" umd.lu.thesis.pums2010.ProcessExpandedTableRandValues
 ```
 - Run java file: ```NationalTravelDemandExec.java``` in PowerShell:
  -- Get the max ID from MySQL: (The max is 309,349,689 for 2010 data)
  ```sql
  select id from person_household_expanded order by id desc limit 1;
  ```
  -- Now divide the total into bulks, say the bulk size is 10,000,000. (And it is better to keep an record of the start and end IDs. So you know what are running, what are done and what should be run next.)
  -- Run ```NationalTravelDemandExec.java``` with arguments:
  ```sh
  javaw -Xmx13500m -classpath ".\dist\Thesis.jar" umd.lu.thesis.pums2010.NationalTravelDemandExec 1 10000000
  ```
  The above runs records from ID=1 to ID=9,999,999
  So the next bulk should be: 10,000,000 to 20,000,000.
  -- Run *multiple* ```NationalTravelDemandExec``` instances *concurrently* to speed up the process.
  -- For example, if the total records (max ID) is 30,000,000 and you want to divide them into 10 batches, you can run the following 10 instances in PowerShell concurrently:
  ```sh
javaw -Xmx13500m -classpath ".\dist\Thesis.jar" umd.lu.thesis.pums2010.NationalTravelDemandExec 1 3000000
javaw -Xmx13500m -classpath ".\dist\Thesis.jar" umd.lu.thesis.pums2010.NationalTravelDemandExec 3000000 6000000
javaw -Xmx13500m -classpath ".\dist\Thesis.jar" umd.lu.thesis.pums2010.NationalTravelDemandExec 5999999 9000000
javaw -Xmx13500m -classpath ".\dist\Thesis.jar" umd.lu.thesis.pums2010.NationalTravelDemandExec 8999998 12000000
javaw -Xmx13500m -classpath ".\dist\Thesis.jar" umd.lu.thesis.pums2010.NationalTravelDemandExec 11999997 15000000
javaw -Xmx13500m -classpath ".\dist\Thesis.jar" umd.lu.thesis.pums2010.NationalTravelDemandExec 14999996 18000000
javaw -Xmx13500m -classpath ".\dist\Thesis.jar" umd.lu.thesis.pums2010.NationalTravelDemandExec 17999995 21000000
javaw -Xmx13500m -classpath ".\dist\Thesis.jar" umd.lu.thesis.pums2010.NationalTravelDemandExec 20999994 24000000
javaw -Xmx13500m -classpath ".\dist\Thesis.jar" umd.lu.thesis.pums2010.NationalTravelDemandExec 23999993 27000000
javaw -Xmx13500m -classpath ".\dist\Thesis.jar" umd.lu.thesis.pums2010.NationalTravelDemandExec 26999992 30000000
  ```
  "javaw" runs java programs in the background so that you don't need to open multiple PowerShell windows.

 -- To check the status of the running programs, open ```master.log``` in a text editor (e.g. sublime text).
 -- The whole process will take at least 3 to 4 days given there are 15 to 20 instances running non-stopping on the server. Use the log file to determine the running speed of each instance and *plan ahead* to fire off new instances when the running ones are complete.
 -- In the end, run ```OutputMatricesProcessing.java``` to merge output files.
 ```sh
 javaw -Xmx13500m -classpath ".\dist\Thesis.jar" umd.lu.thesis.pums2010.OutputMatricesProcessing
 ```


