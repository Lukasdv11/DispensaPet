#include <SoftwareSerial.h>
#include <Servo.h>

const byte PIN_BT_RX = 10;
const byte PIN_BT_TX = 11;
const byte PIN_SERVO = 9;
const byte PIN_TRIG = 7;
const byte PIN_ECHO = 6;
const byte PIN_BOTON = 2;

const int ANGULO_CERRADO = 0;
const int ANGULO_ABIERTO = 80;
const unsigned long TIEMPO_ABIERTO_MS = 600;

const unsigned long PAUSA_MINIMA_MS = 3000UL;
const unsigned long VENTANA_MS = 86400000UL;
const byte MAX_PORCIONES_VENTANA = 12;
const unsigned long REPORTE_NIVEL_MS = 30000UL;

const int DISTANCIA_LLENO_CM = 4;
const int DISTANCIA_VACIO_CM = 22;

SoftwareSerial bt(PIN_BT_RX, PIN_BT_TX);
Servo compuerta;

unsigned long ultimaPorcionMs = 0;
bool huboPorcion = false;
unsigned long inicioVentanaMs = 0;
byte porcionesEnVentana = 0;
unsigned long ultimoReporteMs = 0;
int estadoBotonAnterior = HIGH;
unsigned long ultimoCambioBotonMs = 0;

void setup() {
  compuerta.attach(PIN_SERVO);
  compuerta.write(ANGULO_CERRADO);
  delay(400);
  compuerta.detach();

  pinMode(PIN_TRIG, OUTPUT);
  pinMode(PIN_ECHO, INPUT);
  pinMode(PIN_BOTON, INPUT_PULLUP);

  Serial.begin(9600);
  bt.begin(9600);
  enviar("LISTO");
  reportarNivel();
}

void loop() {
  while (bt.available() > 0) {
    char c = bt.read();
    if (c == 'D') {
      intentarDispensar("APP");
    } else if (c == 'N') {
      reportarNivel();
    } else if (c == '\r' || c == '\n' || c == ' ') {
    } else {
      enviar("RECHAZO:COMANDO");
    }
  }

  int lectura = digitalRead(PIN_BOTON);
  if (lectura != estadoBotonAnterior && millis() - ultimoCambioBotonMs > 50) {
    ultimoCambioBotonMs = millis();
    estadoBotonAnterior = lectura;
    if (lectura == LOW) {
      intentarDispensar("BOTON");
    }
  }

  if (millis() - ultimoReporteMs >= REPORTE_NIVEL_MS) {
    reportarNivel();
  }
}

void intentarDispensar(const char* origen) {
  unsigned long ahora = millis();
  if (ahora - inicioVentanaMs >= VENTANA_MS) {
    inicioVentanaMs = ahora;
    porcionesEnVentana = 0;
  }
  if (huboPorcion && ahora - ultimaPorcionMs < PAUSA_MINIMA_MS) {
    enviar("RECHAZO:INTERVALO");
    return;
  }
  if (porcionesEnVentana >= MAX_PORCIONES_VENTANA) {
    enviar("RECHAZO:LIMITE");
    return;
  }

  dispensarPorcion();
  ultimaPorcionMs = millis();
  huboPorcion = true;
  porcionesEnVentana++;

  String mensaje = "ENTREGA:";
  mensaje += origen;
  enviar(mensaje);
  reportarNivel();
}

void dispensarPorcion() {
  compuerta.attach(PIN_SERVO);
  compuerta.write(ANGULO_ABIERTO);
  delay(TIEMPO_ABIERTO_MS);
  compuerta.write(ANGULO_CERRADO);
  delay(400);
  compuerta.detach();
}

int leerNivel() {
  digitalWrite(PIN_TRIG, LOW);
  delayMicroseconds(2);
  digitalWrite(PIN_TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(PIN_TRIG, LOW);
  unsigned long duracion = pulseIn(PIN_ECHO, HIGH, 30000UL);
  if (duracion == 0) return -1;
  int distancia = duracion / 58;
  int nivel = map(distancia, DISTANCIA_VACIO_CM, DISTANCIA_LLENO_CM, 0, 100);
  return constrain(nivel, 0, 100);
}

void reportarNivel() {
  ultimoReporteMs = millis();
  int nivel = leerNivel();
  if (nivel >= 0) {
    enviar("NIVEL:" + String(nivel));
  }
}

void enviar(const String& texto) {
  bt.println(texto);
  Serial.println(texto);
}
