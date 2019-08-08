#include <DMXSerial.h> // auf Serial_1 <- (Serial_eins)
#include <ArduinoJson.h>
#define BTSerial Serial3
#define BAUD 115200
#define JSON_BUFFER 200

int state = 0;

// JSON Buffer + Counter + Flag
char json[JSON_BUFFER] = {0};
int jsonCounter = 0;
bool readJson = false;

// Variablen
int variableWert;
   
void setup()
{
  DMXSerial.init(DMXController);
  Serial.begin(BAUD); 
  BTSerial.begin(BAUD);
}

void loop()
{
  // Seriell auslesen
  readSerial();

  // Wenn Lesen fertig
  if (readJson) {
    Serial.println(json);

    // JSON String zum Parser schicken
    StaticJsonDocument<JSON_BUFFER> doc;
    bool success = parseJson(&doc, json);

    // JSON String auswerten
    if (success) {
      handleJson(doc); 
    }
    
    // Buffer "befreien"
    freeJson();
  }
}

//______________________________________________________________________________________________________________________________________________freeJson
// "Befreit" den JSON Buffer + Flag Reset
void freeJson() {
  for (int i = 0; i < jsonCounter; i++) {
    json[i] = 0;
  }
  
  jsonCounter = 0;
  readJson = false;
}

//______________________________________________________________________________________________________________________________________________readSerial
// Seriell auslesen, sofern vorhanden
void readSerial() {
  if (BTSerial.available()) {
    // Get single char from HM-10
    char c = BTSerial.read();
    // Activate flag if line feed found
    readJson = c == '\n';
    // Add char to buffer
    json[jsonCounter] = c;
    jsonCounter++;
  }
}

//_______________________________________________________________________________________________________________________________________________parseJson
// Parser (Auseinanderpflücken)
bool parseJson(JsonDocument *doc, char buff[]) {
    // Deserialize the JSON document
    DeserializationError error = deserializeJson(*doc, buff);
    
    // Test if parsing succeeds and reset buffer if failed
    if (error) {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.c_str());
      freeJson();
      return false;
    } else {
      return true;
    }
}

//________________________________________________________________________________________________________________________________________________handleJson
// Auswerten der "docs" -> {"cmd":"hanspeter","wert":"123"} 
// -> {"doc1":"value1", "doc2":"value2", "doc3":"value3", "doc4":"value4"} 
// -> {"lamp":"1","hue":"255","sat":"255","val":"255"}
void handleJson(JsonDocument doc) {
  int lamp = doc["lamp"];
  int hue = doc["hue"];
  int sat = doc["sat"];
  int val = doc["val"];
  DMXchannel (lamp, hue, sat, val);
}

//_______________________________________________________________________________________________________________________________________________DMXchannel
void DMXchannel (int lamp,int hue,int sat,int val)
{
    Serial.print("lamp: ");
    Serial.println(lamp);
    Serial.print("hue: ");
    Serial.println(hue);
    Serial.print("sat: ");
    Serial.println(sat);
    Serial.print("val: ");
    Serial.println(val); 
    
  switch (lamp)
  {
    case 1:
    //ARRI HSI
      DMXSerial.write(1,val); //Intensität
      DMXSerial.write(2,hue); //Farbton
      DMXSerial.write(3,sat); //Sättigung
      break;
   
    case 2:
    //TOURLED HSV  
      DMXSerial.write(4,hue); //Farbton
      DMXSerial.write(5,sat); //Sättigung
      DMXSerial.write(6,val); //Intensität  
      break;
 
      case 3:
    //ARRI HSI
      DMXSerial.write(7,val); //Intensität
      DMXSerial.write(8,hue); //Farbton
      DMXSerial.write(9,sat); //Sättigung
      break;
      
    case 4:
    //TOURLED HSV  
      DMXSerial.write(10,hue); //Farbton
      DMXSerial.write(11,sat); //Sättigung
      DMXSerial.write(12,val); //Intensität
      break;
      
    case 5:
    //ARRI HSI
      DMXSerial.write(13,val); //Intensität
      DMXSerial.write(14,hue); //Farbton
      DMXSerial.write(15,sat); //Sättigung
      break;     
     
    case 6:
    //P7 M2
      DMXSerial.write(16,255); //PAN            0-255
      DMXSerial.write(17,255); //PAN fein       0-255
      DMXSerial.write(18,255); //TILT           0-255
      DMXSerial.write(19,255); //TILT fein      0-255
     
      DMXSerial.write(22,255); //Dimmer         0-255
      DMXSerial.write(23,255); //Fokus          0-255
      DMXSerial.write(24,255); //Zoom           0-255
     
      DMXSerial.write(26,255); //GOBO 1         0-191
      DMXSerial.write(27,255); //GOBO 2         0-255
      DMXSerial.write(28,255); //GOBO Pos.      0-191
      DMXSerial.write(29,255); //GOBO Pos. fein 0-255
      DMXSerial.write(30,255); //Farbrad:0-033; CTO:34-39; Linear:64-191
      break;
     
    default:
      break;
  }    
}
