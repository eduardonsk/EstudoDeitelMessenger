import argparse
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import re
import shutil
import subprocess
import sys
from tempfile import TemporaryDirectory
from zipfile import BadZipFile, ZipFile


PROJETO = Path(__file__).resolve().parent / "sistemas-operacionais-deitel"


def executar(comando):
    subprocess.run(comando, check=True, timeout=90)


def compilar(fontes, destino):
    destino.mkdir()
    executar(["javac", "-encoding", "UTF-8", "-d", str(destino), *map(str, fontes)])
    return ["java", "-cp", str(destino)]


def testar_tcp(java):
    with subprocess.Popen([*java, "MessageBufferServer", "0"], stdout=subprocess.PIPE, text=True) as servidor:
        try:
            inicio = servidor.stdout.readline()
            print(inicio, end="", flush=True)
            porta = re.search(r"\bporta=(\d+)\b", inicio)
            if not porta:
                raise RuntimeError("O servidor encerrou antes de informar a porta TCP.")
            with ThreadPoolExecutor(max_workers=2) as clientes:
                comandos = [[*java, "MessageBufferClient", nome, porta.group(1)] for nome in ("A", "B")]
                tarefas = [clientes.submit(executar, comando) for comando in comandos]
                for tarefa in tarefas:
                    tarefa.result()
            saida, _ = servidor.communicate(timeout=30)
            print(saida, end="", flush=True)
            if servidor.returncode:
                raise RuntimeError("O servidor encontrou uma falha ao conferir as mensagens.")
        finally:
            if servidor.poll() is None:
                servidor.kill()


def testar_capitulo_23(arquivo, temporaria):
    exemplos = [("Fig23_09-13", "SharedBufferTest"), ("Fig23_16-17", "SharedBufferTest2")]
    with ZipFile(arquivo) as anexo:
        for figura, classe in exemplos:
            print(f"\nCapítulo 23 | {figura}", flush=True)
            fontes = temporaria / figura
            fontes.mkdir()
            for item in anexo.namelist():
                caminho = Path(item)
                if caminho.parent.as_posix() == f"ch23/{figura}" and caminho.suffix == ".java":
                    (fontes / caminho.name).write_bytes(anexo.read(item))
            if not (fontes / f"{classe}.java").is_file():
                raise ValueError(f"O ZIP não contém ch23/{figura}/{classe}.java.")
            java = compilar(fontes.glob("*.java"), fontes / "classes")
            executar([*java, classe])


def main():
    opcoes = argparse.ArgumentParser(description="Compila e executa os exemplos do estudo de caso.")
    opcoes.add_argument("--capitulo23", type=Path, metavar="ZIP", help="inclui dois exemplos de Capítulo 23.zip")
    opcoes.add_argument("--messenger", type=Path, metavar="ZIP", help="inclui o chat de DeitelMessager.zip")
    args = opcoes.parse_args()
    if not shutil.which("javac") or not shutil.which("java"):
        opcoes.error("Instale um JDK e deixe os comandos javac e java disponíveis no terminal.")
    for arquivo in (args.capitulo23, args.messenger):
        if arquivo and not arquivo.is_file():
            opcoes.error(f"Arquivo não encontrado: {arquivo}")

    with TemporaryDirectory() as pasta:
        temporaria = Path(pasta)
        original = compilar((PROJETO / "ch05").glob("*.java"), temporaria / "ch05")
        for numero in range(1, 4):
            print(f"\nClasses fornecidas | execução {numero} de 3", flush=True)
            executar([*original, "SharedBufferTest"])

        seguro = compilar((PROJETO / "buffer-seguro").glob("*.java"), temporaria / "buffer")
        print("\nBuffer seguro | três produtores e um consumidor", flush=True)
        executar([*seguro, "MessageBufferDemo"])
        print("\nBuffer via TCP | servidor e dois clientes", flush=True)
        testar_tcp(seguro)
        if args.capitulo23:
            testar_capitulo_23(args.capitulo23, temporaria)
        if args.messenger:
            print("\nDeitel Messenger | mensagem de Alice para Bruno", flush=True)
            executar([sys.executable, str(PROJETO / "verificacao-messenger" / "executar.py"), str(args.messenger)])
    print("\nExecuções concluídas.", flush=True)


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, ValueError, BadZipFile, subprocess.SubprocessError) as erro:
        sys.exit(f"Falha na execução: {erro}")
