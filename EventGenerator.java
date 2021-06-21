import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

class EventGenerator {

    private static String buildBifDataString(String dataType){
        String returnString = "";
        switch (dataType) {
            case "BOOLEAN":
                returnString = "                                      " + "value";
                break;

            case "FLOAT32":
                returnString = "                                      " + "value";
                break;

            case "Dbpos":
                returnString = "                                      string_to_val(value)";
                break;

            // Below datatypes not tested
            case "INT8U":
                returnString = "                                      " + "value";
                break;

            case "INT32U":
                returnString = "                                      " + "value";
                break;
        
            case "Timestamp":
                returnString = "                                      string_to_val(value)";
                break;
        
            case "VISSTRING255":
                returnString = "                                      string_to_val(value)";
                break;
        
            case "Quality":
                returnString = "                                      string_to_val(value)";
                break;
        
            default:
                returnString = "                                      -1";
                break;
        }
        return returnString;
    }

    private static String buildEventDataString(String dataType){
        String returnString = "";
        switch (dataType) {
            case "BOOLEAN":
                returnString = "value: " + "bool";
                break;

            case "FLOAT32":
                returnString = "value: " + "double";
                break;

            case "Dbpos":
                returnString = "value: " + "string";
                break;

            // TODO: Below datatypes not tested
            case "INT8U":
                returnString = "value: " + "count";
                break;

            case "INT32U":
                returnString = "value: " + "int";
                break;
        
            case "Timestamp":
                returnString = "value: " + "string";
                break;
        
            case "VISSTRING255":
                returnString = "value: " + "string";
                break;
        
            case "Quality":
                returnString = "value: " + "string";
                break;
        
            default:
                returnString = "error: int";
                break;
        }
        return returnString;
    }

    private static String buildDataString(String dataType, String index) {
        String returnString = "";
        switch (dataType) {
            case "BOOLEAN":
                returnString = "    bool " + "value = (this->connection()->upflow()->current_pdu_data_pair_vector[" + index +"].first == \"true\") ? true : false;";
                break;

            case "FLOAT32":
                returnString = "    double " + "value = std::stod(this->connection()->upflow()->current_pdu_data_pair_vector[" + index +"].first);";
                break;

             case "Dbpos":
                returnString = "    string " + "value = this->connection()->upflow()->current_pdu_data_pair_vector[" + index +"].first;";
                break;

            // TODO: Below datatypes not tested
            case "INT8U":
                returnString = "    unsigned " + "value = std::stoi(this->connection()->upflow()->current_pdu_data_pair_vector[" + index +"].first);";
                break;

            case "INT32U":
                returnString = "    int " + "value = std::stoi(this->connection()->upflow()->current_pdu_data_pair_vector[" + index +"].first);";
                break;

            case "Timestamp":
                returnString = "    string " + "value = this->connection()->upflow()->current_pdu_data_pair_vector[" + index +"].first;";
                break;

            case "VISSTRING255":
                returnString = "    string " + "value = this->connection()->upflow()->current_pdu_data_pair_vector[" + index +"].first;";
                break;

            case "Quality":
                returnString = "    string " + "value = this->connection()->upflow()->current_pdu_data_pair_vector[" + index +"].first;";
                break;

            default:
                returnString = "cerr << \"unkown data type\"" + " << endl;";
                break;
        }
        return returnString;
    }

    public static void main(String[] args) {

        String analyzerFilename = "custom-events.pac";
        String eventFilename = "event_test.bif";

        String fileName = "user_configuration.csv";

        File file = new File(fileName);

        try {
            // write analyzer
            FileWriter analyzerWriter = new FileWriter(analyzerFilename);
            PrintWriter analyzerOutputStream = new PrintWriter(analyzerWriter);
            Scanner inputStream = new Scanner(file);
            analyzerOutputStream.println("refine flow MMS_Flow += {");
            analyzerOutputStream.println("  function rule_function():bool");
            analyzerOutputStream.println("  %{");

            // write event
            FileWriter eventWriter = new FileWriter(eventFilename);
            PrintWriter eventOutputStream = new PrintWriter(eventWriter);

            // attributes (file first line)
            String attributeNames[] = inputStream.nextLine().split(",");

            // zeek target data
            int lineCounter = 2;
            while (inputStream.hasNext()) {
                String data = inputStream.nextLine();
                String[] values = data.split(",");

                analyzerOutputStream.println("    " + "string " + attributeNames[1] + "_from_csv_line_" + lineCounter + " = \"" + values[1] + "\";");
                analyzerOutputStream.println("    if(concatenated_domain_item_id==" + attributeNames[1] + "_from_csv_line_" + lineCounter + " && is_request && " + "this->connection()->upflow()->current_pdu_data_pair_vector.size() != 0){");
                analyzerOutputStream.println("  " + buildDataString(values[3], values[2]));
                analyzerOutputStream.println("      BifEvent::generate_" + values[0] + "(connection()->bro_analyzer(),");
                analyzerOutputStream.println("                                      connection()->bro_analyzer()->Conn(),");
                analyzerOutputStream.println("                                      invoke_id,");
                analyzerOutputStream.println("                                      this->connection()->upflow()->service,");
                analyzerOutputStream.println(buildBifDataString(values[3]));
                analyzerOutputStream.println("                                      );");
                analyzerOutputStream.println("    }");
                analyzerOutputStream.println();

                // event_test.bif TODO other data type
                eventOutputStream.println("##");
                eventOutputStream.println("##");
                eventOutputStream.println("##");
                eventOutputStream.println("##");
                eventOutputStream.println("##");
                eventOutputStream.print("event " + values[0] + "%(c: connection, invoke_id: int, service: int, ");
                eventOutputStream.print(buildEventDataString(values[3]));
                eventOutputStream.print("%);" + "\n");

                lineCounter++;
            }

            // analyzer
            analyzerOutputStream.println("    return false;");
            analyzerOutputStream.println("  %}");
            analyzerOutputStream.println("}");
            analyzerOutputStream.close();

            // event
            eventOutputStream.close();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
