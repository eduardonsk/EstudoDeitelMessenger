"""Uso: python executar.py caminho/para/DeitelMessager.zip (Python 3.8+ e JDK 11+)."""
from concurrent.futures import ThreadPoolExecutor
from contextlib import ExitStack
from pathlib import Path
import subprocess
import sys
import tempfile
import uuid
import zipfile


def ajustar(fonte, original, local):
    if fonte.count(original) != 1:
        raise ValueError(f"Versao inesperada dos fontes ao localizar {original!r}")
    return fonte.replace(original, local)


def compilar(arquivo, pasta):
    base = "DeitelMessager/src/"
    loop = 'java.net.InetAddress.getByName("127.0.0.1")'
    interface = f"java.net.NetworkInterface.getByInetAddress({loop})"
    fontes = []
    with zipfile.ZipFile(arquivo) as zip_livro:
        for item in zip_livro.namelist():
            if not item.startswith(base + "com/deitel/messenger/") or not item.endswith(".java"):
                continue
            # javac aceita fontes com pacote em qualquer pasta. O nome simples
            # evita extrair caminhos arbitrários e ignora os binários do anexo.
            destino = pasta / Path(item).name
            fonte = zip_livro.read(item).decode("utf-8")
            if destino.name == "DeitelMessengerServer.java":
                fonte = ajustar(fonte, "SERVER_PORT, 100", f"SERVER_PORT, 100, {loop}")
            elif destino.name == "MulticastSender.java":
                fonte = ajustar(fonte, "socket.send( packet );",
                    f"socket.setOption(java.net.StandardSocketOptions.IP_MULTICAST_IF, {interface});\n"
                    "socket.setOption(java.net.StandardSocketOptions.IP_MULTICAST_TTL, 0);\n"
                    "socket.send( packet );")
            elif destino.name == "PacketReceiver.java":
                for metodo in ("joinGroup", "leaveGroup"):
                    fonte = ajustar(fonte, f"{metodo}( multicastGroup )",
                        f"{metodo}(new java.net.InetSocketAddress(multicastGroup, 0), {interface})")
            destino.write_text(fonte, encoding="utf-8")
            fontes.append(str(destino))

    cliente = Path(__file__).resolve().with_name("ClienteDeTeste.java")
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(pasta), *fontes, str(cliente)],
                   check=True, timeout=30)


def iniciar(pilha, comando):
    p = subprocess.Popen(comando, stdout=subprocess.PIPE, text=True)
    def encerrar():
        if p.poll() is None:
            p.kill()
        p.wait(timeout=5)
        p.stdout.close()
    pilha.callback(encerrar)
    return p


def aguardar_linha(leitor, processo, esperado):
    linha = leitor.submit(processo.stdout.readline).result(timeout=10).rstrip()
    if esperado not in linha:
        raise RuntimeError(f"Esperava {esperado!r}; recebi {linha!r}")
    print(linha, flush=True)


def executar(arquivo):
    with tempfile.TemporaryDirectory() as temporario:
        pasta = Path(temporario)
        compilar(arquivo, pasta)
        java = ["java", "-Djava.net.preferIPv4Stack=true", "-cp", str(pasta)]
        ensaio = uuid.uuid4().hex[:12]
        print("TCP 127.0.0.1:12345 | UDP 239.0.0.1:5555 | interface 127.0.0.1 | TTL 0", flush=True)
        with ThreadPoolExecutor(max_workers=1) as leitor, ExitStack() as pilha:
            servidor = iniciar(pilha, [*java,
                "com.deitel.messenger.sockets.server.DeitelMessengerServerTest"])
            aguardar_linha(leitor, servidor, "Server listening on port 12345")
            bruno = iniciar(pilha, [*java, "ClienteDeTeste", "Bruno", ensaio])
            aguardar_linha(leitor, bruno, "Bruno iniciou a escuta")
            subprocess.run([*java, "ClienteDeTeste", "Alice", ensaio], check=True, timeout=15)
            bruno.wait(timeout=15)
            print(bruno.stdout.read(), end="", flush=True)
            if bruno.returncode:
                raise RuntimeError("Bruno terminou sem confirmar a mensagem esperada.")
        print("SUCESSO: remetente e texto conferidos nos dois processos clientes.")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    executar(sys.argv[1])
