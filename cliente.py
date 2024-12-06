import socket
import threading
import time

# Semáforos para controlar el acceso a cada servidor
sem_principal = threading.Semaphore(1)
sem_secundario = threading.Semaphore(1)

def escribir_archivo(cliente_socket):
    nombre_archivo = input("Ingrese el nombre del archivo a guardar: ")
    contenido = input("Ingrese el contenido del archivo: ")

    mensaje = f"WRITE:{nombre_archivo}:{contenido}\n"
    try:
        cliente_socket.sendall(mensaje.encode())
        print("Solicitud de escritura enviada al servidor.")

        respuesta = cliente_socket.recv(4096).decode()
        print("Respuesta del servidor:", respuesta)

    except socket.timeout:
        print("Error: Tiempo de espera agotado para la respuesta del servidor.")
    except Exception as e:
        print("Error en la comunicación:", e)

def leer_archivo(cliente_socket):
    nombre_archivo = input("Ingrese el nombre del archivo a leer: ")
    mensaje = f"READ:{nombre_archivo}\n"
    try:
        cliente_socket.sendall(mensaje.encode())
        print("Solicitud de lectura enviada al servidor.")

        respuesta = cliente_socket.recv(4096).decode()
        print("Contenido del archivo:", respuesta)

    except socket.timeout:
        print("Error: Tiempo de espera agotado para la respuesta del servidor.")
    except Exception as e:
        print("Error en la comunicación:", e)

def elegir_servidor():
    while True:
        print("\n1. Conectar al Servidor Principal")
        print("2. Conectar al Servidor Secundario")
        print("3. Salir")
        opcion = input("Seleccione una opción: ")

        if opcion == "1":
            return ('localhost', 5000), sem_principal
        elif opcion == "2":
            return ('localhost', 5001), sem_secundario
        elif opcion == "3":
            print("Cerrando cliente...")
            exit()
        else:
            print("Opción no válida.")

def manejo_cliente(cliente_socket, semaforo):
    try:
        while True:
            print("\n1. Escribir archivo")
            print("2. Leer archivo")
            print("3. Salir")
            opcion = input("Seleccione una opción: ")

            if opcion == "1":
                escribir_archivo(cliente_socket)
            elif opcion == "2":
                leer_archivo(cliente_socket)
            elif opcion == "3":
                print("Cerrando cliente...")
                break
            else:
                print("Opción no válida.")
    finally:
        # Liberar el semáforo al salir
        semaforo.release()
        cliente_socket.close()
        print("Conexión cerrada y turno liberado.")

def iniciar_cliente():
    servidor, semaforo = elegir_servidor()

    print(f"Esperando turno para conectarse al servidor en {servidor[0]}:{servidor[1]}...")
    semaforo.acquire()  # Esperar turno para conectar
    print(f"Turno adquirido, conectándose al servidor en {servidor[0]}:{servidor[1]}.")

    cliente_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    cliente_socket.connect(servidor)

    print(f"Conexión establecida con el servidor en {servidor[0]}:{servidor[1]}.")

    hilo_cliente = threading.Thread(target=manejo_cliente, args=(cliente_socket, semaforo))
    hilo_cliente.start()

if __name__ == "__main__":
    iniciar_cliente()
