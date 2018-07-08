#include <Servo.h>    //use of the library disables analogWrite() (PWM) functionality on pins 9 and 10, whether or not there is a Servo on those pins, Uses Timer1 (16 bits)
#include <SevenSeg.h> // Uses Timer2
#include <NewPing.h> //To avoid conflict with SevenSeg library, usage of timer in NewPing.h should be disabled (#define TIMER_ENABLED false).
#include "DHT.h" // Include DHT Libraries from Adafruit


#define servopin 9 
#define MAX_DISTANCE 200

#define DHTPIN 10       // DHT-22 Output Pin connection
#define DHTTYPE DHT22   // DHT Type is DHT22(AM2302,RHT03)

const int trigPinF = 7;  //Ultrasonic sensor front
const int echoPinF = 8;

const int trigPinB = 11;  //Ultrasonic sensor back
const int echoPinB = 12;

DHT dht(DHTPIN, DHTTYPE); // Initialize DHT sensor for normal 16Mhz Arduino

NewPing sonarF(trigPinF, echoPinF, MAX_DISTANCE);
NewPing sonarB(trigPinB, echoPinB, MAX_DISTANCE);

SevenSeg disp(A0, A1, A2, A3, A4, A5, 2); //Analog or Digital pin does not matter
const int numOfDigits = 4;
int digitPins[numOfDigits] = {3,4,5,6};  //Analog or Digital pin does not matter

Servo myServo;
String Measurement;
String receivedData = "-1";  //Data coming from Android smart phone through bluetooth module

int angle = 0;    //Servo motor angle
int distance1 = 0;
int distance2 = 0;

float hum;    // Stores humidity value in percent
float temp;   // Stores temperature value in Celcius
float duration; // Stores HC-SR04 pulse duration value
float soundSp_M_S;  // Stores calculated speed of sound in M/S
float soundSp_CM_uS;  // Stores calculated speed of sound in cm/uS
float DistCoeficient;

void sendMeasurement(int angle, int distance1, int distance2){
  
                Measurement = String(angle);
                Measurement += ",";
                Measurement += String(distance1);
                Measurement += ",";
                Measurement += String(distance2);

                if(distance1 > 0 && distance2 > 0){
                  digitalWrite(LED_BUILTIN, HIGH);
                  disp.write("F"+String(distance1));
                  delay(850);
                  disp.write("B"+String(distance2));
                }else if(distance1 > 0) {
                  digitalWrite(LED_BUILTIN, HIGH);
                  disp.write("F"+String(distance1));
                  delay(100);
                }else if( distance2 > 0 ){
                  digitalWrite(LED_BUILTIN, HIGH);
                  disp.write("B"+String(distance2));
                  delay(100);
               }
                Serial.println(Measurement);
                digitalWrite(LED_BUILTIN, LOW); 
}

 int MapAngle(int angle){
      if(angle >= 0 && angle < 5)
        return map(angle,0,5,500,900);
      else if(angle >= 5 && angle < 10)
        return map(angle,5,10,900,1000);
      else 
        return map(angle,10,180,1000,2400);                   
 }
 
void setup() {
  pinMode(LED_BUILTIN, OUTPUT); 
  myServo.attach(servopin);
  Serial.begin(115200); // Starts the serial communication
  
  disp.setDigitPins(numOfDigits, digitPins);
  disp.setCommonAnode();
  disp.setTimer(2);  //Timer0: used for delay(), millis() and micros() functions, Timer1: used for servo library
  //disp.setRefreshRate(50);
  disp.startTimer();
  
  delay(4000);  // Delay so DHT22 sensor can stabalize
   
  hum = dht.readHumidity();  // Get Humidity value
  temp = dht.readTemperature();  // Get Temperature value
    
  // Calculate the Speed of Sound in M/S
  soundSp_M_S = 331.3 + (0.606 * temp) + (0.0124 * hum);
  soundSp_CM_uS = soundSp_M_S / 10000.0;        // Convert to cm/uS
  DistCoeficient = soundSp_CM_uS/2;
}

void loop() {
   if (Serial.available()){
        receivedData = Serial.readString();
   }   
          if(receivedData == "-2"){   // -2 to turn on servo
              for(angle = 0; angle <= 180; angle += 5)    // command to move from 0 degrees to 180 degrees 
              {             
                myServo.writeMicroseconds(MapAngle(angle));                 //command to rotate the servo to the specified angle
                delay(300);
    
                duration = sonarF.ping();  //Send a ping, returns the echo time in microseconds or 0 (zero) if no ping echo within set distance limit
                distance1 = (int) (duration*DistCoeficient) ;                       

                duration = sonarB.ping();  //Send a ping, returns the echo time in microseconds or 0 (zero) if no ping echo within set distance limit
                distance2 = (int) (duration*DistCoeficient);                             
                delay(50);
                
                sendMeasurement(angle, distance1, distance2);
                                    
                if (Serial.available()){
                    receivedData =  Serial.readString();
                }
                
                if(receivedData == "-1"){  // -1 to stop servo
                   /*while(receivedData!= "-2"){
                    if(Serial.available()){
                         receivedData =  Serial.readString();
                     }
                   }*/
                   break;
                }

              } 

              sendMeasurement(-1, -1, -1);  // marks the end of measurement
              //disp.clearDisp();  //Not working?
      
          }else if(receivedData != "-1" && receivedData != "-2"){
                angle = receivedData.toInt();
                myServo.write(angle); 
                delay(400);
                duration = sonarF.ping();  //Send a ping, returns the echo time in microseconds or 0 (zero) if no ping echo within set distance limit
                distance1 = (int) (duration*DistCoeficient) ;                       

                duration = sonarB.ping();  //Send a ping, returns the echo time in microseconds or 0 (zero) if no ping echo within set distance limit
                distance2 = (int) (duration*DistCoeficient);                            
                sendMeasurement(angle, distance1, distance2);
                receivedData = "-1";
                delay(50);
                sendMeasurement(-1, -1, -1);  // marks the end of measurement 
          }
}


/*When an interrupt service routine (ISR) is called, interrupts are automatically disabled.  
 Interrupts are automatically enabled when returning from an ISR. */

ISR (TIMER2_COMPA_vect){
  sei();
  disp.interruptAction();
}
