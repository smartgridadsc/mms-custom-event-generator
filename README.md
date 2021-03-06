# Overview

This repository contains files related to the MMS plugin found here: <https://github.com/smartgridadsc/MMS-protocol-parser-for-Zeek-IDS/>. It contains programs to auto-generate custom-events.pac file to prioritize which packets to send to the Zeek script for processing. Details of each file are described below.

1. sample.icd - This is an IED capability description file. It defines data objects that are used by an IED during communication. The data objects are organized hierarchically in this format: logical devices (LD), logical nodes (LN), data objects (DO), data attributes (DA).

2. mms_configuration_partial.csv - This is a csv file specified by the user. It contains variable names that the user wants to extract the MMS object references. The variable names can be found embedded as private blocks in the \<DA\> tags of the sample.icd file.
  
3. SCLParser.java - This is a Java program that takes the sample.icd and mms_configuration_partial.csv as inputs and outputs a user_configuration in csv format. Based on a variable name specified in the mms_configuration_partial.csv file, the program will parse the icd file to reconstruct the MMS object reference (i.e., LD/LN$FC$DO$DA). In addition to the MMS object reference, the program also stores the index position of the MMS object reference and its data type. 
  
4. EventGenerator.java - This is a Java program. It takes as input the user_configuration.csv generated by the SCLParser program and outputs two files namely, custom-events.pac and event_test.bif. These two files should be used in companion with the MMS plugin found at <https://github.com/smartgridadsc/MMS-protocol-parser-for-Zeek-IDS/>. The custom-events.pac file contains filtering logic to customize which MMS packets or events to send to the Zeek script. The event_test.bif contains the event declarations.
  
# Usage
  1. Run __*make*__ in the directory where you download the mms-custom-event-generator package. This command compiles the .java programs into .class executables. 
  2. Run __*java SCLParser*__ in terminal to output the user_configuration.csv file.
  3. Run __*java EventGenerator*__ in terminal to generate the custom-events.pac and event_test.bif files.
  
# Steps to integrate with MMS plugin
  1. Clone the MMS plugin from <https://github.com/smartgridadsc/MMS-protocol-parser-for-Zeek-IDS/>.
	
	git clone https://github.com/smartgridadsc/MMS-protocol-parser-for-Zeek-IDS
  2. Copy custom-events.pac and event_test.bif into the src directory of the MMS plugin.
  3. Update the mms.pac to include header files (highlighted in green) 
  
```diff
  %extern{
  	#include "events.bif.h"
+       #include "event_test.bif.h"
  %}
  ...
  %include mms-analyzer.pac
+ %include custom-events.pac
``` 
  
  4. Update the CMakeLists.txt to include the custom-events.pac and event_test.bif files (highlighted in green). Example below:

```diff
    ...
    bro_plugin_bif(src/events.bif 
+   src/event_test.bif)
    bro_plugin_pac(
    src/mms.pac 
    src/mms-analyzer.pac 
    src/mms-protocol.pac 
    src/mms-asn1.pac
+   src/custom-events.pac                              
    )
    ...
```
  5. In the mms-analyzer.pac file, uncomment the rule_function().
  6. Install the plugin following these steps. Some commands may require root privileges.
  
    cd MMS-protocol-parser-for-Zeek-IDS/
    ./configure --bro-dist=/path_to_zeek
    make
    make install

# Sample Zeek scripts
The sample_scripts folder contains two \*.bro scripts, i.e., generic_script.bro and custom_script.bro. These scripts implement the same detection logic to detect attacks on the synchronization process in EPIC [1] but in different ways. generic_script.bro uses generic events, i.e., MMS read response and MMS write request events. Therefore, additional filtering logic needs to be implemented to extract the correct MMS object references (or messages) for detection. custom_script.bro uses custom event handlers where the corresponding MMS object references are filtered in the event engine. Thus, no filtering is needed in custom-script.bro script. Before testing the custom-script.bro, make sure to copy the custom-events.pac and event_test.bif files to the MMS plugin src folder and follow steps 2 to 6 listed in the "Steps to integrate with MMS plugin" section to compile the plugin. It may be necessary to ./configure, make, make install to rebuild Zeek. 

