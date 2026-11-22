from concurrent.futures import ThreadPoolExecutor
from contextlib import ExitStack
from pathlib import Path
import argparse
import subprocess
import tempfile
import uuid
import zipfile


def substituir_uma_vez(fonte, trecho, substituto):
    if fonte.count(trecho) != 1:
        raise ValueError(f"Versão inesperada do código do livro ao localizar {trecho!r}.")
    return fonte.replace(trecho, substituto)


def restringir_ao_loopback(nome, fonte):
    loopback = 'java.net.InetAddress.getByName("127.0.0.1")'
    interface = f"java.net.NetworkInterface.getByInetAddress({loopback})"

    if nome == "DeitelMessengerServer.java":
        return substituir_uma_vez(fonte, "SERVER_PORT, 100", f"SERVER_PORT, 100, {loopback}")

    if nome == "MulticastSender.java":
        enviar = "socket.send( packet );"
        opcoes_locais = (
            f"socket.setOption(java.net.StandardSocketOptions.IP_MULTICAST_IF, {interface});\n"
            "socket.setOption(java.net.StandardSocketOptions.IP_MULTICAST_TTL, 0);\n"
        )
        return substituir_uma_vez(fonte, enviar, opcoes_locais + enviar)

    if nome == "PacketReceiver.java":
        for metodo in ("joinGroup", "leaveGroup"):
            fonte = substituir_uma_vez(
                fonte,
                f"{metodo}( multicastGroup )",
                f"{metodo}(new java.net.InetSocketAddress(multicastGroup, 0), {interface})",
            )
    return fonte


def compilar(arquivo_zip, pasta):
    fontes = []
    with zipfile.ZipFile(arquivo_zip) as anexo:
        for caminho in anexo.namelist():
            if not caminho.startswith("DeitelMessager/src/com/deitel/messenger/"):
                continue
            if not caminho.endswith(".java"):
                continue
            destino = pasta / Path(caminho).name
            original = anexo.read(caminho).decode("utf-8")
            destino.write_text(restringir_ao_loopback(destino.name, original), encoding="utf-8")
            fontes.append(str(destino))

    cliente_de_teste = Path(__file__).with_name("ClienteDeTeste.java")
    subprocess.run(
        ["javac", "-encoding", "UTF-8", "-d", str(pasta), *fontes, str(cliente_de_teste)],
        check=True, timeout=30,
    )


def iniciar_processo(recursos, comando):
    processo = subprocess.Popen(comando, stdout=subprocess.PIPE, text=True)

    def encerrar():
        if processo.poll() is None:
            processo.kill()
        processo.wait(timeout=5)
        processo.stdout.close()

    recursos.callback(encerrar)
    return processo


def aguardar_linha(leitor, processo, texto_esperado):
    leitura = leitor.submit(processo.stdout.readline)
    linha = leitura.result(timeout=10).rstrip()
    if texto_esperado not in linha:
        raise RuntimeError(f"Esperava {texto_esperado!r}, recebi {linha!r}.")
    print(linha, flush=True)


def executar(arquivo_zip):
    with tempfile.TemporaryDirectory() as diretorio:
        pasta = Path(diretorio)
        compilar(arquivo_zip, pasta)
        java = ["java", "-Djava.net.preferIPv4Stack=true", "-cp", str(pasta)]
        classe_servidor = "com.deitel.messenger.sockets.server.DeitelMessengerServerTest"
        ensaio = uuid.uuid4().hex[:12]
        print("TCP 127.0.0.1:12345 | UDP 239.0.0.1:5555 | interface 127.0.0.1 | TTL 0", flush=True)
        with ThreadPoolExecutor(max_workers=1) as leitor, ExitStack() as recursos:
            servidor = iniciar_processo(recursos, [*java, classe_servidor])
            aguardar_linha(leitor, servidor, "Server listening on port 12345")

            bruno = iniciar_processo(recursos, [*java, "ClienteDeTeste", "Bruno", ensaio])
            aguardar_linha(leitor, bruno, "Bruno iniciou a escuta")

            subprocess.run([*java, "ClienteDeTeste", "Alice", ensaio], check=True, timeout=15)
            bruno.wait(timeout=15)
            print(bruno.stdout.read(), end="", flush=True)
            if bruno.returncode:
                raise RuntimeError("Bruno terminou sem confirmar a mensagem esperada.")
        print("SUCESSO: remetente e texto conferidos nos dois processos clientes.")


if __name__ == "__main__":
    argumentos = argparse.ArgumentParser(
        description="Executa o Messenger com dois clientes locais. Requer Python 3.8+ e JDK 11+.",
        usage="python %(prog)s caminho/para/DeitelMessager.zip",
    )
    argumentos.add_argument("arquivo_zip", type=Path, help="ZIP do Messenger fornecido na disciplina")
    arquivo_zip = argumentos.parse_args().arquivo_zip
    if not arquivo_zip.is_file():
        argumentos.error(f"Arquivo não encontrado: {arquivo_zip}")
    executar(arquivo_zip)