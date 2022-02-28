# Simulador de movimientos de fraude

Este proyecto permite generar diferentes movimientos de tarjetas (cajero, comercio físico y sitios online) simulando acciones
fraudulentas.


## Uso

### Proceso de simulación de fraude
El servicio lanza un proceso de simulación de fraude de la siguiente forma:

- recibe un parámetro indicando las tarjetas y los tipos de fraude que se quieren simular por tarjeta durante la simulación
- recibe un parámetro indicando el número de iteraciones que va a realizar y otro parámetro indicando el tiempo entre ellas
- recibe un parámetro indicando 
- por cada iteración:
    - lanza, en paralelo, el proceso de simulación asociado a cada tarjeta, de forma que no los movimientos asociados a las tarjetas se "mezclen",
    simulando un escenario real
    - por cada tarjeta, en paralelo y según la configuración recibida se podrán generar los siguientes casos
        1) caso de fraude en cajero: genera varios movimientos en cajeros diferentes con esa tarjeta
        2) caso de fraude en comercio físico: genera varios movimientos en comercios diferentes con esa tarjeta
        3) caso de fraude por múltiples compras online: genera varios movimientos en sitios online diferentes con esa tarjeta y superando la cantidad definida para fraude (100)
        4) caso de fraude por compra en sitio online fraudulento: genera un movimiento de compra online en un sitio potencialmente fraudulento (empieza por "fraud-")
        5) caso de fraude por uso en cajero y comercio físico a la vez: genera un movimiento de cajero y uno de comercio físico para la misma tarjeta. Si se ha indicado
           los casos 1 y 2 en la configuración, este caso también se crearía, ya que con 1 y 2 se dispondría de movimientos en cajero y en comercio físico
    
Cada proceso dispone de un identificador único y, al finalizar la creación de movimientos, se escribe un mensaje en un topic de resultados incluyendo el número de movimientos
creados en los diferentes topics (cajero, físico y online) y el número de casos de fraude que se deberían detectar en total.


El proceso se arranca mediante un API Rest, que detallamos a continuación


### API
El servicio expone un API Rest con una operación POST publicada en la ruta: http://localhost:8090/fraud-simulation 
Esta operación permite iniciar el proceso de simulación de fraude, creando n movimientos de tarjetas. 

El cuerpo de la petición POST debe ser un objeto JSON con la siguiente estructura:

- iterations (numérico): número de iteraciones que va a realizar el proceso
- msBetweenIterations (numérico): tiempo en milisegundos entre cada iteración
- cardsFraudConfig (array de objetos): array de configuración de tarjetas y tipos de fraude. Los objetos del array deben tener la siguiente estructura:
    - card (texto): número de la tarjeta
    - fraudTypes (array de números): tipos de fraude que se van a aplicar a la tarjeta. Los valores pueden ser 1,2,3,4 o 5. Se explica más adelante


#### Tipos de fraude
Los tipos de fraude (atributo "fraudTypes" del array de objetos "cardsFraudConfig") son:

| Tipo de fraude                 |      Descripción                                                                          |  Valor API |
|--------------------------------|:-----------------------------------------------------------------------------------------:|-----------:|
| Fraude en cajero               |  Varios movimientos en cajeros diferentes con una tarjeta en un periodo corto de tiempo   |     1      |
| Fraude en comercio             |  Varios movimientos en comercios diferentes con una tarjeta en un periodo corto de tiempo |     2      |
| Fraude online                  |  Varios movimientos online con una tarjeta en un periodo corto de tiempo                  |     3      |
| Fraude online sitio sospechoso |  Compra online en un sitio potencialmente fraudulento (empieza por "fraud-")              |     4      |
| Fraude cajero y comercio       |  Movimientos en cajero y comercio con una tarjeta en un periodo corto de tiempo           |     5      |


#### Ejemplos de invocación

##### Todos los casos de fraude para todas las tarjetas
El siguiente comando genera un proceso de simulación de fraude para todos los casos (1,2,3,4,5) y tres tarjetas. Además
realizará 3 iteraciones estableciendo un tiempo de espera entre iteración de 10 segundos:

```shell
curl --location --request POST 'http://localhost:8090/fraud-simulation' \
--header 'Content-Type: application/json' \
--data-raw '{
    "iterations": 3,
    "msBetweenIterations": 10000,
    "cardsFraudConfig": [
        {
            "card": "card-1",
            "fraudTypes": [1,2,3,4]
        },
        {
            "card": "card-2",
            "fraudTypes": [1,2,3,4]
        },
        {
            "card": "card-3",
            "fraudTypes": [1,2,3,4]
        }
    ]
}'
```