The testing scenario as mentioned is the synchronization of generators process in EPIC. Sychronization of generators is the process of connecting two or more generators to the grid to meet load demands. The frequency of the incoming generator must match the running grid before it can be connected to supply power. In EPIC, this process is handled by different devices using MMS protocol and Modbus protocol. Details of the synchronization process with reference to the EPIC testbed are described as follows:

1. Assume G2 (the reference generator) is already connected to the grid and we are connecting G1 (incoming generator) in parallel. 
2. To synchronize G1, SCADA sends a SCADA_Q2C_Sync_Activated (MMS) command to the PLC. 
3. Upon receiving the sync command from SCADA, the PLC will send a Modbus message to command motor 1 (connected to G1) to rotate at a higher speed, usually a few rpms higher than the normal speed. 
4. The changes in motor speed will cause the phase angle of the incoming generator (G1) to decrease at a normal rate of 4-5 degrees to catch up with the reference generator (G2).
5. This phase angle information (MIED2_Phase_Angle) is monitored by MIED2 using MMS protocol.
6. When the phase angle difference between the two generators reaches approximately zero, the MIED2 will close the circuit breaker and send a Q2C_In_Sync MMS messsge to inform the SCADA that synchronization is complete.

The key MMS messages in the synchronization process are SCADA_Q2C_Sync_Activated, MIED2_Phase_Angle, and Q2C_In_Sync. The corresponding MMS object references are given as follows: 

* SCADA_Q2C_Sync_Activated -> WAGO61850ServerLogicalDevice_GGIO17$CO$SPCSO2$Oper
* MIED2_Phase_Angle -> MIED2PROT_LLN0$Measurement
* Q2C_In_Sync -> MIED2CTRL_V16GGIO1$ST$Ind3$stVal

We consider an attack scenario where the PLC is compromised. In other words, motor 1 of the incoming generator is rotating at the same speed as the reference generator. Thus, the phase angle will not decrease at the normal rate of 4-5 degrees, and MIED2 will not close the circuit breaker to connect the incoming generator (G1). This attack is captured in the sync_attack.pcapng. Based on this attack scenario, we design the detection logic in both scripts to check if the phase angle change of G1 is outside the predefined range of 4-5 degrees. If so, the script will raise an alarm. In both scripts, when and which MMS message to check is based on matching the MMS object references. The main difference between generic_script.bro and custom_script.bro lies in where the filtering of the MMS object references is performed, i.e., in the script itself or in the event engine, respectively.  

To run and test the scripts,

    cd <path_to_zeek>
    ./build/src/bro -r <path_to_pcap>/sync_attack.pcapng <path_to_plugin>/scripts/<script name e.g. generic_script.bro or custom_script.bro>

# Future Readings
  [1] H.C. Tan, V. Mohanraj, B. Chen, S. Nan, A. Yang, ???An IEC 61850 MMS Traffic Parser for Customizable and Efficient Intrusion Detection???
  
## References
[1] [https://itrust.sutd.edu.sg/testbeds/electric-power-intelligent-control-epic/](https://itrust.sutd.edu.sg/testbeds/electric-power-intelligent-control-epic/)

## Contact Information
If you would like to get in touch regarding our project, please contact any of our contributors via email:
1. Chen Binbin
Email: [binbin.chen@adsc-create.edu.sg](mailto:binbin.chen@adsc-create.edu.sg)
2. Tan Heng Chuan
Email: [hc.tan@adsc-create.edu.sg](mailto:hc.tan@adsc-create.edu.sg)

For more information on our organization???s research activities, please refer to our main website: [https://adsc.illinois.edu](https://adsc.illinois.edu)
  
  