En este caso se generarán movimientos en todos los topics (cajero, comercio y online)

(*) No es necesario indicar (1,2,3,4,5) porque al indicar los casos 1 (fraude en cajero) y 2 (fraude en comercio), de forma transitiva se
genera el caso 5 (fraude en cajero y comercio)

##### Caso de fraude en cajero
El siguiente comando genera un proceso de simulación de fraude para el caso de fraude en cajero (1) y una tarjeta. Además
realizará una única iteración:

```shell
curl --location --request POST 'http://localhost:8090/fraud-simulation' \
--header 'Content-Type: application/json' \
--data-raw '{
    "iterations": 1,
    "msBetweenIterations": 1,
    "cardsFraudConfig": [
        {
            "card": "card-1",
            "fraudTypes": [1]
        }
    ]
}'
```

En este caso solo se generarán movimientos en el topic de cajeros

##### Caso de fraude en comercio
El siguiente comando genera un proceso de simulación de fraude para el caso de fraude en comercio (2) y dos tarjetas. Además
realizará dos iteraciones y esperará 5 segundos entre cada iteración:

```shell
curl --location --request POST 'http://localhost:8090/fraud-simulation' \
--header 'Content-Type: application/json' \
--data-raw '{
    "iterations": 2,
    "msBetweenIterations": 5000,
    "cardsFraudConfig": [
        {
            "card": "card-1",
            "fraudTypes": [2]
        },
        {
            "card": "card-2",
            "fraudTypes": [2]
        }
    ]
}'
```

En este caso solo se generarán movimientos en el topic de comercios

##### Caso de fraude online por múltiples compras
El siguiente comando genera un proceso de simulación de fraude para el caso de fraude online por múltiples compras (3) y una tarjeta. Además
realizará dos iteraciones y esperará 5 segundos entre cada iteración:

```shell
curl --location --request POST 'http://localhost:8090/fraud-simulation' \
--header 'Content-Type: application/json' \
--data-raw '{
    "iterations": 2,
    "msBetweenIterations": 5000,
    "cardsFraudConfig": [
        {
            "card": "card-1",
            "fraudTypes": [3]
        }
    ]
}'
```

En este caso solo se generarán movimientos en el topic de online

##### Caso de fraude online en sitio potencialmente fraudulento
El siguiente comando genera un proceso de simulación de fraude para el caso de fraude online por compra en sitio potencialmente fraudulento (4) y una tarjeta. Además
realizará dos iteraciones y esperará 5 segundos entre cada iteración:

```shell
curl --location --request POST 'http://localhost:8090/fraud-simulation' \
--header 'Content-Type: application/json' \
--data-raw '{
    "iterations": 2,
    "msBetweenIterations": 5000,
    "cardsFraudConfig": [
        {
            "card": "card-1",
            "fraudTypes": [4]
        }
    ]
}'
```

En este caso solo se generarán movimientos en el topic de online

##### Caso de fraude por uso en cajero y comercio en un periodo corto de tiempo
El siguiente comando genera un proceso de simulación de fraude para el caso de fraude online por uso en cajero y comercio en un periodo corto de tiempo (5) y una tarjeta. Además
realizará una única iteración:

```shell
curl --location --request POST 'http://localhost:8090/fraud-simulation' \
--header 'Content-Type: application/json' \
--data-raw '{
    "iterations": 1,
    "msBetweenIterations": 1,
    "cardsFraudConfig": [
        {
            "card": "card-1",
            "fraudTypes": [5]
        }
    ]
}'
```

En este caso solo se generarán movimientos en el topic de cajeros y comercios


## Guía de desarrollo

### Arrancar en local
El proyecto etá basado en Quarkus y Maven. El servicio se puede arrancar en local apuntando a una instancia de Kafka. Para ello, tendremos
que configurar las propiedades del servicio ("src/main/resources/application.properties") para indicar la ruta del broker de Kafka, mediante la propiedad

````shell
kafka.bootstrap.servers=<indica la ruta al broker>
````

Una vez configurada, ejecutaremos:

```shell
mvn quarkus:dev -DskipTests
```


### Imagen Docker

Para construir la imagen Docker del servicio que puede ser desplegada en una plataforma de contenedores, se debe lanzar el comando:

```shell
 mvn clean package -DskipTests -Dquarkus.container-image.build=true
```

Se generará la imagen 